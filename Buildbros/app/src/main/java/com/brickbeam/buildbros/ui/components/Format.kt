package com.brickbeam.buildbros.ui.components

import com.brickbeam.buildbros.calc.Conv
import com.brickbeam.buildbros.calc.LengthFormat
import com.brickbeam.buildbros.calc.Region
import com.brickbeam.buildbros.calc.UnitSystem
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formats measurements in the user's unit system and money in the region's currency. */
class Fmt(val system: UnitSystem, val region: Region) {
    private val locale: Locale = Locale.forLanguageTag(region.localeTag)
    private val money: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(region.currency)
        val d = region.zone.decimals
        minimumFractionDigits = d
        maximumFractionDigits = d
    }

    val imperial: Boolean get() = system == UnitSystem.IMPERIAL

    fun money(v: Double): String = money.format(v)
    fun moneyPrecise(v: Double): String = NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(region.currency)
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(v)

    fun num(v: Double, decimals: Int = 2): String = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = decimals
    }.format(v)

    fun int(v: Int): String = NumberFormat.getIntegerInstance(locale).format(v)

    fun length(m: Double) = LengthFormat.big(m, system)
    fun small(mm: Double) = LengthFormat.small(mm, system)

    val lengthUnit get() = if (imperial) "ft" else "m"
    val smallUnit get() = if (imperial) "in" else "mm"
    val areaUnit get() = if (imperial) "sq ft" else "m²"
    val volumeUnit get() = if (imperial) "yd³" else "m³"

    fun area(m2: Double) = if (imperial) "${num(Conv.m2ToFt2(m2), 1)} sq ft" else "${num(m2, 2)} m²"
    fun volume(m3: Double) = if (imperial) "${num(Conv.m3ToYd3(m3), 2)} yd³" else "${num(m3, 2)} m³"
    fun volumeFt3(m3: Double) = if (imperial) "${num(Conv.m3ToFt3(m3), 1)} ft³" else "${num(m3 * 1000, 0)} L"
    fun weight(kg: Double) = if (imperial) "${num(Conv.kgToLb(kg), 0)} lb" else "${num(kg, 1)} kg"
    fun bulk(kg: Double) = if (imperial) "${num(kg / Conv.KG_PER_SHORT_TON, 2)} ton" else "${num(kg / 1000, 2)} t"
    fun litres(l: Double) = if (imperial) "${num(l / Conv.L_PER_GAL, 2)} gal" else "${num(l, 2)} L"
    fun coverageUnit() = if (imperial) "sq ft/gal" else "m²/L"

    /** Area input helper (m² <-> user's unit). */
    fun areaFromUser(v: Double) = if (imperial) v * Conv.M2_PER_FT2 else v
    fun areaToUser(m2: Double) = if (imperial) Conv.m2ToFt2(m2) else m2
}
