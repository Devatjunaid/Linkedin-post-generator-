package com.walkies.dogwalk.ui.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.Walk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalkSummary(val walk: Walk, val pee: Int, val poo: Int)

data class WalkState(
    val loaded: Boolean = false,
    val dogName: String = "",
    val photoUri: String? = null,
    val goalMinutes: Int = 30,
    val active: Walk? = null,
    val events: List<LogEntry> = emptyList(),
    /** Steps since the walk started, or null when steps aren't being counted. */
    val steps: Long? = null,
    val recent: List<WalkSummary> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class WalkViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    val stepCounterAvailable = container.stepCounter.isAvailable
    private val stepPermission = MutableStateFlow(false)

    private val events = repo.activeWalk.flatMapLatest { walk ->
        if (walk == null) flowOf(emptyList()) else repo.walkEntries(walk.id)
    }

    private val steps = combine(repo.activeWalk, stepPermission) { walk, granted -> walk to granted }
        .flatMapLatest { (walk, granted) ->
            if (walk == null || !granted || !stepCounterAvailable) {
                flowOf<Long?>(null)
            } else {
                container.stepCounter.totalSteps().map<Long, Long?> { total ->
                    val start = walk.startSteps
                    if (start == null || total < start) {
                        // First reading of this walk, or the phone rebooted mid-walk.
                        repo.setWalkStartSteps(walk, total)
                        0L
                    } else {
                        total - start
                    }
                }.onStart { emit(0L) }
            }
        }

    private val recent = combine(repo.recentWalks(), repo.walkCounts()) { walks, counts ->
        walks.map { w ->
            WalkSummary(
                walk = w,
                pee = counts.firstOrNull { it.walkId == w.id && it.type == LogType.PEE.name }?.count ?: 0,
                poo = counts.firstOrNull { it.walkId == w.id && it.type == LogType.POO.name }?.count ?: 0,
            )
        }
    }

    val state: StateFlow<WalkState> = combine(
        container.settings.settings,
        repo.activeWalk,
        events,
        steps,
        recent,
    ) { settings, active, events, steps, recent ->
        WalkState(
            loaded = true,
            dogName = settings.dogName,
            photoUri = settings.dogPhotoUri,
            goalMinutes = settings.walkGoalMinutes,
            active = active,
            events = events,
            steps = steps,
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalkState())

    fun onStepPermission(granted: Boolean) {
        stepPermission.value = granted
    }

    fun start() {
        viewModelScope.launch { repo.startWalk() }
    }

    fun log(type: LogType, onLogged: (LogEntry) -> Unit) {
        val walk = state.value.active ?: return
        viewModelScope.launch { onLogged(repo.log(type, walkId = walk.id)) }
    }

    fun undo(entry: LogEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    fun finish(onSaved: () -> Unit) {
        val current = state.value
        val walk = current.active ?: return
        viewModelScope.launch {
            repo.finishWalk(walk, current.steps)
            onSaved()
        }
    }

    fun discard() {
        val walk = state.value.active ?: return
        viewModelScope.launch { repo.deleteWalk(walk) }
    }
}
