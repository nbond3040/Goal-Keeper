package com.goalkeeper.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.GoalSummary
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.components.EmptyState
import com.goalkeeper.app.ui.components.FloatingNavClearance
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.GoalIconTile
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SegmentedTabs
import com.goalkeeper.app.ui.components.SignalBars
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.Schedule
import com.goalkeeper.core.streak.StreakStats
import java.time.Instant
import java.time.LocalDate

/** The Goals list: Active goals grouped by importance tier, and an Archived tab. */
@Composable
fun GoalsScreen(
    onOpenGoal: (Long) -> Unit,
    onOpenRank: () -> Unit,
    onCreateGoal: () -> Unit,
    viewModel: GoalsViewModel = viewModel(
        factory = gkViewModelFactory { c -> GoalsViewModel(c.goalRepository, c.clock) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    GoalsContent(
        state = state,
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        onOpenGoal = onOpenGoal,
        onOpenRank = onOpenRank,
        onCreateGoal = onCreateGoal,
        onRestore = viewModel::restore,
    )
}

@Composable
internal fun GoalsContent(
    state: GoalsUiState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onOpenRank: () -> Unit,
    onCreateGoal: () -> Unit,
    onRestore: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(GkTheme.colors.background)) {
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingNavClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    GoalsHeader(
                        activeCount = state.active.size,
                        archivedCount = state.archived.size,
                        onOpenRank = onOpenRank,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item {
                    SegmentedTabs(
                        options = listOf("Active", "Archived"),
                        selectedIndex = selectedTab,
                        onSelect = onSelectTab,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                if (selectedTab == 0) {
                    if (state.active.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No active goals",
                                body = "Create a goal to start tracking a streak.",
                                actionLabel = "New goal",
                                onAction = onCreateGoal,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    } else {
                        for (tier in Importance.descending) {
                            val tierRows = state.active.filter { it.summary.goal.importance == tier }
                            if (tierRows.isEmpty()) continue
                            item {
                                TierHeader(importance = tier, modifier = Modifier.padding(horizontal = 20.dp))
                            }
                            items(tierRows, key = { it.summary.goal.id }) { row ->
                                ActiveGoalCard(
                                    row = row,
                                    onClick = { onOpenGoal(row.summary.goal.id) },
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                        }
                    }
                } else {
                    if (state.archived.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No archived goals",
                                body = "Goals you archive or complete land here.",
                                icon = GkIcons.Archive,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    } else {
                        items(state.archived, key = { it.goal.id }) { row ->
                            ArchivedGoalCard(
                                row = row,
                                onClick = { onOpenGoal(row.goal.id) },
                                onRestore = { onRestore(row.goal.id) },
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsHeader(
    activeCount: Int,
    archivedCount: Int,
    onOpenRank: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Goals", style = MaterialTheme.typography.headlineMedium, color = colors.text)
            MonoLabel(text = "$activeCount active · $archivedCount archived")
        }
        TextButton(onClick = onOpenRank) {
            Icon(GkIcons.DragHandle, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Rank", style = MaterialTheme.typography.labelLarge, color = colors.accentText)
        }
    }
}

@Composable
private fun TierHeader(importance: Importance, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoLabel(text = importance.label)
        SignalBars(importance = importance)
    }
}

@Composable
private fun ActiveGoalCard(row: ActiveGoalRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    GkCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoalIconTile(icon = row.summary.goal.icon)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = row.summary.goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.scheduleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.statsLine,
                    style = GkTheme.mono.meta,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row.targetLabel?.let { label ->
                    Text(
                        text = label,
                        style = GkTheme.mono.meta,
                        color = if (row.targetPassed) colors.danger else colors.accentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedGoalCard(
    row: ArchivedGoalRow,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    val achieved = row.goal.achievedAt != null
    GkCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoalIconTile(icon = row.goal.icon, container = colors.raised, tint = colors.muted)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = row.goal.title,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (achieved) {
                        Icon(
                            imageVector = GkIcons.Trophy,
                            contentDescription = "Achieved",
                            tint = colors.accentText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = if (achieved) "Achieved ${row.achievedDateLabel.orEmpty()}" else "Archived",
                    style = GkTheme.mono.meta,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onRestore) {
                Text("Restore", style = MaterialTheme.typography.labelLarge, color = colors.accentText)
            }
        }
    }
}

@Preview
@Composable
private fun GoalsScreenPreview() {
    GoalKeeperTheme {
        GoalsContent(
            state = fakeGoalsUiState(),
            selectedTab = 0,
            onSelectTab = {},
            onOpenGoal = {},
            onOpenRank = {},
            onCreateGoal = {},
            onRestore = {},
        )
    }
}

private fun fakeGoalsUiState(): GoalsUiState {
    val today = LocalDate.of(2026, 9, 25)
    val goal = Goal(
        id = 1,
        title = "Run a half marathon",
        icon = GoalIcon.RUN,
        importance = Importance.CRITICAL,
        schedule = Schedule.DAILY,
        targetDate = today.plusDays(199),
        startDate = today.minusDays(60),
        createdAt = Instant.now(),
    )
    val stats = StreakStats(
        current = 42,
        best = 58,
        totalDone = 200,
        completionRate30 = 0.92f,
        today = com.goalkeeper.core.model.DayStatus.PENDING,
        atRisk = false,
        nextMilestone = 50,
    )
    val row = ActiveGoalRow(
        summary = GoalSummary(goal, stats, weekProgress = 0.6f),
        scheduleLabel = "Every day",
        statsLine = "42d · best 58 · 92%",
        targetLabel = "Apr 12, 2027 · 199d left",
        targetPassed = false,
    )
    return GoalsUiState(loading = false, active = listOf(row), archived = emptyList())
}
