package com.snaptensio.bplog.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.snaptensio.bplog.data.Arm
import com.snaptensio.bplog.data.Guideline
import com.snaptensio.bplog.data.Medication
import com.snaptensio.bplog.data.Position
import com.snaptensio.bplog.data.Profile
import com.snaptensio.bplog.data.Reading
import com.snaptensio.bplog.data.Source
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.domain.average
import com.snaptensio.bplog.domain.inTarget
import com.snaptensio.bplog.domain.isEvening
import com.snaptensio.bplog.domain.isMorning
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ReportOptions(
    val includeChart: Boolean = true,
    val includeTable: Boolean = true,
    val includeMeds: Boolean = true,
    val includeNotes: Boolean = false,
)

object Exporters {
    private fun exportDir(context: Context) = File(context.cacheDir, "exports").apply { mkdirs() }

    fun share(context: Context, file: File, mime: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---------------------------------------------------------------- CSV

    private val csvDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val csvTime = SimpleDateFormat("HH:mm", Locale.US)

    private fun esc(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun writeCsv(context: Context, profile: Profile, readings: List<Reading>): File {
        val file = File(exportDir(context), "snaptensio-${profile.name.filter { it.isLetterOrDigit() }.ifEmpty { "readings" }}.csv")
        file.bufferedWriter().use { w ->
            w.write("date,time,systolic,diastolic,pulse,arm,position,tags,note,source,averaged_count\n")
            for (r in readings.sortedBy { it.timestamp }) {
                val d = Date(r.timestamp)
                w.write(
                    listOf(
                        csvDate.format(d), csvTime.format(d), r.systolic.toString(), r.diastolic.toString(),
                        r.pulse?.toString() ?: "", r.arm, r.position, esc(r.tags.replace(",", ";")), esc(r.note),
                        r.source, r.averagedCount.toString(),
                    ).joinToString(","),
                )
                w.write("\n")
            }
        }
        return file
    }

    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { out += sb.toString(); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out.map { it.trim() }
    }

    /**
     * Imports readings from SnapTensio's own CSV or from most other apps' exports: the header
     * just needs date and systolic/diastolic columns (common names are recognised).
     * Returns the parsed readings and the number of rows that were skipped.
     */
    fun parseCsv(input: InputStream, profileId: Long): Pair<List<Reading>, Int> {
        val lines = input.bufferedReader().readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList<Reading>() to 0
        val header = splitCsv(lines.first()).map { it.lowercase(Locale.US).replace(Regex("[^a-z]"), "") }
        fun col(vararg names: String) = header.indexOfFirst { h -> names.any { h == it || h.startsWith(it) } }
        val iDate = col("date", "datetime", "timestamp", "time")
        val iTime = header.indexOfFirst { it == "time" }.takeIf { it != iDate } ?: -1
        val iSys = col("systolic", "sys", "upper")
        val iDia = col("diastolic", "dia", "lower")
        val iPulse = col("pulse", "heartrate", "hr", "bpm")
        val iArm = col("arm")
        val iPos = col("position", "posture")
        val iTags = col("tags", "tag")
        val iNote = col("note", "notes", "comment")
        if (iDate < 0 || iSys < 0 || iDia < 0) return emptyList<Reading>() to lines.size - 1

        val formats = listOf(
            "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy HH:mm", "MM/dd/yyyy HH:mm", "dd.MM.yyyy HH:mm", "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy",
        ).map { SimpleDateFormat(it, Locale.US).apply { isLenient = false } }

        var skipped = 0
        val out = mutableListOf<Reading>()
        for (line in lines.drop(1)) {
            val c = splitCsv(line)
            fun get(i: Int) = if (i in c.indices) c[i] else ""
            val stamp = (get(iDate) + if (iTime >= 0) " " + get(iTime) else "").trim()
            val ts = formats.firstNotNullOfOrNull { f -> runCatching { f.parse(stamp)?.time }.getOrNull() }
            val sys = get(iSys).toDoubleOrNull()?.roundToInt()
            val dia = get(iDia).toDoubleOrNull()?.roundToInt()
            val pulse = get(iPulse).toDoubleOrNull()?.roundToInt()
            if (ts == null || BpClassifier.validate(sys, dia, pulse) != null) {
                skipped++
                continue
            }
            out += Reading(
                profileId = profileId,
                timestamp = ts,
                systolic = sys!!,
                diastolic = dia!!,
                pulse = pulse,
                arm = get(iArm).uppercase(Locale.US).takeIf { a -> Arm.entries.any { it.name == a } } ?: Arm.LEFT.name,
                position = get(iPos).uppercase(Locale.US).takeIf { p -> Position.entries.any { it.name == p } } ?: Position.SITTING.name,
                tags = get(iTags).replace(";", ","),
                note = get(iNote),
                source = Source.IMPORT.name,
            )
        }
        return out to skipped
    }

    // ---------------------------------------------------------------- PDF

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val M = 40f

    fun writePdf(
        context: Context,
        profile: Profile,
        readings: List<Reading>,
        meds: List<Medication>,
        guideline: Guideline,
        rangeLabel: String,
        options: ReportOptions,
    ): File {
        val sorted = readings.sortedBy { it.timestamp }
        val doc = PdfDocument()
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 27, 45); textSize = 10f }
        val muted = Paint(ink).apply { color = Color.rgb(85, 98, 116) }
        val bold = Paint(ink).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val title = Paint(bold).apply { textSize = 20f }
        val h2 = Paint(bold).apply { textSize = 13f }
        val brand = Paint(bold).apply { color = Color.rgb(31, 79, 216); textSize = 11f }
        val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(244, 246, 248) }
        val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val rowFmt = SimpleDateFormat("EEE d MMM  HH:mm", Locale.getDefault())

