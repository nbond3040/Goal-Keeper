package com.goalkeeper.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.goalkeeper.app.data.settings.AppSettings
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.data.settings.ThemeMode
import com.goalkeeper.app.di.AppContainer
import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.Importance
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate

private const val LogTag = "TodayWidgetUpdater"

/**
 * Keeps the "Today's goals" widget fresh for as long as the app process is alive: in-app
 * check-ins, the notification "Done" action and the midnight rollover all flow through the same
 * repositories this observes, so one subscription here covers every source without each of those
 * call sites having to remember to poke the widget themselves.
 *
 * [TodayWidget] itself also reloads reactively while a live Glance session is on screen (see its
 * `provideContent` block); this object's job is the [updateAll] push so a widget that is *not*
 * currently recomposing (a cold RemoteViews on the home screen) still catches up promptly.
 */
@OptIn(FlowPreview::class)
object TodayWidgetUpdater {

    fun start(context: Context, container: AppContainer) {
        val appContext = context.applicationContext
        combine(
            container.goalRepository.observeActiveGoals(),
            container.goalRepository.observeAllCheckIns(),
            container.settingsRepository.settings,
            container.clock.todayFlow(),
        ) { goals, checkIns, settings, today ->
            fingerprint(goals, checkIns, settings, today)
        }
            .debounce(300)
            .distinctUntilChanged()
            .onEach {
                try {
                    TodayWidget().updateAll(appContext)
                } catch (t: Throwable) {
                    // A widget refresh must never take the app process down with it.
                    Log.w(LogTag, "Failed to refresh the Today's goals widget", t)
                }
            }
            .catch { t -> Log.w(LogTag, "Today's goals widget update stream failed", t) }
            .launchIn(container.applicationScope)
    }

    private fun fingerprint(
        goals: List<Goal>,
        checkIns: List<CheckIn>,
        settings: AppSettings,
        today: LocalDate,
    ): WidgetFingerprint = WidgetFingerprint(
        goals = goals.map { GoalFingerprint(it.id, it.title, it.importance, it.rank, it.schedule.mask) },
        checkIns = checkIns.map { CheckInFingerprint(it.goalId, it.date, it.status) },
        colorway = settings.colorway,
        themeMode = settings.themeMode,
        today = today,
    )
}

/** Only the fields that change what the widget draws, so unrelated goal/check-in edits don't trigger a redraw. */
private data class WidgetFingerprint(
    val goals: List<GoalFingerprint>,
    val checkIns: List<CheckInFingerprint>,
    val colorway: Colorway,
    val themeMode: ThemeMode,
    val today: LocalDate,
)

private data class GoalFingerprint(
    val id: Long,
    val title: String,
    val importance: Importance,
    val rank: Int,
    val scheduleMask: Int,
)

private data class CheckInFingerprint(
    val goalId: Long,
    val date: LocalDate,
    val status: CheckInStatus,
)
