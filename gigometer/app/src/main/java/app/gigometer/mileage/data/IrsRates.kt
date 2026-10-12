package app.gigometer.mileage.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A business standard-mileage rate that applies from [start] until the next period starts. */
data class RatePeriod(val start: LocalDate, val dollarsPerMile: Double)

object IrsRates {
    /**
     * IRS business standard mileage rates. 2026 changed mid-year:
     * 72.5¢ for Jan 1 – Jun 30 and 76¢ from Jul 1 (IRS Announcement 2026-11).
     * Users can add new periods in Settings when the IRS publishes a new rate.
     */
    val DEFAULTS = listOf(
        RatePeriod(LocalDate.of(2023, 1, 1), 0.655),
        RatePeriod(LocalDate.of(2024, 1, 1), 0.67),
        RatePeriod(LocalDate.of(2025, 1, 1), 0.70),
        RatePeriod(LocalDate.of(2026, 1, 1), 0.725),
        RatePeriod(LocalDate.of(2026, 7, 1), 0.76),
    )

    fun encode(periods: List<RatePeriod>): String =
        periods.sortedBy { it.start }.joinToString(";") { "${it.start}=${it.dollarsPerMile}" }

    fun decode(raw: String?): List<RatePeriod> {
        if (raw.isNullOrBlank()) return DEFAULTS
        val parsed = raw.split(";").mapNotNull { part ->
            val (d, r) = part.split("=").takeIf { it.size == 2 } ?: return@mapNotNull null
            runCatching { RatePeriod(LocalDate.parse(d), r.toDouble()) }.getOrNull()
        }
        return parsed.ifEmpty { DEFAULTS }.sortedBy { it.start }
    }

    fun rateOn(date: LocalDate, periods: List<RatePeriod>): Double =
        periods.lastOrNull { !it.start.isAfter(date) }?.dollarsPerMile
            ?: periods.firstOrNull()?.dollarsPerMile ?: 0.0

    fun rateFor(trip: Trip, periods: List<RatePeriod>): Double =
        rateOn(Instant.ofEpochMilli(trip.startTime).atZone(ZoneId.systemDefault()).toLocalDate(), periods)

    fun deduction(trip: Trip, periods: List<RatePeriod>): Double =
        if (trip.isBusiness) trip.miles * rateFor(trip, periods) else 0.0
}
