package com.harrycodess.fueltracker.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import com.harrycodess.fueltracker.domain.UnitPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VehicleItem(val vehicle: Vehicle, val tripCount: Int, val totalCost: Double)

data class VehiclesUiState(
    val items: List<VehicleItem> = emptyList(),
    val units: UnitPrefs = UnitPrefs(),
    val currency: String = "$",
)

class VehiclesViewModel(
    private val repository: FuelRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<VehiclesUiState> = combine(
        repository.vehicles,
        repository.trips,
        settingsRepository.settings,
    ) { vehicles, trips, settings ->
        val byVehicle = trips.groupBy { it.vehicleId }
        VehiclesUiState(
            items = vehicles.map { v ->
                val list = byVehicle[v.id].orEmpty()
                VehicleItem(v, list.size, list.sumOf { it.totalCost })
            },
            units = settings.units,
            currency = settings.currency,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehiclesUiState())

    fun add(name: String, efficiencyKmPerL: Double) {
        viewModelScope.launch { repository.addVehicle(name, efficiencyKmPerL) }
    }

    fun update(vehicle: Vehicle, name: String, efficiencyKmPerL: Double) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle.copy(name = name, efficiencyKmPerL = efficiencyKmPerL))
        }
    }

    fun delete(vehicle: Vehicle) {
        viewModelScope.launch { repository.deleteVehicle(vehicle) }
    }
}
