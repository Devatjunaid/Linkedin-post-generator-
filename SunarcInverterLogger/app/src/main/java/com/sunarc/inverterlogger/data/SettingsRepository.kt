package com.sunarc.inverterlogger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sunarc.inverterlogger.domain.SystemProfile
import com.sunarc.inverterlogger.domain.TariffModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val country = stringPreferencesKey("country")
        val name = stringPreferencesKey("system_name")
        val kwp = doublePreferencesKey("kwp")
        val tilt = doublePreferencesKey("tilt")
        val azimuth = doublePreferencesKey("azimuth")
        val lat = doublePreferencesKey("lat")
        val lon = doublePreferencesKey("lon")
        val tariff = stringPreferencesKey("tariff_model")
        val buy = doublePreferencesKey("buy_price")
        val export = doublePreferencesKey("export_price")
        val selfUse = doublePreferencesKey("self_use")
        val co2 = intPreferencesKey("co2")
        val cost = doublePreferencesKey("system_cost")
        val install = longPreferencesKey("install_day")
        val sunset = booleanPreferencesKey("sunset_reminder")
        val monthly = booleanPreferencesKey("monthly_summary")
    }

    val profile: Flow<SystemProfile> = context.settingsStore.data.map { p ->
        SystemProfile(
            onboarded = p[K.onboarded] ?: false,
            countryCode = p[K.country] ?: "DE",
            systemName = p[K.name] ?: "My solar system",
            kwp = p[K.kwp] ?: 0.0,
            tiltDeg = p[K.tilt] ?: 30.0,
            azimuthDeg = p[K.azimuth] ?: 180.0,
            latitude = p[K.lat],
            longitude = p[K.lon],
            tariffModel = p[K.tariff]?.let { name -> TariffModel.entries.firstOrNull { it.name == name } },
            buyPrice = p[K.buy] ?: 0.0,
            exportPrice = p[K.export] ?: 0.0,
            selfUseShare = p[K.selfUse] ?: 0.35,
            co2Grams = p[K.co2],
            systemCost = p[K.cost],
            installEpochDay = p[K.install],
            sunsetReminder = p[K.sunset] ?: false,
            monthlySummary = p[K.monthly] ?: false
        )
    }

    suspend fun current(): SystemProfile = profile.first()

    suspend fun save(profile: SystemProfile) {
        context.settingsStore.edit { p ->
            p[K.onboarded] = profile.onboarded
            p[K.country] = profile.countryCode
            p[K.name] = profile.systemName
            p[K.kwp] = profile.kwp
            p[K.tilt] = profile.tiltDeg
            p[K.azimuth] = profile.azimuthDeg
            profile.latitude?.let { p[K.lat] = it } ?: p.remove(K.lat)
            profile.longitude?.let { p[K.lon] = it } ?: p.remove(K.lon)
            profile.tariffModel?.let { p[K.tariff] = it.name } ?: p.remove(K.tariff)
            p[K.buy] = profile.buyPrice
            p[K.export] = profile.exportPrice
            p[K.selfUse] = profile.selfUseShare
            profile.co2Grams?.let { p[K.co2] = it } ?: p.remove(K.co2)
            profile.systemCost?.let { p[K.cost] = it } ?: p.remove(K.cost)
            profile.installEpochDay?.let { p[K.install] = it } ?: p.remove(K.install)
            p[K.sunset] = profile.sunsetReminder
            p[K.monthly] = profile.monthlySummary
        }
    }

    suspend fun update(transform: (SystemProfile) -> SystemProfile) = save(transform(current()))
}
