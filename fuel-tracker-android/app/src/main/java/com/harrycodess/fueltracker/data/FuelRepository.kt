package com.harrycodess.fueltracker.data

import androidx.room.withTransaction
import com.harrycodess.fueltracker.data.local.AppDatabase
import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.data.local.Vehicle
import kotlinx.coroutines.flow.Flow

class FuelRepository(private val db: AppDatabase) {
    private val tripDao = db.tripDao()
    private val vehicleDao = db.vehicleDao()

    val trips: Flow<List<Trip>> = tripDao.observeAll()
    val vehicles: Flow<List<Vehicle>> = vehicleDao.observeAll()

    suspend fun getTrip(id: Long): Trip? = tripDao.getById(id)
    suspend fun getAllTrips(): List<Trip> = tripDao.getAll()
    suspend fun saveTrip(trip: Trip): Long = tripDao.upsert(trip)
    suspend fun deleteTrip(trip: Trip) = tripDao.delete(trip)

    suspend fun addVehicle(name: String, efficiencyKmPerL: Double): Long =
        vehicleDao.insert(Vehicle(name = name, efficiencyKmPerL = efficiencyKmPerL))

    suspend fun updateVehicle(vehicle: Vehicle) = db.withTransaction {
        vehicleDao.update(vehicle)
        tripDao.renameVehicle(vehicle.id, vehicle.name)
    }

    /** Deletes the vehicle; its trips are kept and still show the vehicle's name. */
    suspend fun deleteVehicle(vehicle: Vehicle) = db.withTransaction {
        tripDao.detachVehicle(vehicle.id)
        vehicleDao.delete(vehicle)
    }
}
