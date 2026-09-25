package com.goalkeeper.app.data

import com.goalkeeper.app.data.repo.StoredRank
import com.goalkeeper.app.data.repo.checklistRows
import com.goalkeeper.app.data.repo.planRanking
import com.goalkeeper.app.data.settings.AppSettings
import com.goalkeeper.app.data.settings.Colorway
import com.goalkeeper.app.data.settings.ThemeMode
import com.goalkeeper.app.data.settings.parseSettingsMap
import com.goalkeeper.app.data.settings.toExportMap
import com.goalkeeper.core.model.ChecklistItem
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DataLogicTest {

    @Test
    fun `ranking restarts at zero in each tier and follows list order`() {
        val stored = mapOf(
            1L to StoredRank(Importance.CRITICAL.level, 4),
            2L to StoredRank(Importance.HIGH.level, 3),
            3L to StoredRank(Importance.HIGH.level, 3),
        )
        val updates = planRanking(
            listOf(3L to Importance.HIGH, 1L to Importance.CRITICAL, 2L to Importance.HIGH),
            stored,
        )
        assertEquals(listOf(3L, 1L, 2L), updates.map { it.id })
        assertEquals(listOf(0, 0, 1), updates.map { it.sortOrder })
        assertTrue(updates.none { it.importanceChanged })
    }

    @Test
    fun `moving tiers adopts the new default nudge count only when it wasn't customized`() {
        val stored = mapOf(
            1L to StoredRank(Importance.MEDIUM.level, Importance.MEDIUM.defaultNudges),
            2L to StoredRank(Importance.MEDIUM.level, 6),
        )
        val updates = planRanking(listOf(1L to Importance.CRITICAL, 2L to Importance.CRITICAL), stored)
        assertEquals(Importance.CRITICAL.defaultNudges, updates[0].nudgeCount)
        assertEquals(6, updates[1].nudgeCount)
        assertTrue(updates.all { it.importanceChanged })
        assertEquals(listOf(0, 1), updates.map { it.sortOrder })
    }

    @Test
    fun `unknown goals are skipped without consuming a rank`() {
        val stored = mapOf(2L to StoredRank(Importance.LOW.level, 1))
        val updates = planRanking(listOf(99L to Importance.LOW, 2L to Importance.LOW), stored)
        assertEquals(1, updates.size)
        assertEquals(0, updates.single().sortOrder)
    }

    @Test
    fun `checklist rows drop blanks and renumber positions`() {
        val entry = JournalEntry(
            id = 7, goalId = 1, type = JournalEntryType.CHECKLIST,
            items = listOf(
                ChecklistItem(id = 10, text = "  Buy shoes ", done = true, position = 5),
                ChecklistItem(id = 11, text = "   ", position = 6),
                ChecklistItem(id = 12, text = "Sign up", position = 9),
            ),
            createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        )
        val rows = checklistRows(7, entry)
        assertEquals(listOf("Buy shoes", "Sign up"), rows.map { it.text })
        assertEquals(listOf(0, 1), rows.map { it.position })
        assertTrue(rows.all { it.id == 0L && it.entryId == 7L })
        assertEquals(listOf(true, false), rows.map { it.done })
    }

    @Test
    fun `settings survive an export and import round trip`() {
        val settings = AppSettings(
            colorway = Colorway.EMBER_ORANGE,
            themeMode = ThemeMode.LIGHT,
            defaultReminderMinute = 19 * 60 + 15,
            briefingEnabled = true,
            briefingMinute = 7 * 60,
        )
        val parsed = parseSettingsMap(settings.toExportMap())
        assertEquals(Colorway.EMBER_ORANGE, parsed.colorway)
        assertEquals(ThemeMode.LIGHT, parsed.themeMode)
        assertEquals(19 * 60 + 15, parsed.defaultReminderMinute)
        assertEquals(true, parsed.briefingEnabled)
        assertEquals(7 * 60, parsed.briefingMinute)
    }

    @Test
    fun `invalid or unknown settings values are ignored`() {
        val parsed = parseSettingsMap(
            mapOf(
                "colorway" to "NEON_PINK",
                "theme_mode" to "dark",
                "default_reminder_minute" to "2000",
                "briefing_enabled" to "yes",
                "briefing_minute" to "abc",
                "something_else" to "1",
            ),
        )
        assertNull(parsed.colorway)
        assertNull(parsed.themeMode)
        assertNull(parsed.defaultReminderMinute)
        assertNull(parsed.briefingEnabled)
        assertNull(parsed.briefingMinute)
        assertFalse(AppSettings().toExportMap().containsKey("notifications_prompted"))
    }
}
