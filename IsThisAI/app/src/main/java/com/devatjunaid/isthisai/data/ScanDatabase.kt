package com.devatjunaid.isthisai.data

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

/** One saved scan. The full report is stored as JSON so it can be reopened exactly as it was. */
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val verdict: String,
    val strength: String,
    val aiTool: String?,
    val scannedAt: Long,
    val sizeBytes: Long,
    val previewPath: String?,
    val reportJson: String,
)

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY scannedAt DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans ORDER BY scannedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun get(id: Long): ScanEntity?

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scans")
    suspend fun clear()

    @Query("SELECT previewPath FROM scans WHERE previewPath IS NOT NULL")
    suspend fun allPreviewPaths(): List<String>
}

@Database(entities = [ScanEntity::class], version = 1, exportSchema = true)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scans(): ScanDao

    companion object {
        fun create(context: Context): ScanDatabase =
            Room.databaseBuilder(context, ScanDatabase::class.java, "scans.db").build()
    }
}
