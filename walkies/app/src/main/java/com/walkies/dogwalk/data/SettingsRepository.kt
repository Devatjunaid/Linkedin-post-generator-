package com.walkies.dogwalk.data

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "walkies_settings")

data class Settings(
    val dogName: String,
    val dogPhotoUri: String?,
    val walkGoalMinutes: Int,
    val noPeeAlertEnabled: Boolean,
    val noPeeAlertHours: Int,
    val nudgeDismissedAt: Long,
)

class SettingsRepository(private val context: Context, private val defaultDogName: String) {

    private object Keys {
        val dogName = stringPreferencesKey("dog_name")
        val dogPhoto = stringPreferencesKey("dog_photo_uri")
        val walkGoal = intPreferencesKey("walk_goal_minutes")
        val noPeeEnabled = booleanPreferencesKey("no_pee_alert_enabled")
        val noPeeHours = intPreferencesKey("no_pee_alert_hours")
        val nudgeDismissedAt = longPreferencesKey("nudge_dismissed_at")
        val remindersSeeded = booleanPreferencesKey("reminders_seeded")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            dogName = p[Keys.dogName]?.takeIf { it.isNotBlank() } ?: defaultDogName,
            dogPhotoUri = p[Keys.dogPhoto],
            walkGoalMinutes = p[Keys.walkGoal] ?: 30,
            noPeeAlertEnabled = p[Keys.noPeeEnabled] ?: true,
            noPeeAlertHours = p[Keys.noPeeHours] ?: 6,
            nudgeDismissedAt = p[Keys.nudgeDismissedAt] ?: 0L,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setDogName(name: String) = context.dataStore.edit { it[Keys.dogName] = name.trim() }

    suspend fun setDogPhoto(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(Keys.dogPhoto) else it[Keys.dogPhoto] = uri
    }

    suspend fun setWalkGoal(minutes: Int) = context.dataStore.edit { it[Keys.walkGoal] = minutes }

    suspend fun setNoPeeAlert(enabled: Boolean, hours: Int) = context.dataStore.edit {
        it[Keys.noPeeEnabled] = enabled
        it[Keys.noPeeHours] = hours
    }

    suspend fun dismissNudge(now: Long) = context.dataStore.edit { it[Keys.nudgeDismissedAt] = now }

    /** Returns true exactly once, the first time it is called on a fresh install. */
    suspend fun claimReminderSeeding(): Boolean {
        var claimed = false
        context.dataStore.edit {
            if (it[Keys.remindersSeeded] != true) {
                it[Keys.remindersSeeded] = true
                claimed = true
            }
        }
        return claimed
    }
}
