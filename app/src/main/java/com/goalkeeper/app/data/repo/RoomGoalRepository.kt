package com.goalkeeper.app.data.repo

import androidx.room.withTransaction
import com.goalkeeper.app.data.db.CheckInEntity
import com.goalkeeper.app.data.db.GoalKeeperDatabase
import com.goalkeeper.app.data.db.toDomain
import com.goalkeeper.app.data.db.toEntity
import com.goalkeeper.app.notifications.ReminderSync
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.model.CheckIn
import com.goalkeeper.core.model.CheckInStatus
import com.goalkeeper.core.model.Goal
import com.goalkeeper.core.model.Importance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomGoalRepository(
    private val db: GoalKeeperDatabase,
    private val reminders: ReminderSync,
    private val clock: AppClock,
) : GoalRepository {

    private val goals = db.goalDao()
    private val checkIns = db.checkInDao()

    override fun observeActiveGoals(): Flow<List<Goal>> =
        goals.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeArchivedGoals(): Flow<List<Goal>> =
        goals.observeArchived().map { list -> list.map { it.toDomain() } }

    override fun observeAllGoals(): Flow<List<Goal>> =
        // The query is ranked; a stable sort moves archived goals after the active ones.
        goals.observeAll().map { list -> list.map { it.toDomain() }.sortedBy { it.archived } }

    override fun observeGoal(id: Long): Flow<Goal?> = goals.observe(id).map { it?.toDomain() }

    override suspend fun getGoal(id: Long): Goal? = goals.get(id)?.toDomain()

    override suspend fun getActiveGoals(): List<Goal> = goals.getActive().map { it.toDomain() }

    override suspend fun saveGoal(goal: Goal): Long {
        val cleaned = goal.copy(title = goal.title.trim(), why = goal.why.trim())
        val id = db.withTransaction {
            val existing = if (cleaned.id != 0L) goals.get(cleaned.id) else null
            if (existing == null) {
                val rank = goals.maxSortOrder(cleaned.importance.level) + 1
                goals.insert(cleaned.copy(id = 0, rank = rank).toEntity())
            } else {
                val movedTier = existing.importance != cleaned.importance.level
                val rank = if (movedTier) goals.maxSortOrder(cleaned.importance.level) + 1 else existing.sortOrder
                // The editor never changes when a goal started or whether it's archived.
                goals.update(
                    cleaned.toEntity().copy(
                        sortOrder = rank,
                        startEpochDay = existing.startEpochDay,
                        createdAt = existing.createdAt,
                        archived = existing.archived,
                        achievedAt = existing.achievedAt,
                    ),
                )
                existing.id
            }
        }
        reminders.sync(id)
        return id
    }

    override suspend fun deleteGoal(id: Long) {
        reminders.cancel(id)
        goals.delete(id)
    }

    override suspend fun setArchived(id: Long, archived: Boolean, achieved: Boolean) {
        val achievedAt = if (archived && achieved) clock.instant().toEpochMilli() else null
        goals.setArchived(id, archived, achievedAt)
        reminders.sync(id)
    }

    override suspend fun saveRanking(ordered: List<Pair<Long, Importance>>) {
        val movedTier = db.withTransaction {
            val stored = goals.getActive().associateBy { it.id }
            val updates = planRanking(ordered, stored.mapValues { (_, e) -> StoredRank(e.importance, e.nudgeCount) })
            for (update in updates) {
                val entity = stored[update.id] ?: continue
                if (update.nudgeCount != entity.nudgeCount) {
                    goals.update(
                        entity.copy(
                            importance = update.importance.level,
                            sortOrder = update.sortOrder,
                            nudgeCount = update.nudgeCount,
                        ),
                    )
                } else {
                    goals.updateRanking(update.id, update.importance.level, update.sortOrder)
                }
            }
            updates.filter { it.importanceChanged }.map { it.id }
        }
        movedTier.forEach { reminders.sync(it) }
    }

    override fun observeCheckIns(goalId: Long): Flow<List<CheckIn>> =
        checkIns.observeForGoal(goalId).map { list -> list.map { it.toDomain() } }

    override fun observeCheckInsBetween(from: LocalDate, to: LocalDate): Flow<List<CheckIn>> =
        checkIns.observeBetween(from.toEpochDay(), to.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override fun observeAllCheckIns(): Flow<List<CheckIn>> =
        checkIns.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCheckIns(goalId: Long): List<CheckIn> = checkIns.getForGoal(goalId).map { it.toDomain() }

    override suspend fun getCheckIn(goalId: Long, date: LocalDate): CheckIn? =
        checkIns.get(goalId, date.toEpochDay())?.toDomain()

    override suspend fun setCheckIn(goalId: Long, date: LocalDate, status: CheckInStatus?, note: String?) {
        val epochDay = date.toEpochDay()
        if (status == null) {
            checkIns.delete(goalId, epochDay)
        } else {
            val existing = checkIns.get(goalId, epochDay)
            checkIns.upsert(
                CheckInEntity(
                    goalId = goalId,
                    epochDay = epochDay,
                    status = status.name,
                    note = note?.trim()?.takeIf { it.isNotEmpty() } ?: existing?.note,
                    // Keep the original time unless the status changed (it's shown as "done 07:12").
                    createdAt = existing?.takeIf { it.status == status.name }?.createdAt ?: clock.instant().toEpochMilli(),
                ),
            )
        }
        reminders.sync(goalId)
    }
}

/** What the database currently holds for a goal's ranking. */
internal data class StoredRank(val importanceLevel: Int, val nudgeCount: Int)

internal data class RankingUpdate(
    val id: Long,
    val importance: Importance,
    val sortOrder: Int,
    val nudgeCount: Int,
    val importanceChanged: Boolean,
)

/**
 * Turns the Rank screen's top-to-bottom list into per-goal updates: ranks restart at 0 in each tier, and a goal
 * that changes tier while still on its old tier's default nudge count gets the new tier's default (a customized
 * count is kept). Ids missing from [stored] are skipped.
 */
internal fun planRanking(ordered: List<Pair<Long, Importance>>, stored: Map<Long, StoredRank>): List<RankingUpdate> {
    val nextRank = mutableMapOf<Importance, Int>()
    return ordered.mapNotNull { (id, importance) ->
        val current = stored[id] ?: return@mapNotNull null
        val sortOrder = nextRank.getOrDefault(importance, 0)
        nextRank[importance] = sortOrder + 1
        val oldImportance = Importance.fromLevel(current.importanceLevel)
        val changed = oldImportance != importance
        val nudges = if (changed && current.nudgeCount == oldImportance.defaultNudges) importance.defaultNudges else current.nudgeCount
        RankingUpdate(id, importance, sortOrder, nudges, changed)
    }
}
