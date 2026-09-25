package com.goalkeeper.app.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.ui.components.CheckInPill
import com.goalkeeper.app.ui.components.GoalRingIcon
import com.goalkeeper.app.ui.components.MonoLabel
import com.goalkeeper.app.ui.components.SignalBars
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the colorway gallery into docs/screenshots (recorded by `./gradlew :app:recordRoborazziDebug`). */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class DesignSystemScreenshotTest {

    @Test
    fun colorways() {
        captureRoboImage(
            "../docs/screenshots/colorways.png",
            RoborazziOptions(),
            // Positional (widthDp, heightDp): parameter names aren't part of the verified API surface.
            RoborazziComposeOptions { size(760, 1180) },
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Colorway.entries.forEach { ColorwaySample(it, dark = true) }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Colorway.entries.forEach { ColorwaySample(it, dark = false) }
                }
            }
        }
    }
}

@Composable
private fun ColorwaySample(colorway: Colorway, dark: Boolean) {
    GoalKeeperTheme(colorway = colorway, darkTheme = dark) {
        val colors = GkTheme.colors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MonoLabel("${colorway.displayName} · ${if (dark) "dark" else "light"}", color = colors.accentText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.card)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignalBars(Importance.CRITICAL)
                Spacer(Modifier.width(10.dp))
                GoalRingIcon(icon = GoalIcon.RUN, progress = 0.57f)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Run a half marathon", color = colors.text)
                    Text("42d streak · nudge 18:00", style = GkTheme.mono.meta, color = colors.muted)
                }
                CheckInPill(status = if (dark) DayStatus.PENDING else DayStatus.DONE, onClick = {})
            }
        }
    }
}
