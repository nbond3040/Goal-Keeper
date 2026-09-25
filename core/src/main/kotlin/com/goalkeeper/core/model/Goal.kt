package com.goalkeeper.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * A long-term goal the user builds a streak on.
 *
 * Ordering in ranked lists: [importance] (highest first), then [rank] (lowest first), then [id].
 */
data class Goal(
    val id: Long = 0,
    val title: String,
    val why: String = "",
    val icon: GoalIcon = GoalIcon.TARGET,
    val importance: Importance = Importance.MEDIUM,
    val rank: Int = 0,
    val schedule: Schedule = Schedule.DAILY,
    val targetDate: LocalDate? = null,
    val reminder: ReminderConfig = ReminderConfig.defaultsFor(importance),
    /** First day that counts for streaks and stats (normally the creation date). */
    val startDate: LocalDate,
    val createdAt: Instant,
    val archived: Boolean = false,
    /** Set when the goal was archived because it was achieved. */
    val achievedAt: Instant? = null,
) {
    val isActive: Boolean get() = !archived

    companion object {
        /** Comparator for ranked lists: importance desc, rank asc, id asc. */
        val RANKED: Comparator<Goal> = compareByDescending<Goal> { it.importance.level }
            .thenBy { it.rank }
            .thenBy { it.id }
    }
}
