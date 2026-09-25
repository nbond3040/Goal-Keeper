package com.goalkeeper.app.ui.common

import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.streak.StreakCalculator
import com.goalkeeper.core.streak.StreakStats
import java.time.LocalDate

/** A goal together with its computed streak numbers as of a given day. */
data class GoalSummary(
    val goal: Goal,
    val stats: StreakStats,
    /** This week's progress (0..1), see [StreakCalculator.weekProgress]. */
    val weekProgress: Float,
) {
    val todayStatus: DayStatus get() = stats.today

    val isDoneToday: Boolean get() = stats.today == DayStatus.DONE

    /** True when today needs a check-in (pending) or already has one. */
    val isActionableToday: Boolean get() = stats.today != DayStatus.REST && stats.today != DayStatus.BEFORE_START
}

/** Builds summaries for [goals] (order preserved) from a flat list of check-ins of any goals. */
fun summarize(goals: List<Goal>, checkIns: List<CheckIn>, today: LocalDate): List<GoalSummary> {
    val byGoal = checkIns.groupBy { it.goalId }
    return goals.map { goal ->
        val own = byGoal[goal.id].orEmpty()
        GoalSummary(
            goal = goal,
            stats = StreakCalculator.stats(goal, own, today),
            weekProgress = StreakCalculator.weekProgress(goal, own, today),
        )
    }
}
