package com.goalkeeper.core.backup

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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The JSON backup format. Dates are ISO-8601 strings (LocalDate "2026-09-25", Instant "2026-09-25T18:00:00Z"),
 * enums are their names. IDs are preserved so check-ins and journal entries keep pointing at their goals.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: String,
    val goals: List<GoalDto> = emptyList(),
    val checkIns: List<CheckInDto> = emptyList(),
    val journal: List<JournalEntryDto> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
) {
    companion object {
        const val FORMAT = "goal-keeper-backup"
        const val VERSION = 1
    }
}

@Serializable
data class GoalDto(
    val id: Long,
    val title: String,
    val why: String = "",
    val icon: String,
    val importance: Int,
    val rank: Int = 0,
    val scheduleMask: Int,
    val targetDate: String? = null,
    val reminderEnabled: Boolean,
    val reminderStartMinute: Int,
    val nudgeCount: Int,
    val nudgeIntervalMinutes: Int,
    val startDate: String,
    val createdAt: String,
    val archived: Boolean = false,
    val achievedAt: String? = null,
)

@Serializable
data class CheckInDto(
    val goalId: Long,
    val date: String,
    val status: String,
    val note: String? = null,
    val createdAt: String,
)

@Serializable
data class ChecklistItemDto(
    val text: String,
    val done: Boolean = false,
    val position: Int = 0,
)

@Serializable
data class JournalEntryDto(
    val id: Long,
    val goalId: Long,
    val type: String,
    val title: String = "",
    val body: String = "",
    val mood: String? = null,
    val pinned: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val items: List<ChecklistItemDto> = emptyList(),
)

