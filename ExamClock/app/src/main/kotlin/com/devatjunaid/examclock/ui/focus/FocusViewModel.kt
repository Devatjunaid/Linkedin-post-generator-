package com.devatjunaid.examclock.ui.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.examclock.AppContainer
import com.devatjunaid.examclock.data.db.SessionEntity
import com.devatjunaid.examclock.data.prefs.FocusTimerState
import com.devatjunaid.examclock.data.prefs.UserPreferences
import com.devatjunaid.examclock.notifications.FocusAlarm
import com.devatjunaid.examclock.notifications.Notifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FocusOption(val sessionId: Long, val examId: Long?, val label: String, val minutes: Int)

data class FocusUiState(
    val loaded: Boolean = false,
    val timer: FocusTimerState = FocusTimerState(),
    val prefs: UserPreferences = UserPreferences(),
    val now: Long = System.currentTimeMillis(),
    val options: List<FocusOption> = emptyList(),
    val selectedSessionId: Long = 0L,
    val linkedLabel: String? = null,
    val linkedExamTitle: String? = null,
) {
    val phase: Int get() = timer.phase
    val isBreak: Boolean get() = timer.phase == FocusTimerState.PHASE_BREAK
    val running: Boolean get() = timer.isRunning
    val paused: Boolean get() = timer.isPaused

    /** Length of the block the user would start (or is running) now, in millis. */
    val totalMillis: Long
        get() = when {
            running || paused -> timer.blockMinutes * 60_000L
            isBreak -> breakMinutes * 60_000L
            else -> prefs.focusMinutes * 60_000L
        }

    val remainingMillis: Long
        get() = when {
            running -> (timer.endAt - now).coerceAtLeast(0L)
            paused -> timer.pausedRemaining
            else -> totalMillis
        }

    val breakMinutes: Int
        get() = if (timer.completedBlocks > 0 && timer.completedBlocks % prefs.blocksBeforeLongBreak.coerceAtLeast(1) == 0) prefs.longBreakMinutes else prefs.shortBreakMinutes
}

/**
 * Pomodoro-style timer. The end time is stored in DataStore, so the countdown stays correct if
 * the app is closed or killed; a finished focus block is logged exactly once.
 */
