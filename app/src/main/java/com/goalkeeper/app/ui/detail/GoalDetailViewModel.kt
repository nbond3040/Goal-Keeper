package com.goalkeeper.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.data.repo.JournalRepository
import com.goalkeeper.app.ui.navigation.GoalDetailRoute
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import com.goalkeeper.core.reminder.NudgeMessages
import com.goalkeeper.core.streak.StreakCalculator
import com.goalkeeper.core.streak.StreakStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Everything [GoalDetailScreen] needs once the goal has loaded. */
data class GoalDetailData(
    val goal: Goal,
    val stats: StreakStats,
    val today: LocalDate,
    val nowTime: LocalTime,
    val zone: ZoneId,
    /** This goal's check-ins, unfiltered. */
    val checkIns: List<CheckIn>,
    val checkInsByDate: Map<LocalDate, CheckInStatus>,
    /** 16 weeks, Monday..Sunday per week, oldest week first (column-major — see [com.goalkeeper.app.ui.components.StatusHeatmap]). */
    val heatmapStatuses: List<DayStatus>,
    val heatmapMonthLabels: List<String>,
    val entries: List<JournalEntry>,
    val entryCount: Int,
)

sealed interface GoalDetailUiState {
    data object Loading : GoalDetailUiState

    /** The goal doesn't exist — never did, or was deleted while this screen was open. */
    data object NotFound : GoalDetailUiState

    data class Ready(val data: GoalDetailData) : GoalDetailUiState
}

/** One-shot events the stateful screen consumes (snackbars). */
sealed interface GoalDetailEvent {
    data class Milestone(val message: String) : GoalDetailEvent
}

class GoalDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val journalRepository: JournalRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<GoalDetailRoute>()

    val goalId: Long get() = route.goalId
    val startOnJournalTab: Boolean get() = route.openJournal

    private val eventsChannel = Channel<GoalDetailEvent>(Channel.BUFFERED)
    val events: Flow<GoalDetailEvent> = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<GoalDetailUiState> = combine(
        goalRepository.observeGoal(route.goalId),
        goalRepository.observeCheckIns(route.goalId),
        journalRepository.observeEntries(route.goalId),
        journalRepository.observeCount(route.goalId),
        clock.todayFlow(),
    ) { goal, checkIns, entries, entryCount, today ->
        if (goal == null) {
            GoalDetailUiState.NotFound
        } else {
            GoalDetailUiState.Ready(buildData(goal, checkIns, entries, entryCount, today))
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState.Loading)

    private fun buildData(
        goal: Goal,
        checkIns: List<CheckIn>,
        entries: List<JournalEntry>,
        entryCount: Int,
        today: LocalDate,
    ): GoalDetailData {
        val stats = StreakCalculator.stats(goal, checkIns, today)
        val checkInsByDate = checkIns.associate { it.date to it.status }

        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val heatmapFrom = thisMonday.minusWeeks(15)
        val heatmapTo = thisMonday.plusDays(6)
        val heatmapStatuses = StreakCalculator.dayStatuses(goal, checkIns, heatmapFrom, heatmapTo, today).map { it.second }
        val monthLabels = (0 until 16)
            .map { week -> heatmapFrom.plusWeeks(week.toLong()).month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase() }
            .distinctConsecutive()

        return GoalDetailData(
            goal = goal,
            stats = stats,
            today = today,
            nowTime = clock.now().toLocalTime(),
            zone = clock.zone(),
            checkIns = checkIns,
            checkInsByDate = checkInsByDate,
            heatmapStatuses = heatmapStatuses,
            heatmapMonthLabels = monthLabels,
            entries = entries,
            entryCount = entryCount,
        )
    }

    /** Sets or clears today's check-in. A milestone snackbar fires only when marking DONE lands on one. */
    fun checkIn(status: CheckInStatus?) {
        val data = readyData() ?: return
        viewModelScope.launch { goalRepository.setCheckIn(data.goal.id, data.today, status) }
        if (status == CheckInStatus.DONE) {
            val projected = withCheckIn(data, data.today, status)
            val newCurrent = StreakCalculator.stats(data.goal, projected, data.today).current
            if (newCurrent in StreakCalculator.MILESTONES) {
                val message = NudgeMessages.milestone(data.goal.title, newCurrent)
                viewModelScope.launch { eventsChannel.send(GoalDetailEvent.Milestone(message)) }
            }
        }
    }

    /** Cycles a calendar cell: none/MISSED -> DONE -> SKIPPED -> none. Future dates are ignored. */
    fun toggleDay(date: LocalDate) {
        val data = readyData() ?: return
        if (date.isAfter(data.today)) return
        val next = when (data.checkInsByDate[date]) {
            null -> CheckInStatus.DONE
            CheckInStatus.DONE -> CheckInStatus.SKIPPED
            CheckInStatus.SKIPPED -> null
        }
        viewModelScope.launch { goalRepository.setCheckIn(data.goal.id, date, next) }
    }

    fun setArchived(archived: Boolean, achieved: Boolean = false) {
        viewModelScope.launch { goalRepository.setArchived(goalId, archived, achieved) }
    }

    fun delete() {
        viewModelScope.launch { goalRepository.deleteGoal(goalId) }
    }

    /** Saves the composer's quick log entry. Blank text is ignored. */
    fun saveQuickEntry(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            journalRepository.saveEntry(
                JournalEntry(
                    id = 0,
                    goalId = goalId,
                    type = JournalEntryType.ENTRY,
                    body = trimmed,
                    createdAt = Instant.EPOCH,
                    updatedAt = Instant.EPOCH,
                ),
            )
        }
    }

    fun setPinned(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.setPinned(entry.id, !entry.pinned) }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.deleteEntry(entry.id) }
    }

    fun setItemDone(itemId: Long, done: Boolean) {
        viewModelScope.launch { journalRepository.setItemDone(itemId, done) }
    }

    private fun readyData(): GoalDetailData? = (uiState.value as? GoalDetailUiState.Ready)?.data

    private fun withCheckIn(data: GoalDetailData, date: LocalDate, status: CheckInStatus): List<CheckIn> {
        val without = data.checkIns.filterNot { it.date == date }
        return without + CheckIn(goalId = data.goal.id, date = date, status = status, createdAt = clock.instant())
    }
}

private fun List<String>.distinctConsecutive(): List<String> {
    val out = mutableListOf<String>()
    for (value in this) if (out.lastOrNull() != value) out += value
    return out
}
