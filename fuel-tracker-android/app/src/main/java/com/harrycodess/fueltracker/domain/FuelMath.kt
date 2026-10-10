package com.harrycodess.fueltracker.domain

/** Units the user enters and reads values in. Everything is stored in metric. */
enum class UnitSystem { METRIC, IMPERIAL }

/** How fuel efficiency is shown when [UnitSystem.METRIC] is active. Imperial always uses mpg. */
enum class EfficiencyFormat { KM_PER_L, L_PER_100KM }

data class UnitPrefs(
    val system: UnitSystem = UnitSystem.METRIC,
    val efficiencyFormat: EfficiencyFormat = EfficiencyFormat.KM_PER_L,
) {
    val isMetric: Boolean get() = system == UnitSystem.METRIC
    val distanceUnit: String get() = if (isMetric) "km" else "mi"
    val volumeUnit: String get() = if (isMetric) "L" else "gal"
    val efficiencyUnit: String
        get() = when {
            !isMetric -> "mpg"
            efficiencyFormat == EfficiencyFormat.KM_PER_L -> "km/L"
            else -> "L/100km"
        }

    fun pricePerVolumeUnit(currency: String): String = "$currency / $volumeUnit"
}

data class TripEstimate(
    val fuelLiters: Double,
    val totalCost: Double,
    val costPerKm: Double,
)

/** Pure conversion and cost helpers. Canonical units: km, litres, km/L, price per litre. */
object FuelMath {
    const val KM_PER_MILE = 1.609344
    const val LITERS_PER_GALLON = 3.785411784 // US gallon

    fun distanceToKm(value: Double, system: UnitSystem): Double =
        if (system == UnitSystem.METRIC) value else value * KM_PER_MILE

    fun kmToDistance(km: Double, system: UnitSystem): Double =
        if (system == UnitSystem.METRIC) km else km / KM_PER_MILE

    fun priceToPerLiter(value: Double, system: UnitSystem): Double =
        if (system == UnitSystem.METRIC) value else value / LITERS_PER_GALLON

    fun perLiterToPrice(pricePerLiter: Double, system: UnitSystem): Double =
        if (system == UnitSystem.METRIC) pricePerLiter else pricePerLiter * LITERS_PER_GALLON

    fun litersToVolume(liters: Double, system: UnitSystem): Double =
        if (system == UnitSystem.METRIC) liters else liters / LITERS_PER_GALLON

    /** Converts an efficiency typed in the user's units to km/L. Returns null for non-positive input. */
    fun efficiencyToKmPerL(value: Double, units: UnitPrefs): Double? {
        if (value <= 0.0) return null
        return when {
            !units.isMetric -> value * KM_PER_MILE / LITERS_PER_GALLON
            units.efficiencyFormat == EfficiencyFormat.KM_PER_L -> value
            else -> 100.0 / value
        }
    }

    /** Converts km/L to the user's efficiency units. */
    fun kmPerLToEfficiency(kmPerL: Double, units: UnitPrefs): Double {
        if (kmPerL <= 0.0) return 0.0
        return when {
            !units.isMetric -> kmPerL * LITERS_PER_GALLON / KM_PER_MILE
            units.efficiencyFormat == EfficiencyFormat.KM_PER_L -> kmPerL
            else -> 100.0 / kmPerL
        }
    }

    /** Cost per km converted to cost per the user's distance unit. */
    fun costPerDistance(costPerKm: Double, system: UnitSystem): Double =
        if (system == UnitSystem.METRIC) costPerKm else costPerKm * KM_PER_MILE

    fun estimate(distanceKm: Double, pricePerLiter: Double, kmPerL: Double): TripEstimate? {
        if (distanceKm <= 0.0 || pricePerLiter <= 0.0 || kmPerL <= 0.0) return null
        val liters = distanceKm / kmPerL
        val cost = liters * pricePerLiter
        return TripEstimate(fuelLiters = liters, totalCost = cost, costPerKm = cost / distanceKm)
    }
}

/** Parses user input, accepting both "." and "," as the decimal separator. */
fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private val decimalInputRegex = Regex("^\\d{0,7}([.,]\\d{0,3})?$")

/** True when [text] is an acceptable in-progress decimal entry (digits plus one separator). */
fun isValidDecimalInput(text: String): Boolean = decimalInputRegex.matches(text)
