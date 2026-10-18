package com.devatjunaid.balconysolar

import com.devatjunaid.balconysolar.data.PvgisClient
import org.junit.Assert.assertEquals
import org.junit.Test

class PvgisParseTest {
    @Test
    fun averagesYearsPerMonth() {
        val rows = (2020..2021).flatMap { y -> (1..12).map { m -> """{"year":$y,"month":$m,"H(h)_m":${if (y == 2020) 30.0 else 32.0},"T2m":${m.toDouble()}}""" } }
        val json = """{"outputs":{"monthly":[${rows.joinToString(",")}]}}"""
        val c = PvgisClient.parse(json, 9.0)
        assertEquals(31.0 / 31.0, c.ghiDaily[0], 1e-9)
        assertEquals(31.0 / 28.25, c.ghiDaily[1], 1e-9)
        assertEquals(12.0, c.tempMean[11], 1e-9)
    }
}
