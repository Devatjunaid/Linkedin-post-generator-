package com.odomate.app.util

import com.odomate.app.domain.StatusCalculator
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

object Formats {
    private val intFormat: NumberFormat get() = NumberFormat.getIntegerInstance()
    private val mediumDate: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val shortMonthDay: DateTimeFormatter get() = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val monthYear: DateTimeFormatter get() = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun number(value: Int): String = intFormat.format(value)
    fun number(value: Long): String = intFormat.format(value)

    fun distance(value: Int, unit: String): String = "${number(value)} $unit"

    fun date(millis: Long): String = StatusCalculator.toLocalDate(millis).format(mediumDate)
    fun date(date: LocalDate): String = date.format(mediumDate)
    fun isoDate(millis: Long): String = StatusCalculator.toLocalDate(millis).toString()

    /** "Nov 20" for this year, "Nov 20, 2027" otherwise. */
    fun shortDate(millis: Long): String {
        val d = StatusCalculator.toLocalDate(millis)
        return if (d.year == LocalDate.now().year) d.format(shortMonthDay) else d.format(mediumDate)
    }

    fun monthHeader(millis: Long): String = StatusCalculator.toLocalDate(millis).format(monthYear)

    fun money(value: Double, symbol: String): String {
        val nf = NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$symbol${nf.format(value)}"
    }

    fun decimal(value: Double, digits: Int = 1): String {
        val nf = NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 0
            maximumFractionDigits = digits
        }
        return nf.format(value)
    }

    fun days(count: Long): String {
        val n = abs(count)
        return when {
            n >= 60 -> "${n / 30} months"
            n == 1L -> "1 day"
            else -> "$n days"
        }
    }

    /** Parses user-typed numbers like "48,215", "48 215" or "48.215" into an Int. */
    fun parseInt(text: String): Int? = text.filter { it.isDigit() }.takeIf { it.isNotEmpty() && it.length <= 9 }?.toIntOrNull()

    /** Parses decimal input accepting either comma or dot as the decimal separator. */
    fun parseDecimal(text: String): Double? {
        val t = text.trim().replace(" ", "")
        if (t.isEmpty()) return null
        val normalized = if (t.count { it == ',' } == 1 && !t.contains('.')) t.replace(',', '.') else t.replace(",", "")
        return normalized.toDoubleOrNull()
    }
}
