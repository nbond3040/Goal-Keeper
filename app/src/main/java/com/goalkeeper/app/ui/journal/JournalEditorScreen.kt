package com.goalkeeper.app.ui.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.components.GkIconButton
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.JournalEntryType

/**
 * Create (a new entry id of 0) or edit a journal entry. The goal, entry id and entry type come from
 * [com.goalkeeper.app.ui.navigation.JournalEditorRoute] via the ViewModel's SavedStateHandle.
 */
@Composable
fun JournalEditorScreen(
    onBack: () -> Unit,
    viewModel: JournalEditorViewModel = viewModel(
        factory = gkViewModelFactory { c -> JournalEditorViewModel(createSavedStateHandle(), c.goalRepository, c.journalRepository) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var newItemText by rememberSaveable { mutableStateOf("") }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val ready = uiState as? JournalEditorUiState.Ready

    val requestBack: () -> Unit = {
        if (ready != null && ready.dirty) showUnsavedDialog = true else onBack()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                JournalEditorEvent.Saved, JournalEditorEvent.Deleted -> onBack()
            }
        }
    }

    BackHandler(onBack = requestBack)

    Scaffold(
        containerColor = GkTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            GkTopBar(
                title = ready?.let { editorTitle(it.isNew, it.type) },
                subtitle = ready?.goalTitle,
                onBack = requestBack,
                actions = {
                    if (ready != null) {
                        TextButton(onClick = { viewModel.save() }, enabled = ready.canSave) {
                            Text("Save", color = if (ready.canSave) GkTheme.colors.accentText else GkTheme.colors.subtle)
                        }
                        if (!ready.isNew) {
                            Box {
                                GkIconButton(
                                    icon = GkIcons.More,
                                    contentDescription = "More options",
                                    onClick = { menuExpanded = true },
                                )
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text(if (ready.pinned) "Unpin" else "Pin") },
                                        leadingIcon = { Icon(GkIcons.Pin, contentDescription = null) },
                                        onClick = { menuExpanded = false; viewModel.togglePinned() },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = GkTheme.colors.danger) },
                                        leadingIcon = { Icon(GkIcons.Delete, contentDescription = null, tint = GkTheme.colors.danger) },
                                        onClick = { menuExpanded = false; confirmDelete = true },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (ready != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                when (ready.type) {
                    JournalEntryType.ENTRY -> EntryFields(
                        mood = ready.mood,
                        body = ready.body,
                        onMoodTap = viewModel::toggleMood,
                        onBodyChange = viewModel::setBody,
                    )

                    JournalEntryType.NOTE -> NoteFields(
                        title = ready.title,
                        body = ready.body,
                        onTitleChange = viewModel::setTitle,
                        onBodyChange = viewModel::setBody,
                    )

                    JournalEntryType.CHECKLIST -> ChecklistFields(
                        title = ready.title,
                        items = ready.items,
                        newItemText = newItemText,
                        onTitleChange = viewModel::setTitle,
                        onNewItemTextChange = { newItemText = it },
                        onCommitNewItem = { viewModel.addItem(newItemText); newItemText = "" },
                        onItemTextChange = viewModel::updateItemText,
                        onItemDoneChange = viewModel::setItemDone,
                        onRemoveItem = viewModel::removeItem,
                        onMoveItem = viewModel::moveItem,
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Save changes?") },
            text = { Text("You have unsaved changes to this entry.") },
            confirmButton = {
                TextButton(onClick = { showUnsavedDialog = false; viewModel.save() }) {
                    Text("Save", color = GkTheme.colors.accentText)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showUnsavedDialog = false }) { Text("Keep editing") }
                    TextButton(onClick = { showUnsavedDialog = false; onBack() }) {
                        Text("Discard", color = GkTheme.colors.danger)
                    }
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                    Text("Delete", color = GkTheme.colors.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun editorTitle(isNew: Boolean, type: JournalEntryType): String {
    val noun = when (type) {
        JournalEntryType.ENTRY -> "log entry"
        JournalEntryType.NOTE -> "note"
        JournalEntryType.CHECKLIST -> "task list"
    }
    return if (isNew) "New $noun" else "Edit $noun"
}
