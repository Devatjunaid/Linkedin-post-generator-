package com.sunarc.inverterlogger.domain

import java.time.ZoneId

data class SystemProfile(
    val onboarded: Boolean = false,
    val countryCode: String = "DE",
    val systemName: String = "My solar system",
    val kwp: Double = 0.0,
    val tiltDeg: Double = 30.0,
    val azimuthDeg: Double = 180.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tariffModel: TariffModel? = null,
    /** 0 means "not set yet"; the app asks instead of guessing prices. */
    val buyPrice: Double = 0.0,
    val exportPrice: Double = 0.0,
    val selfUseShare: Double = 0.35,
    val co2Grams: Int? = null,
    val systemCost: Double? = null,
    val installEpochDay: Long? = null,
    val sunsetReminder: Boolean = false,
    val monthlySummary: Boolean = false
) {
    val country: Country get() = Countries.byCode(countryCode)
    val lat: Double get() = latitude ?: country.latitude
    val lon: Double get() = longitude ?: country.longitude
    val effectiveTariffModel: TariffModel get() = tariffModel ?: country.tariffModel
    val effectiveCo2: Int get() = co2Grams ?: country.gridCo2GramsPerKwh
    val pricesSet: Boolean get() = buyPrice > 0
    val zone: ZoneId get() = ZoneId.systemDefault()
    val currency: String get() = country.currencySymbol

    val tariff: Tariff get() = Tariff(effectiveTariffModel, buyPrice, exportPrice, selfUseShare)

    val pvSystem: PvSystem? get() = if (kwp > 0) PvSystem(kwp, tiltDeg, azimuthDeg, lat, lon) else null
}
