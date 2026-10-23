package com.walkies.dogwalk.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LogEntry::class, Walk::class, Reminder::class],
    version = 1,
    exportSchema = false,
)
abstract class WalkiesDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun walkDao(): WalkDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        fun build(context: Context): WalkiesDatabase =
            Room.databaseBuilder(context, WalkiesDatabase::class.java, "walkies.db").build()
    }
}
