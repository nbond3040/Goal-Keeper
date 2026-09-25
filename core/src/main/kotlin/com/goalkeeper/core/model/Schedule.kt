package com.goalkeeper.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The days of the week a goal should be worked on, stored as a 7-bit mask
 * (bit 0 = Monday … bit 6 = Sunday). An empty mask is treated as daily.
 */
data class Schedule(val mask: Int) {

    /** The effective mask: never empty, never wider than 7 bits. */
    val effectiveMask: Int get() = (mask and ALL_DAYS_MASK).takeIf { it != 0 } ?: ALL_DAYS_MASK

    fun includes(day: DayOfWeek): Boolean = effectiveMask and bit(day) != 0

    fun isScheduled(date: LocalDate): Boolean = includes(date.dayOfWeek)

    val days: List<DayOfWeek> get() = DayOfWeek.entries.filter(::includes)

    val isDaily: Boolean get() = effectiveMask == ALL_DAYS_MASK

    val daysPerWeek: Int get() = Integer.bitCount(effectiveMask)

    fun toggle(day: DayOfWeek): Schedule = Schedule(effectiveMask xor bit(day))

    companion object {
        const val ALL_DAYS_MASK = 0x7F

        val DAILY = Schedule(ALL_DAYS_MASK)
        val WEEKDAYS = of(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
        val WEEKENDS = of(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))

        fun of(days: Collection<DayOfWeek>): Schedule = Schedule(days.fold(0) { acc, day -> acc or bit(day) })

        private fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    }
}
