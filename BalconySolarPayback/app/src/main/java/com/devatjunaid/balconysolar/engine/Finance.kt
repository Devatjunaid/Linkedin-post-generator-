package com.devatjunaid.balconysolar.engine

import kotlin.math.pow

/** Year-by-year cash flow: degradation, electricity price change, feed-in, maintenance and discounting. */
object Finance {

    fun compute(annualKwh: Double, selfUsedKwh: Double, exportedKwh: Double, f: FinanceInputs): FinanceResult {
        val years = f.lifetimeYears.coerceIn(1, 40)
        val d = f.degradationPct / 100.0
        val g = f.priceChangePct / 100.0
        val r = f.discountRatePct / 100.0
        val cumulative = ArrayList<Double>(years + 1)
        cumulative += -f.systemCost
        var cum = -f.systemCost
        var payback: Double? = if (f.systemCost <= 0) 0.0 else null
        var total = 0.0
        var npv = -f.systemCost
        var discountedEnergy = 0.0
        var discountedCost = f.systemCost
        var lifetimeEnergy = 0.0
        var first = 0.0
        for (y in 1..years) {
            val ageing = (1 - d).pow(y - 1)
            val price = f.pricePerKwh * (1 + g).pow(y - 1)
            val saving = selfUsedKwh * ageing * price + exportedKwh * ageing * f.feedInPerKwh - f.maintenancePerYear
            if (y == 1) first = saving
            val before = cum
            cum += saving
            total += saving
            if (payback == null && cum >= 0 && saving > 0) payback = (y - 1) + (-before / saving)
            val disc = (1 + r).pow(y)
            npv += saving / disc
            discountedEnergy += annualKwh * ageing / disc
            discountedCost += f.maintenancePerYear / disc
            lifetimeEnergy += annualKwh * ageing
            cumulative += cum
        }
        return FinanceResult(
            paybackYears = payback,
            firstYearSavings = first,
            lifetimeSavings = total,
            netGain = total - f.systemCost,
            roiMultiple = if (f.systemCost > 0) total / f.systemCost else 0.0,
            npv = npv,
            lcoe = if (discountedEnergy > 0) discountedCost / discountedEnergy else 0.0,
            co2KgPerYear = annualKwh * f.co2GramsPerKwh / 1000.0,
            co2KgLifetime = lifetimeEnergy * f.co2GramsPerKwh / 1000.0,
            cumulative = cumulative,
        )
    }
}
