package com.brickbeam.buildbros.calc

import kotlin.math.abs

enum class UnitSystem { IMPERIAL, METRIC }

/** Exact conversion factors (NIST / international definitions). */
object Conv {
    const val M_PER_FT = 0.3048
    const val M_PER_IN = 0.0254
    const val M2_PER_FT2 = 0.09290304
    const val M3_PER_FT3 = 0.028316846592
    const val M3_PER_YD3 = 0.764554857984
    const val KG_PER_LB = 0.45359237
    const val KG_PER_SHORT_TON = 907.18474
    const val L_PER_GAL = 3.785411784
    const val L_PER_QT = 0.946352946
    const val MPA_PER_PSI = 0.006894757293168
    const val KG_M_PER_LB_FT = KG_PER_LB / M_PER_FT

    fun ftToM(ft: Double) = ft * M_PER_FT
    fun mToFt(m: Double) = m / M_PER_FT
    fun inToMm(inch: Double) = inch * 25.4
    fun mmToIn(mm: Double) = mm / 25.4
    fun m2ToFt2(m2: Double) = m2 / M2_PER_FT2
    fun m3ToYd3(m3: Double) = m3 / M3_PER_YD3
    fun m3ToFt3(m3: Double) = m3 / M3_PER_FT3
    fun kgToLb(kg: Double) = kg / KG_PER_LB
}

/**
 * Parses lengths typed the way people on site write them.
 *
 * Imperial: `12`, `12.5`, `12'`, `12' 6"`, `12'6`, `12 ft 6 in`, `6"`, `6 in`, `3 1/2"`.
 * A bare number means feet. Metric: `3.6`, `3,6`, `360 cm`, `3600 mm`, `3.6 m`; a bare number means metres.
 */
object LengthParser {
    private const val NUM = """(\d+(?:[.,]\d+)?(?:\s+\d+/\d+)?|\d+/\d+)"""
    private val IMPERIAL = Regex("""^\s*(?:$NUM\s*')?\s*(?:$NUM\s*(")?)?\s*$""")
    private val METRIC = Regex("""^\s*(\d+(?:[.,]\d+)?)\s*(mm|cm|m)?\s*$""")

    /** Big lengths (walls, rooms, slabs). Returns metres or null. */
    fun parse(text: String, system: UnitSystem): Double? =
        if (system == UnitSystem.IMPERIAL) parseImperial(text, bareIsFeet = true) else parseMetric(text, "m")

    /** Small dimensions (joints, tiles, thickness). Returns millimetres or null. */
    fun parseSmallMm(text: String, system: UnitSystem): Double? =
        if (system == UnitSystem.IMPERIAL) parseImperial(text, bareIsFeet = false)?.let { it * 1000.0 }
        else parseMetric(text, "mm")?.let { it * 1000.0 }

    private fun normalizeImperial(text: String): String = text.lowercase()
        .replace("″", "\"").replace("”", "\"").replace("''", "\"")
        .replace("′", "'").replace("’", "'")
        .replace(Regex("""\b(feet|foot|ft)\b"""), "'")
        .replace(Regex("""\b(inches|inch|in)\b"""), "\"")
        .replace('-', ' ')

    private fun parseImperial(raw: String, bareIsFeet: Boolean): Double? {
        val text = normalizeImperial(raw)
        if (text.isBlank()) return null
        val m = IMPERIAL.matchEntire(text) ?: return null
        val g1 = m.groups[1]?.value
        val g2 = m.groups[2]?.value
        val inchMark = m.groups[3] != null
        if (g1 == null && g2 == null) return null
        val feet: Double
        val inches: Double
        if (g1 != null) {
            feet = parseNumber(g1) ?: return null
            inches = g2?.let { parseNumber(it) ?: return null } ?: 0.0
        } else if (inchMark || !bareIsFeet) {
            feet = 0.0
            inches = parseNumber(g2!!) ?: return null
        } else {
            feet = parseNumber(g2!!) ?: return null
            inches = 0.0
        }
        return feet * Conv.M_PER_FT + inches * Conv.M_PER_IN
    }

    private fun parseMetric(raw: String, defaultUnit: String): Double? {
        val m = METRIC.matchEntire(raw.lowercase()) ?: return null
        val v = parseNumber(m.groupValues[1]) ?: return null
        return when (m.groups[2]?.value ?: defaultUnit) {
            "mm" -> v / 1000.0
            "cm" -> v / 100.0
            else -> v
        }
    }

    /** Accepts `1.5`, `1,5`, `3/8`, `3 1/2`. */
    fun parseNumber(raw: String): Double? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val parts = t.split(Regex("""\s+"""))
        var total = 0.0
        for (p in parts) {
            total += if (p.contains('/')) {
                val (a, b) = p.split('/').let { if (it.size != 2) return null else it[0] to it[1] }
                val n = a.toDoubleOrNull() ?: return null
                val d = b.toDoubleOrNull() ?: return null
                if (d == 0.0) return null
                n / d
            } else {
                p.replace(',', '.').toDoubleOrNull() ?: return null
            }
        }
        return total
    }
}

/** Formats metres back into the user's preferred notation. */
object LengthFormat {
    fun big(m: Double, system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) feetInches(m) else "${trim(m, 2)} m"

    fun small(mm: Double, system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) "${inchFraction(mm / 25.4)}\"" else "${trim(mm, 1)} mm"

    fun feetInches(m: Double): String {
        val totalIn = m / Conv.M_PER_IN
        var ft = (totalIn / 12).toInt()
        var inch = Math.round((totalIn - ft * 12) * 8) / 8.0
        if (inch >= 12.0) { ft += 1; inch -= 12.0 }
        return if (inch == 0.0) "$ft′" else "$ft′ ${inchFraction(inch)}″"
    }

    /** 3.375 -> "3 3/8" (nearest 1/16). */
    fun inchFraction(inches: Double): String {
        val sixteenths = Math.round(inches * 16).toInt()
        val whole = sixteenths / 16
        var num = sixteenths % 16
        var den = 16
        while (num != 0 && num % 2 == 0) { num /= 2; den /= 2 }
        return when {
            num == 0 -> "$whole"
            whole == 0 -> "$num/$den"
            else -> "$whole $num/$den"
        }
    }

    fun trim(v: Double, decimals: Int): String {
        val s = String.format(java.util.Locale.US, "%.${decimals}f", v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }
}

internal fun Double.nearlyZero() = abs(this) < 1e-9
