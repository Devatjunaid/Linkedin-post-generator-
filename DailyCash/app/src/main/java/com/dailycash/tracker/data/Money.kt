package com.dailycash.tracker.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * All amounts are stored as Long minor units (cents, paise...) using the currency's own
 * number of decimals, so there is never any floating point drift in balances.
 */
object Money {

    private val symbols = mapOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "INR" to "₹", "JPY" to "¥", "CNY" to "¥",
        "PKR" to "Rs", "AED" to "AED ", "SAR" to "SAR ", "TRY" to "₺", "KRW" to "₩", "NGN" to "₦",
        "PHP" to "₱", "THB" to "฿", "BDT" to "৳", "RUB" to "₽", "VND" to "₫", "ILS" to "₪",
        "UAH" to "₴", "BRL" to "R$", "ZAR" to "R", "MAD" to "DH ", "EGP" to "E£", "KES" to "KSh ",
        "MXN" to "MX$", "CAD" to "CA$", "AUD" to "A$", "SGD" to "S$", "CHF" to "CHF ", "IDR" to "Rp",
        "MYR" to "RM", "NZD" to "NZ$", "HKD" to "HK$", "LKR" to "Rs ", "NPR" to "Rs ", "QAR" to "QAR ",
        "KWD" to "KD ", "OMR" to "OMR ", "BHD" to "BD ", "PLN" to "zł ", "SEK" to "kr ", "NOK" to "kr ",
        "DKK" to "kr ", "CZK" to "Kč ", "HUF" to "Ft ",
    )

    fun digits(code: String): Int =
        runCatching { Currency.getInstance(code).defaultFractionDigits }.getOrDefault(2).coerceIn(0, 4)

    fun symbol(code: String): String =
        symbols[code] ?: runCatching { Currency.getInstance(code).getSymbol(Locale.getDefault()) + " " }
            .getOrDefault("$code ")

    fun displayName(code: String): String =
        runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }.getOrDefault(code)

    fun toMajor(minor: Long, code: String): BigDecimal = BigDecimal.valueOf(minor, digits(code))

    fun toMinor(major: BigDecimal, code: String): Long =
        major.setScale(digits(code), RoundingMode.HALF_EVEN).unscaledValue().toLong()

    /** "€1,234.50", "−₹420", "¥1,200". */
    fun format(minor: Long, code: String, withSymbol: Boolean = true, forceSign: Boolean = false): String {
        val d = digits(code)
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = if (minor % pow10(d) == 0L && d > 0) 0 else d
            maximumFractionDigits = d
            isGroupingUsed = true
        }
        val body = nf.format(toMajor(kotlin.math.abs(minor), code))
        val sign = when {
            minor < 0 -> "−"
            forceSign && minor > 0 -> "+"
            else -> ""
        }
        return sign + (if (withSymbol) symbol(code) else "") + body
    }

    /** Same as [format] but always shows the decimals (for the amount being typed). */
    fun formatExact(minor: Long, code: String): String {
        val d = digits(code)
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = d
            maximumFractionDigits = d
        }
        return symbol(code) + nf.format(toMajor(minor, code))
    }

    /** Parses keypad text such as "12.5" into minor units, or null when it is not a valid amount. */
    fun parse(text: String, code: String): Long? {
        if (text.isBlank()) return null
        return runCatching { toMinor(BigDecimal(text), code) }.getOrNull()?.takeIf { it >= 0 }
    }

    fun pow10(n: Int): Long {
        var r = 1L
        repeat(n) { r *= 10 }
        return r
    }

    fun allCurrencies(): List<String> =
        Currency.getAvailableCurrencies()
            .map { it.currencyCode }
            .filter { it.length == 3 && it !in setOf("XXX", "XTS", "XAU", "XAG", "XPT", "XPD", "XBA", "XBB", "XBC", "XBD", "XDR", "XSU", "XUA") }
            .sorted()
}

/** Notes and coins in circulation, in major units, used for the tap-to-count tray. */
object Denominations {
    data class DenomSet(val notes: List<String>, val coins: List<String>)

