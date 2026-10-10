package com.harrycodess.fueltracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.harrycodess.fueltracker.AppContainer
import com.harrycodess.fueltracker.FuelTrackerApp
import com.harrycodess.fueltracker.ui.calculator.CalculatorViewModel
import com.harrycodess.fueltracker.ui.edit.EditTripViewModel
import com.harrycodess.fueltracker.ui.history.HistoryViewModel
import com.harrycodess.fueltracker.ui.settings.SettingsViewModel
import com.harrycodess.fueltracker.ui.vehicles.VehiclesViewModel

/** Builds every ViewModel from the app's [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val c = container()
            CalculatorViewModel(c.repository, c.settingsRepository)
        }
        initializer {
            val c = container()
            HistoryViewModel(c.repository, c.settingsRepository)
        }
        initializer {
            val c = container()
            EditTripViewModel(createSavedStateHandle(), c.repository, c.settingsRepository)
        }
        initializer {
            val c = container()
            SettingsViewModel(c.repository, c.settingsRepository)
        }
        initializer {
            val c = container()
            VehiclesViewModel(c.repository, c.settingsRepository)
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FuelTrackerApp).container
