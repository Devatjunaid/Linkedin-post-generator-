package com.sunarc.inverterlogger.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DomainLogicTest {

    @Test
    fun numberInterpretation() {
        assertEquals(listOf(1.5), NumberParsing.interpret("1,5", DecimalStyle.COMMA))
        assertEquals(listOf(12481.0), NumberParsing.interpret("12.481,0", DecimalStyle.COMMA))
        assertEquals(listOf(1234567.0), NumberParsing.interpret("1,234,567", DecimalStyle.POINT))
        assertEquals(listOf(1234.0, 1.234), NumberParsing.interpret("1,234", DecimalStyle.POINT))
        assertEquals(listOf(1.234, 1234.0), NumberParsing.interpret("1.234", DecimalStyle.COMMA))
    }

    @Test
    fun interstitialCapIsThirtySeconds() {
        var now = 1_000_000L
        val cap = FrequencyCap(AdConfig.INTERSTITIAL_INTERVAL_MS) { now }
        assertTrue(cap.canShow())
        cap.markShown()
        now += 29_999
        assertFalse(cap.canShow())
        now += 1
        assertTrue(cap.canShow())
    }

    @Test
    fun bannerRetriesTwiceThenWaitsFifteenSeconds() {
        val p = BannerRetryPolicy(AdConfig.BANNER_QUICK_ATTEMPTS, AdConfig.BANNER_BACKOFF_MS)
        assertEquals(0, p.delayBeforeNextAttempt())
        p.onFailed()
        assertEquals(0, p.delayBeforeNextAttempt())
        p.onFailed()
        assertEquals(15_000, p.delayBeforeNextAttempt())
        p.onFailed()
        assertEquals(15_000, p.delayBeforeNextAttempt())
        p.onLoaded()
        assertEquals(0, p.delayBeforeNextAttempt())
    }

    @Test
    fun munichSunriseAndClearSkyYield() {
        val zone = ZoneId.of("Europe/Berlin")
        val (rise, set) = SolarMath.sunriseSunset(LocalDate.of(2026, 10, 19), 48.14, 11.58, zone)
        assertNotNull(rise); assertNotNull(set)
        // Munich, 19 Oct: sunrise ≈ 07:38, sunset ≈ 18:16 local time.
        assertEquals(7 * 60 + 38.0, (rise!!.hour * 60 + rise.minute).toDouble(), 6.0)
        assertEquals(18 * 60 + 16.0, (set!!.hour * 60 + set.minute).toDouble(), 6.0)
        val curve = SolarMath.clearSkyDay(PvSystem(6.4, 35.0, 180.0, 48.14, 11.58), LocalDate.of(2026, 10, 19), zone)
        // A clear October day for 6.4 kWp south at 35° is roughly 25–40 kWh.
        assertTrue("clear sky ${curve.clearSkyKwh}", curve.clearSkyKwh in 22.0..42.0)
        val summer = SolarMath.clearSkyDay(PvSystem(6.4, 35.0, 180.0, 48.14, 11.58), LocalDate.of(2026, 6, 21), zone)
        assertTrue(summer.clearSkyKwh > curve.clearSkyKwh)
        val noon = SolarMath.sunPosition(ZonedDateTime.of(2026, 6, 21, 13, 30, 0, 0, zone), 48.14, 11.58)
        assertEquals(65.0, noon.elevationDeg, 1.5)
        assertEquals(180.0, noon.azimuthDeg, 10.0)
    }

    @Test
    fun dailyYieldsFromTodayAndTotals() {
        val zone = ZoneId.of("UTC")
        fun at(d: Int, h: Int) = ZonedDateTime.of(2026, 10, d, h, 0, 0, 0, zone).toInstant()
        val readings = listOf(
            Reading(inverterId = 1, timestamp = at(1, 12), todayKwh = 8.0, totalKwh = 1000.0, powerW = null, source = ReadingSource.PHOTO),
            Reading(inverterId = 1, timestamp = at(1, 18), todayKwh = 15.0, totalKwh = 1007.0, powerW = null, source = ReadingSource.PHOTO),
            // Days 2 and 3 not scanned, day 4 only the total: 30 kWh spread over 3 days.
            Reading(inverterId = 1, timestamp = at(4, 18), todayKwh = null, totalKwh = 1037.0, powerW = null, source = ReadingSource.PHOTO),
            Reading(inverterId = 2, timestamp = at(1, 18), todayKwh = 5.0, totalKwh = null, powerW = null, source = ReadingSource.MANUAL)
        )
        val days = Aggregation.dailyYields(readings, zone)
        assertEquals(20.0, days.first { it.date.dayOfMonth == 1 }.kwh, 1e-9)
        assertEquals(10.0, days.first { it.date.dayOfMonth == 2 }.kwh, 1e-9)
        assertTrue(days.first { it.date.dayOfMonth == 3 }.estimated)
        val stats = Aggregation.stats(days, 6.4)
        assertEquals(50.0, stats.totalKwh, 1e-9)
        assertEquals(1, stats.bestDay!!.date.dayOfMonth)
    }

    @Test
    fun moneyAndPayback() {
        val feedIn = Economics.value(100.0, Tariff(TariffModel.FEED_IN, 0.32, 0.08, 0.6))
        assertEquals(60.0 * 0.32 + 40.0 * 0.08, feedIn.total, 1e-9)
        val net = Economics.value(100.0, Tariff(TariffModel.NET_METERING, 0.15, 0.0, 0.5))
        assertEquals(15.0, net.total, 1e-9)
        assertEquals(10L, Economics.daysToPayback(1000.0, 900.0, 10.0))
        assertEquals(0L, Economics.daysToPayback(1000.0, 1200.0, 10.0))
        assertEquals(7.0, Economics.co2Kg(20.0, 350), 1e-9)
    }

    @Test
    fun sanityFlagsFallingTotal() {
        val prev = Reading(inverterId = 1, timestamp = java.time.Instant.EPOCH, todayKwh = 3.0, totalKwh = 5000.0, powerW = null, source = ReadingSource.PHOTO)
        val issues = SanityChecker.check(10.0, 4990.0, null, 6.0, prev, 30.0, sameDay = false)
        assertTrue(issues.any { it.level == IssueLevel.WARNING && it.message.contains("lower than last time") })
        val ok = SanityChecker.check(10.0, 5007.0, 2000.0, 6.0, prev, 30.0, sameDay = true)
        assertTrue(ok.none { it.level == IssueLevel.WARNING })
    }

    @Test
    fun sunSpecIntModel() {
        val regs = HashMap<Int, Int>()
        fun put(addr: Int, vararg v: Int) = v.forEachIndexed { i, x -> regs[addr + i] = x }
        put(40000, 0x5375, 0x6E53)
        // Common model: id 1, length 66, manufacturer "SMA".
        put(40002, 1, 66)
        put(40004, ('S'.code shl 8) or 'M'.code, ('A'.code shl 8))
        // Inverter model 103, length 50.
        val inv = 40002 + 2 + 66
        put(inv, 103, 50)
        val b = inv + 2
        put(b + 12, 3210, 0)               // W = 3210, W_SF = 0
        put(b + 14, 5001, 0xFFFE)          // Hz = 50.01 with SF -2
        put(b + 22, 0, 12_481_000 and 0xFFFF) // WH low word set below
        regs[b + 22] = 12_481_000 ushr 16
        regs[b + 23] = 12_481_000 and 0xFFFF
        put(b + 24, 0)                     // WH_SF = 0
        put(b + 27, 3805, 0xFFFF)          // DCV = 380.5 (SF -1)
        put(b + 31, 41, 0, 0, 0, 0)        // Tmp = 41 °C, SF 0
        put(b + 36, 4)                     // MPPT
        put(b + 50, 0xFFFF, 0)
        val reader = RegisterReader { address, count -> IntArray(count) { regs[address + it] ?: 0 } }
        val r = SunSpec.read(reader)
        assertEquals("SMA", r.manufacturer)
        assertEquals(3210.0, r.powerW!!, 1e-9)
        assertEquals(12481.0, r.lifetimeKwh!!, 1e-9)
        assertEquals(50.01, r.acFrequency!!, 1e-9)
        assertEquals(380.5, r.dcVoltage!!, 1e-9)
        assertEquals(InverterState.MPPT, r.state)
    }

    @Test
    fun modbusFrames() {
        val req = ModbusTcp.readHoldingRequest(7, 3, 40000, 2)
        assertEquals(12, req.size)
        assertEquals(3, req[7].toInt())
        val reply = byteArrayOf(0, 7, 0, 0, 0, 7, 3, 3, 4, 0x53, 0x75, 0x6E, 0x53)
        val regs = ModbusTcp.parseResponse(reply, 7, 2)
        assertEquals(0x5375, regs[0]); assertEquals(0x6E53, regs[1])
    }

    @Test
    fun csvEscapingBlocksFormulas() {
        assertEquals("'=SUM(A1)", CsvExport.escape("=SUM(A1)"))
        assertEquals("\"a,b\"", CsvExport.escape("a,b"))
    }

    @Test
    fun faultKeywordDecoding() {
        assertEquals(FaultCategory.INSULATION, FaultLibrary.decode("E033", "PV Isolation Low", "growatt").category)
        assertEquals(Severity.INSTALLER, FaultLibrary.decode(null, "Arc fault detected", null).severity)
        assertEquals("Unknown code", FaultLibrary.decode("X9", "", null).title)
    }

    @Test
    fun imageOpsThresholdSeparatesInk() {
        val px = IntArray(100) { if (it % 10 < 3) 0xFF202020.toInt() else 0xFFB0C090.toInt() }
        val out = ImageOps.apply(ImageOps.Pass.THRESHOLD_BOLD, px, 10, 10)
        assertEquals(0xFF000000.toInt(), out[0])
        assertEquals(0xFFFFFFFF.toInt(), out[9])
    }

    @Test
    fun brandsCoverBothRegions() {
        assertTrue(InverterBrands.forRegion(Region.NORTH_AMERICA).size >= 10)
        assertTrue(InverterBrands.forRegion(Region.EUROPE).size >= 15)
        assertEquals("SM", InverterBrands.byId("sma").initials)
    }
}
