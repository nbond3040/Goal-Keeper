package com.goalkeeper.core.insights

import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.streak.StreakCalculator
import java.time.DayOfWeek
import java.time.LocalDate

/** Totals for one calendar day across several goals. */
data class DailyTotal(val date: LocalDate, val done: Int, val scheduled: Int) {
    /** done / scheduled clamped to 0..1; 0 when nothing is scheduled. */
    val ratio: Float get() = if (scheduled <= 0) 0f else (done.toFloat() / scheduled).coerceIn(0f, 1f)
}

/** Headline numbers across all given goals. */
data class OverallStats(
    val activeGoals: Int,
    val doneToday: Int,
    val scheduledToday: Int,
    /** Highest current streak among the goals, and whose it is. */
    val topStreak: Int,
    val topStreakGoalId: Long?,
    /** done / (done + missed) over the last 7 days (today inclusive, pending today excluded); null if nothing to rate. */
    val weekRate: Float?,
    val totalCheckIns: Int,
    /** Highest best-ever streak among the goals. */
    val bestEverStreak: Int,
)

object InsightsCalculator {

    /**
     * One [DailyTotal] per day from [from] to [to] inclusive (ascending), across [goals].
     * Per goal and day, using the StreakCalculator day status:
     * DONE -> done+1 and scheduled+1; MISSED or PENDING -> scheduled+1;
     * FUTURE on a scheduled day on/after the goal's start -> scheduled+1; everything else adds nothing.
     * [checkIns] may contain check-ins of any goal; ones for goals not in [goals] are ignored.
     */
    fun dailyTotals(goals: List<Goal>, checkIns: List<CheckIn>, from: LocalDate, to: LocalDate, today: LocalDate): List<DailyTotal> {
        if (from.isAfter(to)) return emptyList()
        val index = indexCheckIns(goals, checkIns)

        val result = ArrayList<DailyTotal>()
        var date = from
        while (!date.isAfter(to)) {
            var done = 0
            var scheduled = 0
            for (goal in goals) {
                val goalIndex = index.getValue(goal.id)
                when (StreakCalculator.statusOn(goal, goalIndex.map, goalIndex.effectiveStart, date, today)) {
                    DayStatus.DONE -> {
                        done++
                        scheduled++
                    }
                    DayStatus.MISSED, DayStatus.PENDING -> scheduled++
                    DayStatus.FUTURE -> if (goal.schedule.isScheduled(date) && !date.isBefore(goal.startDate)) scheduled++
                    DayStatus.SKIPPED, DayStatus.REST, DayStatus.BEFORE_START -> Unit
                }
            }
            result.add(DailyTotal(date, done, scheduled))
            date = date.plusDays(1)
        }
        return result
    }

    /** Headline stats as of [today]. Archived goals in [goals] are ignored. */
    fun overall(goals: List<Goal>, checkIns: List<CheckIn>, today: LocalDate): OverallStats {
        val activeGoals = goals.filterNot { it.archived }
        val checkInsByGoal = checkIns.groupBy { it.goalId }

        var topStreak = 0
        var topStreakGoalId: Long? = null
        var bestEverStreak = 0
        var totalCheckIns = 0

        // Ranked order first so that on a tie the goal that ranks first keeps topStreakGoalId.
        for (goal in activeGoals.sortedWith(Goal.RANKED)) {
            val stats = StreakCalculator.stats(goal, checkInsByGoal[goal.id].orEmpty(), today)
            if (topStreakGoalId == null || stats.current > topStreak) {
                topStreak = stats.current
                topStreakGoalId = goal.id
            }
            if (stats.best > bestEverStreak) bestEverStreak = stats.best
            totalCheckIns += stats.totalDone
        }

        val todayTotal = dailyTotals(activeGoals, checkIns, today, today, today).first()
        val weekRate = weekCompletionRate(activeGoals, checkIns, today)

        return OverallStats(
            activeGoals = activeGoals.size,
            doneToday = todayTotal.done,
            scheduledToday = todayTotal.scheduled,
            topStreak = topStreak,
            topStreakGoalId = topStreakGoalId,
            weekRate = weekRate,
            totalCheckIns = totalCheckIns,
            bestEverStreak = bestEverStreak,
        )
    }

    /**
     * Completion rate per weekday (Monday first, always 7 entries) over the last [weeks] weeks ending today:
     * done / (done + missed) for that weekday across [goals]; 0 when nothing to rate.
     */
    fun weekdayRates(goals: List<Goal>, checkIns: List<CheckIn>, today: LocalDate, weeks: Int = 12): List<Pair<DayOfWeek, Float>> {
        val index = indexCheckIns(goals, checkIns)
        val done = IntArray(7)
        val missed = IntArray(7)

        val from = today.minusDays(weeks.toLong() * 7 - 1)
        var date = from
        while (!date.isAfter(today)) {
            val slot = date.dayOfWeek.value - 1
            for (goal in goals) {
                val goalIndex = index.getValue(goal.id)
                when (StreakCalculator.statusOn(goal, goalIndex.map, goalIndex.effectiveStart, date, today)) {
                    DayStatus.DONE -> done[slot]++
                    DayStatus.MISSED -> missed[slot]++
                    else -> Unit
                }
            }
            date = date.plusDays(1)
        }

        return DayOfWeek.entries.mapIndexed { i, day ->
            val total = done[i] + missed[i]
            day to (if (total == 0) 0f else done[i].toFloat() / total)
        }
    }

    /** Per-goal check-in map and effective start, built once so day-by-day loops don't rescan check-ins. */
    private data class GoalIndex(val map: Map<LocalDate, CheckInStatus>, val effectiveStart: LocalDate)

    private fun indexCheckIns(goals: List<Goal>, checkIns: List<CheckIn>): Map<Long, GoalIndex> {
        val byGoal = checkIns.groupBy { it.goalId }
        return goals.associateBy(Goal::id) { goal ->
            val map = byGoal[goal.id]?.associate { it.date to it.status } ?: emptyMap()
            GoalIndex(map, StreakCalculator.effectiveStart(goal, map.keys))
        }
    }

    private fun weekCompletionRate(goals: List<Goal>, checkIns: List<CheckIn>, today: LocalDate): Float? {
        val index = indexCheckIns(goals, checkIns)
        var done = 0
        var missed = 0

        var date = today.minusDays(6)
        while (!date.isAfter(today)) {
            for (goal in goals) {
                val goalIndex = index.getValue(goal.id)
                when (StreakCalculator.statusOn(goal, goalIndex.map, goalIndex.effectiveStart, date, today)) {
                    DayStatus.DONE -> done++
                    DayStatus.MISSED -> missed++
                    else -> Unit
                }
            }
            date = date.plusDays(1)
        }

        val total = done + missed
        return if (total == 0) null else done.toFloat() / total
    }
}
