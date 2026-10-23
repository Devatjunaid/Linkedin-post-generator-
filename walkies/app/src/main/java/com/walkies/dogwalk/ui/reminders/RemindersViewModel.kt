package com.walkies.dogwalk.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** Suggestion drawn from the dog's own log: average time between pees. */
data class Nudge(val averageGapMillis: Long, val suggestedHours: Int)

data class RemindersState(
    val loaded: Boolean = false,
    val dogName: String = "",
    val reminders: List<Reminder> = emptyList(),
    val noPeeEnabled: Boolean = true,
    val noPeeHours: Int = 6,
    val nudge: Nudge? = null,
)

class RemindersViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val since = System.currentTimeMillis() - 14 * DAY

    val state: StateFlow<RemindersState> = combine(
        repo.reminders,
        container.settings.settings,
        repo.entriesBetween(since, Long.MAX_VALUE),
    ) { reminders, settings, entries ->
        val nudge = computeNudge(entries)?.takeIf { n ->
            System.currentTimeMillis() - settings.nudgeDismissedAt > 7 * DAY &&
                !(settings.noPeeAlertEnabled && settings.noPeeAlertHours == n.suggestedHours)
        }
        RemindersState(
            loaded = true,
            dogName = settings.dogName,
            reminders = reminders,
            noPeeEnabled = settings.noPeeAlertEnabled,
            noPeeHours = settings.noPeeAlertHours,
            nudge = nudge,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState())

    fun toggle(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch { repo.saveReminder(reminder.copy(enabled = enabled)) }
    }

    fun save(reminder: Reminder) {
        viewModelScope.launch { repo.saveReminder(reminder) }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch { repo.deleteReminder(reminder) }
    }

    fun setNoPee(enabled: Boolean, hours: Int) {
        viewModelScope.launch {
            container.settings.setNoPeeAlert(enabled, hours)
            container.scheduler.scheduleNoPeeAlert()
        }
    }

    fun acceptNudge(nudge: Nudge) = setNoPee(true, nudge.suggestedHours)

    fun dismissNudge() {
        viewModelScope.launch { container.settings.dismissNudge(System.currentTimeMillis()) }
    }

    /** Pee breaks used for the nudge; overnight gaps (over 10 h) are ignored. */
    private fun computeNudge(entries: List<LogEntry>): Nudge? {
        val pees = entries.filter { it.type == LogType.PEE }.map { it.timestamp }.sorted()
        val gaps = pees.zipWithNext { a, b -> b - a }.filter { it in 20 * MINUTE..10 * HOUR }
        if (gaps.size < MIN_GAPS) return null
        val avg = gaps.average().toLong()
        val suggested = ceil((avg + 30 * MINUTE) / HOUR.toDouble()).toInt().coerceIn(2, 12)
        return Nudge(avg, suggested)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR
        const val MIN_GAPS = 6
    }
}
