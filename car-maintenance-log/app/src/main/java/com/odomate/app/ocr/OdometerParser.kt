package com.odomate.app.ocr

import kotlin.math.abs

/** One piece of recognised text with its box, in the upright image's pixel space. */
data class OcrToken(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class OdometerCandidate(val value: Int, val score: Float, val plausible: Boolean)

/**
 * Picks the most likely odometer number out of everything the camera can read on a dashboard:
 * speed, rpm, clock, trip meter, temperature and the odometer itself.
 */
object OdometerParser {
    private val thousandsGrouped = Regex("""^\d{1,3}([ .,'’]\d{3})+$""")
    private val tripLike = Regex("""^\d{1,4}[.,]\d$""")
    private val plainDigits = Regex("""^\d{3,7}$""")
    private val badContext = Regex("""(km/h|kmh|mph|rpm|x1000|°|:|%|l/100|mpg|trip|range)""", RegexOption.IGNORE_CASE)
    private val odoContext = Regex("""(odo|total|km|mi\b|miles)""", RegexOption.IGNORE_CASE)

    fun normalize(raw: String): String? {
        var t = raw.trim()
        if (t.isEmpty()) return null
        val digitCount = t.count { it.isDigit() }
        // Fix common OCR confusions only when the token is mostly digits.
        if (digitCount >= 2 && digitCount >= t.count { it.isLetterOrDigit() } * 0.6) {
            t = t.map {
                when (it) {
                    'O', 'o', 'D', 'Q' -> '0'
                    'I', 'l', '|', 'i', '!' -> '1'
                    'S', 's' -> '5'
                    'B' -> '8'
                    'Z', 'z' -> '2'
                    'G' -> '6'
                    else -> it
                }
            }.joinToString("")
        }
        t = t.trim('.', ',', '-', ' ')
        return when {
            thousandsGrouped.matches(t) -> t.filter { it.isDigit() }
            tripLike.matches(t) -> null
            else -> t.filter { it.isDigit() || it == ' ' }
                .split(' ')
                .filter { it.isNotEmpty() }
                .let { parts ->
                    // "048 215" → one number; otherwise keep the longest group.
                    if (parts.size in 2..3 && parts.drop(1).all { it.length == 3 } && parts.first().length <= 3) parts.joinToString("")
                    else parts.maxByOrNull { it.length }
                }
                ?.takeIf { plainDigits.matches(it) }
        }
    }

    fun candidates(
        tokens: List<OcrToken>,
        frameWidth: Int,
        frameHeight: Int,
        lastReading: Int?,
    ): List<OdometerCandidate> {
        if (tokens.isEmpty()) return emptyList()
        val maxHeight = tokens.maxOf { it.height }.coerceAtLeast(1f)
        val cx = frameWidth / 2f
        val cy = frameHeight / 2f
        val result = mutableListOf<OdometerCandidate>()

        for (token in tokens) {
            if (badContext.containsMatchIn(token.text)) continue
            val digits = normalize(token.text) ?: continue
            val value = digits.toIntOrNull() ?: continue
            if (value <= 0) continue

            var score = 0f
            // Larger text is more likely to be the main number in the scan window.
            score += (token.height / maxHeight) * 2f
            // Closer to the centre of the scan frame is better.
            val dx = abs(token.centerX - cx) / (frameWidth / 2f).coerceAtLeast(1f)
            val dy = abs(token.centerY - cy) / (frameHeight / 2f).coerceAtLeast(1f)
            score += (1f - ((dx + dy) / 2f).coerceIn(0f, 1f)) * 2f
            // Odometers usually show 4–6 digits; speeds and rpm show 1–3.
            score += when (digits.length) {
                5, 6 -> 2f
                4, 7 -> 1f
                else -> 0f
            }
            if (digits.length >= 6 && digits.startsWith("0")) score += 0.5f
            if (odoContext.containsMatchIn(token.text)) score += 1f

            var plausible = true
            if (lastReading != null) {
                val diff = value - lastReading
                when {
                    diff in 0..30_000 -> score += 4f - (diff / 30_000f) * 2f
                    diff < 0 && diff > -50 -> { score += 1f; plausible = false }
                    else -> { score -= 4f; plausible = false }
                }
            }
            result += OdometerCandidate(value, score, plausible)
        }
        return result
            .groupBy { it.value }
            .map { (_, list) -> list.maxBy { it.score } }
            .sortedByDescending { it.score }
    }

    fun best(tokens: List<OcrToken>, frameWidth: Int, frameHeight: Int, lastReading: Int?): OdometerCandidate? =
        candidates(tokens, frameWidth, frameHeight, lastReading).firstOrNull()
}