    private val table = mapOf(
        "USD" to DenomSet(listOf("100", "50", "20", "10", "5", "1"), listOf("0.25", "0.10", "0.05", "0.01")),
        "EUR" to DenomSet(listOf("200", "100", "50", "20", "10", "5"), listOf("2", "1", "0.50", "0.20", "0.10", "0.05")),
        "GBP" to DenomSet(listOf("50", "20", "10", "5"), listOf("2", "1", "0.50", "0.20", "0.10")),
        "INR" to DenomSet(listOf("500", "200", "100", "50", "20", "10"), listOf("20", "10", "5", "2", "1")),
        "PKR" to DenomSet(listOf("5000", "1000", "500", "100", "50", "20", "10"), listOf("10", "5", "2", "1")),
        "AED" to DenomSet(listOf("1000", "500", "200", "100", "50", "20", "10", "5"), listOf("1", "0.50", "0.25")),
        "SAR" to DenomSet(listOf("500", "200", "100", "50", "10", "5"), listOf("2", "1", "0.50")),
        "QAR" to DenomSet(listOf("500", "200", "100", "50", "10", "5", "1"), listOf("0.50", "0.25")),
        "JPY" to DenomSet(listOf("10000", "5000", "1000"), listOf("500", "100", "50", "10")),
        "CNY" to DenomSet(listOf("100", "50", "20", "10", "5", "1"), listOf("1", "0.50")),
        "CAD" to DenomSet(listOf("100", "50", "20", "10", "5"), listOf("2", "1", "0.25", "0.10")),
        "AUD" to DenomSet(listOf("100", "50", "20", "10", "5"), listOf("2", "1", "0.50", "0.20")),
        "NZD" to DenomSet(listOf("100", "50", "20", "10", "5"), listOf("2", "1", "0.50", "0.20")),
        "TRY" to DenomSet(listOf("200", "100", "50", "20", "10", "5"), listOf("1", "0.50", "0.25")),
        "MAD" to DenomSet(listOf("200", "100", "50", "20"), listOf("10", "5", "2", "1")),
        "EGP" to DenomSet(listOf("200", "100", "50", "20", "10", "5"), listOf("1", "0.50")),
        "BDT" to DenomSet(listOf("1000", "500", "100", "50", "20", "10"), listOf("5", "2", "1")),
        "LKR" to DenomSet(listOf("5000", "1000", "500", "100", "50", "20"), listOf("10", "5", "2", "1")),
        "NPR" to DenomSet(listOf("1000", "500", "100", "50", "20", "10"), listOf("2", "1")),
        "NGN" to DenomSet(listOf("1000", "500", "200", "100", "50"), emptyList()),
        "KES" to DenomSet(listOf("1000", "500", "200", "100", "50"), listOf("20", "10", "5", "1")),
        "MXN" to DenomSet(listOf("1000", "500", "200", "100", "50", "20"), listOf("10", "5", "2", "1")),
        "BRL" to DenomSet(listOf("200", "100", "50", "20", "10", "5"), listOf("1", "0.50", "0.25", "0.10")),
        "ZAR" to DenomSet(listOf("200", "100", "50", "20", "10"), listOf("5", "2", "1", "0.50")),
        "THB" to DenomSet(listOf("1000", "500", "100", "50", "20"), listOf("10", "5", "2", "1")),
        "IDR" to DenomSet(listOf("100000", "50000", "20000", "10000", "5000", "2000"), listOf("1000", "500")),
        "MYR" to DenomSet(listOf("100", "50", "20", "10", "5", "1"), listOf("0.50", "0.20", "0.10")),
        "SGD" to DenomSet(listOf("100", "50", "10", "5", "2"), listOf("1", "0.50", "0.20", "0.10")),
        "PHP" to DenomSet(listOf("1000", "500", "200", "100", "50", "20"), listOf("20", "10", "5", "1")),
        "KRW" to DenomSet(listOf("50000", "10000", "5000", "1000"), listOf("500", "100")),
        "CHF" to DenomSet(listOf("200", "100", "50", "20", "10"), listOf("5", "2", "1", "0.50")),
        "HKD" to DenomSet(listOf("1000", "500", "100", "50", "20"), listOf("10", "5", "2", "1")),
        "VND" to DenomSet(listOf("500000", "200000", "100000", "50000", "20000", "10000"), emptyList()),
        "RUB" to DenomSet(listOf("5000", "1000", "500", "100"), listOf("10", "5", "2", "1")),
        "PLN" to DenomSet(listOf("200", "100", "50", "20", "10"), listOf("5", "2", "1")),
        "SEK" to DenomSet(listOf("1000", "500", "200", "100", "50", "20"), listOf("10", "5", "2", "1")),
    )

    fun forCurrency(code: String): DenomSet? = table[code]

    fun toMinor(major: String, code: String): Long = Money.toMinor(BigDecimal(major), code)
}
