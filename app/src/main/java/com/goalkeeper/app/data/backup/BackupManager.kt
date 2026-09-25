package com.goalkeeper.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.goalkeeper.app.data.db.GoalKeeperDatabase
import com.goalkeeper.app.data.db.toDomain
import com.goalkeeper.app.data.db.toEntity
import com.goalkeeper.app.data.settings.SettingsRepository
import com.goalkeeper.app.notifications.ReminderSync
import com.goalkeeper.app.util.AppClock
import com.goalkeeper.core.backup.BackupCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class BackupSummary(val goals: Int, val checkIns: Int, val journalEntries: Int)

/** Exports everything to a JSON file and restores it again (a restore replaces all current data). */
class BackupManager(
    context: Context,
    private val db: GoalKeeperDatabase,
    private val settings: SettingsRepository,
    private val reminders: ReminderSync,
    private val clock: AppClock,
) {
    private val resolver = context.applicationContext.contentResolver

    fun suggestedFileName(): String = "goal-keeper-backup-${clock.today()}.json"

    /** Writes a backup to [uri] (from the system file picker). Throws [IOException] if it can't be written. */
    suspend fun exportTo(uri: Uri): BackupSummary {
        val goals = db.goalDao().getAll().map { it.toDomain() }
        val checkIns = db.checkInDao().getAll().map { it.toDomain() }
        val journal = db.journalDao().getAll().map { it.toDomain() }
        val json = BackupCodec.encode(
            BackupCodec.toBackup(goals, checkIns, journal, settings.exportMap(), clock.instant()),
        )
        withContext(Dispatchers.IO) {
            val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file for writing")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        }
        return BackupSummary(goals.size, checkIns.size, journal.size)
    }

    /**
     * Replaces all goals, check-ins, journal entries and settings with the backup at [uri]. The file is fully
     * parsed and validated before anything is deleted. Throws [com.goalkeeper.core.backup.BackupFormatException]
     * for a file that isn't a valid backup and [IOException] when it can't be read.
     */
    suspend fun importFrom(uri: Uri): BackupSummary {
        val json = withContext(Dispatchers.IO) {
            val stream = resolver.openInputStream(uri) ?: throw IOException("Couldn't open the file")
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val contents = BackupCodec.fromBackup(BackupCodec.decode(json))

        db.withTransaction {
            val journalDao = db.journalDao()
            journalDao.deleteAllItems()
            journalDao.deleteAllEntries()
            db.checkInDao().deleteAll()
            db.goalDao().deleteAll()

            db.goalDao().insertAll(contents.goals.map { it.toEntity() })
            db.checkInDao().insertAll(contents.checkIns.map { it.toEntity() })
            journalDao.insertEntries(contents.journal.map { it.toEntity() })
            val items = contents.journal.flatMap { entry ->
                entry.items.map { item -> item.toEntity(entry.id).copy(id = 0) }
            }
            if (items.isNotEmpty()) journalDao.insertItems(items)
        }

        settings.importMap(contents.settings)
        reminders.syncAll()
        return BackupSummary(contents.goals.size, contents.checkIns.size, contents.journal.size)
    }
}
