package com.odomate.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.odomate.app.data.db.AppDatabase
import com.odomate.app.data.db.FuelEntry
import com.odomate.app.data.db.OdometerReading
import com.odomate.app.data.db.ServiceRecord
import com.odomate.app.data.db.ServiceType
import com.odomate.app.data.db.Vehicle
import com.odomate.app.util.Formats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** JSON backup / restore of the whole garage, plus CSV export of one vehicle's history. */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    suspend fun exportJson(target: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("format", "odomate-backup")
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("vehicles", JSONArray(db.vehicles().getAll().map { it.toJson() }))
            root.put("readings", JSONArray(db.readings().getAll().map { it.toJson() }))
            root.put("serviceTypes", JSONArray(db.serviceTypes().getAll().map { it.toJson() }))
            root.put("records", JSONArray(db.records().getAll().map { it.toJson() }))
            root.put("fuel", JSONArray(db.fuel().getAll().map { it.toJson() }))
            val s = settings.current()
            root.put("settings", JSONObject().apply {
                put("distanceUnit", s.distanceUnit)
                put("volumeUnit", s.volumeUnit)
                put("currency", s.currency)
            })
            context.contentResolver.openOutputStream(target, "wt")?.use { it.write(root.toString(2).toByteArray()) }
                ?: error("Could not open file")
            Unit
        }
    }

    /** Replaces all data with the backup's contents. Receipt images are not part of the backup. */
    suspend fun importJson(source: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(source)?.use { it.readBytes().decodeToString() }
                ?: error("Could not open file")
            val root = JSONObject(text)
            require(root.optString("format") == "odomate-backup") { "This file is not an OdoMate backup" }
            val vehicles = root.getJSONArray("vehicles").objects().map { it.toVehicle() }
            val readings = root.getJSONArray("readings").objects().map { it.toReading() }
            val types = root.getJSONArray("serviceTypes").objects().map { it.toServiceType() }
            val records = root.getJSONArray("records").objects().map { it.toRecord() }
            val fuel = root.optJSONArray("fuel")?.objects()?.map { it.toFuel() } ?: emptyList()
            db.withTransaction {
                db.vehicles().deleteAll()
                vehicles.forEach { db.vehicles().insert(it) }
                readings.forEach { db.readings().insert(it) }
                types.forEach { db.serviceTypes().insert(it) }
                records.forEach { db.records().insert(it) }
                fuel.forEach { db.fuel().insert(it) }
            }
            root.optJSONObject("settings")?.let { s ->
                s.optString("distanceUnit").takeIf { it.isNotBlank() }?.let { settings.setDistanceUnit(it) }
                s.optString("volumeUnit").takeIf { it.isNotBlank() }?.let { settings.setVolumeUnit(it) }
                s.optString("currency").takeIf { it.isNotBlank() }?.let { settings.setCurrency(it) }
            }
            vehicles.firstOrNull()?.let { settings.setActiveVehicle(it.id) }
            vehicles.size
        }
    }

    /** Writes a CSV of all history for one vehicle into the cache and returns a shareable content Uri. */
    suspend fun exportCsv(vehicleId: Long): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settings.current()
            val vehicle = db.vehicles().get(vehicleId) ?: error("Vehicle not found")
            val rows = mutableListOf<List<String>>()
            rows += listOf("Date", "Type", "Odometer (${s.distanceUnit})", "Details", "Cost", "Shop / station", "Notes")
            db.records().getForVehicle(vehicleId).forEach {
                rows += listOf(Formats.isoDate(it.date), "Service", it.odometer.toString(), it.serviceNames,
                    it.cost?.let { c -> "%.2f".format(java.util.Locale.US, c) } ?: "", it.shop, it.notes)
            }
            db.fuel().getAll().filter { it.vehicleId == vehicleId }.forEach {
                rows += listOf(Formats.isoDate(it.date), "Fuel", it.odometer.toString(),
                    "%.2f %s%s".format(java.util.Locale.US, it.volume, s.volumeUnit, if (it.fullTank) " (full)" else ""),
                    it.cost?.let { c -> "%.2f".format(java.util.Locale.US, c) } ?: "", it.station, it.notes)
            }
            db.readings().getForVehicle(vehicleId).forEach {
                rows += listOf(Formats.isoDate(it.date), "Odometer reading", it.value.toString(), it.source, "", "", "")
            }
            val header = rows.first()
            val body = rows.drop(1).sortedByDescending { it[0] }
            val csv = (listOf(header) + body).joinToString("\n") { row -> row.joinToString(",") { csvCell(it) } }
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = vehicle.name.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "vehicle" }
            val file = File(dir, "OdoMate_${safeName}_history.csv")
            file.writeText(csv)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    private fun csvCell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    // ------------------------------------------------------------------ JSON mapping

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
    private fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }

    private fun Vehicle.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("make", make); put("model", model)
        putNullable("year", year); put("plate", plate); put("createdAt", createdAt)
    }

    private fun JSONObject.toVehicle() = Vehicle(
        id = getLong("id"), name = getString("name"), make = optString("make"), model = optString("model"),
        year = optIntOrNull("year"), plate = optString("plate"), createdAt = optLong("createdAt", System.currentTimeMillis()),
    )

    private fun OdometerReading.toJson() = JSONObject().apply {
        put("id", id); put("vehicleId", vehicleId); put("value", value); put("date", date); put("source", source)
    }

    private fun JSONObject.toReading() = OdometerReading(
        id = getLong("id"), vehicleId = getLong("vehicleId"), value = getInt("value"), date = getLong("date"),
        source = optString("source", "manual"),
    )

    private fun ServiceType.toJson() = JSONObject().apply {
        put("id", id); put("vehicleId", vehicleId); put("name", name); put("iconKey", iconKey); put("category", category)
        putNullable("intervalDistance", intervalDistance); putNullable("intervalMonths", intervalMonths)
        put("remindDistanceBefore", remindDistanceBefore); put("remindDaysBefore", remindDaysBefore)
        putNullable("baselineOdometer", baselineOdometer); putNullable("baselineDate", baselineDate)
        putNullable("lastDoneOdometer", lastDoneOdometer); putNullable("lastDoneDate", lastDoneDate)
        put("enabled", enabled); put("isCustom", isCustom); put("sortOrder", sortOrder); put("notes", notes)
    }

    private fun JSONObject.toServiceType() = ServiceType(
        id = getLong("id"), vehicleId = getLong("vehicleId"), name = getString("name"),
        iconKey = optString("iconKey", "wrench"), category = optString("category", Category.CUSTOM),
        intervalDistance = optIntOrNull("intervalDistance"), intervalMonths = optIntOrNull("intervalMonths"),
        remindDistanceBefore = optInt("remindDistanceBefore", 500), remindDaysBefore = optInt("remindDaysBefore", 7),
        baselineOdometer = optIntOrNull("baselineOdometer"), baselineDate = optLongOrNull("baselineDate"),
        lastDoneOdometer = optIntOrNull("lastDoneOdometer"), lastDoneDate = optLongOrNull("lastDoneDate"),
        enabled = optBoolean("enabled", true), isCustom = optBoolean("isCustom", false),
        sortOrder = optInt("sortOrder", 0), notes = optString("notes"),
    )

    private fun ServiceRecord.toJson() = JSONObject().apply {
        put("id", id); put("vehicleId", vehicleId); put("date", date); put("odometer", odometer)
        putNullable("cost", cost); put("shop", shop); put("notes", notes)
        put("serviceTypeIds", JSONArray(serviceTypeIds)); put("serviceNames", serviceNames)
    }

    private fun JSONObject.toRecord(): ServiceRecord {
        val ids = optJSONArray("serviceTypeIds")
        return ServiceRecord(
            id = getLong("id"), vehicleId = getLong("vehicleId"), date = getLong("date"), odometer = getInt("odometer"),
            cost = optDoubleOrNull("cost"), shop = optString("shop"), notes = optString("notes"), receiptPath = null,
            serviceTypeIds = if (ids == null) emptyList() else (0 until ids.length()).map { ids.getLong(it) },
            serviceNames = optString("serviceNames"),
        )
    }

    private fun FuelEntry.toJson() = JSONObject().apply {
        put("id", id); put("vehicleId", vehicleId); put("date", date); put("odometer", odometer); put("volume", volume)
        putNullable("cost", cost); put("fullTank", fullTank); put("station", station); put("notes", notes)
    }

    private fun JSONObject.toFuel() = FuelEntry(
        id = getLong("id"), vehicleId = getLong("vehicleId"), date = getLong("date"), odometer = getInt("odometer"),
        volume = getDouble("volume"), cost = optDoubleOrNull("cost"), fullTank = optBoolean("fullTank", true),
        station = optString("station"), notes = optString("notes"),
    )
}
