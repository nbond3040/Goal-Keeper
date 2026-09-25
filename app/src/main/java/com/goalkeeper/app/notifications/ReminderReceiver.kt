package com.goalkeeper.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goalkeeper.app.appContainer
import kotlinx.coroutines.launch

/**
 * Fires when a nudge or briefing alarm goes off. Explicit alarms only (see [ReminderScheduler]),
 * so [onReceive] can trust [Intent.getAction] to be one of the two actions declared below.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_NUDGE -> {
                if (!intent.hasExtra(EXTRA_GOAL_ID) ||
                    !intent.hasExtra(EXTRA_EPOCH_DAY) ||
                    !intent.hasExtra(EXTRA_INDEX) ||
                    !intent.hasExtra(EXTRA_COUNT)
                ) {
                    return
                }
                val goalId = intent.getLongExtra(EXTRA_GOAL_ID, 0L)
                val epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, 0L)
                val index = intent.getIntExtra(EXTRA_INDEX, 0)
                val count = intent.getIntExtra(EXTRA_COUNT, 0)
                val scheduler = context.appContainer.reminderScheduler
                val pending = goAsync()
                context.appContainer.applicationScope.launch {
                    try {
                        scheduler.handleNudgeAlarm(goalId, epochDay, index, count)
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_BRIEFING -> {
                val scheduler = context.appContainer.reminderScheduler
                val pending = goAsync()
                context.appContainer.applicationScope.launch {
                    try {
                        scheduler.handleBriefingAlarm()
                    } finally {
                        pending.finish()
                    }
                }
            }

            else -> Unit
        }
    }

    companion object {
        const val ACTION_NUDGE = "com.goalkeeper.app.action.NUDGE"
        const val ACTION_BRIEFING = "com.goalkeeper.app.action.BRIEFING"
        const val EXTRA_GOAL_ID = "goalId"
        const val EXTRA_EPOCH_DAY = "epochDay"
        const val EXTRA_INDEX = "index"
        const val EXTRA_COUNT = "count"
    }
}
