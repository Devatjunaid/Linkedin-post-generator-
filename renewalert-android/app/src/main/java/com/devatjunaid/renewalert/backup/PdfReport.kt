package com.devatjunaid.renewalert.backup

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.devatjunaid.renewalert.data.SubStatus
import com.devatjunaid.renewalert.domain.Billing
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.domain.Portfolio
import java.time.LocalDate

/** A one-or-more page A4 summary the user can save or print. */
object PdfReport {
    private const val W = 595
    private const val H = 842
    private const val M = 40f

    fun write(context: Context, uri: Uri, p: Portfolio) {
        val doc = PdfDocument()
        val cur = p.settings.currency
        val title = Paint().apply { color = Color.rgb(21, 23, 28); textSize = 22f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val h2 = Paint().apply { color = Color.rgb(21, 23, 28); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { color = Color.rgb(40, 44, 52); textSize = 10.5f; isAntiAlias = true }
        val muted = Paint().apply { color = Color.rgb(91, 97, 107); textSize = 9.5f; isAntiAlias = true }
        val accent = Paint().apply { color = Color.rgb(61, 99, 255); isAntiAlias = true }
        val line = Paint().apply { color = Color.rgb(226, 228, 222); strokeWidth = 1f }

        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
        var c = page.canvas
        var y = M

        fun newPage() {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            c = page.canvas
            y = M
        }

        fun ensure(space: Float) { if (y + space > H - M) newPage() }

        c.drawRect(0f, 0f, W.toFloat(), 6f, accent)
        y += 20f
        c.drawText("Renew Alert subscription report", M, y, title)
        y += 18f
        c.drawText("Generated ${Formats.fullDate(p.today)} · amounts in $cur", M, y, muted)
        y += 30f

        val summary = listOf(
            "Monthly spend" to Formats.money(p.monthlyTotal, cur),
            "Yearly spend" to Formats.money(p.yearlyTotal, cur),
            "Active" to p.active.size.toString(),
            "Trials" to p.trials.size.toString(),
            "Saved by cancelling" to Formats.money(p.savedTotalMain, cur),
        )
        summary.forEach { (k, v) ->
            c.drawText(k, M, y, body)
            c.drawText(v, W - M - body.measureText(v), y, h2)
            y += 18f
        }
        y += 14f

        c.drawText("By category (monthly)", M, y, h2); y += 18f
        p.categories.forEach { cat ->
            ensure(18f)
            val v = Formats.money(cat.monthlyMain, cur)
            c.drawText("${cat.category} (${cat.count})", M, y, body)
            c.drawText(v, W - M - body.measureText(v), y, body)
            y += 16f
        }
        y += 16f

        ensure(40f)
        c.drawText("Subscriptions", M, y, h2); y += 16f
        val cols = floatArrayOf(M, 220f, 330f, 420f)
        listOf("Name", "Price", "Next charge", "Per year ($cur)").forEachIndexed { i, s -> c.drawText(s, cols[i], y, muted) }
        y += 6f; c.drawLine(M, y, W - M, y, line); y += 14f
        p.subs.sortedWith(compareBy({ it.status.ordinal }, { it.name.lowercase() })).forEach { s ->
            ensure(18f)
            val status = when (s.status) {
                SubStatus.TRIAL -> " (trial)"; SubStatus.PAUSED -> " (paused)"; SubStatus.CANCELLED -> " (cancelled)"; else -> ""
            }
            c.drawText(ellipsize(s.name + status, body, cols[1] - cols[0] - 8), cols[0], y, body)
            c.drawText(Formats.money(s.priceMinor, s.currency) + Billing.perCycleSuffix(s.cycleUnit, s.cycleCount), cols[1], y, body)
            c.drawText(p.nextCharge(s)?.let { Formats.dayMonth(it) } ?: "-", cols[2], y, body)
            val yearly = if (s.status == SubStatus.ACTIVE) Formats.money(p.yearlyMain(s), cur) else "-"
            c.drawText(yearly, W - M - body.measureText(yearly), y, body)
            y += 17f
        }

        ensure(40f)
        y += 20f
        c.drawText("Created with Renew Alert on ${LocalDate.now()}. Data stays on your device.", M, y, muted)
        doc.finishPage(page)

        context.contentResolver.openOutputStream(uri, "wt")?.use { doc.writeTo(it) }
            ?: run { doc.close(); error("Couldn't open the file for writing") }
        doc.close()
    }

    private fun ellipsize(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > width) t = t.dropLast(1)
        return "$t…"
    }
}
