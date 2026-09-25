package com.goalkeeper.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.components.EmptyState
import com.goalkeeper.app.ui.components.FloatingNavClearance
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.StatTile
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.insights.OverallStats

/** Top-level insights dashboard: headline numbers, a 16-week heatmap, weekly bars, best days and a leaderboard. */
@Composable
fun InsightsScreen(
    onOpenGoal: (Long) -> Unit,
    viewModel: InsightsViewModel = viewModel(
        factory = gkViewModelFactory { c -> InsightsViewModel(c.goalRepository, c.clock) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsContent(state = state, onOpenGoal = onOpenGoal)
}

@Composable
internal fun InsightsContent(state: InsightsUiState, onOpenGoal: (Long) -> Unit) {
    val colors = GkTheme.colors
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingNavClearance

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { InsightsHeader() }
        if (!state.hasGoals) {
            item {
                EmptyState(
                    title = "No goals yet",
                    body = "Add a goal and start checking in — your streaks and trends will show up here.",
                    icon = GkIcons.Insights,
                )
            }
        } else {
            item { OverallStatsGrid(overall = state.overall) }
            item { ConsistencyCard(ratios = state.heatmapRatios) }
            item {
                WeeklyCheckInsCard(
                    weeklyDone = state.weeklyDone,
                    rangeStart = state.weeklyRangeStart,
                    rangeEnd = state.weeklyRangeEnd,
                )
            }
            item { BestDaysCard(rates = state.weekdayRates) }
            item { LeaderboardCard(leaderboard = state.leaderboard, onOpenGoal = onOpenGoal) }
        }
    }
}

@Composable
private fun InsightsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium, color = GkTheme.colors.text)
        MonoLabel("LAST 16 WEEKS")
    }
}

@Composable
private fun OverallStatsGrid(overall: OverallStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(value = "${overall.activeGoals}", label = "ACTIVE GOALS", modifier = Modifier.weight(1f))
            StatTile(value = "${overall.totalCheckIns}", label = "CHECK-INS", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                value = "${overall.bestEverStreak}",
                label = "BEST STREAK EVER",
                valueColor = GkTheme.colors.accentText,
                modifier = Modifier.weight(1f),
            )
            StatTile(value = Formats.percent(overall.weekRate), label = "7-DAY RATE", modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
private fun InsightsContentEmptyPreview() {
    GoalKeeperTheme {
        InsightsContent(state = InsightsUiState(loading = false, hasGoals = false), onOpenGoal = {})
    }
}
