package com.odomate.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val plate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** "2019 Honda Civic" style description. */
val Vehicle.subtitle: String
    get() = listOfNotNull(year?.toString(), make.takeIf { it.isNotBlank() }, model.takeIf { it.isNotBlank() })
        .joinToString(" ")

object ReadingSource {
    const val INITIAL = "initial"
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val SERVICE = "service"
    const val FUEL = "fuel"
}

@Entity(
    tableName = "readings",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("vehicleId")],
)
data class OdometerReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val value: Int,
    val date: Long,
    val source: String,
)

@Entity(
    tableName = "service_types",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("vehicleId")],
)
data class ServiceType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val iconKey: String,
    val category: String,
    /** Distance interval in the user's distance unit, or null for time-only services. */
    val intervalDistance: Int?,
    /** Time interval in months, or null for distance-only services. */
    val intervalMonths: Int?,
    val remindDistanceBefore: Int,
    val remindDaysBefore: Int,
    /** "Last done" the user entered by hand (before any logged record). */
    val baselineOdometer: Int? = null,
    val baselineDate: Long? = null,
    /** Effective last-done point: latest logged record, else the baseline. */
    val lastDoneOdometer: Int? = null,
    val lastDoneDate: Long? = null,
    val enabled: Boolean = true,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0,
    val notes: String = "",
    /** 0 = none, 1 = due-soon sent, 2 = overdue sent. Reset when the service is logged. */
    val notifiedLevel: Int = 0,
    val notifiedAt: Long = 0,
)

@Entity(
    tableName = "service_records",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("vehicleId")],
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val odometer: Int,
    val cost: Double?,
    val shop: String = "",
    val notes: String = "",
    val receiptPath: String? = null,
    val serviceTypeIds: List<Long>,
    /** Names at the time of logging, so history survives renames/deletes. */
    val serviceNames: String,
)

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("vehicleId")],
)
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val odometer: Int,
    val volume: Double,
    val cost: Double?,
    val fullTank: Boolean = true,
    val station: String = "",
    val notes: String = "",
)
