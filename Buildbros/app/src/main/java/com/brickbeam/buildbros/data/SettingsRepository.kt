package com.brickbeam.buildbros.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brickbeam.buildbros.calc.DefaultPrices
import com.brickbeam.buildbros.calc.PriceKey
import com.brickbeam.buildbros.calc.PriceZone
import com.brickbeam.buildbros.calc.Region
import com.brickbeam.buildbros.calc.Regions
import com.brickbeam.buildbros.calc.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "buildbros_settings")

enum class ThemeId { NIGHT, BLUEPRINT, TERRACOTTA, HIVIS }

enum class UnitPreference(val label: String) { AUTO("Auto (by region)"), IMPERIAL("Imperial"), METRIC("Metric") }

data class AppSettings(
    val theme: ThemeId = ThemeId.NIGHT,
    val motion: Boolean = true,
    val regionCode: String? = null,
    val unitPreference: UnitPreference = UnitPreference.AUTO,
) {
    val region: Region get() = Regions.byCode(regionCode) ?: Regions.DEFAULT
    val hasChosenRegion: Boolean get() = Regions.byCode(regionCode) != null
    val unitSystem: UnitSystem
        get() = when (unitPreference) {
            UnitPreference.AUTO -> region.defaultUnitSystem
            UnitPreference.IMPERIAL -> UnitSystem.IMPERIAL
            UnitPreference.METRIC -> UnitSystem.METRIC
        }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val MOTION = booleanPreferencesKey("motion")
        val REGION = stringPreferencesKey("region")
        val UNITS = stringPreferencesKey("units")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = p[Keys.THEME]?.let { runCatching { ThemeId.valueOf(it) }.getOrNull() } ?: ThemeId.NIGHT,
            motion = p[Keys.MOTION] ?: true,
            regionCode = p[Keys.REGION],
            unitPreference = p[Keys.UNITS]?.let { runCatching { UnitPreference.valueOf(it) }.getOrNull() }
                ?: UnitPreference.AUTO,
        )
    }

    suspend fun setTheme(theme: ThemeId) = context.dataStore.edit { it[Keys.THEME] = theme.name }
    suspend fun setMotion(on: Boolean) = context.dataStore.edit { it[Keys.MOTION] = on }
    suspend fun setRegion(code: String) = context.dataStore.edit { it[Keys.REGION] = code }
    suspend fun setUnits(pref: UnitPreference) = context.dataStore.edit { it[Keys.UNITS] = pref.name }

    // ---- Prices: defaults per currency zone, overridden by what the user types in ----

    private fun priceKey(zone: PriceZone, key: PriceKey) = doublePreferencesKey("price_${zone.name}_${key.name}")

    fun prices(zone: PriceZone): Flow<Map<PriceKey, Double>> = context.dataStore.data.map { p ->
        val defaults = DefaultPrices.forZone(zone)
        PriceKey.entries.associateWith { k -> p[priceKey(zone, k)] ?: defaults[k] ?: 0.0 }
    }

    fun customizedKeys(zone: PriceZone): Flow<Set<PriceKey>> = context.dataStore.data.map { p ->
        PriceKey.entries.filter { p[priceKey(zone, it)] != null }.toSet()
    }

    suspend fun setPrice(zone: PriceZone, key: PriceKey, value: Double) =
        context.dataStore.edit { it[priceKey(zone, key)] = value }

    suspend fun resetPrices(zone: PriceZone) = context.dataStore.edit { p ->
        PriceKey.entries.forEach { p.remove(priceKey(zone, it)) }
    }
}
