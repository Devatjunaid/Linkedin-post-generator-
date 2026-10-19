package com.sunarc.inverterlogger.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "readings", indices = [Index("inverterId"), Index("timestamp")])
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inverterId: Long,
    val timestamp: Long,
    val todayKwh: Double?,
    val totalKwh: Double?,
    val powerW: Double?,
    val source: String,
    val faultCode: String?,
    val note: String?,
    val confidence: Double?,
    val photoPath: String?
)

@Entity(tableName = "inverters")
data class InverterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brandId: String,
    val model: String,
    /** Address on the home network for SunSpec Modbus TCP, if the user set one. */
    val host: String?,
    val port: Int,
    val unitId: Int,
    val createdAt: Long
)

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings ORDER BY timestamp DESC")
    suspend fun all(): List<ReadingEntity>

    @Query("SELECT * FROM readings WHERE inverterId = :inverterId ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestFor(inverterId: Long): ReadingEntity?

    @Insert
    suspend fun insert(reading: ReadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<ReadingEntity>)

    @Update
    suspend fun update(reading: ReadingEntity)

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM readings")
    suspend fun clear()
}

@Dao
interface InverterDao {
    @Query("SELECT * FROM inverters ORDER BY createdAt")
    fun observeAll(): Flow<List<InverterEntity>>

    @Query("SELECT * FROM inverters ORDER BY createdAt")
    suspend fun all(): List<InverterEntity>

    @Insert
    suspend fun insert(inverter: InverterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inverters: List<InverterEntity>)

    @Update
    suspend fun update(inverter: InverterEntity)

    @Query("DELETE FROM inverters WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM inverters")
    suspend fun clear()
}

@Database(entities = [ReadingEntity::class, InverterEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readings(): ReadingDao
    abstract fun inverters(): InverterDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sunarc.db").build()
    }
}
