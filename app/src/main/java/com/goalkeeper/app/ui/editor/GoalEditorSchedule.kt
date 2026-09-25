package com.goalkeeper.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SegmentedTabs
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.Schedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

/** Section 4 — quick schedule options plus the seven day toggles (Monday first). */
@Composable
internal fun GoalScheduleSection(scheduleMask: Int, onQuickSchedule: (Int) -> Unit, onToggleDay: (DayOfWeek) -> Unit) {
    val colors = GkTheme.colors
    val schedule = Schedule(scheduleMask)
    val quickIndex = when (scheduleMask) {
        Schedule.DAILY.mask -> 0
        Schedule.WEEKDAYS.mask -> 1
        else -> 2
    }
    GkCard {
        MonoLabel("SCHEDULE")
        Spacer(Modifier.height(14.dp))
        SegmentedTabs(
            options = listOf("Every day", "Weekdays", "Custom"),
            selectedIndex = quickIndex,
            onSelect = { index ->
                when (index) {
                    0 -> onQuickSchedule(Schedule.DAILY.mask)
                    1 -> onQuickSchedule(Schedule.WEEKDAYS.mask)
                    else -> Unit
                }
            },
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DayOfWeek.entries.forEach { day ->
                DayToggle(day = day, selected = schedule.includes(day), onClick = { onToggleDay(day) })
            }
        }
        Spacer(Modifier.height(14.dp))
        val daysPerWeek = schedule.daysPerWeek
        MonoLabel(if (daysPerWeek == 1) "1 DAY A WEEK" else "$daysPerWeek DAYS A WEEK", color = colors.text)
    }
}

@Composable
private fun DayToggle(day: DayOfWeek, selected: Boolean, onClick: () -> Unit) {
    val colors = GkTheme.colors
    val letter = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
    val fullName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else colors.raised)
            .border(1.dp, if (selected) colors.accent else colors.border, CircleShape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics { contentDescription = fullName },
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = GkTheme.mono.small, color = if (selected) colors.onAccent else colors.muted)
    }
}

/** Section 5 — optional target date; tap opens a Material3 date picker limited to today or later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalTargetDateSection(targetDate: LocalDate?, today: LocalDate, onDateSelected: (LocalDate?) -> Unit) {
    val colors = GkTheme.colors
    var showDialog by remember { mutableStateOf(false) }
    GkCard {
        MonoLabel("TARGET DATE")
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { showDialog = true }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(GkIcons.Calendar, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (targetDate != null) Formats.mediumDate.format(targetDate) else "No target date",
                style = MaterialTheme.typography.bodyLarge,
                color = if (targetDate != null) colors.text else colors.muted,
                modifier = Modifier.weight(1f),
            )
            if (targetDate != null) {
                TextButton(onClick = { onDateSelected(null) }) {
                    Text("Clear", color = colors.accentText)
                }
            }
            Icon(GkIcons.ChevronRight, contentDescription = null, tint = colors.subtle, modifier = Modifier.size(20.dp))
        }
    }
    if (showDialog) {
        TargetDatePickerDialog(
            initialDate = targetDate ?: today,
            today = today,
            onDismiss = { showDialog = false },
            onConfirm = { date ->
                onDateSelected(date)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetDatePickerDialog(
    initialDate: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val selectableDates = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !date.isBefore(today)
            }
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectableDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                val date = if (millis != null) {
                    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                } else {
                    initialDate
                }
                onConfirm(date)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
