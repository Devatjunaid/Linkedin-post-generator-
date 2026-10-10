package com.harrycodess.fueltracker.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import com.harrycodess.fueltracker.data.settings.UserSettings
import com.harrycodess.fueltracker.domain.EfficiencyFormat
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.TripEstimate
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.domain.UnitSystem
import com.harrycodess.fueltracker.domain.parseDecimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Raw text the user typed, always in the units in effect when it was typed. */
data class CalculatorForm(
    val distance: String = "",
    val price: String = "",
    val efficiency: String = "",
    val selectedVehicleId: Long? = null,
)

data class CalculatorUiState(
    val form: CalculatorForm = CalculatorForm(),
    val settings: UserSettings = UserSettings(),
    val vehicles: List<Vehicle> = emptyList(),
) {
    val units: UnitPrefs get() = settings.units
    val selectedVehicle: Vehicle? get() = vehicles.firstOrNull { it.id == form.selectedVehicleId }

    val distanceKm: Double?
        get() = parseDecimal(form.distance)?.takeIf { it > 0 }?.let { FuelMath.distanceToKm(it, units.system) }
    val pricePerLiter: Double?
        get() = parseDecimal(form.price)?.takeIf { it > 0 }?.let { FuelMath.priceToPerLiter(it, units.system) }
    val kmPerL: Double?
        get() = parseDecimal(form.efficiency)?.let { FuelMath.efficiencyToKmPerL(it, units) }

    val estimate: TripEstimate?
        get() {
            val d = distanceKm ?: return null
            val p = pricePerLiter ?: return null
            val e = kmPerL ?: return null
            return FuelMath.estimate(d, p, e)
        }

    val canSave: Boolean get() = estimate != null
}

sealed interface CalculatorEvent {
    data object TripSaved : CalculatorEvent
}

class CalculatorViewModel(
    private val repository: FuelRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val form = MutableStateFlow(CalculatorForm())
    private var currentUnits = UnitPrefs()

    private val _events = Channel<CalculatorEvent>(Channel.BUFFERED)
    val events: Flow<CalculatorEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<CalculatorUiState> =
        combine(form, settingsRepository.settings, repository.vehicles) { f, s, v ->
            CalculatorUiState(form = f, settings = s, vehicles = v)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalculatorUiState())

    init {
        viewModelScope.launch {
            var previous: UnitPrefs? = null
            settingsRepository.settings.map { it.units }.distinctUntilChanged().collect { units ->
                val old = previous
                if (old == null) {
                    prefill(units)
                } else if (old != units) {
                    // Units changed (here or in Settings): convert what's typed so the trip stays the same.
                    form.update { it.convert(old, units) }
                }
                currentUnits = units
                previous = units
            }
        }
    }

    /** Pre-fills the last fuel price and the last used vehicle's efficiency. */
    private suspend fun prefill(units: UnitPrefs) {
        val settings = settingsRepository.settings.first()
        val vehicles = repository.vehicles.first()
        val vehicle = vehicles.firstOrNull { it.id == settings.lastVehicleId } ?: vehicles.firstOrNull()
        form.update { f ->
            f.copy(
                price = f.price.ifEmpty {
                    settings.lastPricePerLiter?.let { Fmt.input(FuelMath.perLiterToPrice(it, units.system)) }.orEmpty()
                },
                selectedVehicleId = f.selectedVehicleId ?: vehicle?.id,
                efficiency = f.efficiency.ifEmpty {
                    vehicle?.let { Fmt.input(FuelMath.kmPerLToEfficiency(it.efficiencyKmPerL, units)) }.orEmpty()
                },
            )
        }
    }

    fun onDistanceChange(value: String) = form.update { it.copy(distance = value) }
    fun onPriceChange(value: String) = form.update { it.copy(price = value) }
    fun onEfficiencyChange(value: String) = form.update { it.copy(efficiency = value) }

    fun onVehicleSelected(vehicle: Vehicle) = form.update {
        it.copy(
            selectedVehicleId = vehicle.id,
            efficiency = Fmt.input(FuelMath.kmPerLToEfficiency(vehicle.efficiencyKmPerL, currentUnits)),
        )
    }

    fun onUnitSystemChange(system: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(system) }
    }

    fun onToggleEfficiencyFormat() {
        val next = if (currentUnits.efficiencyFormat == EfficiencyFormat.KM_PER_L) {
            EfficiencyFormat.L_PER_100KM
        } else {
            EfficiencyFormat.KM_PER_L
        }
        viewModelScope.launch { settingsRepository.setEfficiencyFormat(next) }
    }

    fun addVehicle(name: String, efficiencyKmPerL: Double) {
        viewModelScope.launch {
            val id = repository.addVehicle(name, efficiencyKmPerL)
            form.update {
                it.copy(
                    selectedVehicleId = id,
                    efficiency = Fmt.input(FuelMath.kmPerLToEfficiency(efficiencyKmPerL, currentUnits)),
                )
            }
        }
    }

    fun saveTrip(note: String, date: LocalDate) {
        val state = uiState.value
        val estimate = state.estimate ?: return
        val distanceKm = state.distanceKm ?: return
        val pricePerLiter = state.pricePerLiter ?: return
        val kmPerL = state.kmPerL ?: return
        val vehicle = state.selectedVehicle
        viewModelScope.launch {
            repository.saveTrip(
                Trip(
                    vehicleId = vehicle?.id,
                    vehicleName = vehicle?.name ?: "No vehicle",
                    dateEpochDay = date.toEpochDay(),
                    distanceKm = distanceKm,
                    fuelPricePerLiter = pricePerLiter,
                    efficiencyKmPerL = kmPerL,
                    fuelLiters = estimate.fuelLiters,
                    totalCost = estimate.totalCost,
                    note = note.trim(),
                ),
            )
            settingsRepository.setLastUsed(pricePerLiter, vehicle?.id)
            form.update { it.copy(distance = "") }
            _events.send(CalculatorEvent.TripSaved)
        }
    }
}

private fun CalculatorForm.convert(from: UnitPrefs, to: UnitPrefs): CalculatorForm {
    fun String.mapNumber(transform: (Double) -> Double): String =
        parseDecimal(this)?.takeIf { it > 0 }?.let { Fmt.input(transform(it)) } ?: this

    return copy(
        distance = distance.mapNumber { FuelMath.kmToDistance(FuelMath.distanceToKm(it, from.system), to.system) },
        price = price.mapNumber { FuelMath.perLiterToPrice(FuelMath.priceToPerLiter(it, from.system), to.system) },
        efficiency = efficiency.mapNumber { value ->
            FuelMath.efficiencyToKmPerL(value, from)?.let { FuelMath.kmPerLToEfficiency(it, to) } ?: value
        },
    )
}
