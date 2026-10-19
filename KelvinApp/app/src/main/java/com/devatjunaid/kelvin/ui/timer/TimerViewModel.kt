package com.devatjunaid.kelvin.ui.timer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.kelvin.domain.Phase
import com.devatjunaid.kelvin.domain.PhaseType
import com.devatjunaid.kelvin.domain.SessionPlan
import com.devatjunaid.kelvin.domain.SessionRecord
import com.devatjunaid.kelvin.domain.TimerEngine
import com.devatjunaid.kelvin.domain.TimerEvent
import com.devatjunaid.kelvin.util.Cues
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUi(
    val sessionId: Long = -1L,
    val phases: List<Phase> = emptyList(),
    val index: Int = 0,
    val remainingMs: Long = 0L,
    val phaseDurationMs: Long = 1L,
    val phaseElapsedMs: Long = 0L,
    val totalElapsedMs: Long = 0L,
    val totalPlannedMs: Long = 1L,
    val running: Boolean = false,
    val finished: Boolean = false,
    val segments: List<Float> = emptyList(),
) {
    val phase: Phase? get() = phases.getOrNull(index)
    val nextPhase: Phase? get() = phases.getOrNull(index + 1)
    val phaseProgress: Float get() = (phaseElapsedMs.toFloat() / phaseDurationMs).coerceIn(0f, 1f)
    val roundsTotal: Int get() = phases.maxOfOrNull { it.round } ?: 1
}

/** Owns the running session so it survives rotation; ticks every 100 ms from the monotonic clock. */
class TimerViewModel(app: Application) : AndroidViewModel(app) {
    private val cues = Cues(app)
    private var engine: TimerEngine? = null
    private var plan: SessionPlan? = null
    private var sessionId: Long = -1L
    private var startedAtWallClock = 0L
    private var tickJob: Job? = null

    private val _ui = MutableStateFlow(TimerUi())
    val ui: StateFlow<TimerUi> = _ui.asStateFlow()

    /** Starts [plan] unless session [id] is already running (e.g. after a rotation). */
    fun begin(id: Long, plan: SessionPlan, sound: Boolean, vibration: Boolean) {
        cues.soundEnabled = sound
        cues.vibrationEnabled = vibration
        if (sessionId == id && engine != null) return
        tickJob?.cancel()
        sessionId = id
        this.plan = plan
        startedAtWallClock = System.currentTimeMillis()
        engine = TimerEngine(plan.phases) { SystemClock.elapsedRealtime() }.also { it.start() }
        publish()
        cues.phaseStart()
        startTicking()
    }

    /** Stops ticking and cues; called when the user leaves the session (saved or discarded). */
    fun end() {
        tickJob?.cancel()
        engine?.pause()
    }

    fun updateCuePrefs(sound: Boolean, vibration: Boolean) {
        cues.soundEnabled = sound
        cues.vibrationEnabled = vibration
    }

    fun toggle() {
        engine?.toggle()
        publish()
    }

    fun addThirtySeconds() {
        engine?.addTime(30)
        publish()
    }

    fun skipPhase() {
        val e = engine ?: return
        handle(e.skipPhase())
        publish()
    }

    fun finishNow() {
        val e = engine ?: return
        handle(e.finishNow(), silent = true)
        publish()
    }

    fun record(mood: Int?, note: String, coldTempC: Float?, heatTempC: Float?): SessionRecord? {
        val e = engine ?: return null
        val p = plan ?: return null
        val total = e.actualTotalSeconds()
        if (total <= 0) return null
        return SessionRecord(
            startedAt = startedAtWallClock,
            kind = p.kind,
            title = p.title,
            totalSec = total,
            coldSec = e.actualSecondsFor(PhaseType.COLD),
            heatSec = e.actualSecondsFor(PhaseType.HEAT),
            rounds = e.roundsCompleted(),
            coldTempC = if (e.actualSecondsFor(PhaseType.COLD) > 0) coldTempC else null,
            heatTempC = if (e.actualSecondsFor(PhaseType.HEAT) > 0) heatTempC else null,
            mood = mood,
            note = note.trim(),
        )
    }

    fun actualColdSeconds(): Int = engine?.actualSecondsFor(PhaseType.COLD) ?: 0
    fun actualHeatSeconds(): Int = engine?.actualSecondsFor(PhaseType.HEAT) ?: 0
    fun actualTotalSeconds(): Int = engine?.actualTotalSeconds() ?: 0
    fun roundsCompleted(): Int = engine?.roundsCompleted() ?: 0

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                val e = engine ?: break
                handle(e.tick())
                publish()
                if (e.finished) break
                delay(100)
            }
        }
    }

    private fun handle(events: List<TimerEvent>, silent: Boolean = false) {
        if (silent) return
        val phaseType = engine?.currentPhase?.type
        events.forEach { event ->
            when (event) {
                is TimerEvent.PhaseStarted -> cues.phaseStart()
                is TimerEvent.Countdown -> cues.countdown(event.secondsLeft)
                is TimerEvent.MinuteMark -> if (phaseType == PhaseType.COLD) cues.minuteMark()
                TimerEvent.Finished -> cues.finished()
            }
        }
    }

    private fun publish() {
        val e = engine ?: return
        _ui.value = TimerUi(
            sessionId = sessionId,
            phases = e.phases,
            index = e.index,
            remainingMs = e.phaseRemainingMs(),
            phaseDurationMs = e.phaseDurationMs().coerceAtLeast(1L),
            phaseElapsedMs = e.phaseElapsedMs().coerceAtMost(e.phaseDurationMs()),
            totalElapsedMs = e.totalElapsedMs(),
            totalPlannedMs = e.totalPlannedMs().coerceAtLeast(1L),
            running = e.isRunning,
            finished = e.finished,
            segments = e.segmentProgress(),
        )
    }

    override fun onCleared() {
        tickJob?.cancel()
        cues.release()
    }
}
