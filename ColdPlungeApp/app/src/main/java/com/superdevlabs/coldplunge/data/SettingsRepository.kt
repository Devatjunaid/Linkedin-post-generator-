package com.superdevlabs.coldplunge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

data class Settings(
    val fahrenheit: Boolean = false,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val breathingGuide: Boolean = true,
    val weeklyColdGoalMin: Int = 11,
    val weeklyHeatGoalMin: Int = 57,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 30,
    val onboardingDone: Boolean = false,
    val lastColdSec: Int = 180,
    val lastColdTempC: Float = 4f,
    val lastHeatSec: Int = 15 * 60,
    val lastHeatTempC: Float = 85f,
    val favoriteProtocolId: Long = -1L,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    object Keys {
        val Fahrenheit = booleanPreferencesKey("fahrenheit")
        val Sound = booleanPreferencesKey("sound")
        val Vibration = booleanPreferencesKey("vibration")
        val BreathingGuide = booleanPreferencesKey("breathing_guide")
        val WeeklyColdGoal = intPreferencesKey("weekly_cold_goal_min")
        val WeeklyHeatGoal = intPreferencesKey("weekly_heat_goal_min")
        val ReminderEnabled = booleanPreferencesKey("reminder_enabled")
        val ReminderHour = intPreferencesKey("reminder_hour")
        val ReminderMinute = intPreferencesKey("reminder_minute")
        val OnboardingDone = booleanPreferencesKey("onboarding_done")
        val LastColdSec = intPreferencesKey("last_cold_sec")
        val LastColdTemp = floatPreferencesKey("last_cold_temp_c")
        val LastHeatSec = intPreferencesKey("last_heat_sec")
        val LastHeatTemp = floatPreferencesKey("last_heat_temp_c")
        val FavoriteProtocol = longPreferencesKey("favorite_protocol_id")
    }

    val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            val d = Settings()
            Settings(
                fahrenheit = p[Keys.Fahrenheit] ?: d.fahrenheit,
                sound = p[Keys.Sound] ?: d.sound,
                vibration = p[Keys.Vibration] ?: d.vibration,
                breathingGuide = p[Keys.BreathingGuide] ?: d.breathingGuide,
                weeklyColdGoalMin = p[Keys.WeeklyColdGoal] ?: d.weeklyColdGoalMin,
                weeklyHeatGoalMin = p[Keys.WeeklyHeatGoal] ?: d.weeklyHeatGoalMin,
                reminderEnabled = p[Keys.ReminderEnabled] ?: d.reminderEnabled,
                reminderHour = p[Keys.ReminderHour] ?: d.reminderHour,
                reminderMinute = p[Keys.ReminderMinute] ?: d.reminderMinute,
                onboardingDone = p[Keys.OnboardingDone] ?: d.onboardingDone,
                lastColdSec = p[Keys.LastColdSec] ?: d.lastColdSec,
                lastColdTempC = p[Keys.LastColdTemp] ?: d.lastColdTempC,
                lastHeatSec = p[Keys.LastHeatSec] ?: d.lastHeatSec,
                lastHeatTempC = p[Keys.LastHeatTemp] ?: d.lastHeatTempC,
                favoriteProtocolId = p[Keys.FavoriteProtocol] ?: d.favoriteProtocolId,
            )
        }

    suspend fun current(): Settings = settings.first()

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }
}
