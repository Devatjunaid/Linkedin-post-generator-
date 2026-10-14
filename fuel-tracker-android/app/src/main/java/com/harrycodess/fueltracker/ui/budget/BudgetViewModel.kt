package com.harrycodess.fueltracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

data class MonthTotal(val month: YearMonth, val total: Double)
data class VehicleSpend(val name: String, val total: Double)

data class BudgetUiState(
    val isLoading: Boolean = true,
    val currency: String = "$",
    val budget: Double? = null,
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val spent: Double = 0.0,
    val tripCount: Int = 0,
    /** Oldest first, ending with the current month. */
    val history: List<MonthTotal> = emptyList(),
    val byVehicle: List<VehicleSpend> = emptyList(),
) {
    val daysInMonth: Int get() = month.lengthOfMonth()
    val daysLeft: Int get() = daysInMonth - today.dayOfMonth + 1
    val remaining: Double get() = (budget ?: 0.0) - spent
    val fraction: Float get() = budget?.takeIf { it > 0 }?.let { (spent / it).toFloat() } ?: 0f
    val dailyAllowance: Double get() = max(remaining, 0.0) / daysLeft
    val projected: Double get() = spent / today.dayOfMonth * daysInMonth
}

class BudgetViewModel(
    repository: FuelRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.trips,
        settingsRepository.settings,
    ) { trips, settings ->
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val byMonth = trips.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
        val thisMonth = byMonth[month].orEmpty()
        BudgetUiState(
            isLoading = false,
            currency = settings.currency,
            budget = settings.monthlyBudget,
            month = month,
            today = today,
            spent = thisMonth.sumOf { it.totalCost },
            tripCount = thisMonth.size,
            history = (5 downTo 0).map { back ->
                val m = month.minusMonths(back.toLong())
                MonthTotal(m, byMonth[m].orEmpty().sumOf { it.totalCost })
            },
            byVehicle = thisMonth.groupBy { it.vehicleName }
                .map { (name, list) -> VehicleSpend(name, list.sumOf { it.totalCost }) }
                .sortedByDescending { it.total },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun setBudget(amount: Double?) {
        viewModelScope.launch { settingsRepository.setMonthlyBudget(amount) }
    }
}
