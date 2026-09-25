package com.goalkeeper.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.GoalSummary
import com.goalkeeper.app.ui.components.BarChart
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GoalIconTile
import com.goalkeeper.app.ui.components.IntensityHeatmap
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.theme.GkTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** "CONSISTENCY" — a 16-week Monday–Sunday completion heatmap with a 4-swatch legend. */
@Composable
internal fun ConsistencyCard(ratios: List<Float?>) {
    val colors = GkTheme.colors
    GkCard {
        MonoLabel("CONSISTENCY")
        Spacer(Modifier.height(14.dp))
        IntensityHeatmap(ratios = ratios, description = "16-week completion heatmap")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Less", style = MaterialTheme.typography.bodySmall, color = colors.muted)
            repeat(4) { index ->
                val ratio = index / 3f
                val swatch = if (index == 0) colors.raised else colors.accent.copy(alpha = 0.3f + 0.7f * ratio)
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(swatch))
            }
            Text("More", style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
    }
}

/** "WEEKLY CHECK-INS" — DONE count per week for the last 12 weeks. */
@Composable
internal fun WeeklyCheckInsCard(weeklyDone: List<Float>, rangeStart: LocalDate, rangeEnd: LocalDate) {
    val maxValue = (weeklyDone.maxOrNull() ?: 0f).coerceAtLeast(1f)
    GkCard {
        MonoLabel("WEEKLY CHECK-INS")
        Spacer(Modifier.height(14.dp))
        BarChart(values = weeklyDone, maxValue = maxValue, description = "Check-ins per week")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MonoLabel(Formats.monoDate(rangeStart))
            MonoLabel(Formats.monoDate(rangeEnd))
        }
    }
}

/** "BEST DAYS" — completion rate per weekday over the last 12 weeks. */
@Composable
internal fun BestDaysCard(rates: List<Pair<DayOfWeek, Float>>) {
    val colors = GkTheme.colors
    GkCard(contentPadding = PaddingValues(0.dp)) {
        MonoLabel("BEST DAYS", modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
        rates.forEachIndexed { index, (day, rate) ->
            if (index > 0) HorizontalDivider(color = colors.border)
            BestDayRow(day = day, rate = rate)
        }
    }
}

@Composable
private fun BestDayRow(day: DayOfWeek, rate: Float) {
    val colors = GkTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.width(40.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.raised),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(rate.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.accent),
            )
        }
        Text(
            text = Formats.percent(rate),
            style = GkTheme.mono.meta,
            color = colors.text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

/** "STREAK LEADERBOARD" — goals ranked by current streak (ties broken by best streak). */
@Composable
internal fun LeaderboardCard(leaderboard: List<GoalSummary>, onOpenGoal: (Long) -> Unit) {
    val colors = GkTheme.colors
    GkCard(contentPadding = PaddingValues(0.dp)) {
        MonoLabel("STREAK LEADERBOARD", modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
        leaderboard.forEachIndexed { index, summary ->
            if (index > 0) HorizontalDivider(color = colors.border)
            LeaderboardRow(rank = index + 1, summary = summary, onClick = { onOpenGoal(summary.goal.id) })
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, summary: GoalSummary, onClick: () -> Unit) {
    val colors = GkTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "$rank",
            style = GkTheme.mono.meta,
            color = colors.muted,
            modifier = Modifier.width(20.dp),
        )
        GoalIconTile(icon = summary.goal.icon, size = 36.dp)
        Text(
            text = summary.goal.title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text("${summary.stats.current}d", style = GkTheme.mono.meta, color = colors.accentText)
            Text("best ${summary.stats.best}", style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
    }
}
