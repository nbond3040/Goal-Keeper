package com.goalkeeper.core.streak

import com.goalkeeper.core.doneRange
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Schedule
import com.goalkeeper.core.testCheckIn
import com.goalkeeper.core.testGoal
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreakCalculatorTest {

    // Fixed "today" throughout — 2026-09-25 is a Friday. Never LocalDate.now() in tests.
    private val today: LocalDate = LocalDate.of(2026, 9, 25)

    @Test
    fun `empty history leaves today pending with no streak`() {
        val goal = testGoal(startDate = today)
        val stats = StreakCalculator.stats(goal, emptyList(), today)

        assertEquals(DayStatus.PENDING, stats.today)
        assertEquals(0, stats.current)
        assertEquals(0, stats.best)
        assertEquals(0, stats.totalDone)
        assertNull(stats.completionRate30)
        assertFalse(stats.atRisk)
        assertEquals(3, stats.nextMilestone)
    }

    @Test
    fun `today pending keeps the streak built on prior days`() {
        val start = today.minusDays(5)
        val goal = testGoal(startDate = start)
        val checkIns = doneRange(goal.id, start, today.minusDays(1)) // 5 DONE days, today untouched

        val stats = StreakCalculator.stats(goal, checkIns, today)

        assertEquals(DayStatus.PENDING, stats.today)
        assertEquals(5, stats.current)
        assertEquals(5, stats.best)
        assertTrue(stats.atRisk)
    }

    @Test
    fun `today done counts toward the current streak`() {
        val start = today.minusDays(5)
        val goal = testGoal(startDate = start)
        val checkIns = doneRange(goal.id, start, today) // includes today

        val stats = StreakCalculator.stats(goal, checkIns, today)

        assertEquals(DayStatus.DONE, stats.today)
        assertEquals(6, stats.current)
        assertEquals(6, stats.best)
        assertFalse(stats.atRisk) // not PENDING, so never at risk
    }

    @Test
    fun `a missed day breaks current but best keeps the older run`() {
        val start = today.minusDays(9) // 9/16
        val goal = testGoal(startDate = start)
        val checkIns = buildList {
            addAll(doneRange(goal.id, start, today.minusDays(5))) // 9/16..9/20: 5-day run
            // 9/21 left unchecked -> MISSED, breaking the chain
            addAll(doneRange(goal.id, today.minusDays(3), today.minusDays(1))) // 9/22..9/24: 3-day run
            // today (9/25) left unchecked -> PENDING
        }

        val stats = StreakCalculator.stats(goal, checkIns, today)

        assertEquals(3, stats.current)
        assertEquals(5, stats.best)
        assertTrue(stats.best > stats.current)
    }

    @Test
    fun `skipped days are neutral and do not break or extend the chain`() {
        val start = today.minusDays(4) // 9/21
        val goal = testGoal(startDate = start)
        val checkIns = listOf(
            testCheckIn(goalId = goal.id, date = today.minusDays(4), status = CheckInStatus.DONE),
            testCheckIn(goalId = goal.id, date = today.minusDays(3), status = CheckInStatus.SKIPPED),
            testCheckIn(goalId = goal.id, date = today.minusDays(2), status = CheckInStatus.DONE),
            testCheckIn(goalId = goal.id, date = today.minusDays(1), status = CheckInStatus.DONE),
            // today left unchecked -> PENDING
        )

        val stats = StreakCalculator.stats(goal, checkIns, today)

        // The skip neither breaks the chain (current spans across it) nor is counted itself.
        assertEquals(3, stats.current)
        assertEquals(3, stats.best)
        assertEquals(3, stats.totalDone)
    }

    @Test
    fun `weekday schedule treats weekends as rest and the streak survives them`() {
        val start = LocalDate.of(2026, 9, 14) // Monday, two weeks of weekdays before today
        val goal = testGoal(startDate = start, schedule = Schedule.WEEKDAYS)
        val checkIns = buildList {
            addAll(doneRange(goal.id, start, LocalDate.of(2026, 9, 18))) // Mon-Fri done
            // Sat 9/19, Sun 9/20: no check-in, and unscheduled -> REST
            addAll(doneRange(goal.id, LocalDate.of(2026, 9, 21), today.minusDays(1))) // Mon-Thu done
            // today, Fri 9/25: no check-in -> PENDING
        }

        val stats = StreakCalculator.stats(goal, checkIns, today)

        assertEquals(
            DayStatus.REST,
            StreakCalculator.dayStatus(goal, checkIns.associate { it.date to it.status }, LocalDate.of(2026, 9, 19), today),
        )
        assertEquals(9, stats.current)
        assertEquals(9, stats.best)
        assertEquals(9, stats.totalDone)
    }

    @Test
    fun `a done check-in on an unscheduled day still extends the streak`() {
        val saturday = LocalDate.of(2026, 9, 19)
        val goal = testGoal(startDate = saturday, schedule = Schedule.WEEKDAYS)
        val checkIns = listOf(testCheckIn(goalId = goal.id, date = saturday, status = CheckInStatus.DONE))

        val status = StreakCalculator.dayStatus(goal, checkIns.associate { it.date to it.status }, saturday, saturday)
        assertEquals(DayStatus.DONE, status) // not REST, even though Saturday isn't scheduled

        val stats = StreakCalculator.stats(goal, checkIns, saturday)
        assertEquals(1, stats.current)
        assertEquals(1, stats.best)
    }

    @Test
    fun `a back-filled check-in before start date moves the effective start`() {
        val goal = testGoal(startDate = LocalDate.of(2026, 9, 20))
        val backfillDate = LocalDate.of(2026, 9, 15)
        val checkIns = listOf(testCheckIn(goalId = goal.id, date = backfillDate, status = CheckInStatus.DONE))
        val checkInMap = checkIns.associate { it.date to it.status }

        // Before the backfilled date: still BEFORE_START.
        assertEquals(DayStatus.BEFORE_START, StreakCalculator.dayStatus(goal, checkInMap, backfillDate.minusDays(1), today))
        // The backfilled date itself: DONE.
        assertEquals(DayStatus.DONE, StreakCalculator.dayStatus(goal, checkInMap, backfillDate, today))
        // Between the backfill and the "official" start date: now MISSED rather than BEFORE_START.
        assertEquals(DayStatus.MISSED, StreakCalculator.dayStatus(goal, checkInMap, backfillDate.plusDays(1), today))
        assertEquals(DayStatus.MISSED, StreakCalculator.dayStatus(goal, checkInMap, goal.startDate, today))

        val stats = StreakCalculator.stats(goal, checkIns, today)
        assertEquals(1, stats.totalDone) // the backfilled day counts
        assertEquals(0, stats.current) // but it's long since broken by the missed days after it
        assertEquals(1, stats.best)
    }

    @Test
    fun `future dated check-ins are ignored for totals and current streak`() {
        val start = today.minusDays(5)
        val goal = testGoal(startDate = start)
        val futureDate = today.plusDays(3)
        val checkIns = listOf(
            testCheckIn(goalId = goal.id, date = today.minusDays(1), status = CheckInStatus.DONE),
            testCheckIn(goalId = goal.id, date = today, status = CheckInStatus.DONE),
            testCheckIn(goalId = goal.id, date = futureDate, status = CheckInStatus.DONE),
        )
        val checkInMap = checkIns.associate { it.date to it.status }

        assertEquals(DayStatus.FUTURE, StreakCalculator.dayStatus(goal, checkInMap, futureDate, today))

        val stats = StreakCalculator.stats(goal, checkIns, today)
        assertEquals(2, stats.totalDone) // the future check-in is not counted
        assertEquals(2, stats.current)
        assertEquals(2, stats.best)
    }

    @Test
    fun `completionRate30 excludes pending, skipped and rest days`() {
        val start = today.minusDays(40)
        val goal = testGoal(startDate = start)
        val checkIns = buildList {
            addAll(doneRange(goal.id, today.minusDays(29), today.minusDays(20))) // 10 DONE
            for (offset in 19 downTo 15) {
                add(testCheckIn(goalId = goal.id, date = today.minusDays(offset.toLong()), status = CheckInStatus.SKIPPED)) // 5 SKIPPED
            }
            // days -14..-1 (14 days): left unchecked -> MISSED
            // today: left unchecked -> PENDING
        }

        val stats = StreakCalculator.stats(goal, checkIns, today)

        assertNotNull(stats.completionRate30)
        assertEquals(10f / 24f, stats.completionRate30!!, 0.0001f)
    }

    @Test
    fun `completionRate30 is null when there is nothing to rate`() {
        val goal = testGoal(startDate = today, schedule = Schedule.WEEKENDS) // today (Friday) is REST
        val stats = StreakCalculator.stats(goal, emptyList(), today)
        assertNull(stats.completionRate30)
    }

    @Test
    fun `atRisk is only true when today is pending with a live streak`() {
        val goalWithStreak = testGoal(id = 1, startDate = today.minusDays(2))
        val pendingStats = StreakCalculator.stats(
            goalWithStreak,
            doneRange(1, today.minusDays(2), today.minusDays(1)),
            today,
        )
        assertTrue(pendingStats.atRisk)

        val goalNoStreak = testGoal(id = 2, startDate = today)
        val freshStats = StreakCalculator.stats(goalNoStreak, emptyList(), today)
        assertFalse(freshStats.atRisk) // pending today, but current == 0

        val goalDoneToday = testGoal(id = 3, startDate = today.minusDays(2))
        val doneStats = StreakCalculator.stats(
            goalDoneToday,
            doneRange(3, today.minusDays(2), today),
            today,
        )
        assertFalse(doneStats.atRisk) // today isn't pending
    }

    @Test
    fun `nextMilestone boundaries`() {
        val zeroGoal = testGoal(id = 1, startDate = today)
        assertEquals(3, StreakCalculator.stats(zeroGoal, emptyList(), today).nextMilestone)

        val threeGoal = testGoal(id = 2, startDate = today.minusDays(2))
        val threeStats = StreakCalculator.stats(threeGoal, doneRange(2, today.minusDays(2), today), today)
        assertEquals(3, threeStats.current)
        assertEquals(7, threeStats.nextMilestone)

        val thousandGoal = testGoal(id = 3, startDate = today.minusDays(999))
        val thousandStats = StreakCalculator.stats(thousandGoal, doneRange(3, today.minusDays(999), today), today)
        assertEquals(1000, thousandStats.current)
        assertNull(thousandStats.nextMilestone)
    }

    @Test
    fun `weekProgress with a skipped day`() {
        // Week is Mon 2026-09-21 .. Sun 2026-09-27; today is Friday.
        val goal = testGoal(startDate = LocalDate.of(2026, 9, 1))
        val checkIns = listOf(
            testCheckIn(goalId = goal.id, date = LocalDate.of(2026, 9, 21), status = CheckInStatus.DONE), // Mon
            testCheckIn(goalId = goal.id, date = LocalDate.of(2026, 9, 22), status = CheckInStatus.DONE), // Tue
            testCheckIn(goalId = goal.id, date = LocalDate.of(2026, 9, 23), status = CheckInStatus.SKIPPED), // Wed
            testCheckIn(goalId = goal.id, date = LocalDate.of(2026, 9, 24), status = CheckInStatus.DONE), // Thu
            // Fri (today), Sat, Sun: no check-in
        )

        val progress = StreakCalculator.weekProgress(goal, checkIns, today)

        // denominator: Mon,Tue,Thu,Fri,Sat,Sun (Wed removed for being skipped) = 6; numerator: Mon,Tue,Thu = 3
        assertEquals(0.5f, progress, 0.0001f)
    }

    @Test
    fun `weekProgress when the start date falls mid week`() {
        // Week is Mon 2026-09-21 .. Sun 2026-09-27; goal starts mid-week on Wednesday.
        val start = LocalDate.of(2026, 9, 23)
        val goal = testGoal(startDate = start)
        val checkIns = listOf(
            testCheckIn(goalId = goal.id, date = LocalDate.of(2026, 9, 23), status = CheckInStatus.DONE), // Wed
            testCheckIn(goalId = goal.id, date = LocalDate.of(2026, 9, 24), status = CheckInStatus.DONE), // Thu
        )

        val progress = StreakCalculator.weekProgress(goal, checkIns, today)

        // Mon/Tue are before the start date and excluded; denominator = Wed,Thu,Fri,Sat,Sun = 5; numerator = 2
        assertEquals(0.4f, progress, 0.0001f)
    }

    @Test
    fun `weekProgress is 0 when the denominator is 0`() {
        // Goal doesn't start until next week, so every day of this week is before the effective start.
        val goal = testGoal(startDate = LocalDate.of(2026, 9, 28))
        assertEquals(0f, StreakCalculator.weekProgress(goal, emptyList(), today))
    }

    @Test
    fun `dayStatuses returns an ascending inclusive range`() {
        val start = today.minusDays(2)
        val goal = testGoal(startDate = start)
        val checkIns = listOf(
            testCheckIn(goalId = goal.id, date = start, status = CheckInStatus.DONE),
            testCheckIn(goalId = goal.id, date = start.plusDays(1), status = CheckInStatus.SKIPPED),
        )

        val statuses = StreakCalculator.dayStatuses(goal, checkIns, today.minusDays(3), today.plusDays(2), today)

        assertEquals(
            listOf(
                today.minusDays(3) to DayStatus.BEFORE_START,
                today.minusDays(2) to DayStatus.DONE,
                today.minusDays(1) to DayStatus.SKIPPED,
                today to DayStatus.PENDING,
                today.plusDays(1) to DayStatus.FUTURE,
                today.plusDays(2) to DayStatus.FUTURE,
            ),
            statuses,
        )
    }

    @Test
    fun `dayStatuses is empty when from is after to`() {
        val goal = testGoal(startDate = today)
        assertEquals(emptyList(), StreakCalculator.dayStatuses(goal, emptyList(), today, today.minusDays(1), today))
    }

    @Test
    fun `stats stays correct and fast over a long history`() {
        val start = today.minusDays(1499)
        val goal = testGoal(id = 1, startDate = start)
        val checkIns = buildList {
            addAll(doneRange(goal.id, start, today.minusDays(200))) // long early run
            // one gap day at today-199 -> MISSED, resets the run
            addAll(doneRange(goal.id, today.minusDays(198), today.minusDays(1))) // run up to yesterday
            // today left pending
        }

        val stats = StreakCalculator.stats(goal, checkIns, today)

        assertEquals(198, stats.current)
        assertEquals(1300, stats.best) // start..today-200 inclusive
        assertEquals(198 + 1300, stats.totalDone)
        assertTrue(stats.atRisk)
    }
}
