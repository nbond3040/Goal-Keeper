package com.goalkeeper.app.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.data.settings.ThemeMode
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.goals.GoalRowEntry
import com.goalkeeper.app.ui.goals.RankList
import com.goalkeeper.app.ui.goals.TierHeaderEntry
import com.goalkeeper.app.ui.settings.AboutSection
import com.goalkeeper.app.ui.settings.AppearanceSection
import com.goalkeeper.app.ui.settings.BackupSection
import com.goalkeeper.app.ui.settings.ReminderSettingsSection
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.Importance
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Secondary screens: the Rank screen, and the settings sections stacked on a canvas tall enough for all of them. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class SecondaryScreensScreenshotTest {

    @Test
    fun rank() {
        val entries = Importance.entries.reversed().flatMap { tier ->
            listOf(TierHeaderEntry(tier)) + SampleData.goals.filter { it.importance == tier }.map { GoalRowEntry(it) }
        }
        captureRoboImage("../docs/screenshots/rank.png", RoborazziOptions(), RoborazziComposeOptions { size(411, 891) }) {
            GoalKeeperTheme(darkTheme = true) {
                Column(modifier = Modifier.fillMaxSize().background(GkTheme.colors.background)) {
                    GkTopBar(title = "Rank goals", subtitle = "Drag to reorder · drop into a tier", onBack = {}) {
                        TextButton(onClick = {}) { Text("Save", color = GkTheme.colors.accentText) }
                    }
                    RankList(
                        entries = entries,
                        onMove = { _, _ -> },
                        onSetTier = { _, _ -> },
                        onMoveUp = {},
                        onMoveDown = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    @Test
    fun settings() {
        captureRoboImage(
            "../docs/screenshots/settings_full.png",
            RoborazziOptions(),
            RoborazziComposeOptions { size(411, 1500) },
        ) {
            GoalKeeperTheme(darkTheme = true) {
                Column(modifier = Modifier.fillMaxSize().background(GkTheme.colors.background)) {
                    GkTopBar(title = "Settings", onBack = {})
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        AppearanceSection(
                            colorway = Colorway.BOLD_BLUE,
                            themeMode = ThemeMode.DARK,
                            onColorwaySelected = {},
                            onThemeModeSelected = {},
                        )
                        ReminderSettingsSection(
                            defaultReminderMinute = 18 * 60,
                            briefingEnabled = true,
                            briefingMinute = 8 * 60,
                            notificationsEnabled = true,
                            canScheduleExactAlarms = false,
                            hasExactAlarmSettingsScreen = true,
                            onDefaultReminderMinuteChanged = {},
                            onBriefingEnabledChanged = {},
                            onBriefingMinuteChanged = {},
                            onOpenNotificationSettings = {},
                            onOpenExactAlarmSettings = {},
                            onSendTestNudge = {},
                        )
                        BackupSection(onExport = {}, onImportSelected = {})
                        AboutSection()
                    }
                }
            }
        }
    }
}
