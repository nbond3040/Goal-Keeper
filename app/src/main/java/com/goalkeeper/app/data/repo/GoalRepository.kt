package com.goalkeeper.app.data.repo

import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.Importance
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Goals and their daily check-ins. Every write re-syncs that goal's reminder alarms,
 * so callers never talk to the scheduler directly.
 */
interface GoalRepository {
    /** Active goals in ranked order (importance desc, rank asc, id asc). */
    fun observeActiveGoals(): Flow<List<Goal>>

    /** Archived goals, most recently achieved/created first. */
    fun observeArchivedGoals(): Flow<List<Goal>>

    /** Every goal, active ones first in ranked order. */
    fun observeAllGoals(): Flow<List<Goal>>

    fun observeGoal(id: Long): Flow<Goal?>

    suspend fun getGoal(id: Long): Goal?

    suspend fun getActiveGoals(): List<Goal>

    /**
     * Inserts ([Goal.id] == 0) or updates a goal and returns its id. New goals are placed last within their
     * importance (rank = max + 1). When an existing goal's importance changes, it moves to the end of its new tier.
     */
    suspend fun saveGoal(goal: Goal): Long

    /** Deletes the goal together with its check-ins and journal (cascade) and cancels its reminders. */
    suspend fun deleteGoal(id: Long)

    /** Archives or restores a goal. [achieved] records it as accomplished (sets achievedAt). */
    suspend fun setArchived(id: Long, archived: Boolean, achieved: Boolean = false)

    /**
     * Persists the manual ranking from the Rank screen: [ordered] is every active goal from top to bottom
     * with the importance tier it was dropped into. Rank restarts at 0 within each tier.
     */
    suspend fun saveRanking(ordered: List<Pair<Long, Importance>>)

    fun observeCheckIns(goalId: Long): Flow<List<CheckIn>>

    /** Check-ins of all goals with dates in [from]..[to]. */
    fun observeCheckInsBetween(from: LocalDate, to: LocalDate): Flow<List<CheckIn>>

    fun observeAllCheckIns(): Flow<List<CheckIn>>

    suspend fun getCheckIns(goalId: Long): List<CheckIn>

    suspend fun getCheckIn(goalId: Long, date: LocalDate): CheckIn?

    /** Sets the check-in for [date]; a null [status] removes it. */
    suspend fun setCheckIn(goalId: Long, date: LocalDate, status: CheckInStatus?, note: String? = null)
}
