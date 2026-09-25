package com.goalkeeper.app.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.editor.GoalEditorContent
import com.goalkeeper.app.ui.editor.GoalEditorUiState
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.model.Schedule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The goal editor, rendered tall enough to show the whole form. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class EditorScreenshotTest {

    @Test
    fun editor() {
        val state = GoalEditorUiState(
            loading = false,
            isNew = true,
            today = SampleData.today,
            title = "Run a half marathon",
            why = "Prove to myself I can finish what I start — and feel strong at 40.",
            icon = GoalIcon.RUN,
            importance = Importance.CRITICAL,
            scheduleMask = Schedule.DAILY.mask,
            targetDate = LocalDate.of(2027, 4, 12),
            reminder = ReminderConfig(enabled = true, startMinuteOfDay = 18 * 60, nudgeCount = 4, intervalMinutes = 60),
        )
        captureRoboImage(
            "../docs/screenshots/editor.png",
            RoborazziOptions(),
            RoborazziComposeOptions { size(411, 2100) },
        ) {
            GoalKeeperTheme(darkTheme = true) {
                Column(modifier = Modifier.fillMaxSize().background(GkTheme.colors.background)) {
                    GkTopBar(title = "New goal", onBack = {}) {
                        TextButton(onClick = {}) { Text("Save", color = GkTheme.colors.accentText) }
                    }
                    GoalEditorContent(
                        state = state,
                        onTitleChange = {},
                        onWhyChange = {},
                        onIconSelected = {},
                        onImportanceSelected = {},
                        onQuickSchedule = {},
                        onToggleDay = {},
                        onTargetDateSelected = {},
                        onRemindersEnabledChanged = {},
                        onReminderStartMinuteChanged = {},
                        onIncrementNudges = {},
                        onDecrementNudges = {},
                        onIntervalSelected = {},
                        onMarkAchieved = {},
                        onArchive = {},
                        onRestore = {},
                        onDelete = {},
                    )
                }
            }
        }
    }
}
