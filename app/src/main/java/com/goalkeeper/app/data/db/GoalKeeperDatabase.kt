package com.goalkeeper.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GoalEntity::class, CheckInEntity::class, JournalEntryEntity::class, ChecklistItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GoalKeeperDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun checkInDao(): CheckInDao
    abstract fun journalDao(): JournalDao

    companion object {
        const val FILE_NAME = "goal_keeper.db"

        fun build(context: Context): GoalKeeperDatabase =
            Room.databaseBuilder(context.applicationContext, GoalKeeperDatabase::class.java, FILE_NAME).build()
    }
}
