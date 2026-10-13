package com.odomate.app.domain

import com.odomate.app.data.db.FuelEntry
import com.odomate.app.data.db.OdometerReading
import com.odomate.app.data.db.ServiceRecord
import java.time.YearMonth

data class MonthBar(val month: YearMonth, val distance: Int)

data class FuelEconomy(val average: Double?, val last: Double?)

object Stats {
    /** Distance driven per month for the last [months] months, attributing each delta to the later reading's month. */
    fun monthlyDistance(readings: List<OdometerReading>, months: Int = 12, now: Long = System.currentTimeMillis()): List<MonthBar> {
        val current = YearMonth.from(StatusCalculator.toLocalDate(now))
        val buckets = LinkedHashMap<YearMonth, Int>()
        for (i in months - 1 downTo 0) buckets[current.minusMonths(i.toLong())] = 0
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.date }.thenBy { it.value })
        var prevMax: Int? = null
        for (r in sorted) {
            val prev = prevMax
            if (prev != null && r.value > prev) {
                val ym = YearMonth.from(StatusCalculator.toLocalDate(r.date))
                if (buckets.containsKey(ym)) buckets[ym] = (buckets[ym] ?: 0) + (r.value - prev)
            }
            prevMax = if (prev == null) r.value else maxOf(prev, r.value)
        }
        return buckets.map { (k, v) -> MonthBar(k, v) }
    }

    fun distanceInYear(readings: List<OdometerReading>, year: Int): Int =
        monthlyDistanceForYear(readings, year)

    private fun monthlyDistanceForYear(readings: List<OdometerReading>, year: Int): Int {
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.date }.thenBy { it.value })
        var prevMax: Int? = null
        var total = 0
        for (r in sorted) {
            val prev = prevMax
            if (prev != null && r.value > prev && StatusCalculator.toLocalDate(r.date).year == year) {
                total += r.value - prev
            }
            prevMax = if (prev == null) r.value else maxOf(prev, r.value)
        }
        return total
    }

    fun spentInYear(records: List<ServiceRecord>, fuel: List<FuelEntry>, year: Int): Double =
        records.filter { StatusCalculator.toLocalDate(it.date).year == year }.sumOf { it.cost ?: 0.0 } +
            fuel.filter { StatusCalculator.toLocalDate(it.date).year == year }.sumOf { it.cost ?: 0.0 }

    /**
     * Full-tank to full-tank economy (distance per volume unit). Partial fills between two full fills
     * are added to the volume of the later full fill.
     */
    fun fuelEconomy(entries: List<FuelEntry>): FuelEconomy {
        val sorted = entries.sortedWith(compareBy<FuelEntry> { it.odometer }.thenBy { it.date })
        val results = mutableListOf<Double>()
        var lastFullOdo: Int? = null
        var volumeSince = 0.0
        for (e in sorted) {
            if (lastFullOdo == null) {
                if (e.fullTank) lastFullOdo = e.odometer
                continue
            }
            volumeSince += e.volume
            if (e.fullTank) {
                val dist = e.odometer - lastFullOdo
                if (dist > 0 && volumeSince > 0) results += dist / volumeSince
                lastFullOdo = e.odometer
                volumeSince = 0.0
            }
        }
        return FuelEconomy(results.takeIf { it.isNotEmpty() }?.average(), results.lastOrNull())
    }
}
