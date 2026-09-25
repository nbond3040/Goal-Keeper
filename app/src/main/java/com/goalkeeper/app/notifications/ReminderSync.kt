package com.goalkeeper.app.notifications

/** Keeps reminder alarms in step with the database. Implemented by [ReminderScheduler]. */
interface ReminderSync {
    /** Re-arms (or cancels) the next nudge alarm of one goal and clears its notification if it is resolved. */
    suspend fun sync(goalId: Long)

    /** Re-arms every goal's alarm plus the morning briefing. Safe to call any time; idempotent. */
    suspend fun syncAll()

    /** Cancels a goal's pending alarm and visible notification (used when a goal is deleted). */
    fun cancel(goalId: Long)
}
