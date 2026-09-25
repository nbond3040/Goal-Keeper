package com.goalkeeper.app

import android.app.Application
import android.content.Context
import com.goalkeeper.app.di.AppContainer
import kotlinx.coroutines.launch

class GoalKeeperApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.createChannels()
        // Alarms are wiped by force-stop and some OEM task killers; re-arm on every process start.
        container.applicationScope.launch { container.reminderScheduler.syncAll() }
    }
}

/** The app's dependency container, reachable from any [Context]. */
val Context.appContainer: AppContainer
    get() = (applicationContext as GoalKeeperApp).container
