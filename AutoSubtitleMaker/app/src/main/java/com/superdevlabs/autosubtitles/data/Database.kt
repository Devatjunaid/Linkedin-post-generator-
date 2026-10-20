package com.superdevlabs.autosubtitles.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

object ProjectStatus {
    const val NEW = "NEW"
    const val READY = "READY"
    const val FAILED = "FAILED"
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Content URI returned by the photo picker; only read once while importing. */
    val sourceUri: String,
    /** App-private copy of the video, so the project still works after the URI grant expires. */
    val videoPath: String?,
    val thumbnailPath: String?,
    val durationMs: Long,
    /** Display size (rotation already applied). */
    val width: Int,
    val height: Int,
    val languageId: String,
    val status: String,
    val captionsJson: String,
    val styleJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projects(): ProjectDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "subtitles.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
