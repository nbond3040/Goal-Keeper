package com.goalkeeper.app.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.goalkeeper.app.ui.components.GkIconButton
import com.goalkeeper.app.ui.components.GkIcons
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.detail.GoalDetailContent
import com.goalkeeper.app.ui.detail.buildGoalDetailData
import com.goalkeeper.app.ui.journal.JournalFilter
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * The goal detail screen for [SampleData.run], built through the same [buildGoalDetailData] the ViewModel uses.
 * The `_full` variants use a tall canvas so the whole scrolling screen is visible at once.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class DetailScreenshotTest {

    @Test
    fun detail() = capture("detail", height = 891, selectedTab = 0)

    @Test
    fun detailFull() = capture("detail_full", height = 2300, selectedTab = 0)

    @Test
    fun goalJournalFull() = capture("goal_journal_full", height = 1700, selectedTab = 1)

    private fun capture(name: String, height: Int, selectedTab: Int) {
        captureRoboImage(
            "../docs/screenshots/$name.png",
            RoborazziOptions(),
            RoborazziComposeOptions { size(411, height) },
        ) {
            GoalKeeperTheme(darkTheme = true) { DetailSample(selectedTab) }
        }
    }
}

@Composable
private fun DetailSample(selectedTab: Int) {
    val goal = SampleData.run
    val entries = SampleData.journal.filter { it.goalId == goal.id }.sortedByDescending { it.createdAt }
    val data = buildGoalDetailData(
        goal = goal,
        checkIns = SampleData.checkIns.filter { it.goalId == goal.id },
        entries = entries,
        entryCount = entries.size,
        today = SampleData.today,
        nowTime = LocalTime.of(17, 30),
        zone = ZoneOffset.UTC,
    )
    Column(modifier = Modifier.fillMaxSize().background(GkTheme.colors.background)) {
        GkTopBar(title = goal.title, onBack = {}) {
            GkIconButton(icon = GkIcons.Edit, contentDescription = "Edit goal", onClick = {})
            GkIconButton(icon = GkIcons.More, contentDescription = "More options", onClick = {})
        }
        GoalDetailContent(
            data = data,
            selectedTab = selectedTab,
            onSelectTab = {},
            journalFilter = JournalFilter.ALL,
            onSelectJournalFilter = {},
            composerText = "",
            onComposerTextChange = {},
            onSendQuickEntry = {},
            onCheckIn = {},
            onToggleDay = {},
            onEditGoal = {},
            onOpenEntry = { _, _ -> },
            onTogglePin = {},
            onDeleteEntry = {},
            onSetItemDone = { _, _ -> },
            use24h = false,
            modifier = Modifier.weight(1f),
        )
    }
}
