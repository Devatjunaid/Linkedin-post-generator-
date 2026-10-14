package com.devatjunaid.renewalert.domain

import com.devatjunaid.renewalert.data.AppSettings
import com.devatjunaid.renewalert.data.PricePoint
import com.devatjunaid.renewalert.data.SubStatus
import com.devatjunaid.renewalert.data.Subscription
import com.devatjunaid.renewalert.data.Usage
import java.time.LocalDate
import java.time.YearMonth

/** Converts amounts into the user's main currency with the rates they entered. */
class Fx(val main: String, private val rates: Map<String, Double>) {
    fun rate(currency: String): Double = if (currency == main) 1.0 else rates[currency] ?: 1.0
    fun hasRate(currency: String): Boolean = currency == main || rates.containsKey(currency)
    fun toMain(minor: Double, currency: String): Double = minor * rate(currency)
}

data class Charge(
    val sub: Subscription,
    val date: LocalDate,
    val amountMinor: Long,
    val amountMain: Double,
    val isTrialConversion: Boolean,
)

data class CategoryShare(val category: String, val monthlyMain: Double, val count: Int)

enum class SuggestionType { TRIAL_ENDING, RARELY_USED, OVERLAP, PRICE_INCREASE, MISSING_RATE }

data class Suggestion(
    val type: SuggestionType,
    val title: String,
    val body: String,
    val subscriptionId: Long?,
    val yearlySavingMain: Double = 0.0,
)

