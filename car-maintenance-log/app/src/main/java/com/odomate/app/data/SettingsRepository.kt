package com.odomate.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class AppSettings(
    val distanceUnit: String = "km",
    val volumeUnit: String = "L",
    val currency: String = "$",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val weeklyNudge: Boolean = true,
    val activeVehicleId: Long = 0,
    val lastNudgeAt: Long = 0,
    val launchCount: Int = 0,
    val ratePromptDone: Boolean = false,
) {
    val miles: Boolean get() = distanceUnit == "mi"
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val distance = stringPreferencesKey("distance_unit")
        val volume = stringPreferencesKey("volume_unit")
        val currency = stringPreferencesKey("currency")
        val theme = stringPreferencesKey("theme")
        val notifications = booleanPreferencesKey("notifications")
        val reminderHour = intPreferencesKey("reminder_hour")
        val weeklyNudge = booleanPreferencesKey("weekly_nudge")
        val activeVehicle = longPreferencesKey("active_vehicle")
        val lastNudge = longPreferencesKey("last_nudge")
        val launchCount = intPreferencesKey("launch_count")
        val rateDone = booleanPreferencesKey("rate_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            distanceUnit = p[Keys.distance] ?: "km",
            volumeUnit = p[Keys.volume] ?: "L",
            currency = p[Keys.currency] ?: "$",
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            notificationsEnabled = p[Keys.notifications] ?: true,
            reminderHour = p[Keys.reminderHour] ?: 9,
            weeklyNudge = p[Keys.weeklyNudge] ?: true,
            activeVehicleId = p[Keys.activeVehicle] ?: 0L,
            lastNudgeAt = p[Keys.lastNudge] ?: 0L,
            launchCount = p[Keys.launchCount] ?: 0,
            ratePromptDone = p[Keys.rateDone] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setDistanceUnit(unit: String) = context.dataStore.edit { it[Keys.distance] = unit }
    suspend fun setVolumeUnit(unit: String) = context.dataStore.edit { it[Keys.volume] = unit }
    suspend fun setCurrency(symbol: String) = context.dataStore.edit { it[Keys.currency] = symbol }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setNotifications(on: Boolean) = context.dataStore.edit { it[Keys.notifications] = on }
    suspend fun setReminderHour(hour: Int) = context.dataStore.edit { it[Keys.reminderHour] = hour }
    suspend fun setWeeklyNudge(on: Boolean) = context.dataStore.edit { it[Keys.weeklyNudge] = on }
    suspend fun setActiveVehicle(id: Long) = context.dataStore.edit { it[Keys.activeVehicle] = id }
    suspend fun setLastNudge(at: Long) = context.dataStore.edit { it[Keys.lastNudge] = at }
    suspend fun setRateDone() = context.dataStore.edit { it[Keys.rateDone] = true }
    suspend fun incrementLaunchCount() = context.dataStore.edit { it[Keys.launchCount] = (it[Keys.launchCount] ?: 0) + 1 }
}
