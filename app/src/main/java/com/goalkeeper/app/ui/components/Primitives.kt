package com.goalkeeper.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance

/** A console panel: card color, 1dp border, rounded corners. Clickable when [onClick] is set. */
@Composable
fun GkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(22.dp),
    color: Color = GkTheme.colors.card,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GkTheme.colors
    val border = BorderStroke(1.dp, colors.border)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = colors.text,
            border = border,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = colors.text,
            border = border,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Uppercase monospace label, e.g. "PRIORITY QUEUE". */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GkTheme.colors.muted,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = GkTheme.mono.label,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A mono section label with an optional accent-colored text action on the right. */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MonoLabel(label)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = GkTheme.colors.accentText)
            }
        }
    }
}

/** Importance as 1–4 filled "signal" bars. */
@Composable
fun SignalBars(
    importance: Importance,
    modifier: Modifier = Modifier,
    activeColor: Color = GkTheme.colors.accent,
    inactiveColor: Color = GkTheme.colors.border,
) {
    val heights = listOf(6.dp, 9.dp, 12.dp, 16.dp)
    Row(
        modifier = modifier
            .height(16.dp)
            .semantics { contentDescription = "${importance.label} importance" },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { index, height ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < importance.level) activeColor else inactiveColor),
            )
        }
    }
}

/** A circular progress ring (0..1) with optional centered content. Animates changes. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
    strokeWidth: Dp = 3.dp,
    trackColor: Color = GkTheme.colors.border,
    progressColor: Color = GkTheme.colors.accent,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "ringProgress")
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (animated > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** The goal's icon inside a progress ring (the priority-queue row avatar). */
@Composable
fun GoalRingIcon(
    icon: GoalIcon,
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
) {
    ProgressRing(progress = progress, modifier = modifier, diameter = diameter) {
        Icon(
            imageVector = GkIcons.forGoal(icon),
            contentDescription = null,
            tint = GkTheme.colors.text,
            modifier = Modifier.size(diameter * 0.42f),
        )
    }
}

/** The goal's icon on a tinted rounded tile. */
@Composable
fun GoalIconTile(
    icon: GoalIcon,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    container: Color = GkTheme.colors.accentContainer,
    tint: Color = GkTheme.colors.accentText,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(GkIcons.forGoal(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * The check-in button of a goal row: outlined "Check in" when pending, filled "Done" once checked in,
 * muted "Skipped" for an excused day, and a quiet "Rest" on unscheduled days.
 */
@Composable
fun CheckInPill(
    status: DayStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    val done = status == DayStatus.DONE
    val (container, content, border) = when (status) {
        DayStatus.DONE -> Triple(colors.accent, colors.onAccent, colors.accent)
        DayStatus.SKIPPED, DayStatus.REST -> Triple(Color.Transparent, colors.muted, colors.border)
        else -> Triple(Color.Transparent, colors.accentText, colors.accentText)
    }
    val label = when (status) {
        DayStatus.DONE -> "Done"
        DayStatus.SKIPPED -> "Skipped"
        DayStatus.REST -> "Rest"
        else -> "Check in"
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(1.5.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (done) Icon(GkIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** A small stat panel: big monospace value over a mono label. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = GkTheme.colors.text,
) {
    GkCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(value, style = GkTheme.mono.value, color = valueColor, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        MonoLabel(label)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = GkTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.raised,
            disabledContentColor = colors.subtle,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = GkTheme.colors.text,
) {
    val colors = GkTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor, disabledContentColor = colors.subtle),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

/** The segmented control from the mockups (e.g. All / Log / Tasks / Notes). */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.raised)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) colors.accent else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.onAccent else colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A 44dp round icon button. */
@Composable
fun GkIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GkTheme.colors.text,
    container: Color = Color.Transparent,
    border: BorderStroke? = null,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = if (enabled) tint else GkTheme.colors.subtle.copy(alpha = 0.5f),
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Top bar for pushed screens: back button, title with optional mono subtitle, trailing actions.
 * Handles the status-bar inset itself, so use it as a Scaffold `topBar`.
 */
@Composable
fun GkTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = GkTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            GkIconButton(icon = GkIcons.Back, contentDescription = "Back", onClick = onBack)
        } else {
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = GkTheme.mono.meta,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Centered placeholder for empty lists. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = GkIcons.Goals,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = GkTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(colors.accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(34.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.text, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.muted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            PrimaryButton(text = actionLabel, onClick = onAction, icon = GkIcons.Add)
        }
    }
}
