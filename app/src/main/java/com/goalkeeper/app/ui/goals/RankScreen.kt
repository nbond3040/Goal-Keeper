package com.goalkeeper.app.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.components.GoalIconTile
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SignalBars
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.Schedule
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder ranking screen. Long-press anywhere on a row (or press-drag its handle) to pick it up;
 * dropping it past a tier header moves it into that tier. A tier chip and "Move up"/"Move down"
 * accessibility actions offer a non-drag path to the same result.
 */
@Composable
fun RankScreen(
    onBack: () -> Unit,
    viewModel: RankViewModel = viewModel(
        factory = gkViewModelFactory { c -> RankViewModel(c.goalRepository) },
    ),
) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }

    val attemptBack: () -> Unit = {
        if (viewModel.hasChanges) showDiscardDialog = true else onBack()
    }

    BackHandler(onBack = attemptBack)

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your re-ranking hasn't been saved.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }

    Scaffold(
        containerColor = GkTheme.colors.background,
        topBar = {
            GkTopBar(
                title = "Rank goals",
                subtitle = "Drag to reorder · drop into a tier",
                onBack = attemptBack,
                actions = {
                    TextButton(onClick = { viewModel.save(onBack) }) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge,
                            color = GkTheme.colors.accentText,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            RankList(
                entries = viewModel.entries,
                onMove = viewModel::move,
                onSetTier = viewModel::setTier,
                onMoveUp = viewModel::moveUp,
                onMoveDown = viewModel::moveDown,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }
}

@Composable
internal fun RankList(
    entries: List<RankEntry>,
    onMove: (Int, Int) -> Unit,
    onSetTier: (Long, Importance) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    // The drag gesture runs in a coroutine that outlives most recompositions (it only restarts if a row's
    // key changes), so it must read the *current* list/callback rather than the ones captured when it started.
    val latestEntries by rememberUpdatedState(entries)
    val latestOnMove by rememberUpdatedState(onMove)

    fun handleDrag(key: Any, deltaY: Float) {
        dragOffset += deltaY
        val layoutInfo = listState.layoutInfo
        val draggedInfo = layoutInfo.visibleItemsInfo.find { it.key == key } ?: return
        val draggedTop = draggedInfo.offset + dragOffset
        val draggedCenter = draggedTop + draggedInfo.size / 2f

        val currentIndex = latestEntries.indexOfFirst { it.key == key }
        val targetInfo = layoutInfo.visibleItemsInfo.firstOrNull { info ->
            info.key != key && draggedCenter >= info.offset && draggedCenter < info.offset + info.size
        }
        if (targetInfo != null && currentIndex >= 0) {
            val targetIndex = latestEntries.indexOfFirst { it.key == targetInfo.key }
            // Index 0 is the Critical header: goals can't be dropped above it.
            if (targetIndex >= 1 && targetIndex != currentIndex) {
                // Keep the item's position relative to the finger continuous across the index change.
                dragOffset += (draggedInfo.offset - targetInfo.offset).toFloat()
                latestOnMove(currentIndex, targetIndex)
            }
        }

        val edgeZone = 72f
        val viewportStart = layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        when {
            draggedTop < viewportStart + edgeZone -> scope.launch { listState.scrollBy(-24f) }
            draggedTop + draggedInfo.size > viewportEnd - edgeZone -> scope.launch { listState.scrollBy(24f) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            when (entry) {
                is TierHeaderEntry -> {
                    item(key = entry.key) {
                        TierHeaderRow(
                            importance = entry.importance,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 16.dp, bottom = 8.dp),
                        )
                    }
                    val next = entries.getOrNull(index + 1)
                    if (next == null || next is TierHeaderEntry) {
                        item(key = "${entry.key}|empty") {
                            EmptyTierPlaceholder(modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                }
                is GoalRowEntry -> {
                    item(key = entry.key) {
                        val isDragging = draggingKey == entry.key
                        DraggableGoalRow(
                            entry = entry,
                            isDragging = isDragging,
                            dragOffsetY = if (isDragging) dragOffset else 0f,
                            rowModifier = if (isDragging) Modifier else Modifier.animateItem(),
                            onTierSelected = { tier -> onSetTier(entry.goal.id, tier) },
                            onMoveUpAction = { onMoveUp(entry.goal.id) },
                            onMoveDownAction = { onMoveDown(entry.goal.id) },
                            onDragStart = { draggingKey = entry.key; dragOffset = 0f },
                            onDragDelta = { delta -> handleDrag(entry.key, delta) },
                            onDragEnd = { draggingKey = null; dragOffset = 0f },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierHeaderRow(importance: Importance, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoLabel(text = importance.label)
        SignalBars(importance = importance)
    }
}

@Composable
private fun EmptyTierPlaceholder(modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val inset = strokeWidth / 2f
                drawRoundRect(
                    color = colors.border,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Drop a goal here", style = MaterialTheme.typography.bodySmall, color = colors.subtle)
    }
}

/** One draggable goal row: handle, icon, title, and a tier chip; long-press or handle-drag to reorder. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableGoalRow(
    entry: GoalRowEntry,
    isDragging: Boolean,
    dragOffsetY: Float,
    rowModifier: Modifier,
    onTierSelected: (Importance) -> Unit,
    onMoveUpAction: () -> Unit,
    onMoveDownAction: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colors = GkTheme.colors
    val goal = entry.goal
    var tierMenuExpanded by remember { mutableStateOf(false) }

    GkCard(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .graphicsLayer {
                translationY = dragOffsetY
                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                shape = RoundedCornerShape(22.dp)
                clip = isDragging
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Move up") { onMoveUpAction(); true },
                    CustomAccessibilityAction("Move down") { onMoveDownAction(); true },
                )
            }
            .pointerInput(entry.key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount -> change.consume(); onDragDelta(dragAmount.y) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = GkIcons.DragHandle,
                contentDescription = "Drag to reorder",
                tint = colors.subtle,
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(entry.key) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount -> change.consume(); onDragDelta(dragAmount.y) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        )
                    },
            )
            GoalIconTile(icon = goal.icon, size = 40.dp)
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box {
                ImportanceChip(importance = goal.importance, onClick = { tierMenuExpanded = true })
                DropdownMenu(expanded = tierMenuExpanded, onDismissRequest = { tierMenuExpanded = false }) {
                    Importance.descending.forEach { tier ->
                        DropdownMenuItem(
                            text = { Text(tier.label) },
                            onClick = { tierMenuExpanded = false; onTierSelected(tier) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportanceChip(importance: Importance, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = GkTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.raised,
        contentColor = colors.text,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = importance.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Icon(imageVector = GkIcons.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Preview
@Composable
private fun RankScreenPreview() {
    GoalKeeperTheme {
        Scaffold(containerColor = GkTheme.colors.background) { innerPadding ->
            RankList(
                entries = fakeRankEntries(),
                onMove = { _, _ -> },
                onSetTier = { _, _ -> },
                onMoveUp = {},
                onMoveDown = {},
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }
}

private fun fakeRankEntries(): List<RankEntry> {
    fun goal(id: Long, title: String, importance: Importance) = Goal(
        id = id,
        title = title,
        icon = GoalIcon.RUN,
        importance = importance,
        schedule = Schedule.DAILY,
        startDate = LocalDate.of(2026, 7, 1),
        createdAt = Instant.now(),
    )
    return listOf(
        TierHeaderEntry(Importance.CRITICAL),
        GoalRowEntry(goal(1, "Run a half marathon", Importance.CRITICAL)),
        TierHeaderEntry(Importance.HIGH),
        GoalRowEntry(goal(2, "Learn Spanish", Importance.HIGH)),
        GoalRowEntry(goal(3, "Write the novel", Importance.HIGH)),
        TierHeaderEntry(Importance.MEDIUM),
        TierHeaderEntry(Importance.LOW),
        GoalRowEntry(goal(4, "Read 30 minutes", Importance.LOW)),
    )
}
