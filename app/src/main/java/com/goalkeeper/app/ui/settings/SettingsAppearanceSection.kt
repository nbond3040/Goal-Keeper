package com.goalkeeper.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.data.settings.ThemeMode
import com.goalkeeper.app.ui.components.GkCard
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SegmentedTabs
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.gkColors
import com.goalkeeper.app.ui.theme.resolveDarkTheme

/** "APPEARANCE" — an 8-way colorway grid and the light/dark/system theme mode. */
@Composable
internal fun AppearanceSection(
    colorway: Colorway,
    themeMode: ThemeMode,
    onColorwaySelected: (Colorway) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    val dark = resolveDarkTheme(themeMode)
    GkCard(modifier = Modifier.fillMaxWidth()) {
        MonoLabel("APPEARANCE")
        Spacer(Modifier.height(14.dp))
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Colorway.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { way ->
                        ColorwayTile(
                            colorway = way,
                            dark = dark,
                            selected = way == colorway,
                            onClick = { onColorwaySelected(way) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        MonoLabel("THEME")
        Spacer(Modifier.height(10.dp))
        SegmentedTabs(
            options = ThemeMode.entries.map { it.label },
            selectedIndex = ThemeMode.entries.indexOf(themeMode),
            onSelect = { index -> onThemeModeSelected(ThemeMode.entries[index]) },
        )
    }
}

@Composable
private fun ColorwayTile(
    colorway: Colorway,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    val accent = remember(colorway, dark) { gkColors(colorway, dark).accent }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.raised)
            .border(1.5.dp, if (selected) colors.accent else Color.Transparent, RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(36.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(accent),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(1.dp, colors.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(GkIcons.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(10.dp))
                }
            }
        }
        Text(
            text = colorway.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = colors.text,
            // Two lines for every tile, so "Midnight Navy" wraps instead of truncating and the tiles stay level.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
