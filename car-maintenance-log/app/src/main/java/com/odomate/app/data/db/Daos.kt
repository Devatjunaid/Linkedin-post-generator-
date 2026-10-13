package com.odomate.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles ORDER BY createdAt ASC")
    suspend fun getAll(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun get(id: Long): Vehicle?

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()
}

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings WHERE vehicleId = :vehicleId ORDER BY date DESC, value DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<OdometerReading>>

    @Query("SELECT * FROM readings WHERE vehicleId = :vehicleId ORDER BY date DESC, value DESC")
    suspend fun getForVehicle(vehicleId: Long): List<OdometerReading>

    @Query("SELECT * FROM readings")
    suspend fun getAll(): List<OdometerReading>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun get(id: Long): OdometerReading?

    @Insert
    suspend fun insert(reading: OdometerReading): Long

    @Update
    suspend fun update(reading: OdometerReading)

    @Delete
    suspend fun delete(reading: OdometerReading)
}

@Dao
interface ServiceTypeDao {
    @Query("SELECT * FROM service_types WHERE vehicleId = :vehicleId ORDER BY sortOrder ASC, id ASC")
    fun observeForVehicle(vehicleId: Long): Flow<List<ServiceType>>

    @Query("SELECT * FROM service_types WHERE vehicleId = :vehicleId ORDER BY sortOrder ASC, id ASC")
    suspend fun getForVehicle(vehicleId: Long): List<ServiceType>

    @Query("SELECT * FROM service_types")
    suspend fun getAll(): List<ServiceType>

    @Query("SELECT * FROM service_types WHERE id = :id")
    fun observe(id: Long): Flow<ServiceType?>

    @Query("SELECT * FROM service_types WHERE id = :id")
    suspend fun get(id: Long): ServiceType?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM service_types WHERE vehicleId = :vehicleId")
    suspend fun maxSortOrder(vehicleId: Long): Int

    @Insert
    suspend fun insert(type: ServiceType): Long

    @Insert
    suspend fun insertAll(types: List<ServiceType>)

    @Update
    suspend fun update(type: ServiceType)

    @Delete
    suspend fun delete(type: ServiceType)
}

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    suspend fun getForVehicle(vehicleId: Long): List<ServiceRecord>

    @Query("SELECT * FROM service_records")
    suspend fun getAll(): List<ServiceRecord>

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun get(id: Long): ServiceRecord?

    @Insert
    suspend fun insert(record: ServiceRecord): Long

    @Update
    suspend fun update(record: ServiceRecord)

    @Delete
    suspend fun delete(record: ServiceRecord)
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries")
    suspend fun getAll(): List<FuelEntry>

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun get(id: Long): FuelEntry?

    @Insert
    suspend fun insert(entry: FuelEntry): Long

    @Update
    suspend fun update(entry: FuelEntry)

    @Delete
    suspend fun delete(entry: FuelEntry)
}
