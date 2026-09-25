package com.goalkeeper.app.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.goalkeeper.app.data.repo.GoalRepository
import com.goalkeeper.app.data.settings.SettingsRepository
import com.goalkeeper.app.ui.navigation.GoalEditorRoute
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.model.Schedule
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** The goal editor's form state. All fields are edited locally and only written on [GoalEditorViewModel.save]. */
data class GoalEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val title: String = "",
    val titleError: String? = null,
    val why: String = "",
    val icon: GoalIcon = GoalIcon.TARGET,
    val importance: Importance = Importance.MEDIUM,
    val scheduleMask: Int = Schedule.DAILY.mask,
    val targetDate: LocalDate? = null,
    val reminder: ReminderConfig = ReminderConfig.defaultsFor(Importance.MEDIUM),
    val archived: Boolean = false,
    val achievedAt: Instant? = null,
    val dirty: Boolean = false,
    val saving: Boolean = false,
)

/** One-shot navigation/side-effect events consumed by [GoalEditorScreen]. */
sealed interface GoalEditorEvent {
    data class Saved(val goalId: Long, val isNew: Boolean) : GoalEditorEvent
    data object Deleted : GoalEditorEvent
    /** The goal was archived, restored or marked achieved: nothing left to edit here right now. */
    data object Closed : GoalEditorEvent
}

/** The subset of the form that "unsaved changes" is judged against. */
private data class FormSnapshot(
    val title: String,
    val why: String,
    val icon: GoalIcon,
    val importance: Importance,
    val scheduleMask: Int,
    val targetDate: LocalDate?,
    val reminder: ReminderConfig,
)

private fun GoalEditorUiState.snapshot() = FormSnapshot(title, why, icon, importance, scheduleMask, targetDate, reminder)

class GoalEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<GoalEditorRoute>()

    private val _uiState = MutableStateFlow(GoalEditorUiState(isNew = route.goalId == 0L, today = clock.today()))
    val uiState: StateFlow<GoalEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<GoalEditorEvent>(Channel.BUFFERED)
    val events: Flow<GoalEditorEvent> = _events.receiveAsFlow()

    /** The goal as loaded from the repository (edit mode only); saving copies edits onto it. */
    private var loadedGoal: Goal? = null

    /** Once the user changes the nudge count by hand, importance changes stop overwriting it. */
    private var nudgeManuallyChanged = false

    private var baseline = _uiState.value.snapshot()

    init {
        viewModelScope.launch {
            val goalId = route.goalId
            if (goalId == 0L) {
                val settings = settingsRepository.current()
                val loaded = GoalEditorUiState(
                    loading = false,
                    isNew = true,
                    today = clock.today(),
                    importance = Importance.MEDIUM,
                    scheduleMask = Schedule.DAILY.mask,
                    icon = GoalIcon.TARGET,
                    reminder = ReminderConfig.defaultsFor(Importance.MEDIUM, settings.defaultReminderMinute),
                )
                baseline = loaded.snapshot()
                _uiState.value = loaded
            } else {
                val goal = goalRepository.getGoal(goalId)
                if (goal == null) {
                    // Deleted elsewhere while we were loading; there is nothing left to edit.
                    _events.send(GoalEditorEvent.Deleted)
                    return@launch
                }
                loadedGoal = goal
                val loaded = GoalEditorUiState(
                    loading = false,
                    isNew = false,
                    today = clock.today(),
                    title = goal.title,
                    why = goal.why,
                    icon = goal.icon,
                    importance = goal.importance,
                    scheduleMask = goal.schedule.mask,
                    targetDate = goal.targetDate,
                    reminder = goal.reminder,
                    archived = goal.archived,
                    achievedAt = goal.achievedAt,
                )
                baseline = loaded.snapshot()
                _uiState.value = loaded
            }
        }
    }

    private fun mutate(transform: (GoalEditorUiState) -> GoalEditorUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(dirty = next.snapshot() != baseline)
        }
    }

    fun updateTitle(value: String) = mutate { it.copy(title = value, titleError = null) }

    fun updateWhy(value: String) = mutate { it.copy(why = value) }

    fun selectIcon(icon: GoalIcon) = mutate { it.copy(icon = icon) }

    fun selectImportance(importance: Importance) = mutate { current ->
        val reminder = if (nudgeManuallyChanged) current.reminder else current.reminder.copy(nudgeCount = importance.defaultNudges)
        current.copy(importance = importance, reminder = reminder)
    }

    /** Applies a whole-mask quick option ("Every day" / "Weekdays"). */
    fun selectQuickSchedule(mask: Int) = mutate { it.copy(scheduleMask = mask) }

    fun toggleDay(day: DayOfWeek) = mutate { current ->
        val schedule = Schedule(current.scheduleMask)
        val isLastSelectedDay = Integer.bitCount(current.scheduleMask) == 1 && schedule.includes(day)
        // Schedule treats an all-zero mask as "every day"; block the toggle instead of letting the
        // last selected day silently flip the meaning of the schedule to daily.
        if (isLastSelectedDay) current else current.copy(scheduleMask = schedule.toggle(day).mask)
    }

    fun selectTargetDate(date: LocalDate?) = mutate { it.copy(targetDate = date) }

    fun setRemindersEnabled(enabled: Boolean) = mutate { it.copy(reminder = it.reminder.copy(enabled = enabled)) }

    fun setReminderStartMinute(minuteOfDay: Int) = mutate { it.copy(reminder = it.reminder.copy(startMinuteOfDay = minuteOfDay)) }

    fun incrementNudgeCount() = mutate { current ->
        nudgeManuallyChanged = true
        val count = (current.reminder.nudgeCount + 1).coerceAtMost(ReminderConfig.MAX_NUDGES)
        current.copy(reminder = current.reminder.copy(nudgeCount = count))
    }

    fun decrementNudgeCount() = mutate { current ->
        nudgeManuallyChanged = true
        val count = (current.reminder.nudgeCount - 1).coerceAtLeast(1)
        current.copy(reminder = current.reminder.copy(nudgeCount = count))
    }

    fun selectInterval(minutes: Int) = mutate { it.copy(reminder = it.reminder.copy(intervalMinutes = minutes)) }

    fun save() {
        val current = _uiState.value
        if (current.saving) return
        val trimmedTitle = current.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val schedule = Schedule(current.scheduleMask)
            val reminder = current.reminder.normalized()
            val why = current.why.trim()
            val existing = loadedGoal
            val goal = if (existing != null) {
                existing.copy(
                    title = trimmedTitle,
                    why = why,
                    icon = current.icon,
                    importance = current.importance,
                    schedule = schedule,
                    targetDate = current.targetDate,
                    reminder = reminder,
                )
            } else {
                Goal(
                    title = trimmedTitle,
                    why = why,
                    icon = current.icon,
                    importance = current.importance,
                    schedule = schedule,
                    targetDate = current.targetDate,
                    reminder = reminder,
                    startDate = clock.today(),
                    createdAt = clock.instant(),
                )
            }
            val id = goalRepository.saveGoal(goal)
            baseline = current.snapshot()
            _uiState.update { it.copy(saving = false, dirty = false) }
            _events.send(GoalEditorEvent.Saved(goalId = id, isNew = current.isNew))
        }
    }

    fun markAchieved() {
        val id = loadedGoal?.id ?: return
        viewModelScope.launch {
            goalRepository.setArchived(id, archived = true, achieved = true)
            _events.send(GoalEditorEvent.Closed)
        }
    }

    fun setArchived(archived: Boolean) {
        val id = loadedGoal?.id ?: return
        viewModelScope.launch {
            goalRepository.setArchived(id, archived = archived, achieved = false)
            _events.send(GoalEditorEvent.Closed)
        }
    }

    fun delete() {
        val id = loadedGoal?.id ?: return
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
            _events.send(GoalEditorEvent.Deleted)
        }
    }
}
