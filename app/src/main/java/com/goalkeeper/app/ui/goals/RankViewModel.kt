package com.goalkeeper.app.ui.goals

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.Importance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One row of the Rank list: either a fixed tier header or a draggable goal. */
sealed interface RankEntry {
    val key: Any
}

data class TierHeaderEntry(val importance: Importance) : RankEntry {
    override val key: Any get() = "header-${importance.name}"
}

data class GoalRowEntry(val goal: Goal) : RankEntry {
    override val key: Any get() = "goal-${goal.id}"
}

/**
 * Holds the editable ranking list directly (as Compose state) rather than behind a [StateFlow], since the
 * drag gesture needs to mutate individual entries without waiting on a combine/emit round-trip.
 */
class RankViewModel(private val goalRepository: GoalRepository) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _entries = mutableStateListOf<RankEntry>()

    /** Headers (always 4, Critical→Low) interleaved with goal rows; a goal's tier is the nearest header above it. */
    val entries: List<RankEntry> get() = _entries

    private var savedOrder: List<Pair<Long, Importance>> = emptyList()

    init {
        viewModelScope.launch {
            val goals = goalRepository.getActiveGoals().sortedWith(Goal.RANKED)
            _entries.addAll(buildEntries(goals))
            savedOrder = currentOrder()
            _loading.value = false
        }
    }

    val hasChanges: Boolean get() = currentOrder() != savedOrder

    /** Moves the entry at [fromIndex] to [toIndex] in the flat list. Crossing a header changes that goal's tier. */
    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in _entries.indices || toIndex !in _entries.indices) return
        if (_entries[fromIndex] !is GoalRowEntry) return
        // Index 0 is always the Critical header; nothing may sit above it.
        val target = toIndex.coerceAtLeast(1)
        if (target == fromIndex) return
        _entries.add(target, _entries.removeAt(fromIndex))
    }

    /** Moves [goalId] to the end of [importance]'s section (the tier chip's dropdown). */
    fun setTier(goalId: Long, importance: Importance) {
        val fromIndex = _entries.indexOfFirst { it is GoalRowEntry && it.goal.id == goalId }
        if (fromIndex < 0) return
        val row = _entries.removeAt(fromIndex) as GoalRowEntry
        val headerIndex = _entries.indexOfFirst { it is TierHeaderEntry && it.importance == importance }
        if (headerIndex < 0) {
            // Should not happen (all four headers always exist), but keep the entry rather than drop it.
            _entries.add(fromIndex, row)
            return
        }
        var insertAt = headerIndex + 1
        while (insertAt < _entries.size && _entries[insertAt] !is TierHeaderEntry) insertAt++
        _entries.add(insertAt, row)
    }

    /** "Move up" accessibility action: swaps with the previous flat-list entry (may cross into the tier above). */
    fun moveUp(goalId: Long) {
        val index = _entries.indexOfFirst { it is GoalRowEntry && it.goal.id == goalId }
        // Index 0 is the Critical header, so a goal right below it is already at the top.
        if (index <= 1) return
        _entries.add(index - 1, _entries.removeAt(index))
    }

    /** "Move down" accessibility action: swaps with the next flat-list entry (may cross into the tier below). */
    fun moveDown(goalId: Long) {
        val index = _entries.indexOfFirst { it is GoalRowEntry && it.goal.id == goalId }
        if (index < 0 || index >= _entries.size - 1) return
        _entries.add(index + 1, _entries.removeAt(index))
    }

    /** Persists the ranking if it changed, then always calls [onDone] (e.g. to navigate back). */
    fun save(onDone: () -> Unit) {
        if (!hasChanges) {
            onDone()
            return
        }
        val ordered = currentOrder()
        viewModelScope.launch {
            goalRepository.saveRanking(ordered)
            savedOrder = ordered
            onDone()
        }
    }

    private fun currentOrder(): List<Pair<Long, Importance>> {
        var tier = Importance.CRITICAL
        val result = ArrayList<Pair<Long, Importance>>(_entries.size)
        for (entry in _entries) {
            when (entry) {
                is TierHeaderEntry -> tier = entry.importance
                is GoalRowEntry -> result.add(entry.goal.id to tier)
            }
        }
        return result
    }

    private fun buildEntries(goals: List<Goal>): List<RankEntry> {
        val byTier = goals.groupBy { it.importance }
        val result = ArrayList<RankEntry>()
        for (tier in Importance.descending) {
            result.add(TierHeaderEntry(tier))
            byTier[tier]?.forEach { result.add(GoalRowEntry(it)) }
        }
        return result
    }
}
