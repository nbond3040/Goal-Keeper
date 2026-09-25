package com.goalkeeper.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goalkeeper.app.appContainer
import kotlinx.coroutines.launch

/**
 * Re-arms every alarm after events that can silently wipe or invalidate them: reboot, app update,
 * a manual clock/time-zone change, or the user flipping the exact-alarm permission in Settings.
 * [ReminderSync.syncAll] is idempotent, so no action-specific handling is needed here.
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        context.appContainer.applicationScope.launch {
            try {
                context.appContainer.reminderScheduler.syncAll()
            } finally {
                pending.finish()
            }
        }
    }
}
