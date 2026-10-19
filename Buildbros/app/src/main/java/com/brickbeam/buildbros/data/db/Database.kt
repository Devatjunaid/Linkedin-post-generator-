package com.brickbeam.buildbros.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val regionCode: String,
    /** Extra local/city sales tax (US) added on top of the state rate. */
    val localTaxPct: Double = 0.0,
    val contingencyPct: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val budget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "line_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    /** [com.brickbeam.buildbros.calc.Category] name. */
    val category: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val owned: Boolean = false,
    /** Where it came from, e.g. "Brick wall 20′ × 8′". */
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class ProjectSummary(
    val id: Long,
    val name: String,
    val regionCode: String,
    val updatedAt: Long,
    val itemCount: Int,
    val materials: Double,
)

@Dao
interface ProjectDao {
    @Query(
        """
        SELECT p.id, p.name, p.regionCode, p.updatedAt,
               COUNT(li.id) AS itemCount,
               COALESCE(SUM(CASE WHEN li.owned = 0 THEN li.quantity * li.unitPrice ELSE 0 END), 0) AS materials
        FROM projects p LEFT JOIN line_items li ON li.projectId = p.id
        GROUP BY p.id ORDER BY p.updatedAt DESC
        """,
    )
    fun observeSummaries(): Flow<List<ProjectSummary>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProject(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: Long): ProjectEntity?

    @Query("SELECT * FROM line_items WHERE projectId = :projectId ORDER BY createdAt ASC, id ASC")
    fun observeItems(projectId: Long): Flow<List<LineItemEntity>>

    @Query("SELECT * FROM line_items WHERE projectId = :projectId ORDER BY createdAt ASC, id ASC")
    suspend fun getItems(projectId: Long): List<LineItemEntity>

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Insert
    suspend fun insertItems(items: List<LineItemEntity>)

    @Update
    suspend fun updateItem(item: LineItemEntity)

    @Query("DELETE FROM line_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE projects SET updatedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long = System.currentTimeMillis())
}

@Database(entities = [ProjectEntity::class, LineItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "buildbros.db").build()
    }
}
