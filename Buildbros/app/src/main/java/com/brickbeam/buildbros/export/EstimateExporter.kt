package com.brickbeam.buildbros.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.brickbeam.buildbros.calc.Category
import com.brickbeam.buildbros.calc.EstimateTotals
import com.brickbeam.buildbros.calc.Region
import com.brickbeam.buildbros.calc.RegionGroup
import com.brickbeam.buildbros.data.db.LineItemEntity
import com.brickbeam.buildbros.data.db.ProjectEntity
import com.brickbeam.buildbros.ui.components.Fmt
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

/** Builds shareable PDF / CSV / plain-text versions of an estimate. Runs off the main thread. */
object EstimateExporter {

    private fun exportDir(context: Context) = File(context.cacheDir, "exports").apply { mkdirs() }

    private fun safeName(name: String) = name.replace(Regex("[^A-Za-z0-9-_ ]"), "").trim().replace(' ', '_').ifEmpty { "estimate" }

    fun csv(context: Context, project: ProjectEntity, items: List<LineItemEntity>, totals: EstimateTotals, fmt: Fmt): File {
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder()
        sb.append("Category,Item,Quantity,Unit,Unit price,Total,Already have,Note\n")
        items.forEach { i ->
            val cat = runCatching { Category.valueOf(i.category).label }.getOrDefault(i.category)
            sb.append(listOf(q(cat), q(i.name), i.quantity.toString(), q(i.unit), "%.2f".format(java.util.Locale.US, i.unitPrice),
                "%.2f".format(java.util.Locale.US, i.quantity * i.unitPrice), if (i.owned) "yes" else "no", q(i.note)).joinToString(","))
            sb.append('\n')
        }
        sb.append('\n')
        sb.append("Materials,,,,,${"%.2f".format(java.util.Locale.US, totals.materials)}\n")
        sb.append("${if (totals.taxIncluded) "Tax included" else "Tax"} (${totals.taxRatePct}%),,,,,${"%.2f".format(java.util.Locale.US, totals.tax)}\n")
        sb.append("Contingency,,,,,${"%.2f".format(java.util.Locale.US, totals.contingency)}\n")
        sb.append("Delivery,,,,,${"%.2f".format(java.util.Locale.US, totals.delivery)}\n")
        sb.append("Total (${fmt.region.currency}),,,,,${"%.2f".format(java.util.Locale.US, totals.total)}\n")
        val file = File(exportDir(context), "${safeName(project.name)}.csv")
        file.writeText(sb.toString())
        return file
    }

    fun text(project: ProjectEntity, items: List<LineItemEntity>, totals: EstimateTotals, fmt: Fmt): String = buildString {
        appendLine("${project.name} — material estimate (${fmt.region.name})")
        appendLine()
        items.filter { !it.owned }.forEach {
            appendLine("• ${it.name}: ${fmt.num(it.quantity)} ${it.unit} × ${fmt.moneyPrecise(it.unitPrice)} = ${fmt.moneyPrecise(it.quantity * it.unitPrice)}")
        }
        appendLine()
        appendLine("Materials: ${fmt.moneyPrecise(totals.materials)}")
        appendLine((if (totals.taxIncluded) "Incl. " else "+ ") + "tax ${fmt.num(totals.taxRatePct, 3)}%: ${fmt.moneyPrecise(totals.tax)}")
        if (totals.contingency > 0) appendLine("+ Contingency: ${fmt.moneyPrecise(totals.contingency)}")
        if (totals.delivery > 0) appendLine("+ Delivery: ${fmt.moneyPrecise(totals.delivery)}")
        appendLine("TOTAL: ${fmt.moneyPrecise(totals.total)}")
        appendLine()
        appendLine("Made with Buildbros. Estimates only; confirm quantities and prices with your supplier.")
    }

    fun pdf(context: Context, project: ProjectEntity, items: List<LineItemEntity>, totals: EstimateTotals, region: Region, fmt: Fmt): File {
        val letter = region.group == RegionGroup.US
        val pw = if (letter) 612 else 595
        val ph = if (letter) 792 else 842
        val margin = 40f
        val doc = PdfDocument()
        val ink = Color.rgb(0x22, 0x22, 0x22)
        val mute = Color.rgb(0x6E, 0x6E, 0x6E)
        val accent = Color.rgb(0xFF, 0x7A, 0x1A)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 10f }
        val bold = Paint(body).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val small = Paint(body).apply { color = mute; textSize = 8.5f }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        val cols = floatArrayOf(margin, margin + 250f, margin + 310f, margin + 380f, pw - margin)
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun header(c: Canvas) {
            fill.color = Color.rgb(0x15, 0x17, 0x1C)
            c.drawRect(0f, 0f, pw.toFloat(), 64f, fill)
            // Mini logo: bricks + beam + roof.
            fill.color = accent
            c.drawRoundRect(RectF(margin, 16f, margin + 32f, 48f), 8f, 8f, fill)
            fill.color = Color.rgb(0xC4, 0x53, 0x2F)
            c.drawRect(margin + 6f, 36f, margin + 15f, 42f, fill)
            c.drawRect(margin + 17f, 36f, margin + 26f, 42f, fill)
            fill.color = Color.rgb(0x6B, 0x81, 0x96)
            c.drawRect(margin + 5f, 31f, margin + 27f, 34f, fill)
            val wm = Paint(title).apply { color = Color.WHITE; textSize = 18f }
            c.drawText("Buildbros", margin + 42f, 38f, wm)
            val sub = Paint(small).apply { color = Color.rgb(0xA7, 0xAD, 0xB8) }
            c.drawText("Material estimate", margin + 42f, 51f, sub)
            c.drawText("Page ${pageNo}", pw - margin - 40f, 38f, sub)
        }

