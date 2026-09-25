package com.goalkeeper.core.model

import java.time.Instant
import java.time.LocalDate

enum class CheckInStatus {
    /** The goal was worked on that day. Extends the streak. */
    DONE,

    /** An excused day (sick, travel…). Neither extends nor breaks the streak. */
    SKIPPED,
}

/** At most one check-in exists per goal per calendar day. */
data class CheckIn(
    val goalId: Long,
    val date: LocalDate,
    val status: CheckInStatus,
    val note: String? = null,
    val createdAt: Instant,
)

/** How a single calendar day looks for one goal. */
enum class DayStatus {
    /** Checked in as done. */
    DONE,

    /** Checked in as skipped (excused). */
    SKIPPED,

    /** A past scheduled day on/after the start date with no check-in. Breaks the streak. */
    MISSED,

    /** Today, scheduled, not checked in yet. Does not break the streak (yet). */
    PENDING,

    /** Not a scheduled day and no check-in. Never breaks the streak. */
    REST,

    /** After today. */
    FUTURE,

    /** Before the goal's start date with no check-in. */
    BEFORE_START,
}