class FocusViewModel(
    private val container: AppContainer,
    context: Context,
    initialSessionId: Long?,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val prefsRepo = container.preferences
    private val repo = container.repository
    private val now = MutableStateFlow(System.currentTimeMillis())
    private val extras = MutableStateFlow(FocusUiState())

    private val _events = MutableStateFlow<Int?>(null)
    /** Non-null once after a focus block completes (value = blocks done today in this run). */
    val blockCompleted: StateFlow<Int?> = _events

    val state: StateFlow<FocusUiState> = combine(prefsRepo.timer, prefsRepo.preferences, now, extras) { timer, prefs, t, ex ->
        ex.copy(loaded = true, timer = timer, prefs = prefs, now = t)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    init {
        viewModelScope.launch {
            val today = repo.openSessionsOn(LocalDate.now())
            val exams = repo.observeExams().first().associateBy { it.id }
            val options = today.map { it.toOption(exams[it.examId]?.title) }
            val timer = prefsRepo.timer.first()
            val linkedId = when {
                timer.sessionId != 0L -> timer.sessionId
                initialSessionId != null && initialSessionId != 0L -> initialSessionId
                else -> 0L
            }
            val linked = if (linkedId != 0L) repo.session(linkedId) else null
            extras.value = FocusUiState(
                options = options,
                selectedSessionId = linked?.id ?: 0L,
                linkedLabel = linked?.title,
                linkedExamTitle = linked?.examId?.let { exams[it]?.title },
            )
        }
        viewModelScope.launch {
            while (isActive) {
                now.value = System.currentTimeMillis()
                val timer = prefsRepo.timer.first()
                if (timer.isRunning && now.value >= timer.endAt) finishBlock(timer)
                delay(250)
            }
        }
    }

    private fun SessionEntity.toOption(examTitle: String?) =
        FocusOption(id, examId, listOfNotNull(examTitle, title.takeIf { it != examTitle }).joinToString(" · "), durationMinutes)

    fun selectSession(option: FocusOption?) {
        val current = extras.value
        extras.value = current.copy(
            selectedSessionId = option?.sessionId ?: 0L,
            linkedLabel = option?.label,
            linkedExamTitle = null,
        )
    }

    fun start() = viewModelScope.launch {
        val s = state.value
        val nowMs = System.currentTimeMillis()
        val minutes = if (s.isBreak) s.breakMinutes else s.prefs.focusMinutes
        val option = s.options.firstOrNull { it.sessionId == s.selectedSessionId }
        val sessionId = if (s.isBreak) s.timer.sessionId else s.selectedSessionId
        val examId = if (s.isBreak) s.timer.examId else (option?.examId ?: s.timer.examId)
        val next = s.timer.copy(
            phase = if (s.isBreak) FocusTimerState.PHASE_BREAK else FocusTimerState.PHASE_FOCUS,
            endAt = nowMs + minutes * 60_000L,
            pausedRemaining = 0L,
            blockMinutes = minutes,
            sessionId = sessionId,
            examId = examId,
            blockStartedAt = nowMs,
        )
        prefsRepo.setTimer(next)
        FocusAlarm.schedule(appContext, next.endAt, isBreak = s.isBreak)
    }

    fun pause() = viewModelScope.launch {
        val t = prefsRepo.timer.first()
        if (!t.isRunning) return@launch
        prefsRepo.setTimer(t.copy(endAt = 0L, pausedRemaining = (t.endAt - System.currentTimeMillis()).coerceAtLeast(1_000L)))
        FocusAlarm.cancel(appContext)
    }

    fun resume() = viewModelScope.launch {
        val t = prefsRepo.timer.first()
        if (!t.isPaused) return@launch
        val endAt = System.currentTimeMillis() + t.pausedRemaining
        prefsRepo.setTimer(t.copy(endAt = endAt, pausedRemaining = 0L))
        FocusAlarm.schedule(appContext, endAt, isBreak = t.phase == FocusTimerState.PHASE_BREAK)
    }

    /** Ends the current block early. Focus time already spent (1 min or more) still counts. */
    fun skip() = viewModelScope.launch {
        val t = prefsRepo.timer.first()
        FocusAlarm.cancel(appContext)
        if (t.phase == FocusTimerState.PHASE_FOCUS && (t.isRunning || t.isPaused)) {
            logPartial(t)
            prefsRepo.setTimer(t.copy(phase = FocusTimerState.PHASE_BREAK, endAt = 0L, pausedRemaining = 0L, completedBlocks = t.completedBlocks + 1))
        } else {
            prefsRepo.setTimer(t.copy(phase = FocusTimerState.PHASE_IDLE, endAt = 0L, pausedRemaining = 0L))
        }
    }

    /** Stops everything and resets the cycle. */
    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        val t = prefsRepo.timer.first()
        FocusAlarm.cancel(appContext)
        if (t.phase == FocusTimerState.PHASE_FOCUS && (t.isRunning || t.isPaused)) logPartial(t)
        prefsRepo.setTimer(FocusTimerState())
        extras.value = extras.value.copy(selectedSessionId = 0L, linkedLabel = null, linkedExamTitle = null)
        onDone()
    }

    fun consumeEvent() {
        _events.value = null
    }

    private suspend fun logPartial(t: FocusTimerState) {
        val remaining = if (t.isRunning) t.endAt - System.currentTimeMillis() else t.pausedRemaining
        val spent = ((t.blockMinutes * 60_000L - remaining) / 60_000L).toInt()
        if (spent >= 1) repo.logFocus(t.examId.takeIf { it != 0L }, t.sessionId.takeIf { it != 0L }, t.blockStartedAt, spent)
    }

    private suspend fun finishBlock(t: FocusTimerState) {
        val wasFocus = t.phase == FocusTimerState.PHASE_FOCUS
        val next = if (wasFocus) {
            t.copy(phase = FocusTimerState.PHASE_BREAK, endAt = 0L, pausedRemaining = 0L, completedBlocks = t.completedBlocks + 1)
        } else {
            t.copy(phase = FocusTimerState.PHASE_FOCUS, endAt = 0L, pausedRemaining = 0L)
        }
        if (!prefsRepo.claimFinishedBlock(t.endAt, next)) return
        Notifications.cancel(appContext, Notifications.ID_FOCUS)
        if (wasFocus) {
            repo.logFocus(t.examId.takeIf { it != 0L }, t.sessionId.takeIf { it != 0L }, t.blockStartedAt, t.blockMinutes)
            _events.value = next.completedBlocks
        }
    }
}
