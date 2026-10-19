package com.sunarc.inverterlogger.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class OcrBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val height: Float get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f
    val centerX: Float get() = (left + right) / 2f
}

data class OcrLine(val text: String, val box: OcrBox? = null)

data class ParsedValue(
    val field: Field,
    /** In base units: kWh for energy, W for power, V, Hz, °C. */
    val value: Double,
    val raw: String,
    val confidence: Double
)

data class ParseContext(
    val brand: InverterBrand? = null,
    val systemKwp: Double? = null,
    val lastTotalKwh: Double? = null,
    val decimalStyle: DecimalStyle = DecimalStyle.POINT
)

data class ParseResult(
    val values: Map<Field, ParsedValue>,
    val faultCode: String?,
    val detectedBrand: InverterBrand?,
    val rawText: String
) {
    val energyFieldCount: Int get() = values.keys.count { it.unitKind == UnitKind.ENERGY || it == Field.POWER_NOW }

    /** Used to pick the best of several OCR passes over the same photo. */
    val score: Double get() = values.values.sumOf { it.confidence } + if (faultCode != null) 0.5 else 0.0

    operator fun get(field: Field): ParsedValue? = values[field]

    companion object {
        val EMPTY = ParseResult(emptyMap(), null, null, "")
    }
}

/**
 * Turns OCR lines from an inverter display or monitoring-app screenshot into readings.
 *
 * 1. Find labels ("E-Today", "Tagesertrag", "Pac"…) and numbers with units on each line.
 * 2. Pair each label with the nearest number on its line, or on the line just below/right of it.
 * 3. Give unlabeled numbers a field from their unit and size (small kWh → today, large → total).
 * 4. Where "1.234" could mean two things, keep the reading that is physically plausible.
 */
class ReadingParser {

    private data class LabelHit(val field: Field, val line: Int, val start: Int, val end: Int)
    private data class ValueHit(val line: Int, val token: NumberToken, var used: Boolean = false)

    fun parse(lines: List<OcrLine>, ctx: ParseContext): ParseResult {
        val cleaned = lines.map { it.copy(text = preprocess(it.text)) }.filter { it.text.isNotBlank() }
        val rawText = cleaned.joinToString("\n") { it.text }
        val detected = InverterBrands.detect(rawText)
        val brand = ctx.brand?.takeIf { it.id != "other" } ?: detected ?: ctx.brand
        val labels = FieldLabels.forBrand(brand)

        val labelHits = mutableListOf<LabelHit>()
        val valueHits = mutableListOf<ValueHit>()
        cleaned.forEachIndexed { i, line ->
            labelHits += findLabels(line.text.lowercase(), labels, i)
            NumberParsing.findNumbers(line.text, ctx.decimalStyle).forEach { valueHits += ValueHit(i, it) }
        }

        // Fault codes first, so their digits are never read as energy.
        val fault = findFaultCode(cleaned, valueHits)

        val results = linkedMapOf<Field, ParsedValue>()

        // Labeled values. Longer labels go first so "E-Today" wins over "Today".
        for (hit in labelHits.filter { it.field != Field.FAULT_CODE }.sortedByDescending { it.end - it.start }) {
            if (results.containsKey(hit.field)) continue
            val candidate = sameLineValue(hit, labelHits, valueHits, cleaned[hit.line].text.length)
                ?: neighbourValue(hit, cleaned, valueHits)
                ?: continue
            val parsed = toParsed(hit.field, candidate.token, ctx, labeled = true) ?: continue
            candidate.used = true
            results[hit.field] = parsed
        }

        // Unlabeled values with a clear unit.
        val free = valueHits.filter { !it.used && it.token.unit != null }
        if (Field.POWER_NOW !in results) {
            free.firstOrNull { NumberParsing.unitKind(it.token.unit) == UnitKind.POWER }?.let { v ->
                toParsed(Field.POWER_NOW, v.token, ctx, labeled = false)?.let { results[Field.POWER_NOW] = it; v.used = true }
            }
        }
        val energies = free.filter { !it.used && NumberParsing.unitKind(it.token.unit) == UnitKind.ENERGY }
            .sortedBy { NumberParsing.toBase(it.token.candidates.first(), it.token.unit, UnitKind.ENERGY, ctx.systemKwp) }
        if (Field.ENERGY_TODAY !in results) {
            for (v in energies) {
                val p = toParsed(Field.ENERGY_TODAY, v.token, ctx, labeled = false) ?: continue
                results[Field.ENERGY_TODAY] = p
                v.used = true
                break
            }
        }
        if (Field.ENERGY_TOTAL !in results) {
            val today = results[Field.ENERGY_TODAY]?.value ?: 0.0
            for (v in energies.asReversed()) {
                if (v.used) continue
                val p = toParsed(Field.ENERGY_TOTAL, v.token, ctx, labeled = false) ?: continue
                if (p.value <= today) break
                results[Field.ENERGY_TOTAL] = p
                v.used = true
                break
            }
        }

        if (fault != null) {
            results[Field.FAULT_CODE] = ParsedValue(Field.FAULT_CODE, fault.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0, fault, 0.8)
        }
        return ParseResult(results, fault, detected, rawText)
    }

