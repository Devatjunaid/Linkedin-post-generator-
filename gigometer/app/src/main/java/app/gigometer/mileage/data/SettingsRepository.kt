package app.gigometer.mileage.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

val DEFAULT_PLATFORMS = listOf("Uber", "Lyft", "DoorDash", "Uber Eats", "Instacart", "Grubhub", "Amazon Flex")

data class AppSettings(
    val autoDetect: Boolean = true,
    val batterySaver: Boolean = true,
    val reviewReminder: Boolean = false,
    val lookupAddresses: Boolean = true,
    /** Minutes after midnight. */
    val workStart: Int = 7 * 60,
    val workEnd: Int = 23 * 60,
    /** Trips detected inside work hours are saved as Business automatically. */
    val workHoursAutoBusiness: Boolean = true,
    val platforms: List<String> = listOf("Uber", "DoorDash", "Instacart"),
    val lastPlatform: String? = null,
    val rates: List<RatePeriod> = IrsRates.DEFAULTS,
    val driverName: String = "",
    val onboarded: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object K {
        val autoDetect = booleanPreferencesKey("auto_detect")
        val batterySaver = booleanPreferencesKey("battery_saver")
        val reminder = booleanPreferencesKey("review_reminder")
        val lookup = booleanPreferencesKey("lookup_addresses")
        val workStart = intPreferencesKey("work_start")
        val workEnd = intPreferencesKey("work_end")
        val workAuto = booleanPreferencesKey("work_auto_business")
        val platforms = stringPreferencesKey("platforms")
        val lastPlatform = stringPreferencesKey("last_platform")
        val rates = stringPreferencesKey("rates")
        val name = stringPreferencesKey("driver_name")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            autoDetect = this[K.autoDetect] ?: d.autoDetect,
            batterySaver = this[K.batterySaver] ?: d.batterySaver,
            reviewReminder = this[K.reminder] ?: d.reviewReminder,
            lookupAddresses = this[K.lookup] ?: d.lookupAddresses,
            workStart = this[K.workStart] ?: d.workStart,
            workEnd = this[K.workEnd] ?: d.workEnd,
            workHoursAutoBusiness = this[K.workAuto] ?: d.workHoursAutoBusiness,
            platforms = this[K.platforms]?.split("|")?.filter { it.isNotBlank() } ?: d.platforms,
            lastPlatform = this[K.lastPlatform],
            rates = IrsRates.decode(this[K.rates]),
            driverName = this[K.name] ?: "",
            onboarded = this[K.onboarded] ?: false,
        )
    }

    suspend fun setAutoDetect(v: Boolean) = context.dataStore.edit { it[K.autoDetect] = v }
    suspend fun setBatterySaver(v: Boolean) = context.dataStore.edit { it[K.batterySaver] = v }
    suspend fun setReminder(v: Boolean) = context.dataStore.edit { it[K.reminder] = v }
    suspend fun setLookupAddresses(v: Boolean) = context.dataStore.edit { it[K.lookup] = v }
    suspend fun setWorkAutoBusiness(v: Boolean) = context.dataStore.edit { it[K.workAuto] = v }
    suspend fun setWorkHours(start: Int, end: Int) = context.dataStore.edit {
        it[K.workStart] = start; it[K.workEnd] = end
    }
    suspend fun setPlatforms(list: List<String>) = context.dataStore.edit {
        it[K.platforms] = list.map { p -> p.replace("|", "/").trim() }.filter { p -> p.isNotBlank() }.distinct().joinToString("|")
    }
    suspend fun setLastPlatform(p: String?) = context.dataStore.edit {
        if (p == null) it.remove(K.lastPlatform) else it[K.lastPlatform] = p
    }
    suspend fun setRates(list: List<RatePeriod>) = context.dataStore.edit { it[K.rates] = IrsRates.encode(list) }
    suspend fun setDriverName(v: String) = context.dataStore.edit { it[K.name] = v.trim() }
    suspend fun setOnboarded() = context.dataStore.edit { it[K.onboarded] = true }
}

fun AppSettings.isWithinWorkHours(minuteOfDay: Int): Boolean =
    if (workStart <= workEnd) minuteOfDay in workStart until workEnd
    else minuteOfDay >= workStart || minuteOfDay < workEnd // overnight shift, e.g. 6 PM – 2 AM
