package com.harrycodess.fueltracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import com.harrycodess.fueltracker.data.settings.ThemeMode
import com.harrycodess.fueltracker.data.settings.UserSettings
import com.harrycodess.fueltracker.domain.EfficiencyFormat
import com.harrycodess.fueltracker.domain.UnitSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val vehicleCount: Int = 0,
    val tripCount: Int = 0,
)

class SettingsViewModel(
    private val repository: FuelRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        repository.vehicles,
        repository.trips,
    ) { settings, vehicles, trips ->
        SettingsUiState(settings, vehicles.size, trips.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(system) }
    }

    fun setEfficiencyFormat(format: EfficiencyFormat) {
        viewModelScope.launch { settingsRepository.setEfficiencyFormat(format) }
    }

    fun setCurrency(symbol: String) {
        viewModelScope.launch { settingsRepository.setCurrency(symbol) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    suspend fun allTrips(): List<Trip> = repository.getAllTrips()
}
