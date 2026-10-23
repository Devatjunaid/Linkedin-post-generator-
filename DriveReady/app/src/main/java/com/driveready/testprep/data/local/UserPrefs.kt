package com.driveready.testprep.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "driveready_prefs")

data class UserSettings(
    val onboardingDone: Boolean = false,
    val countryId: String? = null,
    val regionId: String? = null,
    /** Test date as epoch day, or null if not set. */
    val testDateEpochDay: Long? = null,
    val masteredSigns: Set<String> = emptySet(),
    val checklistDone: Set<String> = emptySet(),
    val oralConfident: Set<String> = emptySet(),
    val maneuversConfident: Set<String> = emptySet(),
    val syllabusDone: Set<String> = emptySet(),
    val streakDays: Int = 0,
    val lastActiveEpochDay: Long = 0
)

class UserPrefs(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val country = stringPreferencesKey("country_id")
        val region = stringPreferencesKey("region_id")
        val testDate = longPreferencesKey("test_date")
        val signs = stringSetPreferencesKey("mastered_signs")
        val checklist = stringSetPreferencesKey("checklist_done")
        val oral = stringSetPreferencesKey("oral_confident")
        val maneuvers = stringSetPreferencesKey("maneuvers_confident")
        val syllabus = stringSetPreferencesKey("syllabus_done")
        val streak = intPreferencesKey("streak_days")
        val lastActive = longPreferencesKey("last_active_day")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            onboardingDone = p[Keys.onboarding] ?: false,
            countryId = p[Keys.country],
            regionId = p[Keys.region],
            testDateEpochDay = p[Keys.testDate],
            masteredSigns = p[Keys.signs] ?: emptySet(),
            checklistDone = p[Keys.checklist] ?: emptySet(),
            oralConfident = p[Keys.oral] ?: emptySet(),
            maneuversConfident = p[Keys.maneuvers] ?: emptySet(),
            syllabusDone = p[Keys.syllabus] ?: emptySet(),
            streakDays = p[Keys.streak] ?: 0,
            lastActiveEpochDay = p[Keys.lastActive] ?: 0
        )
    }

    suspend fun setOnboardingDone() = context.dataStore.edit { it[Keys.onboarding] = true }

    suspend fun setCountry(countryId: String, regionId: String?) = context.dataStore.edit {
        it[Keys.country] = countryId
        if (regionId != null) it[Keys.region] = regionId else it.remove(Keys.region)
    }

    suspend fun setTestDate(epochDay: Long?) = context.dataStore.edit {
        if (epochDay != null) it[Keys.testDate] = epochDay else it.remove(Keys.testDate)
    }

    suspend fun toggleSign(id: String) = toggle(Keys.signs, id)
    suspend fun toggleChecklist(id: String) = toggle(Keys.checklist, id)
    suspend fun toggleOral(id: String) = toggle(Keys.oral, id)
    suspend fun toggleManeuver(id: String) = toggle(Keys.maneuvers, id)
    suspend fun toggleSyllabus(id: String) = toggle(Keys.syllabus, id)

    /** Updates the daily study streak; call once when the app opens. */
    suspend fun recordActiveDay(today: Long) = context.dataStore.edit {
        val last = it[Keys.lastActive] ?: 0L
        val streak = it[Keys.streak] ?: 0
        it[Keys.streak] = when (today - last) {
            0L -> streak.coerceAtLeast(1)
            1L -> streak + 1
            else -> 1
        }
        it[Keys.lastActive] = today
    }

    private suspend fun toggle(key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>, id: String) =
        context.dataStore.edit {
            val current = it[key] ?: emptySet()
            it[key] = if (id in current) current - id else current + id
        }
}
