package com.goalkeeper.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val RANKED = "ORDER BY importance DESC, sortOrder ASC, id ASC"

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE archived = 0 $RANKED")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE archived = 1 ORDER BY COALESCE(achievedAt, createdAt) DESC")
    fun observeArchived(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals $RANKED")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observe(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE archived = 0 $RANKED")
    suspend fun getActive(): List<GoalEntity>

    @Query("SELECT * FROM goals $RANKED")
    suspend fun getAll(): List<GoalEntity>

    /** Highest sortOrder among active goals of [importance], or -1 when there are none. */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM goals WHERE archived = 0 AND importance = :importance")
    suspend fun maxSortOrder(importance: Int): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("UPDATE goals SET importance = :importance, sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateRanking(id: Long, importance: Int, sortOrder: Int)

    @Query("UPDATE goals SET archived = :archived, achievedAt = :achievedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, achievedAt: Long?)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins WHERE goalId = :goalId ORDER BY epochDay ASC")
    fun observeForGoal(goalId: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE goalId = :goalId ORDER BY epochDay ASC")
    suspend fun getForGoal(goalId: Long): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay ORDER BY epochDay ASC")
    fun observeBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins ORDER BY epochDay ASC")
    suspend fun getAll(): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE goalId = :goalId AND epochDay = :epochDay")
    suspend fun get(goalId: Long, epochDay: Long): CheckInEntity?

    @Upsert
    suspend fun upsert(checkIn: CheckInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(checkIns: List<CheckInEntity>)

    @Query("DELETE FROM check_ins WHERE goalId = :goalId AND epochDay = :epochDay")
    suspend fun delete(goalId: Long, epochDay: Long)

    @Query("DELETE FROM check_ins")
    suspend fun deleteAll()
}

@Dao
interface JournalDao {
    @Transaction
    @Query("SELECT * FROM journal_entries WHERE goalId = :goalId ORDER BY pinned DESC, createdAt DESC")
    fun observeForGoal(goalId: Long): Flow<List<JournalEntryWithItems>>

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<JournalEntryWithItems>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun observe(id: Long): Flow<JournalEntryWithItems?>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun get(id: Long): JournalEntryWithItems?

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY createdAt ASC")
    suspend fun getAll(): List<JournalEntryWithItems>

    @Query("SELECT COUNT(*) FROM journal_entries WHERE goalId = :goalId")
    fun observeCount(goalId: Long): Flow<Int>

    @Insert
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JournalEntryEntity>)

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Query("UPDATE journal_entries SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE journal_entries SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllEntries()

    @Insert
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM checklist_items WHERE id = :itemId")
    suspend fun getItem(itemId: Long): ChecklistItemEntity?

    @Query("UPDATE checklist_items SET done = :done WHERE id = :itemId")
    suspend fun setItemDone(itemId: Long, done: Boolean)

    @Query("DELETE FROM checklist_items WHERE entryId = :entryId")
    suspend fun deleteItems(entryId: Long)

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllItems()
}
