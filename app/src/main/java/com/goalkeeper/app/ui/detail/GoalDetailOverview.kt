package com.goalkeeper.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIconButton
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.PrimaryButton
import com.goalkeeper.app.ui.components.SecondaryButton
import com.goalkeeper.app.ui.components.StatusHeatmap
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import com.goalkeeper.core.reminder.ReminderPlanner
import com.goalkeeper.core.streak.StreakCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The Overview tab: today's check-in action, 16-week heatmap, month calendar, nudges, why, and a journal peek. */
internal fun LazyListScope.overviewSection(
    data: GoalDetailData,
    onCheckIn: (CheckInStatus?) -> Unit,
    onToggleDay: (LocalDate) -> Unit,
    onEditReminders: () -> Unit,
    onOpenJournalTab: () -> Unit,
    use24h: Boolean,
) {
    item(key = "checkin-actions") { CheckInActionsRow(data.stats.today, onCheckIn) }
    item(key = "heatmap") { HeatmapCard(data) }
    item(key = "calendar") { MonthCalendarCard(data, onToggleDay) }
    item(key = "nudges") { NudgesCard(data, onEditReminders, use24h) }
    if (data.goal.why.isNotBlank() || data.goal.targetDate != null) {
        item(key = "why") { WhyCard(data.goal) }
    }
    val latest = data.entries.maxByOrNull { it.createdAt }
    if (latest != null) {
        item(key = "latest-journal") { LatestJournalCard(latest, data.zone, use24h, onOpenJournalTab) }
    }
}

@Composable
private fun CheckInActionsRow(today: DayStatus, onCheckIn: (CheckInStatus?) -> Unit, modifier: Modifier = Modifier) {
    when (today) {
        DayStatus.PENDING -> Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = "Check in today",
                onClick = { onCheckIn(CheckInStatus.DONE) },
                icon = GkIcons.Check,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(text = "Skip", onClick = { onCheckIn(CheckInStatus.SKIPPED) })
        }

        DayStatus.DONE -> QuietStatusRow("Done today ✓", onUndo = { onCheckIn(null) }, modifier = modifier)

        DayStatus.SKIPPED -> QuietStatusRow("Skipped today", onUndo = { onCheckIn(null) }, modifier = modifier)

        DayStatus.REST -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Rest day — check in anyway?",
                style = MaterialTheme.typography.titleSmall,
                color = GkTheme.colors.muted,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(text = "Check in", onClick = { onCheckIn(CheckInStatus.DONE) })
        }

        DayStatus.MISSED, DayStatus.FUTURE, DayStatus.BEFORE_START -> Unit
    }
}

@Composable
private fun QuietStatusRow(label: String, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = GkTheme.colors.muted)
        TextButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("Undo", style = MaterialTheme.typography.labelLarge, color = GkTheme.colors.accentText)
        }
    }
}

