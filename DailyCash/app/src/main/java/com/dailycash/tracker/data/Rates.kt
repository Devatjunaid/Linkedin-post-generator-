package com.dailycash.tracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext
import java.net.HttpURLConnection
import java.net.URL

/** Units of each currency per 1 USD. */
data class Rates(
    val perUsd: Map<String, BigDecimal>,
    val updatedAt: Long,
    val live: Boolean,
) {
    fun has(code: String) = perUsd.containsKey(code)

    /** Converts minor units between currencies, or null if a rate is missing. */
    fun convert(minor: Long, from: String, to: String): Long? {
        if (from == to) return minor
        val rFrom = perUsd[from] ?: return null
        val rTo = perUsd[to] ?: return null
        if (rFrom.signum() == 0) return null
        val major = Money.toMajor(minor, from)
        val converted = major.divide(rFrom, MathContext.DECIMAL64).multiply(rTo, MathContext.DECIMAL64)
        return Money.toMinor(converted, to)
    }

    /** Human readable "1 EUR = ₹92.10". */
    fun describe(from: String, to: String): String? {
        if (from == to) return null
        val oneUnit = Money.pow10(Money.digits(from))
        val v = convert(oneUnit, from, to) ?: return null
        return "1 $from = ${Money.format(v, to)}"
    }
}

/**
 * Exchange rates from open.er-api.com (free, no key, ~160 currencies), cached on device.
 * Until the first successful download a built-in offline estimate is used and clearly labelled.
 */
class RatesRepository(context: Context) {
    private val sp = context.getSharedPreferences("rates_cache", Context.MODE_PRIVATE)
    private val _rates = MutableStateFlow(loadCached() ?: fallback())
    val rates: StateFlow<Rates> = _rates.asStateFlow()

    fun isStale(): Boolean =
        !_rates.value.live || System.currentTimeMillis() - _rates.value.updatedAt > STALE_MS

    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            try {
                if (conn.responseCode != 200) return@runCatching false
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parse(body) ?: return@runCatching false
                sp.edit().putString(KEY_JSON, body).apply()
                _rates.value = parsed
                true
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
    }

    private fun loadCached(): Rates? = sp.getString(KEY_JSON, null)?.let(::parse)

    private fun parse(json: String): Rates? = runCatching {
        val o = JSONObject(json)
        if (o.optString("result") != "success") return@runCatching null
        val r = o.getJSONObject("rates")
        val map = HashMap<String, BigDecimal>(r.length())
        r.keys().forEach { k -> map[k] = BigDecimal(r.get(k).toString()) }
        map["USD"] = BigDecimal.ONE
        Rates(map, o.optLong("time_last_update_unix") * 1000L, live = true)
    }.getOrNull()

    private fun fallback(): Rates {
        // Approximate mid-market values, used only until the first online refresh.
        val approx = mapOf(
            "USD" to "1", "EUR" to "0.86", "GBP" to "0.75", "INR" to "88.5", "PKR" to "281", "AED" to "3.6725",
            "SAR" to "3.75", "QAR" to "3.64", "KWD" to "0.307", "OMR" to "0.385", "BHD" to "0.376",
            "JPY" to "150", "CNY" to "7.12", "CAD" to "1.39", "AUD" to "1.53", "NZD" to "1.72", "TRY" to "41.5",
            "MAD" to "9.2", "EGP" to "48.5", "BDT" to "122", "LKR" to "302", "NPR" to "141", "NGN" to "1520",
            "KES" to "129", "MXN" to "18.6", "BRL" to "5.4", "ZAR" to "17.6", "THB" to "32.6", "IDR" to "16400",
            "MYR" to "4.22", "SGD" to "1.29", "PHP" to "57.5", "KRW" to "1390", "CHF" to "0.80", "HKD" to "7.78",
            "VND" to "26300", "RUB" to "81", "PLN" to "3.66", "SEK" to "9.5", "NOK" to "10.1", "DKK" to "6.42",
            "CZK" to "21", "HUF" to "336", "ILS" to "3.35", "UAH" to "41.5",
        )
        return Rates(approx.mapValues { BigDecimal(it.value) }, 0L, live = false)
    }

    private companion object {
        const val ENDPOINT = "https://open.er-api.com/v6/latest/USD"
        const val KEY_JSON = "json"
        const val STALE_MS = 12L * 60 * 60 * 1000
    }
}
