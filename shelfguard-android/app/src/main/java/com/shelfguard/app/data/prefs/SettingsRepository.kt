package com.shelfguard.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val shopName: String = "My Shop",
    val currency: String = "Rs",
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val defaultAlertDays: Int = 7,
    val defaultMinStock: Int = 5,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val shopName = stringPreferencesKey("shop_name")
        val currency = stringPreferencesKey("currency")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val defaultAlertDays = intPreferencesKey("default_alert_days")
        val defaultMinStock = intPreferencesKey("default_min_stock")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            shopName = p[Keys.shopName] ?: d.shopName,
            currency = p[Keys.currency] ?: d.currency,
            remindersEnabled = p[Keys.remindersEnabled] ?: d.remindersEnabled,
            reminderHour = p[Keys.reminderHour] ?: d.reminderHour,
            reminderMinute = p[Keys.reminderMinute] ?: d.reminderMinute,
            defaultAlertDays = p[Keys.defaultAlertDays] ?: d.defaultAlertDays,
            defaultMinStock = p[Keys.defaultMinStock] ?: d.defaultMinStock,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setShopName(value: String) = context.dataStore.edit { it[Keys.shopName] = value.trim() }
    suspend fun setCurrency(value: String) = context.dataStore.edit { it[Keys.currency] = value.trim() }
    suspend fun setRemindersEnabled(value: Boolean) = context.dataStore.edit { it[Keys.remindersEnabled] = value }
    suspend fun setReminderTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.reminderHour] = hour
        it[Keys.reminderMinute] = minute
    }
    suspend fun setDefaultAlertDays(value: Int) = context.dataStore.edit { it[Keys.defaultAlertDays] = value }
    suspend fun setDefaultMinStock(value: Int) = context.dataStore.edit { it[Keys.defaultMinStock] = value }
}
