package com.goalkeeper.app.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Wall-clock access. Reads the system time zone on every call so travel and DST changes are picked up
 * without restarting the app.
 */
open class AppClock {
    open fun zone(): ZoneId = ZoneId.systemDefault()

    open fun instant(): Instant = Instant.now()

    fun now(): LocalDateTime = LocalDateTime.ofInstant(instant(), zone())

    fun today(): LocalDate = now().toLocalDate()

    /** Emits today's date now and again shortly after every midnight. */
    fun todayFlow(): Flow<LocalDate> = flow {
        while (true) {
            emit(today())
            val now = now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            // Wake a moment after midnight; cap the wait so time-zone changes are noticed within the hour.
            val wait = Duration.between(now, nextMidnight).plusSeconds(1).toMillis().coerceIn(1_000L, 3_600_000L)
            delay(wait)
        }
    }.distinctUntilChanged()
}
