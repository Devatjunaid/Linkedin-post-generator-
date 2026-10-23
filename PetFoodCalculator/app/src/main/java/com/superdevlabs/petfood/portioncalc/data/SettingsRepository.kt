package com.superdevlabs.petfood.portioncalc.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.superdevlabs.petfood.portioncalc.domain.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val unitKey = stringPreferencesKey("weight_unit")
    private val selectedPetKey = longPreferencesKey("selected_pet")

    val weightUnit: Flow<WeightUnit> = context.dataStore.data.map { prefs ->
        prefs[unitKey]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.KG
    }

    val selectedPetId: Flow<Long?> = context.dataStore.data.map { it[selectedPetKey] }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { it[unitKey] = unit.name }
    }

    suspend fun setSelectedPet(id: Long) {
        context.dataStore.edit { it[selectedPetKey] = id }
    }
}
