package com.goalkeeper.app.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.Mood
import java.time.LocalTime

/** Small pill naming the goal an entry belongs to (journal feed cards only). Tap navigates to that goal. */
@Composable
internal fun GoalChip(info: GoalChipInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = GkIcons.forGoal(info.icon),
            contentDescription = null,
            tint = GkTheme.colors.accentText,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = info.title,
            style = MaterialTheme.typography.labelMedium,
            color = GkTheme.colors.accentText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A short mood tag: mono text in an accent-bordered pill (e.g. "STRONG"). */
@Composable
internal fun MoodChip(mood: Mood, modifier: Modifier = Modifier) {
    val color = GkTheme.colors.accentText
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text = mood.label, style = GkTheme.mono.label, color = color)
    }
}

/** ENTRY card body: time + optional mood tag, then the log body (ellipsized). */
@Composable
internal fun LogEntryBody(entry: JournalEntry, time: LocalTime, use24h: Boolean, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = Formats.time(time, use24h), style = GkTheme.mono.meta, color = colors.muted)
            entry.mood?.let { MoodChip(it) }
        }
        Text(
            text = entry.body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** NOTE card body: title plus a short body preview. */
@Composable
internal fun NoteBody(entry: JournalEntry, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = entry.title.ifBlank { "Untitled note" },
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.body.isNotBlank()) {
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** CHECKLIST card body: title + done count + progress bar + up to 6 toggleable rows. */
@Composable
internal fun ChecklistBody(
    entry: JournalEntry,
    onSetItemDone: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    val total = entry.items.size
    val done = entry.doneCount
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.title.ifBlank { "Task list" },
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$done/$total",
                style = GkTheme.mono.meta.copy(fontWeight = FontWeight.Bold),
                color = colors.accentText,
            )
        }
        val progress = if (total == 0) 0f else done.toFloat() / total
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.raised),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.accent),
            )
        }
        entry.items.take(6).forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(
                    checked = item.done,
                    onCheckedChange = { checked -> onSetItemDone(item.id, checked) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accent,
                        uncheckedColor = colors.border,
                        checkmarkColor = colors.onAccent,
                    ),
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.done) colors.muted else colors.text,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (total > 6) {
            Text(text = "+${total - 6} more", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        }
    }
}
