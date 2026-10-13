package com.snaptensio.bplog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id")
    fun observeAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles ORDER BY id")
    suspend fun getAll(): List<Profile>

    @Insert
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)
}

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeForProfile(profileId: Long): Flow<List<Reading>>

    @Query("SELECT * FROM readings WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getForProfile(profileId: Long): List<Reading>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun get(id: Long): Reading?

    @Insert
    suspend fun insert(reading: Reading): Long

    @Insert
    suspend fun insertAll(readings: List<Reading>)

    @Update
    suspend fun update(reading: Reading)

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY name")
    fun observeForProfile(profileId: Long): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE active = 1")
    suspend fun getAllActive(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun get(id: Long): Medication?

    @Insert
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT d.* FROM dose_logs d JOIN medications m ON m.id = d.medicationId WHERE m.profileId = :profileId AND d.dateKey >= :fromDateKey")
    fun observeDoseLogs(profileId: Long, fromDateKey: Int): Flow<List<DoseLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoseLog(log: DoseLog): Long

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND dateKey = :dateKey AND time = :time")
    suspend fun deleteDoseLog(medicationId: Long, dateKey: Int, time: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour, minute")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)
}
