package com.harrycodess.fueltracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.harrycodess.fueltracker.domain.EfficiencyFormat
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.domain.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val units: UnitPrefs = UnitPrefs(),
    val currency: String = "$",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Last fuel price used, in price per litre, to pre-fill the calculator. */
    val lastPricePerLiter: Double? = null,
    val lastVehicleId: Long? = null,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<UserSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserSettings(
                units = UnitPrefs(
                    system = enumOrDefault(p[Keys.UNIT_SYSTEM], UnitSystem.METRIC),
                    efficiencyFormat = enumOrDefault(p[Keys.EFFICIENCY_FORMAT], EfficiencyFormat.KM_PER_L),
                ),
                currency = p[Keys.CURRENCY] ?: "$",
                themeMode = enumOrDefault(p[Keys.THEME_MODE], ThemeMode.SYSTEM),
                lastPricePerLiter = p[Keys.LAST_PRICE_PER_LITER],
                lastVehicleId = p[Keys.LAST_VEHICLE_ID],
            )
        }

    suspend fun setUnitSystem(system: UnitSystem) {
        store.edit { it[Keys.UNIT_SYSTEM] = system.name }
    }

    suspend fun setEfficiencyFormat(format: EfficiencyFormat) {
        store.edit { it[Keys.EFFICIENCY_FORMAT] = format.name }
    }

    suspend fun setCurrency(symbol: String) {
        store.edit { it[Keys.CURRENCY] = symbol }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLastUsed(pricePerLiter: Double, vehicleId: Long?) {
        store.edit {
            it[Keys.LAST_PRICE_PER_LITER] = pricePerLiter
            if (vehicleId != null) it[Keys.LAST_VEHICLE_ID] = vehicleId else it.remove(Keys.LAST_VEHICLE_ID)
        }
    }

    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val EFFICIENCY_FORMAT = stringPreferencesKey("efficiency_format")
        val CURRENCY = stringPreferencesKey("currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_PRICE_PER_LITER = doublePreferencesKey("last_price_per_liter")
        val LAST_VEHICLE_ID = longPreferencesKey("last_vehicle_id")
    }
}

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
