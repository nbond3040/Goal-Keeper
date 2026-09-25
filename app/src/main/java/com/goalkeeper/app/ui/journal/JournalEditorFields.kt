package com.goalkeeper.app.ui.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.components.GkIconButton
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.Mood

/** A themed, borderless-looking text field: raised container, accent cursor/border when focused. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    textColor: Color = GkTheme.colors.text,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = GkTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = colors.subtle) },
        textStyle = textStyle,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.raised,
            unfocusedContainerColor = colors.raised,
            disabledContainerColor = colors.raised,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            cursorColor = colors.accent,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedPlaceholderColor = colors.subtle,
            unfocusedPlaceholderColor = colors.subtle,
        ),
    )
}

/** ENTRY editor body: an optional mood selector, then a large multiline field. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryFields(
    mood: Mood?,
    body: String,
    onMoodTap: (Mood) -> Unit,
    onBodyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Mood.entries.forEach { value ->
                MoodChipSelectable(mood = value, selected = mood == value, onClick = { onMoodTap(value) })
            }
        }
        StyledTextField(
            value = body,
            onValueChange = onBodyChange,
            placeholder = "What happened? How did it feel?",
            modifier = Modifier.fillMaxWidth(),
            minLines = 7,
        )
    }
}

@Composable
private fun MoodChipSelectable(mood: Mood, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.accent else colors.raised,
        contentColor = if (selected) colors.onAccent else colors.text,
        border = if (selected) null else BorderStroke(1.dp, colors.border),
    ) {
        Text(
            text = mood.label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** NOTE editor body: a title field and a multiline body field. */
@Composable
internal fun NoteFields(
    title: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StyledTextField(value = title, onValueChange = onTitleChange, placeholder = "Title", singleLine = true)
        StyledTextField(
            value = body,
            onValueChange = onBodyChange,
            placeholder = "Write your note…",
            modifier = Modifier.fillMaxWidth(),
            minLines = 8,
        )
    }
}

/** CHECKLIST editor body: title, progress, editable rows, and an "add task" row. */
@Composable
internal fun ChecklistFields(
    title: String,
    items: List<ChecklistDraftItem>,
    newItemText: String,
    onTitleChange: (String) -> Unit,
    onNewItemTextChange: (String) -> Unit,
    onCommitNewItem: () -> Unit,
    onItemTextChange: (Long, String) -> Unit,
    onItemDoneChange: (Long, Boolean) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onMoveItem: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StyledTextField(value = title, onValueChange = onTitleChange, placeholder = "Title", singleLine = true)

        val done = items.count { it.done }
        val total = items.size
        if (total > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$done of $total done", style = MaterialTheme.typography.labelLarge, color = colors.muted)
                val progress = done.toFloat() / total
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(colors.raised, RoundedCornerShape(3.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(colors.accent, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }

        items.forEachIndexed { index, item ->
            ChecklistEditRow(
                item = item,
                canMoveUp = index > 0,
                canMoveDown = index < items.lastIndex,
                onTextChange = { text -> onItemTextChange(item.id, text) },
                onDoneChange = { checked -> onItemDoneChange(item.id, checked) },
                onDelete = { onRemoveItem(item.id) },
                onMoveUp = { onMoveItem(item.id, -1) },
                onMoveDown = { onMoveItem(item.id, 1) },
            )
        }

        AddTaskRow(text = newItemText, onTextChange = onNewItemTextChange, onCommit = onCommitNewItem)
    }
}

@Composable
private fun ChecklistEditRow(
    item: ChecklistDraftItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTextChange: (String) -> Unit,
    onDoneChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Checkbox(
            checked = item.done,
            onCheckedChange = onDoneChange,
            colors = CheckboxDefaults.colors(checkedColor = colors.accent, uncheckedColor = colors.border, checkmarkColor = colors.onAccent),
        )
        StyledTextField(
            value = item.text,
            // Long tasks wrap instead of scrolling sideways, but a task stays one paragraph.
            onValueChange = { onTextChange(it.replace('\n', ' ')) },
            placeholder = "Task",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            textColor = if (item.done) colors.muted else colors.text,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            ),
        )
        Box {
            GkIconButton(
                icon = GkIcons.More,
                contentDescription = "Task options",
                onClick = { menuExpanded = true },
                tint = colors.muted,
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Move up") },
                    enabled = canMoveUp,
                    onClick = { menuExpanded = false; onMoveUp() },
                )
                DropdownMenuItem(
                    text = { Text("Move down") },
                    enabled = canMoveDown,
                    onClick = { menuExpanded = false; onMoveDown() },
                )
            }
        }
        GkIconButton(icon = GkIcons.Delete, contentDescription = "Delete task", onClick = onDelete, tint = colors.muted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskRow(text: String, onTextChange: (String) -> Unit, onCommit: () -> Unit, modifier: Modifier = Modifier) {
    StyledTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = "Add task",
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommit() }),
    )
}
