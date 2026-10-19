package com.devatjunaid.kelvin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class, ProtocolEntity::class], version = 1, exportSchema = false)
abstract class KelvinDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun protocols(): ProtocolDao

    companion object {
        fun build(context: Context): KelvinDatabase =
            Room.databaseBuilder(context, KelvinDatabase::class.java, "kelvin.db").build()
    }
}
