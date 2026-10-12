package app.gigometer.mileage.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import app.gigometer.mileage.Format
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.Category
import app.gigometer.mileage.data.IrsRates
import app.gigometer.mileage.data.Trip
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** IRS-style mileage log as PDF / CSV, plus JSON backup & restore. Everything is generated on the phone. */
object Exporter {

    data class RateLine(val start: LocalDate, val end: LocalDate, val rate: Double, val miles: Double) {
        val amount: Double get() = miles * rate
    }

    /** Business miles split by the IRS rate period they fall in. */
    fun rateBreakdown(trips: List<Trip>, settings: AppSettings, year: Int): List<RateLine> {
        val periods = settings.rates
        val yearStart = LocalDate.of(year, 1, 1)
        val yearEnd = LocalDate.of(year, 12, 31)
        val inYear = periods.filter { it.start.year <= year }
        val lines = mutableListOf<RateLine>()
        val starts = (listOf(yearStart) + inYear.map { it.start }.filter { it.isAfter(yearStart) && it.year == year }).distinct().sorted()
        for ((i, s) in starts.withIndex()) {
            val e = if (i + 1 < starts.size) starts[i + 1].minusDays(1) else yearEnd
            val rate = IrsRates.rateOn(s, periods)
            val miles = trips.filter { it.isBusiness }.filter {
                val d = Format.localDate(it.startTime); !d.isBefore(s) && !d.isAfter(e)
            }.sumOf { it.miles }
            lines += RateLine(s, e, rate, miles)
        }
        return lines
    }

    private fun exportDir(ctx: Context) = File(ctx.cacheDir, "exports").apply { mkdirs() }

    private fun csvEscape(s: String?): String {
        val v = s ?: ""
        return if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }

    fun writeCsv(ctx: Context, trips: List<Trip>, settings: AppSettings, year: Int): File {
        val f = File(exportDir(ctx), "Gigometer-mileage-$year.csv")
        f.bufferedWriter().use { w ->
            w.appendLine("Date,Start time,End time,From,To,Category,Platform,Business purpose,Miles,Rate per mile,Deduction")
            trips.forEach { t ->
                val rate = IrsRates.rateFor(t, settings.rates)
                w.appendLine(
                    listOf(
                        Format.localDate(t.startTime).toString(),
                        Format.time(t.startTime),
                        t.endTime?.let(Format::time) ?: "",
                        csvEscape(t.startAddress),
                        csvEscape(t.endAddress),
                        when (t.category) { Category.BUSINESS -> "Business"; Category.PERSONAL -> "Personal"; else -> "Unreviewed" },
                        csvEscape(t.platform),
                        csvEscape(purposeOf(t)),
                        Format.milesPrecise(t.miles),
                        if (t.isBusiness) String.format(java.util.Locale.US, "%.3f", rate) else "",
                        if (t.isBusiness) String.format(java.util.Locale.US, "%.2f", t.miles * rate) else "",
                    ).joinToString(",")
                )
            }
        }
        return f
    }

    private fun purposeOf(t: Trip): String = when {
        t.purpose.isNotBlank() -> t.purpose
        t.isBusiness && t.platform != null -> "Gig deliveries/rides — ${t.platform}"
        t.isBusiness -> "Gig work"
        else -> ""
    }

