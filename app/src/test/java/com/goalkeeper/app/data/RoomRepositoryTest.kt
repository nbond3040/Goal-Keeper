package com.goalkeeper.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goalkeeper.app.data.db.GoalKeeperDatabase
import com.goalkeeper.app.data.repo.RoomGoalRepository
import com.goalkeeper.app.data.repo.RoomJournalRepository
import com.goalkeeper.app.notifications.ReminderSync
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.ChecklistItem
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.Importance
import com.goalkeeper.core.model.JournalEntry
import com.goalkeeper.core.model.JournalEntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** The Room repositories against a real in-memory database. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class RoomRepositoryTest {

    private class FakeClock(var current: Instant) : AppClock() {
        override fun zone(): ZoneId = ZoneOffset.UTC
        override fun instant(): Instant = current
    }

    private class RecordingSync : ReminderSync {
        val synced = mutableListOf<Long>()
        val cancelled = mutableListOf<Long>()
        override suspend fun sync(goalId: Long) {
            synced += goalId
        }
        override suspend fun syncAll() = Unit
        override fun cancel(goalId: Long) {
            cancelled += goalId
        }
    }

    private val clock = FakeClock(Instant.parse("2026-09-25T18:00:00Z"))
    private val sync = RecordingSync()
    private lateinit var db: GoalKeeperDatabase
    private lateinit var goals: RoomGoalRepository
    private lateinit var journal: RoomJournalRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), GoalKeeperDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        goals = RoomGoalRepository(db, sync, clock)
        journal = RoomJournalRepository(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newGoal(title: String, importance: Importance = Importance.MEDIUM): Long =
        goals.saveGoal(Goal(title = title, importance = importance, startDate = LocalDate.of(2026, 9, 1), createdAt = clock.instant()))

    private fun note(goalId: Long, body: String, id: Long = 0) = JournalEntry(
        id = id, goalId = goalId, type = JournalEntryType.NOTE, title = "Paces", body = body,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    @Test
    fun newGoalsGoLastInTheirTier() = runTest {
        val a = newGoal("A")
        val b = newGoal("B")
        val c = newGoal("C", Importance.HIGH)

        assertEquals(listOf(c, a, b), goals.observeActiveGoals().first().map { it.id })
        assertEquals(listOf(0, 0, 1), goals.observeActiveGoals().first().map { it.rank })
        assertEquals(listOf(a, b, c), sync.synced)
    }

    @Test
    fun changingImportanceMovesTheGoalToTheEndOfItsNewTier() = runTest {
        val a = newGoal("A")
        val c = newGoal("C", Importance.HIGH)
        val goal = goals.getGoal(a)!!

        goals.saveGoal(goal.copy(importance = Importance.HIGH))

        assertEquals(listOf(c, a), goals.observeActiveGoals().first().map { it.id })
        assertEquals(1, goals.getGoal(a)!!.rank)
        assertEquals(goal.createdAt, goals.getGoal(a)!!.createdAt)
    }

    @Test
    fun rankingIntoAnotherTierAdoptsItsDefaultNudgesUnlessCustomized() = runTest {
        val a = newGoal("A")
        val b = newGoal("B")
        goals.saveGoal(goals.getGoal(b)!!.let { it.copy(reminder = it.reminder.copy(nudgeCount = 6)) })

        goals.saveRanking(listOf(a to Importance.CRITICAL, b to Importance.CRITICAL))

        assertEquals(Importance.CRITICAL.defaultNudges, goals.getGoal(a)!!.reminder.nudgeCount)
        assertEquals(6, goals.getGoal(b)!!.reminder.nudgeCount)
        assertEquals(listOf(0, 1), listOf(goals.getGoal(a)!!.rank, goals.getGoal(b)!!.rank))
    }

    @Test
    fun checkInsKeepTheirTimeUnlessTheStatusChanges() = runTest {
        val a = newGoal("A")
        val today = LocalDate.of(2026, 9, 25)
        goals.setCheckIn(a, today, CheckInStatus.DONE)
        clock.current = Instant.parse("2026-09-25T19:00:00Z")
        goals.setCheckIn(a, today, CheckInStatus.DONE)
        assertEquals(Instant.parse("2026-09-25T18:00:00Z"), goals.getCheckIn(a, today)!!.createdAt)

        goals.setCheckIn(a, today, CheckInStatus.SKIPPED)
        assertEquals(Instant.parse("2026-09-25T19:00:00Z"), goals.getCheckIn(a, today)!!.createdAt)

        goals.setCheckIn(a, today, null)
        assertNull(goals.getCheckIn(a, today))
        assertTrue(sync.synced.count { it == a } >= 4)
    }

    @Test
    fun editingAnEntryKeepsItsCreationTime() = runTest {
        val goalId = newGoal("Run")
        val id = journal.saveEntry(note(goalId, "Easy 10:30"))
        val created = journal.getEntry(id)!!.createdAt
        assertEquals(Instant.parse("2026-09-25T18:00:00Z"), created)

        clock.current = Instant.parse("2026-09-27T09:00:00Z")
        journal.saveEntry(note(goalId, "Easy 10:15", id = id))

        val edited = journal.getEntry(id)!!
        assertEquals(created, edited.createdAt)
        assertEquals(Instant.parse("2026-09-27T09:00:00Z"), edited.updatedAt)
        assertEquals("Easy 10:15", edited.body)
    }

    @Test
    fun savingAnEntryDeletedMeanwhileKeepsTheText() = runTest {
        val goalId = newGoal("Run")
        val id = journal.saveEntry(note(goalId, "First"))
        journal.deleteEntry(id)

        val newId = journal.saveEntry(note(goalId, "Still here", id = id))

        assertEquals("Still here", journal.getEntry(newId)!!.body)
    }

    @Test
    fun checklistItemsAreRewrittenInOrderWithoutBlanks() = runTest {
        val goalId = newGoal("Run")
        val entry = JournalEntry(
            goalId = goalId, type = JournalEntryType.CHECKLIST, title = "Race prep",
            items = listOf(
                ChecklistItem(text = "Register", done = true, position = 7),
                ChecklistItem(text = "   ", position = 0),
                ChecklistItem(text = " Buy trainers ", position = 3),
            ),
            createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        )
        val id = journal.saveEntry(entry)

        val saved = journal.getEntry(id)!!
        assertEquals(listOf("Register", "Buy trainers"), saved.items.map { it.text })
        assertEquals(listOf(0, 1), saved.items.map { it.position })
        assertEquals(listOf(true, false), saved.items.map { it.done })

        journal.setItemDone(saved.items[1].id, true)
        assertEquals(2, journal.getEntry(id)!!.doneCount)
    }

    @Test
    fun deletingAGoalRemovesItsJournalAndCancelsReminders() = runTest {
        val goalId = newGoal("Run")
        journal.saveEntry(note(goalId, "Easy 10:30"))
        goals.setCheckIn(goalId, LocalDate.of(2026, 9, 25), CheckInStatus.DONE)

        goals.deleteGoal(goalId)

        assertNull(goals.getGoal(goalId))
        assertEquals(0, journal.observeCount(goalId).first())
        assertTrue(goals.getCheckIns(goalId).isEmpty())
        assertEquals(listOf(goalId), sync.cancelled)
    }
}
