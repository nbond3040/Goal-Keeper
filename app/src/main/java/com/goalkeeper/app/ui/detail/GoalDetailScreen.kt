package com.goalkeeper.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.common.rememberUse24h
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIconButton
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SegmentedTabs
import com.goalkeeper.app.ui.components.StatTile
import com.goalkeeper.app.ui.journal.JournalFilter
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.model.Schedule
import com.goalkeeper.core.streak.StreakStats
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Goal detail: header, streak panel, stat grid, then an Overview / Journal segmented view.
 * [goalId] comes from [com.goalkeeper.app.ui.navigation.GoalDetailRoute] via the ViewModel's SavedStateHandle.
 */
@Composable
fun GoalDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenEntry: (Long, Long, JournalEntryType) -> Unit,
    viewModel: GoalDetailViewModel = viewModel(
        factory = gkViewModelFactory { c -> GoalDetailViewModel(createSavedStateHandle(), c.goalRepository, c.journalRepository, c.clock) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goalId = viewModel.goalId
    val data = (uiState as? GoalDetailUiState.Ready)?.data

    // The goal can vanish while this screen is open (deleted here or elsewhere) — leave, once.
    var leftOnNotFound by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (uiState is GoalDetailUiState.NotFound && !leftOnNotFound) {
            leftOnNotFound = true
            onBack()
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(if (viewModel.startOnJournalTab) 1 else 0) }
    var journalFilter by rememberSaveable { mutableStateOf(JournalFilter.ALL) }
    var composerText by rememberSaveable { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val use24h = rememberUse24h()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GoalDetailEvent.Milestone -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = GkTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            GkTopBar(
                title = data?.goal?.title,
                onBack = onBack,
                actions = {
                    if (data != null) {
                        GkIconButton(icon = GkIcons.Edit, contentDescription = "Edit goal", onClick = { onEdit(goalId) })
                        Box {
                            GkIconButton(icon = GkIcons.More, contentDescription = "More options", onClick = { menuExpanded = true })
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                if (!data.goal.archived) {
                                    DropdownMenuItem(
                                        text = { Text("Mark achieved") },
                                        leadingIcon = { Icon(GkIcons.Trophy, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.setArchived(archived = true, achieved = true)
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (data.goal.archived) "Restore" else "Archive") },
                                    leadingIcon = {
                                        Icon(if (data.goal.archived) GkIcons.Unarchive else GkIcons.Archive, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.setArchived(archived = !data.goal.archived)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = GkTheme.colors.danger) },
                                    leadingIcon = { Icon(GkIcons.Delete, contentDescription = null, tint = GkTheme.colors.danger) },
                                    onClick = { menuExpanded = false; confirmDelete = true },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (data != null) {
                GoalDetailContent(
                    data = data,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    journalFilter = journalFilter,
                    onSelectJournalFilter = { journalFilter = it },
                    composerText = composerText,
                    onComposerTextChange = { composerText = it },
                    onSendQuickEntry = {
                        viewModel.saveQuickEntry(composerText)
                        composerText = ""
                    },
                    onCheckIn = viewModel::checkIn,
                    onToggleDay = viewModel::toggleDay,
                    onEditGoal = { onEdit(goalId) },
                    onOpenEntry = { entryId, type -> onOpenEntry(goalId, entryId, type) },
                    onTogglePin = viewModel::setPinned,
                    onDeleteEntry = viewModel::deleteEntry,
                    onSetItemDone = viewModel::setItemDone,
                    use24h = use24h,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (confirmDelete && data != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${data.goal.title}\"?") },
            text = { Text("This deletes its check-ins and journal too. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                    Text("Delete", color = GkTheme.colors.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GoalDetailContent(
    data: GoalDetailData,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    journalFilter: JournalFilter,
    onSelectJournalFilter: (JournalFilter) -> Unit,
    composerText: String,
    onComposerTextChange: (String) -> Unit,
    onSendQuickEntry: () -> Unit,
    onCheckIn: (CheckInStatus?) -> Unit,
    onToggleDay: (LocalDate) -> Unit,
    onEditGoal: () -> Unit,
    onOpenEntry: (Long, JournalEntryType) -> Unit,
    onTogglePin: (JournalEntry) -> Unit,
    onDeleteEntry: (JournalEntry) -> Unit,
    onSetItemDone: (Long, Boolean) -> Unit,
    use24h: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 6.dp,
                bottom = if (selectedTab == 1) 104.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "header") { GoalHeader(data.goal) }
            item(key = "streak") { StreakPanel(data) }
            item(key = "stats") { StatGrid(data) }
            item(key = "tabs") {
                SegmentedTabs(
                    options = listOf("Overview", "Journal · ${data.entryCount}"),
                    selectedIndex = selectedTab,
                    onSelect = onSelectTab,
                )
            }
            if (selectedTab == 0) {
                overviewSection(
                    data = data,
                    onCheckIn = { status ->
                        if (status == CheckInStatus.DONE) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCheckIn(status)
                    },
                    onToggleDay = onToggleDay,
                    onEditReminders = onEditGoal,
                    onOpenJournalTab = { onSelectTab(1) },
                    use24h = use24h,
                )
            } else {
                journalSection(
                    data = data,
                    filter = journalFilter,
                    onSelectFilter = onSelectJournalFilter,
                    onOpenEntry = { entry -> onOpenEntry(entry.id, entry.type) },
                    onTogglePin = onTogglePin,
                    onDeleteEntry = onDeleteEntry,
                    onSetItemDone = onSetItemDone,
                    use24h = use24h,
                )
            }
        }
        if (selectedTab == 1) {
            JournalComposerBar(
                text = composerText,
                onTextChange = onComposerTextChange,
                onSend = onSendQuickEntry,
                onNewEntry = { onOpenEntry(0, JournalEntryType.ENTRY) },
                onNewNote = { onOpenEntry(0, JournalEntryType.NOTE) },
                onNewChecklist = { onOpenEntry(0, JournalEntryType.CHECKLIST) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun GoalHeader(goal: Goal, modifier: Modifier = Modifier) {
    Column(modifier) {
        MonoLabel(
            text = "${goal.importance.label} · ${scheduleLabel(goal.schedule)}",
            color = GkTheme.colors.accentText,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = goal.title,
            style = MaterialTheme.typography.headlineMedium,
            color = GkTheme.colors.text,
        )
    }
}

@Composable
private fun StreakPanel(data: GoalDetailData, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    val stats = data.stats
    GkCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                MonoLabel("Current streak")
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stats.current.toString(),
                        style = GkTheme.mono.hero,
                        color = colors.accentText,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "days",
                        style = GkTheme.mono.small.copy(fontSize = 16.sp),
                        color = colors.muted,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Box(
                modifier = Modifier.size(68.dp).clip(CircleShape).background(colors.accentContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(GkIcons.Flame, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(32.dp))
            }
        }
        val nextMilestone = stats.nextMilestone
        if (nextMilestone != null) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Next milestone: ${Formats.days(nextMilestone)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted,
                )
                Text(
                    text = "${stats.current}/$nextMilestone",
                    style = GkTheme.mono.small,
                    color = colors.text,
                )
            }
            Spacer(Modifier.height(8.dp))
            val progress = (stats.current.toFloat() / nextMilestone).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.raised),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.accent),
                )
            }
        }
    }
}

@Composable
private fun StatGrid(data: GoalDetailData, modifier: Modifier = Modifier) {
    val stats = data.stats
    val fourth = fourthStatTile(data)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(value = stats.best.toString(), label = "Best streak", modifier = Modifier.weight(1f))
            StatTile(value = Formats.percent(stats.completionRate30), label = "30-day rate", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(value = stats.totalDone.toString(), label = "Check-ins", modifier = Modifier.weight(1f))
            StatTile(value = fourth.first, label = fourth.second, modifier = Modifier.weight(1f))
        }
    }
}

/** The 4th stat tile: days to target (or "target passed") when a target date is set, else days active. */
private fun fourthStatTile(data: GoalDetailData): Pair<String, String> {
    val goal = data.goal
    val targetDate = goal.targetDate
    return if (targetDate != null) {
        val days = ChronoUnit.DAYS.between(data.today, targetDate)
        if (days >= 0) days.toString() to "Days to target" else (-days).toString() to "Target passed"
    } else {
        val active = ChronoUnit.DAYS.between(goal.startDate, data.today) + 1
        active.coerceAtLeast(1).toString() to "Days active"
    }
}

private fun scheduleLabel(schedule: Schedule): String = when {
    schedule.isDaily -> "Every day"
    schedule.effectiveMask == Schedule.WEEKDAYS.effectiveMask -> "Weekdays"
    schedule.effectiveMask == Schedule.WEEKENDS.effectiveMask -> "Weekends"
    else -> schedule.days.joinToString(" · ") { day ->
        day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
    }
}

@Preview
@Composable
private fun GoalDetailContentPreview() {
    GoalKeeperTheme {
        Box(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
            GoalDetailContent(
                data = fakeGoalDetailData(),
                selectedTab = 0,
                onSelectTab = {},
                journalFilter = JournalFilter.ALL,
                onSelectJournalFilter = {},
                composerText = "",
                onComposerTextChange = {},
                onSendQuickEntry = {},
                onCheckIn = {},
                onToggleDay = {},
                onEditGoal = {},
                onOpenEntry = { _, _ -> },
                onTogglePin = {},
                onDeleteEntry = {},
                onSetItemDone = { _, _ -> },
                use24h = true,
            )
        }
    }
}

private fun fakeGoalDetailData(): GoalDetailData {
    val today = LocalDate.of(2026, 9, 25)
    val goal = Goal(
        id = 1,
        title = "Run a half marathon",
        why = "I want to prove to myself I can finish something hard.",
        icon = GoalIcon.RUN,
        importance = Importance.CRITICAL,
        schedule = Schedule.DAILY,
        targetDate = today.plusDays(60),
        reminder = ReminderConfig.defaultsFor(Importance.CRITICAL),
        startDate = today.minusDays(90),
        createdAt = Instant.EPOCH,
    )
    val stats = StreakStats(
        current = 42,
        best = 58,
        totalDone = 211,
        completionRate30 = 0.92f,
        today = DayStatus.PENDING,
        atRisk = true,
        nextMilestone = 50,
    )
    val heatmap = List(112) { index ->
        when {
            index >= 105 -> DayStatus.PENDING
            index % 17 == 0 -> DayStatus.SKIPPED
            index % 23 == 0 -> DayStatus.MISSED
            else -> DayStatus.DONE
        }
    }
    return GoalDetailData(
        goal = goal,
        stats = stats,
        today = today,
        nowTime = LocalTime.of(19, 30),
        zone = ZoneId.systemDefault(),
        checkIns = emptyList(),
        checkInsByDate = emptyMap(),
        heatmapStatuses = heatmap,
        heatmapMonthLabels = listOf("JUN", "JUL", "AUG", "SEP"),
        entries = emptyList(),
        entryCount = 12,
    )
}
