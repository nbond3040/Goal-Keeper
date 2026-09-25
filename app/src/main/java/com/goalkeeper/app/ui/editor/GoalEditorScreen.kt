package com.goalkeeper.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Create ([onSaved] with `isNew == true`) or edit a goal. `goalId` (0 = new) comes from
 * [com.goalkeeper.app.ui.navigation.GoalEditorRoute] via the saved state handle.
 */
@Composable
fun GoalEditorScreen(
    onBack: () -> Unit,
    onSaved: (Long, Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: GoalEditorViewModel = viewModel(
        factory = gkViewModelFactory { c ->
            GoalEditorViewModel(createSavedStateHandle(), c.goalRepository, c.settingsRepository, c.clock)
        },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GoalEditorEvent.Saved -> onSaved(event.goalId, event.isNew)
                GoalEditorEvent.Deleted -> onDeleted()
                GoalEditorEvent.Closed -> onBack()
            }
        }
    }

    val handleBack: () -> Unit = {
        if (state.dirty) showDiscardDialog = true else onBack()
    }
    BackHandler(onBack = handleBack)

    Scaffold(
        containerColor = GkTheme.colors.background,
        topBar = {
            GkTopBar(
                title = if (state.isNew) "New goal" else "Edit goal",
                onBack = handleBack,
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = !state.loading && !state.saving) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.titleMedium,
                            color = GkTheme.colors.accentText,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GkTheme.colors.accent)
            }
        } else {
            GoalEditorContent(
                state = state,
                onTitleChange = viewModel::updateTitle,
                onWhyChange = viewModel::updateWhy,
                onIconSelected = viewModel::selectIcon,
                onImportanceSelected = viewModel::selectImportance,
                onQuickSchedule = viewModel::selectQuickSchedule,
                onToggleDay = viewModel::toggleDay,
                onTargetDateSelected = viewModel::selectTargetDate,
                onRemindersEnabledChanged = viewModel::setRemindersEnabled,
                onReminderStartMinuteChanged = viewModel::setReminderStartMinute,
                onIncrementNudges = viewModel::incrementNudgeCount,
                onDecrementNudges = viewModel::decrementNudgeCount,
                onIntervalSelected = viewModel::selectInterval,
                onMarkAchieved = viewModel::markAchieved,
                onArchive = { viewModel.setArchived(true) },
                onRestore = { viewModel.setArchived(false) },
                onDelete = viewModel::delete,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to this goal haven't been saved.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) {
                    Text("Discard", color = GkTheme.colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
internal fun GoalEditorContent(
    state: GoalEditorUiState,
    onTitleChange: (String) -> Unit,
    onWhyChange: (String) -> Unit,
    onIconSelected: (GoalIcon) -> Unit,
    onImportanceSelected: (Importance) -> Unit,
    onQuickSchedule: (Int) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onTargetDateSelected: (LocalDate?) -> Unit,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    onReminderStartMinuteChanged: (Int) -> Unit,
    onIncrementNudges: () -> Unit,
    onDecrementNudges: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onMarkAchieved: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        GoalInfoSection(
            title = state.title,
            titleError = state.titleError,
            why = state.why,
            onTitleChange = onTitleChange,
            onWhyChange = onWhyChange,
        )
        GoalIconSection(selected = state.icon, onSelect = onIconSelected)
        GoalImportanceSection(selected = state.importance, onSelect = onImportanceSelected)
        GoalScheduleSection(scheduleMask = state.scheduleMask, onQuickSchedule = onQuickSchedule, onToggleDay = onToggleDay)
        GoalTargetDateSection(targetDate = state.targetDate, today = state.today, onDateSelected = onTargetDateSelected)
        GoalRemindersSection(
            reminder = state.reminder,
            onEnabledChanged = onRemindersEnabledChanged,
            onStartMinuteChanged = onReminderStartMinuteChanged,
            onIncrementNudges = onIncrementNudges,
            onDecrementNudges = onDecrementNudges,
            onIntervalSelected = onIntervalSelected,
        )
        if (!state.isNew) {
            GoalManageSection(
                title = state.title,
                archived = state.archived,
                onMarkAchieved = onMarkAchieved,
                onArchive = onArchive,
                onRestore = onRestore,
                onDelete = onDelete,
            )
        }
    }
}

@Preview
@Composable
private fun GoalEditorContentPreview() {
    GoalKeeperTheme {
        GoalEditorContent(
            state = GoalEditorUiState(loading = false, isNew = false, title = "Run a half marathon"),
            onTitleChange = {},
            onWhyChange = {},
            onIconSelected = {},
            onImportanceSelected = {},
            onQuickSchedule = {},
            onToggleDay = {},
            onTargetDateSelected = {},
            onRemindersEnabledChanged = {},
            onReminderStartMinuteChanged = {},
            onIncrementNudges = {},
            onDecrementNudges = {},
            onIntervalSelected = {},
            onMarkAchieved = {},
            onArchive = {},
            onRestore = {},
            onDelete = {},
        )
    }
}
