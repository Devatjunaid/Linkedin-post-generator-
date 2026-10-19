package com.sunarc.inverterlogger.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ReadingSource(val label: String) {
    PHOTO("Photo scan"),
    SCREENSHOT("Screenshot scan"),
    MANUAL("Typed in"),
    NETWORK("Local network")
}

data class Reading(
    val id: Long = 0,
    val inverterId: Long,
    val timestamp: Instant,
    val todayKwh: Double?,
    val totalKwh: Double?,
    val powerW: Double?,
    val source: ReadingSource,
    val faultCode: String? = null,
    val note: String? = null,
    val confidence: Double? = null,
    val photoPath: String? = null
)

data class DailyYield(val date: LocalDate, val kwh: Double, val estimated: Boolean)

data class PeriodStats(
    val totalKwh: Double,
    val bestDay: DailyYield?,
    val averageKwh: Double,
    val specificYield: Double?,
    val daysWithData: Int
)

object Aggregation {

    /**
     * One energy figure per calendar day, summed over all inverters.
     * The day's highest "today" reading wins; without one, the rise of the lifetime total is used,
     * spread evenly over any days that were not scanned.
     */
    fun dailyYields(readings: List<Reading>, zone: ZoneId): List<DailyYield> {
        val perDay = mutableMapOf<LocalDate, Pair<Double, Boolean>>()
        readings.groupBy { it.inverterId }.forEach { (_, list) ->
            val byDate = list.groupBy { it.timestamp.atZone(zone).toLocalDate() }.toSortedMap()
            var prevDate: LocalDate? = null
            var prevTotal: Double? = null
            for ((date, dayReadings) in byDate) {
                val today = dayReadings.mapNotNull { it.todayKwh }.maxOrNull()
                val total = dayReadings.mapNotNull { it.totalKwh }.maxOrNull()
                if (today != null) {
                    add(perDay, date, today, false)
                } else if (total != null && prevTotal != null && prevDate != null && total >= prevTotal) {
                    val gap = ChronoUnit.DAYS.between(prevDate, date).toInt().coerceAtLeast(1)
                    val share = (total - prevTotal) / gap
                    for (i in 1..gap) add(perDay, prevDate.plusDays(i.toLong()), share, true)
                }
                if (total != null) {
                    // Use the end-of-day total; a mid-day "today" plus the total still lines up.
                    prevTotal = total
                    prevDate = date
                }
            }
        }
        return perDay.entries.sortedBy { it.key }.map { DailyYield(it.key, it.value.first, it.value.second) }
    }

    private fun add(map: MutableMap<LocalDate, Pair<Double, Boolean>>, date: LocalDate, kwh: Double, estimated: Boolean) {
        val old = map[date]
        map[date] = if (old == null) kwh to estimated else (old.first + kwh) to (old.second || estimated)
    }

    fun stats(days: List<DailyYield>, kwp: Double?): PeriodStats {
        val total = days.sumOf { it.kwh }
        val withData = days.filter { it.kwh > 0 }
        return PeriodStats(
            totalKwh = total,
            bestDay = withData.maxByOrNull { it.kwh },
            averageKwh = if (withData.isEmpty()) 0.0 else total / withData.size,
            specificYield = kwp?.takeIf { it > 0 }?.let { total / it },
            daysWithData = withData.size
        )
    }

    fun lastDays(days: List<DailyYield>, end: LocalDate, count: Int): List<DailyYield> {
        val map = days.associateBy { it.date }
        return (count - 1 downTo 0).map { back ->
            val d = end.minusDays(back.toLong())
            map[d] ?: DailyYield(d, 0.0, false)
        }
    }

    fun month(days: List<DailyYield>, month: YearMonth): List<DailyYield> {
        val map = days.associateBy { it.date }
        return (1..month.lengthOfMonth()).map { d ->
            val date = month.atDay(d)
            map[date] ?: DailyYield(date, 0.0, false)
        }
    }

    /** Twelve monthly totals for [year]. */
    fun year(days: List<DailyYield>, year: Int): List<Double> =
        (1..12).map { m -> days.filter { it.date.year == year && it.date.monthValue == m }.sumOf { it.kwh } }

    /** Average yearly loss of output, comparing the best days of each year (needs two years of data). */
    fun degradationPercentPerYear(days: List<DailyYield>): Double? {
        val byYear = days.groupBy { it.date.year }.filterValues { it.size >= 60 }
        if (byYear.size < 2) return null
        val bestShare = byYear.mapValues { (_, list) ->
            val sorted = list.map { it.kwh }.sortedDescending()
            sorted.take((sorted.size / 10).coerceAtLeast(5)).average()
        }.toSortedMap()
        val first = bestShare.entries.first()
        val last = bestShare.entries.last()
        val years = last.key - first.key
        if (years <= 0 || first.value <= 0) return null
        return ((first.value - last.value) / first.value * 100.0 / years)
    }
}