    /** Splits glued text such as "Pac3210W" and normalises dashes and odd spaces. */
    internal fun preprocess(text: String): String = text
        .replace('–', '-').replace('—', '-').replace(' ', ' ')
        .replace(Regex("([A-Za-z]{2,})(\\d)"), "$1 $2")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun findLabels(lower: String, labels: Map<Field, List<String>>, line: Int): List<LabelHit> {
        val all = labels.flatMap { (f, words) -> words.map { f to it } }.sortedByDescending { it.second.length }
        val taken = BooleanArray(lower.length)
        val hits = mutableListOf<LabelHit>()
        for ((field, word) in all) {
            var from = 0
            while (true) {
                val idx = lower.indexOf(word, from)
                if (idx < 0) break
                val end = idx + word.length
                from = idx + 1
                val before = if (idx == 0) ' ' else lower[idx - 1]
                val after = if (end >= lower.length) ' ' else lower[end]
                if (before.isLetterOrDigit() || after.isLetter()) continue
                if ((idx until end).any { taken[it] }) continue
                (idx until end).forEach { taken[it] = true }
                hits += LabelHit(field, line, idx, end)
            }
        }
        return hits
    }

    private fun sameLineValue(hit: LabelHit, labels: List<LabelHit>, values: List<ValueHit>, lineLength: Int): ValueHit? {
        val onLine = values.filter { it.line == hit.line && !it.used && compatible(hit.field, it.token) }
        if (onLine.isEmpty()) return null
        val nextLabelStart = labels.filter { it.line == hit.line && it.start > hit.end }.minOfOrNull { it.start } ?: lineLength
        val prevLabelEnd = labels.filter { it.line == hit.line && it.end <= hit.start }.maxOfOrNull { it.end } ?: 0
        // Value after the label, before the next label ("E-Today 18.4kWh").
        onLine.filter { it.token.start >= hit.end && it.token.start < nextLabelStart }.minByOrNull { it.token.start }?.let { return it }
        // Value before the label ("18.4 kWh today").
        return onLine.filter { it.token.end <= hit.start && it.token.start >= prevLabelEnd }.maxByOrNull { it.token.end }
    }

    private fun neighbourValue(hit: LabelHit, lines: List<OcrLine>, values: List<ValueHit>): ValueHit? {
        val labelBox = lines[hit.line].box
        val candidates = values.filter { !it.used && it.line != hit.line && compatible(hit.field, it.token) }
        if (candidates.isEmpty()) return null
        if (labelBox == null) {
            // No geometry (e.g. tests): use the very next line only.
            return candidates.firstOrNull { it.line == hit.line + 1 }
        }
        return candidates.mapNotNull { v ->
            val b = lines[v.line].box ?: return@mapNotNull null
            val h = max(labelBox.height, 1f)
            val below = b.top >= labelBox.centerY && b.top - labelBox.bottom < 2.5f * h &&
                min(b.right, labelBox.right + h * 2) > max(b.left, labelBox.left - h * 2)
            val right = abs(b.centerY - labelBox.centerY) < 0.7f * h && b.left >= labelBox.right - h * 0.5f
            if (!below && !right) null
            else v to (if (right) b.left - labelBox.right else (b.top - labelBox.bottom) + abs(b.centerX - labelBox.centerX))
        }.minByOrNull { it.second }?.first
    }

