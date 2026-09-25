package com.goalkeeper.app.ui.navigation

import com.goalkeeper.core.model.JournalEntryType
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object GoalsRoute

@Serializable
data object InsightsRoute

@Serializable
data object JournalFeedRoute

@Serializable
data object SettingsRoute

@Serializable
data object RankRoute

/** Goal detail; [openJournal] starts on the Journal tab. */
@Serializable
data class GoalDetailRoute(val goalId: Long, val openJournal: Boolean = false)

/** Create ([goalId] == 0) or edit a goal. */
@Serializable
data class GoalEditorRoute(val goalId: Long = 0L)

/** Create ([entryId] == 0, of [type]) or edit a journal entry of [goalId]. [type] is a [JournalEntryType] name. */
@Serializable
data class JournalEditorRoute(
    val goalId: Long,
    val entryId: Long = 0L,
    val type: String = JournalEntryType.ENTRY.name,
)
