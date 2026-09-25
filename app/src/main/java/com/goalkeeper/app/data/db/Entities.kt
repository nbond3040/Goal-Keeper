package com.goalkeeper.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Dates are stored as epoch days, instants as epoch millis, enums by name (importance by level). */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val why: String,
    val icon: String,
    val importance: Int,
    val sortOrder: Int,
    val scheduleMask: Int,
    val targetEpochDay: Long?,
    val reminderEnabled: Boolean,
    val reminderStartMinute: Int,
    val nudgeCount: Int,
    val nudgeIntervalMinutes: Int,
    val startEpochDay: Long,
    val createdAt: Long,
    val archived: Boolean,
    val achievedAt: Long?,
)

@Entity(
    tableName = "check_ins",
    primaryKeys = ["goalId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId"), Index("epochDay")],
)
data class CheckInEntity(
    val goalId: Long,
    val epochDay: Long,
    /** [com.goalkeeper.core.model.CheckInStatus] name. */
    val status: String,
    val note: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId")],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    /** [com.goalkeeper.core.model.JournalEntryType] name. */
    val type: String,
    val title: String,
    val body: String,
    /** [com.goalkeeper.core.model.Mood] name, if any. */
    val mood: String?,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val text: String,
    val done: Boolean,
    val position: Int,
)

data class JournalEntryWithItems(
    @Embedded val entry: JournalEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val items: List<ChecklistItemEntity>,
)
