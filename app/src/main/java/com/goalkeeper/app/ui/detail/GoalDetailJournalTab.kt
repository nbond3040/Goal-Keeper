package com.goalkeeper.app.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.components.EmptyState
import com.goalkeeper.app.ui.components.GkIconButton
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.SegmentedTabs
import com.goalkeeper.app.ui.journal.JournalFilter
import com.goalkeeper.app.ui.journal.accepts
import com.goalkeeper.app.ui.journal.buildJournalRows
import com.goalkeeper.app.ui.journal.journalTimeline
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.JournalEntry

/** The Journal tab: All/Log/Tasks/Notes filter, then the shared rail timeline (or an empty state). */
internal fun LazyListScope.journalSection(
    data: GoalDetailData,
    filter: JournalFilter,
    onSelectFilter: (JournalFilter) -> Unit,
    onOpenEntry: (JournalEntry) -> Unit,
    onTogglePin: (JournalEntry) -> Unit,
    onDeleteEntry: (JournalEntry) -> Unit,
    onSetItemDone: (Long, Boolean) -> Unit,
    use24h: Boolean,
) {
    item(key = "journal-filter") {
        SegmentedTabs(
            options = JournalFilter.entries.map { it.label },
            selectedIndex = JournalFilter.entries.indexOf(filter),
            onSelect = { index -> onSelectFilter(JournalFilter.entries[index]) },
        )
    }

    val filtered = data.entries.filter { filter.accepts(it) }
    if (filtered.isEmpty()) {
        item(key = "journal-empty") {
            EmptyState(
                title = if (data.entries.isEmpty()) "Your journal is empty" else "Nothing in ${filter.label.lowercase()}",
                body = "Log a quick thought, jot a note, or start a task list — three ways to keep track of this goal.",
                icon = GkIcons.Journal,
            )
        }
    } else {
        val rows = buildJournalRows(filtered, data.today, data.zone)
        journalTimeline(
            rows = rows,
            zone = data.zone,
            use24h = use24h,
            onOpen = onOpenEntry,
            onTogglePin = onTogglePin,
            onDelete = onDeleteEntry,
            onSetItemDone = onSetItemDone,
        )
    }
}

/** Bottom composer bar: "+" menu (task list / note / log entry) plus a quick log-entry field and send button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalComposerBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onNewEntry: () -> Unit,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding(),
        shape = RoundedCornerShape(20.dp),
        color = colors.raised,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box {
                GkIconButton(
                    icon = GkIcons.Add,
                    contentDescription = "Add journal entry",
                    onClick = { menuExpanded = true },
                    container = colors.card,
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Detailed log entry") },
                        leadingIcon = { Icon(GkIcons.Entry, contentDescription = null) },
                        onClick = { menuExpanded = false; onNewEntry() },
                    )
                    DropdownMenuItem(
                        text = { Text("Note") },
                        leadingIcon = { Icon(GkIcons.Note, contentDescription = null) },
                        onClick = { menuExpanded = false; onNewNote() },
                    )
                    DropdownMenuItem(
                        text = { Text("Task list") },
                        leadingIcon = { Icon(GkIcons.Checklist, contentDescription = null) },
                        onClick = { menuExpanded = false; onNewChecklist() },
                    )
                }
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Log a thought…", color = colors.muted) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = colors.accent,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                ),
            )
            GkIconButton(
                icon = GkIcons.Send,
                contentDescription = "Send",
                onClick = { if (text.isNotBlank()) onSend() },
                container = colors.accent,
                tint = colors.onAccent,
            )
        }
    }
}
