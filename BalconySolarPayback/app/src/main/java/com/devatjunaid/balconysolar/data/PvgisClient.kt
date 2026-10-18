package com.devatjunaid.balconysolar.data

import com.devatjunaid.balconysolar.engine.MonthlyClimate
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fetches long-term monthly horizontal irradiation and temperature for exact coordinates from the
 * European Commission's PVGIS API (no key required). Only the coordinates are sent.
 */
object PvgisClient {

    private const val ENDPOINT = "https://re.jrc.ec.europa.eu/api/v5_3/MRcalc"
    private val DAYS = doubleArrayOf(31.0, 28.25, 31.0, 30.0, 31.0, 30.0, 31.0, 31.0, 30.0, 31.0, 30.0, 31.0)

    class PvgisException(message: String) : IOException(message)

    suspend fun fetch(latitude: Double, longitude: Double, diurnalRange: Double): MonthlyClimate = withContext(Dispatchers.IO) {
        val url = URI(String.format(Locale.US, "%s?lat=%.4f&lon=%.4f&horirrad=1&avtemp=1&outputformat=json", ENDPOINT, latitude, longitude)).toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
                throw PvgisException(if (msg.isNotBlank()) msg else "PVGIS answered with HTTP $code")
            }
            parse(body, diurnalRange)
        } finally {
            conn.disconnect()
        }
    }

    internal fun parse(json: String, diurnalRange: Double): MonthlyClimate {
        val root = JSONObject(json)
        val monthly = root.getJSONObject("outputs").getJSONArray("monthly")
        val hSum = DoubleArray(12)
        val tSum = DoubleArray(12)
        val hCount = IntArray(12)
        val tCount = IntArray(12)
        val years = HashSet<Int>()
        for (i in 0 until monthly.length()) {
            val o = monthly.getJSONObject(i)
            val m = o.getInt("month") - 1
            if (m !in 0..11) continue
            years += o.optInt("year")
            val h = o.optDouble("H(h)_m", Double.NaN)
            if (!h.isNaN()) { hSum[m] += h; hCount[m]++ }
            val t = o.optDouble("T2m", Double.NaN)
            if (!t.isNaN()) { tSum[m] += t; tCount[m]++ }
        }
        if (hCount.any { it == 0 }) throw PvgisException("PVGIS returned incomplete data for this location")
        val ghi = List(12) { hSum[it] / hCount[it] / DAYS[it] }
        val temp = List(12) { if (tCount[it] > 0) tSum[it] / tCount[it] else Double.NaN }
        if (temp.any { it.isNaN() }) throw PvgisException("PVGIS returned no temperature for this location")
        val span = if (years.isNotEmpty()) " ${years.min()}–${years.max()}" else ""
        return MonthlyClimate(ghi, temp, diurnalRange, "PVGIS satellite data$span")
    }
}
