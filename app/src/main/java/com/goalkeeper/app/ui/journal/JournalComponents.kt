package com.goalkeeper.app.ui.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import java.time.LocalDate
import java.time.ZoneId

/**
 * Shared journal timeline pieces used by both the per-goal Journal tab (`ui.detail`) and the global
 * Journal feed (`ui.journal`). Internal: both packages live in the same module.
 */

/** The All / Log / Tasks / Notes filter shown above every journal timeline. */
internal enum class JournalFilter(val label: String) {
    ALL("All"),
    LOG("Log"),
    TASKS("Tasks"),
    NOTES("Notes"),
}

internal fun JournalFilter.accepts(entry: JournalEntry): Boolean = when (this) {
    JournalFilter.ALL -> true
    JournalFilter.LOG -> entry.type == JournalEntryType.ENTRY
    JournalFilter.TASKS -> entry.type == JournalEntryType.CHECKLIST
    JournalFilter.NOTES -> entry.type == JournalEntryType.NOTE
}

/** What the journal feed needs to render a tappable goal chip on an entry card. */
internal data class GoalChipInfo(val goalId: Long, val icon: GoalIcon, val title: String)

/** One row of a rendered journal timeline: a section header or an entry. */
internal sealed interface JournalRow {
    data object PinnedHeader : JournalRow

    data class DayHeader(val label: String, val isToday: Boolean) : JournalRow

    /** [emphasized] is true for entries under the "Today" header (accent-tinted dot). */
    data class EntryRow(val entry: JournalEntry, val emphasized: Boolean) : JournalRow
}

/**
 * Groups [entries] (already ordered pinned-first-then-newest by the repository) into timeline rows:
 * a "Pinned" section first, then the rest under day headers ("Today", "Yesterday", or [Formats.monoDate]).
 * [zone] converts each entry's [JournalEntry.createdAt] instant to a calendar day.
 */
internal fun buildJournalRows(entries: List<JournalEntry>, today: LocalDate, zone: ZoneId): List<JournalRow> {
    if (entries.isEmpty()) return emptyList()
    val pinned = entries.filter { it.pinned }
    val rest = entries.filterNot { it.pinned }

    val rows = mutableListOf<JournalRow>()
    if (pinned.isNotEmpty()) {
        rows += JournalRow.PinnedHeader
        pinned.forEach { rows += JournalRow.EntryRow(it, emphasized = false) }
    }

    var lastDay: LocalDate? = null
    var lastIsToday = false
    for (entry in rest) {
        val day = entry.createdAt.atZone(zone).toLocalDate()
        if (day != lastDay) {
            val isToday = day == today
            val label = when {
                isToday -> "TODAY"
                day == today.minusDays(1) -> "YESTERDAY"
                else -> Formats.monoDate(day)
            }
            rows += JournalRow.DayHeader(label, isToday)
            lastDay = day
            lastIsToday = isToday
        }
        rows += JournalRow.EntryRow(entry, emphasized = lastIsToday)
    }
    return rows
}

private fun JournalRow.rowKey(): Any = when (this) {
    JournalRow.PinnedHeader -> "pinned-header"
    is JournalRow.DayHeader -> "day-$label"
    is JournalRow.EntryRow -> "entry-${entry.id}"
}

/** One slot of the timeline: a 36dp gutter with a continuous rail line (and optional dot) beside the content. */
@Composable
internal fun TimelineRow(
    isLast: Boolean,
    modifier: Modifier = Modifier,
    dot: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = if (isLast) 0.dp else 14.dp),
    ) {
        Box(modifier = Modifier.width(36.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(GkTheme.colors.border),
                )
            }
            if (dot != null) dot()
        }
        Spacer(modifier = Modifier.width(14.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/** The round type-icon marker on the rail for one entry. Accent-tinted for today's entries. */
@Composable
internal fun EntryTypeDot(type: JournalEntryType, emphasized: Boolean, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (emphasized) colors.accentContainer else colors.raised),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = GkIcons.forEntryType(type),
            contentDescription = null,
            tint = if (emphasized) colors.accentText else colors.text,
            modifier = Modifier.size(17.dp),
        )
    }
}

/**
 * One journal entry card: tap opens it, long-press shows Pin/Unpin + Delete (Delete asks to confirm).
 * [goalChip], when non-null, is shown above the entry body (the journal feed's goal label).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun JournalEntryCard(
    entry: JournalEntry,
    zone: ZoneId,
    use24h: Boolean,
    goalChip: GoalChipInfo?,
    onOpen: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onSetItemDone: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        GkCard(
            onClick = null,
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onLongClick = { menuExpanded = true }, onClick = onOpen),
        ) {
            if (goalChip != null) {
                GoalChip(info = goalChip, onClick = { onOpenGoal(goalChip.goalId) })
                Spacer(modifier = Modifier.height(8.dp))
            }
            when (entry.type) {
                JournalEntryType.ENTRY -> LogEntryBody(entry, entry.createdAt.atZone(zone).toLocalTime(), use24h)
                JournalEntryType.NOTE -> NoteBody(entry)
                JournalEntryType.CHECKLIST -> ChecklistBody(entry, onSetItemDone)
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(if (entry.pinned) "Unpin" else "Pin") },
                leadingIcon = { Icon(GkIcons.Pin, contentDescription = null) },
                onClick = { menuExpanded = false; onTogglePin() },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = GkTheme.colors.danger) },
                leadingIcon = { Icon(GkIcons.Delete, contentDescription = null, tint = GkTheme.colors.danger) },
                onClick = { menuExpanded = false; confirmDelete = true },
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete", color = GkTheme.colors.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/**
 * Emits [rows] into a [LazyListScope] as a rail timeline. [goalChipFor] returns non-null to show a goal
 * chip on an entry's card (used by the journal feed only); [onOpenGoal] is only invoked in that case.
 */
internal fun LazyListScope.journalTimeline(
    rows: List<JournalRow>,
    zone: ZoneId,
    use24h: Boolean,
    onOpen: (JournalEntry) -> Unit,
    onTogglePin: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
    onSetItemDone: (Long, Boolean) -> Unit,
    goalChipFor: (JournalEntry) -> GoalChipInfo? = { null },
    onOpenGoal: (Long) -> Unit = {},
) {
    itemsIndexed(items = rows, key = { _, row -> row.rowKey() }) { index, row ->
        val isLast = index == rows.lastIndex
        when (row) {
            JournalRow.PinnedHeader -> TimelineRow(isLast = isLast) { MonoLabel("Pinned") }

            is JournalRow.DayHeader -> TimelineRow(isLast = isLast) {
                MonoLabel(row.label, color = if (row.isToday) GkTheme.colors.accentText else GkTheme.colors.muted)
            }

            is JournalRow.EntryRow -> TimelineRow(
                isLast = isLast,
                dot = { EntryTypeDot(type = row.entry.type, emphasized = row.emphasized) },
            ) {
                JournalEntryCard(
                    entry = row.entry,
                    zone = zone,
                    use24h = use24h,
                    goalChip = goalChipFor(row.entry),
                    onOpen = { onOpen(row.entry) },
                    onOpenGoal = onOpenGoal,
                    onTogglePin = { onTogglePin(row.entry) },
                    onDelete = { onDelete(row.entry) },
                    onSetItemDone = onSetItemDone,
                )
            }
        }
    }
}
