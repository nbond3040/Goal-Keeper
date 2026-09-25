package com.goalkeeper.app.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.common.rememberUse24h
import com.goalkeeper.app.ui.components.EmptyState
import com.goalkeeper.app.ui.components.FloatingNavClearance
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SegmentedTabs
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import com.goalkeeper.core.model.Mood
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The global journal across every goal (top-level tab; floating nav sits over its bottom edge). */
@Composable
fun JournalFeedScreen(
    onOpenEntry: (Long, Long, JournalEntryType) -> Unit,
    onOpenGoal: (Long) -> Unit,
    viewModel: JournalFeedViewModel = viewModel(
        factory = gkViewModelFactory { c -> JournalFeedViewModel(c.goalRepository, c.journalRepository, c.clock) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(JournalFilter.ALL) }
    val use24h = rememberUse24h()

    JournalFeedContent(
        data = (uiState as? JournalFeedUiState.Ready)?.data,
        filter = filter,
        onSelectFilter = { filter = it },
        onOpenEntry = onOpenEntry,
        onOpenGoal = onOpenGoal,
        onTogglePin = viewModel::setPinned,
        onDeleteEntry = viewModel::deleteEntry,
        onSetItemDone = viewModel::setItemDone,
        use24h = use24h,
    )
}

@Composable
private fun JournalFeedContent(
    data: JournalFeedData?,
    filter: JournalFilter,
    onSelectFilter: (JournalFilter) -> Unit,
    onOpenEntry: (Long, Long, JournalEntryType) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onTogglePin: (JournalEntry) -> Unit,
    onDeleteEntry: (JournalEntry) -> Unit,
    onSetItemDone: (Long, Boolean) -> Unit,
    use24h: Boolean,
    modifier: Modifier = Modifier,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(GkTheme.colors.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = topInset + 16.dp,
            bottom = bottomInset + FloatingNavClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "header") {
            Column {
                Text("Journal", style = MaterialTheme.typography.headlineMedium, color = GkTheme.colors.text)
                if (data != null) {
                    Spacer(Modifier.height(4.dp))
                    MonoLabel(feedSubtitle(data.entryCount, data.goalCount))
                }
            }
        }
        item(key = "filter") {
            SegmentedTabs(
                options = JournalFilter.entries.map { it.label },
                selectedIndex = JournalFilter.entries.indexOf(filter),
                onSelect = { index -> onSelectFilter(JournalFilter.entries[index]) },
            )
        }
        if (data != null) {
            val filtered = data.entries.filter { filter.accepts(it) }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = if (data.entries.isEmpty()) "Your journal is empty" else "Nothing in ${filter.label.lowercase()}",
                        body = "Open a goal and use its Journal tab to log a thought, jot a note, or start a task list.",
                        icon = GkIcons.Journal,
                    )
                }
            } else {
                val rows = buildJournalRows(filtered, data.today, data.zone)
                journalTimeline(
                    rows = rows,
                    zone = data.zone,
                    use24h = use24h,
                    onOpen = { entry -> onOpenEntry(entry.goalId, entry.id, entry.type) },
                    onTogglePin = onTogglePin,
                    onDelete = onDeleteEntry,
                    onSetItemDone = onSetItemDone,
                    goalChipFor = { entry -> data.goalChips[entry.goalId] },
                    onOpenGoal = onOpenGoal,
                )
            }
        }
    }
}

private fun feedSubtitle(entryCount: Int, goalCount: Int): String {
    val entryWord = if (entryCount == 1) "entry" else "entries"
    val goalWord = if (goalCount == 1) "goal" else "goals"
    return "$entryCount $entryWord · $goalCount $goalWord"
}

@Preview
@Composable
private fun JournalFeedContentPreview() {
    GoalKeeperTheme {
        JournalFeedContent(
            data = fakeJournalFeedData(),
            filter = JournalFilter.ALL,
            onSelectFilter = {},
            onOpenEntry = { _, _, _ -> },
            onOpenGoal = {},
            onTogglePin = {},
            onDeleteEntry = {},
            onSetItemDone = { _, _ -> },
            use24h = true,
        )
    }
}

private fun fakeJournalFeedData(): JournalFeedData {
    val today = LocalDate.of(2026, 9, 25)
    val entries = listOf(
        JournalEntry(
            id = 1,
            goalId = 1,
            type = JournalEntryType.ENTRY,
            body = "Heavy legs for the first two miles, then negative-split the last three.",
            mood = Mood.STRONG,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
        JournalEntry(
            id = 2,
            goalId = 2,
            type = JournalEntryType.NOTE,
            title = "Book list",
            body = "Atomic Habits, Deep Work, The Pragmatic Programmer",
            createdAt = Instant.now().minusSeconds(86_400),
            updatedAt = Instant.now().minusSeconds(86_400),
        ),
    )
    return JournalFeedData(
        entries = entries,
        entryCount = entries.size,
        goalCount = 2,
        goalChips = mapOf(
            1L to GoalChipInfo(1, GoalIcon.RUN, "Run a half marathon"),
            2L to GoalChipInfo(2, GoalIcon.BOOK, "Read more"),
        ),
        zone = ZoneId.systemDefault(),
        today = today,
    )
}
