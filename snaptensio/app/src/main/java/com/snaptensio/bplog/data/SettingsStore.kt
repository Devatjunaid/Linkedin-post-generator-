package com.snaptensio.bplog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class Guideline(val label: String) { AHA("AHA/ACC 2017"), ESC("ESC/ESH") }
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val onboarded: Boolean = false,
    val activeProfileId: Long = 0,
    val guideline: Guideline = Guideline.AHA,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val crisisAlerts: Boolean = true,
    val emergencyNumber: String = defaultEmergencyNumber(),
    val doctorNumber: String = "",
)

/** 911 in North America, 112 elsewhere (112 is routed on GSM phones in most countries). Editable in Settings. */
fun defaultEmergencyNumber(): String = when (Locale.getDefault().country) {
    "US", "CA", "MX", "PR" -> "911"
    "GB" -> "999"
    "AU" -> "000"
    "IN" -> "112"
    "PK" -> "1122"
    else -> "112"
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val activeProfile = longPreferencesKey("active_profile")
        val guideline = stringPreferencesKey("guideline")
        val theme = stringPreferencesKey("theme")
        val crisisAlerts = booleanPreferencesKey("crisis_alerts")
        val emergency = stringPreferencesKey("emergency_number")
        val doctor = stringPreferencesKey("doctor_number")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboarded = p[Keys.onboarded] ?: false,
            activeProfileId = p[Keys.activeProfile] ?: 0,
            guideline = p[Keys.guideline]?.let { runCatching { Guideline.valueOf(it) }.getOrNull() } ?: Guideline.AHA,
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            crisisAlerts = p[Keys.crisisAlerts] ?: true,
            emergencyNumber = p[Keys.emergency] ?: defaultEmergencyNumber(),
            doctorNumber = p[Keys.doctor] ?: "",
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboarded() = context.dataStore.edit { it[Keys.onboarded] = true }
    suspend fun setActiveProfile(id: Long) = context.dataStore.edit { it[Keys.activeProfile] = id }
    suspend fun setGuideline(g: Guideline) = context.dataStore.edit { it[Keys.guideline] = g.name }
    suspend fun setTheme(t: ThemeMode) = context.dataStore.edit { it[Keys.theme] = t.name }
    suspend fun setCrisisAlerts(on: Boolean) = context.dataStore.edit { it[Keys.crisisAlerts] = on }
    suspend fun setEmergencyNumber(n: String) = context.dataStore.edit { it[Keys.emergency] = n.trim() }
    suspend fun setDoctorNumber(n: String) = context.dataStore.edit { it[Keys.doctor] = n.trim() }
}
