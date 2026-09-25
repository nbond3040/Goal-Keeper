package com.goalkeeper.core.reminder

import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.model.Schedule
import com.goalkeeper.core.testGoal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderPlannerTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25) // Friday

    // ---- nudgeTimes ----

    @Test
    fun `nudgeTimes lists start plus interval steps`() {
        val config = ReminderConfig(startMinuteOfDay = 8 * 60, nudgeCount = 3, intervalMinutes = 30)
        assertEquals(
            listOf(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0)),
            ReminderPlanner.nudgeTimes(config),
        )
    }

    @Test
    fun `nudgeTimes drops times that would land at or after midnight`() {
        val config = ReminderConfig(startMinuteOfDay = 22 * 60 + 30, nudgeCount = 4, intervalMinutes = 60)
        assertEquals(
            listOf(LocalTime.of(22, 30), LocalTime.of(23, 30)),
            ReminderPlanner.nudgeTimes(config),
        )
    }

    @Test
    fun `nudgeTimes normalizes an out-of-range config before computing`() {
        // startMinuteOfDay clamps to 1439, nudgeCount to 8, intervalMinutes to 15 -> only the clamped start survives.
        val config = ReminderConfig(startMinuteOfDay = 5000, nudgeCount = 99, intervalMinutes = 1)
        assertEquals(listOf(LocalTime.of(23, 59)), ReminderPlanner.nudgeTimes(config))
    }

    @Test
    fun `nudgeTimes clamps nudgeCount and intervalMinutes independently`() {
        // nudgeCount clamps from 20 to 8, intervalMinutes clamps from 500 to 240 (MAX_INTERVAL).
        val config = ReminderConfig(startMinuteOfDay = 8 * 60, nudgeCount = 20, intervalMinutes = 500)
        assertEquals(
            listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0)),
            ReminderPlanner.nudgeTimes(config),
        )
    }

    @Test
    fun `nudgeTimes is never empty`() {
        val config = ReminderConfig(startMinuteOfDay = 23 * 60 + 59, nudgeCount = 5, intervalMinutes = 60)
        assertEquals(listOf(LocalTime.of(23, 59)), ReminderPlanner.nudgeTimes(config))
    }

    // ---- nextNudge ----

    private val twoADayReminder = ReminderConfig(enabled = true, startMinuteOfDay = 8 * 60, nudgeCount = 2, intervalMinutes = 60)

    @Test
    fun `nextNudge picks the next time later today`() {
        val goal = testGoal(startDate = today.minusDays(30), reminder = twoADayReminder)
        val now = LocalDateTime.of(today, LocalTime.of(7, 0))

        val plan = ReminderPlanner.nextNudge(goal, now) { false }

        assertEquals(NudgePlan(goal.id, today, LocalDateTime.of(today, LocalTime.of(8, 0)), index = 0, count = 2), plan)
    }

    @Test
    fun `nextNudge rolls to the next day once today's nudges have passed`() {
        val goal = testGoal(startDate = today.minusDays(30), reminder = twoADayReminder)
        val now = LocalDateTime.of(today, LocalTime.of(9, 30)) // both 8:00 and 9:00 are in the past

        val plan = ReminderPlanner.nextNudge(goal, now) { false }

        val tomorrow = today.plusDays(1)
        assertEquals(NudgePlan(goal.id, tomorrow, LocalDateTime.of(tomorrow, LocalTime.of(8, 0)), index = 0, count = 2), plan)
    }

    @Test
    fun `nextNudge skips days that are already resolved`() {
        val goal = testGoal(startDate = today.minusDays(30), reminder = twoADayReminder)
        val now = LocalDateTime.of(today, LocalTime.of(7, 0))

        val plan = ReminderPlanner.nextNudge(goal, now) { date -> date == today }

        val tomorrow = today.plusDays(1)
        assertEquals(NudgePlan(goal.id, tomorrow, LocalDateTime.of(tomorrow, LocalTime.of(8, 0)), index = 0, count = 2), plan)
    }

    @Test
    fun `nextNudge skips unscheduled days`() {
        val goal = testGoal(startDate = today.minusDays(30), schedule = Schedule.WEEKDAYS, reminder = twoADayReminder)
        val saturday = LocalDate.of(2026, 9, 26)
        val now = LocalDateTime.of(saturday, LocalTime.of(7, 0))

        val plan = ReminderPlanner.nextNudge(goal, now) { false }

        val monday = LocalDate.of(2026, 9, 28)
        assertEquals(NudgePlan(goal.id, monday, LocalDateTime.of(monday, LocalTime.of(8, 0)), index = 0, count = 2), plan)
    }

    @Test
    fun `nextNudge respects the goal start date`() {
        val start = today.plusDays(2)
        val goal = testGoal(startDate = start, reminder = twoADayReminder)
        val now = LocalDateTime.of(today, LocalTime.of(7, 0))

        val plan = ReminderPlanner.nextNudge(goal, now) { false }

        assertEquals(NudgePlan(goal.id, start, LocalDateTime.of(start, LocalTime.of(8, 0)), index = 0, count = 2), plan)
    }

    @Test
    fun `nextNudge is null when the goal is archived`() {
        val goal = testGoal(startDate = today.minusDays(30), reminder = twoADayReminder, archived = true)
        assertNull(ReminderPlanner.nextNudge(goal, LocalDateTime.of(today, LocalTime.of(7, 0))) { false })
    }

    @Test
    fun `nextNudge is null when reminders are disabled`() {
        val goal = testGoal(startDate = today.minusDays(30), reminder = twoADayReminder.copy(enabled = false))
        assertNull(ReminderPlanner.nextNudge(goal, LocalDateTime.of(today, LocalTime.of(7, 0))) { false })
    }

    @Test
    fun `nextNudge is null when nothing is eligible within the lookahead window`() {
        val goal = testGoal(startDate = today.minusDays(30), reminder = twoADayReminder)
        val now = LocalDateTime.of(today, LocalTime.of(7, 0))

        val plan = ReminderPlanner.nextNudge(goal, now) { true } // every day already resolved

        assertNull(plan)
    }

    @Test
    fun `nextNudge treats now equal to a nudge time as already past`() {
        val goal = testGoal(startDate = today.minusDays(30), reminder = twoADayReminder)
        val now = LocalDateTime.of(today, LocalTime.of(8, 0)) // exactly the first nudge time

        val plan = ReminderPlanner.nextNudge(goal, now) { false }

        assertEquals(NudgePlan(goal.id, today, LocalDateTime.of(today, LocalTime.of(9, 0)), index = 1, count = 2), plan)
    }

    // ---- nextDailyTime ----

    @Test
    fun `nextDailyTime uses today when the time is still ahead`() {
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))
        assertEquals(LocalDateTime.of(today, LocalTime.of(10, 0)), ReminderPlanner.nextDailyTime(10 * 60, now))
    }

    @Test
    fun `nextDailyTime rolls to tomorrow when the time has passed`() {
        val now = LocalDateTime.of(today, LocalTime.of(11, 0))
        assertEquals(LocalDateTime.of(today.plusDays(1), LocalTime.of(10, 0)), ReminderPlanner.nextDailyTime(10 * 60, now))
    }

    @Test
    fun `nextDailyTime rolls to tomorrow when now equals the time exactly`() {
        val now = LocalDateTime.of(today, LocalTime.of(10, 0))
        assertEquals(LocalDateTime.of(today.plusDays(1), LocalTime.of(10, 0)), ReminderPlanner.nextDailyTime(10 * 60, now))
    }
}
