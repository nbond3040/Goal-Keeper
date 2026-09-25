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
import java.time.Instant
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackupCodecTest {

    private val goal1 = Goal(
        id = 1,
        title = "Run a half marathon",
        why = "Stay healthy",
        icon = GoalIcon.RUN,
        importance = Importance.HIGH,
        rank = 2,
        schedule = Schedule.WEEKDAYS,
        targetDate = LocalDate.of(2027, 1, 1),
        reminder = ReminderConfig(enabled = true, startMinuteOfDay = 420, nudgeCount = 3, intervalMinutes = 45),
        startDate = LocalDate.of(2026, 1, 1),
        createdAt = Instant.parse("2026-01-01T08:00:00Z"),
        archived = false,
        achievedAt = null,
    )

    private val goal2 = Goal(
        id = 2,
        title = "Read more",
        icon = GoalIcon.BOOK,
        importance = Importance.LOW,
        schedule = Schedule.DAILY,
        reminder = ReminderConfig.defaultsFor(Importance.LOW),
        startDate = LocalDate.of(2025, 6, 1),
        createdAt = Instant.parse("2025-06-01T00:00:00Z"),
        archived = true,
        achievedAt = Instant.parse("2026-05-01T12:00:00Z"),
    )

    private val checkIns = listOf(
        CheckIn(goalId = 1, date = LocalDate.of(2026, 9, 20), status = CheckInStatus.DONE, note = "felt great", createdAt = Instant.parse("2026-09-20T18:05:00Z")),
        CheckIn(goalId = 2, date = LocalDate.of(2025, 6, 2), status = CheckInStatus.SKIPPED, note = null, createdAt = Instant.parse("2025-06-02T09:00:00Z")),
    )

    private val journal = listOf(
        JournalEntry(
            id = 100,
            goalId = 1,
            type = JournalEntryType.ENTRY,
            body = "Great run today",
            mood = Mood.STRONG,
            pinned = true,
            createdAt = Instant.parse("2026-09-20T18:10:00Z"),
            updatedAt = Instant.parse("2026-09-20T18:10:00Z"),
        ),
        JournalEntry(
            id = 101,
            goalId = 2,
            type = JournalEntryType.CHECKLIST,
            title = "Books to read",
            items = listOf(
                ChecklistItem(id = 55, entryId = 101, text = "Book A", done = true, position = 0),
                ChecklistItem(id = 56, entryId = 101, text = "Book B", done = false, position = 1),
            ),
            createdAt = Instant.parse("2025-06-05T00:00:00Z"),
            updatedAt = Instant.parse("2025-06-06T00:00:00Z"),
        ),
    )

    private val settings = mapOf("theme" to "dark", "reminderSound" to "chime")
    private val exportedAt: Instant = Instant.parse("2026-09-25T12:00:00Z")

    private val minimalGoalDto = GoalDto(
        id = 1,
        title = "Minimal",
        icon = "TARGET",
        importance = 2,
        scheduleMask = Schedule.ALL_DAYS_MASK,
        reminderEnabled = true,
        reminderStartMinute = 600,
        nudgeCount = 1,
        nudgeIntervalMinutes = 60,
        startDate = "2026-01-01",
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `round trips domain objects through encode and decode`() {
        val file = BackupCodec.toBackup(listOf(goal1, goal2), checkIns, journal, settings, exportedAt)
        val json = BackupCodec.encode(file)
        val decoded = BackupCodec.decode(json)
        val contents = BackupCodec.fromBackup(decoded)

        assertEquals(BackupFile.FORMAT, decoded.format)
        assertEquals(BackupFile.VERSION, decoded.version)

        assertEquals(listOf(goal1, goal2), contents.goals)
        assertEquals(checkIns, contents.checkIns)
        // Checklist item ids are re-created on import: normalized to 0 for comparison.
        val expectedJournal = journal.map { entry -> entry.copy(items = entry.items.map { it.copy(id = 0) }) }
        assertEquals(expectedJournal, contents.journal)
        assertEquals(settings, contents.settings)
    }

    @Test
    fun `decode rejects an unrecognized format`() {
        val json = BackupCodec.encode(BackupFile(format = "some-other-app-backup", exportedAt = "2026-01-01T00:00:00Z"))
        assertFailsWith<BackupFormatException> { BackupCodec.decode(json) }
    }

    @Test
    fun `decode rejects a version newer than this app supports`() {
        val json = BackupCodec.encode(BackupFile(version = BackupFile.VERSION + 1, exportedAt = "2026-01-01T00:00:00Z"))
        assertFailsWith<BackupFormatException> { BackupCodec.decode(json) }
    }

    @Test
    fun `decode accepts the current version`() {
        val json = BackupCodec.encode(BackupFile(version = BackupFile.VERSION, exportedAt = "2026-01-01T00:00:00Z"))
        assertEquals(BackupFile.VERSION, BackupCodec.decode(json).version)
    }

    @Test
    fun `decode rejects invalid JSON`() {
        assertFailsWith<BackupFormatException> { BackupCodec.decode("{ not valid json") }
        assertFailsWith<BackupFormatException> { BackupCodec.decode("this isn't json at all") }
    }

    @Test
    fun `decode ignores unknown JSON keys`() {
        val json = """
            {
              "format": "goal-keeper-backup",
              "version": 1,
              "exportedAt": "2026-01-01T00:00:00Z",
              "somethingFromTheFuture": { "nested": true },
              "goals": [],
              "checkIns": [],
              "journal": [],
              "settings": {}
            }
        """.trimIndent()

        val file = BackupCodec.decode(json)
        assertEquals(BackupFile.FORMAT, file.format)
        assertEquals(0, file.goals.size)
    }

    @Test
    fun `fromBackup rejects a check-in that references an unknown goal id`() {
        val file = BackupFile(
            exportedAt = "2026-01-01T00:00:00Z",
            goals = listOf(minimalGoalDto),
            checkIns = listOf(CheckInDto(goalId = 42, date = "2026-01-01", status = "DONE", createdAt = "2026-01-01T00:00:00Z")),
        )
        assertFailsWith<BackupFormatException> { BackupCodec.fromBackup(file) }
    }

    @Test
    fun `fromBackup rejects a journal entry that references an unknown goal id`() {
        val file = BackupFile(
            exportedAt = "2026-01-01T00:00:00Z",
            goals = listOf(minimalGoalDto),
            journal = listOf(
                JournalEntryDto(id = 1, goalId = 42, type = "ENTRY", createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z"),
            ),
        )
        assertFailsWith<BackupFormatException> { BackupCodec.fromBackup(file) }
    }

    @Test
    fun `fromBackup accepts a check-in and journal entry that reference a goal in the file`() {
        val file = BackupFile(
            exportedAt = "2026-01-01T00:00:00Z",
            goals = listOf(minimalGoalDto),
            checkIns = listOf(CheckInDto(goalId = 1, date = "2026-01-01", status = "DONE", createdAt = "2026-01-01T00:00:00Z")),
            journal = listOf(
                JournalEntryDto(id = 1, goalId = 1, type = "ENTRY", createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z"),
            ),
        )
        val contents = BackupCodec.fromBackup(file)
        assertEquals(1, contents.checkIns.size)
        assertEquals(1, contents.journal.size)
    }

    @Test
    fun `fromBackup wraps a malformed enum into BackupFormatException`() {
        val file = BackupFile(
            exportedAt = "2026-01-01T00:00:00Z",
            goals = listOf(minimalGoalDto),
            checkIns = listOf(CheckInDto(goalId = 1, date = "2026-01-01", status = "NOT_A_STATUS", createdAt = "2026-01-01T00:00:00Z")),
        )
        assertFailsWith<BackupFormatException> { BackupCodec.fromBackup(file) }
    }

    @Test
    fun `fromBackup wraps a malformed date into BackupFormatException`() {
        val file = BackupFile(exportedAt = "2026-01-01T00:00:00Z", goals = listOf(minimalGoalDto.copy(startDate = "not-a-date")))
        assertFailsWith<BackupFormatException> { BackupCodec.fromBackup(file) }
    }

    @Test
    fun `fromBackup falls back to TARGET for an unknown icon instead of throwing`() {
        val file = BackupFile(exportedAt = "2026-01-01T00:00:00Z", goals = listOf(minimalGoalDto.copy(icon = "NOT_A_REAL_ICON")))
        val contents = BackupCodec.fromBackup(file)
        assertEquals(GoalIcon.TARGET, contents.goals.single().icon)
    }
}
