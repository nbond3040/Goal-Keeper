package com.goalkeeper.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.goalkeeper.app.appContainer
import com.goalkeeper.core.model.CheckInStatus

/** The goal id a widget tap applies to, carried through [androidx.glance.action.actionParametersOf]. */
val GoalIdKey = ActionParameters.Key<Long>("goalId")

/**
 * Toggles today's check-in for the tapped goal: pending/skipped -> DONE, DONE -> removed.
 * [com.goalkeeper.app.data.repo.GoalRepository] re-syncs that goal's reminders on the write, so
 * this only has to refresh the widget's own view afterwards.
 */
class ToggleCheckInAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val goalId = parameters[GoalIdKey] ?: return
        val container = context.appContainer
        val today = container.clock.today()
        val existing = container.goalRepository.getCheckIn(goalId, today)
        if (existing?.status == CheckInStatus.DONE) {
            container.goalRepository.setCheckIn(goalId, today, null)
        } else {
            container.goalRepository.setCheckIn(goalId, today, CheckInStatus.DONE)
        }
        TodayWidget().update(context, glanceId)
    }
}
