package com.goalkeeper.app.ui.today

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.data.settings.AppSettings
import com.goalkeeper.app.data.settings.SettingsRepository
import com.goalkeeper.app.notifications.NotificationHelper
import com.goalkeeper.app.notifications.ReminderScheduler
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.GoalSummary
import com.goalkeeper.app.ui.common.summarize
import com.goalkeeper.app.ui.components.WeekDayUi
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.insights.InsightsCalculator
import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.reminder.NudgeMessages
import com.goalkeeper.core.streak.StreakCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** One goal row on the Today queue or rest-day list, with the bits [TodayViewModel] precomputed for display. */
data class TodayGoalRow(
    val summary: GoalSummary,
    /** Local time the day's check-in was recorded, when [GoalSummary.todayStatus] is [DayStatus.DONE]. */
    val doneAt: LocalTime?,
)

data class TodayUiState(
    val loading: Boolean = true,
    val dateLabel: String = "",
    val headline: String = "",
    val weekDays: List<WeekDayUi> = emptyList(),
    val todayStat: String = "0/0",
    val topStreakStat: String = "0",
    val weekRateStat: String = Formats.percent(null),
    val priorityQueue: List<TodayGoalRow> = emptyList(),
    val restDay: List<TodayGoalRow> = emptyList(),
    val hasActiveGoals: Boolean = false,
    val showNotificationsBanner: Boolean = false,
    val showExactAlarmBanner: Boolean = false,
)

/** One-shot UI effects: milestone celebration and the undo-able check-in removal. */
sealed interface TodayEvent {
    data class Milestone(val message: String) : TodayEvent
    data class CheckInRemoved(val goalId: Long, val date: LocalDate, val previousStatus: CheckInStatus) : TodayEvent
}

