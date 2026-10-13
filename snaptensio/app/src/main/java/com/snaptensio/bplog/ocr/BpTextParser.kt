package com.snaptensio.bplog.ocr

/**
 * A number-like token found by text recognition, with the geometry of its bounding box.
 * Kept free of Android/ML Kit types so the parsing logic is unit-testable on the JVM.
 */
data class NumberToken(val raw: String, val top: Int, val bottom: Int, val left: Int, val right: Int) {
    val height get() = bottom - top
    val centerY get() = (top + bottom) / 2
}

data class BpResult(val systolic: Int, val diastolic: Int, val pulse: Int?) {
    override fun toString() = "$systolic/$diastolic" + (pulse?.let { " · $it" } ?: "")
}

object BpTextParser {

    /** Characters ML Kit commonly returns for 7-segment LCD digits. */
    private val confusions = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', '|' to '1', 'i' to '1', '!' to '1',
        'Z' to '2', 'z' to '2',
        'E' to '3',
        'A' to '4',
        'S' to '5', 's' to '5',
        'G' to '6', 'b' to '6',
        'T' to '7', '?' to '7',
        'B' to '8',
        'g' to '9', 'q' to '9',
    )

    /** Normalises a token to 2–3 digits, or null when it isn't a plausible reading. */
    fun normalize(raw: String): Int? {
        // Times (12:30), dates (10/13) and decimals are never BP values.
        if (raw.any { it == ':' || it == '/' || it == '.' || it == ',' || it == '-' }) return null
        val mapped = raw.trim().map { c -> if (c.isDigit()) c else confusions[c] ?: return null }.joinToString("")
        if (mapped.length !in 2..3) return null
        if (mapped.length == 3 && mapped[0] == '0') return null
        return mapped.toIntOrNull()
    }

    /**
     * Picks systolic, diastolic and pulse from all tokens on screen.
     * BP monitors print SYS largest on top, DIA below it, and pulse smallest at the bottom,
     * so we keep the tallest plausible numbers and read them top to bottom.
     */
    fun parse(tokens: List<NumberToken>): BpResult? {
        val values = tokens.mapNotNull { t -> normalize(t.raw)?.let { t to it } }
        if (values.size < 2) return null
        val tallest = values.maxOf { it.first.height }.coerceAtLeast(1)
        // Ignore small print (clock, memory number, unit labels).
        val big = values.filter { it.first.height >= tallest * 0.45 }
            .sortedByDescending { it.first.height }
            .take(4)
            .sortedBy { it.first.centerY }
        if (big.size < 2) return null

        // Try top-to-bottom first, then every ordered combination, keep the first plausible one.
        for (i in big.indices) for (j in big.indices) {
            if (j <= i) continue
            val sys = big[i].second
            val dia = big[j].second
            if (!plausible(sys, dia)) continue
            val pulse = big.drop(j + 1).map { it.second }.firstOrNull { it in 35..200 }
            return BpResult(sys, dia, pulse)
        }
        return null
    }

    private fun plausible(sys: Int, dia: Int) =
        sys in 70..260 && dia in 35..160 && sys - dia >= 10 && sys - dia <= 120
}

/**
 * Accepts a live result only after the same numbers were read in several recent frames,
 * which filters out flicker and half-drawn LCD segments.
 */
class ConsensusTracker(private val window: Int = 6, private val required: Int = 3) {
    private val recent = ArrayDeque<BpResult>()

    fun add(result: BpResult?): BpResult? {
        if (result != null) {
            recent.addLast(result)
            while (recent.size > window) recent.removeFirst()
        }
        return best()
    }

    /** The most frequent result if it reached the required agreement. */
    fun best(): BpResult? {
        val counts = recent.groupingBy { it.systolic to it.diastolic }.eachCount()
        val (key, count) = counts.maxByOrNull { it.value } ?: return null
        if (count < required) return null
        val pulses = recent.filter { (it.systolic to it.diastolic) == key }.mapNotNull { it.pulse }
        val pulse = pulses.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        return BpResult(key.first, key.second, pulse)
    }

    fun agreement(): Int = recent.groupingBy { it.systolic to it.diastolic }.eachCount().values.maxOrNull() ?: 0

    fun reset() = recent.clear()
}
