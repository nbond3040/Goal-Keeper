package com.goalkeeper.core.reminder

import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.ReminderConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One scheduled reminder notification. */
data class NudgePlan(
    val goalId: Long,
    /** The day the nudge is about. */
    val date: LocalDate,
    /** Local wall-clock time to fire. Always on [date]. */
    val at: LocalDateTime,
    /** 0-based position among that day's nudges. */
    val index: Int,
    /** Number of nudges that day. */
    val count: Int,
)

object ReminderPlanner {

    /** How many days ahead [nextNudge] searches (today + this many days). */
    const val LOOKAHEAD_DAYS = 8

    /**
     * The nudge times of one day for [config] (after [ReminderConfig.normalized]):
     * start, start + interval, … — `nudgeCount` of them — dropping any that would land at or after midnight.
     * Ascending; never empty (the start time itself is always before midnight).
     */
    fun nudgeTimes(config: ReminderConfig): List<LocalTime> {
        val normalized = config.normalized()
        val times = ArrayList<LocalTime>(normalized.nudgeCount)
        for (k in 0 until normalized.nudgeCount) {
            val minuteOfDay = normalized.startMinuteOfDay + k * normalized.intervalMinutes
            if (minuteOfDay < ReminderConfig.MINUTES_PER_DAY) {
                times.add(LocalTime.ofSecondOfDay(minuteOfDay * 60L))
            }
        }
        return times
    }

    /**
     * The first nudge strictly after [now] for [goal], or null when the goal is archived, its reminders are
     * disabled, or nothing is due within [LOOKAHEAD_DAYS]. A date is eligible when the goal's schedule includes it,
     * it is not before [Goal.startDate], and [isResolved] returns false for it (resolved = a DONE or SKIPPED
     * check-in exists for that date).
     */
    fun nextNudge(goal: Goal, now: LocalDateTime, isResolved: (LocalDate) -> Boolean): NudgePlan? {
        if (goal.archived || !goal.reminder.enabled) return null
        val times = nudgeTimes(goal.reminder)
        val today = now.toLocalDate()

        for (offset in 0..LOOKAHEAD_DAYS) {
            val date = today.plusDays(offset.toLong())
            if (!goal.schedule.isScheduled(date)) continue
            if (date.isBefore(goal.startDate)) continue
            if (isResolved(date)) continue

            for ((index, time) in times.withIndex()) {
                val at = LocalDateTime.of(date, time)
                if (at.isAfter(now)) {
                    return NudgePlan(goalId = goal.id, date = date, at = at, index = index, count = times.size)
                }
            }
            // Every nudge on this otherwise-eligible date has already passed; try the next date.
        }
        return null
    }

    /** The next occurrence of [minuteOfDay] (0..1439) strictly after [now]. */
    fun nextDailyTime(minuteOfDay: Int, now: LocalDateTime): LocalDateTime {
        val time = LocalTime.ofSecondOfDay(minuteOfDay * 60L)
        val todayAt = LocalDateTime.of(now.toLocalDate(), time)
        return if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    }
}
