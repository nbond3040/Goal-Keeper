package com.goalkeeper.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.rememberUse24h
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.reminder.ReminderPlanner

/** Section 6 — reminders: enable switch, first nudge time, nudge count and spacing, and a live preview. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalRemindersSection(
    reminder: ReminderConfig,
    onEnabledChanged: (Boolean) -> Unit,
    onStartMinuteChanged: (Int) -> Unit,
    onIncrementNudges: () -> Unit,
    onDecrementNudges: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
) {
    val colors = GkTheme.colors
    val use24h = rememberUse24h()
    var showTimeDialog by remember { mutableStateOf(false) }
    val nudgeTimes = remember(reminder) { ReminderPlanner.nudgeTimes(reminder) }

    GkCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("REMINDERS", modifier = Modifier.weight(1f))
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onEnabledChanged,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.accent,
                    checkedThumbColor = colors.onAccent,
                    checkedBorderColor = colors.accent,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Nudges only fire on scheduled days you haven't checked in yet, and stop as soon as you do.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )

        if (reminder.enabled) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showTimeDialog = true }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(GkIcons.Alarm, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("First nudge at", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                    Text(
                        text = Formats.time(reminder.startMinuteOfDay, use24h),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text,
                    )
                }
                Icon(GkIcons.ChevronRight, contentDescription = null, tint = colors.subtle, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Nudges per day", style = MaterialTheme.typography.bodyMedium, color = colors.text)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepperButton(
                        symbol = "−",
                        description = "Fewer nudges",
                        enabled = reminder.nudgeCount > 1,
                        onClick = onDecrementNudges,
                    )
                    Text(
                        text = "${reminder.nudgeCount}",
                        style = GkTheme.mono.value,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(30.dp),
                    )
                    StepperButton(
                        symbol = "+",
                        description = "More nudges",
                        enabled = reminder.nudgeCount < ReminderConfig.MAX_NUDGES,
                        onClick = onIncrementNudges,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Every", style = MaterialTheme.typography.bodyMedium, color = colors.text)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderConfig.INTERVAL_CHOICES.forEach { minutes ->
                    IntervalChip(
                        label = intervalLabel(minutes),
                        selected = minutes == reminder.intervalMinutes,
                        onClick = { onIntervalSelected(minutes) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            MonoLabel("TODAY'S NUDGES")
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (nudgeTimes.isEmpty()) "—" else nudgeTimes.joinToString(" · ") { Formats.time(it, use24h) },
                style = GkTheme.mono.meta,
                color = colors.text,
            )
            if (nudgeTimes.size < reminder.nudgeCount) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Only ${nudgeTimes.size} of ${reminder.nudgeCount} nudges fit before midnight " +
                        "— try an earlier start time or a shorter interval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                )
            }
        }
    }

    if (showTimeDialog) {
        ReminderTimePickerDialog(
            initialMinuteOfDay = reminder.startMinuteOfDay,
            use24h = use24h,
            onDismiss = { showTimeDialog = false },
            onConfirm = { minute ->
                onStartMinuteChanged(minute)
                showTimeDialog = false
            },
        )
    }
}

@Composable
private fun StepperButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = GkTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp).semantics { contentDescription = description },
        shape = CircleShape,
        color = colors.raised,
        contentColor = if (enabled) colors.text else colors.subtle,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun IntervalChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = GkTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.accent else colors.raised,
        contentColor = if (selected) colors.onAccent else colors.text,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.border),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = GkTheme.mono.meta)
        }
    }
}

private fun intervalLabel(minutes: Int): String = when (minutes) {
    60 -> "1h"
    90 -> "1.5h"
    120 -> "2h"
    180 -> "3h"
    240 -> "4h"
    else -> "${minutes}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialMinuteOfDay: Int,
    use24h: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = use24h,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
    )
}
