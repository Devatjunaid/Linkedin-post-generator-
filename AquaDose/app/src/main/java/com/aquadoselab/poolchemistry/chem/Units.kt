package com.aquadoselab.poolchemistry.chem

import com.aquadoselab.poolchemistry.data.TempUnit
import com.aquadoselab.poolchemistry.data.UnitSystem
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToInt

/** An amount of product in base units: millilitres or grams. */
sealed class Quantity {
    data class Volume(val ml: Double) : Quantity()
    data class Mass(val grams: Double) : Quantity()
}

data class FormattedAmount(val main: String, val secondary: String?)

object Units {
    private const val ML_PER_FL_OZ = 29.5735
    private const val G_PER_OZ = 28.3495
    private const val G_PER_LB = 453.592
    private val one = DecimalFormat("#,##0.#")
    private val zero = DecimalFormat("#,##0")
    private val two = DecimalFormat("#,##0.##")

    fun num(v: Double): String = when {
        abs(v) >= 100 -> zero.format(v)
        else -> one.format(v)
    }

    fun ppm(v: Double): String = if (abs(v) >= 100) zero.format(v) else one.format(v)

    fun ph(v: Double): String = DecimalFormat("0.0").format(v)

    fun signed(v: Double): String = (if (v > 0) "+" else "") + two.format(v)

    fun amount(q: Quantity, units: UnitSystem): FormattedAmount = when (q) {
        is Quantity.Volume -> volume(q.ml, units)
        is Quantity.Mass -> mass(q.grams, units)
    }

    private fun volume(ml: Double, units: UnitSystem): FormattedAmount {
        if (units == UnitSystem.METRIC) {
            return if (ml < 1000) FormattedAmount("${zero.format(ml)} mL", null)
            else FormattedAmount("${two.format(ml / 1000)} L", null)
        }
        val flOz = ml / ML_PER_FL_OZ
        return when {
            flOz < 2 -> FormattedAmount("${one.format(flOz)} fl oz", "≈ ${one.format(flOz * 2)} tbsp")
            flOz < 128 -> FormattedAmount("${one.format(flOz)} fl oz", "≈ ${cups(flOz / 8)} cups")
            else -> FormattedAmount("${two.format(flOz / 128)} gal", "${zero.format(flOz)} fl oz")
        }
    }

    private fun cups(c: Double): String {
        val quarters = (c * 4).roundToInt()
        val whole = quarters / 4
        val frac = when (quarters % 4) { 1 -> "¼"; 2 -> "½"; 3 -> "¾"; else -> "" }
        return when {
            whole == 0 && frac.isEmpty() -> "¼"
            whole == 0 -> frac
            else -> "$whole$frac"
        }
    }

    private fun mass(g: Double, units: UnitSystem): FormattedAmount {
        if (units == UnitSystem.METRIC) {
            return if (g < 1000) FormattedAmount("${zero.format(g)} g", null)
            else FormattedAmount("${two.format(g / 1000)} kg", null)
        }
        val oz = g / G_PER_OZ
        return if (oz < 16) FormattedAmount("${one.format(oz)} oz", "by weight")
        else FormattedAmount("${two.format(g / G_PER_LB)} lb", "${zero.format(oz)} oz by weight")
    }

    fun gallonsToDisplay(gallons: Double, units: UnitSystem): String =
        if (units == UnitSystem.METRIC) "${zero.format(gallons * LITERS_PER_GALLON)} L"
        else "${zero.format(gallons)} gal"

    fun volumeUnit(units: UnitSystem) = if (units == UnitSystem.METRIC) "L" else "gal"
    fun lengthUnit(units: UnitSystem) = if (units == UnitSystem.METRIC) "m" else "ft"

    fun tempDisplay(tempF: Double, unit: TempUnit): String =
        if (unit == TempUnit.C) "${((tempF - 32) * 5 / 9).roundToInt()}°C" else "${tempF.roundToInt()}°F"

    fun fToUnit(tempF: Double, unit: TempUnit) = if (unit == TempUnit.C) (tempF - 32) * 5 / 9 else tempF
    fun unitToF(value: Double, unit: TempUnit) = if (unit == TempUnit.C) value * 9 / 5 + 32 else value
}
