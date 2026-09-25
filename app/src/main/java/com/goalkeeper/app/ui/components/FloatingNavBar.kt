package com.goalkeeper.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goalkeeper.app.ui.theme.GkTheme

enum class TopLevelTab(val label: String, val icon: ImageVector) {
    TODAY("Today", GkIcons.Today),
    GOALS("Goals", GkIcons.Goals),
    INSIGHTS("Insights", GkIcons.Insights),
    JOURNAL("Journal", GkIcons.Journal),
}

/**
 * Space a top-level screen must leave at the bottom of its scrolling content so the floating nav bar
 * doesn't cover it (add the navigation-bar inset on top of this).
 */
val FloatingNavClearance = 104.dp

/** The Midnight Console pill navigation: Today · Goals · [+] · Insights · Journal. */
@Composable
fun FloatingNavBar(
    selected: TopLevelTab?,
    onSelect: (TopLevelTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GkTheme.colors
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(66.dp),
        shape = RoundedCornerShape(33.dp),
        color = colors.raised,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(TopLevelTab.TODAY, selected == TopLevelTab.TODAY, onSelect)
            NavItem(TopLevelTab.GOALS, selected == TopLevelTab.GOALS, onSelect)
            Surface(
                onClick = onAdd,
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = colors.accent,
                contentColor = colors.onAccent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(GkIcons.Add, contentDescription = "New goal", modifier = Modifier.size(26.dp))
                }
            }
            NavItem(TopLevelTab.INSIGHTS, selected == TopLevelTab.INSIGHTS, onSelect)
            NavItem(TopLevelTab.JOURNAL, selected == TopLevelTab.JOURNAL, onSelect)
        }
    }
}

@Composable
private fun NavItem(tab: TopLevelTab, selected: Boolean, onSelect: (TopLevelTab) -> Unit) {
    val colors = GkTheme.colors
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accentContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
            .semantics { contentDescription = tab.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = if (selected) colors.onAccentContainer else colors.muted,
            modifier = Modifier.size(24.dp),
        )
    }
}
