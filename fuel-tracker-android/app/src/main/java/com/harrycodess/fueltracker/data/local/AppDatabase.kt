package com.harrycodess.fueltracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Trip::class, Vehicle::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun vehicleDao(): VehicleDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fuel_tracker.db")
                .addCallback(SeedCallback)
                .build()
    }

    /** Creates a starter vehicle on first launch so the calculator is usable immediately. */
    private object SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO vehicles (name, efficiencyKmPerL, createdAt) VALUES (?, ?, ?)",
                arrayOf<Any>("My Car", 14.0, System.currentTimeMillis()),
            )
        }
    }
}
