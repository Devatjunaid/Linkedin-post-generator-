package app.gigometer.mileage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trip::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trips(): TripDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "gigometer.db")
                .build()
                .also { instance = it }
        }
    }
}
