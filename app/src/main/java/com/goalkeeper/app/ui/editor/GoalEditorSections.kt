package com.goalkeeper.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SignalBars
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance

/** Section 1 — title and "why it matters". */
@Composable
internal fun GoalInfoSection(
    title: String,
    titleError: String?,
    why: String,
    onTitleChange: (String) -> Unit,
    onWhyChange: (String) -> Unit,
) {
    GkCard {
        MonoLabel("GOAL")
        Spacer(Modifier.height(14.dp))
        EditorTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "Title",
            isError = titleError != null,
            singleLine = true,
        )
        if (titleError != null) {
            Spacer(Modifier.height(6.dp))
            Text(titleError, style = MaterialTheme.typography.bodySmall, color = GkTheme.colors.danger)
        }
        Spacer(Modifier.height(16.dp))
        EditorTextField(
            value = why,
            onValueChange = onWhyChange,
            label = "Why it matters",
            placeholder = "The reason you'll read when motivation dips",
            singleLine = false,
            minLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This shows up in your reminder notifications, so make it something worth reading on a tired evening.",
            style = MaterialTheme.typography.bodySmall,
            color = GkTheme.colors.muted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val colors = GkTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.raised,
            unfocusedContainerColor = colors.raised,
            disabledContainerColor = colors.raised,
            errorContainerColor = colors.raised,
            focusedIndicatorColor = colors.accent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = colors.danger,
            cursorColor = colors.accent,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedLabelColor = colors.accentText,
            unfocusedLabelColor = colors.muted,
            errorLabelColor = colors.danger,
            focusedPlaceholderColor = colors.subtle,
            unfocusedPlaceholderColor = colors.subtle,
        ),
    )
}

/**
 * Section 2 — icon picker: a wrapping grid of tiles (never a lazy grid inside the scrolling form). As many
 * columns as fit at 44dp or more, with the tiles stretched so each row spans the card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalIconSection(selected: GoalIcon, onSelect: (GoalIcon) -> Unit) {
    GkCard(modifier = Modifier.fillMaxWidth()) {
        MonoLabel("ICON")
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 8.dp
            val columns = ((maxWidth + spacing) / (44.dp + spacing)).toInt().coerceAtLeast(1)
            // Whole pixels, rounded down, so a full row can never overflow and wrap early.
            val tileSize = with(LocalDensity.current) {
                ((constraints.maxWidth - spacing.roundToPx() * (columns - 1)) / columns).toDp()
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
                maxItemsInEachRow = columns,
            ) {
                GoalIcon.entries.forEach { icon ->
                    IconTile(icon = icon, selected = icon == selected, size = tileSize, onClick = { onSelect(icon) })
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: GoalIcon, selected: Boolean, size: Dp, onClick: () -> Unit) {
    val colors = GkTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.accent else colors.raised)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = icon.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = GkIcons.forGoal(icon),
            contentDescription = null,
            tint = if (selected) colors.onAccent else colors.text,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Section 3 — importance: four selectable rows, each with signal bars and its default nudge count. */
@Composable
internal fun GoalImportanceSection(selected: Importance, onSelect: (Importance) -> Unit) {
    val colors = GkTheme.colors
    GkCard(contentPadding = PaddingValues(0.dp)) {
        MonoLabel("IMPORTANCE", modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
        Importance.entries.forEach { option ->
            HorizontalDivider(color = colors.border)
            ImportanceRow(option = option, selected = option == selected, onClick = { onSelect(option) })
        }
        HorizontalDivider(color = colors.border)
        Text(
            text = "Important goals get more nudges and rank higher.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun ImportanceRow(option: Importance, selected: Boolean, onClick: () -> Unit) {
    val colors = GkTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SignalBars(importance = option, activeColor = if (selected) colors.accent else colors.subtle)
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        MonoLabel(
            text = if (option.defaultNudges == 1) "1 NUDGE" else "${option.defaultNudges} NUDGES",
            color = if (selected) colors.accentText else colors.muted,
        )
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(GkIcons.Check, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
            }
        }
    }
}
