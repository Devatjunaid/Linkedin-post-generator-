package com.inkformlabs.pdfsigner.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observe(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun get(id: String): DocumentEntity?

    @Upsert
    suspend fun upsert(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SignatureEntity>>

    @Query("SELECT * FROM signatures WHERE id = :id")
    suspend fun get(id: String): SignatureEntity?

    @Upsert
    suspend fun upsert(signature: SignatureEntity)

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun delete(id: String)
}
