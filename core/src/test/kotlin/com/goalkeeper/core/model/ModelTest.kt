package com.goalkeeper.core.model

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelTest {

    @Test
    fun `toggling a day adds and removes it`() {
        val weekdays = Schedule.WEEKDAYS
        val withSaturday = weekdays.toggle(DayOfWeek.SATURDAY)
        assertTrue(withSaturday.includes(DayOfWeek.SATURDAY))
        assertEquals(6, withSaturday.daysPerWeek)
        assertFalse(withSaturday.toggle(DayOfWeek.SATURDAY).includes(DayOfWeek.SATURDAY))
    }

    @Test
    fun `removing the last remaining day keeps the schedule unchanged`() {
        val mondayOnly = Schedule.of(listOf(DayOfWeek.MONDAY))
        val toggled = mondayOnly.toggle(DayOfWeek.MONDAY)
        assertEquals(mondayOnly, toggled)
        assertEquals(listOf(DayOfWeek.MONDAY), toggled.days)
        assertFalse(toggled.isDaily)
    }

    @Test
    fun `empty mask is treated as daily`() {
        assertTrue(Schedule(0).isDaily)
        assertEquals(7, Schedule(0).daysPerWeek)
    }

    @Test
    fun `start time is clamped into the day`() {
        assertEquals(LocalTime.of(18, 30), ReminderConfig(startMinuteOfDay = 18 * 60 + 30).startTime)
        assertEquals(LocalTime.of(23, 59), ReminderConfig(startMinuteOfDay = 1500).startTime)
        assertEquals(LocalTime.MIDNIGHT, ReminderConfig(startMinuteOfDay = -5).startTime)
    }

    @Test
    fun `importance defaults escalate nudges`() {
        assertEquals(listOf(1, 2, 3, 4), Importance.entries.map { ReminderConfig.defaultsFor(it).nudgeCount })
        assertEquals(Importance.CRITICAL, Importance.descending.first())
    }
}
