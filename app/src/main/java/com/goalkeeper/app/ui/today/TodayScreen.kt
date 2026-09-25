package com.goalkeeper.app.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.common.rememberUse24h
import com.goalkeeper.app.ui.components.CheckInPill
import com.goalkeeper.app.ui.components.EmptyState
import com.goalkeeper.app.ui.components.FloatingNavClearance
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIconButton
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.GoalRingIcon
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.PrimaryButton
import com.goalkeeper.app.ui.components.SecondaryButton
import com.goalkeeper.app.ui.components.SectionHeader
import com.goalkeeper.app.ui.components.SignalBars
import com.goalkeeper.app.ui.components.StatTile
import com.goalkeeper.app.ui.components.WeekDayUi
import com.goalkeeper.app.ui.components.WeekStrip
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.Schedule
import com.goalkeeper.core.streak.StreakStats
import com.goalkeeper.app.ui.common.GoalSummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The Today dashboard: header, week strip, headline stats, capability banners and the priority queue
 * of goals actionable today, followed by a rest-day section for goals not scheduled today.
 */
@Composable
fun TodayScreen(
    onOpenGoal: (Long) -> Unit,
    onOpenRank: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateGoal: () -> Unit,
    viewModel: TodayViewModel = viewModel(
        factory = gkViewModelFactory { c ->
            TodayViewModel(c.goalRepository, c.settingsRepository, c.reminderScheduler, c.notificationHelper, c.clock)
        },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshCapabilities()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.Milestone -> snackbarHostState.showSnackbar(event.message, withDismissAction = true)
                is TodayEvent.CheckInRemoved -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Check-in removed",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoRemove(event.goalId, event.date, event.previousStatus)
                    }
                }
            }
        }
    }

    TodayContent(
        state = state,
        onOpenGoal = onOpenGoal,
        onOpenRank = onOpenRank,
        onOpenSettings = onOpenSettings,
        onCreateGoal = onCreateGoal,
        onCheckInGoal = viewModel::checkIn,
        onRemoveCheckIn = viewModel::removeCheckIn,
        onSkipToday = viewModel::skipToday,
        onOpenNotificationSettings = { context.startActivity(viewModel.notificationSettingsIntent()) },
        onAllowExactAlarms = { viewModel.exactAlarmSettingsIntent()?.let { context.startActivity(it) } },
        onDismissExactAlarmHint = viewModel::dismissExactAlarmHint,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
internal fun TodayContent(
    state: TodayUiState,
    onOpenGoal: (Long) -> Unit,
    onOpenRank: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateGoal: () -> Unit,
    onCheckInGoal: (Long) -> Unit,
    onRemoveCheckIn: (Long) -> Unit,
    onSkipToday: (Long) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onAllowExactAlarms: () -> Unit,
    onDismissExactAlarmHint: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val onCheckIn: (Long) -> Unit = { id ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onCheckInGoal(id)
    }
    val onRemove: (Long) -> Unit = { id ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onRemoveCheckIn(id)
    }
    val onSkip: (Long) -> Unit = { id ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onSkipToday(id)
    }

    Box(modifier = modifier.fillMaxSize().background(GkTheme.colors.background)) {
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val use24h = rememberUse24h()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingNavClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    TodayHeader(
                        dateLabel = state.dateLabel,
                        headline = state.headline,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item {
                    WeekStrip(days = state.weekDays, modifier = Modifier.padding(horizontal = 14.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatTile(value = state.todayStat, label = "Today", modifier = Modifier.weight(1f))
                        StatTile(
                            value = state.topStreakStat,
                            label = "Top streak",
                            valueColor = GkTheme.colors.accentText,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(value = state.weekRateStat, label = "This week", modifier = Modifier.weight(1f))
                    }
                }
                if (state.showNotificationsBanner) {
                    item {
                        NotificationsBanner(
                            onOpenSettings = onOpenNotificationSettings,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                if (state.showExactAlarmBanner) {
                    item {
                        ExactAlarmBanner(
                            onAllow = onAllowExactAlarms,
                            onDismiss = onDismissExactAlarmHint,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                if (!state.hasActiveGoals) {
                    item {
                        EmptyState(
                            title = "Let's set a goal",
                            body = "Add your first goal to start building a streak.",
                            actionLabel = "New goal",
                            onAction = onCreateGoal,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                } else {
                    if (state.priorityQueue.isNotEmpty()) {
                        item {
                            SectionHeader(
                                label = "Priority queue",
                                actionLabel = "Re-rank",
                                onAction = onOpenRank,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                        item {
                            PriorityQueueCard(
                                rows = state.priorityQueue,
                                use24h = use24h,
                                onOpen = onOpenGoal,
                                onCheckIn = onCheckIn,
                                onRemove = onRemove,
                                onSkip = onSkip,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    if (state.restDay.isNotEmpty()) {
                        item {
                            SectionHeader(label = "Rest day", modifier = Modifier.padding(horizontal = 20.dp))
                        }
                        item {
                            RestDayCard(
                                rows = state.restDay,
                                use24h = use24h,
                                onOpen = onOpenGoal,
                                onCheckIn = onCheckIn,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    if (state.priorityQueue.isEmpty() && state.restDay.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Nothing due today",
                                body = "Your goals will show up here once they're scheduled.",
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingNavClearance),
        )
    }
}

@Composable
private fun TodayHeader(
    dateLabel: String,
    headline: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MonoLabel(text = dateLabel, color = colors.accentText)
            Text(text = headline, style = MaterialTheme.typography.headlineMedium, color = colors.text)
        }
        GkIconButton(
            icon = GkIcons.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
            container = colors.card,
            border = BorderStroke(1.dp, colors.border),
        )
    }
}

@Composable
private fun NotificationsBanner(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    GkCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(GkIcons.BellActive, contentDescription = null, tint = colors.accentText)
                Text("Notifications are off", style = MaterialTheme.typography.titleSmall, color = colors.text)
            }
            Text(
                text = "Nudges can't reach you until notifications are allowed for Goal Keeper.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            SecondaryButton(text = "Open notification settings", onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ExactAlarmBanner(onAllow: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    GkCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(GkIcons.Alarm, contentDescription = null, tint = colors.accentText)
                Text(
                    text = "Allow exact alarms so nudges arrive on time",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Dismiss", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Allow", onClick = onAllow, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PriorityQueueCard(
    rows: List<TodayGoalRow>,
    use24h: Boolean,
    onOpen: (Long) -> Unit,
    onCheckIn: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    GkCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = GkTheme.colors.border)
            val goalId = row.summary.goal.id
            TodayRowItem(
                row = row,
                dimmed = false,
                use24h = use24h,
                showMenu = true,
                onOpen = { onOpen(goalId) },
                onPillClick = {
                    if (row.summary.todayStatus == DayStatus.DONE || row.summary.todayStatus == DayStatus.SKIPPED) {
                        onRemove(goalId)
                    } else {
                        onCheckIn(goalId)
                    }
                },
                onSkip = { onSkip(goalId) },
                onClear = { onRemove(goalId) },
            )
        }
    }
}

@Composable
private fun RestDayCard(
    rows: List<TodayGoalRow>,
    use24h: Boolean,
    onOpen: (Long) -> Unit,
    onCheckIn: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    GkCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = GkTheme.colors.border)
            val goalId = row.summary.goal.id
            TodayRowItem(
                row = row,
                dimmed = true,
                use24h = use24h,
                showMenu = false,
                onOpen = { onOpen(goalId) },
                onPillClick = { onCheckIn(goalId) },
                onSkip = {},
                onClear = {},
            )
        }
    }
}

/** One goal row shared by the priority queue and the rest-day list. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodayRowItem(
    row: TodayGoalRow,
    dimmed: Boolean,
    use24h: Boolean,
    showMenu: Boolean,
    onOpen: () -> Unit,
    onPillClick: () -> Unit,
    onSkip: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        val interactionModifier = if (showMenu) {
            Modifier.combinedClickable(onClick = onOpen, onLongClick = { menuExpanded = true })
        } else {
            Modifier.clickable(onClick = onOpen)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (dimmed) it.alpha(0.55f) else it }
                .then(interactionModifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SignalBars(importance = row.summary.goal.importance)
            GoalRingIcon(icon = row.summary.goal.icon, progress = row.summary.weekProgress)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = row.summary.goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildMetaText(row, use24h, colors.accentText),
                    style = GkTheme.mono.meta,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CheckInPill(status = row.summary.todayStatus, onClick = onPillClick)
        }
        if (showMenu) {
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Skip today (keeps streak)") },
                    onClick = { menuExpanded = false; onSkip() },
                )
                DropdownMenuItem(
                    text = { Text("Clear today") },
                    onClick = { menuExpanded = false; onClear() },
                )
                DropdownMenuItem(
                    text = { Text("Open") },
                    onClick = { menuExpanded = false; onOpen() },
                )
            }
        }
    }
}

private fun buildMetaText(row: TodayGoalRow, use24h: Boolean, riskColor: Color): AnnotatedString {
    val stats = row.summary.stats
    val status = row.summary.todayStatus
    val isRestDay = status == DayStatus.REST
    return buildAnnotatedString {
        when {
            status == DayStatus.SKIPPED -> append("skipped today")
            stats.current == 0 -> append(if (isRestDay) "no streak yet" else "start a streak today")
            else -> {
                val streakText = "${stats.current}d streak"
                if (stats.atRisk) {
                    withStyle(SpanStyle(color = riskColor)) { append(streakText) }
                } else {
                    append(streakText)
                }
                when {
                    status == DayStatus.DONE ->
                        append(" · done ${row.doneAt?.let { Formats.time(it, use24h) } ?: "--:--"}")
                    // A rest-day goal has no nudge scheduled today, even if reminders are enabled.
                    !isRestDay && row.summary.goal.reminder.enabled ->
                        append(" · nudge ${Formats.time(row.summary.goal.reminder.startMinuteOfDay, use24h)}")
                }
            }
        }
    }
}

@Preview
@Composable
private fun TodayScreenPreview() {
    GoalKeeperTheme {
        TodayContent(
            state = fakeTodayUiState(),
            onOpenGoal = {},
            onOpenRank = {},
            onOpenSettings = {},
            onCreateGoal = {},
            onCheckInGoal = {},
            onRemoveCheckIn = {},
            onSkipToday = {},
            onOpenNotificationSettings = {},
            onAllowExactAlarms = {},
            onDismissExactAlarmHint = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

private fun fakeTodayUiState(): TodayUiState {
    val today = LocalDate.of(2026, 9, 25)
    val goal = Goal(
        id = 1,
        title = "Run a half marathon",
        icon = GoalIcon.RUN,
        importance = Importance.CRITICAL,
        schedule = Schedule.DAILY,
        startDate = today.minusDays(60),
        createdAt = Instant.now(),
    )
    val stats = StreakStats(
        current = 42,
        best = 58,
        totalDone = 200,
        completionRate30 = 0.92f,
        today = DayStatus.PENDING,
        atRisk = true,
        nextMilestone = 50,
    )
    val row = TodayGoalRow(GoalSummary(goal, stats, weekProgress = 0.6f), doneAt = null)
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = (0..6).map { offset ->
        val date = monday.plusDays(offset.toLong())
        WeekDayUi(date = date, progress = if (date.isAfter(today)) 0f else 0.7f, isToday = date == today, isFuture = date.isAfter(today))
    }
    return TodayUiState(
        loading = false,
        dateLabel = Formats.monoDate(today),
        headline = "Keep the streaks lit.",
        weekDays = weekDays,
        todayStat = "2/5",
        topStreakStat = "118",
        weekRateStat = "84%",
        priorityQueue = listOf(row),
        restDay = emptyList(),
        hasActiveGoals = true,
    )
}
