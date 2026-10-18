package com.devatjunaid.balconysolar

import com.devatjunaid.balconysolar.data.ClimateTables
import com.devatjunaid.balconysolar.engine.Consumption
import com.devatjunaid.balconysolar.engine.Finance
import com.devatjunaid.balconysolar.engine.FinanceInputs
import com.devatjunaid.balconysolar.engine.Household
import com.devatjunaid.balconysolar.engine.Irradiance
import com.devatjunaid.balconysolar.engine.PvSystem
import com.devatjunaid.balconysolar.engine.Site
import com.devatjunaid.balconysolar.engine.SolarPosition
import com.devatjunaid.balconysolar.engine.YieldEngine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {

    private fun site(country: String, city: String): Site {
        val c = ClimateTables.byCode(country).cities.first { it.name == city }
        return Site(c.latitude, c.longitude, c.zoneId, c.climate)
    }

    @Test
    fun berlinSolsticeMatchesNoaa() {
        // NOAA Solar Calculator, Berlin 52.52 N 13.405 E, 2025-06-21: rise 04:43, set 21:33, noon 13:08, elevation 60.93°.
        val t = SolarPosition.sunTimes(LocalDate.of(2025, 6, 21), 52.52, 13.405, ZoneId.of("Europe/Berlin"))
        assertEquals(LocalTime.of(4, 43), t.sunrise!!.toLocalTime().truncatedTo(ChronoUnit.MINUTES))
        assertEquals(LocalTime.of(21, 33), t.sunset!!.toLocalTime().truncatedTo(ChronoUnit.MINUTES))
        assertEquals(LocalTime.of(13, 8), t.solarNoon.toLocalTime().truncatedTo(ChronoUnit.MINUTES))
        assertEquals(60.93, t.noonElevationDeg, 0.05)
    }

    @Test
    fun equinoxDeclinationNearZero() {
        val s = SolarPosition.at(Instant.parse("2025-03-20T09:01:00Z"), 0.0, 0.0)
        assertEquals(0.0, s.declinationDeg, 0.05)
    }

    @Test
    fun polarNightAndDay() {
        val zone = ZoneId.of("Europe/Oslo")
        assertEquals(SolarPosition.Polar.POLAR_NIGHT, SolarPosition.sunTimes(LocalDate.of(2025, 12, 21), 78.2, 15.6, zone).polar)
        assertEquals(SolarPosition.Polar.POLAR_DAY, SolarPosition.sunTimes(LocalDate.of(2025, 6, 21), 78.2, 15.6, zone).polar)
    }

    @Test
    fun simulationConservesHorizontalIrradiation() {
        val s = site("DE", "Berlin")
        val y = YieldEngine.simulate(s, PvSystem())
        assertEquals(s.climate.annualGhi(), y.ghiAnnualKwhM2, 2.0)
    }

    @Test
    fun berlinSpecificYieldInPvgisRange() {
        // PVGIS reports roughly 950–1,050 kWh/kWp for a south-facing 35° system in Berlin.
        val y = YieldEngine.simulate(site("DE", "Berlin"), PvSystem(tiltDeg = 35.0))
        assertTrue("got ${y.specificYield}", y.specificYield in 930.0..1080.0)
    }

    @Test
    fun verticalRailingYieldsLessThanTilted() {
        val s = site("DE", "Berlin")
        val vertical = YieldEngine.simulate(s, PvSystem(tiltDeg = 90.0)).annualKwh
        val tilted = YieldEngine.simulate(s, PvSystem(tiltDeg = 35.0)).annualKwh
        assertTrue(vertical < tilted * 0.8)
    }

    @Test
    fun southernHemisphereFacesNorth() {
        val s = site("AU", "Sydney")
        val north = YieldEngine.simulate(s, PvSystem(tiltDeg = 30.0, azimuthDeg = 0.0)).annualKwh
        val south = YieldEngine.simulate(s, PvSystem(tiltDeg = 30.0, azimuthDeg = 180.0)).annualKwh
        assertTrue(north > south * 1.3)
    }

    @Test
    fun inverterClipsWhenUndersized() {
        val s = site("ES", "Seville")
        val big = YieldEngine.simulate(s, PvSystem(panelCount = 4, tiltDeg = 30.0, inverterAcW = 600.0))
        assertTrue(big.peakAcW <= 600.0 + 1e-6)
        assertTrue(big.losses.last().lossKwh > 0)
    }

    @Test
    fun classClearnessKeepsMean() {
        for (kt in listOf(0.2, 0.45, 0.6, 0.7, 0.76)) {
            assertEquals(kt, YieldEngine.classClearness(kt).average(), 1e-9)
        }
    }

    @Test
    fun perezReducesToIsotropicForOvercast() {
        // Fully overcast sky (no beam): F1 is small, result close to the isotropic view factor.
        val tilt = Math.toRadians(60.0)
        val d = Irradiance.perezSkyDiffuse(tilt, 0.5, Math.toRadians(50.0), 200.0, 0.0, 1361.0, Irradiance.airMass(50.0))
        assertEquals(200.0 * (1 + kotlin.math.cos(tilt)) / 2, d, 25.0)
    }

    @Test
    fun selfConsumptionWithinBounds() {
        val y = YieldEngine.simulate(site("DE", "Berlin"), PvSystem())
        val c = Consumption.compute(y, Household(annualKwh = 2500.0), false)
        assertTrue(c.selfConsumptionPct in 55.0..85.0)
        assertEquals(y.annualKwh, c.selfUsedKwh + c.exportedKwh, 1.0)
        val withBattery = Consumption.compute(y, Household(annualKwh = 2500.0, batteryKwh = 2.0), false)
        assertTrue(withBattery.selfUsedKwh > c.selfUsedKwh)
    }

    @Test
    fun paybackIsExactForFlatCashflow() {
        val f = Finance.compute(1000.0, 500.0, 500.0, FinanceInputs(systemCost = 500.0, pricePerKwh = 0.4, priceChangePct = 0.0, degradationPct = 0.0, lifetimeYears = 10))
        assertEquals(2.5, f.paybackYears!!, 1e-9)
        assertEquals(2000.0, f.lifetimeSavings, 1e-9)
        assertEquals(0.05, f.lcoe, 1e-9)
    }

    @Test
    fun noPaybackWhenSavingsTooSmall() {
        val f = Finance.compute(10.0, 5.0, 5.0, FinanceInputs(systemCost = 5000.0, pricePerKwh = 0.1, lifetimeYears = 20))
        assertNull(f.paybackYears)
        assertNotNull(f.cumulative.last())
    }

    @Test
    fun everyTableEntryIsValid() {
        for (c in ClimateTables.countries) for (city in c.cities) {
            ZoneId.of(city.zoneId)
            assertTrue("${city.name} GHI", city.climate.ghiDaily.all { it in 0.05..9.0 })
            assertTrue("${city.name} temp", city.climate.tempMean.all { it in -30.0..45.0 })
            val annual = city.climate.annualGhi()
            assertTrue("${city.name} annual $annual", annual in 650.0..2500.0)
            // Brightest summer month beats mid-winter (monsoon months may be duller than spring).
            val g = city.climate.ghiDaily
            if (city.latitude < -20) assertTrue("${city.name} season", listOf(9, 10, 11, 0, 1).maxOf { g[it] } > g[5])
            if (city.latitude > 20) assertTrue("${city.name} season", (3..7).maxOf { g[it] } > g[11])
        }
    }
}
