package com.goalkeeper.core.model

import java.time.LocalTime

/**
 * Daily reminder settings for a goal. On scheduled days that are not yet checked in,
 * the app sends [nudgeCount] notifications starting at [startMinuteOfDay] and spaced
 * [intervalMinutes] apart. Nudges never spill past midnight.
 */
data class ReminderConfig(
    val enabled: Boolean = true,
    val startMinuteOfDay: Int = DEFAULT_START_MINUTE,
    val nudgeCount: Int = 1,
    val intervalMinutes: Int = 60,
) {
    val startTime: LocalTime get() = LocalTime.of((startMinuteOfDay / 60).coerceIn(0, 23), startMinuteOfDay % 60)

    /** Clamps every field into its supported range. */
    fun normalized(): ReminderConfig = copy(
        startMinuteOfDay = startMinuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1),
        nudgeCount = nudgeCount.coerceIn(1, MAX_NUDGES),
        intervalMinutes = intervalMinutes.coerceIn(MIN_INTERVAL, MAX_INTERVAL),
    )

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_START_MINUTE = 18 * 60
        const val MAX_NUDGES = 8
        const val MIN_INTERVAL = 15
        const val MAX_INTERVAL = 240
        val INTERVAL_CHOICES = listOf(15, 30, 45, 60, 90, 120, 180, 240)

        /** Default reminders for a new goal of the given importance. */
        fun defaultsFor(importance: Importance, startMinuteOfDay: Int = DEFAULT_START_MINUTE) = ReminderConfig(
            enabled = true,
            startMinuteOfDay = startMinuteOfDay,
            nudgeCount = importance.defaultNudges,
            intervalMinutes = 60,
        )
    }
}
