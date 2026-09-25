package com.goalkeeper.core.streak

import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.DayStatus
import com.goalkeeper.core.model.Goal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Aggregate streak numbers for one goal as of a given day. */
data class StreakStats(
    /** DONE days in the unbroken chain ending today (a pending today does not break it). */
    val current: Int,
    /** Longest chain ever, using the same rules as [current]. Always >= [current]. */
    val best: Int,
    /** All DONE check-ins on or before today. */
    val totalDone: Int,
    /** done / (done + missed) over the last 30 days (today inclusive); null when there is nothing to rate. */
    val completionRate30: Float?,
    /** Status of today. */
    val today: DayStatus,
    /** True when today is [DayStatus.PENDING] and [current] > 0 — the streak dies at midnight without a check-in. */
    val atRisk: Boolean,
    /** Smallest entry of [StreakCalculator.MILESTONES] greater than [current], or null past the last one. */
    val nextMilestone: Int?,
)

/**
 * Streak rules (single source of truth for the whole app):
 *
 * - The *effective start* of a goal is the earlier of [Goal.startDate] and its earliest check-in date,
 *   so back-filled check-ins count.
 * - Day status, evaluated in this order:
 *   1. date after today -> [DayStatus.FUTURE]
 *   2. a check-in exists -> [DayStatus.DONE] or [DayStatus.SKIPPED] (on any day, scheduled or not)
 *   3. date before the effective start -> [DayStatus.BEFORE_START]
 *   4. schedule does not include the date -> [DayStatus.REST]
 *   5. date is today -> [DayStatus.PENDING]
 *   6. otherwise -> [DayStatus.MISSED]
 * - Walking a chain: DONE adds one; SKIPPED, REST and PENDING are neutral; MISSED and BEFORE_START end it.
 */
object StreakCalculator {

    val MILESTONES: List<Int> = listOf(3, 7, 14, 21, 30, 50, 75, 100, 150, 200, 250, 300, 365, 500, 750, 1000)

    /** Full statistics for [goal]. [checkIns] must only contain this goal's check-ins. */
    fun stats(goal: Goal, checkIns: Collection<CheckIn>, today: LocalDate): StreakStats {
        val checkInMap = checkIns.associate { it.date to it.status }
        val start = effectiveStart(goal, checkInMap.keys)

        val current = currentStreak(goal, checkInMap, start, today)
        val best = bestStreak(goal, checkInMap, start, today)
        val totalDone = checkInMap.count { (date, status) -> status == CheckInStatus.DONE && !date.isAfter(today) }
        val rate30 = completionRate(goal, checkInMap, start, today.minusDays(29), today, today)
        val todayStatus = statusOn(goal, checkInMap, start, today, today)
        val atRisk = todayStatus == DayStatus.PENDING && current > 0
        val nextMilestone = MILESTONES.firstOrNull { it > current }

        return StreakStats(
            current = current,
            best = best,
            totalDone = totalDone,
            completionRate30 = rate30,
            today = todayStatus,
            atRisk = atRisk,
            nextMilestone = nextMilestone,
        )
    }

    /** Status of one [date]. [checkIns] maps dates to this goal's check-in status. */
    fun dayStatus(goal: Goal, checkIns: Map<LocalDate, CheckInStatus>, date: LocalDate, today: LocalDate): DayStatus {
        val start = effectiveStart(goal, checkIns.keys)
        return statusOn(goal, checkIns, start, date, today)
    }

    /** Status of every day from [from] to [to] inclusive, in ascending date order. Empty if [from] > [to]. */
    fun dayStatuses(
        goal: Goal,
        checkIns: Collection<CheckIn>,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
    ): List<Pair<LocalDate, DayStatus>> {
        if (from.isAfter(to)) return emptyList()
        val checkInMap = checkIns.associate { it.date to it.status }
        val start = effectiveStart(goal, checkInMap.keys)

        val result = ArrayList<Pair<LocalDate, DayStatus>>()
        var date = from
        while (!date.isAfter(to)) {
            result.add(date to statusOn(goal, checkInMap, start, date, today))
            date = date.plusDays(1)
        }
        return result
    }

