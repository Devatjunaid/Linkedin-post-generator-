package com.driveready.testprep.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "test_attempts")
data class TestAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val countryId: String,
    /** "theory" or "hazard". */
    val type: String,
    val score: Int,
    val total: Int,
    val passed: Boolean,
    val timestamp: Long,
    /** Weakest category label for theory attempts, empty otherwise. */
    val weakestCategory: String = ""
)

@Dao
interface TestAttemptDao {
    @Insert
    suspend fun insert(attempt: TestAttempt)

    @Query("SELECT * FROM test_attempts WHERE countryId = :countryId ORDER BY timestamp ASC")
    fun observeForCountry(countryId: String): Flow<List<TestAttempt>>

    @Query("DELETE FROM test_attempts")
    suspend fun clear()
}

@Database(entities = [TestAttempt::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attempts(): TestAttemptDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "driveready.db")
                .build()
                .also { instance = it }
        }
    }
}
