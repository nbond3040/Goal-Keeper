package com.goalkeeper.core.model

import java.time.Instant

enum class JournalEntryType(val label: String) {
    /** A free-form log entry — "just talk about the goal". May carry a [Mood]. */
    ENTRY("Log"),

    /** A reference note with a title and body. */
    NOTE("Note"),

    /** A task list made of [ChecklistItem]s. */
    CHECKLIST("Tasks"),
}

enum class Mood(val label: String) {
    GREAT("Great"),
    STRONG("Strong"),
    OKAY("Okay"),
    TIRED("Tired"),
    ROUGH("Rough");

    companion object {
        fun fromKey(key: String?): Mood? = entries.firstOrNull { it.name == key }
    }
}

data class ChecklistItem(
    val id: Long = 0,
    val entryId: Long = 0,
    val text: String,
    val done: Boolean = false,
    val position: Int = 0,
)

data class JournalEntry(
    val id: Long = 0,
    val goalId: Long,
    val type: JournalEntryType,
    val title: String = "",
    val body: String = "",
    val mood: Mood? = null,
    val pinned: Boolean = false,
    /** Only used when [type] is [JournalEntryType.CHECKLIST]; sorted by [ChecklistItem.position]. */
    val items: List<ChecklistItem> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val doneCount: Int get() = items.count { it.done }
}
