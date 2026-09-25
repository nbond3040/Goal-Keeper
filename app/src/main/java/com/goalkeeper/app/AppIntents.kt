package com.goalkeeper.app

import android.content.Context
import android.content.Intent

/** Intents that open specific places in the app (used by notifications). */
object AppIntents {
    const val EXTRA_GOAL_ID = "com.goalkeeper.app.extra.GOAL_ID"

    /** Opens the goal detail screen of [goalId]. */
    fun openGoal(context: Context, goalId: Long): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_GOAL_ID, goalId)
        }

    /** Opens the app on its start screen. */
    fun openApp(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
}
