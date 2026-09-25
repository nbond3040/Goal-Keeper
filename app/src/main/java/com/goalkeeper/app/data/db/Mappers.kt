package com.goalkeeper.app.data.db

import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.ChecklistItem
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import com.goalkeeper.core.model.Mood
import com.goalkeeper.core.model.ReminderConfig
import com.goalkeeper.core.model.Schedule
import java.time.Instant
import java.time.LocalDate

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    title = title,
    why = why,
    icon = GoalIcon.fromKey(icon),
    importance = Importance.fromLevel(importance),
    rank = sortOrder,
    schedule = Schedule(scheduleMask),
    targetDate = targetEpochDay?.let(LocalDate::ofEpochDay),
    reminder = ReminderConfig(
        enabled = reminderEnabled,
        startMinuteOfDay = reminderStartMinute,
        nudgeCount = nudgeCount,
        intervalMinutes = nudgeIntervalMinutes,
    ),
    startDate = LocalDate.ofEpochDay(startEpochDay),
    createdAt = Instant.ofEpochMilli(createdAt),
    archived = archived,
    achievedAt = achievedAt?.let(Instant::ofEpochMilli),
)

fun Goal.toEntity(): GoalEntity {
    val reminder = reminder.normalized()
    return GoalEntity(
        id = id,
        title = title,
        why = why,
        icon = icon.name,
        importance = importance.level,
        sortOrder = rank,
        scheduleMask = schedule.effectiveMask,
        targetEpochDay = targetDate?.toEpochDay(),
        reminderEnabled = reminder.enabled,
        reminderStartMinute = reminder.startMinuteOfDay,
        nudgeCount = reminder.nudgeCount,
        nudgeIntervalMinutes = reminder.intervalMinutes,
        startEpochDay = startDate.toEpochDay(),
        createdAt = createdAt.toEpochMilli(),
        archived = archived,
        achievedAt = achievedAt?.toEpochMilli(),
    )
}

fun CheckInEntity.toDomain(): CheckIn = CheckIn(
    goalId = goalId,
    date = LocalDate.ofEpochDay(epochDay),
    status = CheckInStatus.entries.firstOrNull { it.name == status } ?: CheckInStatus.DONE,
    note = note,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun CheckIn.toEntity(): CheckInEntity = CheckInEntity(
    goalId = goalId,
    epochDay = date.toEpochDay(),
    status = status.name,
    note = note,
    createdAt = createdAt.toEpochMilli(),
)

fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    entryId = entryId,
    text = text,
    done = done,
    position = position,
)

fun ChecklistItem.toEntity(entryId: Long): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    entryId = entryId,
    text = text,
    done = done,
    position = position,
)

fun JournalEntryWithItems.toDomain(): JournalEntry = JournalEntry(
    id = entry.id,
    goalId = entry.goalId,
    type = JournalEntryType.entries.firstOrNull { it.name == entry.type } ?: JournalEntryType.ENTRY,
    title = entry.title,
    body = entry.body,
    mood = Mood.fromKey(entry.mood),
    pinned = entry.pinned,
    items = items.map { it.toDomain() }.sortedBy { it.position },
    createdAt = Instant.ofEpochMilli(entry.createdAt),
    updatedAt = Instant.ofEpochMilli(entry.updatedAt),
)

fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
    id = id,
    goalId = goalId,
    type = type.name,
    title = title,
    body = body,
    mood = mood?.name,
    pinned = pinned,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
