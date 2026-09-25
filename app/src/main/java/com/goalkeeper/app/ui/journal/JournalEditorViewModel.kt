package com.goalkeeper.app.ui.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.data.repo.JournalRepository
import com.goalkeeper.app.ui.navigation.JournalEditorRoute
import com.goalkeeper.core.model.ChecklistItem
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import com.goalkeeper.core.model.Mood
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** One checklist row as edited in the UI; [id] is negative for a row not yet persisted. */
data class ChecklistDraftItem(val id: Long, val text: String, val done: Boolean)

private data class Draft(
    val type: JournalEntryType,
    val pinned: Boolean,
    val mood: Mood?,
    val title: String,
    val body: String,
    val items: List<ChecklistDraftItem>,
)

sealed interface JournalEditorUiState {
    data object Loading : JournalEditorUiState

    data class Ready(
        val isNew: Boolean,
        val goalTitle: String,
        val type: JournalEntryType,
        val pinned: Boolean,
        val mood: Mood?,
        val title: String,
        val body: String,
        val items: List<ChecklistDraftItem>,
        val dirty: Boolean,
    ) : JournalEditorUiState {
        val canSave: Boolean
            get() = when (type) {
                JournalEntryType.ENTRY -> body.isNotBlank()
                JournalEntryType.NOTE -> title.isNotBlank() || body.isNotBlank()
                JournalEntryType.CHECKLIST -> title.isNotBlank() || items.any { it.text.isNotBlank() }
            }
    }
}

sealed interface JournalEditorEvent {
    data object Saved : JournalEditorEvent

    data object Deleted : JournalEditorEvent
}

/** Create ([JournalEditorRoute.entryId] == 0) or edit a journal entry. */
class JournalEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val journalRepository: JournalRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<JournalEditorRoute>()
    private val isNew = route.entryId == 0L
    private val routeType = runCatching { JournalEntryType.valueOf(route.type) }.getOrDefault(JournalEntryType.ENTRY)

    private var baseline = Draft(type = routeType, pinned = false, mood = null, title = "", body = "", items = emptyList())
    private val draft = MutableStateFlow(baseline)
    private val isLoaded = MutableStateFlow(false)
    private var nextLocalItemId = -1L

    private val eventsChannel = Channel<JournalEditorEvent>(Channel.BUFFERED)
    val events: Flow<JournalEditorEvent> = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (!isNew) {
                val existing = journalRepository.getEntry(route.entryId)
                if (existing != null) {
                    baseline = Draft(
                        type = existing.type,
                        pinned = existing.pinned,
                        mood = existing.mood,
                        title = existing.title,
                        body = existing.body,
                        items = existing.items.map { ChecklistDraftItem(it.id, it.text, it.done) },
                    )
                    draft.value = baseline
                }
            }
            isLoaded.value = true
        }
    }

    private val goalTitleFlow: Flow<String> = goalRepository.observeGoal(route.goalId).map { it?.title.orEmpty() }

    val uiState: StateFlow<JournalEditorUiState> = combine(isLoaded, draft, goalTitleFlow) { loaded, current, goalTitle ->
        if (!loaded) {
            JournalEditorUiState.Loading
        } else {
            JournalEditorUiState.Ready(
                isNew = isNew,
                goalTitle = goalTitle,
                type = current.type,
                pinned = current.pinned,
                mood = current.mood,
                title = current.title,
                body = current.body,
                items = current.items,
                dirty = current != baseline,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalEditorUiState.Loading)

    fun setTitle(text: String) {
        draft.update { it.copy(title = text) }
    }

    fun setBody(text: String) {
        draft.update { it.copy(body = text) }
    }

    /** Tapping the already-selected mood again clears it. */
    fun toggleMood(mood: Mood) {
        draft.update { it.copy(mood = if (it.mood == mood) null else mood) }
    }

    fun addItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val id = nextLocalItemId--
        draft.update { it.copy(items = it.items + ChecklistDraftItem(id = id, text = trimmed, done = false)) }
    }

    fun updateItemText(itemId: Long, text: String) {
        draft.update { d -> d.copy(items = d.items.map { if (it.id == itemId) it.copy(text = text) else it }) }
    }

    fun setItemDone(itemId: Long, done: Boolean) {
        draft.update { d -> d.copy(items = d.items.map { if (it.id == itemId) it.copy(done = done) else it }) }
    }

    fun removeItem(itemId: Long) {
        draft.update { d -> d.copy(items = d.items.filterNot { it.id == itemId }) }
    }

    /** Moves the item at [itemId] by [delta] positions (e.g. -1 to move up, +1 to move down). */
    fun moveItem(itemId: Long, delta: Int) {
        draft.update { d ->
            val index = d.items.indexOfFirst { it.id == itemId }
            if (index < 0) return@update d
            val target = (index + delta).coerceIn(0, d.items.lastIndex)
            if (target == index) return@update d
            val reordered = d.items.toMutableList()
            val item = reordered.removeAt(index)
            reordered.add(target, item)
            d.copy(items = reordered)
        }
    }

    /** Pin/unpin persists immediately (it isn't part of the unsaved-changes draft). */
    fun togglePinned() {
        val newPinned = !draft.value.pinned
        // Update baseline before draft so no recombination can ever observe a stale baseline.
        baseline = baseline.copy(pinned = newPinned)
        draft.update { it.copy(pinned = newPinned) }
        if (!isNew) {
            viewModelScope.launch { journalRepository.setPinned(route.entryId, newPinned) }
        }
    }

    fun save() {
        val state = uiState.value as? JournalEditorUiState.Ready ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            journalRepository.saveEntry(
                JournalEntry(
                    id = route.entryId,
                    goalId = route.goalId,
                    type = state.type,
                    title = state.title.trim(),
                    body = state.body.trim(),
                    mood = if (state.type == JournalEntryType.ENTRY) state.mood else null,
                    pinned = state.pinned,
                    items = if (state.type == JournalEntryType.CHECKLIST) {
                        state.items
                            .filter { it.text.isNotBlank() }
                            .mapIndexed { index, item ->
                                ChecklistItem(
                                    id = item.id.coerceAtLeast(0),
                                    entryId = route.entryId,
                                    text = item.text.trim(),
                                    done = item.done,
                                    position = index,
                                )
                            }
                    } else {
                        emptyList()
                    },
                    createdAt = Instant.EPOCH,
                    updatedAt = Instant.EPOCH,
                ),
            )
            eventsChannel.send(JournalEditorEvent.Saved)
        }
    }

    fun delete() {
        if (isNew) return
        viewModelScope.launch {
            journalRepository.deleteEntry(route.entryId)
            eventsChannel.send(JournalEditorEvent.Deleted)
        }
    }
}
