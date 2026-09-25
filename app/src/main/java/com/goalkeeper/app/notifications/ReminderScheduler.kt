package com.goalkeeper.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.goalkeeper.app.data.db.GoalEntity
import com.goalkeeper.app.data.db.GoalKeeperDatabase
import com.goalkeeper.app.data.db.toDomain
import com.goalkeeper.app.data.settings.SettingsRepository
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.reminder.NudgeMessages
import com.goalkeeper.core.reminder.NudgePlan
import com.goalkeeper.core.reminder.ReminderPlanner
import com.goalkeeper.core.streak.StreakCalculator
import java.time.LocalDate

/**
 * Keeps every goal's next nudge alarm (and the morning briefing alarm) in sync with the database,
 * and turns firing alarms into notifications. The single source of truth for "is a nudge due" is
 * [ReminderPlanner]; this class only wires that pure decision to [AlarmManager] and [NotificationHelper].
 */
class ReminderScheduler(
    private val context: Context,
    private val db: GoalKeeperDatabase,
    private val settings: SettingsRepository,
    private val notifications: NotificationHelper,
    private val clock: AppClock,
) : ReminderSync {

    override suspend fun sync(goalId: Long) {
        val entity = db.goalDao().get(goalId)
        if (entity == null) {
            cancel(goalId)
            return
        }
        syncGoalEntity(entity)
    }

    override suspend fun syncAll() {
        db.goalDao().getAll().forEach { entity ->
            runCatching { syncGoalEntity(entity) }
        }
        syncBriefing()
    }

    override fun cancel(goalId: Long) {
        alarmManager()?.cancel(nudgePendingIntent(goalId))
        notifications.cancelNudge(goalId)
    }

    /** Re-arms (or cancels) the morning briefing alarm from the current settings. */
    suspend fun syncBriefing() {
        val current = settings.current()
        if (current.briefingEnabled) {
            val at = ReminderPlanner.nextDailyTime(current.briefingMinute, clock.now())
            scheduleAlarm(at.atZone(clock.zone()).toInstant().toEpochMilli(), briefingPendingIntent())
        } else {
            alarmManager()?.cancel(briefingPendingIntent())
        }
    }

    /** Whether the app can currently schedule exact alarms (always true below API 31). */
    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager()?.canScheduleExactAlarms() ?: false
        } else {
            true
        }

    /** Deep-links into the system's "allow exact alarms" screen, or null below API 31 (not needed there). */
    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            null
        }

    /**
     * Shows a sample nudge so the user can see what reminders look like. Returns false when
     * notifications are not allowed; true otherwise (including when there are no goals yet).
     */
    suspend fun sendTestNudge(): Boolean {
        if (!notifications.areNotificationsEnabled()) return false

        val today = clock.today()
        val activeGoals = db.goalDao().getActive()
        val goal: Goal
        val streak: Int
        if (activeGoals.isEmpty()) {
            goal = Goal(
                id = TEST_GOAL_ID,
                title = "Your first goal",
                why = "",
                startDate = today,
                createdAt = clock.instant(),
            )
            streak = 0
        } else {
            goal = activeGoals.first().toDomain()
            val checkIns = db.checkInDao().getForGoal(goal.id).map { it.toDomain() }
            streak = StreakCalculator.stats(goal, checkIns, today).current
        }

        val count = goal.reminder.nudgeCount
        val text = NudgeMessages.nudge(goal.title, goal.why, streak, 0, count, variant = today.dayOfYear)
        notifications.showNudge(goal, today, text, 0, count)
        return true
    }

    /** Handles a fired nudge alarm: shows the notification if still warranted, then arms the next one. */
    internal suspend fun handleNudgeAlarm(goalId: Long, epochDay: Long, index: Int, count: Int) {
        val entity = db.goalDao().get(goalId)
        if (entity != null && !entity.archived && entity.reminderEnabled) {
            val goal = entity.toDomain()
            val date = LocalDate.ofEpochDay(epochDay)
            val today = clock.today()
            val checkIns = db.checkInDao().getForGoal(goalId).map { it.toDomain() }
            val resolved = checkIns.any { it.date == date }
            if (shouldShowNudge(goal, date, today, resolved)) {
                val streak = StreakCalculator.stats(goal, checkIns, today).current
                val text = NudgeMessages.nudge(goal.title, goal.why, streak, index, count, variant = today.dayOfYear + index)
                notifications.showNudge(goal, date, text, index, count)
            }
        }
        // Always re-arm, whether or not we showed anything above (the goal may have been deleted,
        // archived, or already resolved by the time this alarm fired).
        sync(goalId)
    }

    /** Handles a fired briefing alarm: shows the summary if enabled, then arms tomorrow's briefing. */
    internal suspend fun handleBriefingAlarm() {
        val current = settings.current()
        if (current.briefingEnabled) {
            val today = clock.today()
            val activeEntities = db.goalDao().getActive()
            var dueCount = 0
            var topTitle: String? = null
            var longest = 0
            for (entity in activeEntities) {
                val goal = entity.toDomain()
                val checkIns = db.checkInDao().getForGoal(goal.id).map { it.toDomain() }
                val stats = StreakCalculator.stats(goal, checkIns, today)
                longest = maxOf(longest, stats.current)
                val resolvedToday = checkIns.any { it.date == today }
                if (isDueForBriefing(goal, today, resolvedToday)) {
                    dueCount++
                    if (topTitle == null) topTitle = goal.title
                }
            }
            notifications.showBriefing(NudgeMessages.briefing(dueCount, topTitle, longest))
        }
        syncBriefing()
    }

    /** Loads a goal's check-ins, computes its next nudge, and arms (or cancels) its alarm accordingly. */
    private suspend fun syncGoalEntity(entity: GoalEntity) {
        if (entity.archived || !entity.reminderEnabled) {
            cancel(entity.id)
            return
        }
        val goal = entity.toDomain()
        val checkIns = db.checkInDao().getForGoal(entity.id).map { it.toDomain() }
        val resolvedDates = checkIns.map { it.date }.toSet()

        if (clock.today() in resolvedDates) {
            // Today is already checked in: drop whatever nudge is currently on screen right away,
            // independent of whatever future date the alarm below ends up armed for.
            notifications.cancelNudge(goal.id)
        }

        val plan = ReminderPlanner.nextNudge(goal, clock.now()) { date -> date in resolvedDates }
        if (plan == null) {
            cancel(goal.id)
        } else {
            armNudge(plan)
        }
    }

    private fun armNudge(plan: NudgePlan) {
        val triggerAt = plan.at.atZone(clock.zone()).toInstant().toEpochMilli()
        val pendingIntent = nudgePendingIntent(plan.goalId, plan.date.toEpochDay(), plan.index, plan.count)
        scheduleAlarm(triggerAt, pendingIntent)
    }

    private fun scheduleAlarm(triggerAt: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager() ?: return
        if (canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                return
            } catch (_: SecurityException) {
                // The permission was revoked between the check above and this call; fall back below.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun alarmManager(): AlarmManager? = context.getSystemService(AlarmManager::class.java)

    /**
     * The alarm PendingIntent for one goal's next nudge. One slot per goal (the `data` Uri only
     * varies by [goalId]), so arming a new nudge replaces whatever was previously scheduled.
     * The default extras are only used when building a PendingIntent purely to cancel one.
     */
    private fun nudgePendingIntent(goalId: Long, epochDay: Long = 0L, index: Int = 0, count: Int = 0): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NUDGE
            data = Uri.parse("goalkeeper://nudge/$goalId")
            putExtra(ReminderReceiver.EXTRA_GOAL_ID, goalId)
            putExtra(ReminderReceiver.EXTRA_EPOCH_DAY, epochDay)
            putExtra(ReminderReceiver.EXTRA_INDEX, index)
            putExtra(ReminderReceiver.EXTRA_COUNT, count)
        }
        return PendingIntent.getBroadcast(
            context,
            goalId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun briefingPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_BRIEFING
            data = Uri.parse("goalkeeper://briefing")
        }
        return PendingIntent.getBroadcast(
            context,
            BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Placeholder goal id used by [sendTestNudge] when the user has no goals yet. */
        const val TEST_GOAL_ID = 999_999L

        private const val BRIEFING_REQUEST_CODE = 1_000_000
    }
}

/**
 * Whether a fired nudge alarm for [goal]'s [date] should actually show a notification right now:
 * the goal must still be active with reminders enabled, the alarm must be for today, and today
 * must not already be checked in.
 */
internal fun shouldShowNudge(goal: Goal, date: LocalDate, today: LocalDate, resolved: Boolean): Boolean =
    goal.isActive && goal.reminder.enabled && date == today && !resolved

/**
 * Whether [goal] still counts toward the morning briefing: active, scheduled today, on or after
 * its start date, and not yet checked in today.
 */
internal fun isDueForBriefing(goal: Goal, today: LocalDate, resolvedToday: Boolean): Boolean =
    goal.isActive && goal.schedule.isScheduled(today) && !today.isBefore(goal.startDate) && !resolvedToday
