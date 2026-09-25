package com.goalkeeper.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalkeeper.app.data.backup.BackupManager
import com.goalkeeper.app.data.settings.AppSettings
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.data.settings.SettingsRepository
import com.goalkeeper.app.data.settings.ThemeMode
import com.goalkeeper.app.notifications.NotificationHelper
import com.goalkeeper.app.notifications.ReminderScheduler
import com.goalkeeper.core.backup.BackupFormatException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val notificationsEnabled: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
)

/** One-shot snackbar messages consumed by [SettingsScreen]. */
sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
}

/** Notification / exact-alarm permission state, which is polled rather than observed as a flow. */
private data class DeviceStatus(val notificationsEnabled: Boolean, val canScheduleExactAlarms: Boolean)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val notificationHelper: NotificationHelper,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val status = MutableStateFlow(currentDeviceStatus())

    val uiState: StateFlow<SettingsUiState> = combine(settingsRepository.settings, status) { settings, st ->
        SettingsUiState(
            loading = false,
            settings = settings,
            notificationsEnabled = st.notificationsEnabled,
            canScheduleExactAlarms = st.canScheduleExactAlarms,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    private fun currentDeviceStatus() = DeviceStatus(
        notificationsEnabled = notificationHelper.areNotificationsEnabled(),
        canScheduleExactAlarms = reminderScheduler.canScheduleExactAlarms(),
    )

    /** Re-reads the notification / exact-alarm permission state; call this from onResume. */
    fun refreshStatus() {
        status.value = currentDeviceStatus()
    }

    fun setColorway(colorway: Colorway) {
        viewModelScope.launch { settingsRepository.setColorway(colorway) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDefaultReminderMinute(minute: Int) {
        viewModelScope.launch { settingsRepository.setDefaultReminderMinute(minute) }
    }

    fun setBriefing(enabled: Boolean, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setBriefing(enabled, minute)
            reminderScheduler.syncBriefing()
        }
    }

    fun suggestedBackupFileName(): String = backupManager.suggestedFileName()

    fun appNotificationSettingsIntent() = notificationHelper.appNotificationSettingsIntent()

    fun exactAlarmSettingsIntent() = reminderScheduler.exactAlarmSettingsIntent()

    fun sendTestNudge() {
        viewModelScope.launch {
            val sent = reminderScheduler.sendTestNudge()
            _events.send(SettingsEvent.Message(if (sent) "Test nudge sent" else "Notifications are blocked"))
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val summary = backupManager.exportTo(uri)
                _events.send(SettingsEvent.Message("Exported ${summary.goals} goals, ${summary.checkIns} check-ins"))
            } catch (e: BackupFormatException) {
                _events.send(SettingsEvent.Message(e.message ?: "Couldn't export that backup"))
            } catch (e: IOException) {
                _events.send(SettingsEvent.Message("Couldn't write the backup file"))
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val summary = backupManager.importFrom(uri)
                _events.send(
                    SettingsEvent.Message(
                        "Imported ${summary.goals} goals, ${summary.checkIns} check-ins, ${summary.journalEntries} journal entries",
                    ),
                )
            } catch (e: BackupFormatException) {
                _events.send(SettingsEvent.Message(e.message ?: "That file isn't a valid Goal Keeper backup"))
            } catch (e: IOException) {
                _events.send(SettingsEvent.Message("Couldn't read that file"))
            }
        }
    }
}
