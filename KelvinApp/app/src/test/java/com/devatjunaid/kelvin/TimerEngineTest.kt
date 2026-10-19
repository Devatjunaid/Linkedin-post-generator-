package com.devatjunaid.kelvin

import com.devatjunaid.kelvin.domain.Phase
import com.devatjunaid.kelvin.domain.PhaseType
import com.devatjunaid.kelvin.domain.TimerEngine
import com.devatjunaid.kelvin.domain.TimerEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {
    private var now = 0L
    private val phases = listOf(
        Phase(PhaseType.HEAT, 60, 85f, 1),
        Phase(PhaseType.COLD, 30, 4f, 1),
        Phase(PhaseType.REST, 20, null, 1),
    )

    private fun engine() = TimerEngine(phases) { now }

    @Test
    fun countsDownAndAdvances() {
        val e = engine()
        e.start()
        now = 59_000
        assertTrue(e.tick().isEmpty() || e.index == 0)
        assertEquals(1_000, e.phaseRemainingMs())
        now = 60_000
        val events = e.tick()
        assertTrue(events.first() is TimerEvent.PhaseStarted)
        assertEquals(1, e.index)
        assertEquals(30_000, e.phaseRemainingMs())
    }

    @Test
    fun catchesUpAfterBackgroundingAndCarriesOverflow() {
        val e = engine()
        e.start()
        now = 95_000 // 60 heat + 30 cold done, 5 s into rest
        val events = e.tick()
        assertEquals(2, events.count { it is TimerEvent.PhaseStarted })
        assertEquals(2, e.index)
        assertEquals(15_000, e.phaseRemainingMs())
        assertEquals(60, e.actualSecondsFor(PhaseType.HEAT))
        assertEquals(30, e.actualSecondsFor(PhaseType.COLD))
    }

    @Test
    fun pauseFreezesTime() {
        val e = engine()
        e.start()
        now = 10_000
        e.pause()
        now = 500_000
        e.tick()
        assertEquals(0, e.index)
        assertEquals(50_000, e.phaseRemainingMs())
        assertFalse(e.isRunning)
        e.start()
        now = 510_000
        e.tick()
        assertEquals(40_000, e.phaseRemainingMs())
    }

    @Test
    fun skipKeepsOnlyTimeDone() {
        val e = engine()
        e.start()
        now = 20_000
        e.skipPhase()
        assertEquals(1, e.index)
        assertEquals(20, e.actualSecondsFor(PhaseType.HEAT))
        assertTrue(e.isRunning)
    }

    @Test
    fun addTimeExtendsPhase() {
        val e = engine()
        e.start()
        e.addTime(30)
        now = 80_000
        e.tick()
        assertEquals(0, e.index)
        assertEquals(10_000, e.phaseRemainingMs())
        assertEquals(140_000, e.totalPlannedMs())
    }

    @Test
    fun finishesAndReportsTotals() {
        val e = engine()
        e.start()
        now = 200_000
        val events = e.tick()
        assertTrue(events.last() is TimerEvent.Finished)
        assertTrue(e.finished)
        assertEquals(110, e.actualTotalSeconds())
        assertEquals(1, e.roundsCompleted())
    }

    @Test
    fun countdownFiresOncePerSecond() {
        val e = engine()
        e.start()
        now = 57_100
        assertEquals(listOf<TimerEvent>(TimerEvent.Countdown(3)), e.tick())
        now = 57_500
        assertTrue(e.tick().isEmpty())
        now = 58_100
        assertEquals(listOf<TimerEvent>(TimerEvent.Countdown(2)), e.tick())
    }

    @Test
    fun finishNowKeepsPartialTime() {
        val e = engine()
        e.start()
        now = 75_000
        e.tick()
        e.finishNow()
        assertTrue(e.finished)
        assertEquals(60, e.actualSecondsFor(PhaseType.HEAT))
        assertEquals(15, e.actualSecondsFor(PhaseType.COLD))
    }
}