/** Domain-level content of a backup. Checklist item ids are 0 (they are re-created on import). */
data class BackupContents(
    val goals: List<Goal>,
    val checkIns: List<CheckIn>,
    val journal: List<JournalEntry>,
    val settings: Map<String, String>,
)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupCodec {

    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Pretty-printed JSON for [file]. */
    fun encode(file: BackupFile): String = jsonFormat.encodeToString(file)

    /**
     * Parses [json] (unknown keys ignored). Throws [BackupFormatException] when it is not valid JSON, the
     * `format` is not [BackupFile.FORMAT], or the `version` is newer than [BackupFile.VERSION].
     */
    fun decode(json: String): BackupFile {
        val file = try {
            jsonFormat.decodeFromString<BackupFile>(json)
        } catch (e: SerializationException) {
            throw BackupFormatException("Not a valid Goal Keeper backup: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("Not a valid Goal Keeper backup: ${e.message}", e)
        }
        if (file.format != BackupFile.FORMAT) {
            throw BackupFormatException("Unrecognized backup format: ${file.format}")
        }
        if (file.version > BackupFile.VERSION) {
            throw BackupFormatException("Backup was created by a newer version of the app (v${file.version})")
        }
        return file
    }

    /** Builds a [BackupFile] from domain objects. */
    fun toBackup(
        goals: List<Goal>,
        checkIns: List<CheckIn>,
        journal: List<JournalEntry>,
        settings: Map<String, String>,
        exportedAt: Instant,
    ): BackupFile = BackupFile(
        exportedAt = exportedAt.toString(),
        goals = goals.map { it.toDto() },
        checkIns = checkIns.map { it.toDto() },
        journal = journal.map { it.toDto() },
        settings = settings,
    )

    /**
     * Converts a decoded file back to domain objects. Throws [BackupFormatException] on malformed dates/enums
     * or on check-ins/journal entries that reference a goal id missing from the file.
     */
    fun fromBackup(file: BackupFile): BackupContents {
        val goals = file.goals.map { it.toDomain() }
        val goalIds = goals.mapTo(HashSet()) { it.id }

        val checkIns = file.checkIns.map { dto ->
            if (dto.goalId !in goalIds) {
                throw BackupFormatException("Check-in on ${dto.date} references unknown goal id ${dto.goalId}")
            }
            dto.toDomain()
        }
        val journal = file.journal.map { dto ->
            if (dto.goalId !in goalIds) {
                throw BackupFormatException("Journal entry ${dto.id} references unknown goal id ${dto.goalId}")
            }
            dto.toDomain()
        }
        return BackupContents(goals = goals, checkIns = checkIns, journal = journal, settings = file.settings)
    }

    // ---- domain -> dto ----

    private fun Goal.toDto(): GoalDto = GoalDto(
        id = id,
        title = title,
        why = why,
        icon = icon.name,
        importance = importance.level,
        rank = rank,
        scheduleMask = schedule.mask,
        targetDate = targetDate?.toString(),
        reminderEnabled = reminder.enabled,
        reminderStartMinute = reminder.startMinuteOfDay,
        nudgeCount = reminder.nudgeCount,
        nudgeIntervalMinutes = reminder.intervalMinutes,
        startDate = startDate.toString(),
        createdAt = createdAt.toString(),
        archived = archived,
        achievedAt = achievedAt?.toString(),
    )

    private fun CheckIn.toDto(): CheckInDto = CheckInDto(
        goalId = goalId,
        date = date.toString(),
        status = status.name,
        note = note,
        createdAt = createdAt.toString(),
    )

    private fun JournalEntry.toDto(): JournalEntryDto = JournalEntryDto(
        id = id,
        goalId = goalId,
        type = type.name,
        title = title,
        body = body,
        mood = mood?.name,
        pinned = pinned,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        items = items.map { it.toDto() },
    )

    private fun ChecklistItem.toDto(): ChecklistItemDto = ChecklistItemDto(
        text = text,
        done = done,
        position = position,
    )

    // ---- dto -> domain ----

    private fun GoalDto.toDomain(): Goal = Goal(
        id = id,
        title = title,
        why = why,
        icon = GoalIcon.fromKey(icon),
        importance = Importance.fromLevel(importance),
        rank = rank,
        schedule = Schedule(scheduleMask),
        targetDate = targetDate?.toBackupLocalDate(),
        reminder = ReminderConfig(
            enabled = reminderEnabled,
            startMinuteOfDay = reminderStartMinute,
            nudgeCount = nudgeCount,
            intervalMinutes = nudgeIntervalMinutes,
        ),
        startDate = startDate.toBackupLocalDate(),
        createdAt = createdAt.toBackupInstant(),
        archived = archived,
        achievedAt = achievedAt?.toBackupInstant(),
    )

    private fun CheckInDto.toDomain(): CheckIn = CheckIn(
        goalId = goalId,
        date = date.toBackupLocalDate(),
        status = status.toBackupEnum<CheckInStatus>("check-in status"),
        note = note,
        createdAt = createdAt.toBackupInstant(),
    )

    private fun JournalEntryDto.toDomain(): JournalEntry = JournalEntry(
        id = id,
        goalId = goalId,
        type = type.toBackupEnum<JournalEntryType>("journal entry type"),
        title = title,
        body = body,
        mood = Mood.fromKey(mood),
        pinned = pinned,
        items = items.map { it.toDomain(entryId = id) },
        createdAt = createdAt.toBackupInstant(),
        updatedAt = updatedAt.toBackupInstant(),
    )

    /** Checklist items are re-created on import: a fresh id (0) and the parent entry's id. */
    private fun ChecklistItemDto.toDomain(entryId: Long): ChecklistItem = ChecklistItem(
        id = 0,
        entryId = entryId,
        text = text,
        done = done,
        position = position,
    )

    // ---- parsing helpers ----

    private fun String.toBackupLocalDate(): LocalDate = try {
        LocalDate.parse(this)
    } catch (e: DateTimeParseException) {
        throw BackupFormatException("Invalid date: $this", e)
    }

    private fun String.toBackupInstant(): Instant = try {
        Instant.parse(this)
    } catch (e: DateTimeParseException) {
        throw BackupFormatException("Invalid timestamp: $this", e)
    }

    private inline fun <reified T : Enum<T>> String.toBackupEnum(what: String): T = try {
        enumValueOf<T>(this)
    } catch (e: IllegalArgumentException) {
        throw BackupFormatException("Invalid $what: $this", e)
    }
}
