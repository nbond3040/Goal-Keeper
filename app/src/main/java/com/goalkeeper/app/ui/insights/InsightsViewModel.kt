package com.goalkeeper.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.ui.common.GoalSummary
import com.goalkeeper.app.ui.common.summarize
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.insights.InsightsCalculator
import com.goalkeeper.core.insights.OverallStats
import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.Goal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** How many Monday–Sunday weeks the consistency heatmap covers. */
private const val HEATMAP_WEEKS = 16

/** How many weeks the weekly check-ins bar chart covers. */
private const val BAR_WEEKS = 12

data class InsightsUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val hasGoals: Boolean = false,
    val overall: OverallStats = OverallStats(
        activeGoals = 0,
        doneToday = 0,
        scheduledToday = 0,
        topStreak = 0,
        topStreakGoalId = null,
        weekRate = null,
        totalCheckIns = 0,
        bestEverStreak = 0,
    ),
    /** [HEATMAP_WEEKS] * 7 ratios, column-major Monday–Sunday weeks, oldest first; null = nothing to show yet. */
    val heatmapRatios: List<Float?> = emptyList(),
    /** DONE count per week for the last [BAR_WEEKS] weeks, oldest first. */
    val weeklyDone: List<Float> = emptyList(),
    val weeklyRangeStart: LocalDate = LocalDate.now(),
    val weeklyRangeEnd: LocalDate = LocalDate.now(),
    val weekdayRates: List<Pair<DayOfWeek, Float>> = emptyList(),
    val leaderboard: List<GoalSummary> = emptyList(),
)

/** Combines active goals, every check-in and the rolling "today" into the Insights screen's numbers. */
class InsightsViewModel(
    goalRepository: GoalRepository,
    clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        goalRepository.observeActiveGoals(),
        goalRepository.observeAllCheckIns(),
        clock.todayFlow(),
    ) { goals, checkIns, today ->
        buildInsightsUiState(goals, checkIns, today)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
}

/** Pure reduction into the screen's numbers (shared by [InsightsViewModel] and screenshot tests). */
internal fun buildInsightsUiState(goals: List<Goal>, checkIns: List<CheckIn>, today: LocalDate): InsightsUiState {
    if (goals.isEmpty()) {
        return InsightsUiState(loading = false, today = today, hasGoals = false)
    }

    val overall = InsightsCalculator.overall(goals, checkIns, today)

    val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val heatmapTo = currentWeekMonday.plusDays(6)
    val heatmapFrom = currentWeekMonday.minusWeeks((HEATMAP_WEEKS - 1).toLong())
    val heatmapTotals = InsightsCalculator.dailyTotals(goals, checkIns, heatmapFrom, heatmapTo, today)
    val heatmapRatios = heatmapTotals.map { total ->
        when {
            total.date.isAfter(today) -> null
            total.scheduled == 0 -> 0f
            else -> total.ratio
        }
    }

    val barsFrom = currentWeekMonday.minusWeeks((BAR_WEEKS - 1).toLong())
    val barTotals = InsightsCalculator.dailyTotals(goals, checkIns, barsFrom, heatmapTo, today)
    val weeklyDone = barTotals.chunked(7).map { week -> week.sumOf { it.done }.toFloat() }

    val weekdayRates = InsightsCalculator.weekdayRates(goals, checkIns, today)

    val leaderboard = summarize(goals, checkIns, today)
        .sortedWith(compareByDescending<GoalSummary> { it.stats.current }.thenByDescending { it.stats.best })

    return InsightsUiState(
        loading = false,
        today = today,
        hasGoals = true,
        overall = overall,
        heatmapRatios = heatmapRatios,
        weeklyDone = weeklyDone,
        weeklyRangeStart = barsFrom,
        weeklyRangeEnd = currentWeekMonday,
        weekdayRates = weekdayRates,
        leaderboard = leaderboard,
    )
}
