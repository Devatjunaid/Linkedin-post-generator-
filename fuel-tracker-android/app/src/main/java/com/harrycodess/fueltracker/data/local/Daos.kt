package com.harrycodess.fueltracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY dateEpochDay DESC, createdAt DESC")
    suspend fun getAll(): List<Trip>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): Trip?

    /** Inserts a new trip or replaces the one with the same id (used for edit and undo). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trip: Trip): Long

    @Delete
    suspend fun delete(trip: Trip)

    @Query("UPDATE trips SET vehicleName = :name WHERE vehicleId = :vehicleId")
    suspend fun renameVehicle(vehicleId: Long, name: String)

    @Query("UPDATE trips SET vehicleId = NULL WHERE vehicleId = :vehicleId")
    suspend fun detachVehicle(vehicleId: Long)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Vehicle>>

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)
}
