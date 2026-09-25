package com.goalkeeper.app.data.repo

import androidx.room.withTransaction
import com.goalkeeper.app.data.db.ChecklistItemEntity
import com.goalkeeper.app.data.db.GoalKeeperDatabase
import com.goalkeeper.app.data.db.toDomain
import com.goalkeeper.app.data.db.toEntity
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomJournalRepository(
    private val db: GoalKeeperDatabase,
    private val clock: AppClock,
) : JournalRepository {

    private val dao = db.journalDao()

    override fun observeEntries(goalId: Long): Flow<List<JournalEntry>> =
        dao.observeForGoal(goalId).map { list -> list.map { it.toDomain() } }

    override fun observeRecent(limit: Int): Flow<List<JournalEntry>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeEntry(id: Long): Flow<JournalEntry?> = dao.observe(id).map { it?.toDomain() }

    override fun observeCount(goalId: Long): Flow<Int> = dao.observeCount(goalId)

    override suspend fun getEntry(id: Long): JournalEntry? = dao.get(id)?.toDomain()

    override suspend fun saveEntry(entry: JournalEntry): Long = db.withTransaction {
        val now = clock.instant()
        val cleaned = entry.copy(title = entry.title.trim(), body = entry.body.trim())
        val stored = if (cleaned.id == 0L) null else dao.get(cleaned.id)?.entry
        val id = if (stored == null) {
            // New, or deleted while it was being edited: save it as a new entry rather than lose the text.
            val createdAt = if (cleaned.createdAt == Instant.EPOCH) now else cleaned.createdAt
            dao.insertEntry(cleaned.copy(id = 0, createdAt = createdAt, updatedAt = now).toEntity())
        } else {
            // Editors pass Instant.EPOCH when they don't know the creation time: keep the stored one.
            val createdAt = if (cleaned.createdAt == Instant.EPOCH) Instant.ofEpochMilli(stored.createdAt) else cleaned.createdAt
            dao.updateEntry(cleaned.copy(createdAt = createdAt, updatedAt = now).toEntity())
            cleaned.id
        }
        dao.deleteItems(id)
        if (cleaned.type == JournalEntryType.CHECKLIST) {
            val items = checklistRows(id, cleaned)
            if (items.isNotEmpty()) dao.insertItems(items)
        }
        id
    }

    override suspend fun deleteEntry(id: Long) {
        dao.deleteEntry(id)
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        dao.setPinned(id, pinned)
    }

    override suspend fun setItemDone(itemId: Long, done: Boolean) {
        val item = dao.getItem(itemId) ?: return
        dao.setItemDone(itemId, done)
        dao.touch(item.entryId, clock.instant().toEpochMilli())
    }
}

/** Fresh item rows for [entry]: blank texts dropped, positions rewritten 0..n-1 in list order. */
internal fun checklistRows(entryId: Long, entry: JournalEntry): List<ChecklistItemEntity> =
    entry.items
        .map { it.text.trim() to it.done }
        .filter { (text, _) -> text.isNotEmpty() }
        .mapIndexed { index, (text, done) ->
            ChecklistItemEntity(id = 0, entryId = entryId, text = text, done = done, position = index)
        }
