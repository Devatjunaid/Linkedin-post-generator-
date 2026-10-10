package com.harrycodess.fueltracker.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.TripEstimate
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.domain.parseDecimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Editable copy of a trip. Text values use [units], captured when the trip was loaded. */
data class EditTripForm(
    val original: Trip? = null,
    val units: UnitPrefs = UnitPrefs(),
    val currency: String = "$",
    val distance: String = "",
    val price: String = "",
    val efficiency: String = "",
    val vehicleId: Long? = null,
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val note: String = "",
    val notFound: Boolean = false,
)

data class EditTripUiState(
    val form: EditTripForm = EditTripForm(),
    val vehicles: List<Vehicle> = emptyList(),
) {
    val isLoaded: Boolean get() = form.original != null
    val distanceKm: Double?
        get() = parseDecimal(form.distance)?.takeIf { it > 0 }?.let { FuelMath.distanceToKm(it, form.units.system) }
    val pricePerLiter: Double?
        get() = parseDecimal(form.price)?.takeIf { it > 0 }?.let { FuelMath.priceToPerLiter(it, form.units.system) }
    val kmPerL: Double?
        get() = parseDecimal(form.efficiency)?.let { FuelMath.efficiencyToKmPerL(it, form.units) }
    val estimate: TripEstimate?
        get() {
            val d = distanceKm ?: return null
            val p = pricePerLiter ?: return null
            val e = kmPerL ?: return null
            return FuelMath.estimate(d, p, e)
        }
}

sealed interface EditTripEvent {
    data object Saved : EditTripEvent
    data object Deleted : EditTripEvent
}

class EditTripViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FuelRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle.get<Long>(TRIP_ID_ARG)) { "Missing trip id" }
    private val form = MutableStateFlow(EditTripForm())

    private val _events = Channel<EditTripEvent>(Channel.BUFFERED)
    val events: Flow<EditTripEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<EditTripUiState> = combine(form, repository.vehicles) { f, v ->
        EditTripUiState(form = f, vehicles = v)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditTripUiState())

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val trip = repository.getTrip(tripId)
            if (trip == null) {
                form.update { it.copy(notFound = true) }
                return@launch
            }
            val units = settings.units
            form.value = EditTripForm(
                original = trip,
                units = units,
                currency = settings.currency,
                distance = Fmt.input(FuelMath.kmToDistance(trip.distanceKm, units.system)),
                price = Fmt.input(FuelMath.perLiterToPrice(trip.fuelPricePerLiter, units.system)),
                efficiency = Fmt.input(FuelMath.kmPerLToEfficiency(trip.efficiencyKmPerL, units)),
                vehicleId = trip.vehicleId,
                epochDay = trip.dateEpochDay,
                note = trip.note,
            )
        }
    }

    fun onDistanceChange(value: String) = form.update { it.copy(distance = value) }
    fun onPriceChange(value: String) = form.update { it.copy(price = value) }
    fun onEfficiencyChange(value: String) = form.update { it.copy(efficiency = value) }
    fun onNoteChange(value: String) = form.update { it.copy(note = value.take(40)) }
    fun onDateChange(date: LocalDate) = form.update { it.copy(epochDay = date.toEpochDay()) }
    fun onVehicleSelected(vehicle: Vehicle) = form.update {
        it.copy(
            vehicleId = vehicle.id,
            efficiency = Fmt.input(FuelMath.kmPerLToEfficiency(vehicle.efficiencyKmPerL, it.units)),
        )
    }

    fun save() {
        val state = uiState.value
        val original = state.form.original ?: return
        val estimate = state.estimate ?: return
        val vehicle = state.vehicles.firstOrNull { it.id == state.form.vehicleId }
        viewModelScope.launch {
            repository.saveTrip(
                original.copy(
                    vehicleId = vehicle?.id ?: original.vehicleId,
                    vehicleName = vehicle?.name ?: original.vehicleName,
                    dateEpochDay = state.form.epochDay,
                    distanceKm = state.distanceKm ?: original.distanceKm,
                    fuelPricePerLiter = state.pricePerLiter ?: original.fuelPricePerLiter,
                    efficiencyKmPerL = state.kmPerL ?: original.efficiencyKmPerL,
                    fuelLiters = estimate.fuelLiters,
                    totalCost = estimate.totalCost,
                    note = state.form.note.trim(),
                ),
            )
            _events.send(EditTripEvent.Saved)
        }
    }

    fun delete() {
        val original = form.value.original ?: return
        viewModelScope.launch {
            repository.deleteTrip(original)
            _events.send(EditTripEvent.Deleted)
        }
    }

    companion object {
        const val TRIP_ID_ARG = "tripId"
    }
}
