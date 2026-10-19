package com.devatjunaid.kelvin.domain

sealed interface TimerEvent {
    data class PhaseStarted(val index: Int, val phase: Phase) : TimerEvent
    data class Countdown(val secondsLeft: Int) : TimerEvent
    data class MinuteMark(val minutes: Int) : TimerEvent
    data object Finished : TimerEvent
}

/**
 * Wall-clock-independent session timer. All timing comes from [clock] (monotonic millis,
 * e.g. SystemClock.elapsedRealtime), so the timer stays exact when the app is paused,
 * backgrounded or the screen turns off: on the next [tick] it catches up and carries any
 * overflow into the following phases.
 */
class TimerEngine(
    val phases: List<Phase>,
    private val clock: () -> Long,
) {
    init {
        require(phases.isNotEmpty()) { "A session needs at least one phase" }
    }

    private val durationsMs = phases.map { it.durationSec * 1000L }.toMutableList()
    private val doneMs = LongArray(phases.size)
    private var accumulatedMs = 0L
    private var runningSince: Long? = null
    private var lastCountdown = -1
    private var lastMinute = 0

    var index: Int = 0
        private set
    var finished: Boolean = false
        private set
    val isRunning: Boolean get() = runningSince != null
    val currentPhase: Phase get() = phases[index.coerceAtMost(phases.lastIndex)]

    fun phaseDurationMs(i: Int = index): Long = durationsMs[i.coerceIn(0, phases.lastIndex)]

    fun phaseElapsedMs(): Long {
        if (finished) return 0L
        return accumulatedMs + (runningSince?.let { clock() - it } ?: 0L)
    }

    fun phaseRemainingMs(): Long =
        if (finished) 0L else (phaseDurationMs() - phaseElapsedMs()).coerceAtLeast(0L)

    fun totalPlannedMs(): Long = durationsMs.sum()

    fun totalElapsedMs(): Long {
        var sum = 0L
        for (i in 0 until index.coerceAtMost(phases.size)) sum += doneMs[i]
        return sum + phaseElapsedMs().coerceAtMost(phaseDurationMs())
    }

    /** 0..1 progress of each phase, for segmented progress bars. */
    fun segmentProgress(): List<Float> = phases.indices.map { i ->
        when {
            finished || i < index -> 1f
            i > index -> 0f
            else -> (phaseElapsedMs().toFloat() / phaseDurationMs()).coerceIn(0f, 1f)
        }
    }

    fun start() {
        if (!finished && runningSince == null) runningSince = clock()
    }

    fun pause() {
        runningSince?.let { accumulatedMs += clock() - it }
        runningSince = null
    }

    fun toggle() = if (isRunning) pause() else start()

    /** Extends the current phase. */
    fun addTime(seconds: Int) {
        if (!finished) durationsMs[index] = (durationsMs[index] + seconds * 1000L).coerceAtLeast(1000L)
    }

    /** Ends the current phase now and moves on, keeping the time actually done. */
    fun skipPhase(): List<TimerEvent> {
        if (finished) return emptyList()
        val elapsed = phaseElapsedMs().coerceAtMost(phaseDurationMs())
        return advance(doneForPhase = elapsed, overflowMs = 0L)
    }

    /** Stops the whole session early, keeping the time actually done. */
    fun finishNow(): List<TimerEvent> {
        if (finished) return emptyList()
        doneMs[index] = phaseElapsedMs().coerceAtMost(phaseDurationMs())
        finished = true
        runningSince = null
        accumulatedMs = 0L
        return listOf(TimerEvent.Finished)
    }

    fun tick(): List<TimerEvent> {
        val events = mutableListOf<TimerEvent>()
        while (!finished) {
            val elapsed = phaseElapsedMs()
            val duration = phaseDurationMs()
            if (elapsed < duration) break
            events += advance(doneForPhase = duration, overflowMs = elapsed - duration)
        }
        if (finished) return events

        val remainingMs = phaseRemainingMs()
        val secondsLeft = ((remainingMs + 999) / 1000).toInt()
        if (secondsLeft != lastCountdown && (secondsLeft in 1..3 || secondsLeft == 10)) {
            events += TimerEvent.Countdown(secondsLeft)
        }
        lastCountdown = secondsLeft

        val minutes = (phaseElapsedMs() / 60_000L).toInt()
        if (minutes > lastMinute) {
            lastMinute = minutes
            events += TimerEvent.MinuteMark(minutes)
        }
        return events
    }

    fun actualSecondsFor(type: PhaseType): Int =
        phases.indices.filter { phases[it].type == type }.sumOf { doneMs[it] }.let { (it / 1000L).toInt() }

    fun actualTotalSeconds(): Int = (doneMs.sum() / 1000L).toInt()

    /** Rounds in which at least part of the cold phase was done. */
    fun roundsCompleted(): Int = phases.indices
        .filter { phases[it].type == PhaseType.COLD && doneMs[it] > 0 }
        .map { phases[it].round }
        .distinct().size
        .let { if (it == 0 && doneMs.any { d -> d > 0 }) 1 else it }

    private fun advance(doneForPhase: Long, overflowMs: Long): List<TimerEvent> {
        val wasRunning = isRunning
        doneMs[index] = doneForPhase
        lastCountdown = -1
        lastMinute = 0
        if (index == phases.lastIndex) {
            finished = true
            runningSince = null
            accumulatedMs = 0L
            return listOf(TimerEvent.Finished)
        }
        index++
        accumulatedMs = overflowMs
        runningSince = if (wasRunning) clock() else null
        return listOf(TimerEvent.PhaseStarted(index, phases[index]))
    }
}
