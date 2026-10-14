package com.harrycodess.fueltracker.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrycodess.fueltracker.data.CarCatalog
import com.harrycodess.fueltracker.data.CarCategory
import com.harrycodess.fueltracker.data.CatalogCar
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.ui.calculator.CalculatorPrefill
import com.harrycodess.fueltracker.ui.calculator.CalculatorPrefillBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CarSort { POPULAR, MOST_EFFICIENT }

data class CarsUiState(
    val query: String = "",
    val category: CarCategory? = null,
    val sort: CarSort = CarSort.POPULAR,
    val units: UnitPrefs = UnitPrefs(),
    val results: List<CatalogCar> = CarCatalog.cars,
)

sealed interface CarsEvent {
    data class AddedToVehicles(val name: String) : CarsEvent
}

class CarsViewModel(
    private val repository: FuelRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<CarCategory?>(null)
    private val sort = MutableStateFlow(CarSort.POPULAR)

    private val _events = Channel<CarsEvent>(Channel.BUFFERED)
    val events: Flow<CarsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<CarsUiState> = combine(
        query,
        category,
        sort,
        settingsRepository.settings,
    ) { q, c, s, settings ->
        val tokens = q.trim().lowercase().split(" ").filter { it.isNotBlank() }
        val filtered = CarCatalog.cars.filter { car ->
            (c == null || car.category == c) &&
                tokens.all { token ->
                    car.make.lowercase().contains(token) ||
                        car.model.lowercase().contains(token) ||
                        car.engine.lowercase().contains(token) ||
                        car.category.label.lowercase().contains(token)
                }
        }
        CarsUiState(
            query = q,
            category = c,
            sort = s,
            units = settings.units,
            results = if (s == CarSort.MOST_EFFICIENT) filtered.sortedByDescending { it.kmPerL } else filtered,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CarsUiState())

    fun onQueryChange(value: String) {
        query.value = value.take(40)
    }

    fun onCategoryChange(value: CarCategory?) {
        category.value = value
    }

    fun onSortChange(value: CarSort) {
        sort.value = value
    }

    /** Sends the model's typical mileage to the calculator. */
    fun useInCalculator(car: CatalogCar) {
        CalculatorPrefillBus.request.value = CalculatorPrefill(car.name, car.kmPerL)
    }

    fun addToMyVehicles(car: CatalogCar) {
        viewModelScope.launch {
            repository.addVehicle(car.name, car.kmPerL)
            _events.send(CarsEvent.AddedToVehicles(car.name))
        }
    }
}