data class Tariff(
    val model: TariffModel,
    val buyPricePerKwh: Double,
    val exportPricePerKwh: Double,
    /** Share of solar energy used in the home, 0..1. Only used for feed-in and net billing. */
    val selfUseShare: Double
)

data class MoneySplit(val selfUsedKwh: Double, val exportedKwh: Double, val selfUsedValue: Double, val exportedValue: Double) {
    val total: Double get() = selfUsedValue + exportedValue
}

object Economics {
    fun value(kwh: Double, tariff: Tariff): MoneySplit = when (tariff.model) {
        TariffModel.NET_METERING -> MoneySplit(kwh * tariff.selfUseShare, kwh * (1 - tariff.selfUseShare), kwh * tariff.selfUseShare * tariff.buyPricePerKwh, kwh * (1 - tariff.selfUseShare) * tariff.buyPricePerKwh)
        TariffModel.NET_BILLING, TariffModel.FEED_IN -> {
            val self = kwh * tariff.selfUseShare
            val exported = kwh - self
            MoneySplit(self, exported, self * tariff.buyPricePerKwh, exported * tariff.exportPricePerKwh)
        }
    }

    fun co2Kg(kwh: Double, gramsPerKwh: Int): Double = kwh * gramsPerKwh / 1000.0

    /** Days until the system has paid for itself at the recent saving rate, or null if unknown. */
    fun daysToPayback(systemCost: Double?, savedSoFar: Double, recentDailySaving: Double): Long? {
        if (systemCost == null || systemCost <= 0 || recentDailySaving <= 0) return null
        val left = systemCost - savedSoFar
        if (left <= 0) return 0
        return kotlin.math.ceil(left / recentDailySaving).toLong()
    }
}

enum class IssueLevel { INFO, WARNING }

data class SanityIssue(val level: IssueLevel, val message: String)

/** Catches misreads before they are saved. */
object SanityChecker {
    fun check(
        todayKwh: Double?,
        totalKwh: Double?,
        powerW: Double?,
        systemKwp: Double?,
        previous: Reading?,
        clearSkyKwhToday: Double?,
        sameDay: Boolean
    ): List<SanityIssue> {
        val issues = mutableListOf<SanityIssue>()
        if (todayKwh == null && totalKwh == null && powerW == null) {
            issues += SanityIssue(IssueLevel.WARNING, "No values yet. Retake the photo or type them in.")
            return issues
        }
        if (previous?.totalKwh != null && totalKwh != null && totalKwh < previous.totalKwh - 0.05) {
            issues += SanityIssue(IssueLevel.WARNING, "Lifetime total is lower than last time (${fmt(previous.totalKwh)} kWh). A digit may be misread.")
        }
        if (sameDay && previous?.todayKwh != null && todayKwh != null && todayKwh < previous.todayKwh - 0.05) {
            issues += SanityIssue(IssueLevel.WARNING, "Today is lower than your earlier scan today (${fmt(previous.todayKwh)} kWh).")
        }
        if (systemKwp != null && systemKwp > 0) {
            if (todayKwh != null && clearSkyKwhToday != null && clearSkyKwhToday > 0 && todayKwh > clearSkyKwhToday * 1.35) {
                issues += SanityIssue(IssueLevel.WARNING, "Today is more than a ${fmt(systemKwp)} kWp system can make on a clear day (${fmt(clearSkyKwhToday)} kWh).")
            }
            if (powerW != null && powerW > systemKwp * 1000 * 1.25) {
                issues += SanityIssue(IssueLevel.WARNING, "Live power is above your system size. Check the unit (W or kW).")
            }
        }
        if (previous?.totalKwh != null && totalKwh != null && previous.todayKwh != null && todayKwh != null && sameDay) {
            val totalRise = totalKwh - previous.totalKwh
            val todayRise = todayKwh - previous.todayKwh
            if (totalRise >= 0 && kotlin.math.abs(totalRise - todayRise) <= 1.0 + todayRise * 0.1) {
                issues += SanityIssue(IssueLevel.INFO, "Total rose ${fmt(totalRise)} kWh since the last scan, matching Today.")
            }
        }
        return issues
    }

    private fun fmt(v: Double) = if (v >= 100) "%,.0f".format(v) else "%.1f".format(v)
}
