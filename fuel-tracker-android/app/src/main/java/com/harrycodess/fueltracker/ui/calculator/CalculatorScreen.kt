package com.harrycodess.fueltracker.ui.calculator

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.domain.UnitSystem
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.DecimalField
import com.harrycodess.fueltracker.ui.components.ResultCard
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.components.SectionLabel
import com.harrycodess.fueltracker.ui.components.SegmentedSelector
import com.harrycodess.fueltracker.ui.components.VehicleChipsRow
import com.harrycodess.fueltracker.ui.components.VehicleDialog
import com.harrycodess.fueltracker.ui.preview.PreviewData
import com.harrycodess.fueltracker.ui.theme.FuelTrackerTheme
import com.harrycodess.fueltracker.util.findActivity
import kotlinx.coroutines.CompletableDeferred

@Composable
fun CalculatorScreen(
    snackbarHostState: SnackbarHostState,
    showInterstitial: (Activity, () -> Unit) -> Unit,
    onViewHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CalculatorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnViewHistory by rememberUpdatedState(onViewHistory)
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showAddVehicle by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CalculatorEvent.TripSaved -> {
                    // Interstitial first (natural break after saving), then the confirmation.
                    val adDone = CompletableDeferred<Unit>()
                    val activity = context.findActivity()
                    if (activity != null) showInterstitial(activity) { adDone.complete(Unit) } else adDone.complete(Unit)
                    adDone.await()
                    val result = snackbarHostState.showSnackbar(
                        message = "Trip saved",
                        actionLabel = "View",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) currentOnViewHistory()
                }
            }
        }
    }

    CalculatorContent(
        state = state,
        onUnitSystemChange = viewModel::onUnitSystemChange,
        onToggleEfficiencyFormat = viewModel::onToggleEfficiencyFormat,
        onVehicleSelected = viewModel::onVehicleSelected,
        onAddVehicle = { showAddVehicle = true },
        onDistanceChange = viewModel::onDistanceChange,
        onPriceChange = viewModel::onPriceChange,
        onEfficiencyChange = viewModel::onEfficiencyChange,
        onSaveClick = { showSaveDialog = true },
        onOpenSettings = onOpenSettings,
    )

    if (showSaveDialog) {
        SaveTripDialog(
            state = state,
            onDismiss = { showSaveDialog = false },
            onConfirm = { note, date ->
                showSaveDialog = false
                viewModel.saveTrip(note, date)
            },
        )
    }
    if (showAddVehicle) {
        VehicleDialog(
            units = state.units,
            onDismiss = { showAddVehicle = false },
            onConfirm = { name, kmPerL ->
                showAddVehicle = false
                viewModel.addVehicle(name, kmPerL)
            },
        )
    }
}

@Composable
fun CalculatorContent(
    state: CalculatorUiState,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onToggleEfficiencyFormat: () -> Unit,
    onVehicleSelected: (Vehicle) -> Unit,
    onAddVehicle: () -> Unit,
    onDistanceChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onEfficiencyChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val units = state.units

    Column(modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "Trip Calculator",
            subtitle = "Quick log a trip",
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SegmentedSelector(
                options = UnitSystem.entries,
                selected = units.system,
                label = { if (it == UnitSystem.METRIC) "Metric" else "Imperial" },
                onSelect = onUnitSystemChange,
            )

            Column {
                SectionLabel("Vehicle")
                VehicleChipsRow(
                    vehicles = state.vehicles,
                    selectedId = state.form.selectedVehicleId,
                    onSelect = onVehicleSelected,
                    onAdd = onAddVehicle,
                )
            }

            DecimalField(
                value = state.form.distance,
                onValueChange = onDistanceChange,
                label = "Distance",
                icon = Icons.Rounded.Place,
                unit = units.distanceUnit,
            )
            DecimalField(
                value = state.form.price,
                onValueChange = onPriceChange,
                label = "Fuel price",
                icon = Icons.Rounded.LocalGasStation,
                unit = units.pricePerVolumeUnit(state.settings.currency),
            )
            DecimalField(
                value = state.form.efficiency,
                onValueChange = onEfficiencyChange,
                label = "Efficiency",
                icon = Icons.Rounded.Speed,
                unit = units.efficiencyUnit,
                imeAction = ImeAction.Done,
                onImeDone = { focusManager.clearFocus() },
                onUnitClick = if (units.isMetric) onToggleEfficiencyFormat else null,
            )

            ResultCard(
                estimate = state.estimate,
                units = units,
                currency = state.settings.currency,
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSaveClick()
                },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Save Trip")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun CalculatorPreview() {
    FuelTrackerTheme {
        Surface {
            CalculatorContent(
                state = PreviewData.calculatorState,
                onUnitSystemChange = {},
                onToggleEfficiencyFormat = {},
                onVehicleSelected = {},
                onAddVehicle = {},
                onDistanceChange = {},
                onPriceChange = {},
                onEfficiencyChange = {},
                onSaveClick = {},
                onOpenSettings = {},
            )
        }
    }
}
