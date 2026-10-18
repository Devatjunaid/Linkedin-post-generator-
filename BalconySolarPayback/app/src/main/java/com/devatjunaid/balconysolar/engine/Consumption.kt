package com.devatjunaid.balconysolar.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Self-consumption from overlapping the PV day profiles (sunny / average / cloudy, per month)
 * with an hourly household load profile, with an optional battery simulated step by step.
 */
object Consumption {

    private const val BATTERY_USABLE = 0.9
    private const val BATTERY_ONE_WAY_EFFICIENCY = 0.95
    private const val BATTERY_POWER_W = 800.0
    /** Share of the mean load that is always on (fridge, router, standby). */
    private const val BASE_LOAD_SHARE = 0.35

    /**
     * Expected PV power used directly when the instantaneous load fluctuates around its mean:
     * load = always-on base + an exponentially distributed share (appliances switching on and off).
     * E[min(P, L)] then has a closed form; it is lower than min(P, mean) because PV is wasted
     * during low-load moments even when the average load exceeds the PV output.
     */
    internal fun expectedDirectUse(pv: Double, meanLoad: Double): Double {
        if (pv <= 0.0 || meanLoad <= 0.0) return 0.0
        val base = BASE_LOAD_SHARE * meanLoad
        if (pv <= base) return pv
        val variable = meanLoad - base
        return base + variable * (1 - kotlin.math.exp(-(pv - base) / variable))
    }

    fun compute(y: YieldResult, h: Household, southernHemisphere: Boolean): ConsumptionResult {
        val slots = y.slotsPerDay
        val dtH = y.stepMinutes / 60.0
        val factors = h.season.factors(southernHemisphere)
        val weightedDays = (0 until 12).sumOf { factors[it] * DAYS_IN_MONTH[it] }
        val shares = h.profile.shares
        val loadDay = List(12) { m ->
            val dailyKwh = h.annualKwh * factors[m] / weightedDays
            DoubleArray(slots) { s -> dailyKwh * 1000.0 * shares[(s * y.stepMinutes) / 60] }
        }

        val monthlySelf = DoubleArray(12)
        var direct = 0.0
        var charged = 0.0
        var delivered = 0.0
        val capacityWh = h.batteryKwh.coerceAtLeast(0.0) * 1000.0 * BATTERY_USABLE
        for (m in 0 until 12) {
            val load = loadDay[m]
            for (c in 0 until 3) {
                val pv = y.classDayW[m][c]
                var dayDirect = 0.0
                var dayCharged = 0.0
                var dayDelivered = 0.0
                var soc = 0.0
                // Repeat the day so the battery starts each day in its steady state.
                val repeats = if (capacityWh > 0) 3 else 1
                repeat(repeats) { r ->
                    val last = r == repeats - 1
                    for (s in 0 until slots) {
                        val d = expectedDirectUse(pv[s], load[s])
                        var ch = 0.0
                        var dis = 0.0
                        if (capacityWh > 0) {
                            val surplus = pv[s] - d
                            val deficit = load[s] - d
                            ch = minOf(surplus, BATTERY_POWER_W, max(0.0, capacityWh - soc) / BATTERY_ONE_WAY_EFFICIENCY / dtH)
                            soc += ch * BATTERY_ONE_WAY_EFFICIENCY * dtH
                            dis = minOf(deficit, BATTERY_POWER_W, soc * BATTERY_ONE_WAY_EFFICIENCY / dtH)
                            soc -= dis / BATTERY_ONE_WAY_EFFICIENCY * dtH
                        }
                        if (last) {
                            dayDirect += d * dtH
                            dayCharged += ch * dtH
                            dayDelivered += dis * dtH
                        }
                    }
                }
                val scale = DAYS_IN_MONTH[m] / 3.0 / 1000.0
                direct += dayDirect * scale
                charged += dayCharged * scale
                delivered += dayDelivered * scale
                monthlySelf[m] += (dayDirect + dayDelivered) * scale
            }
        }

        val annual = y.annualKwh
        val override = h.selfConsumptionOverridePct
        val selfUsed: Double
        val exported: Double
        if (override != null) {
            selfUsed = annual * override.coerceIn(0.0, 100.0) / 100.0
            exported = annual - selfUsed
        } else {
            selfUsed = direct + delivered
            exported = max(0.0, annual - direct - charged)
        }
        val monthly = if (override != null) {
            y.monthlyKwh.map { it * override.coerceIn(0.0, 100.0) / 100.0 }
        } else monthlySelf.toList()
        return ConsumptionResult(
            selfUsedKwh = selfUsed,
            exportedKwh = exported,
            selfConsumptionPct = if (annual > 0) selfUsed / annual * 100 else 0.0,
            autarkyPct = if (h.annualKwh > 0) min(100.0, selfUsed / h.annualKwh * 100) else 0.0,
            monthlySelfUsedKwh = monthly,
            batteryDeliveredKwh = if (override != null) 0.0 else delivered,
            loadDayW = loadDay,
            calculated = override == null,
        )
    }
}
