package com.superdevlabs.coldplunge.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class DayType { NONE, COLD, HEAT, CONTRAST }

data class WeekTotals(val coldSec: Int, val heatSec: Int, val sessions: Int)

data class LogStats(
    val totalSessions: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val mostColdInOneSessionSec: Int,
    val coldestTempC: Float?,
    val totalColdSec: Int,
    val totalHeatSec: Int,
) {
    companion object {
        val Empty = LogStats(0, 0, 0, 0, null, 0, 0)
    }
}

object Stats {

    fun dateOf(epochMs: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /** Weeks start on Monday (ISO-8601). */
    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekTotals(sessions: List<SessionRecord>, today: LocalDate, zone: ZoneId): WeekTotals {
        val start = weekStart(today)
        val end = start.plusDays(7)
        val inWeek = sessions.filter {
            val d = dateOf(it.startedAt, zone)
            !d.isBefore(start) && d.isBefore(end)
        }
        return WeekTotals(inWeek.sumOf { it.coldSec }, inWeek.sumOf { it.heatSec }, inWeek.size)
    }

    fun dayTypes(sessions: List<SessionRecord>, zone: ZoneId): Map<LocalDate, DayType> =
        sessions.groupBy { dateOf(it.startedAt, zone) }.mapValues { (_, list) ->
            val cold = list.any { it.coldSec > 0 }
            val heat = list.any { it.heatSec > 0 }
            when {
                cold && heat -> DayType.CONTRAST
                cold -> DayType.COLD
                heat -> DayType.HEAT
                else -> DayType.NONE
            }
        }

    /** Consecutive days with a session, ending today (or yesterday, if today has none yet). */
    fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun bestStreak(days: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days.sorted()) {
            run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    fun compute(sessions: List<SessionRecord>, today: LocalDate, zone: ZoneId): LogStats {
        if (sessions.isEmpty()) return LogStats.Empty
        val days = sessions.map { dateOf(it.startedAt, zone) }.toSet()
        return LogStats(
            totalSessions = sessions.size,
            currentStreak = currentStreak(days, today),
            bestStreak = bestStreak(days),
            mostColdInOneSessionSec = sessions.maxOf { it.coldSec },
            coldestTempC = sessions.filter { it.coldSec > 0 }.mapNotNull { it.coldTempC }.minOrNull(),
            totalColdSec = sessions.sumOf { it.coldSec },
            totalHeatSec = sessions.sumOf { it.heatSec },
        )
    }
}
