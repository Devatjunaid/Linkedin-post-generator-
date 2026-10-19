package com.dailycash.tracker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One cash movement: money spent from, or added to, a currency wallet. */
@Entity(tableName = "entries", indices = [Index("timestamp"), Index("currency")])
data class CashEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val label: String,
    val note: String,
    val countryCode: String,
    val timestamp: Long,
    /** Home currency and value at the moment of saving, so history keeps the rate of that day. */
    val homeCurrency: String,
    val homeAmountMinor: Long?,
) {
    companion object {
        const val SPEND = 0
        const val ADD = 1
    }
}

fun CashEntry.isSpend() = type == CashEntry.SPEND

@Entity(tableName = "custom_categories", indices = [Index(value = ["name"], unique = true)])
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

data class CurrencyBalance(val currency: String, val balance: Long)

@Dao
interface CashDao {
    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun observeEntries(): Flow<List<CashEntry>>

    @Query(
        "SELECT currency, SUM(CASE WHEN type = 1 THEN amountMinor ELSE -amountMinor END) AS balance " +
            "FROM entries GROUP BY currency ORDER BY MAX(timestamp) DESC"
    )
    fun observeBalances(): Flow<List<CurrencyBalance>>

    @Insert
    suspend fun insert(entry: CashEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CashEntry)

    @Delete
    suspend fun delete(entry: CashEntry)

    @Query("SELECT * FROM custom_categories ORDER BY name COLLATE NOCASE")
    fun observeCustomCategories(): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomCategory(category: CustomCategory): Long

    @Delete
    suspend fun deleteCustomCategory(category: CustomCategory)
}

@Database(entities = [CashEntry::class, CustomCategory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cashDao(): CashDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "daily_cash.db").build()
    }
}
