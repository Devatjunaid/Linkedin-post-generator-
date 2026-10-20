package com.superdevlabs.coldplunge.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class, ProtocolEntity::class], version = 1, exportSchema = false)
abstract class PlungeDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun protocols(): ProtocolDao

    companion object {
        fun build(context: Context): PlungeDatabase =
            Room.databaseBuilder(context, PlungeDatabase::class.java, "plunge.db").build()
    }
}
