package com.harrycodess.fueltracker.ui.preview

import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.ui.calculator.CalculatorForm
import com.harrycodess.fueltracker.ui.calculator.CalculatorUiState
import com.harrycodess.fueltracker.ui.history.HistoryStats
import com.harrycodess.fueltracker.ui.history.HistoryUiState
import com.harrycodess.fueltracker.ui.history.MonthGroup
import java.time.LocalDate
import java.time.YearMonth

/** Sample data for Android Studio previews only. */
object PreviewData {
    private val civic = Vehicle(id = 1, name = "Civic", efficiencyKmPerL = 14.5)
    private val tucson = Vehicle(id = 2, name = "Tucson", efficiencyKmPerL = 10.8)
    val vehicles = listOf(civic, tucson)

    private fun trip(id: Long, v: Vehicle, date: LocalDate, km: Double, kmPerL: Double, note: String): Trip {
        val price = 1.79
        val liters = km / kmPerL
        return Trip(
            id = id,
            vehicleId = v.id,
            vehicleName = v.name,
            dateEpochDay = date.toEpochDay(),
            distanceKm = km,
            fuelPricePerLiter = price,
            efficiencyKmPerL = kmPerL,
            fuelLiters = liters,
            totalCost = liters * price,
            note = note,
        )
    }

    val trips = listOf(
        trip(1, civic, LocalDate.of(2026, 10, 8), 245.0, 14.5, "Weekend trip"),
        trip(2, tucson, LocalDate.of(2026, 10, 5), 62.0, 10.8, "Commute"),
        trip(3, civic, LocalDate.of(2026, 10, 2), 118.0, 15.1, "Errands"),
        trip(4, tucson, LocalDate.of(2026, 9, 27), 410.0, 11.2, "Road trip"),
    )

    val calculatorState = CalculatorUiState(
        form = CalculatorForm(distance = "245", price = "1.79", efficiency = "14.5", selectedVehicleId = 1),
        vehicles = vehicles,
    )

    val historyState: HistoryUiState
        get() {
            val groups = trips.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
                .map { (m, list) -> MonthGroup(m, list.sumOf { it.totalCost }, list) }
            return HistoryUiState(
                isLoading = false,
                vehicles = vehicles,
                availableMonths = groups.map { it.month },
                groups = groups,
                stats = HistoryStats.from(trips),
                totalTripCount = trips.size,
            )
        }
}
