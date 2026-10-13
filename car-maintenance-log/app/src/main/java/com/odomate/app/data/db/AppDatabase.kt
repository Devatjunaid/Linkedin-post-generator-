package com.odomate.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Vehicle::class, OdometerReading::class, ServiceType::class, ServiceRecord::class, FuelEntry::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicles(): VehicleDao
    abstract fun readings(): ReadingDao
    abstract fun serviceTypes(): ServiceTypeDao
    abstract fun records(): ServiceRecordDao
    abstract fun fuel(): FuelDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "odomate.db").build()
    }
}
