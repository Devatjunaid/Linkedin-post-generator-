package com.superdevlabs.coldplunge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<SessionEntity>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ProtocolDao {
    @Query("SELECT * FROM protocols ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProtocolEntity>>

    @Upsert
    suspend fun upsert(protocol: ProtocolEntity): Long

    @Query("DELETE FROM protocols WHERE id = :id")
    suspend fun delete(id: Long)
}
