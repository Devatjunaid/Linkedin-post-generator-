package com.voltora.evcalculator.domain

import com.voltora.evcalculator.data.Country
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

const val KM_PER_MILE = 1.609344

class Formatter(val country: Country, val miles: Boolean) {
    private val symbols = DecimalFormatSymbols(Locale.US)

    private fun pattern(decimals: Int) =
        DecimalFormat(if (decimals <= 0) "#,##0" else "#,##0." + "0".repeat(decimals), symbols)

    private fun withSymbol(number: String): String {
        val s = country.symbol
        return if (s.last().isLetter()) "$s $number" else "$s$number"
    }

    fun money(v: Double): String {
        val sign = if (v < 0) "-" else ""
        return sign + withSymbol(pattern(country.moneyDecimals).format(abs(v)))
    }

    /** Price per unit: keeps a third decimal for prices like £0.265. */
    fun rate(v: Double): String {
        val d = when {
            country.moneyDecimals == 0 && v >= 10 -> 0
            v < 1 && (v * 1000).roundToInt() % 10 != 0 -> 3
            else -> 2
        }
        return withSymbol(pattern(d).format(v))
    }

    val distanceUnit: String get() = if (miles) "mi" else "km"

    fun distanceValue(km: Double): Double = if (miles) km / KM_PER_MILE else km

    fun distance(km: Double): String = "${pattern(0).format(distanceValue(km))} $distanceUnit"

    fun toKm(value: Double): Double = if (miles) value * KM_PER_MILE else value

    fun kwh(v: Double): String = "${pattern(1).format(v)} kWh"

    fun consumption(kwhPer100Km: Double): String =
        if (miles) "${pattern(1).format(kwhPer100Km * KM_PER_MILE)} kWh/100 mi"
        else "${pattern(1).format(kwhPer100Km)} kWh/100 km"

    fun percent(soc: Double): String = "${(soc * 100).roundToInt()}%"

    fun power(kw: Double): String =
        if (kw % 1.0 == 0.0) "${kw.toInt()} kW" else "${DecimalFormat("0.#", symbols).format(kw)} kW"

    fun per100Label(): String = if (miles) "Per 100 mi" else "Per 100 km"

    fun per100(costPer100Km: Double): String = money(if (miles) costPer100Km * KM_PER_MILE else costPer100Km)

    companion object {
        fun duration(minutes: Double): String {
            val m = minutes.roundToInt()
            return when {
                m < 60 -> "$m min"
                m % 60 == 0 -> "${m / 60}h"
                else -> "${m / 60}h ${m % 60}m"
            }
        }

        fun number(v: Double, decimals: Int): String =
            DecimalFormat(if (decimals <= 0) "0" else "0." + "#".repeat(decimals), DecimalFormatSymbols(Locale.US)).format(v)
    }
}
