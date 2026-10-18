package com.inkformlabs.pdfsigner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.inkformPrefs: DataStore<Preferences> by preferencesDataStore(name = "inkform_prefs")

enum class ThemeChoice(val title: String) {
    CITRUS("Citrus Ink"),
    MIDNIGHT("Midnight Teal"),
    CORAL("Coral Paper"),
    VIOLET("Royal Violet"),
}

class SettingsStore(context: Context) {
    private val store = context.applicationContext.inkformPrefs
    private val themeKey = stringPreferencesKey("theme")

    val theme: Flow<ThemeChoice> = store.data.map { prefs ->
        prefs[themeKey]?.let { saved -> ThemeChoice.entries.firstOrNull { it.name == saved } }
            ?: ThemeChoice.CITRUS
    }

    suspend fun setTheme(theme: ThemeChoice) {
        store.edit { it[themeKey] = theme.name }
    }
}

/** Personal details used only on this device to auto-fill forms. */
data class Profile(
    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "",
    val dateOfBirth: String = "",
    val company: String = "",
) {
    val isEmpty: Boolean get() = listOf(
        fullName, firstName, lastName, email, phone, address, city, state, zip, country, dateOfBirth, company,
    ).all { it.isBlank() }

    /** Full name, falling back to first + last. */
    val displayName: String
        get() = fullName.ifBlank { listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ") }
}

class ProfileStore(context: Context) {
    private val store = context.applicationContext.inkformPrefs

    private object Keys {
        val fullName = stringPreferencesKey("p_full_name")
        val firstName = stringPreferencesKey("p_first_name")
        val lastName = stringPreferencesKey("p_last_name")
        val email = stringPreferencesKey("p_email")
        val phone = stringPreferencesKey("p_phone")
        val address = stringPreferencesKey("p_address")
        val city = stringPreferencesKey("p_city")
        val state = stringPreferencesKey("p_state")
        val zip = stringPreferencesKey("p_zip")
        val country = stringPreferencesKey("p_country")
        val dob = stringPreferencesKey("p_dob")
        val company = stringPreferencesKey("p_company")
    }

    val profile: Flow<Profile> = store.data.map { p ->
        Profile(
            fullName = p[Keys.fullName].orEmpty(),
            firstName = p[Keys.firstName].orEmpty(),
            lastName = p[Keys.lastName].orEmpty(),
            email = p[Keys.email].orEmpty(),
            phone = p[Keys.phone].orEmpty(),
            address = p[Keys.address].orEmpty(),
            city = p[Keys.city].orEmpty(),
            state = p[Keys.state].orEmpty(),
            zip = p[Keys.zip].orEmpty(),
            country = p[Keys.country].orEmpty(),
            dateOfBirth = p[Keys.dob].orEmpty(),
            company = p[Keys.company].orEmpty(),
        )
    }

    suspend fun save(profile: Profile) {
        store.edit { p ->
            p[Keys.fullName] = profile.fullName.trim()
            p[Keys.firstName] = profile.firstName.trim()
            p[Keys.lastName] = profile.lastName.trim()
            p[Keys.email] = profile.email.trim()
            p[Keys.phone] = profile.phone.trim()
            p[Keys.address] = profile.address.trim()
            p[Keys.city] = profile.city.trim()
            p[Keys.state] = profile.state.trim()
            p[Keys.zip] = profile.zip.trim()
            p[Keys.country] = profile.country.trim()
            p[Keys.dob] = profile.dateOfBirth.trim()
            p[Keys.company] = profile.company.trim()
        }
    }
}
