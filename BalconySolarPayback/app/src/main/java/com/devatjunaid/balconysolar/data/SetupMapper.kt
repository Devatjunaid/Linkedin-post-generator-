package com.devatjunaid.balconysolar.data

import com.devatjunaid.balconysolar.data.db.SetupEntity
import com.devatjunaid.balconysolar.domain.CustomLocation
import com.devatjunaid.balconysolar.domain.Results
import com.devatjunaid.balconysolar.domain.SetupState
import com.devatjunaid.balconysolar.engine.FinanceInputs
import com.devatjunaid.balconysolar.engine.Household
import com.devatjunaid.balconysolar.engine.LoadProfile
import com.devatjunaid.balconysolar.engine.LoadSeason
import com.devatjunaid.balconysolar.engine.MonthlyClimate
import com.devatjunaid.balconysolar.engine.Mounting
import com.devatjunaid.balconysolar.engine.PvSystem
import java.util.Locale

object SetupMapper {

    fun toEntity(id: Long, name: String, s: SetupState, r: Results?, now: Long): SetupEntity = SetupEntity(
        id = id,
        name = name,
        savedAt = now,
        countryCode = s.countryCode,
        cityName = s.cityName,
        customLabel = s.custom?.label,
        customLat = s.custom?.latitude,
        customLon = s.custom?.longitude,
        customZone = s.custom?.zoneId,
        refinedGhi = s.refinedClimate?.ghiDaily?.let(::join),
        refinedTemp = s.refinedClimate?.tempMean?.let(::join),
        refinedSource = s.refinedClimate?.source,
        panelCount = s.pv.panelCount,
        panelWp = s.pv.panelWp,
        tiltDeg = s.pv.tiltDeg,
        azimuthDeg = s.pv.azimuthDeg,
        inverterAcW = s.pv.inverterAcW,
        inverterEfficiency = s.pv.inverterEfficiency,
        tempCoeffPct = s.pv.tempCoeffPct,
        moduleEfficiency = s.pv.moduleEfficiency,
        mounting = s.pv.mounting.name,
        shadingPct = s.pv.shadingPct,
        soilingPct = s.pv.soilingPct,
        cableMismatchPct = s.pv.cableMismatchPct,
        albedo = s.pv.albedo,
        annualKwh = s.household.annualKwh,
        loadProfile = s.household.profile.name,
        loadSeason = s.household.season.name,
        batteryKwh = s.household.batteryKwh,
        selfUseOverridePct = s.household.selfConsumptionOverridePct,
        systemCost = s.finance.systemCost,
        pricePerKwh = s.finance.pricePerKwh,
        priceChangePct = s.finance.priceChangePct,
        feedInPerKwh = s.finance.feedInPerKwh,
        degradationPct = s.finance.degradationPct,
        lifetimeYears = s.finance.lifetimeYears,
        discountRatePct = s.finance.discountRatePct,
        maintenancePerYear = s.finance.maintenancePerYear,
        co2GramsPerKwh = s.finance.co2GramsPerKwh,
        currencyCode = s.finance.currencyCode,
        resultAnnualKwh = r?.yield?.annualKwh ?: 0.0,
        resultPaybackYears = r?.finance?.paybackYears,
    )

    fun toState(e: SetupEntity): SetupState {
        val custom = if (e.customLat != null && e.customLon != null && e.customZone != null) {
            CustomLocation(e.customLabel ?: "My balcony", e.customLat, e.customLon, e.customZone)
        } else null
        val ghi = e.refinedGhi?.let(::split)
        val temp = e.refinedTemp?.let(::split)
        val base = SetupState(
            countryCode = e.countryCode,
            cityName = e.cityName,
            custom = custom,
            pv = PvSystem(
                panelCount = e.panelCount, panelWp = e.panelWp, tiltDeg = e.tiltDeg, azimuthDeg = e.azimuthDeg,
                inverterAcW = e.inverterAcW, inverterEfficiency = e.inverterEfficiency, tempCoeffPct = e.tempCoeffPct,
                moduleEfficiency = e.moduleEfficiency,
                mounting = runCatching { Mounting.valueOf(e.mounting) }.getOrDefault(Mounting.RAILING),
                shadingPct = e.shadingPct, soilingPct = e.soilingPct, cableMismatchPct = e.cableMismatchPct, albedo = e.albedo,
            ),
            household = Household(
                annualKwh = e.annualKwh,
                profile = runCatching { LoadProfile.valueOf(e.loadProfile) }.getOrDefault(LoadProfile.STANDARD),
                season = runCatching { LoadSeason.valueOf(e.loadSeason) }.getOrDefault(LoadSeason.HEATING),
                batteryKwh = e.batteryKwh,
                selfConsumptionOverridePct = e.selfUseOverridePct,
            ),
            finance = FinanceInputs(
                systemCost = e.systemCost, pricePerKwh = e.pricePerKwh, priceChangePct = e.priceChangePct,
                feedInPerKwh = e.feedInPerKwh, degradationPct = e.degradationPct, lifetimeYears = e.lifetimeYears,
                discountRatePct = e.discountRatePct, maintenancePerYear = e.maintenancePerYear,
                co2GramsPerKwh = e.co2GramsPerKwh, currencyCode = e.currencyCode,
            ),
        )
        if (ghi == null || temp == null || ghi.size != 12 || temp.size != 12) return base
        val dtr = base.climateCity.city.climate.diurnalRange
        return base.copy(refinedClimate = MonthlyClimate(ghi, temp, dtr, e.refinedSource ?: "PVGIS"))
    }

    private fun join(v: List<Double>) = v.joinToString(",") { String.format(Locale.US, "%.4f", it) }
    private fun split(s: String) = s.split(',').mapNotNull { it.trim().toDoubleOrNull() }
}
