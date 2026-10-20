package com.superdevlabs.coldplunge.domain

import kotlin.math.abs
import kotlin.math.roundToInt

object Units {
    fun cToF(c: Float): Float = c * 9f / 5f + 32f

    fun temp(c: Float?, fahrenheit: Boolean): String {
        if (c == null) return "—"
        return if (fahrenheit) "${cToF(c).roundToInt()}°F" else "${trim(c)}°C"
    }

    /** Value only, no unit, for compact displays. */
    fun tempValue(c: Float, fahrenheit: Boolean): String =
        if (fahrenheit) cToF(c).roundToInt().toString() else trim(c)

    fun unit(fahrenheit: Boolean): String = if (fahrenheit) "°F" else "°C"

    private fun trim(c: Float): String {
        val rounded = (c * 10f).roundToInt() / 10f
        return if (abs(rounded - rounded.roundToInt()) < 0.05f) rounded.roundToInt().toString() else rounded.toString()
    }

    /** 75 -> "1:15", 3725 -> "1:02:05". */
    fun clock(totalSec: Int): String {
        val s = totalSec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    /** Compact duration: under an hour -> "48m" or "2m 30s"; otherwise "1h 05m". */
    fun duration(totalSec: Int): String {
        val s = totalSec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return when {
            h > 0 -> "%dh %02dm".format(h, m)
            m == 0 -> "${sec}s"
            sec == 0 -> "${m}m"
            else -> "${m}m ${sec}s"
        }
    }
}
