package com.devatjunaid.renewalert.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE")
    abstract suspend fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    abstract suspend fun get(id: Long): Subscription?

    @Insert
    abstract suspend fun insert(sub: Subscription): Long

    @Update
    abstract suspend fun update(sub: Subscription)

    @Delete
    abstract suspend fun delete(sub: Subscription)

    @Query("DELETE FROM subscriptions")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM price_points ORDER BY effectiveEpochDay")
    abstract fun observePrices(): Flow<List<PricePoint>>

    @Query("SELECT * FROM price_points ORDER BY effectiveEpochDay")
    abstract suspend fun getAllPrices(): List<PricePoint>

    @Query("SELECT * FROM price_points WHERE subscriptionId = :id ORDER BY effectiveEpochDay")
    abstract suspend fun pricesFor(id: Long): List<PricePoint>

    @Insert
    abstract suspend fun insertPrice(p: PricePoint): Long

    @Query("DELETE FROM price_points WHERE subscriptionId = :id AND effectiveEpochDay = :day")
    abstract suspend fun deletePriceOn(id: Long, day: Long)

    @Transaction
    open suspend fun replaceAll(subs: List<Subscription>, prices: List<PricePoint>) {
        deleteAll()
        insertAll(subs, prices)
    }

    @Transaction
    open suspend fun insertAll(subs: List<Subscription>, prices: List<PricePoint>) {
        val idMap = HashMap<Long, Long>()
        for (s in subs) idMap[s.id] = insert(s.copy(id = 0))
        for (p in prices) {
            val newId = idMap[p.subscriptionId] ?: continue
            insertPrice(p.copy(id = 0, subscriptionId = newId))
        }
    }
}

@Database(entities = [Subscription::class, PricePoint::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SubscriptionDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "renewalert.db").build()
    }
}
