package com.harrycodess.fueltracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import com.harrycodess.fueltracker.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HistoryFilter(
    val vehicleId: Long? = null,
    val month: YearMonth? = null,
) {
    val isActive: Boolean get() = vehicleId != null || month != null
}

data class MonthGroup(
    val month: YearMonth,
    val total: Double,
    val trips: List<Trip>,
)

data class HistoryStats(
    val totalCost: Double = 0.0,
    val totalKm: Double = 0.0,
    val totalLiters: Double = 0.0,
    val tripCount: Int = 0,
) {
    val avgKmPerL: Double get() = if (totalLiters > 0) totalKm / totalLiters else 0.0
    val costPerKm: Double get() = if (totalKm > 0) totalCost / totalKm else 0.0

    companion object {
        fun from(trips: List<Trip>) = HistoryStats(
            totalCost = trips.sumOf { it.totalCost },
            totalKm = trips.sumOf { it.distanceKm },
            totalLiters = trips.sumOf { it.fuelLiters },
            tripCount = trips.size,
        )
    }
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val vehicles: List<Vehicle> = emptyList(),
    val filter: HistoryFilter = HistoryFilter(),
    val availableMonths: List<YearMonth> = emptyList(),
    val groups: List<MonthGroup> = emptyList(),
    val stats: HistoryStats = HistoryStats(),
    val totalTripCount: Int = 0,
) {
    val filteredTrips: List<Trip> get() = groups.flatMap { it.trips }
}

class HistoryViewModel(
    private val repository: FuelRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter())

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.trips,
        repository.vehicles,
        settingsRepository.settings,
        filter,
    ) { trips, vehicles, settings, f ->
        val months = trips.map { it.yearMonth() }.distinct()
        val filtered = trips.filter { trip ->
            (f.vehicleId == null || trip.vehicleId == f.vehicleId) &&
                (f.month == null || trip.yearMonth() == f.month)
        }
        HistoryUiState(
            isLoading = false,
            settings = settings,
            vehicles = vehicles,
            filter = f,
            availableMonths = months,
            groups = filtered.groupBy { it.yearMonth() }.map { (month, list) ->
                MonthGroup(month, list.sumOf { it.totalCost }, list)
            },
            stats = HistoryStats.from(filtered),
            totalTripCount = trips.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setVehicleFilter(vehicleId: Long?) = filter.update { it.copy(vehicleId = vehicleId) }
    fun setMonthFilter(month: YearMonth?) = filter.update { it.copy(month = month) }
    fun clearFilters() = filter.update { HistoryFilter() }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { repository.deleteTrip(trip) }
    }

    /** Undo for a delete: re-inserts the trip with its original id. */
    fun restoreTrip(trip: Trip) {
        viewModelScope.launch { repository.saveTrip(trip) }
    }
}

private fun Trip.yearMonth(): YearMonth = YearMonth.from(LocalDate.ofEpochDay(dateEpochDay))
