package com.goalkeeper.app.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.data.repo.JournalRepository
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.JournalEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Everything [JournalFeedScreen] needs once the first combine of goals/entries/today lands. */
data class JournalFeedData(
    val entries: List<JournalEntry>,
    val entryCount: Int,
    val goalCount: Int,
    val goalChips: Map<Long, GoalChipInfo>,
    val zone: ZoneId,
    val today: LocalDate,
)

sealed interface JournalFeedUiState {
    data object Loading : JournalFeedUiState

    data class Ready(val data: JournalFeedData) : JournalFeedUiState
}

/** The global journal across every goal (top-level Journal tab). */
class JournalFeedViewModel(
    private val goalRepository: GoalRepository,
    private val journalRepository: JournalRepository,
    private val clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<JournalFeedUiState> = combine(
        journalRepository.observeRecent(500),
        goalRepository.observeAllGoals(),
        clock.todayFlow(),
    ) { entries, goals, today ->
        val chips = goals.associate { goal -> goal.id to GoalChipInfo(goal.id, goal.icon, goal.title) }
        JournalFeedUiState.Ready(
            JournalFeedData(
                entries = entries,
                entryCount = entries.size,
                // Distinct goals actually represented among the loaded entries, not the app's total goal count.
                goalCount = entries.map { it.goalId }.distinct().size,
                goalChips = chips,
                zone = clock.zone(),
                today = today,
            ),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalFeedUiState.Loading)

    fun setPinned(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.setPinned(entry.id, !entry.pinned) }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.deleteEntry(entry.id) }
    }

    fun setItemDone(itemId: Long, done: Boolean) {
        viewModelScope.launch { journalRepository.setItemDone(itemId, done) }
    }
}