class TodayViewModel(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val notificationHelper: NotificationHelper,
    private val clock: AppClock,
) : ViewModel() {

    private data class Capabilities(val notificationsEnabled: Boolean, val exactAlarmsAllowed: Boolean)

    private data class Inputs(
        val today: LocalDate,
        val goals: List<Goal>,
        val checkIns: List<CheckIn>,
        val settings: AppSettings,
    )

    private val capabilities = MutableStateFlow(readCapabilities())

    private val eventChannel = Channel<TodayEvent>(Channel.BUFFERED)
    val events: Flow<TodayEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<TodayUiState> = combine(
        combine(
            clock.todayFlow(),
            goalRepository.observeActiveGoals(),
            goalRepository.observeAllCheckIns(),
            settingsRepository.settings,
        ) { today, goals, checkIns, settings -> Inputs(today, goals, checkIns, settings) },
        capabilities,
    ) { inputs, caps ->
        buildTodayUiState(
            today = inputs.today,
            goals = inputs.goals,
            checkIns = inputs.checkIns,
            settings = inputs.settings,
            notificationsEnabled = caps.notificationsEnabled,
            exactAlarmsAllowed = caps.exactAlarmsAllowed,
            zone = clock.zone(),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Re-reads the notification/exact-alarm capability checks; call on resume. */
    fun refreshCapabilities() {
        capabilities.value = readCapabilities()
    }

    fun notificationSettingsIntent(): Intent = notificationHelper.appNotificationSettingsIntent()

    fun exactAlarmSettingsIntent(): Intent? = reminderScheduler.exactAlarmSettingsIntent()

    fun dismissExactAlarmHint() {
        viewModelScope.launch { settingsRepository.setExactAlarmHintDismissed(true) }
    }

    /** Marks today DONE for [goalId] (works from pending, or as a bonus check-in on a rest day). */
    fun checkIn(goalId: Long) {
        val state = uiState.value
        val row = state.priorityQueue.find { it.summary.goal.id == goalId }
            ?: state.restDay.find { it.summary.goal.id == goalId }
            ?: return
        val newStreak = row.summary.stats.current + 1
        val title = row.summary.goal.title
        viewModelScope.launch {
            goalRepository.setCheckIn(goalId, clock.today(), CheckInStatus.DONE)
            if (StreakCalculator.MILESTONES.contains(newStreak)) {
                eventChannel.send(TodayEvent.Milestone(NudgeMessages.milestone(title, newStreak)))
            }
        }
    }

    /** Removes today's check-in (DONE or SKIPPED), offering an undo. Used by the pill and the "Clear today" menu item. */
    fun removeCheckIn(goalId: Long) {
        val row = uiState.value.priorityQueue.find { it.summary.goal.id == goalId } ?: return
        val previousStatus = when (row.summary.todayStatus) {
            DayStatus.DONE -> CheckInStatus.DONE
            DayStatus.SKIPPED -> CheckInStatus.SKIPPED
            else -> return
        }
        val date = clock.today()
        viewModelScope.launch {
            goalRepository.setCheckIn(goalId, date, null)
            eventChannel.send(TodayEvent.CheckInRemoved(goalId, date, previousStatus))
        }
    }

    /** "Skip today (keeps streak)" menu action. */
    fun skipToday(goalId: Long) {
        viewModelScope.launch { goalRepository.setCheckIn(goalId, clock.today(), CheckInStatus.SKIPPED) }
    }

    fun undoRemove(goalId: Long, date: LocalDate, previousStatus: CheckInStatus) {
        viewModelScope.launch { goalRepository.setCheckIn(goalId, date, previousStatus) }
    }

    private fun readCapabilities() = Capabilities(
        notificationsEnabled = notificationHelper.areNotificationsEnabled(),
        exactAlarmsAllowed = reminderScheduler.canScheduleExactAlarms(),
    )
}

/** Pure reduction of Today's inputs into display state (shared by [TodayViewModel] and screenshot tests). */
internal fun buildTodayUiState(
    today: LocalDate,
    goals: List<Goal>,
    checkIns: List<CheckIn>,
    settings: AppSettings,
    notificationsEnabled: Boolean,
    exactAlarmsAllowed: Boolean,
    zone: ZoneId,
): TodayUiState {
    val rows = summarize(goals, checkIns, today).map { summary ->
        val doneAt = if (summary.todayStatus == DayStatus.DONE) {
            checkIns.firstOrNull { it.goalId == summary.goal.id && it.date == today }
                ?.let { LocalDateTime.ofInstant(it.createdAt, zone).toLocalTime() }
        } else {
            null
        }
        TodayGoalRow(summary, doneAt)
    }
    val priorityQueue = rows.filter { it.summary.isActionableToday }
    val restDay = rows.filter { it.summary.todayStatus == DayStatus.REST }

    val overall = InsightsCalculator.overall(goals, checkIns, today)
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sunday = monday.plusDays(6)
    val weekDays = InsightsCalculator.dailyTotals(goals, checkIns, monday, sunday, today).map { total ->
        WeekDayUi(
            date = total.date,
            progress = total.ratio,
            isToday = total.date == today,
            isFuture = total.date.isAfter(today),
        )
    }

    val anyAtRisk = priorityQueue.any { it.summary.stats.atRisk }
    val headline = headlineFor(
        hasActiveGoals = goals.isNotEmpty(),
        scheduledToday = overall.scheduledToday,
        doneToday = overall.doneToday,
        anyAtRisk = anyAtRisk,
    )

    return TodayUiState(
        loading = false,
        dateLabel = Formats.monoDate(today),
        headline = headline,
        weekDays = weekDays,
        todayStat = "${overall.doneToday}/${overall.scheduledToday}",
        topStreakStat = overall.topStreak.toString(),
        weekRateStat = Formats.percent(overall.weekRate),
        priorityQueue = priorityQueue,
        restDay = restDay,
        hasActiveGoals = goals.isNotEmpty(),
        showNotificationsBanner = !notificationsEnabled && goals.any { it.reminder.enabled },
        showExactAlarmBanner = !exactAlarmsAllowed && !settings.exactAlarmHintDismissed,
    )
}

private fun headlineFor(hasActiveGoals: Boolean, scheduledToday: Int, doneToday: Int, anyAtRisk: Boolean): String = when {
    !hasActiveGoals -> "Let's set a goal."
    scheduledToday == 0 -> "A quiet day. Recharge."
    doneToday >= scheduledToday -> "Everything's done. Nice."
    anyAtRisk -> "Keep the streaks lit."
    doneToday == 0 -> "Let's get moving."
    else -> "Keep it up."
}
