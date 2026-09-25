package com.goalkeeper.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.goalkeeper.app.AppIntents
import com.goalkeeper.app.R
import com.goalkeeper.app.appContainer
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.data.settings.ThemeMode
import com.goalkeeper.app.ui.common.GoalSummary
import com.goalkeeper.app.ui.common.summarize
import com.goalkeeper.app.ui.theme.GkColors
import com.goalkeeper.app.ui.theme.gkColors
import com.goalkeeper.core.model.DayStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * "Today's goals" home-screen widget: the day's priority queue with streaks and one-tap
 * check-in, in the Midnight Console style.
 *
 * Data is loaded reactively inside [provideContent] (Glance 1.1) rather than once before it, so a
 * live widget session keeps refreshing as goals, check-ins, settings or the day roll over while the
 * widget is on screen. [TodayWidgetUpdater] additionally pushes [updateAll]-driven refreshes while
 * the app process is alive (e.g. after an in-app check-in), which recomposes any live session too.
 */
class TodayWidget : GlanceAppWidget() {

    // Recompose with the widget's exact current size so row widths and the short-widget layout
    // below can be computed correctly instead of guessing from a single fixed estimate.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state by remember { widgetStateFlow(context) }.collectAsState(
                initial = WidgetUiState(colors = gkColors(Colorway.BOLD_BLUE, dark = true), loaded = false),
            )
            WidgetContent(context = context, state = state)
        }
    }
}

/** Everything [WidgetContent] needs to render one frame of the widget. */
private data class WidgetUiState(
    val colors: GkColors,
    /** Actionable-today goals, in ranked order (mirrors [GoalRepository.observeActiveGoals]). */
    val actionable: List<GoalSummary> = emptyList(),
    val hasAnyActiveGoals: Boolean = false,
    /** False only for the placeholder frame before the first database read, so it never claims "No goals yet". */
    val loaded: Boolean = true,
)

/** Combines the same sources [TodayWidgetUpdater] watches into what one widget frame needs to draw. */
private fun widgetStateFlow(context: Context): Flow<WidgetUiState> {
    val container = context.appContainer
    return combine(
        container.goalRepository.observeActiveGoals(),
        container.goalRepository.observeAllCheckIns(),
        container.settingsRepository.settings,
        container.clock.todayFlow(),
    ) { goals, checkIns, settings, today ->
        val dark = when (settings.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM ->
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        }
        WidgetUiState(
            colors = gkColors(settings.colorway, dark),
            actionable = summarize(goals, checkIns, today).filter { it.isActionableToday },
            hasAnyActiveGoals = goals.isNotEmpty(),
        )
    }
}

@Composable
private fun WidgetContent(context: Context, state: WidgetUiState) {
    val colors = state.colors
    val hideMeta = LocalSize.current.height < 120.dp
    val total = state.actionable.size
    val doneCount = state.actionable.count { it.isDoneToday }
    val allDone = total > 0 && doneCount == total

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(colors.card)
            .padding(14.dp),
    ) {
        HeaderRow(context = context, colors = colors, doneCount = doneCount, total = total)
        Spacer(modifier = GlanceModifier.height(10.dp))

        when {
            !state.loaded -> Unit

            !state.hasAnyActiveGoals -> EmptyPanel(
                context = context,
                colors = colors,
                title = "No goals yet",
                subtitle = "Tap to add one",
            )

            state.actionable.isEmpty() -> EmptyPanel(
                context = context,
                colors = colors,
                title = "Nothing due today",
                subtitle = "Rest day — nice.",
            )

            else -> {
                if (allDone) {
                    Text(
                        text = "All done today — streaks safe.",
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(colors.accentText),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    items(state.actionable, itemId = { it.goal.id }) { summary ->
                        GoalRow(context = context, colors = colors, summary = summary, hideMeta = hideMeta)
                    }
                }
            }
        }
    }
}

/** Mono "TODAY" + "done/total", and a "+" that opens the app. Tapping the row itself also opens it. */
@Composable
private fun HeaderRow(context: Context, colors: GkColors, doneCount: Int, total: Int) {
    val openApp = actionStartActivity(AppIntents.openApp(context))
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(openApp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TODAY",
                style = TextStyle(
                    color = ColorProvider(colors.muted),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "$doneCount/$total",
                style = TextStyle(
                    color = ColorProvider(colors.text),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Box(
            modifier = GlanceModifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Image(
                provider = ImageProvider(R.drawable.widget_ic_add),
                contentDescription = "Open Goal Keeper",
                colorFilter = ColorFilter.tint(ColorProvider(colors.accentText)),
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(openApp),
            )
        }
    }
}

/** "No goals yet" / "Nothing due today" placeholder; tapping it opens the app. */
@Composable
private fun EmptyPanel(context: Context, colors: GkColors, title: String, subtitle: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable(actionStartActivity(AppIntents.openApp(context))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = TextStyle(color = ColorProvider(colors.text), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    color = ColorProvider(colors.muted),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
    }
}

// Panel padding (14dp * 2 sides) + the row gap (10dp) + the check button (38dp) reserved on the
// right, subtracted from the widget's exact current width so the title never renders under it
// (Glance has no weight-based flex here, so this space has to be reserved by hand).
private val RowChromeWidth = 76.dp
private val CheckButtonSize = 38.dp

/** One goal: title, streak meta (hidden on very short widgets), and a round check button. */
@Composable
private fun GoalRow(context: Context, colors: GkColors, summary: GoalSummary, hideMeta: Boolean) {
    val titleColumnWidth = LocalSize.current.width - RowChromeWidth
    val streak = summary.stats.current
    val metaColor = if (summary.stats.atRisk) colors.accentText else colors.muted

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(actionStartActivity(AppIntents.openGoal(context, summary.goal.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.width(titleColumnWidth)) {
            Text(
                text = summary.goal.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(colors.text),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (!hideMeta) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (streak > 0) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_flame),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(ColorProvider(metaColor)),
                            modifier = GlanceModifier.size(12.dp),
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                    }
                    Text(
                        text = metaLine(streak),
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(metaColor),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        CheckButton(colors = colors, summary = summary)
    }
}

/** "<n>d streak", or an invitation to start when there is no current streak. Pure text formatting. */
internal fun metaLine(streak: Int): String = if (streak > 0) "${streak}d streak" else "start today"

/**
 * Round tap target: pending = raised circle + accent-tinted check, done = accent circle +
 * onAccent check, skipped = raised circle + muted check (kept simple, per the brief).
 */
@Composable
private fun CheckButton(colors: GkColors, summary: GoalSummary) {
    val done = summary.todayStatus == DayStatus.DONE
    val skipped = summary.todayStatus == DayStatus.SKIPPED
    val circleColor = if (done) colors.accent else colors.raised
    val iconColor = when {
        done -> colors.onAccent
        skipped -> colors.muted
        else -> colors.accentText
    }
    Box(
        modifier = GlanceModifier
            .size(CheckButtonSize)
            .cornerRadius(CheckButtonSize / 2)
            .background(circleColor)
            .clickable(actionRunCallback<ToggleCheckInAction>(actionParametersOf(GoalIdKey to summary.goal.id))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_ic_check),
            contentDescription = if (done) "Checked in today, tap to undo" else "Check in today",
            colorFilter = ColorFilter.tint(ColorProvider(iconColor)),
            modifier = GlanceModifier.size(18.dp),
        )
    }
}
