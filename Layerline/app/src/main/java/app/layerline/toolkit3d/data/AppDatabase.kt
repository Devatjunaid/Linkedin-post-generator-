package app.layerline.toolkit3d.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterDao {
    @Query("SELECT * FROM printers ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Printer>>

    @Query("SELECT * FROM printers WHERE id = :id")
    suspend fun get(id: Long): Printer?

    @Insert
    suspend fun insert(printer: Printer): Long

    @Update
    suspend fun update(printer: Printer)

    @Delete
    suspend fun delete(printer: Printer)

    @Query("UPDATE printers SET hoursRun = hoursRun + :hours WHERE id = :id")
    suspend fun addHours(id: Long, hours: Double)

    @Query("SELECT * FROM maintenance ORDER BY id ASC")
    fun observeMaintenance(): Flow<List<MaintenanceTask>>

    @Insert
    suspend fun insertTasks(tasks: List<MaintenanceTask>)

    @Insert
    suspend fun insertTask(task: MaintenanceTask): Long

    @Update
    suspend fun updateTask(task: MaintenanceTask)

    @Delete
    suspend fun deleteTask(task: MaintenanceTask)
}

@Dao
interface SpoolDao {
    @Query("SELECT * FROM spools ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Spool>>

    @Query("SELECT * FROM spools WHERE id = :id")
    suspend fun get(id: Long): Spool?

    @Insert
    suspend fun insert(spool: Spool): Long

    @Update
    suspend fun update(spool: Spool)

    @Delete
    suspend fun delete(spool: Spool)

    @Query("UPDATE spools SET remainingGrams = MAX(0, remainingGrams - :grams) WHERE id = :id")
    suspend fun consume(id: Long, grams: Double)
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<PrintJob>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun get(id: Long): PrintJob?

    @Insert
    suspend fun insert(job: PrintJob): Long

    @Update
    suspend fun update(job: PrintJob)

    @Delete
    suspend fun delete(job: PrintJob)
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Quote>>

    @Insert
    suspend fun insert(quote: Quote): Long

    @Delete
    suspend fun delete(quote: Quote)
}

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibrations ORDER BY createdAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<CalibrationRecord>>

    @Insert
    suspend fun insert(record: CalibrationRecord): Long

    @Delete
    suspend fun delete(record: CalibrationRecord)
}

@Database(
    entities = [Printer::class, MaintenanceTask::class, Spool::class, PrintJob::class, Quote::class, CalibrationRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun printers(): PrinterDao
    abstract fun spools(): SpoolDao
    abstract fun jobs(): JobDao
    abstract fun quotes(): QuoteDao
    abstract fun calibrations(): CalibrationDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "layerline.db").build()
    }
}
