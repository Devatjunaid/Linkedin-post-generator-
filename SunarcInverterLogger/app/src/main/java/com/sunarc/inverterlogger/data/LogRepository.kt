package com.sunarc.inverterlogger.data

import com.sunarc.inverterlogger.data.db.AppDatabase
import com.sunarc.inverterlogger.data.db.InverterEntity
import com.sunarc.inverterlogger.data.db.ReadingEntity
import com.sunarc.inverterlogger.domain.InverterBrand
import com.sunarc.inverterlogger.domain.InverterBrands
import com.sunarc.inverterlogger.domain.Reading
import com.sunarc.inverterlogger.domain.ReadingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

data class Inverter(
    val id: Long,
    val name: String,
    val brandId: String,
    val model: String,
    val host: String?,
    val port: Int,
    val unitId: Int
) {
    val brand: InverterBrand get() = InverterBrands.byId(brandId)
}

class LogRepository(private val db: AppDatabase) {

    val readings: Flow<List<Reading>> = db.readings().observeAll().map { list -> list.map { it.toDomain() } }

    val inverters: Flow<List<Inverter>> = db.inverters().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun allReadings(): List<Reading> = db.readings().all().map { it.toDomain() }

    suspend fun allInverters(): List<Inverter> = db.inverters().all().map { it.toDomain() }

    suspend fun latestFor(inverterId: Long): Reading? = db.readings().latestFor(inverterId)?.toDomain()

    suspend fun save(reading: Reading): Long =
        if (reading.id == 0L) db.readings().insert(reading.toEntity()) else {
            db.readings().update(reading.toEntity()); reading.id
        }

    suspend fun delete(readingId: Long) = db.readings().delete(readingId)

    suspend fun addInverter(name: String, brandId: String, model: String): Long =
        db.inverters().insert(InverterEntity(name = name, brandId = brandId, model = model, host = null, port = 502,
            unitId = InverterBrands.byId(brandId).modbusUnitId, createdAt = System.currentTimeMillis()))

    suspend fun updateInverter(inverter: Inverter) {
        val existing = db.inverters().all().firstOrNull { it.id == inverter.id } ?: return
        db.inverters().update(existing.copy(name = inverter.name, brandId = inverter.brandId, model = inverter.model,
            host = inverter.host, port = inverter.port, unitId = inverter.unitId))
    }

    suspend fun deleteInverter(id: Long) = db.inverters().delete(id)

    suspend fun replaceAll(inverters: List<InverterEntity>, readings: List<ReadingEntity>) {
        db.readings().clear()
        db.inverters().clear()
        db.inverters().insertAll(inverters)
        db.readings().insertAll(readings)
    }

    suspend fun rawInverters() = db.inverters().all()
    suspend fun rawReadings() = db.readings().all()
}

private fun ReadingEntity.toDomain() = Reading(
    id = id, inverterId = inverterId, timestamp = Instant.ofEpochMilli(timestamp), todayKwh = todayKwh,
    totalKwh = totalKwh, powerW = powerW,
    source = ReadingSource.entries.firstOrNull { it.name == source } ?: ReadingSource.MANUAL,
    faultCode = faultCode, note = note, confidence = confidence, photoPath = photoPath
)

private fun Reading.toEntity() = ReadingEntity(
    id = id, inverterId = inverterId, timestamp = timestamp.toEpochMilli(), todayKwh = todayKwh, totalKwh = totalKwh,
    powerW = powerW, source = source.name, faultCode = faultCode, note = note, confidence = confidence, photoPath = photoPath
)

private fun InverterEntity.toDomain() = Inverter(id, name, brandId, model, host, port, unitId)
