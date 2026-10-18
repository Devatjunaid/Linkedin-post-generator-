package app.layerline.toolkit3d.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.layerline.toolkit3d.domain.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** The user's workshop settings: region and the rates every calculation starts from. */
data class Workshop(
    val regionCode: String,
    val symbol: String,
    val decimals: Int,
    val kwhRate: Double,
    val filamentPerKg: Double,
    val labourPerHour: Double,
    val perUsd: Double,
    val marginPercent: Double,
    val failurePercent: Double,
) {
    val region: Region get() = Presets.region(regionCode)
    val money: Money get() = Money(symbol, decimals)

    companion object {
        fun fromRegion(region: Region) = Workshop(
            regionCode = region.code,
            symbol = region.symbol,
            decimals = region.decimals,
            kwhRate = region.kwhRate,
            filamentPerKg = region.filamentPerKg,
            labourPerHour = region.labourPerHour,
            perUsd = region.perUsd,
            marginPercent = 30.0,
            failurePercent = 10.0,
        )

        /** First launch: guess the region from the phone's locale, fall back to the US. */
        fun guess(): Workshop {
            val country = Locale.getDefault().country.uppercase(Locale.US)
            val region = Presets.regions.firstOrNull { it.code == country } ?: Presets.region("US")
            return fromRegion(region)
        }
    }
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val region = stringPreferencesKey("region")
        val symbol = stringPreferencesKey("symbol")
        val decimals = intPreferencesKey("decimals")
        val kwh = doublePreferencesKey("kwh")
        val filament = doublePreferencesKey("filament_per_kg")
        val labour = doublePreferencesKey("labour_per_hour")
        val perUsd = doublePreferencesKey("per_usd")
        val margin = doublePreferencesKey("margin")
        val failure = doublePreferencesKey("failure")
    }

    val workshop: Flow<Workshop> = context.dataStore.data.map { p ->
        val base = p[Keys.region]?.let { Workshop.fromRegion(Presets.region(it)) } ?: Workshop.guess()
        Workshop(
            regionCode = base.regionCode,
            symbol = p[Keys.symbol] ?: base.symbol,
            decimals = p[Keys.decimals] ?: base.decimals,
            kwhRate = p[Keys.kwh] ?: base.kwhRate,
            filamentPerKg = p[Keys.filament] ?: base.filamentPerKg,
            labourPerHour = p[Keys.labour] ?: base.labourPerHour,
            perUsd = p[Keys.perUsd] ?: base.perUsd,
            marginPercent = p[Keys.margin] ?: base.marginPercent,
            failurePercent = p[Keys.failure] ?: base.failurePercent,
        )
    }

    /** Switching region resets every rate to that region's defaults. */
    suspend fun setRegion(code: String) {
        val r = Presets.region(code)
        context.dataStore.edit { p ->
            p[Keys.region] = r.code
            p[Keys.symbol] = r.symbol
            p[Keys.decimals] = r.decimals
            p[Keys.kwh] = r.kwhRate
            p[Keys.filament] = r.filamentPerKg
            p[Keys.labour] = r.labourPerHour
            p[Keys.perUsd] = r.perUsd
        }
    }

    suspend fun save(w: Workshop) {
        context.dataStore.edit { p ->
            p[Keys.region] = w.regionCode
            p[Keys.symbol] = w.symbol
            p[Keys.decimals] = w.decimals
            p[Keys.kwh] = w.kwhRate
            p[Keys.filament] = w.filamentPerKg
            p[Keys.labour] = w.labourPerHour
            p[Keys.perUsd] = w.perUsd
            p[Keys.margin] = w.marginPercent
            p[Keys.failure] = w.failurePercent
        }
    }
}
