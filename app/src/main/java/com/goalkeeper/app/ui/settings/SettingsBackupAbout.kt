package com.goalkeeper.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.BuildConfig
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SecondaryButton
import com.goalkeeper.app.ui.theme.GkTheme

/** "BACKUP" — export or import a JSON backup of goals, check-ins and journal. */
@Composable
internal fun BackupSection(onExport: () -> Unit, onImportSelected: () -> Unit) {
    val colors = GkTheme.colors
    GkCard {
        MonoLabel("BACKUP")
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Android's own device backup already covers Goal Keeper; use this for a manual copy you control.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "Export backup",
                icon = GkIcons.Backup,
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "Import backup",
                icon = GkIcons.Restore,
                onClick = onImportSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "ABOUT" — app version and font licensing. */
@Composable
internal fun AboutSection() {
    val colors = GkTheme.colors
    GkCard {
        MonoLabel("ABOUT")
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Goal Keeper ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text,
            )
            Text(
                text = "Fonts: Space Grotesk & JetBrains Mono (SIL Open Font License)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
    }
}
