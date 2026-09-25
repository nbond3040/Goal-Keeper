package com.goalkeeper.app.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Shared date/time formatting so every screen speaks the same way. */
object Formats {
    private val time24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val time12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    /** "Fri, Sep 25" */
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    /** "Sep 25, 2026" */
    val mediumDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    /** "September 2026" */
    val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    /** "FRI · 25 SEP" — the console-style date used in mono labels. */
    fun monoDate(date: LocalDate): String =
        DateTimeFormatter.ofPattern("EEE · dd MMM").format(date).uppercase()

    fun time(time: LocalTime, use24h: Boolean): String = (if (use24h) time24 else time12).format(time)

    fun time(minuteOfDay: Int, use24h: Boolean): String =
        time(LocalTime.of((minuteOfDay / 60).coerceIn(0, 23), (minuteOfDay % 60).coerceIn(0, 59)), use24h)

    /** "Today", "Yesterday", or a short date. */
    fun relativeDay(date: LocalDate, today: LocalDate): String = when (ChronoUnit.DAYS.between(date, today)) {
        0L -> "Today"
        1L -> "Yesterday"
        -1L -> "Tomorrow"
        else -> shortDate.format(date)
    }

    /** "1 day" / "12 days". */
    fun days(count: Int): String = if (count == 1) "1 day" else "$count days"

    /** Percent text for a 0..1 rate, or "—" when unknown. */
    fun percent(rate: Float?): String = if (rate == null) "—" else "${(rate * 100).toInt()}%"
}

/** Whether the user's device prefers 24-hour time. */
@Composable
fun rememberUse24h(): Boolean {
    val context = LocalContext.current
    return remember(context) { DateFormat.is24HourFormat(context) }
}
