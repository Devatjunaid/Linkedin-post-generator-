package com.walkies.dogwalk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.Reminder
import com.walkies.dogwalk.data.ReminderKind
import com.walkies.dogwalk.data.Walk
import com.walkies.dogwalk.reminders.ReminderScheduler
import com.walkies.dogwalk.util.startMillis
import com.walkies.dogwalk.util.toLocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NextWalk(val label: String, val at: Long)

data class HomeState(
    val loaded: Boolean = false,
    val dogName: String = "",
    val photoUri: String? = null,
    val now: Long = System.currentTimeMillis(),
    val nextWalk: NextWalk? = null,
    val lastPeeAt: Long? = null,
    val activeWalk: Walk? = null,
    val todayEntries: List<LogEntry> = emptyList(),
    val todayWalks: List<Walk> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    private val today = clock.map { it.toLocalDate() }.distinctUntilChanged()

    private val day = today.flatMapLatest { date ->
        val from = date.startMillis()
        val to = date.plusDays(1).startMillis()
        combine(repo.entriesBetween(from, to), repo.walksBetween(from, to)) { entries, walks -> entries to walks }
    }

    private val base = combine(
        container.settings.settings,
        repo.reminders,
        repo.latest(LogType.PEE),
        repo.activeWalk,
        day,
    ) { settings, reminders, lastPee, active, (entries, walks) ->
        HomeState(
            loaded = true,
            dogName = settings.dogName,
            photoUri = settings.dogPhotoUri,
            lastPeeAt = lastPee?.timestamp,
            activeWalk = active,
            todayEntries = entries,
            todayWalks = walks,
            nextWalk = null,
        ) to reminders
    }

    val state: StateFlow<HomeState> = combine(base, clock) { (s, reminders), now ->
        s.copy(now = now, nextWalk = nextWalk(reminders, now))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun log(type: LogType, onLogged: (LogEntry) -> Unit) {
        viewModelScope.launch { onLogged(repo.log(type)) }
    }

    fun undo(entry: LogEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    fun setPhoto(uri: String) {
        viewModelScope.launch { container.settings.setDogPhoto(uri) }
    }

    fun startWalk(onStarted: () -> Unit) {
        viewModelScope.launch {
            repo.startWalk()
            onStarted()
        }
    }

    private fun nextWalk(reminders: List<Reminder>, now: Long): NextWalk? =
        reminders.asSequence()
            .filter { it.enabled && it.kind == ReminderKind.WALK }
            .mapNotNull { r -> ReminderScheduler.nextTrigger(r, now)?.let { NextWalk(r.label, it) } }
            .minByOrNull { it.at }
}