    /**
     * Progress (0..1) through the Monday–Sunday week that contains [today]:
     * DONE days in that week divided by the week's scheduled days on/after the effective start,
     * with SKIPPED days removed from the denominator. Future scheduled days stay in the denominator,
     * so the value fills up as the week goes on. Returns 0 when the denominator is 0; never exceeds 1.
     */
    fun weekProgress(goal: Goal, checkIns: Collection<CheckIn>, today: LocalDate): Float {
        val checkInMap = checkIns.associate { it.date to it.status }
        val start = effectiveStart(goal, checkInMap.keys)
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        var numerator = 0
        var denominator = 0
        var date = monday
        repeat(7) {
            val checkInStatus = checkInMap[date]
            if (checkInStatus == CheckInStatus.DONE) numerator++
            if (goal.schedule.isScheduled(date) && !date.isBefore(start) && checkInStatus != CheckInStatus.SKIPPED) {
                denominator++
            }
            date = date.plusDays(1)
        }
        return if (denominator == 0) 0f else (numerator.toFloat() / denominator).coerceAtMost(1f)
    }

    /**
     * The effective start of [goal]: the earlier of [Goal.startDate] and the earliest of [checkInDates].
     * Exposed internally so callers that evaluate many dates (see [InsightsCalculator][com.goalkeeper.core.insights.InsightsCalculator])
     * can compute it once instead of rescanning the check-in map on every [statusOn] call.
     */
    internal fun effectiveStart(goal: Goal, checkInDates: Iterable<LocalDate>): LocalDate {
        val earliestCheckIn = checkInDates.minOrNull() ?: return goal.startDate
        return if (earliestCheckIn.isBefore(goal.startDate)) earliestCheckIn else goal.startDate
    }

    /** Same rules as [dayStatus], but takes an already-computed [effectiveStart] to avoid recomputing it. */
    internal fun statusOn(
        goal: Goal,
        checkIns: Map<LocalDate, CheckInStatus>,
        effectiveStart: LocalDate,
        date: LocalDate,
        today: LocalDate,
    ): DayStatus {
        if (date.isAfter(today)) return DayStatus.FUTURE
        checkIns[date]?.let {
            return when (it) {
                CheckInStatus.DONE -> DayStatus.DONE
                CheckInStatus.SKIPPED -> DayStatus.SKIPPED
            }
        }
        if (date.isBefore(effectiveStart)) return DayStatus.BEFORE_START
        if (!goal.schedule.isScheduled(date)) return DayStatus.REST
        if (date == today) return DayStatus.PENDING
        return DayStatus.MISSED
    }

    /** Walks backwards from [today] to [start], counting the unbroken DONE chain ending today. */
    private fun currentStreak(goal: Goal, checkIns: Map<LocalDate, CheckInStatus>, start: LocalDate, today: LocalDate): Int {
        var count = 0
        var date = today
        while (!date.isBefore(start)) {
            when (statusOn(goal, checkIns, start, date, today)) {
                DayStatus.DONE -> count++
                DayStatus.SKIPPED, DayStatus.REST, DayStatus.PENDING -> Unit
                DayStatus.MISSED, DayStatus.BEFORE_START, DayStatus.FUTURE -> return count
            }
            date = date.minusDays(1)
        }
        return count
    }

    /** Walks forward from [start] to [today], tracking the longest DONE run (a MISSED resets it to 0). */
    private fun bestStreak(goal: Goal, checkIns: Map<LocalDate, CheckInStatus>, start: LocalDate, today: LocalDate): Int {
        var run = 0
        var best = 0
        var date = start
        while (!date.isAfter(today)) {
            when (statusOn(goal, checkIns, start, date, today)) {
                DayStatus.DONE -> {
                    run++
                    if (run > best) best = run
                }
                DayStatus.MISSED -> run = 0
                DayStatus.SKIPPED, DayStatus.REST, DayStatus.PENDING, DayStatus.BEFORE_START, DayStatus.FUTURE -> Unit
            }
            date = date.plusDays(1)
        }
        return best
    }

    /** done / (done + missed) over [from]..[to] inclusive; null when there is nothing to rate. */
    private fun completionRate(
        goal: Goal,
        checkIns: Map<LocalDate, CheckInStatus>,
        start: LocalDate,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
    ): Float? {
        var done = 0
        var missed = 0
        var date = from
        while (!date.isAfter(to)) {
            when (statusOn(goal, checkIns, start, date, today)) {
                DayStatus.DONE -> done++
                DayStatus.MISSED -> missed++
                else -> Unit
            }
            date = date.plusDays(1)
        }
        val total = done + missed
        return if (total == 0) null else done.toFloat() / total
    }
}
