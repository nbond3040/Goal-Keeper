package com.goalkeeper.app.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.goalkeeper.app.appContainer
import com.goalkeeper.app.data.settings.AppSettings
import com.goalkeeper.app.ui.components.FloatingNavBar
import com.goalkeeper.app.ui.components.TopLevelTab
import com.goalkeeper.app.ui.detail.GoalDetailScreen
import com.goalkeeper.app.ui.editor.GoalEditorScreen
import com.goalkeeper.app.ui.goals.GoalsScreen
import com.goalkeeper.app.ui.goals.RankScreen
import com.goalkeeper.app.ui.insights.InsightsScreen
import com.goalkeeper.app.ui.journal.JournalEditorScreen
import com.goalkeeper.app.ui.journal.JournalFeedScreen
import com.goalkeeper.app.ui.settings.SettingsScreen
import com.goalkeeper.app.ui.theme.GkTheme
import com.goalkeeper.app.ui.today.TodayScreen
import com.goalkeeper.core.model.JournalEntryType
import kotlinx.coroutines.launch

/**
 * The app shell: navigation graph plus the floating pill nav on top-level screens.
 * [pendingGoalId] is a goal to open (from a notification tap); [onPendingGoalHandled] clears it.
 */
@Composable
fun GoalKeeperRoot(
    settings: AppSettings,
    pendingGoalId: Long?,
    onPendingGoalHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.topLevelTab()

    NotificationPermissionPrompt(settings)

    LaunchedEffect(pendingGoalId) {
        if (pendingGoalId != null) {
            navController.navigate(GoalDetailRoute(pendingGoalId)) { launchSingleTop = true }
            onPendingGoalHandled()
        }
    }

    val openEntry: (Long, Long, JournalEntryType) -> Unit = { goalId, entryId, type ->
        navController.navigate(JournalEditorRoute(goalId = goalId, entryId = entryId, type = type.name))
    }
    val openGoal: (Long) -> Unit = { goalId -> navController.navigate(GoalDetailRoute(goalId)) }
    val createGoal: () -> Unit = { navController.navigate(GoalEditorRoute()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GkTheme.colors.background),
    ) {
        NavHost(navController = navController, startDestination = TodayRoute) {
            composable<TodayRoute> {
                TodayScreen(
                    onOpenGoal = openGoal,
                    onOpenRank = { navController.navigate(RankRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onCreateGoal = createGoal,
                )
            }
            composable<GoalsRoute> {
                GoalsScreen(
                    onOpenGoal = openGoal,
                    onOpenRank = { navController.navigate(RankRoute) },
                    onCreateGoal = createGoal,
                )
            }
            composable<InsightsRoute> {
                InsightsScreen(onOpenGoal = openGoal)
            }
            composable<JournalFeedRoute> {
                JournalFeedScreen(onOpenEntry = openEntry, onOpenGoal = openGoal)
            }
            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<RankRoute> {
                RankScreen(onBack = { navController.popBackStack() })
            }
            composable<GoalDetailRoute> {
                GoalDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { goalId -> navController.navigate(GoalEditorRoute(goalId)) },
                    onOpenEntry = openEntry,
                )
            }
            composable<GoalEditorRoute> {
                GoalEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { goalId, isNew ->
                        if (isNew) {
                            navController.navigate(GoalDetailRoute(goalId)) {
                                popUpTo<GoalEditorRoute> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onDeleted = {
                        // Leave both the editor and the deleted goal's detail screen.
                        if (!navController.popBackStack<GoalDetailRoute>(inclusive = true)) {
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable<JournalEditorRoute> {
                JournalEditorScreen(onBack = { navController.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = currentTab != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            FloatingNavBar(
                selected = currentTab,
                onSelect = { tab -> navController.navigateToTab(tab) },
                onAdd = createGoal,
            )
        }
    }
}

private fun NavDestination.topLevelTab(): TopLevelTab? = when {
    hasRoute(TodayRoute::class) -> TopLevelTab.TODAY
    hasRoute(GoalsRoute::class) -> TopLevelTab.GOALS
    hasRoute(InsightsRoute::class) -> TopLevelTab.INSIGHTS
    hasRoute(JournalFeedRoute::class) -> TopLevelTab.JOURNAL
    else -> null
}

private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    val route: Any = when (tab) {
        TopLevelTab.TODAY -> TodayRoute
        TopLevelTab.GOALS -> GoalsRoute
        TopLevelTab.INSIGHTS -> InsightsRoute
        TopLevelTab.JOURNAL -> JournalFeedRoute
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Asks for the notification permission once (Android 13+); reminders are the core of the app. */
@Composable
private fun NotificationPermissionPrompt(settings: AppSettings) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    // Survives recreation so a rotation while the dialog is up doesn't ask twice.
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch {
            container.settingsRepository.setNotificationsPrompted(true)
            if (granted) container.reminderScheduler.syncAll()
        }
    }
    LaunchedEffect(settings.notificationsPrompted) {
        if (!settings.notificationsPrompted && !requested) {
            requested = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
