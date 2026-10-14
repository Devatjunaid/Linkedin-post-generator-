package com.devatjunaid.renewalert.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "renewalert_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val appLock = booleanPreferencesKey("app_lock")
        val hide = booleanPreferencesKey("hide_amounts")
        val shots = booleanPreferencesKey("block_screenshots")
        val renewals = booleanPreferencesKey("renewal_reminders")
        val trials = booleanPreferencesKey("trial_alerts")
        val digest = booleanPreferencesKey("weekly_digest")
        val reminderDays = intPreferencesKey("default_reminder_days")
        val currency = stringPreferencesKey("currency")
        val budget = longPreferencesKey("budget_minor")
        val yearly = booleanPreferencesKey("show_yearly")
        val theme = stringPreferencesKey("theme")
        val rates = stringPreferencesKey("rates")
        val lastBackup = longPreferencesKey("last_backup")
        val lastRun = longPreferencesKey("last_reminder_run_day")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val d = AppSettings()
            AppSettings(
                onboardingDone = p[K.onboarding] ?: d.onboardingDone,
                appLock = p[K.appLock] ?: d.appLock,
                hideAmounts = p[K.hide] ?: d.hideAmounts,
                blockScreenshots = p[K.shots] ?: d.blockScreenshots,
                renewalReminders = p[K.renewals] ?: d.renewalReminders,
                trialAlerts = p[K.trials] ?: d.trialAlerts,
                weeklyDigest = p[K.digest] ?: d.weeklyDigest,
                defaultReminderDays = p[K.reminderDays] ?: d.defaultReminderDays,
                currency = p[K.currency] ?: d.currency,
                budgetMinor = p[K.budget] ?: d.budgetMinor,
                showYearly = p[K.yearly] ?: d.showYearly,
                theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
                rates = decodeRates(p[K.rates]),
                lastBackupMillis = p[K.lastBackup] ?: d.lastBackupMillis,
                lastReminderRunDay = p[K.lastRun] ?: d.lastReminderRunDay,
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[K.onboarding] = v }
    suspend fun setAppLock(v: Boolean) = context.dataStore.edit { it[K.appLock] = v }
    suspend fun setHideAmounts(v: Boolean) = context.dataStore.edit { it[K.hide] = v }
    suspend fun setBlockScreenshots(v: Boolean) = context.dataStore.edit { it[K.shots] = v }
    suspend fun setRenewalReminders(v: Boolean) = context.dataStore.edit { it[K.renewals] = v }
    suspend fun setTrialAlerts(v: Boolean) = context.dataStore.edit { it[K.trials] = v }
    suspend fun setWeeklyDigest(v: Boolean) = context.dataStore.edit { it[K.digest] = v }
    suspend fun setDefaultReminderDays(v: Int) = context.dataStore.edit { it[K.reminderDays] = v }
    suspend fun setCurrency(v: String) = context.dataStore.edit { it[K.currency] = v }
    suspend fun setBudgetMinor(v: Long) = context.dataStore.edit { it[K.budget] = v }
    suspend fun setShowYearly(v: Boolean) = context.dataStore.edit { it[K.yearly] = v }
    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[K.theme] = v.name }
    suspend fun setRates(v: Map<String, Double>) = context.dataStore.edit { it[K.rates] = encodeRates(v) }
    suspend fun setLastBackup(v: Long) = context.dataStore.edit { it[K.lastBackup] = v }
    suspend fun setLastReminderRunDay(v: Long) = context.dataStore.edit { it[K.lastRun] = v }

    /** Restores everything except security/onboarding flags. */
    suspend fun restore(s: AppSettings) = context.dataStore.edit {
        it[K.renewals] = s.renewalReminders
        it[K.trials] = s.trialAlerts
        it[K.digest] = s.weeklyDigest
        it[K.reminderDays] = s.defaultReminderDays
        it[K.currency] = s.currency
        it[K.budget] = s.budgetMinor
        it[K.yearly] = s.showYearly
        it[K.theme] = s.theme.name
        it[K.rates] = encodeRates(s.rates)
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }

    companion object {
        fun encodeRates(m: Map<String, Double>): String =
            m.entries.joinToString(";") { "${it.key}=${it.value}" }

        fun decodeRates(s: String?): Map<String, Double> =
            s.orEmpty().split(';').mapNotNull { part ->
                val kv = part.split('=')
                if (kv.size != 2) return@mapNotNull null
                val v = kv[1].toDoubleOrNull() ?: return@mapNotNull null
                kv[0] to v
            }.toMap()
    }
}