/** All derived numbers the screens show, computed once per data change. */
class Portfolio(
    val subs: List<Subscription>,
    prices: List<PricePoint>,
    val settings: AppSettings,
    val today: LocalDate = LocalDate.now(),
) {
    val fx = Fx(settings.currency, settings.rates)
    private val history: Map<Long, List<PricePoint>> =
        prices.groupBy { it.subscriptionId }.mapValues { (_, v) -> v.sortedBy { it.effectiveEpochDay } }

    fun historyFor(id: Long): List<PricePoint> = history[id].orEmpty()

    val active: List<Subscription> = subs.filter { it.status == SubStatus.ACTIVE }
    val trials: List<Subscription> = subs.filter { it.status == SubStatus.TRIAL }
    val paused: List<Subscription> = subs.filter { it.status == SubStatus.PAUSED }
    val cancelled: List<Subscription> = subs.filter { it.status == SubStatus.CANCELLED }

    fun monthlyMain(sub: Subscription): Double = fx.toMain(Billing.monthlyMinor(sub), sub.currency)
    fun yearlyMain(sub: Subscription): Double = monthlyMain(sub) * 12

    val monthlyTotal: Double = active.sumOf { monthlyMain(it) }
    val yearlyTotal: Double = monthlyTotal * 12
    val trialMonthlyIfKept: Double = trials.sumOf { monthlyMain(it) }

    fun nextCharge(sub: Subscription): LocalDate? = Billing.nextCharge(sub, today)

    /** Charges in a date range. Past ranges include subscriptions that have since ended. */
    fun charges(from: LocalDate, to: LocalDate): List<Charge> =
        subs.flatMap { sub ->
            val dates = when (sub.status) {
                SubStatus.ACTIVE, SubStatus.TRIAL -> Billing.occurrences(sub, from, to)
                else -> Billing.occurrences(sub, from, if (to.isBefore(today)) to else today)
            }
            dates.map { d ->
                val amount = Billing.priceAt(sub, historyFor(sub.id), d)
                Charge(
                    sub = sub,
                    date = d,
                    amountMinor = amount,
                    amountMain = fx.toMain(amount.toDouble(), sub.currency),
                    isTrialConversion = sub.status == SubStatus.TRIAL &&
                        d.toEpochDay() == sub.firstPaymentEpochDay,
                )
            }
        }.sortedWith(compareBy<Charge> { it.date }.thenBy { it.sub.name.lowercase() })

    fun upcoming(days: Long): List<Charge> = charges(today, today.plusDays(days - 1))

    val categories: List<CategoryShare> = active
        .groupBy { it.category }
        .map { (cat, list) -> CategoryShare(cat, list.sumOf { monthlyMain(it) }, list.size) }
        .sortedByDescending { it.monthlyMain }

    fun spentSoFar(sub: Subscription): Long = Billing.spentSoFar(sub, historyFor(sub.id), today)

    fun savedSoFarMain(sub: Subscription): Double =
        fx.toMain(Billing.savedSoFar(sub, today).toDouble(), sub.currency)

    val savedTotalMain: Double = cancelled.sumOf { savedSoFarMain(it) }
    val savedYearlyMain: Double = cancelled.sumOf { yearlyMain(it) }

    /** Actual (scheduled) spend per calendar month, oldest first. */
    fun monthlyHistory(months: Int): List<Pair<YearMonth, Double>> {
        val now = YearMonth.from(today)
        return (months - 1 downTo 0).map { back ->
            val ym = now.minusMonths(back.toLong())
            val list = charges(ym.atDay(1), ym.atEndOfMonth()).filterNot { it.isTrialConversion }
            ym to list.sumOf { it.amountMain }
        }
    }

    fun yearlyHistory(years: Int): List<Pair<Int, Double>> {
        val y = today.year
        return (years - 1 downTo 0).map { back ->
            val year = y - back
            val list = charges(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .filterNot { it.isTrialConversion }
            year to list.sumOf { it.amountMain }
        }
    }

    fun suggestions(): List<Suggestion> {
        val out = ArrayList<Suggestion>()
        trials.forEach { t ->
            val end = t.trialEndEpochDay?.let(LocalDate::ofEpochDay) ?: return@forEach
            val d = Billing.daysUntil(today, end)
            if (d in 0..7) {
                out += Suggestion(
                    SuggestionType.TRIAL_ENDING,
                    "${t.name} trial ends ${Formats.relativeDay(today, end)}",
                    "Cancel before then or it becomes a paid ${Billing.cycleLabel(t.cycleUnit, t.cycleCount).lowercase()} subscription.",
                    t.id,
                    yearlyMain(t),
                )
            }
        }
        val counted = HashSet<Long>()
        active.filter { it.usage == Usage.RARELY }.forEach { s ->
            counted += s.id
            out += Suggestion(
                SuggestionType.RARELY_USED,
                "${s.name} is rarely used",
                "You marked it as rarely used. It costs ${Formats.money(yearlyMain(s), fx.main)} a year.",
                s.id,
                yearlyMain(s),
            )
        }
        active.groupBy { it.category }
            .filter { (cat, list) -> list.size >= 2 && cat != "Other" }
            .forEach { (cat, list) ->
                val total = list.sumOf { yearlyMain(it) }
                val candidate = list.filterNot { it.id in counted }.maxByOrNull { yearlyMain(it) }
                out += Suggestion(
                    SuggestionType.OVERLAP,
                    "${list.size} ${cat.lowercase()} subscriptions overlap",
                    "${list.joinToString { it.name }} cost ${Formats.money(total, fx.main)} a year together." +
                        (candidate?.let { " Dropping one saves up to ${Formats.money(yearlyMain(it), fx.main)}." } ?: ""),
                    null,
                    candidate?.let { yearlyMain(it) } ?: 0.0,
                )
            }
        active.forEach { s ->
            val h = historyFor(s.id)
            if (h.size >= 2) {
                val last = h[h.size - 1]
                val prev = h[h.size - 2]
                val recent = Billing.daysUntil(LocalDate.ofEpochDay(last.effectiveEpochDay), today) <= 365
                if (last.priceMinor > prev.priceMinor && recent) {
                    val diffYear = fx.toMain((last.priceMinor - prev.priceMinor) * Billing.monthlyFactor(s) * 12, s.currency)
                    out += Suggestion(
                        SuggestionType.PRICE_INCREASE,
                        "${s.name} raised its price",
                        "${Formats.money(prev.priceMinor.toDouble(), s.currency)} → ${Formats.money(last.priceMinor.toDouble(), s.currency)} " +
                            "since ${Formats.monthYear(LocalDate.ofEpochDay(last.effectiveEpochDay))}. That's ${Formats.money(diffYear, fx.main)} more a year.",
                        s.id,
                    )
                }
            }
        }
        val missing = subs.map { it.currency }.distinct().filterNot { fx.hasRate(it) }
        if (missing.isNotEmpty()) {
            out += Suggestion(
                SuggestionType.MISSING_RATE,
                "Set exchange rates",
                "Totals treat ${missing.joinToString()} as 1:1 with ${fx.main}. Add rates in Settings › Exchange rates.",
                null,
            )
        }
        return out
    }

    val potentialSavingsMain: Double
        get() = suggestions()
            .filter { it.type == SuggestionType.RARELY_USED || it.type == SuggestionType.OVERLAP }
            .sumOf { it.yearlySavingMain }

    companion object {
        fun empty() = Portfolio(emptyList(), emptyList(), AppSettings())
    }
}
