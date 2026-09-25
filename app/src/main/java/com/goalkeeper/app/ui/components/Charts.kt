package com.goalkeeper.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.DayStatus
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min

/**
 * GitHub-style grid of day statuses for one goal. [statuses] is column-major: each column is one Monday–Sunday
 * week (7 entries), oldest week first. Cells are sized to fill the width (capped at [maxCell]).
 */
@Composable
fun StatusHeatmap(
    statuses: List<DayStatus>,
    modifier: Modifier = Modifier,
    maxCell: Dp = 18.dp,
    gap: Dp = 4.dp,
    description: String = "Check-in history",
) {
    val colors = GkTheme.colors
    val columns = ((statuses.size + 6) / 7).coerceAtLeast(1)
    HeatGrid(columns = columns, modifier = modifier, maxCell = maxCell, gap = gap, description = description) { index, topLeft, cell, radius ->
        when (statuses.getOrNull(index)) {
            DayStatus.DONE -> fillCell(colors.accent, topLeft, cell, radius)
            DayStatus.SKIPPED -> fillCell(colors.accentContainer, topLeft, cell, radius)
            DayStatus.MISSED -> fillCell(colors.raised, topLeft, cell, radius)
            DayStatus.REST -> fillCell(colors.raised.copy(alpha = 0.45f), topLeft, cell, radius)
            DayStatus.PENDING -> drawOutline(colors.accent, topLeft, cell, radius, 2.dp.toPx(), dashed = false)
            DayStatus.FUTURE -> drawOutline(colors.border, topLeft, cell, radius, 1.dp.toPx(), dashed = true)
            DayStatus.BEFORE_START, null -> Unit
        }
    }
}

/**
 * Grid of completion ratios (0..1) across all goals, column-major Monday–Sunday weeks, oldest first.
 * A null entry is a day with nothing to show (future or before any goal existed).
 */
@Composable
fun IntensityHeatmap(
    ratios: List<Float?>,
    modifier: Modifier = Modifier,
    maxCell: Dp = 18.dp,
    gap: Dp = 4.dp,
    description: String = "Completion history",
) {
    val colors = GkTheme.colors
    val columns = ((ratios.size + 6) / 7).coerceAtLeast(1)
    HeatGrid(columns = columns, modifier = modifier, maxCell = maxCell, gap = gap, description = description) { index, topLeft, cell, radius ->
        if (index >= ratios.size) return@HeatGrid
        val ratio = ratios[index]
        when {
            ratio == null -> drawOutline(colors.border, topLeft, cell, radius, 1.dp.toPx(), dashed = true)
            ratio <= 0f -> fillCell(colors.raised, topLeft, cell, radius)
            else -> fillCell(colors.accent.copy(alpha = 0.3f + 0.7f * ratio.coerceIn(0f, 1f)), topLeft, cell, radius)
        }
    }
}

@Composable
private fun HeatGrid(
    columns: Int,
    modifier: Modifier,
    maxCell: Dp,
    gap: Dp,
    description: String,
    drawCell: DrawScope.(index: Int, topLeft: Offset, cell: Size, radius: CornerRadius) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Cells shrink to fit narrow screens; the grid's height follows the resulting cell size.
        val cellDp = minOf(maxCell, (maxWidth - gap * (columns - 1)) / columns)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellDp * 7 + gap * 6)
                .semantics { contentDescription = description },
        ) {
            val gapPx = gap.toPx()
            val cellPx = cellDp.toPx()
            val totalWidth = cellPx * columns + gapPx * (columns - 1)
            val startX = (size.width - totalWidth) / 2f
            val cell = Size(cellPx, cellPx)
            val radius = CornerRadius(cellPx * 0.25f, cellPx * 0.25f)
            for (column in 0 until columns) {
                for (row in 0 until 7) {
                    val topLeft = Offset(startX + column * (cellPx + gapPx), row * (cellPx + gapPx))
                    drawCell(column * 7 + row, topLeft, cell, radius)
                }
            }
        }
    }
}

private fun DrawScope.fillCell(color: Color, topLeft: Offset, cell: Size, radius: CornerRadius) {
    drawRoundRect(color = color, topLeft = topLeft, size = cell, cornerRadius = radius)
}

private fun DrawScope.drawOutline(color: Color, topLeft: Offset, cell: Size, radius: CornerRadius, width: Float, dashed: Boolean) {
    val inset = width / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(cell.width - width, cell.height - width),
        cornerRadius = radius,
        style = Stroke(
            width = width,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(3f, 3f)) else null,
        ),
    )
}

/** One day of the Today screen's week strip. */
data class WeekDayUi(
    val date: LocalDate,
    /** Share of that day's scheduled goals that were done (0..1). */
    val progress: Float,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/** Monday–Sunday strip of completion rings with the date inside; today is highlighted. */
@Composable
fun WeekStrip(
    days: List<WeekDayUi>,
    modifier: Modifier = Modifier,
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    val colors = GkTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            val shape = RoundedCornerShape(16.dp)
            val base = Modifier
                .weight(1f)
                .clip(shape)
                .background(if (day.isToday) colors.accentContainer else Color.Transparent)
                .border(1.dp, if (day.isToday) colors.accent else Color.Transparent, shape)
            val clickable = if (onDayClick != null) base.clickable { onDayClick(day.date) } else base
            Column(
                modifier = clickable.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = GkTheme.mono.small.copy(fontSize = GkTheme.mono.meta.fontSize),
                    color = if (day.isToday) colors.onAccentContainer else colors.muted,
                )
                ProgressRing(
                    progress = if (day.isFuture) 0f else day.progress,
                    diameter = 36.dp,
                    strokeWidth = 3.5.dp,
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = GkTheme.mono.small,
                        color = if (day.isFuture) colors.subtle else colors.text,
                    )
                }
            }
        }
    }
}

/** Simple vertical bar chart (e.g. check-ins per week). Values are normalized to [maxValue]. */
@Composable
fun BarChart(
    values: List<Float>,
    maxValue: Float,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp,
    highlightLast: Boolean = true,
    description: String = "Bar chart",
) {
    val colors = GkTheme.colors
    Box(modifier = modifier.fillMaxWidth().height(height)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = description },
        ) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = min(slot * 0.62f, 22.dp.toPx())
            val radius = CornerRadius(barWidth * 0.3f, barWidth * 0.3f)
            values.forEachIndexed { index, raw ->
                val fraction = if (maxValue <= 0f) 0f else (raw / maxValue).coerceIn(0f, 1f)
                val barHeight = (size.height - 6.dp.toPx()) * fraction + 6.dp.toPx()
                val isLast = highlightLast && index == values.lastIndex
                drawRoundRect(
                    color = if (isLast) colors.accentContainer else colors.accent,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
        }
    }
}
