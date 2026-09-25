package com.goalkeeper.app.data.repo

import com.goalkeeper.core.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    /** A goal's journal: pinned first, then newest first. Checklist items are sorted by position. */
    fun observeEntries(goalId: Long): Flow<List<JournalEntry>>

    /** The newest entries across all goals. */
    fun observeRecent(limit: Int = 200): Flow<List<JournalEntry>>

    fun observeEntry(id: Long): Flow<JournalEntry?>

    fun observeCount(goalId: Long): Flow<Int>

    suspend fun getEntry(id: Long): JournalEntry?

    /**
     * Inserts ([JournalEntry.id] == 0) or updates an entry, replacing its checklist items with [JournalEntry.items]
     * (positions are rewritten 0..n-1 in list order). Sets updatedAt to now (and createdAt for new entries when it
     * is [java.time.Instant.EPOCH]). Returns the entry id.
     */
    suspend fun saveEntry(entry: JournalEntry): Long

    suspend fun deleteEntry(id: Long)

    suspend fun setPinned(id: Long, pinned: Boolean)

    /** Toggles one checklist item straight from a list card; also bumps the entry's updatedAt. */
    suspend fun setItemDone(itemId: Long, done: Boolean)
}
