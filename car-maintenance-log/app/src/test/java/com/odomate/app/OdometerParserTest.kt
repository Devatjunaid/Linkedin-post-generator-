package com.odomate.app

import com.odomate.app.ocr.OcrToken
import com.odomate.app.ocr.OdometerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OdometerParserTest {
    private fun token(text: String, h: Float = 40f, cx: Float = 500f, cy: Float = 500f) =
        OcrToken(text, cx - 100, cy - h / 2, cx + 100, cy + h / 2)

    @Test
    fun normalizesGroupedThousands() {
        assertEquals("48215", OdometerParser.normalize("48,215"))
        assertEquals("48215", OdometerParser.normalize("48 215"))
        assertEquals("048215", OdometerParser.normalize("O48215"))
    }

    @Test
    fun ignoresTripMeterDecimals() {
        assertNull(OdometerParser.normalize("123.4"))
    }

    @Test
    fun prefersPlausibleOdometerOverSpeedAndTrip() {
        val tokens = listOf(
            token("120", h = 80f, cy = 300f),
            token("km/h 60"),
            token("ODO 48215 km", h = 40f),
            token("12:45"),
        )
        val best = OdometerParser.best(tokens, 1000, 1000, lastReading = 47_803)
        assertEquals(48_215, best?.value)
    }

    @Test
    fun flagsReadingsBelowLastAsImplausible() {
        val best = OdometerParser.best(listOf(token("41215")), 1000, 1000, lastReading = 47_803)
        assertEquals(false, best?.plausible)
    }
}
