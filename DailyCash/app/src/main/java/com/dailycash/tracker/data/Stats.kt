package com.dailycash.tracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Pure calculations over the entry list, kept out of the UI so they are easy to reason about. */
object Stats {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun dateOf(e: CashEntry): LocalDate = Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate()

    /** Value of an entry in the current home currency (saved value when the home currency hasn't changed). */
    fun homeValue(e: CashEntry, home: String, rates: Rates): Long? =
        if (e.homeCurrency == home && e.homeAmountMinor != null) e.homeAmountMinor
        else rates.convert(e.amountMinor, e.currency, home)

    fun spends(entries: List<CashEntry>) = entries.filter { it.isSpend() }

    fun spendsBetween(entries: List<CashEntry>, from: LocalDate, to: LocalDate) =
        entries.filter { it.isSpend() && dateOf(it).let { d -> !d.isBefore(from) && !d.isAfter(to) } }

    fun totalHome(entries: List<CashEntry>, home: String, rates: Rates): Long =
        entries.sumOf { homeValue(it, home, rates) ?: 0L }

    /** Average daily spend in one currency over the last [days] days that had any activity window. */
    fun avgDailySpend(entries: List<CashEntry>, currency: String, days: Int = 7): Long {
        val today = LocalDate.now(zone)
        val from = today.minusDays((days - 1).toLong())
        val inCurrency = entries.filter { it.currency == currency }
        if (inCurrency.isEmpty()) return 0
        val first = inCurrency.minOf { dateOf(it) }
        val window = (if (first.isAfter(from)) java.time.temporal.ChronoUnit.DAYS.between(first, today) + 1 else days.toLong())
            .coerceAtLeast(1)
        val spent = inCurrency.filter { it.isSpend() && !dateOf(it).isBefore(from) }.sumOf { it.amountMinor }
        return spent / window
    }

    /** Days of cash left in a wallet at the recent pace, or null when there is no spending to judge by. */
    fun runwayDays(entries: List<CashEntry>, currency: String, balance: Long): Double? {
        val avg = avgDailySpend(entries, currency)
        if (avg <= 0 || balance <= 0) return null
        return balance.toDouble() / avg
    }

    fun dailyTotals(entries: List<CashEntry>, month: YearMonth, home: String, rates: Rates): Map<Int, Long> =
        spends(entries)
            .filter { YearMonth.from(dateOf(it)) == month }
            .groupBy { dateOf(it).dayOfMonth }
            .mapValues { (_, list) -> totalHome(list, home, rates) }

    data class CategoryTotal(val category: Category, val totalHome: Long, val count: Int)

    fun byCategory(entries: List<CashEntry>, home: String, rates: Rates): List<CategoryTotal> =
        spends(entries)
            .groupBy { it.categoryId to it.label }
            .map { (key, list) -> CategoryTotal(Categories.resolve(key.first, key.second), totalHome(list, home, rates), list.size) }
            .sortedByDescending { it.totalHome }

    /** Categories this person uses most, for the quick-pick row. */
    fun favouriteCategoryIds(entries: List<CashEntry>, limit: Int = 8): List<Pair<String, String>> =
        spends(entries).groupingBy { it.categoryId to it.label }.eachCount()
            .entries.sortedByDescending { it.value }.take(limit).map { it.key }

    data class CountryStat(
        val countryCode: String,
        val name: String,
        val totalHome: Long,
        val days: Int,
        val currencies: List<String>,
        val firstSeen: Long,
        val localTotals: Map<String, Long>,
    ) {
        val perDayHome: Long get() = if (days == 0) 0 else totalHome / days
    }

    fun countries(entries: List<CashEntry>, home: String, rates: Rates): List<CountryStat> =
        spends(entries).groupBy { it.countryCode }.map { (cc, list) ->
            CountryStat(
                countryCode = cc,
                name = CountryDetector.countryName(cc),
                totalHome = totalHome(list, home, rates),
                days = list.map { dateOf(it) }.distinct().size,
                currencies = list.map { it.currency }.distinct(),
                firstSeen = list.minOf { it.timestamp },
                localTotals = list.groupBy { it.currency }.mapValues { (_, l) -> l.sumOf { it.amountMinor } },
            )
        }.sortedBy { it.firstSeen }

    data class Leftover(val currency: String, val balance: Long, val homeValue: Long?)

    /**
     * Small foreign balances (worth less than about 15 USD) — usually coins that a bank
     * will not exchange back, so worth spending before going home.
     */
    fun leftovers(balances: List<CurrencyBalance>, home: String, rates: Rates): List<Leftover> =
        balances.filter { it.currency != home && it.balance > 0 }.mapNotNull { b ->
            val usd = rates.convert(b.balance, b.currency, "USD")
            if (usd != null && usd <= 1500) Leftover(b.currency, b.balance, rates.convert(b.balance, b.currency, home)) else null
        }
}