@Composable
private fun HeatmapCard(data: GoalDetailData, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    GkCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Last 16 weeks", style = MaterialTheme.typography.titleMedium, color = colors.text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LegendSwatch(colors.accent)
                Text("done", style = GkTheme.mono.meta, color = colors.muted)
                Spacer(Modifier.width(6.dp))
                LegendSwatch(colors.accentContainer)
                Text("skip", style = GkTheme.mono.meta, color = colors.muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        StatusHeatmap(statuses = data.heatmapStatuses, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.heatmapMonthLabels.forEach { label ->
                Text(label, style = GkTheme.mono.meta, color = colors.muted)
            }
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
private fun MonthCalendarCard(data: GoalDetailData, onToggleDay: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    var displayedMonthEpoch by rememberSaveable { mutableStateOf(data.today.withDayOfMonth(1).toEpochDay()) }
    val displayedMonth = remember(displayedMonthEpoch) { LocalDate.ofEpochDay(displayedMonthEpoch) }
    val currentMonth = remember(data.today) { data.today.withDayOfMonth(1) }
    val canGoNext = displayedMonth.isBefore(currentMonth)

    GkCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Formats.monthYear.format(displayedMonth), style = MaterialTheme.typography.titleMedium, color = colors.text)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                GkIconButton(
                    icon = GkIcons.ChevronLeft,
                    contentDescription = "Previous month",
                    onClick = { displayedMonthEpoch = displayedMonth.minusMonths(1).toEpochDay() },
                )
                GkIconButton(
                    icon = GkIcons.ChevronRight,
                    contentDescription = "Next month",
                    onClick = { displayedMonthEpoch = displayedMonth.plusMonths(1).toEpochDay() },
                    enabled = canGoNext,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = GkTheme.mono.meta, color = colors.muted)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            monthGridDates(displayedMonth).chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { date ->
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) DayCell(date, data, onToggleDay)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Tap a day to edit it", style = MaterialTheme.typography.labelMedium, color = colors.subtle)
    }
}

private fun monthGridDates(monthStart: LocalDate): List<LocalDate?> {
    val leading = monthStart.dayOfWeek.value - 1
    val daysInMonth = monthStart.lengthOfMonth()
    val cells = ArrayList<LocalDate?>(leading + daysInMonth + 6)
    repeat(leading) { cells.add(null) }
    for (day in 1..daysInMonth) cells.add(monthStart.withDayOfMonth(day))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

@Composable
private fun DayCell(date: LocalDate, data: GoalDetailData, onToggleDay: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    val status = StreakCalculator.dayStatus(data.goal, data.checkInsByDate, date, data.today)
    val isFuture = date.isAfter(data.today)
    val shape = CircleShape

    val background = when (status) {
        DayStatus.DONE -> colors.accent
        DayStatus.MISSED -> colors.raised
        else -> Color.Transparent
    }
    val textColor = when (status) {
        DayStatus.DONE -> colors.onAccent
        DayStatus.MISSED, DayStatus.SKIPPED -> colors.text
        DayStatus.REST -> colors.muted
        else -> colors.subtle
    }

    // Clip before clickable so the ripple stays round.
    var cellModifier = modifier
        .fillMaxSize()
        .padding(2.dp)
        .clip(shape)
        .then(if (!isFuture) Modifier.clickable { onToggleDay(date) } else Modifier)
        .background(background)
    if (status == DayStatus.SKIPPED) cellModifier = cellModifier.border(1.5.dp, colors.border, shape)
    if (status == DayStatus.PENDING) cellModifier = cellModifier.border(2.dp, colors.accent, shape)

    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        Text(date.dayOfMonth.toString(), style = GkTheme.mono.small, color = textColor)
    }
}

@Composable
private fun NudgesCard(data: GoalDetailData, onEdit: () -> Unit, use24h: Boolean, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    val reminder = data.goal.reminder
    GkCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoLabel("Nudges · if not done")
            TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(
                    text = if (reminder.enabled) "Edit" else "Turn on",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accentText,
                )
            }
        }
        if (!reminder.enabled) {
            Spacer(Modifier.height(10.dp))
            Text("Reminders off", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        } else {
            Spacer(Modifier.height(18.dp))
            NudgeTimeline(times = ReminderPlanner.nudgeTimes(reminder), now = data.nowTime, use24h = use24h)
        }
    }
}

@Composable
private fun NudgeTimeline(times: List<LocalTime>, now: LocalTime, use24h: Boolean, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    val nextIndex = times.indexOfFirst { it.isAfter(now) }
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(horizontal = 9.dp).fillMaxWidth().height(18.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colors.border),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            times.forEachIndexed { index, time ->
                val isActive = index == nextIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var dotModifier = Modifier.size(18.dp).clip(CircleShape).background(if (isActive) colors.accent else colors.card)
                    if (!isActive) dotModifier = dotModifier.border(2.dp, colors.accent, CircleShape)
                    Box(modifier = dotModifier)
                    Text(
                        text = Formats.time(time, use24h),
                        style = GkTheme.mono.small,
                        color = if (isActive) colors.text else colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun WhyCard(goal: Goal, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    GkCard(modifier = modifier.fillMaxWidth()) {
        MonoLabel("Why it matters")
        if (goal.why.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(goal.why, style = MaterialTheme.typography.bodyMedium, color = colors.text)
        }
        val targetDate = goal.targetDate
        if (targetDate != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Target: ${Formats.mediumDate.format(targetDate)}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun LatestJournalCard(
    entry: JournalEntry,
    zone: ZoneId,
    use24h: Boolean,
    onOpenJournalTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    val zoned = entry.createdAt.atZone(zone)
    GkCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoLabel("Latest in journal")
            TextButton(onClick = onOpenJournalTab, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Open journal", style = MaterialTheme.typography.labelLarge, color = colors.accentText)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "${Formats.monoDate(zoned.toLocalDate())} · ${Formats.time(zoned.toLocalTime(), use24h)}",
            style = GkTheme.mono.meta,
            color = colors.muted,
        )
        Spacer(Modifier.height(6.dp))
        val preview = when (entry.type) {
            JournalEntryType.ENTRY -> entry.body
            JournalEntryType.NOTE -> entry.title.ifBlank { entry.body }
            JournalEntryType.CHECKLIST -> entry.title.ifBlank { "Task list" }
        }
        Text(
            text = preview,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
