package com.harrycodess.fueltracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Default efficiency used to pre-fill the calculator, stored in km/L. */
    val efficiencyKmPerL: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A saved trip. All values are stored in metric units; the UI converts on display.
 * [vehicleName] is a snapshot so history stays readable if the vehicle is deleted.
 */
@Entity(
    tableName = "trips",
    indices = [Index("vehicleId"), Index("dateEpochDay")],
)
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long?,
    val vehicleName: String,
    val dateEpochDay: Long,
    val distanceKm: Double,
    val fuelPricePerLiter: Double,
    val efficiencyKmPerL: Double,
    val fuelLiters: Double,
    val totalCost: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

val Trip.date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
