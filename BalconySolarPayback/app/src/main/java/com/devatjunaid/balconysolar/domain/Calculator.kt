package com.devatjunaid.balconysolar.domain

import com.devatjunaid.balconysolar.data.City
import com.devatjunaid.balconysolar.data.ClimateTables
import com.devatjunaid.balconysolar.data.Country
import com.devatjunaid.balconysolar.engine.Consumption
import com.devatjunaid.balconysolar.engine.ConsumptionResult
import com.devatjunaid.balconysolar.engine.Finance
import com.devatjunaid.balconysolar.engine.FinanceInputs
import com.devatjunaid.balconysolar.engine.FinanceResult
import com.devatjunaid.balconysolar.engine.Household
import com.devatjunaid.balconysolar.engine.MonthlyClimate
import com.devatjunaid.balconysolar.engine.PvSystem
import com.devatjunaid.balconysolar.engine.Site
import com.devatjunaid.balconysolar.engine.YieldEngine
import com.devatjunaid.balconysolar.engine.YieldResult

data class CustomLocation(val label: String, val latitude: Double, val longitude: Double, val zoneId: String)

/** Everything the user has entered. */
data class SetupState(
    val countryCode: String = "DE",
    val cityName: String = "Berlin",
    val custom: CustomLocation? = null,
    /** Monthly climate fetched from PVGIS for the current location, replacing the table. */
    val refinedClimate: MonthlyClimate? = null,
    val pv: PvSystem = PvSystem(),
    val household: Household = Household(),
    val finance: FinanceInputs = FinanceInputs(),
) {
    val country: Country get() = ClimateTables.byCode(countryCode)
    val city: City get() = country.cities.firstOrNull { it.name == cityName } ?: country.cities.first()

    val latitude: Double get() = custom?.latitude ?: city.latitude
    val longitude: Double get() = custom?.longitude ?: city.longitude
    val zoneId: String get() = custom?.zoneId ?: city.zoneId
    val placeLabel: String get() = custom?.label ?: "${city.name}, ${country.name}"
    val southern: Boolean get() = latitude < 0

    /** Table city whose climate is used when no PVGIS data is loaded. */
    val climateCity: ClimateTables.Nearest
        get() = custom?.let { ClimateTables.nearest(it.latitude, it.longitude) }
            ?: ClimateTables.Nearest(country, city, 0.0)

    fun site(): Site = Site(latitude, longitude, zoneId, refinedClimate ?: climateCity.city.climate)

    fun withCountry(code: String): SetupState {
        val c = ClimateTables.byCode(code)
        return copy(countryCode = c.code, cityName = c.cities.first().name, custom = null, refinedClimate = null)
            .withCountryDefaults(c)
    }

    fun withCity(name: String): SetupState {
        val wasSouthern = southern
        val next = copy(cityName = name, custom = null, refinedClimate = null)
        return next.flipAzimuthIfHemisphereChanged(wasSouthern)
    }

    fun withCustom(location: CustomLocation): SetupState {
        val wasSouthern = southern
        val near = ClimateTables.nearest(location.latitude, location.longitude)
        val base = if (near.country.code != countryCode) {
            copy(countryCode = near.country.code, cityName = near.city.name).withCountryDefaults(near.country)
        } else this
        return base.copy(custom = location, refinedClimate = null).flipAzimuthIfHemisphereChanged(wasSouthern)
    }

    private fun withCountryDefaults(c: Country): SetupState {
        val southernNow = c.cities.first().latitude < 0
        return copy(
            pv = pv.copy(
                inverterAcW = c.inverterLimitW,
                azimuthDeg = if (southernNow) 0.0 else 180.0,
            ),
            household = household.copy(annualKwh = c.householdKwh, season = c.season),
            finance = finance.copy(
                systemCost = c.kitPrice,
                pricePerKwh = c.tariff,
                co2GramsPerKwh = c.co2,
                currencyCode = c.currency,
                feedInPerKwh = 0.0,
            ),
        )
    }

    private fun flipAzimuthIfHemisphereChanged(wasSouthern: Boolean): SetupState =
        if (wasSouthern == southern) this else copy(pv = pv.copy(azimuthDeg = (pv.azimuthDeg + 180.0) % 360.0))

    companion object {
        fun default(): SetupState = SetupState().withCountry("DE")
    }
}

data class Results(
    val site: Site,
    val yield: YieldResult,
    val consumption: ConsumptionResult,
    val finance: FinanceResult,
)

object Calculator {
    fun run(state: SetupState): Results {
        val site = state.site()
        val y = YieldEngine.simulate(site, state.pv)
        val c = Consumption.compute(y, state.household, site.latitude < 0)
        val f = Finance.compute(y.annualKwh, c.selfUsedKwh, c.exportedKwh, state.finance)
        return Results(site, y, c, f)
    }
}
