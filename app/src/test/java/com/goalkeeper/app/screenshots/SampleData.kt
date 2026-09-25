package com.goalkeeper.app.screenshots

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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Deterministic demo data (mirrors the design mockups) for screenshot tests. "Today" is Friday 2026-09-25. */
object SampleData {
    val today: LocalDate = LocalDate.of(2026, 9, 25)

    private fun at(date: LocalDate, hour: Int, minute: Int) = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).toInstant(ZoneOffset.UTC)

    private val created = at(LocalDate.of(2026, 5, 1), 9, 0)

    val run = Goal(
        id = 1, title = "Run a half marathon", why = "Prove to myself I can finish what I start — and feel strong at 40.",
        icon = GoalIcon.RUN, importance = Importance.CRITICAL, rank = 0, schedule = Schedule.DAILY,
        targetDate = LocalDate.of(2027, 4, 12),
        reminder = ReminderConfig(enabled = true, startMinuteOfDay = 18 * 60, nudgeCount = 4, intervalMinutes = 60),
        startDate = LocalDate.of(2026, 5, 1), createdAt = created,
    )
    val spanish = Goal(
        id = 2, title = "Learn Spanish", why = "Talk with my partner's family in their own language.",
        icon = GoalIcon.LANGUAGE, importance = Importance.HIGH, rank = 0, schedule = Schedule.DAILY,
        reminder = ReminderConfig(enabled = true, startMinuteOfDay = 19 * 60, nudgeCount = 3, intervalMinutes = 60),
        startDate = LocalDate.of(2026, 5, 1), createdAt = created,
    )
    val novel = Goal(
        id = 3, title = "Write the novel", why = "The story has been in my head for ten years.",
        icon = GoalIcon.WRITE, importance = Importance.HIGH, rank = 1, schedule = Schedule.WEEKDAYS,
        reminder = ReminderConfig(enabled = true, startMinuteOfDay = 20 * 60, nudgeCount = 3, intervalMinutes = 45),
        startDate = LocalDate.of(2026, 8, 1), createdAt = created,
    )
    val noSpend = Goal(
        id = 4, title = "No-spend day", why = "Emergency fund before the new year.",
        icon = GoalIcon.SAVINGS, importance = Importance.MEDIUM, rank = 0, schedule = Schedule.DAILY,
        reminder = ReminderConfig(enabled = true, startMinuteOfDay = 21 * 60, nudgeCount = 2, intervalMinutes = 60),
        startDate = LocalDate.of(2026, 8, 20), createdAt = created,
    )
    val meditate = Goal(
        id = 5, title = "Meditate", why = "",
        icon = GoalIcon.MEDITATE, importance = Importance.MEDIUM, rank = 1,
        schedule = Schedule.of(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)),
        reminder = ReminderConfig(enabled = false),
        startDate = LocalDate.of(2026, 9, 1), createdAt = created,
    )
    val read = Goal(
        id = 6, title = "Read 30 minutes", why = "",
        icon = GoalIcon.BOOK, importance = Importance.LOW, rank = 0, schedule = Schedule.DAILY,
        reminder = ReminderConfig(enabled = true, startMinuteOfDay = 21 * 60 + 30, nudgeCount = 1, intervalMinutes = 60),
        startDate = LocalDate.of(2026, 8, 1), createdAt = created,
    )

    val goals: List<Goal> = listOf(run, spanish, novel, noSpend, meditate, read)

    val checkIns: List<CheckIn> = buildList {
        fun done(goal: Goal, from: LocalDate, to: LocalDate, skip: Set<LocalDate> = emptySet(), onlyScheduled: Boolean = true) {
            var d = from
            while (!d.isAfter(to)) {
                if (!onlyScheduled || goal.schedule.isScheduled(d)) {
                    val status = if (d in skip) CheckInStatus.SKIPPED else CheckInStatus.DONE
                    add(CheckIn(goal.id, d, status, createdAt = at(d, 18, 40)))
                }
                d = d.plusDays(1)
            }
        }
        // Run: a 58-day best streak in early summer, a lapse, then the current run with two excused days.
        done(run, LocalDate.of(2026, 5, 20), LocalDate.of(2026, 7, 16))
        done(run, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 5))
        done(run, LocalDate.of(2026, 8, 14), today.minusDays(1), skip = setOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 16)))
        // Spanish: 118 days straight, including today.
        done(spanish, today.minusDays(117), today)
        // Novel: weekdays since the start of September.
        done(novel, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 28))
        done(novel, LocalDate.of(2026, 9, 14), today.minusDays(1))
        // No-spend: last six days.
        done(noSpend, today.minusDays(6), today.minusDays(1))
        // Meditate: most Mon–Thu this month.
        done(meditate, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 24))
        // Read: 23 days including today with one skip.
        done(read, today.minusDays(22), today, skip = setOf(today.minusDays(4)))
    }

    val journal: List<JournalEntry> = listOf(
        JournalEntry(
            id = 1, goalId = run.id, type = JournalEntryType.CHECKLIST, title = "Race prep", pinned = true,
            items = listOf(
                ChecklistItem(id = 1, entryId = 1, text = "Register for the October 10K", done = true, position = 0),
                ChecklistItem(id = 2, entryId = 1, text = "Buy new trainers", done = true, position = 1),
                ChecklistItem(id = 3, entryId = 1, text = "Build up to a 10-mile long run", done = false, position = 2),
                ChecklistItem(id = 4, entryId = 1, text = "Book a physio check on the knee", done = false, position = 3),
            ),
            createdAt = at(today, 7, 30), updatedAt = at(today, 7, 30),
        ),
        JournalEntry(
            id = 2, goalId = run.id, type = JournalEntryType.ENTRY, mood = Mood.STRONG,
            body = "Heavy legs for the first two miles, then negative-split the last three. Knee held up fine — keeping the Tuesday strength session.",
            createdAt = at(today.minusDays(1), 19, 42), updatedAt = at(today.minusDays(1), 19, 42),
        ),
        JournalEntry(
            id = 3, goalId = run.id, type = JournalEntryType.NOTE, title = "Training paces",
            body = "Easy 10:30 /mi\nTempo 8:45 /mi\nLong run: easy pace + 60 s",
            createdAt = at(today.minusDays(3), 12, 5), updatedAt = at(today.minusDays(3), 12, 5),
        ),
        JournalEntry(
            id = 4, goalId = run.id, type = JournalEntryType.ENTRY, mood = Mood.GREAT,
            body = "Ten miles — longest run ever. Slow, but I didn't stop once.",
            createdAt = at(today.minusDays(5), 9, 15), updatedAt = at(today.minusDays(5), 9, 15),
        ),
        JournalEntry(
            id = 5, goalId = spanish.id, type = JournalEntryType.ENTRY, mood = Mood.OKAY,
            body = "Finally understood the subjunctive in a real conversation. Small win.",
            createdAt = at(today.minusDays(2), 21, 10), updatedAt = at(today.minusDays(2), 21, 10),
        ),
    )
}
