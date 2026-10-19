package com.sunarc.inverterlogger.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.sunarc.inverterlogger.data.db.InverterEntity
import com.sunarc.inverterlogger.data.db.ReadingEntity
import com.sunarc.inverterlogger.domain.Aggregation
import com.sunarc.inverterlogger.domain.CsvExport
import com.sunarc.inverterlogger.domain.Economics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** CSV, PDF report and full backup files. Everything is created on the device; nothing is uploaded. */
class ExportManager(
    private val context: Context,
    private val log: LogRepository,
    private val settings: SettingsRepository
) {
    private fun exportDir() = File(context.cacheDir, "exports").apply { mkdirs() }

    private fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun shareIntent(uri: Uri, mime: String, title: String): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            title
        )

    suspend fun csvFile(): Uri = withContext(Dispatchers.IO) {
        val inverters = log.allInverters().associateBy { it.id }
        val profile = settings.current()
        val text = CsvExport.readings(log.allReadings(), { id -> inverters[id]?.name ?: "Inverter $id" }, profile.zone)
        val file = File(exportDir(), "sunarc-readings-${LocalDate.now()}.csv")
        file.writeText(text)
        uriFor(file)
    }

    suspend fun dailyCsvFile(): Uri = withContext(Dispatchers.IO) {
        val profile = settings.current()
        val days = Aggregation.dailyYields(log.allReadings(), profile.zone)
        val file = File(exportDir(), "sunarc-daily-${LocalDate.now()}.csv")
        file.writeText(CsvExport.dailyYields(days))
        uriFor(file)
    }

    /** A one-page monthly report: totals, a daily bar chart and money saved. */
    suspend fun monthlyReportPdf(month: YearMonth = YearMonth.now()): Uri = withContext(Dispatchers.IO) {
        val profile = settings.current()
        val days = Aggregation.month(Aggregation.dailyYields(log.allReadings(), profile.zone), month)
        val stats = Aggregation.stats(days, profile.kwp.takeIf { it > 0 })
        val money = Economics.value(stats.totalKwh, profile.tariff)

        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 24, 31); textSize = 12f }
        val muted = Paint(ink).apply { color = Color.rgb(96, 106, 120); textSize = 11f }
        val title = Paint(ink).apply { textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 176, 32) }

        c.drawCircle(64f, 70f, 14f, sun)
        c.drawText("Sunarc solar report", 90f, 78f, title)
        c.drawText("${profile.systemName} · ${month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}", 90f, 98f, muted)

        var y = 150f
        fun row(label: String, value: String) {
            c.drawText(label, 48f, y, muted)
            c.drawText(value, 260f, y, ink)
            y += 22f
        }
        row("Energy produced", "%.1f kWh".format(stats.totalKwh))
        row("Days logged", "${stats.daysWithData} of ${month.lengthOfMonth()}")
        row("Daily average", "%.1f kWh".format(stats.averageKwh))
        stats.bestDay?.let { row("Best day", "%.1f kWh on %s".format(it.kwh, it.date)) }
        stats.specificYield?.let { row("Specific yield", "%.2f kWh/kWp".format(it)) }
        if (profile.pricesSet) row("Value of energy", "%s%.2f".format(profile.currency, money.total))
        row("CO₂ avoided (approx.)", "%.1f kg".format(Economics.co2Kg(stats.totalKwh, profile.effectiveCo2)))

        // Daily bars.
        val chart = RectF(48f, y + 30f, 547f, y + 250f)
        val axis = Paint(muted).apply { strokeWidth = 1f }
        c.drawLine(chart.left, chart.bottom, chart.right, chart.bottom, axis)
        val max = (days.maxOfOrNull { it.kwh } ?: 0.0).coerceAtLeast(1.0)
        val slot = chart.width() / days.size
        days.forEachIndexed { i, d ->
            val h = (d.kwh / max * chart.height()).toFloat()
            val bar = Paint(sun).apply { alpha = if (d.estimated) 120 else 255 }
            c.drawRoundRect(RectF(chart.left + i * slot + 2, chart.bottom - h, chart.left + (i + 1) * slot - 2, chart.bottom), 3f, 3f, bar)
            if ((i + 1) % 5 == 0 || i == 0) c.drawText("${d.date.dayOfMonth}", chart.left + i * slot, chart.bottom + 14f, muted)
        }
        c.drawText("Lighter bars are estimated from the lifetime total on days without a scan.", 48f, chart.bottom + 40f, muted)
        c.drawText("Created with Sunarc Inverter Logger on ${LocalDate.now()}.", 48f, 800f, muted)
        doc.finishPage(page)

        val file = File(exportDir(), "sunarc-report-$month.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        uriFor(file)
    }

    /** Writes a complete JSON backup to a file the user picked with the system file picker. */
    suspend fun writeBackup(target: Uri) = withContext(Dispatchers.IO) {
        val p = settings.current()
        val json = JSONObject()
        json.put("format", "sunarc-backup")
        json.put("version", 1)
        json.put("profile", JSONObject().apply {
            put("country", p.countryCode); put("name", p.systemName); put("kwp", p.kwp); put("tilt", p.tiltDeg)
            put("azimuth", p.azimuthDeg); p.latitude?.let { put("lat", it) }; p.longitude?.let { put("lon", it) }
            p.tariffModel?.let { put("tariff", it.name) }; put("buy", p.buyPrice); put("export", p.exportPrice)
            put("selfUse", p.selfUseShare); p.co2Grams?.let { put("co2", it) }; p.systemCost?.let { put("cost", it) }
            p.installEpochDay?.let { put("install", it) }
        })
        json.put("inverters", JSONArray().apply {
            log.rawInverters().forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id); put("name", i.name); put("brand", i.brandId); put("model", i.model)
                    i.host?.let { put("host", it) }; put("port", i.port); put("unit", i.unitId); put("created", i.createdAt)
                })
            }
        })
        json.put("readings", JSONArray().apply {
            log.rawReadings().forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("inverter", r.inverterId); put("t", r.timestamp)
                    r.todayKwh?.let { put("today", it) }; r.totalKwh?.let { put("total", it) }; r.powerW?.let { put("power", it) }
                    put("source", r.source); r.faultCode?.let { put("fault", it) }; r.note?.let { put("note", it) }
                    r.confidence?.let { put("conf", it) }
                })
            }
        })
        context.contentResolver.openOutputStream(target, "wt")?.use { it.write(json.toString(2).toByteArray()) }
            ?: error("Could not open the backup file")
    }

    /** Restores a backup made by [writeBackup]. Replaces the current log. Returns the number of readings. */
    suspend fun restoreBackup(source: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(source)?.use { it.readBytes().decodeToString() }
            ?: error("Could not open the file")
        val json = JSONObject(text)
        require(json.optString("format") == "sunarc-backup") { "This is not a Sunarc backup file." }
        val p = json.getJSONObject("profile")
        settings.update { old ->
            old.copy(
                onboarded = true,
                countryCode = p.optString("country", old.countryCode),
                systemName = p.optString("name", old.systemName),
                kwp = p.optDouble("kwp", old.kwp),
                tiltDeg = p.optDouble("tilt", old.tiltDeg),
                azimuthDeg = p.optDouble("azimuth", old.azimuthDeg),
                latitude = if (p.has("lat")) p.getDouble("lat") else null,
                longitude = if (p.has("lon")) p.getDouble("lon") else null,
                tariffModel = if (p.has("tariff")) com.sunarc.inverterlogger.domain.TariffModel.entries.firstOrNull { it.name == p.getString("tariff") } else null,
                buyPrice = p.optDouble("buy", 0.0),
                exportPrice = p.optDouble("export", 0.0),
                selfUseShare = p.optDouble("selfUse", 0.35),
                co2Grams = if (p.has("co2")) p.getInt("co2") else null,
                systemCost = if (p.has("cost")) p.getDouble("cost") else null,
                installEpochDay = if (p.has("install")) p.getLong("install") else null
            )
        }
        val inv = json.getJSONArray("inverters")
        val inverters = (0 until inv.length()).map { i ->
            val o = inv.getJSONObject(i)
            InverterEntity(o.getLong("id"), o.getString("name"), o.getString("brand"), o.optString("model"),
                if (o.has("host")) o.getString("host") else null, o.optInt("port", 502), o.optInt("unit", 1), o.optLong("created"))
        }
        val rd = json.getJSONArray("readings")
        val readings = (0 until rd.length()).map { i ->
            val o = rd.getJSONObject(i)
            fun d(k: String) = if (o.has(k)) o.getDouble(k) else null
            ReadingEntity(o.getLong("id"), o.getLong("inverter"), o.getLong("t"), d("today"), d("total"), d("power"),
                o.optString("source", "MANUAL"), if (o.has("fault")) o.getString("fault") else null,
                if (o.has("note")) o.getString("note") else null, d("conf"), null)
        }
        log.replaceAll(inverters, readings)
        readings.size
    }
}
