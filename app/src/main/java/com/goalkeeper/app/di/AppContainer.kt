package com.goalkeeper.app.di

import android.content.Context
import com.goalkeeper.app.data.backup.BackupManager
import com.goalkeeper.app.data.db.GoalKeeperDatabase
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.data.repo.JournalRepository
import com.goalkeeper.app.data.repo.RoomGoalRepository
import com.goalkeeper.app.data.repo.RoomJournalRepository
import com.goalkeeper.app.data.settings.DataStoreSettingsRepository
import com.goalkeeper.app.data.settings.SettingsRepository
import com.goalkeeper.app.notifications.NotificationHelper
import com.goalkeeper.app.notifications.ReminderScheduler
import com.goalkeeper.app.util.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency graph, created once by [com.goalkeeper.app.GoalKeeperApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** For work that must outlive a screen (e.g. re-arming alarms). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock = AppClock()

    val database: GoalKeeperDatabase by lazy { GoalKeeperDatabase.build(appContext) }

    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext) }

    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext, database, settingsRepository, notificationHelper, clock)
    }

    val goalRepository: GoalRepository by lazy { RoomGoalRepository(database, reminderScheduler, clock) }

    val journalRepository: JournalRepository by lazy { RoomJournalRepository(database, clock) }

    val backupManager: BackupManager by lazy {
        BackupManager(appContext, database, settingsRepository, reminderScheduler, clock)
    }
}