        fun tableHeader(c: Canvas) {
            c.drawText("Item", cols[0], y, bold)
            c.drawText("Qty", cols[1], y, bold)
            c.drawText("Unit", cols[2], y, bold)
            val up = "Unit price"
            c.drawText(up, cols[4] - 80f - bold.measureText(up), y, bold)
            val t = "Total"
            c.drawText(t, cols[4] - bold.measureText(t), y, bold)
            y += 6f
            fill.color = ink
            c.drawRect(margin, y, pw - margin, y + 1f, fill)
            y += 14f
        }

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, pageNo).create())
            page = p
            canvas = p.canvas
            header(p.canvas)
            y = 92f
        }

        fun ensure(space: Float, withTableHeader: Boolean) {
            if (canvas == null || y + space > ph - margin - 30f) {
                newPage()
                if (withTableHeader) tableHeader(canvas!!)
            }
        }

        newPage()
        val c0 = canvas!!
        c0.drawText(project.name, margin, y, title)
        y += 18f
        c0.drawText(
            "${region.name} · ${region.currency} · ${DateFormat.getDateInstance(DateFormat.LONG).format(Date())}",
            margin, y, small,
        )
        y += 26f
        tableHeader(c0)

        Category.entries.forEach { cat ->
            val group = items.filter { (runCatching { Category.valueOf(it.category) }.getOrNull() ?: Category.OTHER) == cat }
            if (group.isEmpty()) return@forEach
            ensure(18f, true)
            canvas!!.drawText(cat.label.uppercase(), margin, y, Paint(small).apply { color = accent; typeface = Typeface.DEFAULT_BOLD })
            y += 14f
            group.forEach { i ->
                ensure(i.note.isNotBlank().let { if (it) 26f else 16f }, true)
                val c = canvas!!
                val name = ellipsize(i.name + if (i.owned) " (already have)" else "", body, cols[1] - cols[0] - 8f)
                c.drawText(name, cols[0], y, if (i.owned) small else body)
                c.drawText(fmt.num(i.quantity), cols[1], y, body)
                c.drawText(ellipsize(i.unit, body, cols[3] - cols[2] - 4f), cols[2], y, body)
                val up = fmt.moneyPrecise(i.unitPrice)
                c.drawText(up, cols[4] - 80f - body.measureText(up), y, body)
                val tot = if (i.owned) "—" else fmt.moneyPrecise(i.quantity * i.unitPrice)
                c.drawText(tot, cols[4] - body.measureText(tot), y, body)
                if (i.note.isNotBlank()) {
                    y += 11f
                    c.drawText(ellipsize(i.note, small, cols[1] - cols[0]), cols[0] + 8f, y, small)
                }
                y += 16f
            }
        }

        ensure(130f, false)
        val c = canvas!!
        y += 6f
        fill.color = ink
        c.drawRect(cols[2], y, pw - margin, y + 1f, fill)
        y += 16f
        fun totalRow(label: String, value: String, strong: Boolean = false) {
            val p = if (strong) Paint(bold).apply { textSize = 13f } else body
            c.drawText(label, cols[2], y, p)
            c.drawText(value, cols[4] - p.measureText(value), y, p)
            y += if (strong) 20f else 15f
        }
        totalRow("Materials", fmt.moneyPrecise(totals.materials))
        totalRow(
            (if (totals.taxIncluded) "Incl. ${region.taxLabel} " else "${region.taxLabel} + local ") + "${fmt.num(totals.taxRatePct, 3)}%",
            fmt.moneyPrecise(totals.tax),
        )
        if (totals.contingency > 0) totalRow("Contingency", fmt.moneyPrecise(totals.contingency))
        if (totals.delivery > 0) totalRow("Delivery", fmt.moneyPrecise(totals.delivery))
        totalRow("Total", fmt.moneyPrecise(totals.total), strong = true)

        canvas!!.drawText(
            "Estimate only. Quantities follow standard trade formulas; prices are the values entered in the app.",
            margin, ph - margin, small,
        )
        canvas!!.drawText("Confirm with your supplier and follow local building codes.", margin, ph - margin + 11f, small)
        page?.let { doc.finishPage(it) }

        val file = File(exportDir(context), "${safeName(project.name)}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun ellipsize(text: String, paint: Paint, max: Float): String {
        if (paint.measureText(text) <= max) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    fun shareIntent(context: Context, file: File, mime: String, subject: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            clipData = ClipData.newRawUri(subject, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, subject)
    }

    fun shareTextIntent(text: String, subject: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(send, subject)
    }
}
