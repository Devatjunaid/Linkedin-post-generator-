package app.gigometer.mileage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    /** Finished trips only, newest first. */
    @Query("SELECT * FROM trips WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun observeFinished(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE endTime IS NOT NULL AND startTime >= :from AND startTime < :to ORDER BY startTime ASC")
    suspend fun finishedBetween(from: Long, to: Long): List<Trip>

    @Query("SELECT * FROM trips WHERE endTime IS NOT NULL ORDER BY startTime ASC")
    suspend fun allFinished(): List<Trip>

    @Query("SELECT COUNT(*) FROM trips WHERE endTime IS NOT NULL AND category = 'UNREVIEWED'")
    suspend fun unreviewedCount(): Int

    @Query("SELECT * FROM trips WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun openTrip(): Trip?

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun byId(id: Long): Trip?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observe(id: Long): Flow<Trip?>

    @Insert
    suspend fun insert(trip: Trip): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<Trip>)

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE trips SET category = :category, platform = COALESCE(:platform, platform) WHERE id = :id")
    suspend fun setCategory(id: Long, category: String, platform: String?)
}
