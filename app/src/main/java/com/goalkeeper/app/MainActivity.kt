package com.goalkeeper.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goalkeeper.app.data.settings.AppSettings
import com.goalkeeper.app.ui.navigation.GoalKeeperRoot
import com.goalkeeper.app.ui.theme.GoalKeeperTheme
import com.goalkeeper.app.ui.theme.resolveDarkTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** A goal to open once the UI is ready (set from notification taps). */
    private val pendingGoalId = MutableStateFlow<Long?>(null)

    @Volatile
    private var settingsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !settingsLoaded }
        if (savedInstanceState == null) handleIntent(intent)

        val settingsFlow = appContainer.settingsRepository.settings
        setContent {
            val settings: AppSettings? by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            val current = settings ?: return@setContent
            SideEffect { settingsLoaded = true }

            val dark = resolveDarkTheme(current.themeMode)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
                onDispose { }
            }

            GoalKeeperTheme(colorway = current.colorway, darkTheme = dark) {
                val goalToOpen by pendingGoalId.collectAsStateWithLifecycle()
                GoalKeeperRoot(
                    settings = current,
                    pendingGoalId = goalToOpen,
                    onPendingGoalHandled = { pendingGoalId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val goalId = intent?.getLongExtra(AppIntents.EXTRA_GOAL_ID, 0L) ?: 0L
        if (goalId > 0L) pendingGoalId.value = goalId
    }
}
