package com.superdevlabs.petfood.portioncalc.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY createdAt")
    fun observeAll(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets ORDER BY createdAt")
    suspend fun getAll(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE id = :id")
    fun observe(id: Long): Flow<PetEntity?>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun get(id: Long): PetEntity?

    @Insert
    suspend fun insert(pet: PetEntity): Long

    @Update
    suspend fun update(pet: PetEntity)

    @Delete
    suspend fun delete(pet: PetEntity)

    @Query("UPDATE pets SET primaryFoodId = NULL WHERE primaryFoodId = :foodId")
    suspend fun clearPrimaryFood(foodId: Long)

    @Query("UPDATE pets SET secondaryFoodId = NULL WHERE secondaryFoodId = :foodId")
    suspend fun clearSecondaryFood(foodId: Long)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY isPreset, name COLLATE NOCASE")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun get(id: Long): FoodEntity?

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observe(id: Long): Flow<FoodEntity?>

    @Query("SELECT COUNT(*) FROM foods WHERE isPreset = 1")
    suspend fun presetCount(): Int

    @Insert
    suspend fun insert(food: FoodEntity): Long

    @Insert
    suspend fun insertAll(foods: List<FoodEntity>)

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)
}

@Dao
interface FeedingLogDao {
    @Query("SELECT * FROM feeding_log WHERE epochDay = :epochDay")
    fun observeDay(epochDay: Long): Flow<List<FeedingLogEntity>>

    @Query("SELECT * FROM feeding_log WHERE petId = :petId AND epochDay >= :fromEpochDay ORDER BY epochDay DESC, mealIndex")
    fun observeSince(petId: Long, fromEpochDay: Long): Flow<List<FeedingLogEntity>>

    @Query("SELECT COUNT(*) FROM feeding_log WHERE petId = :petId AND epochDay = :epochDay AND mealIndex = :mealIndex")
    suspend fun count(petId: Long, epochDay: Long, mealIndex: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: FeedingLogEntity)

    @Query("DELETE FROM feeding_log WHERE petId = :petId AND epochDay = :epochDay AND mealIndex = :mealIndex")
    suspend fun delete(petId: Long, epochDay: Long, mealIndex: Int)

    @Query("DELETE FROM feeding_log WHERE epochDay < :beforeEpochDay")
    suspend fun deleteOlderThan(beforeEpochDay: Long)
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_log WHERE petId = :petId ORDER BY epochDay, id")
    fun observe(petId: Long): Flow<List<WeightLogEntity>>

    @Insert
    suspend fun insert(entry: WeightLogEntity): Long

    @Delete
    suspend fun delete(entry: WeightLogEntity)

    @Query("SELECT * FROM weight_log WHERE petId = :petId ORDER BY epochDay DESC, id DESC LIMIT 1")
    suspend fun latest(petId: Long): WeightLogEntity?
}

@Dao
interface WikiCacheDao {
    @Query("SELECT * FROM wiki_cache WHERE title = :title")
    suspend fun get(title: String): WikiCacheEntity?

    @Upsert
    suspend fun upsert(entry: WikiCacheEntity)
}
