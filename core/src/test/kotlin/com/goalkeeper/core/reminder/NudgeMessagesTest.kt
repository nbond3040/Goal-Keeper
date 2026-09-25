package com.goalkeeper.core.reminder

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NudgeMessagesTest {

    @Test
    fun `nudge is deterministic for the same inputs`() {
        val a = NudgeMessages.nudge("Run", "Stay healthy", streak = 5, index = 0, count = 3, variant = 7)
        val b = NudgeMessages.nudge("Run", "Stay healthy", streak = 5, index = 0, count = 3, variant = 7)
        assertEquals(a, b)
    }

    @Test
    fun `the last index of a multi-nudge day is an explicit last call`() {
        val text = NudgeMessages.nudge("Run", "", streak = 10, index = 2, count = 3)
        assertTrue(text.body.contains("midnight", ignoreCase = true) || text.body.contains("before the day ends", ignoreCase = true))
    }

    @Test
    fun `a single nudge uses the friendly tone but still calls out the streak`() {
        val text = NudgeMessages.nudge("Run", "", streak = 10, index = 0, count = 1)
        assertFalse(text.body.contains("midnight", ignoreCase = true)) // not a last call
        assertTrue(text.body.contains("10-day streak"))
    }

    @Test
    fun `middle nudges are firmer and distinct from the opening and closing ones`() {
        val first = NudgeMessages.nudge("Run", "", streak = 4, index = 0, count = 4)
        val middle = NudgeMessages.nudge("Run", "", streak = 4, index = 1, count = 4)
        val last = NudgeMessages.nudge("Run", "", streak = 4, index = 3, count = 4)

        assertNotEquals(first.body, middle.body)
        assertNotEquals(middle.body, last.body)
        assertFalse(first.body.contains("midnight", ignoreCase = true))
        assertTrue(last.body.contains("midnight", ignoreCase = true))
    }

    @Test
    fun `streak greater than zero is mentioned, using singular phrasing for one`() {
        val one = NudgeMessages.nudge("Run", "", streak = 1, index = 0, count = 1)
        assertTrue(one.body.contains("1-day streak"))
        assertTrue(one.title.contains("1-day streak"))

        val many = NudgeMessages.nudge("Run", "", streak = 42, index = 0, count = 1)
        assertTrue(many.body.contains("42-day streak"))
        assertTrue(many.title.contains("42-day streak"))
    }

    @Test
    fun `streak of zero encourages starting one instead of naming a length`() {
        val text = NudgeMessages.nudge("Run", "", streak = 0, index = 0, count = 1)
        assertFalse(text.body.contains("0-day streak"))
        assertTrue(text.body.contains("streak", ignoreCase = true))
    }

    @Test
    fun `why is appended when present and omitted when blank or empty`() {
        val withWhy = NudgeMessages.nudge("Run", "so I feel better", streak = 3, index = 0, count = 1)
        assertTrue(withWhy.body.contains("Why: so I feel better"))

        val blankWhy = NudgeMessages.nudge("Run", "   ", streak = 3, index = 0, count = 1)
        assertFalse(blankWhy.body.contains("Why:"))

        val noWhy = NudgeMessages.nudge("Run", "", streak = 3, index = 0, count = 1)
        assertFalse(noWhy.body.contains("Why:"))
    }

    @Test
    fun `why is trimmed before being appended`() {
        val text = NudgeMessages.nudge("Run", "  because reasons  ", streak = 3, index = 0, count = 1)
        assertTrue(text.body.endsWith("Why: because reasons"))
    }

    @Test
    fun `negative variants do not crash and match their floorMod-equivalent positive variant`() {
        // FRIENDLY tone (index 0 of 3) with streak > 0 has exactly 3 phrasings; floorMod(-1, 3) == 2.
        val viaNegative = NudgeMessages.nudge("Run", "", streak = 5, index = 0, count = 3, variant = -1)
        val viaEquivalentPositive = NudgeMessages.nudge("Run", "", streak = 5, index = 0, count = 3, variant = 2)
        assertEquals(viaEquivalentPositive.body, viaNegative.body)

        // Extreme values must not throw either.
        val extreme = NudgeMessages.nudge("Run", "", streak = 5, index = 0, count = 3, variant = Int.MIN_VALUE)
        assertTrue(extreme.body.isNotBlank())
    }

    @Test
    fun `title stays short and includes the goal title`() {
        val text = NudgeMessages.nudge("Run a half marathon", "", streak = 42, index = 1, count = 3)
        assertTrue(text.title.startsWith("Run a half marathon"))
        assertTrue(text.title.length <= "Run a half marathon".length + 45)
    }

    @Test
    fun `variants cycle deterministically across more than one phrasing`() {
        val seen = (0..10).map { NudgeMessages.nudge("Run", "", streak = 5, index = 1, count = 3, variant = it).body }
        assertTrue(seen.distinct().size > 1)
    }

    @Test
    fun `briefing describes a rest day when nothing is scheduled`() {
        val text = NudgeMessages.briefing(scheduledToday = 0, topGoalTitle = null, longestStreak = 0)
        assertTrue(text.body.isNotBlank())
        assertFalse(text.body.contains("Top priority"))
    }

    @Test
    fun `briefing includes the top goal and longest streak when given`() {
        val text = NudgeMessages.briefing(scheduledToday = 3, topGoalTitle = "Run a half marathon", longestStreak = 12)
        assertTrue(text.body.contains("Run a half marathon"))
        assertTrue(text.body.contains("12-day streak"))
        assertTrue(text.body.contains("3"))
    }

    @Test
    fun `briefing omits streak mention when there is none`() {
        val text = NudgeMessages.briefing(scheduledToday = 1, topGoalTitle = "Run", longestStreak = 0)
        assertFalse(text.body.contains("streak"))
    }

    @Test
    fun `milestone names the streak and the goal`() {
        assertEquals("30-day streak on Run a half marathon! Keep going.", NudgeMessages.milestone("Run a half marathon", 30))
    }
}
