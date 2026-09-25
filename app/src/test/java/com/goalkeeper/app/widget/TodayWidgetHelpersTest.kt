package com.goalkeeper.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM test for the text formatting extracted from [TodayWidget]'s row layout. */
class TodayWidgetHelpersTest {

    @Test
    fun `no streak yet invites the user to start`() {
        assertEquals("start today", metaLine(streak = 0))
    }

    @Test
    fun `positive streak is rendered as days`() {
        assertEquals("1d streak", metaLine(streak = 1))
        assertEquals("42d streak", metaLine(streak = 42))
    }
}
