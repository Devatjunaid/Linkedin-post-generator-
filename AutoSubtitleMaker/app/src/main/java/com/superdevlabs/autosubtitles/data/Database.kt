package com.superdevlabs.autosubtitles.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
    /**
     * Fast engine: Vosk model id (e.g. "en-us").
     * Accurate engine: Whisper language code, or "auto".
     */
    val languageId: String,
    val status: String,
    val captionsJson: String,
    val styleJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** "vosk" or a Whisper model id such as "whisper-base". */
    @ColumnInfo(defaultValue = "vosk") val engine: String = "vosk",
    /** Language actually spoken (ISO 639-1), filled in after transcription. Used for translation. */
    @ColumnInfo(defaultValue = "") val sourceLanguage: String = "",
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

@Database(entities = [ProjectEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projects(): ProjectDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "subtitles.db")
                .addMigrations(MIGRATION_1_2)
                // Only a downgrade (installing an older build over a newer one) may wipe data.
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()

        /** v1.1: Whisper engine + detected language. Keeps every existing project. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE projects ADD COLUMN engine TEXT NOT NULL DEFAULT 'vosk'")
                connection.execSQL("ALTER TABLE projects ADD COLUMN sourceLanguage TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
