package com.sunarc.inverterlogger.domain

import kotlin.math.abs

/** A number found in OCR text, with every sensible reading of its separators. */
data class NumberToken(
    val raw: String,
    val start: Int,
    val end: Int,
    /** Candidate values, most likely first. Several when "1.234" could be 1.234 or 1234. */
    val candidates: List<Double>,
    val unit: String?,
    /** True when look-alike letters (O, l, S, B…) had to be turned into digits. */
    val repaired: Boolean
) {
    val ambiguous: Boolean get() = candidates.size > 1
}

object NumberParsing {
    private val confusable = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', '|' to '1', 'i' to '1',
        'S' to '5', 's' to '5',
        'B' to '8',
        'Z' to '2', 'z' to '2'
    )

    private const val NUM_CHARS = "0-9OoDQIl|iSsBZz"
    private val tokenRegex = Regex(
        "(?<![\\p{L}\\d])([$NUM_CHARS][$NUM_CHARS.,']*[$NUM_CHARS]|[0-9])\\s?" +
            "(gwh|mwh|kwh|wh|mw|kw|kva|va|w|vac|vdc|v|hz|°c|℃|°)?(?![\\p{L}\\d])",
        RegexOption.IGNORE_CASE
    )

    fun findNumbers(text: String, style: DecimalStyle): List<NumberToken> {
        val out = mutableListOf<NumberToken>()
        for (m in tokenRegex.findAll(text)) {
            val body = m.groupValues[1].trimEnd('.', ',')
            if (body.none { it.isDigit() }) continue
            // Reject words that only look numeric, e.g. "SOS" or "Ill": need more digits than repairs.
            val repairs = body.count { it in confusable }
            val digits = body.count { it.isDigit() }
            if (repairs > digits) continue
            val fixed = buildString { body.forEach { append(confusable[it] ?: it) } }
            val candidates = interpret(fixed, style)
            if (candidates.isEmpty()) continue
            val unit = m.groupValues[2].takeIf { it.isNotEmpty() }?.lowercase()?.replace("℃", "°c")
            out += NumberToken(m.value.trim(), m.range.first, m.range.last + 1, candidates, unit, repairs > 0)
        }
        return out
    }

    /** Returns the possible values of a digit string with separators, most likely first. */
    fun interpret(s: String, style: DecimalStyle): List<Double> {
        val clean = s.replace("'", "").replace(" ", "")
        if (clean.isEmpty() || clean.any { !it.isDigit() && it != '.' && it != ',' }) return emptyList()
        val dots = clean.count { it == '.' }
        val commas = clean.count { it == ',' }
        fun num(x: String) = x.toDoubleOrNull()
        val result = mutableListOf<Double>()
        when {
            dots == 0 && commas == 0 -> num(clean)?.let { result += it }
            dots > 0 && commas > 0 -> {
                val decimalIsDot = clean.lastIndexOf('.') > clean.lastIndexOf(',')
                val v = if (decimalIsDot) clean.replace(",", "") else clean.replace(".", "").replace(',', '.')
                num(v)?.let { result += it }
            }
            dots > 1 -> num(clean.replace(".", ""))?.let { result += it }
            commas > 1 -> num(clean.replace(",", ""))?.let { result += it }
            dots == 1 -> {
                val after = clean.length - clean.indexOf('.') - 1
                val asDecimal = num(clean)
                val asThousands = num(clean.replace(".", ""))
                // Inverter firmware almost always prints a decimal point, whatever the country.
                if (after == 3 && asThousands != null && asDecimal != null) {
                    result += asDecimal; result += asThousands
                } else asDecimal?.let { result += it }
            }
            else -> { // one comma
                val after = clean.length - clean.indexOf(',') - 1
                val asDecimal = num(clean.replace(',', '.'))
                val asThousands = num(clean.replace(",", ""))
                if (after == 3 && asThousands != null && asDecimal != null) {
                    if (style == DecimalStyle.POINT) { result += asThousands; result += asDecimal }
                    else { result += asDecimal; result += asThousands }
                } else asDecimal?.let { result += it }
            }
        }
        return result.distinct()
    }

    fun unitKind(unit: String?): UnitKind? = when (unit) {
        null -> null
        "gwh", "mwh", "kwh", "wh" -> UnitKind.ENERGY
        "mw", "kw", "w", "kva", "va" -> UnitKind.POWER
        "v", "vac", "vdc" -> UnitKind.VOLTAGE
        "hz" -> UnitKind.FREQUENCY
        "°c", "°" -> UnitKind.TEMPERATURE
        else -> null
    }

    /** Converts to the app's base units: kWh for energy, W for power. */
    fun toBase(value: Double, unit: String?, kind: UnitKind, systemKwp: Double?): Double = when (kind) {
        UnitKind.ENERGY -> when (unit) {
            "wh" -> value / 1000.0
            "mwh" -> value * 1000.0
            "gwh" -> value * 1_000_000.0
            else -> value
        }
        UnitKind.POWER -> when (unit) {
            "kw", "kva" -> value * 1000.0
            "mw" -> value * 1_000_000.0
            "w", "va" -> value
            else -> {
                // No unit: a small number is almost certainly kW, a large one W.
                val limitKw = (systemKwp ?: 15.0) * 1.5
                if (value <= limitKw) value * 1000.0 else value
            }
        }
        else -> value
    }

    fun close(a: Double, b: Double, tolerance: Double) = abs(a - b) <= tolerance
}
