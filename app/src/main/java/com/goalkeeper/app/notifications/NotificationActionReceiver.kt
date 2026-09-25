package com.goalkeeper.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goalkeeper.app.appContainer
import com.goalkeeper.core.model.CheckInStatus
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Handles the "Done" action button on a nudge notification. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return
        if (!intent.hasExtra(EXTRA_GOAL_ID) || !intent.hasExtra(EXTRA_EPOCH_DAY)) return

        val goalId = intent.getLongExtra(EXTRA_GOAL_ID, 0L)
        val epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, 0L)
        val container = context.appContainer
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                val date = LocalDate.ofEpochDay(epochDay)
                val today = container.clock.today()
                // Only "Done" the day the notification is about, and only if it's still today or
                // yesterday (an older, stale notification shouldn't silently back-fill a check-in).
                if (date == today || date == today.minusDays(1)) {
                    container.goalRepository.setCheckIn(goalId, date, CheckInStatus.DONE)
                }
                container.notificationHelper.cancelNudge(goalId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.goalkeeper.app.action.DONE"
        const val EXTRA_GOAL_ID = "goalId"
        const val EXTRA_EPOCH_DAY = "epochDay"
    }
}
