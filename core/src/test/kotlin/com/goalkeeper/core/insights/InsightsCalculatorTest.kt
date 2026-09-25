package com.goalkeeper.core.insights

import com.goalkeeper.core.doneRange
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.Schedule
import com.goalkeeper.core.testCheckIn
import com.goalkeeper.core.testGoal
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InsightsCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25) // Friday

    @Test
    fun `dailyTotals counts future scheduled days and ignores check-ins for unknown goals`() {
        val goalA = testGoal(id = 1, startDate = today.minusDays(10))
        val goalB = testGoal(id = 2, startDate = today.minusDays(10))
        val checkIns = listOf(
            testCheckIn(goalId = 1, date = today.minusDays(1), status = CheckInStatus.DONE),
            // References a goal that isn't in the `goals` list passed below -- must be ignored, not crash.
            testCheckIn(goalId = 999, date = today, status = CheckInStatus.DONE),
        )

        val totals = InsightsCalculator.dailyTotals(
            goals = listOf(goalA, goalB),
            checkIns = checkIns,
            from = today.minusDays(1),
            to = today.plusDays(2),
            today = today,
        )

        assertEquals(
            listOf(
                DailyTotal(today.minusDays(1), done = 1, scheduled = 2), // goalA DONE, goalB MISSED
                DailyTotal(today, done = 0, scheduled = 2), // both PENDING; the unknown goal's check-in is ignored
                DailyTotal(today.plusDays(1), done = 0, scheduled = 2), // both FUTURE, scheduled, past their start
                DailyTotal(today.plusDays(2), done = 0, scheduled = 2),
            ),
            totals,
        )
    }

    @Test
    fun `dailyTotals only counts a future day as scheduled when it is on the goal's schedule and on or after its start`() {
        val weekdayGoal = testGoal(id = 1, startDate = today.minusDays(5), schedule = Schedule.WEEKDAYS)
        val notYetStartedGoal = testGoal(id = 2, startDate = today.plusDays(5))

        val totals = InsightsCalculator.dailyTotals(
            goals = listOf(weekdayGoal, notYetStartedGoal),
            checkIns = emptyList(),
            from = today,
            to = today.plusDays(3),
            today = today,
        )

        assertEquals(
            listOf(
                DailyTotal(today, done = 0, scheduled = 1), // Fri: weekdayGoal PENDING; notYetStarted is BEFORE_START
                DailyTotal(today.plusDays(1), done = 0, scheduled = 0), // Sat: unscheduled for both that matter
                DailyTotal(today.plusDays(2), done = 0, scheduled = 0), // Sun: same
                DailyTotal(today.plusDays(3), done = 0, scheduled = 1), // Mon: weekdayGoal FUTURE+scheduled+started
            ),
            totals,
        )
    }

    @Test
    fun `dailyTotals is empty when from is after to`() {
        val goal = testGoal(startDate = today)
        assertEquals(emptyList(), InsightsCalculator.dailyTotals(listOf(goal), emptyList(), today, today.minusDays(1), today))
    }

    @Test
    fun `overall ignores archived goals and breaks streak ties by ranked order`() {
        val archived = testGoal(id = 1, importance = Importance.CRITICAL, startDate = today.minusDays(100), archived = true)
        val lowerId = testGoal(id = 15, importance = Importance.HIGH, startDate = today.minusDays(5))
        val higherId = testGoal(id = 20, importance = Importance.HIGH, startDate = today.minusDays(5))

        val checkIns = doneRange(archived.id, today.minusDays(100), today.minusDays(1)) +
            doneRange(lowerId.id, today.minusDays(5), today.minusDays(1)) +
            doneRange(higherId.id, today.minusDays(5), today.minusDays(1))
        // Every goal is PENDING today (nothing checked in for today itself).

        val stats = InsightsCalculator.overall(listOf(archived, lowerId, higherId), checkIns, today)

        assertEquals(2, stats.activeGoals) // archived goal excluded
        assertEquals(5, stats.topStreak)
        assertEquals(15L, stats.topStreakGoalId) // tie between lowerId and higherId -> ranked order (lower id) wins
        assertEquals(5, stats.bestEverStreak) // archived goal's 100-day best is excluded
        assertEquals(10, stats.totalCheckIns) // 5 + 5, archived goal's 100 check-ins excluded
        assertEquals(0, stats.doneToday)
        assertEquals(2, stats.scheduledToday)
        assertNotNull(stats.weekRate)
        assertEquals(1f, stats.weekRate!!, 0.0001f)
    }

    @Test
    fun `overall handles no goals without crashing`() {
        val stats = InsightsCalculator.overall(emptyList(), emptyList(), today)
        assertEquals(0, stats.activeGoals)
        assertEquals(0, stats.topStreak)
        assertEquals(null, stats.topStreakGoalId)
        assertEquals(null, stats.weekRate)
        assertEquals(0, stats.totalCheckIns)
        assertEquals(0, stats.bestEverStreak)
    }

    @Test
    fun `weekdayRates always returns 7 entries in Monday-first order`() {
        val rates = InsightsCalculator.weekdayRates(emptyList(), emptyList(), today)
        assertEquals(7, rates.size)
        assertEquals(DayOfWeek.entries.toList(), rates.map { it.first })
        assertTrue(rates.all { it.second == 0f })
    }

    @Test
    fun `weekdayRates computes a rate per weekday over the requested window`() {
        val goal = testGoal(id = 1, startDate = today.minusDays(30))
        // Six consecutive DONE days ending yesterday; today itself is left pending.
        val checkIns = doneRange(goal.id, today.minusDays(6), today.minusDays(1))

        val rates = InsightsCalculator.weekdayRates(listOf(goal), checkIns, today, weeks = 1)

        val byDay = rates.toMap()
        assertEquals(7, rates.size)
        assertEquals(1f, byDay.getValue(DayOfWeek.SATURDAY)) // 2026-09-19, DONE
        assertEquals(1f, byDay.getValue(DayOfWeek.SUNDAY)) // 2026-09-20, DONE
        assertEquals(1f, byDay.getValue(DayOfWeek.MONDAY)) // 2026-09-21, DONE
        assertEquals(1f, byDay.getValue(DayOfWeek.TUESDAY)) // 2026-09-22, DONE
        assertEquals(1f, byDay.getValue(DayOfWeek.WEDNESDAY)) // 2026-09-23, DONE
        assertEquals(1f, byDay.getValue(DayOfWeek.THURSDAY)) // 2026-09-24, DONE
        assertEquals(0f, byDay.getValue(DayOfWeek.FRIDAY)) // 2026-09-25 == today, PENDING -> nothing to rate
    }
}
