package com.dailycash.tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class Prefs(private val context: Context) {
    private val homeKey = stringPreferencesKey("home_currency")
    private val lastKey = stringPreferencesKey("last_currency")

    val homeCurrency: Flow<String?> = context.dataStore.data.map { it[homeKey] }
    val lastCurrency: Flow<String?> = context.dataStore.data.map { it[lastKey] }

    suspend fun setHomeCurrency(code: String) {
        context.dataStore.edit { it[homeKey] = code }
    }

    suspend fun setLastCurrency(code: String) {
        context.dataStore.edit { it[lastKey] = code }
    }
}
