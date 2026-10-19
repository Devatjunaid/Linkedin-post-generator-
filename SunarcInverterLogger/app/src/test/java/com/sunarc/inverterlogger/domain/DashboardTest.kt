package com.sunarc.inverterlogger.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DashboardTest {
    private val zone = ZoneId.systemDefault()

    @Test
    fun buildsTodayAndMoney() {
        val profile = SystemProfile(onboarded = true, countryCode = "DE", kwp = 6.4, buyPrice = 0.32, exportPrice = 0.08, selfUseShare = 0.5, systemCost = 9000.0)
        val now = ZonedDateTime.of(2026, 6, 20, 15, 0, 0, 0, zone)
        val readings = (1..10).map { d ->
            Reading(inverterId = 1, timestamp = now.minusDays(d.toLong()).toInstant(), todayKwh = 30.0 + d % 3, totalKwh = 5000.0 - d * 31, powerW = null, source = ReadingSource.PHOTO)
        } + Reading(inverterId = 1, timestamp = now.minusMinutes(30).toInstant(), todayKwh = 21.0, totalKwh = 5021.0, powerW = 3200.0, source = ReadingSource.PHOTO)
        val d = DashboardBuilder.build(profile, readings, now)
        assertEquals(21.0, d.todayKwh!!, 1e-9)
        assertEquals(3200.0, d.livePowerW!!, 1e-9)
        assertEquals(5021.0, d.lifetimeKwh!!, 1e-9)
        assertNotNull(d.todayMoney)
        assertNotNull(d.healthPercent)
        assertTrue(d.curve.isNotEmpty() && d.curve.max() == 1f)
        assertTrue(d.dayProgress in 0.3f..0.9f)
        assertNotNull(d.expectedBySunset)
        assertTrue(d.expectedBySunset!! > 21.0)
        assertNotNull(d.paybackFraction)
    }

    @Test
    fun emptyLogIsSafe() {
        val d = DashboardBuilder.build(SystemProfile(), emptyList(), ZonedDateTime.now(zone))
        assertNull(d.todayKwh)
        assertNull(d.healthPercent)
        assertTrue(!d.hasData)
    }
}
