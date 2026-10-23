package com.superdevlabs.petfood.portioncalc.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

enum class WeightUnit(val label: String) { KG("kg"), LB("lb") }

object Format {
    private const val LB_PER_KG = 2.20462262

    fun kgToUnit(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) kg else kg * LB_PER_KG

    fun unitToKg(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) value else value / LB_PER_KG

    fun weight(kg: Double, unit: WeightUnit): String = "${oneDecimal(kgToUnit(kg, unit))} ${unit.label}"

    fun oneDecimal(v: Double): String {
        val s = String.format(Locale.getDefault(), "%.1f", v)
        return if (s.endsWith("0") && s.length > 2 && !s[s.length - 2].isDigit()) s.dropLast(2) else s
    }

    fun grams(g: Double): String = if (g >= 1000) "${oneDecimal(g / 1000)} kg" else "${g.roundToInt()} g"

    fun kcal(k: Int): String = String.format(Locale.getDefault(), "%,d kcal", k)

    fun ml(ml: Int): String = if (ml >= 1000) "${oneDecimal(ml / 1000.0)} L" else "$ml ml"

    /** Kitchen-friendly cups rounded to the nearest 1/8, e.g. "1 ¾ cups". */
    fun cups(cups: Double): String {
        val eighths = (cups * 8).roundToInt()
        if (eighths == 0) return "less than ⅛ cup"
        val whole = eighths / 8
        val frac = when (eighths % 8) {
            1 -> "⅛"; 2 -> "¼"; 3 -> "⅜"; 4 -> "½"; 5 -> "⅝"; 6 -> "¾"; 7 -> "⅞"; else -> ""
        }
        val number = when {
            whole == 0 -> frac
            frac.isEmpty() -> "$whole"
            else -> "$whole $frac"
        }
        return if (eighths <= 8) "$number cup" else "$number cups"
    }

    /** Units such as cans or pouches, rounded to the nearest quarter. */
    fun units(units: Double, name: String): String {
        val quarters = (units * 4).roundToInt()
        val whole = quarters / 4
        val frac = when (quarters % 4) { 1 -> "¼"; 2 -> "½"; 3 -> "¾"; else -> "" }
        val number = when {
            quarters == 0 -> "less than ¼"
            whole == 0 -> frac
            frac.isEmpty() -> "$whole"
            else -> "$whole $frac"
        }
        val plural = if (quarters > 4 && !name.endsWith("s")) name + "s" else name
        return "$number $plural"
    }

    fun age(months: Int): String {
        val y = months / 12
        val m = months % 12
        return when {
            y == 0 -> "$m mo"
            m == 0 -> if (y == 1) "1 yr" else "$y yrs"
            else -> "$y y $m m"
        }
    }

    fun signedWeight(kg: Double, unit: WeightUnit): String {
        val v = kgToUnit(kg, unit)
        val sign = if (v > 0) "+" else if (v < 0) "−" else ""
        return "$sign${oneDecimal(abs(v))} ${unit.label}"
    }

    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun wholePart(v: Double): Int = floor(v).toInt()
}
