package com.superdevlabs.coldplunge

import com.superdevlabs.coldplunge.domain.PhaseType
import com.superdevlabs.coldplunge.domain.Protocol
import com.superdevlabs.coldplunge.domain.ProtocolPlanner
import com.superdevlabs.coldplunge.domain.SessionKind
import com.superdevlabs.coldplunge.domain.SessionRecord
import com.superdevlabs.coldplunge.domain.Stats
import com.superdevlabs.coldplunge.domain.Units
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ProtocolAndStatsTest {

    @Test
    fun nordicClassicEndsOnCold() {
        val p = ProtocolPlanner.presets.first()
        val phases = ProtocolPlanner.phases(p)
        assertEquals(8, phases.size) // 3x(heat, cold) + 2 rests
        assertEquals(PhaseType.COLD, phases.last().type)
        val t = ProtocolPlanner.totals(phases)
        assertEquals(45 * 60, t.heatSec)
        assertEquals(9 * 60, t.coldSec)
        assertEquals(10 * 60, t.restSec)
    }

    @Test
    fun warmFinishAddsFinalHeat() {
        val p = Protocol(name = "x", heatSec = 600, coldSec = 60, restSec = 120, rounds = 2, endOnCold = false)
        val phases = ProtocolPlanner.phases(p)
        assertEquals(listOf(PhaseType.HEAT, PhaseType.COLD, PhaseType.REST, PhaseType.HEAT, PhaseType.COLD, PhaseType.REST, PhaseType.HEAT), phases.map { it.type })
    }

    @Test
    fun zeroRestIsDropped() {
        val p = Protocol(name = "x", heatSec = 600, coldSec = 60, restSec = 0, rounds = 3)
        assertEquals(6, ProtocolPlanner.phases(p).size)
    }

    private fun session(day: LocalDate, cold: Int, heat: Int, temp: Float? = 4f) = SessionRecord(
        startedAt = day.atTime(8, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
        kind = SessionKind.CONTRAST, title = "t", totalSec = cold + heat, coldSec = cold, heatSec = heat,
        rounds = 1, coldTempC = temp, heatTempC = 85f, mood = null, note = "",
    )

    @Test
    fun streaksAndWeekTotals() {
        val today = LocalDate.of(2026, 10, 19) // Monday
        val sessions = listOf(
            session(today, 120, 900),
            session(today.minusDays(1), 180, 0, 2f),
            session(today.minusDays(2), 0, 1200),
            session(today.minusDays(5), 60, 0),
            session(today.minusDays(6), 60, 0),
            session(today.minusDays(7), 60, 0),
        )
        val stats = Stats.compute(sessions, today, ZoneOffset.UTC)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(2f, stats.coldestTempC)
        val week = Stats.weekTotals(sessions, today, ZoneOffset.UTC)
        assertEquals(1, week.sessions) // week starts Monday the 19th
        assertEquals(120, week.coldSec)
    }

    @Test
    fun streakSurvivesUntilEndOfToday() {
        val today = LocalDate.of(2026, 10, 19)
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, Stats.currentStreak(days, today))
        assertEquals(0, Stats.currentStreak(setOf(today.minusDays(2)), today))
    }

    @Test
    fun formatting() {
        assertEquals("3:00", Units.clock(180))
        assertEquals("1:02:05", Units.clock(3725))
        assertEquals("2m 30s", Units.duration(150))
        assertEquals("39°F", Units.temp(4f, true))
        assertEquals("3.5°C", Units.temp(3.5f, false))
        assertEquals("4°C", Units.temp(4f, false))
    }
}
