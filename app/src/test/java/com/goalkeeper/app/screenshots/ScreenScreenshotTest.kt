package com.goalkeeper.app.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.goalkeeper.app.data.settings.AppSettings
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.ui.components.FloatingNavBar
import com.goalkeeper.app.ui.components.TopLevelTab
import com.goalkeeper.app.ui.goals.GoalsContent
import com.goalkeeper.app.ui.goals.buildGoalsUiState
import com.goalkeeper.app.ui.insights.InsightsContent
import com.goalkeeper.app.ui.insights.buildInsightsUiState
import com.goalkeeper.app.ui.journal.GoalChipInfo
import com.goalkeeper.app.ui.journal.JournalFeedContent
import com.goalkeeper.app.ui.journal.JournalFeedData
import com.goalkeeper.app.ui.journal.JournalFilter
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.app.ui.today.TodayContent
import com.goalkeeper.app.ui.today.buildTodayUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneOffset

/**
 * Renders the main screens with [SampleData] through the same state builders the ViewModels use.
 * Recorded into docs/screenshots by `./gradlew :app:recordRoborazziDebug` (CI does this on every push).
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class ScreenScreenshotTest {

    @Test
    fun today() = capture("today") {
        WithNav(TopLevelTab.TODAY) { TodayScreenSample() }
    }

    @Test
    fun todayLightEmber() = capture("today_light_ember", colorway = Colorway.EMBER_ORANGE, dark = false) {
        WithNav(TopLevelTab.TODAY) { TodayScreenSample() }
    }

    @Test
    fun goals() = capture("goals") {
        WithNav(TopLevelTab.GOALS) {
            GoalsContent(
                state = buildGoalsUiState(SampleData.today, SampleData.goals, emptyList(), SampleData.checkIns, ZoneOffset.UTC),
                selectedTab = 0,
                onSelectTab = {},
                onOpenGoal = {},
                onOpenRank = {},
                onCreateGoal = {},
                onRestore = {},
            )
        }
    }

    @Test
    fun insights() = capture("insights") {
        WithNav(TopLevelTab.INSIGHTS) {
            InsightsContent(
                state = buildInsightsUiState(SampleData.goals, SampleData.checkIns, SampleData.today),
                onOpenGoal = {},
            )
        }
    }

    @Test
    fun journal() = capture("journal") {
        WithNav(TopLevelTab.JOURNAL) {
            val entries = SampleData.journal.sortedByDescending { it.createdAt }
            JournalFeedContent(
                data = JournalFeedData(
                    entries = entries,
                    entryCount = entries.size,
                    goalCount = entries.map { it.goalId }.distinct().size,
                    goalChips = SampleData.goals.associate { it.id to GoalChipInfo(it.id, it.icon, it.title) },
                    zone = ZoneOffset.UTC,
                    today = SampleData.today,
                ),
                filter = JournalFilter.ALL,
                onSelectFilter = {},
                onOpenEntry = { _, _, _ -> },
                onOpenGoal = {},
                onTogglePin = {},
                onDeleteEntry = {},
                onSetItemDone = { _, _ -> },
                use24h = false,
            )
        }
    }

    private fun capture(
        name: String,
        colorway: Colorway = Colorway.BOLD_BLUE,
        dark: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        captureRoboImage(
            "../docs/screenshots/$name.png",
            RoborazziOptions(),
            RoborazziComposeOptions { size(411, 891) },
        ) {
            GoalKeeperTheme(colorway = colorway, darkTheme = dark) { content() }
        }
    }
}

/** Draws the floating pill nav over a top-level screen, like the real app shell does. */
@Composable
private fun WithNav(tab: TopLevelTab, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        FloatingNavBar(
            selected = tab,
            onSelect = {},
            onAdd = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TodayScreenSample() {
    TodayContent(
        state = buildTodayUiState(
            today = SampleData.today,
            goals = SampleData.goals,
            checkIns = SampleData.checkIns,
            settings = AppSettings(),
            notificationsEnabled = true,
            exactAlarmsAllowed = true,
            zone = ZoneOffset.UTC,
        ),
        onOpenGoal = {},
        onOpenRank = {},
        onOpenSettings = {},
        onCreateGoal = {},
        onCheckInGoal = {},
        onRemoveCheckIn = {},
        onSkipToday = {},
        onOpenNotificationSettings = {},
        onAllowExactAlarms = {},
        onDismissExactAlarmHint = {},
        snackbarHostState = remember { SnackbarHostState() },
    )
}
