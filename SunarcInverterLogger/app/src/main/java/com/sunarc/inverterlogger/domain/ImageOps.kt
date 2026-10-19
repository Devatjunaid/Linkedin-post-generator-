package com.sunarc.inverterlogger.domain

/**
 * Pixel clean-up passes run before text recognition. LCD and seven-segment displays have low
 * contrast and gaps between segments; stretching contrast and thickening strokes makes the digits
 * readable to a general text recogniser. Works on ARGB pixel arrays so it is platform-neutral.
 */
object ImageOps {

    enum class Pass { ORIGINAL, CONTRAST, THRESHOLD_BOLD, INVERTED_BOLD }

    val lcdPasses = listOf(Pass.ORIGINAL, Pass.CONTRAST, Pass.THRESHOLD_BOLD, Pass.INVERTED_BOLD)

    fun apply(pass: Pass, argb: IntArray, width: Int, height: Int): IntArray = when (pass) {
        Pass.ORIGINAL -> argb
        Pass.CONTRAST -> toArgb(stretch(gray(argb)))
        Pass.THRESHOLD_BOLD -> toArgb(dilateDark(threshold(stretch(gray(argb))), width, height, 1))
        Pass.INVERTED_BOLD -> toArgb(dilateDark(threshold(invert(stretch(gray(argb)))), width, height, 1))
    }

    fun gray(argb: IntArray): IntArray = IntArray(argb.size) { i ->
        val p = argb[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        (r * 299 + g * 587 + b * 114) / 1000
    }

    /** Maps the 2nd..98th percentile of brightness onto the full 0..255 range. */
    fun stretch(gray: IntArray): IntArray {
        if (gray.isEmpty()) return gray
        val hist = IntArray(256)
        gray.forEach { hist[it]++ }
        val lowTarget = gray.size * 0.02
        val highTarget = gray.size * 0.98
        var acc = 0
        var lo = 0
        var hi = 255
        for (v in 0..255) { acc += hist[v]; if (acc >= lowTarget) { lo = v; break } }
        acc = 0
        for (v in 0..255) { acc += hist[v]; if (acc >= highTarget) { hi = v; break } }
        if (hi <= lo) return gray
        return IntArray(gray.size) { i -> ((gray[i] - lo) * 255 / (hi - lo)).coerceIn(0, 255) }
    }

    fun invert(gray: IntArray): IntArray = IntArray(gray.size) { 255 - gray[it] }

    /** Otsu's method: picks the threshold that best splits the image into ink and background. */
    fun otsu(gray: IntArray): Int {
        val hist = IntArray(256)
        gray.forEach { hist[it]++ }
        val total = gray.size
        var sum = 0.0
        for (i in 0..255) sum += i * hist[i].toDouble()
        var sumB = 0.0
        var wB = 0
        var best = 0.0
        var threshold = 127
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += t * hist[t].toDouble()
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > best) { best = between; threshold = t }
        }
        return threshold
    }

    fun threshold(gray: IntArray): IntArray {
        val t = otsu(gray)
        return IntArray(gray.size) { if (gray[it] > t) 255 else 0 }
    }

    /** Grows dark strokes by [radius] pixels so separate LCD segments join into whole digits. */
    fun dilateDark(binary: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val out = binary.copyOf()
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (binary[y * width + x] != 0) continue
                for (dy in -radius..radius) for (dx in -radius..radius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height) out[ny * width + nx] = 0
                }
            }
        }
        return out
    }

    fun toArgb(gray: IntArray): IntArray = IntArray(gray.size) { i ->
        val g = gray[i]
        (0xFF shl 24) or (g shl 16) or (g shl 8) or g
    }
}

object CsvExport {
    const val HEADER = "date,time,inverter,today_kwh,lifetime_kwh,power_w,source,fault_code,note"

    fun readings(rows: List<Reading>, inverterName: (Long) -> String, zone: java.time.ZoneId): String = buildString {
        appendLine(HEADER)
        rows.sortedBy { it.timestamp }.forEach { r ->
            val t = r.timestamp.atZone(zone)
            append(t.toLocalDate()).append(',')
            append(t.toLocalTime().withNano(0)).append(',')
            append(escape(inverterName(r.inverterId))).append(',')
            append(r.todayKwh?.let { "%.3f".format(java.util.Locale.ROOT, it) }.orEmpty()).append(',')
            append(r.totalKwh?.let { "%.3f".format(java.util.Locale.ROOT, it) }.orEmpty()).append(',')
            append(r.powerW?.let { "%.0f".format(java.util.Locale.ROOT, it) }.orEmpty()).append(',')
            append(r.source.name).append(',')
            append(escape(r.faultCode.orEmpty())).append(',')
            appendLine(escape(r.note.orEmpty()))
        }
    }

    fun dailyYields(days: List<DailyYield>): String = buildString {
        appendLine("date,kwh,estimated")
        days.forEach { appendLine("${it.date},${"%.3f".format(java.util.Locale.ROOT, it.kwh)},${it.estimated}") }
    }

    fun escape(s: String): String {
        // Leading =,+,-,@ would run as a formula in spreadsheet apps.
        val safe = if (s.isNotEmpty() && s[0] in "=+-@") "'$s" else s
        return if (safe.any { it == ',' || it == '"' || it == '\n' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
