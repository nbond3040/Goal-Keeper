package com.goalkeeper.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.ui.common.Formats
import com.goalkeeper.app.ui.common.GoalSummary
import com.goalkeeper.app.ui.common.summarize
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** An active goal row with its display strings already resolved (schedule copy, stats line, target line). */
data class ActiveGoalRow(
    val summary: GoalSummary,
    val scheduleLabel: String,
    val statsLine: String,
    val targetLabel: String?,
    val targetPassed: Boolean,
)

data class ArchivedGoalRow(val goal: Goal, val achievedDateLabel: String?)

data class GoalsUiState(
    val loading: Boolean = true,
    val active: List<ActiveGoalRow> = emptyList(),
    val archived: List<ArchivedGoalRow> = emptyList(),
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    private val clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        clock.todayFlow(),
        goalRepository.observeActiveGoals(),
        goalRepository.observeArchivedGoals(),
        goalRepository.observeAllCheckIns(),
    ) { today, active, archived, checkIns -> buildGoalsUiState(today, active, archived, checkIns, clock.zone()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    /** Restores an archived goal to active (used by the Archived tab's "Restore" button). */
    fun restore(goalId: Long) {
        viewModelScope.launch { goalRepository.setArchived(goalId, archived = false) }
    }
}

/** Pure reduction into display rows (shared by [GoalsViewModel] and screenshot tests). */
internal fun buildGoalsUiState(
    today: LocalDate,
    active: List<Goal>,
    archived: List<Goal>,
    checkIns: List<CheckIn>,
    zone: ZoneId,
): GoalsUiState {
    val activeRows = summarize(active, checkIns, today).map { summary ->
        val target = summary.goal.targetDate?.let { targetLine(it, today) }
        ActiveGoalRow(
            summary = summary,
            scheduleLabel = scheduleLabel(summary.goal.schedule),
            statsLine = statsLine(summary),
            targetLabel = target?.first,
            targetPassed = target?.second ?: false,
        )
    }
    val archivedRows = archived.map { goal ->
        val label = goal.achievedAt?.let { Formats.mediumDate.format(LocalDateTime.ofInstant(it, zone).toLocalDate()) }
        ArchivedGoalRow(goal, label)
    }
    return GoalsUiState(loading = false, active = activeRows, archived = archivedRows)
}

private fun scheduleLabel(schedule: Schedule): String = when {
    schedule.isDaily -> "Every day"
    schedule.effectiveMask == Schedule.WEEKDAYS.effectiveMask -> "Weekdays"
    schedule.effectiveMask == Schedule.WEEKENDS.effectiveMask -> "Weekends"
    else -> schedule.days.joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private fun statsLine(summary: GoalSummary): String {
    val stats = summary.stats
    return "${stats.current}d · best ${stats.best} · ${Formats.percent(stats.completionRate30)}"
}

/** [String] display text plus whether the date is already in the past. */
private fun targetLine(targetDate: LocalDate, today: LocalDate): Pair<String, Boolean> {
    val days = ChronoUnit.DAYS.between(today, targetDate)
    val dateText = Formats.mediumDate.format(targetDate)
    return if (days < 0) "$dateText · target passed" to true else "$dateText · ${days}d left" to false
}
