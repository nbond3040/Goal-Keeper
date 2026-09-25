package com.goalkeeper.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goalkeeper.app.ui.common.gkViewModelFactory
import com.goalkeeper.app.ui.components.GkTopBar
import com.goalkeeper.app.ui.theme.GkTheme
import kotlinx.coroutines.launch

/** App settings: appearance, reminders, backup and about. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = gkViewModelFactory { c ->
            SettingsViewModel(c.settingsRepository, c.reminderScheduler, c.notificationHelper, c.backupManager)
        },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    // The exact-alarm settings screen either exists or doesn't for this device/OS version; it won't change
    // for the lifetime of this screen, so we only need to ask for it once.
    val exactAlarmIntent = remember { viewModel.exactAlarmSettingsIntent() }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshStatus()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message -> scope.launch { snackbarHostState.showSnackbar(event.text) }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingImportUri = uri }

    Scaffold(
        containerColor = GkTheme.colors.background,
        topBar = { GkTopBar(title = "Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AppearanceSection(
                colorway = state.settings.colorway,
                themeMode = state.settings.themeMode,
                onColorwaySelected = viewModel::setColorway,
                onThemeModeSelected = viewModel::setThemeMode,
            )
            ReminderSettingsSection(
                defaultReminderMinute = state.settings.defaultReminderMinute,
                briefingEnabled = state.settings.briefingEnabled,
                briefingMinute = state.settings.briefingMinute,
                notificationsEnabled = state.notificationsEnabled,
                canScheduleExactAlarms = state.canScheduleExactAlarms,
                hasExactAlarmSettingsScreen = exactAlarmIntent != null,
                onDefaultReminderMinuteChanged = viewModel::setDefaultReminderMinute,
                onBriefingEnabledChanged = { enabled -> viewModel.setBriefing(enabled, state.settings.briefingMinute) },
                onBriefingMinuteChanged = { minute -> viewModel.setBriefing(state.settings.briefingEnabled, minute) },
                onOpenNotificationSettings = { context.startActivity(viewModel.appNotificationSettingsIntent()) },
                onOpenExactAlarmSettings = { exactAlarmIntent?.let(context::startActivity) },
                onSendTestNudge = viewModel::sendTestNudge,
            )
            BackupSection(
                onExport = { exportLauncher.launch(viewModel.suggestedBackupFileName()) },
                onImportSelected = {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
            )
            AboutSection()
        }
    }

    val importUri = pendingImportUri
    if (importUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Replace all data?") },
            text = { Text("Replace all current goals, check-ins and journal with this backup?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    viewModel.importBackup(importUri)
                }) { Text("Replace", color = GkTheme.colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            },
        )
    }
}
