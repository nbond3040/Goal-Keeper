package com.goalkeeper.app.notifications

import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.model.Schedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Pure-JVM tests for the decision functions [shouldShowNudge] and [isDueForBriefing] extracted
 * from [ReminderScheduler]. No Android framework involved: these only touch plain `core` models.
 */
class ReminderSchedulerLogicTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25) // a Friday

    private fun goal(
        archived: Boolean = false,
        reminderEnabled: Boolean = true,
        schedule: Schedule = Schedule.DAILY,
        startDate: LocalDate = today.minusDays(30),
    ): Goal = Goal(
        id = 1L,
        title = "Test goal",
        schedule = schedule,
        reminder = ReminderConfig(enabled = reminderEnabled),
        startDate = startDate,
        createdAt = Instant.EPOCH,
        archived = archived,
    )

    // shouldShowNudge

    @Test
    fun `shows nudge for an active scheduled goal on an unresolved today`() {
        assertTrue(shouldShowNudge(goal(), today, today, resolved = false))
    }

    @Test
    fun `does not show nudge when the goal is archived`() {
        assertFalse(shouldShowNudge(goal(archived = true), today, today, resolved = false))
    }

    @Test
    fun `does not show nudge when reminders are disabled`() {
        assertFalse(shouldShowNudge(goal(reminderEnabled = false), today, today, resolved = false))
    }

    @Test
    fun `does not show nudge once today is already resolved`() {
        assertFalse(shouldShowNudge(goal(), today, today, resolved = true))
    }

    @Test
    fun `does not show a stale nudge whose date is not today`() {
        assertFalse(shouldShowNudge(goal(), today.minusDays(1), today, resolved = false))
        assertFalse(shouldShowNudge(goal(), today.plusDays(1), today, resolved = false))
    }

    // isDueForBriefing

    @Test
    fun `goal is due for briefing when scheduled today and not yet resolved`() {
        assertTrue(isDueForBriefing(goal(), today, resolvedToday = false))
    }

    @Test
    fun `goal is not due for briefing once resolved today`() {
        assertFalse(isDueForBriefing(goal(), today, resolvedToday = true))
    }

    @Test
    fun `goal is not due for briefing before its start date`() {
        assertFalse(isDueForBriefing(goal(startDate = today.plusDays(1)), today, resolvedToday = false))
    }

    @Test
    fun `goal is due for briefing exactly on its start date`() {
        assertTrue(isDueForBriefing(goal(startDate = today), today, resolvedToday = false))
    }

    @Test
    fun `goal is not due for briefing on a day outside its schedule`() {
        // 2026-09-25 is a Friday, so a weekend-only schedule has nothing due.
        assertFalse(isDueForBriefing(goal(schedule = Schedule.WEEKENDS), today, resolvedToday = false))
    }

    @Test
    fun `archived goal is never due for briefing`() {
        assertFalse(isDueForBriefing(goal(archived = true), today, resolvedToday = false))
    }
}
