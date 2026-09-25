package com.goalkeeper.core

import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.model.Schedule
import java.time.Instant
import java.time.LocalDate

/**
 * Shared construction helpers for tests. Every test in this module uses a fixed `today`
 * (never [LocalDate.now]) so results are reproducible; these helpers just cut down on
 * boilerplate when building [Goal]s and [CheckIn]s with sensible defaults.
 */

/** An arbitrary fixed instant, used whenever a test doesn't care about the exact creation time. */
val FIXED_INSTANT: Instant = Instant.parse("2026-09-01T09:00:00Z")

fun testGoal(
    id: Long = 1,
    title: String = "Test goal",
    why: String = "",
    icon: GoalIcon = GoalIcon.TARGET,
    importance: Importance = Importance.MEDIUM,
    rank: Int = 0,
    schedule: Schedule = Schedule.DAILY,
    targetDate: LocalDate? = null,
    reminder: ReminderConfig = ReminderConfig.defaultsFor(importance),
    startDate: LocalDate,
    createdAt: Instant = FIXED_INSTANT,
    archived: Boolean = false,
    achievedAt: Instant? = null,
): Goal = Goal(
    id = id,
    title = title,
    why = why,
    icon = icon,
    importance = importance,
    rank = rank,
    schedule = schedule,
    targetDate = targetDate,
    reminder = reminder,
    startDate = startDate,
    createdAt = createdAt,
    archived = archived,
    achievedAt = achievedAt,
)

fun testCheckIn(
    goalId: Long = 1,
    date: LocalDate,
    status: CheckInStatus,
    note: String? = null,
    createdAt: Instant = FIXED_INSTANT,
): CheckIn = CheckIn(goalId = goalId, date = date, status = status, note = note, createdAt = createdAt)

/** One DONE check-in per day from [from] to [to] inclusive. */
fun doneRange(goalId: Long, from: LocalDate, to: LocalDate): List<CheckIn> {
    val result = ArrayList<CheckIn>()
    var date = from
    while (!date.isAfter(to)) {
        result.add(testCheckIn(goalId = goalId, date = date, status = CheckInStatus.DONE))
        date = date.plusDays(1)
    }
    return result
}
