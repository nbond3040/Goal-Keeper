package com.goalkeeper.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.rememberUse24h
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SecondaryButton
import com.goalkeeper.app.ui.theme.GkTheme

/** "REMINDERS" — default nudge time, morning briefing, notification/exact-alarm status, test nudge. */
@Composable
internal fun ReminderSettingsSection(
    defaultReminderMinute: Int,
    briefingEnabled: Boolean,
    briefingMinute: Int,
    notificationsEnabled: Boolean,
    canScheduleExactAlarms: Boolean,
    hasExactAlarmSettingsScreen: Boolean,
    onDefaultReminderMinuteChanged: (Int) -> Unit,
    onBriefingEnabledChanged: (Boolean) -> Unit,
    onBriefingMinuteChanged: (Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSendTestNudge: () -> Unit,
) {
    val colors = GkTheme.colors
    val use24h = rememberUse24h()
    var editingDefaultTime by remember { mutableStateOf(false) }
    var editingBriefingTime by remember { mutableStateOf(false) }

    GkCard(modifier = Modifier.fillMaxWidth()) {
        MonoLabel("REMINDERS")
        Spacer(Modifier.height(14.dp))

        SettingsTimeRow(
            label = "Default first-nudge time",
            value = Formats.time(defaultReminderMinute, use24h),
            onClick = { editingDefaultTime = true },
        )

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Morning briefing", style = MaterialTheme.typography.bodyLarge, color = colors.text)
                Text(
                    text = "A short summary of what's scheduled today",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            Switch(
                checked = briefingEnabled,
                onCheckedChange = onBriefingEnabledChanged,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.accent,
                    checkedThumbColor = colors.onAccent,
                    checkedBorderColor = colors.accent,
                ),
            )
        }
        if (briefingEnabled) {
            Spacer(Modifier.height(8.dp))
            SettingsTimeRow(
                label = "Briefing time",
                value = Formats.time(briefingMinute, use24h),
                onClick = { editingBriefingTime = true },
            )
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = colors.border)
        Spacer(Modifier.height(14.dp))

        StatusRow(
            label = "Notifications",
            statusOn = notificationsEnabled,
            actionLabel = "Open settings",
            onAction = onOpenNotificationSettings,
        )
        Spacer(Modifier.height(14.dp))
        StatusRow(
            label = "Precise timing",
            statusOn = canScheduleExactAlarms,
            actionLabel = if (hasExactAlarmSettingsScreen) "Open settings" else null,
            onAction = onOpenExactAlarmSettings,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Without this, Android may delay your nudges by a few minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )

        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            text = "Send a test nudge",
            icon = GkIcons.BellActive,
            onClick = onSendTestNudge,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (editingDefaultTime) {
        SettingsTimePickerDialog(
            initialMinuteOfDay = defaultReminderMinute,
            use24h = use24h,
            onDismiss = { editingDefaultTime = false },
            onConfirm = { minute ->
                onDefaultReminderMinuteChanged(minute)
                editingDefaultTime = false
            },
        )
    }
    if (editingBriefingTime) {
        SettingsTimePickerDialog(
            initialMinuteOfDay = briefingMinute,
            use24h = use24h,
            onDismiss = { editingBriefingTime = false },
            onConfirm = { minute ->
                onBriefingMinuteChanged(minute)
                editingBriefingTime = false
            },
        )
    }
}

@Composable
private fun SettingsTimeRow(label: String, value: String, onClick: () -> Unit) {
    val colors = GkTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(GkIcons.Alarm, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.weight(1f))
        Text(value, style = GkTheme.mono.meta, color = colors.muted)
        Spacer(Modifier.width(6.dp))
        Icon(GkIcons.ChevronRight, contentDescription = null, tint = colors.subtle, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatusRow(label: String, statusOn: Boolean, actionLabel: String?, onAction: () -> Unit) {
    val colors = GkTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            Text(
                text = if (statusOn) "On" else "Off",
                style = MaterialTheme.typography.bodySmall,
                color = if (statusOn) colors.accentText else colors.muted,
            )
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = colors.accentText) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTimePickerDialog(
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
