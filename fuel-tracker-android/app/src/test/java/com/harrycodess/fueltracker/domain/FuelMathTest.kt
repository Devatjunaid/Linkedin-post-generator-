package com.harrycodess.fueltracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelMathTest {
    private val eps = 1e-6
    private val metricKmL = UnitPrefs(UnitSystem.METRIC, EfficiencyFormat.KM_PER_L)
    private val metricL100 = UnitPrefs(UnitSystem.METRIC, EfficiencyFormat.L_PER_100KM)
    private val imperial = UnitPrefs(UnitSystem.IMPERIAL)

    @Test
    fun `estimate matches the design example`() {
        val e = FuelMath.estimate(distanceKm = 245.0, pricePerLiter = 1.79, kmPerL = 14.5)
        assertNotNull(e)
        assertEquals(16.896551, e!!.fuelLiters, 1e-5)
        assertEquals(30.24, e.totalCost, 0.01)
        assertEquals(e.totalCost / 245.0, e.costPerKm, eps)
    }

    @Test
    fun `estimate rejects non-positive input`() {
        assertNull(FuelMath.estimate(0.0, 1.0, 10.0))
        assertNull(FuelMath.estimate(10.0, 0.0, 10.0))
        assertNull(FuelMath.estimate(10.0, 1.0, -1.0))
    }

    @Test
    fun `L per 100km converts to km per L`() {
        assertEquals(12.5, FuelMath.efficiencyToKmPerL(8.0, metricL100)!!, eps)
        assertEquals(8.0, FuelMath.kmPerLToEfficiency(12.5, metricL100), eps)
        assertEquals(14.5, FuelMath.efficiencyToKmPerL(14.5, metricKmL)!!, eps)
    }

    @Test
    fun `mpg round-trips through km per L`() {
        val kmPerL = FuelMath.efficiencyToKmPerL(30.0, imperial)!!
        assertEquals(12.754311, kmPerL, 1e-5)
        assertEquals(30.0, FuelMath.kmPerLToEfficiency(kmPerL, imperial), eps)
    }

    @Test
    fun `imperial trip cost equals metric trip cost`() {
        // 100 miles at 30 mpg and $3.50/gal = 3.333 gal = $11.67
        val km = FuelMath.distanceToKm(100.0, UnitSystem.IMPERIAL)
        val perL = FuelMath.priceToPerLiter(3.50, UnitSystem.IMPERIAL)
        val kmPerL = FuelMath.efficiencyToKmPerL(30.0, imperial)!!
        val e = FuelMath.estimate(km, perL, kmPerL)!!
        assertEquals(11.666667, e.totalCost, 1e-5)
        assertEquals(3.333333, FuelMath.litersToVolume(e.fuelLiters, UnitSystem.IMPERIAL), 1e-5)
        assertEquals(0.116667, FuelMath.costPerDistance(e.costPerKm, UnitSystem.IMPERIAL), 1e-5)
    }

    @Test
    fun `zero efficiency is invalid`() {
        assertNull(FuelMath.efficiencyToKmPerL(0.0, metricL100))
        assertEquals(0.0, FuelMath.kmPerLToEfficiency(0.0, imperial), eps)
    }

    @Test
    fun `decimal parsing accepts comma and dot`() {
        assertEquals(1.79, parseDecimal("1,79")!!, eps)
        assertEquals(1.79, parseDecimal(" 1.79 ")!!, eps)
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("abc"))
    }

    @Test
    fun `decimal input validation`() {
        assertTrue(isValidDecimalInput(""))
        assertTrue(isValidDecimalInput("12."))
        assertTrue(isValidDecimalInput("12,5"))
        assertTrue(isValidDecimalInput("0.125"))
        assertFalse(isValidDecimalInput("1.2.3"))
        assertFalse(isValidDecimalInput("-4"))
        assertFalse(isValidDecimalInput("1.2345"))
        assertFalse(isValidDecimalInput("12345678"))
    }

    @Test
    fun `input formatting trims trailing zeros`() {
        assertEquals("14.5", Fmt.input(14.50))
        assertEquals("30", Fmt.input(30.0))
        assertEquals("16.9", Fmt.input(16.896))
        assertEquals("0.13", Fmt.input(0.125))
    }
}