    private fun compatible(field: Field, token: NumberToken): Boolean {
        val kind = NumberParsing.unitKind(token.unit) ?: return field.unitKind != UnitKind.NONE
        return kind == field.unitKind
    }

    private fun toParsed(field: Field, token: NumberToken, ctx: ParseContext, labeled: Boolean): ParsedValue? {
        val kind = NumberParsing.unitKind(token.unit) ?: field.unitKind
        val options = token.candidates.map { NumberParsing.toBase(it, token.unit, kind, ctx.systemKwp) }
        val plausible = options.filter { plausible(field, it, ctx) }
        val value = when {
            plausible.isEmpty() -> return null
            field == Field.ENERGY_TOTAL && ctx.lastTotalKwh != null ->
                plausible.minBy { abs(it - ctx.lastTotalKwh) }
            else -> plausible.first()
        }
        var confidence = 0.45
        if (labeled) confidence += 0.25
        if (token.unit != null) confidence += 0.17
        if (plausible.size == 1 && options.size == 1) confidence += 0.1
        if (token.repaired) confidence -= 0.18
        if (token.ambiguous && plausible.size > 1) confidence -= 0.15
        return ParsedValue(field, value, token.raw, confidence.coerceIn(0.05, 0.99))
    }

    private fun dailyLimitKwh(ctx: ParseContext): Double = ctx.systemKwp?.let { it * 9.0 } ?: 250.0

    private fun plausible(field: Field, v: Double, ctx: ParseContext): Boolean = when (field) {
        Field.ENERGY_TODAY -> v >= 0 && v <= dailyLimitKwh(ctx)
        Field.ENERGY_MONTH -> v >= 0 && v <= dailyLimitKwh(ctx) * 31
        Field.ENERGY_YEAR -> v >= 0 && v <= dailyLimitKwh(ctx) * 366
        Field.ENERGY_TOTAL -> v >= 0 && (ctx.lastTotalKwh == null || v >= ctx.lastTotalKwh - 0.5)
        Field.POWER_NOW -> v >= 0 && v <= (ctx.systemKwp ?: 100.0) * 1000.0 * 1.6
        Field.PV_VOLTAGE -> v in 0.0..1500.0
        Field.GRID_VOLTAGE -> v in 80.0..480.0
        Field.FREQUENCY -> v in 45.0..65.0
        Field.TEMPERATURE -> v in -40.0..120.0
        Field.FAULT_CODE -> true
    }

    private val faultPattern = Regex(
        "(?i)\\b(?:error|fault|event|alarm|warning|state|status|err|fehler|störung|erreur|errore|fallo|code)\\s*(?:no\\.?|nr\\.?|code)?\\s*[:#.-]?\\s*([A-Z]{0,3}[- ]?\\d{2,5}[A-Z]?)\\b"
    )
    private val shortCodePattern = Regex("\\b([EFWA])[- ]?(\\d{2,4})\\b")

    private fun findFaultCode(lines: List<OcrLine>, values: List<ValueHit>): String? {
        for ((i, line) in lines.withIndex()) {
            val m = faultPattern.find(line.text) ?: shortCodePattern.find(line.text) ?: continue
            val inMatch = values.filter { it.line == i && it.token.start in m.range }
            // "Status 3210 W" or "W 300" are readings, not faults.
            if (inMatch.any { it.token.unit != null }) continue
            val raw = if (m.groupValues.size > 2) m.groupValues[1] + m.groupValues[2] else m.groupValues[1]
            inMatch.forEach { it.used = true }
            return raw.replace(" ", "").replace("-", "").uppercase()
        }
        return null
    }
}
