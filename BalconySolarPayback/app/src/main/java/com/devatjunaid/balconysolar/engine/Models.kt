package com.devatjunaid.balconysolar.engine

/** Long-term monthly climate for one place. */
data class MonthlyClimate(
    /** Mean daily global horizontal irradiation per month, kWh/m²/day (12 values). */
    val ghiDaily: List<Double>,
    /** Mean air temperature per month, °C (12 values). */
    val tempMean: List<Double>,
    /** Typical difference between daily maximum and minimum temperature, °C. */
    val diurnalRange: Double = 9.0,
    val source: String,
) {
    init {
        require(ghiDaily.size == 12 && tempMean.size == 12) { "Climate needs 12 monthly values" }
    }

    fun annualGhi(): Double = ghiDaily.indices.sumOf { ghiDaily[it] * DAYS_IN_MONTH[it] }
}

data class Site(
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val climate: MonthlyClimate,
)

enum class Mounting(val label: String, val short: String, val heatLossU: Double) {
    // PVsyst constant heat-loss factors, W/m²K.
    STAND("Free stand / open rack", "Free stand", 29.0),
    RAILING("On balcony railing", "Railing", 22.0),
    WALL("Flat on the wall", "On wall", 15.0),
}

data class PvSystem(
    val panelCount: Int = 2,
    val panelWp: Double = 430.0,
    /** 0° = flat, 90° = vertical railing. */
    val tiltDeg: Double = 60.0,
    /** Direction the panel faces, clockwise from north: 180 = south. */
    val azimuthDeg: Double = 180.0,
    val inverterAcW: Double = 800.0,
    val inverterEfficiency: Double = 0.96,
    /** Power temperature coefficient, %/°C (negative). */
    val tempCoeffPct: Double = -0.34,
    val moduleEfficiency: Double = 0.215,
    val mounting: Mounting = Mounting.RAILING,
    val shadingPct: Double = 0.0,
    val soilingPct: Double = 2.0,
    val cableMismatchPct: Double = 2.0,
    val albedo: Double = 0.20,
) {
    val peakW: Double get() = panelCount * panelWp
}

data class LossStep(val label: String, val lossKwh: Double, val lossPct: Double)

data class YieldResult(
    val annualKwh: Double,
    val monthlyKwh: List<Double>,
    /** kWh per kWp per year. */
    val specificYield: Double,
    val ghiAnnualKwhM2: Double,
    /** Irradiation on the panel plane, kWh/m² per year. */
    val poaAnnualKwhM2: Double,
    /** Energy at STC efficiency before any loss, kWh. */
    val nominalKwh: Double,
    val losses: List<LossStep>,
    val peakAcW: Double,
    /** Mean AC power per time slot of a typical day, per month: [month][slot] in W. */
    val typicalDayW: List<DoubleArray>,
    /** Same as [typicalDayW] split into sunny / average / cloudy day classes: [month][class][slot]. */
    val classDayW: List<List<DoubleArray>>,
    val stepMinutes: Int,
) {
    val slotsPerDay: Int get() = 1440 / stepMinutes
}

enum class LoadProfile(val label: String, internal val hourly: DoubleArray) {
    STANDARD("Typical household", doubleArrayOf(
        0.0302, 0.0247, 0.0219, 0.0206, 0.0204, 0.0222, 0.0295, 0.0398, 0.0447, 0.0456, 0.0459, 0.0478,
        0.0510, 0.0489, 0.0449, 0.0420, 0.0423, 0.0466, 0.0542, 0.0589, 0.0575, 0.0532, 0.0471, 0.0388,
    )),
    AWAY_DAYTIME("Out at work during the day", doubleArrayOf(
        0.031, 0.026, 0.023, 0.022, 0.022, 0.026, 0.042, 0.055, 0.045, 0.030, 0.027, 0.027,
        0.029, 0.028, 0.027, 0.029, 0.036, 0.053, 0.068, 0.072, 0.068, 0.060, 0.050, 0.040,
    )),
    HOME_OFFICE("Home office", doubleArrayOf(
        0.028, 0.023, 0.021, 0.020, 0.020, 0.023, 0.032, 0.044, 0.050, 0.052, 0.052, 0.054,
        0.058, 0.054, 0.051, 0.048, 0.047, 0.050, 0.056, 0.058, 0.054, 0.048, 0.042, 0.035,
    )),
    HOME_ALL_DAY("Home all day / retired", doubleArrayOf(
        0.027, 0.022, 0.020, 0.019, 0.019, 0.022, 0.030, 0.045, 0.055, 0.056, 0.057, 0.062,
        0.066, 0.057, 0.050, 0.047, 0.047, 0.052, 0.058, 0.058, 0.052, 0.045, 0.039, 0.032,
    )),
    ;

    /** Share of daily consumption in each hour, normalised to sum to 1. */
    val shares: DoubleArray by lazy { hourly.sum().let { s -> DoubleArray(24) { hourly[it] / s } } }
}

/** Seasonal shape of household demand. */
enum class LoadSeason(internal val northern: DoubleArray) {
    HEATING(doubleArrayOf(1.12, 1.06, 1.02, 0.97, 0.93, 0.89, 0.88, 0.90, 0.95, 1.01, 1.09, 1.18)),
    COOLING(doubleArrayOf(0.80, 0.80, 0.90, 1.00, 1.15, 1.25, 1.25, 1.22, 1.12, 0.98, 0.85, 0.80)),
    FLAT(DoubleArray(12) { 1.0 }),
    ;

    fun factors(southernHemisphere: Boolean): DoubleArray =
        if (southernHemisphere) DoubleArray(12) { northern[(it + 6) % 12] } else northern
}

data class Household(
    val annualKwh: Double = 2500.0,
    val profile: LoadProfile = LoadProfile.STANDARD,
    val season: LoadSeason = LoadSeason.HEATING,
    val batteryKwh: Double = 0.0,
    /** When set, replaces the calculated self-consumption share (0–100). */
    val selfConsumptionOverridePct: Double? = null,
)

data class ConsumptionResult(
    val selfUsedKwh: Double,
    val exportedKwh: Double,
    val selfConsumptionPct: Double,
    /** Share of the household's annual demand covered by the panels. */
    val autarkyPct: Double,
    val monthlySelfUsedKwh: List<Double>,
    val batteryDeliveredKwh: Double,
    /** Typical-day household load per month, W per slot. */
    val loadDayW: List<DoubleArray>,
    val calculated: Boolean,
)

data class FinanceInputs(
    val systemCost: Double = 499.0,
    val pricePerKwh: Double = 0.39,
    val priceChangePct: Double = 2.0,
    val feedInPerKwh: Double = 0.0,
    val degradationPct: Double = 0.5,
    val lifetimeYears: Int = 20,
    val discountRatePct: Double = 0.0,
    val maintenancePerYear: Double = 0.0,
    val co2GramsPerKwh: Double = 360.0,
    val currencyCode: String = "EUR",
)

data class FinanceResult(
    /** Fractional years until savings cover the cost; null when not reached within the lifetime. */
    val paybackYears: Double?,
    val firstYearSavings: Double,
    val lifetimeSavings: Double,
    val netGain: Double,
    val roiMultiple: Double,
    val npv: Double,
    /** Levelised cost of the energy the panels produce, per kWh. */
    val lcoe: Double,
    val co2KgPerYear: Double,
    val co2KgLifetime: Double,
    /** Cumulative net savings at the end of each year, index 0 = purchase (−cost). */
    val cumulative: List<Double>,
)

internal val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
