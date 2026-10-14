package com.devatjunaid.renewalert.domain

import com.devatjunaid.renewalert.data.CycleUnit
import com.devatjunaid.renewalert.data.PricePoint
import com.devatjunaid.renewalert.data.SubStatus
import com.devatjunaid.renewalert.data.Subscription
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Pure billing-schedule math. All amounts are in hundredths of the currency unit. */
object Billing {

    private const val MAX_STEPS = 20_000

    fun approxDaysPerCycle(sub: Subscription): Double {
        val c = sub.cycleCount.coerceAtLeast(1)
        return when (sub.cycleUnit) {
            CycleUnit.DAY -> 1.0 * c
            CycleUnit.WEEK -> 7.0 * c
            CycleUnit.MONTH -> 30.436875 * c
            CycleUnit.YEAR -> 365.2425 * c
        }
    }

    /** n-th charge date, always computed from the anchor so month-ends don't drift. */
    fun chargeDate(sub: Subscription, n: Long): LocalDate {
        val start = LocalDate.ofEpochDay(sub.firstPaymentEpochDay)
        val steps = sub.cycleCount.coerceAtLeast(1).toLong() * n
        return when (sub.cycleUnit) {
            CycleUnit.DAY -> start.plusDays(steps)
            CycleUnit.WEEK -> start.plusWeeks(steps)
            CycleUnit.MONTH -> start.plusMonths(steps)
            CycleUnit.YEAR -> start.plusYears(steps)
        }
    }

    /** First date (exclusive) on which no more charges happen, or null while running. */
    fun endDate(sub: Subscription): LocalDate? = when (sub.status) {
        SubStatus.PAUSED, SubStatus.CANCELLED ->
            sub.statusChangedEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
        else -> null
    }

    private fun startIndexNear(sub: Subscription, date: LocalDate): Long {
        val start = LocalDate.ofEpochDay(sub.firstPaymentEpochDay)
        val days = ChronoUnit.DAYS.between(start, date)
        if (days <= 0) return 0
        return (days / approxDaysPerCycle(sub)).toLong().minus(2).coerceAtLeast(0)
    }

    /** Charge dates within [from, to] (inclusive). */
    fun occurrences(
        sub: Subscription,
        from: LocalDate,
        to: LocalDate,
        ignoreEnd: Boolean = false,
    ): List<LocalDate> {
        if (to.isBefore(from)) return emptyList()
        val end = if (ignoreEnd) null else endDate(sub)
        val out = ArrayList<LocalDate>()
        var n = startIndexNear(sub, from)
        var steps = 0
        while (steps++ < MAX_STEPS) {
            val d = chargeDate(sub, n)
            if (d.isAfter(to)) break
            if (end != null && !d.isBefore(end)) break
            if (!d.isBefore(from)) out += d
            n++
        }
        return out
    }

    fun firstOnOrAfter(sub: Subscription, date: LocalDate): LocalDate {
        var n = startIndexNear(sub, date)
        var steps = 0
        while (steps++ < MAX_STEPS) {
            val d = chargeDate(sub, n)
            if (!d.isBefore(date)) return d
            n++
        }
        return date
    }

    /** Next upcoming charge for running subscriptions. */
    fun nextCharge(sub: Subscription, today: LocalDate): LocalDate? = when (sub.status) {
        SubStatus.ACTIVE, SubStatus.TRIAL -> firstOnOrAfter(sub, today)
        else -> null
    }

    fun lastChargeBefore(sub: Subscription, date: LocalDate): LocalDate? =
        occurrences(sub, LocalDate.ofEpochDay(sub.firstPaymentEpochDay), date.minusDays(1)).lastOrNull()

    fun monthlyFactor(sub: Subscription): Double {
        val c = sub.cycleCount.coerceAtLeast(1).toDouble()
        return when (sub.cycleUnit) {
            CycleUnit.DAY -> 30.436875 / c
            CycleUnit.WEEK -> 4.348125 / c
            CycleUnit.MONTH -> 1.0 / c
            CycleUnit.YEAR -> 1.0 / (12.0 * c)
        }
    }

    fun monthlyMinor(sub: Subscription): Double = sub.priceMinor * monthlyFactor(sub)

    fun yearlyMinor(sub: Subscription): Double = monthlyMinor(sub) * 12.0

    /** Price in effect on [date], from the price history (sorted by date). */
    fun priceAt(sub: Subscription, history: List<PricePoint>, date: LocalDate): Long {
        if (history.isEmpty()) return sub.priceMinor
        val day = date.toEpochDay()
        val p = history.lastOrNull { it.effectiveEpochDay <= day } ?: history.first()
        return p.priceMinor
    }

    fun paidCharges(sub: Subscription, today: LocalDate): List<LocalDate> =
        occurrences(sub, LocalDate.ofEpochDay(sub.firstPaymentEpochDay), today)

    fun spentSoFar(sub: Subscription, history: List<PricePoint>, today: LocalDate): Long =
        paidCharges(sub, today).sumOf { priceAt(sub, history, it) }

    /** Money not spent thanks to cancelling: charges that would have happened since. */
    fun savedSoFar(sub: Subscription, today: LocalDate): Long {
        if (sub.status != SubStatus.CANCELLED) return 0
        val cancelled = sub.statusChangedEpochDay?.let(LocalDate::ofEpochDay) ?: return 0
        var until = today
        val trialEnd = sub.trialEndEpochDay?.let(LocalDate::ofEpochDay)
        if (trialEnd != null && !cancelled.isAfter(trialEnd) && trialEnd.isAfter(until)) until = trialEnd
        val skipped = occurrences(sub, cancelled, until, ignoreEnd = true).size
        return skipped * sub.priceMinor
    }

    fun daysUntil(today: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

    /** Human description of the cycle, e.g. "Monthly", "Every 3 months". */
    fun cycleLabel(unit: CycleUnit, count: Int): String {
        val c = count.coerceAtLeast(1)
        if (c == 1) return when (unit) {
            CycleUnit.DAY -> "Daily"
            CycleUnit.WEEK -> "Weekly"
            CycleUnit.MONTH -> "Monthly"
            CycleUnit.YEAR -> "Yearly"
        }
        if (unit == CycleUnit.MONTH && c == 3) return "Quarterly"
        if (unit == CycleUnit.MONTH && c == 6) return "Every 6 months"
        val word = when (unit) {
            CycleUnit.DAY -> "days"
            CycleUnit.WEEK -> "weeks"
            CycleUnit.MONTH -> "months"
            CycleUnit.YEAR -> "years"
        }
        return "Every $c $word"
    }

    fun perCycleSuffix(unit: CycleUnit, count: Int): String {
        val c = count.coerceAtLeast(1)
        val word = when (unit) {
            CycleUnit.DAY -> "day"
            CycleUnit.WEEK -> "wk"
            CycleUnit.MONTH -> "mo"
            CycleUnit.YEAR -> "yr"
        }
        return if (c == 1) "/$word" else "/$c $word"
    }
}
