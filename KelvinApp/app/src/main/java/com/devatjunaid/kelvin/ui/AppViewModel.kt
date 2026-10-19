package com.devatjunaid.kelvin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.kelvin.data.Settings
import com.devatjunaid.kelvin.data.SettingsRepository.Keys
import com.devatjunaid.kelvin.domain.LogStats
import com.devatjunaid.kelvin.domain.PhaseType
import com.devatjunaid.kelvin.domain.Protocol
import com.devatjunaid.kelvin.domain.ProtocolPlanner
import com.devatjunaid.kelvin.domain.SessionRecord
import com.devatjunaid.kelvin.domain.Stats
import com.devatjunaid.kelvin.domain.WeekTotals
import com.devatjunaid.kelvin.graph
import com.devatjunaid.kelvin.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = app.graph

    /** Null until the first read from disk completes, so the UI can decide on onboarding. */
    val settings: StateFlow<Settings?> = graph.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val sessions: StateFlow<List<SessionRecord>> = graph.sessions.sessions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customProtocols: StateFlow<List<Protocol>> = graph.protocols.custom
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<LogStats> = sessions
        .map { Stats.compute(it, LocalDate.now(), ZoneId.systemDefault()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LogStats.Empty)

    val week: StateFlow<WeekTotals> = sessions
        .map { Stats.weekTotals(it, LocalDate.now(), ZoneId.systemDefault()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeekTotals(0, 0, 0))

    /** The protocol featured on Today: the user's favourite, else Nordic Classic. */
    val featuredProtocol: StateFlow<Protocol> = combine(settings, customProtocols) { s, custom ->
        val all = ProtocolPlanner.presets + custom
        all.firstOrNull { it.id == s?.favoriteProtocolId } ?: ProtocolPlanner.presets.first()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProtocolPlanner.presets.first())

    fun saveSession(record: SessionRecord) = viewModelScope.launch { graph.sessions.save(record) }

    fun deleteSession(id: Long) = viewModelScope.launch { graph.sessions.delete(id) }

    fun saveProtocol(protocol: Protocol, onSaved: (Protocol) -> Unit = {}) = viewModelScope.launch {
        val id = graph.protocols.save(protocol)
        val savedId = if (protocol.id > 0) protocol.id else id
        graph.settings.set(Keys.FavoriteProtocol, savedId)
        onSaved(protocol.copy(id = savedId, builtIn = false))
    }

    fun deleteProtocol(id: Long) = viewModelScope.launch {
        graph.protocols.delete(id)
        if (settings.value?.favoriteProtocolId == id) graph.settings.set(Keys.FavoriteProtocol, -1L)
    }

    fun setFavorite(protocol: Protocol) = viewModelScope.launch { graph.settings.set(Keys.FavoriteProtocol, protocol.id) }

    fun rememberQuickStart(type: PhaseType, seconds: Int, tempC: Float) = viewModelScope.launch {
        graph.settings.edit {
            if (type == PhaseType.COLD) {
                it[Keys.LastColdSec] = seconds
                it[Keys.LastColdTemp] = tempC
            } else {
                it[Keys.LastHeatSec] = seconds
                it[Keys.LastHeatTemp] = tempC
            }
        }
    }

    fun completeOnboarding() = viewModelScope.launch { graph.settings.set(Keys.OnboardingDone, true) }
    fun setFahrenheit(v: Boolean) = viewModelScope.launch { graph.settings.set(Keys.Fahrenheit, v) }
    fun setSound(v: Boolean) = viewModelScope.launch { graph.settings.set(Keys.Sound, v) }
    fun setVibration(v: Boolean) = viewModelScope.launch { graph.settings.set(Keys.Vibration, v) }
    fun setBreathingGuide(v: Boolean) = viewModelScope.launch { graph.settings.set(Keys.BreathingGuide, v) }
    fun setColdGoal(min: Int) = viewModelScope.launch { graph.settings.set(Keys.WeeklyColdGoal, min.coerceIn(1, 120)) }
    fun setHeatGoal(min: Int) = viewModelScope.launch { graph.settings.set(Keys.WeeklyHeatGoal, min.coerceIn(5, 600)) }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) = viewModelScope.launch {
        graph.settings.edit {
            it[Keys.ReminderEnabled] = enabled
            it[Keys.ReminderHour] = hour
            it[Keys.ReminderMinute] = minute
        }
        val context = getApplication<Application>()
        if (enabled) ReminderScheduler.schedule(context, hour, minute) else ReminderScheduler.cancel(context)
    }
}
