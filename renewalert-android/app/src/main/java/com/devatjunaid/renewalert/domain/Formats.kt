package com.devatjunaid.renewalert.domain

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale

object Formats {
    const val HIDDEN = "••••"

    private fun formatter(currency: String): NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            runCatching { this.currency = Currency.getInstance(currency) }
        }

    /** [minor] is in hundredths of the currency unit. */
    fun money(minor: Double, currency: String, hidden: Boolean = false): String =
        if (hidden) HIDDEN else formatter(currency).format(minor / 100.0)

    fun money(minor: Long, currency: String, hidden: Boolean = false): String =
        money(minor.toDouble(), currency, hidden)

    /** Whole-unit amount without decimals, for charts. */
    fun moneyShort(minor: Double, currency: String): String {
        val f = formatter(currency).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }
        val units = minor / 100.0
        return if (units >= 10_000) f.format(units / 1000.0) + "k" else f.format(units)
    }

    fun symbol(currency: String): String =
        runCatching { Currency.getInstance(currency).getSymbol(Locale.getDefault()) }.getOrDefault(currency)

    /** Parses user input like "12.99" or "12,99" into hundredths. */
    fun parseMinor(text: String): Long? {
        val clean = text.trim().replace(" ", "").replace(',', '.')
        if (clean.isEmpty()) return null
        val parts = clean.split('.')
        val normalized = if (parts.size > 2) parts.dropLast(1).joinToString("") + "." + parts.last() else clean
        val v = normalized.toBigDecimalOrNull() ?: return null
        if (v.signum() < 0) return null
        return v.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    fun minorToInput(minor: Long): String {
        val units = minor / 100
        val cents = minor % 100
        return if (cents == 0L) units.toString() else "%d.%02d".format(Locale.US, units, cents)
    }

    private val shortDate = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val dayMonth = DateTimeFormatter.ofPattern("MMM d")
    private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy")
    private val longMonth = DateTimeFormatter.ofPattern("MMMM yyyy")

    fun shortDate(d: LocalDate): String = d.format(shortDate)
    fun dayMonth(d: LocalDate): String = d.format(dayMonth)
    fun monthYear(d: LocalDate): String = d.format(monthYear)
    fun longMonth(ym: YearMonth): String = ym.format(longMonth)
    fun fullDate(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    fun shortMonth(ym: YearMonth): String = ym.format(DateTimeFormatter.ofPattern("MMM"))

    fun relativeDay(today: LocalDate, d: LocalDate): String {
        val n = ChronoUnit.DAYS.between(today, d)
        return when {
            n == 0L -> "today"
            n == 1L -> "tomorrow"
            n == -1L -> "yesterday"
            n > 1 -> "in $n days"
            else -> "${-n} days ago"
        }
    }
}
