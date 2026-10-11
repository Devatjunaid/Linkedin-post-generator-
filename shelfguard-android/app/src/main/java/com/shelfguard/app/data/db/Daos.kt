package com.shelfguard.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProductWithBatches>>

    @Transaction
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<ProductWithBatches>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    fun observe(id: Long): Flow<ProductWithBatches?>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun get(id: Long): ProductWithBatches?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category COLLATE NOCASE")
    fun observeCategories(): Flow<List<String>>

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE productId = :productId")
    suspend fun forProduct(productId: Long): List<BatchEntity>

    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun get(id: Long): BatchEntity?

    @Query("SELECT * FROM batches WHERE expiryEpochDay IS NOT NULL AND expiryEpochDay < :today")
    suspend fun expired(today: Long): List<BatchEntity>

    @Insert
    suspend fun insert(batch: BatchEntity): Long

    @Insert
    suspend fun insertAll(batches: List<BatchEntity>)

    @Update
    suspend fun update(batch: BatchEntity)

    @Delete
    suspend fun delete(batch: BatchEntity)
}

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun observeSince(since: Long): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE productId = :productId ORDER BY timestamp DESC LIMIT :limit")
    fun observeForProduct(productId: Long, limit: Int): Flow<List<MovementEntity>>

    @Insert
    suspend fun insert(movement: MovementEntity)

    @Query("DELETE FROM movements")
    suspend fun deleteAll()
}
