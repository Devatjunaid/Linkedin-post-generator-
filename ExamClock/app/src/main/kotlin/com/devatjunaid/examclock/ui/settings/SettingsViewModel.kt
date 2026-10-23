package com.devatjunaid.examclock.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.examclock.AppContainer
import com.devatjunaid.examclock.data.prefs.FocusTimerState
import com.devatjunaid.examclock.data.prefs.ThemeMode
import com.devatjunaid.examclock.data.prefs.UserPreferences
import com.devatjunaid.examclock.notifications.FocusAlarm
import com.devatjunaid.examclock.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer, context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val prefs = container.preferences

    val state: StateFlow<UserPreferences?> = prefs.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val privacyOptionsRequired = container.ads.privacyOptionsRequired

    fun setName(v: String) = viewModelScope.launch { prefs.setUserName(v.take(40)) }
    fun setCountry(code: String) = viewModelScope.launch { prefs.setCountry(code) }
    fun setGoal(minutes: Int) = viewModelScope.launch { prefs.setDailyGoal(minutes) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setTheme(mode) }
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { prefs.setKeepScreenOn(v) }

    fun setReminders(enabled: Boolean) = viewModelScope.launch {
        prefs.setReminders(enabled)
        ReminderScheduler.schedule(appContext, enabled, prefs.current().reminderMinuteOfDay)
    }

    fun setReminderMinute(minute: Int) = viewModelScope.launch {
        prefs.setReminderMinute(minute)
        val p = prefs.current()
        ReminderScheduler.schedule(appContext, p.remindersEnabled, minute)
    }

    fun setFocus(focus: Int, shortBreak: Int, longBreak: Int) = viewModelScope.launch {
        prefs.setFocusDurations(focus.coerceIn(5, 120), shortBreak.coerceIn(1, 30), longBreak.coerceIn(5, 60))
    }

    /** Deletes every exam, topic, session and log and returns to first-run setup. */
    fun resetAll(onDone: () -> Unit) = viewModelScope.launch {
        FocusAlarm.cancel(appContext)
        ReminderScheduler.schedule(appContext, false, 0)
        container.repository.clearAll()
        prefs.setTimer(FocusTimerState())
        prefs.clearAll()
        onDone()
    }
}