    fun writePdf(ctx: Context, trips: List<Trip>, settings: AppSettings, year: Int): File {
        val f = File(exportDir(ctx), "Gigometer-mileage-log-$year.pdf")
        val doc = PdfDocument()
        val pageW = 612; val pageH = 792; val margin = 40f
        val ink = Color.rgb(20, 33, 27); val muted = Color.rgb(85, 97, 90); val green = Color.rgb(27, 107, 69)
        val title = Paint().apply { color = ink; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val h2 = Paint().apply { color = ink; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val body = Paint().apply { color = ink; textSize = 9f; isAntiAlias = true }
        val small = Paint().apply { color = muted; textSize = 8f; isAntiAlias = true }
        val headCell = Paint().apply { color = Color.WHITE; textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val fill = Paint().apply { style = Paint.Style.FILL }

        val business = trips.filter { it.isBusiness }
        val lines = rateBreakdown(trips, settings, year)
        val totalMiles = trips.sumOf { it.miles }
        val bizMiles = business.sumOf { it.miles }
        val deduction = lines.sumOf { it.amount }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        // Column x positions: date, from → to, purpose, miles, deduction
        val cols = floatArrayOf(margin, margin + 62, margin + 300, margin + 440, margin + 492)

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            y = margin
            page!!.canvas.drawText("Gigometer · $year mileage log · page $pageNo", margin, pageH - 20f, small)
        }

        fun tableHeader() {
            val c = page!!.canvas
            fill.color = ink
            c.drawRect(margin, y, pageW - margin, y + 18, fill)
            listOf("Date", "From → To", "Business purpose", "Miles", "Deduction").forEachIndexed { i, s ->
                c.drawText(s, cols[i] + 4, y + 12.5f, headCell)
            }
            y += 22
        }

        fun clip(s: String, paint: Paint, width: Float): String {
            if (paint.measureText(s) <= width) return s
            var t = s
            while (t.isNotEmpty() && paint.measureText("$t…") > width) t = t.dropLast(1)
            return "$t…"
        }

        newPage()
        var c = page!!.canvas
        c.drawText("Vehicle Mileage Log — Tax Year $year", margin, y + 18, title); y += 32
        if (settings.driverName.isNotBlank()) { c.drawText("Driver: ${settings.driverName}", margin, y, body); y += 14 }
        c.drawText("Generated ${Format.date(LocalDate.now())} from GPS-recorded trips (contemporaneous log).", margin, y, small); y += 22

        c.drawText("Summary", margin, y, h2); y += 16
        val summary = listOf(
            "Total miles driven (all trips logged)" to Format.milesPrecise(totalMiles),
            "Business miles" to Format.milesPrecise(bizMiles),
            "Business use" to if (totalMiles > 0) "${(bizMiles / totalMiles * 100).toInt()}%" else "—",
            "Business trips" to business.size.toString(),
        )
        summary.forEach { (k, v) -> c.drawText(k, margin, y, body); c.drawText(v, margin + 260, y, body); y += 13 }
        y += 6
        lines.filter { it.miles > 0 }.forEach {
            c.drawText("${Format.date(it.start)} – ${Format.date(it.end)}: ${Format.milesPrecise(it.miles)} mi × ${Format.rate(it.rate)}", margin, y, body)
            c.drawText(Format.money(it.amount), margin + 380, y, body); y += 13
        }
        fill.color = green
        c.drawRect(margin, y, pageW - margin, y + 26, fill)
        val white = Paint(h2).apply { color = Color.WHITE }
        c.drawText("Standard mileage deduction (Schedule C, line 9)", margin + 8, y + 17, white)
        c.drawText(Format.money(deduction), pageW - margin - 8 - white.measureText(Format.money(deduction)), y + 17, white)
        y += 40
        c.drawText("Business trips", margin, y, h2); y += 10
        tableHeader()

        val zebra = Paint().apply { color = Color.rgb(244, 245, 240) }
        business.forEachIndexed { i, t ->
            if (y > pageH - 50) { newPage(); tableHeader() }
            c = page!!.canvas
            if (i % 2 == 0) c.drawRect(margin, y - 10, pageW - margin, y + 5, zebra)
            val route = "${t.startAddress ?: "—"} → ${t.endAddress ?: "—"}"
            c.drawText(Format.localDate(t.startTime).toString(), cols[0] + 4, y, body)
            c.drawText(clip(route, body, cols[2] - cols[1] - 8), cols[1] + 4, y, body)
            c.drawText(clip(purposeOf(t), body, cols[3] - cols[2] - 8), cols[2] + 4, y, body)
            c.drawText(Format.milesPrecise(t.miles), cols[3] + 4, y, body)
            c.drawText(Format.money(t.miles * IrsRates.rateFor(t, settings.rates)), cols[4] + 4, y, body)
            y += 15
        }
        if (business.isEmpty()) { page!!.canvas.drawText("No business trips recorded for $year.", margin, y + 4, body) }
        page?.let { doc.finishPage(it) }
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        return f
    }

    fun share(ctx: Context, file: File, mime: String) {
        val uri: Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---- Backup / restore ----

    fun backupJson(trips: List<Trip>): String {
        val arr = JSONArray()
        trips.forEach { t ->
            arr.put(JSONObject().apply {
                put("startTime", t.startTime); put("endTime", t.endTime ?: JSONObject.NULL)
                put("distanceMeters", t.distanceMeters)
                put("startLat", t.startLat ?: JSONObject.NULL); put("startLng", t.startLng ?: JSONObject.NULL)
                put("endLat", t.endLat ?: JSONObject.NULL); put("endLng", t.endLng ?: JSONObject.NULL)
                put("startAddress", t.startAddress ?: JSONObject.NULL); put("endAddress", t.endAddress ?: JSONObject.NULL)
                put("category", t.category); put("platform", t.platform ?: JSONObject.NULL)
                put("purpose", t.purpose); put("route", t.route)
                put("isManual", t.isManual); put("autoDetected", t.autoDetected)
            })
        }
        return JSONObject().put("app", "gigometer").put("version", 1).put("trips", arr).toString()
    }

    fun parseBackup(json: String): List<Trip> {
        val root = JSONObject(json)
        require(root.optString("app") == "gigometer") { "Not a Gigometer backup file" }
        val arr = root.getJSONArray("trips")
        fun JSONObject.dbl(k: String) = if (isNull(k)) null else getDouble(k)
        fun JSONObject.str(k: String) = if (isNull(k)) null else getString(k)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Trip(
                startTime = o.getLong("startTime"),
                endTime = if (o.isNull("endTime")) o.getLong("startTime") else o.getLong("endTime"),
                distanceMeters = o.getDouble("distanceMeters"),
                startLat = o.dbl("startLat"), startLng = o.dbl("startLng"),
                endLat = o.dbl("endLat"), endLng = o.dbl("endLng"),
                startAddress = o.str("startAddress"), endAddress = o.str("endAddress"),
                category = o.optString("category", Category.UNREVIEWED),
                platform = o.str("platform"),
                purpose = o.optString("purpose", ""),
                route = o.optString("route", ""),
                isManual = o.optBoolean("isManual"),
                autoDetected = o.optBoolean("autoDetected"),
            )
        }
    }
}
