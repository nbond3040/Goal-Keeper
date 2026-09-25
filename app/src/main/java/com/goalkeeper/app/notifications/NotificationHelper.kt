package com.goalkeeper.app.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.goalkeeper.app.AppIntents
import com.goalkeeper.app.R
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.reminder.NudgeText
import java.time.LocalDate

/**
 * Builds and posts every notification the app shows (nudges, the morning briefing) and the
 * notification channels they live in. Pure Android plumbing: no database or business-rule
 * decisions live here, those belong to [ReminderScheduler].
 */
class NotificationHelper(private val context: Context) {

    /** Creates the app's three notification channels. Safe to call repeatedly (updates are no-ops). */
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val important = NotificationChannel(
            CHANNEL_IMPORTANT,
            context.getString(R.string.notif_channel_important_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_important_desc)
        }
        val nudges = NotificationChannel(
            CHANNEL_NUDGES,
            context.getString(R.string.notif_channel_nudges_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_nudges_desc)
        }
        val briefing = NotificationChannel(
            CHANNEL_BRIEFING,
            context.getString(R.string.notif_channel_briefing_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_briefing_desc)
        }
        manager.createNotificationChannels(listOf(important, nudges, briefing))
    }

    /** Whether the app is currently allowed to post notifications. */
    fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Shows nudge [index] of [count] for [goal] on [date]. No-op when notifications are not allowed. */
    @SuppressLint("MissingPermission")
    fun showNudge(goal: Goal, date: LocalDate, text: NudgeText, index: Int, count: Int) {
        if (!areNotificationsEnabled()) return

        val channelId = channelFor(goal.importance)
        val requestCode = goal.id.toInt()

        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode,
            AppIntents.openGoal(context, goal.id).apply {
                data = Uri.parse("goalkeeper://goal/${goal.id}")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DONE
            data = Uri.parse("goalkeeper://done/${goal.id}/${date.toEpochDay()}")
            putExtra(NotificationActionReceiver.EXTRA_GOAL_ID, goal.id)
            putExtra(NotificationActionReceiver.EXTRA_EPOCH_DAY, date.toEpochDay())
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_goalkeeper)
            .setColor(BRAND_COLOR)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(
                if (channelId == CHANNEL_IMPORTANT) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
            )
            .addAction(R.drawable.ic_stat_check, context.getString(R.string.notif_action_done), donePendingIntent)

        if (index == count - 1 && count > 1) {
            builder.setSubText(context.getString(R.string.notif_last_call))
        }

        NotificationManagerCompat.from(context).notify(nudgeNotificationId(goal.id), builder.build())
    }

    /** Shows the morning briefing notification. No-op when notifications are not allowed. */
    @SuppressLint("MissingPermission")
    fun showBriefing(text: NudgeText) {
        if (!areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            BRIEFING_REQUEST_CODE,
            AppIntents.openApp(context).apply {
                data = Uri.parse("goalkeeper://briefing")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(R.drawable.ic_stat_goalkeeper)
            .setColor(BRAND_COLOR)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(BRIEFING_NOTIFICATION_ID, notification)
    }

    /** Dismisses a goal's currently visible nudge notification, if any. */
    fun cancelNudge(goalId: Long) {
        NotificationManagerCompat.from(context).cancel(nudgeNotificationId(goalId))
    }

    /** Deep-links into the system screen for this app's notification settings. */
    fun appNotificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    private fun channelFor(importance: Importance): String = when (importance) {
        Importance.LOW, Importance.MEDIUM -> CHANNEL_NUDGES
        Importance.HIGH, Importance.CRITICAL -> CHANNEL_IMPORTANT
    }

    /** Single source of truth for a goal's nudge notification id, shared by [showNudge] and [cancelNudge]. */
    private fun nudgeNotificationId(goalId: Long): Int = goalId.toInt()

    companion object {
        const val CHANNEL_IMPORTANT = "nudges_important"
        const val CHANNEL_NUDGES = "nudges"
        const val CHANNEL_BRIEFING = "briefing"

        const val BRIEFING_NOTIFICATION_ID = 1_000_000
        private const val BRIEFING_REQUEST_CODE = 1_000_000

        private val BRAND_COLOR = 0xFF3563F0.toInt()
    }
}
