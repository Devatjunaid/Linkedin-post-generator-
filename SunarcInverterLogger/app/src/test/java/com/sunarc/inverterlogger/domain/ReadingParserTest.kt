package com.sunarc.inverterlogger.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingParserTest {
    private val parser = ReadingParser()
    private fun lines(vararg s: String) = s.map { OcrLine(it) }

    @Test
    fun smaDisplay() {
        val r = parser.parse(
            lines("SUNNY BOY 5.0", "E-Today 18.4kWh", "Pac 3210W", "E-Total 12481kWh"),
            ParseContext(InverterBrands.byId("sma"), systemKwp = 6.4)
        )
        assertEquals(18.4, r[Field.ENERGY_TODAY]!!.value, 1e-9)
        assertEquals(3210.0, r[Field.POWER_NOW]!!.value, 1e-9)
        assertEquals(12481.0, r[Field.ENERGY_TOTAL]!!.value, 1e-9)
        assertEquals("sma", r.detectedBrand?.id)
        assertNull(r.faultCode)
    }

    @Test
    fun gluedLabelsAndKilowatts() {
        val r = parser.parse(lines("Pac3.21kW", "E-Day 9.6 kWh"), ParseContext(systemKwp = 5.0))
        assertEquals(3210.0, r[Field.POWER_NOW]!!.value, 1e-6)
        assertEquals(9.6, r[Field.ENERGY_TODAY]!!.value, 1e-9)
    }

    @Test
    fun germanLabelsWithCommaDecimal() {
        val r = parser.parse(
            lines("Tagesertrag", "18,4 kWh", "Gesamtertrag 12,48 MWh", "Leistung 2,1 kW"),
            ParseContext(InverterBrands.byId("kostal"), systemKwp = 8.0, decimalStyle = DecimalStyle.COMMA)
        )
        assertEquals(18.4, r[Field.ENERGY_TODAY]!!.value, 1e-9)
        assertEquals(12480.0, r[Field.ENERGY_TOTAL]!!.value, 1e-6)
        assertEquals(2100.0, r[Field.POWER_NOW]!!.value, 1e-6)
    }

    @Test
    fun appScreenshotWithoutLabelsUsesUnitsAndSize() {
        val r = parser.parse(lines("FusionSolar", "2.84 kW", "21.6 kWh", "9.87 MWh"), ParseContext(systemKwp = 6.0))
        assertEquals(2840.0, r[Field.POWER_NOW]!!.value, 1e-6)
        assertEquals(21.6, r[Field.ENERGY_TODAY]!!.value, 1e-9)
        assertEquals(9870.0, r[Field.ENERGY_TOTAL]!!.value, 1e-6)
        assertEquals("huawei", r.detectedBrand?.id)
    }

    @Test
    fun ambiguousThousandsResolvedByLastTotal() {
        val r = parser.parse(lines("E-Total 12.481 kWh"), ParseContext(lastTotalKwh = 12460.0))
        assertEquals(12481.0, r[Field.ENERGY_TOTAL]!!.value, 1e-9)
    }

    @Test
    fun ocrLookAlikesAreRepaired() {
        val r = parser.parse(lines("E-Today 1B.4 kWh"), ParseContext(systemKwp = 6.0))
        assertEquals(18.4, r[Field.ENERGY_TODAY]!!.value, 1e-9)
        assertTrue(r[Field.ENERGY_TODAY]!!.confidence < 0.9)
    }

    @Test
    fun implausibleTodayIsRejected() {
        val r = parser.parse(lines("Today 184 kWh"), ParseContext(systemKwp = 6.0))
        assertNull(r[Field.ENERGY_TODAY])
    }

    @Test
    fun valueOnLineBelowWithGeometry() {
        val r = parser.parse(
            listOf(
                OcrLine("Yield today", OcrBox(10f, 10f, 120f, 30f)),
                OcrLine("Lifetime", OcrBox(200f, 10f, 300f, 30f)),
                OcrLine("14.2 kWh", OcrBox(12f, 36f, 110f, 60f)),
                OcrLine("3.4 MWh", OcrBox(205f, 36f, 300f, 60f))
            ),
            ParseContext(systemKwp = 5.0)
        )
        assertEquals(14.2, r[Field.ENERGY_TODAY]!!.value, 1e-9)
        assertEquals(3400.0, r[Field.ENERGY_TOTAL]!!.value, 1e-6)
    }

    @Test
    fun faultCodeIsFoundAndNotUsedAsEnergy() {
        val r = parser.parse(lines("State 102", "Day Energy 4.2 kWh"), ParseContext(InverterBrands.byId("fronius"), systemKwp = 5.0))
        assertEquals("102", r.faultCode)
        assertEquals(4.2, r[Field.ENERGY_TODAY]!!.value, 1e-9)
        val info = FaultLibrary.decode(r.faultCode, r.rawText, "fronius")
        assertEquals("AC voltage too high", info.title)
    }

    @Test
    fun statusWithPowerIsNotAFault() {
        val r = parser.parse(lines("Status 3210 W"), ParseContext(systemKwp = 6.0))
        assertNull(r.faultCode)
        assertNotNull(r[Field.POWER_NOW])
    }

    @Test
    fun frenchAndSpanishLabels() {
        val fr = parser.parse(lines("Énergie du jour 12.1 kWh"), ParseContext(systemKwp = 6.0))
        assertEquals(12.1, fr[Field.ENERGY_TODAY]!!.value, 1e-9)
        val es = parser.parse(lines("Energía hoy: 7,5 kWh", "Potencia 1,9 kW"), ParseContext(systemKwp = 6.0, decimalStyle = DecimalStyle.COMMA))
        assertEquals(7.5, es[Field.ENERGY_TODAY]!!.value, 1e-9)
        assertEquals(1900.0, es[Field.POWER_NOW]!!.value, 1e-6)
    }
}
