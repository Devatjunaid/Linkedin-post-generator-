package app.layerline.toolkit3d.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/** Formats amounts the way the selected region writes them, e.g. "Rs 1,180" or "€12.40". */
data class Money(val symbol: String, val decimals: Int) {

    fun format(value: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
            isGroupingUsed = true
        }
        val sign = if (value < 0) "-" else ""
        return sign + symbol + nf.format(kotlin.math.abs(value))
    }

    /** Short form for tight spaces: 12.4k instead of 12,400. */
    fun compact(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000 -> symbol + String.format(Locale.US, "%.1fM", value / 1_000_000)
            abs >= 100_000 -> symbol + String.format(Locale.US, "%.0fk", value / 1000)
            else -> format(value)
        }
    }
}

object Fmt {
    fun num(value: Double, decimals: Int): String = String.format(Locale.US, "%.${decimals}f", value)

    fun grams(value: Double): String =
        if (value >= 1000) String.format(Locale.US, "%.2f kg", value / 1000) else "${value.roundToLong()} g"

    fun duration(minutes: Double): String {
        val total = minutes.roundToLong().coerceAtLeast(0)
        val h = total / 60
        val m = total % 60
        return when {
            h == 0L -> "${m}m"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    fun trim(value: Double, maxDecimals: Int = 2): String {
        val s = String.format(Locale.US, "%.${maxDecimals}f", value)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }
}
