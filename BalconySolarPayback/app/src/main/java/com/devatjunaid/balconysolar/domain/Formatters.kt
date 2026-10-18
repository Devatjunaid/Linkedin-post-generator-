package com.devatjunaid.balconysolar.domain

import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object Fmt {
    private val locale: Locale get() = Locale.getDefault()

    fun number(value: Double, decimals: Int = 0): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)

    fun money(value: Double, currencyCode: String, decimals: Int? = null): String {
        val f = NumberFormat.getCurrencyInstance(locale)
        val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
        if (currency != null) f.currency = currency
        val digits = decimals ?: when {
            abs(value) >= 1000 -> 0
            else -> currency?.defaultFractionDigits?.coerceAtMost(2) ?: 2
        }
        f.minimumFractionDigits = digits
        f.maximumFractionDigits = digits
        return f.format(value)
    }

    /** Unit price such as an electricity tariff, with enough decimals to be meaningful. */
    fun unitPrice(value: Double, currencyCode: String): String {
        val decimals = when {
            value >= 100 -> 0
            value >= 10 -> 1
            else -> 2
        }
        return money(value, currencyCode, decimals)
    }

    fun currencySymbol(code: String): String =
        runCatching { Currency.getInstance(code).getSymbol(locale) }.getOrDefault(code)

    fun kwh(value: Double): String = "${number(value)} kWh"

    fun years(value: Double?): String {
        if (value == null) return "Not within lifetime"
        val months = (value * 12).roundToInt()
        val y = months / 12
        val m = months % 12
        return when {
            y == 0 -> "$m months"
            m == 0 -> if (y == 1) "1 year" else "$y years"
            else -> "$y yr $m mo"
        }
    }

    fun paybackDate(years: Double?, from: LocalDate = LocalDate.now()): String? {
        if (years == null) return null
        val date = from.plusDays((years * 365.25).roundToLong())
        return date.format(DateTimeFormatter.ofPattern("MMM yyyy", locale))
    }

    fun time(t: ZonedDateTime?): String = t?.format(DateTimeFormatter.ofPattern("HH:mm", locale)) ?: "—"

    fun date(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun degrees(v: Double, decimals: Int = 0): String = "${number(v, decimals)}°"

    fun compass(azimuth: Double): String {
        val names = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return names[(((azimuth % 360) + 360) % 360 / 45.0).roundToInt() % 8]
    }

    fun compassLong(azimuth: Double): String = when (compass(azimuth)) {
        "N" -> "North"; "NE" -> "North-east"; "E" -> "East"; "SE" -> "South-east"
        "S" -> "South"; "SW" -> "South-west"; "W" -> "West"; else -> "North-west"
    }

    fun coordinate(lat: Double, lon: Double): String {
        val ns = if (lat >= 0) "N" else "S"
        val ew = if (lon >= 0) "E" else "W"
        return "${number(abs(lat), 2)}° $ns · ${number(abs(lon), 2)}° $ew"
    }

    /** Parses user input with either decimal separator. */
    fun parse(text: String): Double? = text.trim().replace(" ", "").replace(',', '.').toDoubleOrNull()

    val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
}
