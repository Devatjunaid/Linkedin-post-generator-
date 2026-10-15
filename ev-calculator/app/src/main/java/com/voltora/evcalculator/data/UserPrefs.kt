package com.voltora.evcalculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DistanceUnit { AUTO, KM, MILES }

data class UserPrefs(
    val countryCode: String? = null,
    val modelId: String? = null,
    val currentSoc: Float = 0.34f,
    val homeChargerKw: Double = 11.0,
    val homeRate: Double? = null,
    val acRate: Double? = null,
    val dcRate: Double? = null,
    val petrolPrice: Double? = null,
    val petrolLPer100: Double = 7.0,
    val distanceUnit: DistanceUnit = DistanceUnit.AUTO,
    val onboarded: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ev_calc_prefs")

class PrefsRepository(private val context: Context) {
    private object Keys {
        val country = stringPreferencesKey("country")
        val model = stringPreferencesKey("model")
        val soc = floatPreferencesKey("soc")
        val homeKw = doublePreferencesKey("home_kw")
        val homeRate = doublePreferencesKey("home_rate")
        val acRate = doublePreferencesKey("ac_rate")
        val dcRate = doublePreferencesKey("dc_rate")
        val petrol = doublePreferencesKey("petrol")
        val petrolL = doublePreferencesKey("petrol_l100")
        val unit = stringPreferencesKey("unit")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            countryCode = p[Keys.country],
            modelId = p[Keys.model],
            currentSoc = p[Keys.soc] ?: 0.34f,
            homeChargerKw = p[Keys.homeKw] ?: 11.0,
            homeRate = p[Keys.homeRate],
            acRate = p[Keys.acRate],
            dcRate = p[Keys.dcRate],
            petrolPrice = p[Keys.petrol],
            petrolLPer100 = p[Keys.petrolL] ?: 7.0,
            distanceUnit = p[Keys.unit]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() }
                ?: DistanceUnit.AUTO,
            onboarded = p[Keys.onboarded] ?: false,
        )
    }

    suspend fun setCountry(code: String) = context.dataStore.edit {
        it[Keys.country] = code
        // Rates are per-country: drop custom rates when the country changes.
        it.remove(Keys.homeRate); it.remove(Keys.acRate); it.remove(Keys.dcRate); it.remove(Keys.petrol)
    }

    suspend fun setModel(id: String) = context.dataStore.edit {
        it[Keys.model] = id
        it[Keys.onboarded] = true
    }

    suspend fun setSoc(soc: Float) = context.dataStore.edit { it[Keys.soc] = soc }
    suspend fun setHomeChargerKw(kw: Double) = context.dataStore.edit { it[Keys.homeKw] = kw }
    suspend fun setPetrolLPer100(v: Double) = context.dataStore.edit { it[Keys.petrolL] = v }
    suspend fun setDistanceUnit(u: DistanceUnit) = context.dataStore.edit { it[Keys.unit] = u.name }

    suspend fun setRates(home: Double?, ac: Double?, dc: Double?, petrol: Double?) = context.dataStore.edit {
        fun put(key: Preferences.Key<Double>, v: Double?) { if (v == null) it.remove(key) else it[key] = v }
        put(Keys.homeRate, home); put(Keys.acRate, ac); put(Keys.dcRate, dc); put(Keys.petrol, petrol)
    }
}
