package com.walkies.dogwalk.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntry): Long

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)

    @Query("SELECT * FROM log_entries WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp ASC")
    suspend fun between(from: Long, to: Long): List<LogEntry>

    @Query("SELECT * FROM log_entries WHERE type = :type ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(type: String): Flow<LogEntry?>

    @Query("SELECT * FROM log_entries WHERE type = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(type: String): LogEntry?

    @Query("SELECT * FROM log_entries WHERE walkId = :walkId ORDER BY timestamp DESC")
    fun observeForWalk(walkId: Long): Flow<List<LogEntry>>

    @Query("DELETE FROM log_entries WHERE walkId = :walkId")
    suspend fun deleteForWalk(walkId: Long)

    @Query(
        "SELECT walkId AS walkId, type AS type, COUNT(*) AS count FROM log_entries " +
            "WHERE walkId IS NOT NULL GROUP BY walkId, type",
    )
    fun observeWalkCounts(): Flow<List<WalkCount>>
}

@Dao
interface WalkDao {
    @Insert
    suspend fun insert(walk: Walk): Long

    @Update
    suspend fun update(walk: Walk)

    @Delete
    suspend fun delete(walk: Walk)

    @Query("SELECT * FROM walks WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<Walk?>

    @Query("SELECT * FROM walks WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun active(): Walk?

    @Query(
        "SELECT * FROM walks WHERE endTime IS NOT NULL AND startTime >= :from AND startTime < :to " +
            "ORDER BY startTime DESC",
    )
    fun observeFinishedBetween(from: Long, to: Long): Flow<List<Walk>>

    @Query(
        "SELECT * FROM walks WHERE endTime IS NOT NULL AND startTime >= :from AND startTime < :to " +
            "ORDER BY startTime ASC",
    )
    suspend fun finishedBetween(from: Long, to: Long): List<Walk>

    @Query("SELECT * FROM walks WHERE endTime IS NOT NULL ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Walk>>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour, minute")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY hour, minute")
    suspend fun all(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun byId(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(reminder: Reminder): Long

    @Insert
    suspend fun insertAll(reminders: List<Reminder>)

    @Delete
    suspend fun delete(reminder: Reminder)
}
