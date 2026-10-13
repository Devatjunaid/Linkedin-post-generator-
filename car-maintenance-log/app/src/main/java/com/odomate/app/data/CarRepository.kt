package com.odomate.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.odomate.app.data.db.AppDatabase
import com.odomate.app.data.db.FuelEntry
import com.odomate.app.data.db.OdometerReading
import com.odomate.app.data.db.ReadingSource
import com.odomate.app.data.db.ServiceRecord
import com.odomate.app.data.db.ServiceType
import com.odomate.app.data.db.Vehicle
import com.odomate.app.domain.ServiceStatus
import com.odomate.app.domain.StatusCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Everything a screen needs to know about one vehicle, kept live from the database. */
data class VehicleSnapshot(
    val vehicle: Vehicle,
    val readings: List<OdometerReading>,
    val types: List<ServiceType>,
    val records: List<ServiceRecord>,
    val fuel: List<FuelEntry>,
    val currentOdometer: Int?,
    val lastReading: OdometerReading?,
    val avgPerDay: Double?,
    val statuses: List<ServiceStatus>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CarRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    val vehicles: Flow<List<Vehicle>> = db.vehicles().observeAll()

    val activeVehicle: Flow<Vehicle?> = combine(vehicles, settings.settings.map { it.activeVehicleId }) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    val activeSnapshot: Flow<VehicleSnapshot?> = activeVehicle
        .map { it?.id }
        .flatMapLatest { id -> if (id == null) flowOf(null) else snapshot(id) }

    fun snapshot(vehicleId: Long): Flow<VehicleSnapshot?> = combine(
        vehicles.map { list -> list.firstOrNull { it.id == vehicleId } },
        db.readings().observeForVehicle(vehicleId),
        db.serviceTypes().observeForVehicle(vehicleId),
        db.records().observeForVehicle(vehicleId),
        db.fuel().observeForVehicle(vehicleId),
    ) { vehicle, readings, types, records, fuel ->
        vehicle?.let { buildSnapshot(it, readings, types, records, fuel) }
    }

    fun buildSnapshot(
        vehicle: Vehicle,
        readings: List<OdometerReading>,
        types: List<ServiceType>,
        records: List<ServiceRecord>,
        fuel: List<FuelEntry>,
        now: Long = System.currentTimeMillis(),
    ): VehicleSnapshot {
        val current = readings.maxOfOrNull { it.value }
        val avg = StatusCalculator.averagePerDay(readings, now)
        val statuses = StatusCalculator.sort(types.map { StatusCalculator.compute(it, current, avg, now) })
        return VehicleSnapshot(
            vehicle = vehicle,
            readings = readings,
            types = types,
            records = records,
            fuel = fuel,
            currentOdometer = current,
            lastReading = readings.maxWithOrNull(compareBy<OdometerReading> { it.date }.thenBy { it.value }),
            avgPerDay = avg,
            statuses = statuses,
        )
    }

    suspend fun snapshotOnce(vehicleId: Long): VehicleSnapshot? {
        val v = db.vehicles().get(vehicleId) ?: return null
        return buildSnapshot(
            v,
            db.readings().getForVehicle(vehicleId),
            db.serviceTypes().getForVehicle(vehicleId),
            db.records().getForVehicle(vehicleId),
            db.fuel().getAll().filter { it.vehicleId == vehicleId },
        )
    }

    // ---------------------------------------------------------------- vehicles

    suspend fun addVehicle(vehicle: Vehicle, initialOdometer: Int): Long {
        val miles = settings.current().miles
        val id = db.withTransaction {
            val newId = db.vehicles().insert(vehicle)
            db.readings().insert(
                OdometerReading(vehicleId = newId, value = initialOdometer, date = System.currentTimeMillis(), source = ReadingSource.INITIAL)
            )
            db.serviceTypes().insertAll(ServiceTemplates.forVehicle(newId, miles))
            newId
        }
        settings.setActiveVehicle(id)
        return id
    }

    suspend fun updateVehicle(vehicle: Vehicle) = db.vehicles().update(vehicle)

    suspend fun getVehicle(id: Long) = db.vehicles().get(id)

    suspend fun deleteVehicle(vehicle: Vehicle) {
        db.records().getForVehicle(vehicle.id).forEach { deleteReceiptFile(it.receiptPath) }
        db.vehicles().delete(vehicle)
        val remaining = db.vehicles().getAll()
        settings.setActiveVehicle(remaining.firstOrNull()?.id ?: 0L)
    }

    suspend fun setActiveVehicle(id: Long) = settings.setActiveVehicle(id)

    // ---------------------------------------------------------------- readings

    suspend fun addReading(vehicleId: Long, value: Int, date: Long, source: String): Long =
        db.readings().insert(OdometerReading(vehicleId = vehicleId, value = value, date = date, source = source))

    suspend fun deleteReading(reading: OdometerReading) = db.readings().delete(reading)

    suspend fun latestReading(vehicleId: Long): OdometerReading? =
        db.readings().getForVehicle(vehicleId).maxWithOrNull(compareBy<OdometerReading> { it.date }.thenBy { it.value })

    suspend fun highestReading(vehicleId: Long): Int? =
        db.readings().getForVehicle(vehicleId).maxOfOrNull { it.value }

    // ---------------------------------------------------------------- service types

    fun observeServiceType(id: Long): Flow<ServiceType?> = db.serviceTypes().observe(id)

    suspend fun getServiceType(id: Long) = db.serviceTypes().get(id)

    suspend fun serviceTypes(vehicleId: Long) = db.serviceTypes().getForVehicle(vehicleId)

    suspend fun saveServiceType(type: ServiceType): Long =
        if (type.id == 0L) {
            val order = db.serviceTypes().maxSortOrder(type.vehicleId) + 1
            db.serviceTypes().insert(type.copy(sortOrder = order))
        } else {
            db.serviceTypes().update(type)
            type.id
        }

    suspend fun updateServiceType(type: ServiceType) = db.serviceTypes().update(type)

    suspend fun deleteServiceType(type: ServiceType) = db.serviceTypes().delete(type)

    /** Sets the "last done" the user knows about before using the app, then recomputes the effective value. */
    suspend fun setBaseline(typeId: Long, odometer: Int?, date: Long?) {
        val type = db.serviceTypes().get(typeId) ?: return
        db.serviceTypes().update(type.copy(baselineOdometer = odometer, baselineDate = date))
        recomputeLastDone(type.vehicleId, listOf(typeId))
    }

    private suspend fun recomputeLastDone(vehicleId: Long, typeIds: Collection<Long>) {
        val records = db.records().getForVehicle(vehicleId)
        for (typeId in typeIds.toSet()) {
            val type = db.serviceTypes().get(typeId) ?: continue
            val latest = records
                .filter { typeId in it.serviceTypeIds }
                .maxWithOrNull(compareBy<ServiceRecord> { it.date }.thenBy { it.odometer })
            val updated = if (latest != null) {
                type.copy(lastDoneOdometer = latest.odometer, lastDoneDate = latest.date)
            } else {
                type.copy(lastDoneOdometer = type.baselineOdometer, lastDoneDate = type.baselineDate)
            }
            db.serviceTypes().update(updated.copy(notifiedLevel = 0, notifiedAt = 0))
        }
    }

    // ---------------------------------------------------------------- service records

    suspend fun getRecord(id: Long) = db.records().get(id)

    suspend fun saveRecord(record: ServiceRecord): Long {
        val previous = if (record.id != 0L) db.records().get(record.id) else null
        val id = db.withTransaction {
            val savedId = if (record.id == 0L) db.records().insert(record) else {
                db.records().update(record); record.id
            }
            if (previous == null) ensureReading(record.vehicleId, record.odometer, record.date, ReadingSource.SERVICE)
            savedId
        }
        if (previous?.receiptPath != null && previous.receiptPath != record.receiptPath) deleteReceiptFile(previous.receiptPath)
        recomputeLastDone(record.vehicleId, record.serviceTypeIds + (previous?.serviceTypeIds ?: emptyList()))
        return id
    }

    suspend fun deleteRecord(record: ServiceRecord) {
        db.records().delete(record)
        deleteReceiptFile(record.receiptPath)
        recomputeLastDone(record.vehicleId, record.serviceTypeIds)
    }

    /** Adds an odometer reading only if it is new information (higher than what we know). */
    private suspend fun ensureReading(vehicleId: Long, odometer: Int, date: Long, source: String) {
        val highest = highestReading(vehicleId)
        if (highest == null || odometer > highest) {
            db.readings().insert(OdometerReading(vehicleId = vehicleId, value = odometer, date = date, source = source))
        }
    }

    // ---------------------------------------------------------------- fuel

    suspend fun getFuel(id: Long) = db.fuel().get(id)

    suspend fun saveFuel(entry: FuelEntry): Long =
        if (entry.id == 0L) {
            val id = db.fuel().insert(entry)
            ensureReading(entry.vehicleId, entry.odometer, entry.date, ReadingSource.FUEL)
            id
        } else {
            db.fuel().update(entry); entry.id
        }

    suspend fun deleteFuel(entry: FuelEntry) = db.fuel().delete(entry)

    // ---------------------------------------------------------------- receipts

    suspend fun importReceipt(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val out = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            } ?: return@runCatching null
            out.absolutePath
        }.getOrNull()
    }

    fun deleteReceiptFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
