package com.goalkeeper.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SecondaryButton
import com.goalkeeper.app.ui.theme.GkTheme

/** Section 7 (edit mode only) — achieve, archive/restore and delete. */
@Composable
internal fun GoalManageSection(
    title: String,
    archived: Boolean,
    onMarkAchieved: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = GkTheme.colors
    var showAchievedConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    GkCard {
        MonoLabel("MANAGE")
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!archived) {
                SecondaryButton(
                    text = "Mark as achieved",
                    icon = GkIcons.Trophy,
                    onClick = { showAchievedConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Archive",
                    icon = GkIcons.Archive,
                    onClick = onArchive,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SecondaryButton(
                    text = "Restore",
                    icon = GkIcons.Unarchive,
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SecondaryButton(
                text = "Delete goal",
                icon = GkIcons.Delete,
                onClick = { showDeleteConfirm = true },
                contentColor = colors.danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showAchievedConfirm) {
        AlertDialog(
            onDismissRequest = { showAchievedConfirm = false },
            title = { Text("Mark as achieved?") },
            text = { Text("\"$title\" will move to your archive as a completed goal.") },
            confirmButton = {
                TextButton(onClick = { showAchievedConfirm = false; onMarkAchieved() }) {
                    Text("Mark as achieved")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAchievedConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete goal?") },
            text = { Text("Delete this goal, its streak history and journal? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
