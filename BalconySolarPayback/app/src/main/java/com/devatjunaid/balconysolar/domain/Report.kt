package com.devatjunaid.balconysolar.domain

/** Plain-text summary for sharing. */
object Report {
    fun build(s: SetupState, r: Results): String {
        val cur = s.finance.currencyCode
        val f = r.finance
        val y = r.yield
        val c = r.consumption
        return buildString {
            appendLine("Balcony solar estimate · ${s.placeLabel}")
            appendLine("${s.pv.panelCount} × ${Fmt.number(s.pv.panelWp)} Wp, tilt ${Fmt.degrees(s.pv.tiltDeg)}, facing ${Fmt.compassLong(s.pv.azimuthDeg)} (${Fmt.degrees(s.pv.azimuthDeg)}), inverter ${Fmt.number(s.pv.inverterAcW)} W")
            appendLine()
            appendLine("Yield: ${Fmt.kwh(y.annualKwh)} per year (${Fmt.number(y.specificYield)} kWh/kWp)")
            appendLine("Used at home: ${Fmt.kwh(c.selfUsedKwh)} (${Fmt.number(c.selfConsumptionPct)} %)")
            appendLine("Savings in year one: ${Fmt.money(f.firstYearSavings, cur)}")
            appendLine("Payback: ${Fmt.years(f.paybackYears)}" + (Fmt.paybackDate(f.paybackYears)?.let { " (around $it)" } ?: ""))
            appendLine("Net gain over ${s.finance.lifetimeYears} years: ${Fmt.money(f.netGain, cur)}")
            appendLine("CO₂ avoided: ${Fmt.number(f.co2KgPerYear)} kg per year")
            appendLine()
            appendLine("Assumptions: cost ${Fmt.money(s.finance.systemCost, cur)}, electricity ${Fmt.unitPrice(s.finance.pricePerKwh, cur)}/kWh, +${Fmt.number(s.finance.priceChangePct, 1)} %/yr, ageing ${Fmt.number(s.finance.degradationPct, 1)} %/yr.")
            appendLine("Climate: ${r.site.climate.source}. Estimate for planning only.")
            append("Calculated with Balcony Solar Payback.")
        }
    }
}
