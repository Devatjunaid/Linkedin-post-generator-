package com.sunarc.inverterlogger.domain

import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

data class Dashboard(
    val today: LocalDate,
    val days: List<DailyYield>,
    val todayKwh: Double?,
    val livePowerW: Double?,
    val lifetimeKwh: Double?,
    val lastReading: Reading?,
    /** Clear-sky output shape for today, scaled 0..1, for the Sun Arc. */
    val curve: List<Float>,
    val dayProgress: Float,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val clearSkyKwh: Double?,
    val expectedBySunset: Double?,
    val healthPercent: Int?,
    val healthTitle: String,
    val healthNote: String,
    val todayMoney: Double?,
    val month: YearMonth,
    val monthStats: PeriodStats,
    val monthMoney: MoneySplit?,
    val savedSoFar: Double?,
    val paybackFraction: Float?,
    val paybackDate: LocalDate?,
    val co2TodayKg: Double?,
    val co2LifetimeKg: Double?,
    val degradationPerYear: Double?,
    val faultsThisMonth: Map<String, Int>
) {
    val hasData: Boolean get() = days.isNotEmpty() || lastReading != null
}

object DashboardBuilder {

    fun build(profile: SystemProfile, readings: List<Reading>, now: ZonedDateTime): Dashboard {
        val zone = profile.zone
        val today = now.toLocalDate()
        val days = Aggregation.dailyYields(readings, zone)
        val todayReadings = readings.filter { it.timestamp.atZone(zone).toLocalDate() == today }
        val todayKwh = days.firstOrNull { it.date == today }?.kwh?.takeIf { todayReadings.isNotEmpty() || it > 0 }

        val recentCutoff = now.toInstant().minus(Duration.ofHours(2))
        val livePower = todayReadings.filter { it.powerW != null && it.timestamp.isAfter(recentCutoff) }
            .groupBy { it.inverterId }.values.mapNotNull { list -> list.maxByOrNull { it.timestamp }?.powerW }
            .takeIf { it.isNotEmpty() }?.sum()

        val lifetime = readings.filter { it.totalKwh != null }.groupBy { it.inverterId }
            .values.mapNotNull { list -> list.maxByOrNull { it.timestamp }?.totalKwh }
            .takeIf { it.isNotEmpty() }?.sum()

        val pv = profile.pvSystem
        val shapeSystem = pv ?: PvSystem(1.0, profile.tiltDeg, profile.azimuthDeg, profile.lat, profile.lon)
        val curveDay = SolarMath.clearSkyDay(shapeSystem, today, zone)
        val peak = curveDay.points.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
        val curve = curveDay.points.map { (it.second / peak).toFloat() }
        val progress = SolarMath.dayProgress(now, curveDay)

        val last = readings.maxByOrNull { it.timestamp }
        val lastToday = todayReadings.filter { it.todayKwh != null }.maxByOrNull { it.timestamp }
        val expected = if (pv != null && lastToday?.todayKwh != null) {
            val share = SolarMath.expectedShareSoFar(lastToday.timestamp.atZone(zone), curveDay)
            if (share in 0.15..0.97) lastToday.todayKwh / share else null
        } else null

        val (healthPercent, title, note) = health(profile, days, today)

        val tariffOk = profile.pricesSet
        val month = YearMonth.from(today)
        val monthDays = Aggregation.month(days, month)
        val monthStats = Aggregation.stats(monthDays, pv?.kwp)
        val monthMoney = if (tariffOk) Economics.value(monthStats.totalKwh, profile.tariff) else null

        val producedForPayback = lifetime ?: days.sumOf { it.kwh }.takeIf { it > 0 }
        val saved = if (tariffOk && producedForPayback != null) Economics.value(producedForPayback, profile.tariff).total else null
        val cost = profile.systemCost
        val paybackFraction = if (saved != null && cost != null && cost > 0) (saved / cost).toFloat().coerceIn(0f, 1f) else null
        val lastYear = days.filter { it.date.isAfter(today.minusDays(365)) }
        val recentDaily = if (tariffOk && lastYear.size >= 7) {
            Economics.value(lastYear.sumOf { it.kwh }, profile.tariff).total / lastYear.size
        } else 0.0
        val paybackDays = if (saved != null) Economics.daysToPayback(cost, saved, recentDaily) else null

        val faults = readings.filter { it.faultCode != null && YearMonth.from(it.timestamp.atZone(zone)) == month }
            .groupingBy { it.faultCode!! }.eachCount()

        return Dashboard(
            today = today,
            days = days,
            todayKwh = todayKwh,
            livePowerW = livePower,
            lifetimeKwh = lifetime,
            lastReading = last,
            curve = curve,
            dayProgress = progress,
            sunrise = curveDay.sunrise,
            sunset = curveDay.sunset,
            clearSkyKwh = pv?.let { curveDay.clearSkyKwh },
            expectedBySunset = expected,
            healthPercent = healthPercent,
            healthTitle = title,
            healthNote = note,
            todayMoney = if (tariffOk && todayKwh != null) Economics.value(todayKwh, profile.tariff).total else null,
            month = month,
            monthStats = monthStats,
            monthMoney = monthMoney,
            savedSoFar = saved,
            paybackFraction = paybackFraction,
            paybackDate = paybackDays?.let { today.plusDays(it) },
            co2TodayKg = todayKwh?.let { Economics.co2Kg(it, profile.effectiveCo2) },
            co2LifetimeKg = producedForPayback?.let { Economics.co2Kg(it, profile.effectiveCo2) },
            degradationPerYear = Aggregation.degradationPercentPerYear(days),
            faultsThisMonth = faults
        )
    }

    /**
     * Compares the best recent days with what the system would make under a clear sky. The best
     * quarter of days stands in for "sunny days", so cloudy weather does not count as a fault.
     */
    fun health(profile: SystemProfile, days: List<DailyYield>, today: LocalDate): Triple<Int?, String, String> {
        val pv = profile.pvSystem ?: return Triple(null, "Add your system size", "Enter your kWp in settings to compare output with what your panels should make.")
        val recent = days.filter { !it.estimated && it.kwh > 0 && it.date.isAfter(today.minusDays(45)) && it.date != today }
        if (recent.size < 4) return Triple(null, "Learning your system", "Scan on at least 4 days to see how your system is performing.")
        val ratios = recent.map { d ->
            val clear = SolarMath.clearSkyDay(pv, d.date, profile.zone, stepMinutes = 20).clearSkyKwh
            if (clear > 0) d.kwh / clear else 0.0
        }.sortedDescending()
        val top = ratios.take((ratios.size / 4).coerceAtLeast(2)).average()
        val pct = (top * 100).toInt().coerceIn(0, 130)
        return when {
            pct >= 85 -> Triple(pct, "Healthy", "Sunny days are producing what a clean, unshaded ${"%.1f".format(pv.kwp)} kWp system should.")
            pct >= 70 -> Triple(pct, "Healthy, slightly low", "Sunny days are a bit below expected. Dust, leaves, new shade or a wrong tilt/direction in settings are the usual causes.")
            pct >= 50 -> Triple(pct, "Below expected", "Even sunny days are well below expected. Check for fault codes, a tripped string or shading, and that your kWp is right.")
            else -> Triple(pct, "Much lower than expected", "Output is far below what the panels should make. Check the inverter for faults and contact your installer if it continues.")
        }
    }
}