        var pageNo = 0
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNo).create())
        var c: Canvas = page.canvas
        var y = 0f

        fun footer() {
            c.drawText("SnapTensio · BP Camera Log — a personal log, not a medical device.", M, PAGE_H - 24f, muted)
            c.drawText("Page $pageNo", PAGE_W - M - 40f, PAGE_H - 24f, muted)
        }

        fun newPage() {
            footer()
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNo).create())
            c = page.canvas
            y = M + 10f
        }

        // Header
        c.drawText("SNAPTENSIO", M, M + 4f, brand)
        c.drawText("Blood pressure report", M, M + 30f, title)
        val span = if (sorted.isEmpty()) rangeLabel else "${dateFmt.format(Date(sorted.first().timestamp))} – ${dateFmt.format(Date(sorted.last().timestamp))}"
        c.drawText("${profile.name} · Target ${profile.targetSystolic}/${profile.targetDiastolic} · $span · ${guideline.label}", M, M + 48f, muted)
        y = M + 70f

        // Summary tiles
        val avg = sorted.average()
        val tiles = listOf(
            "Average" to (avg?.toString() ?: "—"),
            "Readings" to sorted.size.toString(),
            "In target" to if (sorted.isEmpty()) "—" else "${(sorted.count { it.inTarget(profile) } * 100f / sorted.size).roundToInt()}%",
            "Morning" to (sorted.filter { it.isMorning() }.average()?.toString() ?: "—"),
            "Evening" to (sorted.filter { it.isEvening() }.average()?.toString() ?: "—"),
            "Avg pulse" to (avg?.pulse?.toString() ?: "—"),
        )
        val tileW = (PAGE_W - 2 * M - 5 * 8f) / 6
        tiles.forEachIndexed { i, (label, value) ->
            val x = M + i * (tileW + 8f)
            c.drawRoundRect(RectF(x, y, x + tileW, y + 46f), 6f, 6f, box)
            c.drawText(label, x + 8f, y + 16f, muted)
            c.drawText(value, x + 8f, y + 36f, h2)
        }
        y += 66f

        // Chart
        if (options.includeChart && sorted.size >= 2) {
            c.drawText("Trend", M, y, h2)
            y += 10f
            val chart = RectF(M + 24f, y, PAGE_W - M, y + 170f)
            drawChart(c, chart, sorted, profile, muted)
            y = chart.bottom + 26f
        }

        // Category distribution
        if (sorted.isNotEmpty()) {
            c.drawText("Readings by range (${guideline.label})", M, y, h2)
            y += 14f
            val groups = sorted.groupBy { BpClassifier.classify(it.systolic, it.diastolic, guideline) }
                .entries.sortedBy { it.key.severity.level }
            var x = M
            val total = sorted.size.toFloat()
            for ((cat, list) in groups) {
                val w = (PAGE_W - 2 * M) * list.size / total
                c.drawRect(x, y, x + w, y + 10f, Paint().apply { color = cat.severity.color.toArgb() })
                x += w
            }
            y += 24f
            c.drawText(groups.joinToString("   ") { "${it.key.label} ${(it.value.size * 100 / total).roundToInt()}%" }, M, y, ink)
            y += 26f
        }

        // Medications
        if (options.includeMeds && meds.isNotEmpty()) {
            c.drawText("Medications", M, y, h2)
            y += 16f
            for (m in meds) {
                c.drawText("• ${m.name} ${m.dose} — ${m.times.replace(",", ", ")} — since ${dateFmt.format(Date(m.startDate))}${if (!m.active) " (stopped)" else ""}", M, y, ink)
                y += 14f
            }
            y += 12f
        }

        // Table
        if (options.includeTable && sorted.isNotEmpty()) {
            fun tableHeader() {
                c.drawText("All readings", M, y, h2)
                y += 16f
                val cols = listOf("Date & time", "BP", "Pulse", "Range", "Arm / position", if (options.includeNotes) "Tags & note" else "Tags")
                val xs = listOf(M, M + 120f, M + 175f, M + 215f, M + 285f, M + 370f)
                cols.forEachIndexed { i, s -> c.drawText(s, xs[i], y, bold) }
                y += 6f
                c.drawLine(M, y, PAGE_W - M, y, muted)
                y += 12f
            }
            if (y > PAGE_H - 120f) newPage()
            tableHeader()
            for (r in sorted.asReversed()) {
                if (y > PAGE_H - 50f) {
                    newPage()
                    tableHeader()
                }
                val cat = BpClassifier.classify(r.systolic, r.diastolic, guideline)
                val xs = listOf(M, M + 120f, M + 175f, M + 215f, M + 285f, M + 370f)
                c.drawText(rowFmt.format(Date(r.timestamp)), xs[0], y, ink)
                c.drawText("${r.systolic}/${r.diastolic}", xs[1], y, bold)
                c.drawText(r.pulse?.toString() ?: "–", xs[2], y, ink)
                c.drawText(cat.label, xs[3], y, Paint(ink).apply { color = cat.severity.onColor.toArgb() })
                c.drawText("${r.arm.lowercase()} / ${r.position.lowercase()}", xs[4], y, muted)
                val extra = buildString {
                    append(r.tags.replace(",", ", "))
                    if (options.includeNotes && r.note.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(r.note)
                    }
                    if (r.averagedCount > 1) {
                        if (isNotEmpty()) append(" · ")
                        append("avg of ${r.averagedCount}")
                    }
                }
                c.drawText(extra.take(40), xs[5], y, muted)
                y += 14f
            }
        }

        footer()
        doc.finishPage(page)
        val file = File(exportDir(context), "snaptensio-report.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawChart(c: Canvas, r: RectF, sorted: List<Reading>, profile: Profile, label: Paint) {
        val minV = (minOf(sorted.minOf { it.diastolic }, profile.targetDiastolic) - 10).coerceAtLeast(30)
        val maxV = maxOf(sorted.maxOf { it.systolic }, profile.targetSystolic) + 10
        val t0 = sorted.first().timestamp
        val t1 = sorted.last().timestamp.coerceAtLeast(t0 + 1)
        fun x(ts: Long) = r.left + (ts - t0).toFloat() / (t1 - t0) * r.width()
        fun y(v: Int) = r.bottom - (v - minV).toFloat() / (maxV - minV) * r.height()
        val grid = Paint().apply { color = Color.rgb(226, 231, 238); strokeWidth = 0.7f }
        val target = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(138, 150, 168); strokeWidth = 0.8f; style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        var v = (minV / 20 + 1) * 20
        while (v < maxV) {
            c.drawLine(r.left, y(v), r.right, y(v), grid)
            c.drawText(v.toString(), r.left - 22f, y(v) + 3f, label)
            v += 20
        }
        c.drawLine(r.left, y(profile.targetSystolic), r.right, y(profile.targetSystolic), target)
        c.drawLine(r.left, y(profile.targetDiastolic), r.right, y(profile.targetDiastolic), target)
        fun line(color: Int, value: (Reading) -> Int) {
            val p = Path()
            sorted.forEachIndexed { i, rd -> if (i == 0) p.moveTo(x(rd.timestamp), y(value(rd))) else p.lineTo(x(rd.timestamp), y(value(rd))) }
            c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; strokeWidth = 1.6f; style = Paint.Style.STROKE })
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            if (sorted.size <= 120) sorted.forEach { c.drawCircle(x(it.timestamp), y(value(it)), 1.8f, dot) }
        }
        line(Color.rgb(31, 79, 216)) { it.systolic }
        line(Color.rgb(224, 122, 43)) { it.diastolic }
        c.drawText("— Systolic", r.right - 140f, r.top - 2f, Paint(label).apply { color = Color.rgb(31, 79, 216) })
        c.drawText("— Diastolic", r.right - 70f, r.top - 2f, Paint(label).apply { color = Color.rgb(224, 122, 43) })
    }

    /** Renders the first page of a PDF to a bitmap for the on-screen preview. */
    fun renderFirstPage(file: File, widthPx: Int): Bitmap? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(0).use { pg ->
                    val h = (widthPx.toFloat() * pg.height / pg.width).roundToInt()
                    val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    }.getOrNull()
}
