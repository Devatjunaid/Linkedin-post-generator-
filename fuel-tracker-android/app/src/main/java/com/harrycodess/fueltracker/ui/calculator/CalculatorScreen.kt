package com.harrycodess.fueltracker.ui.calculator

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.UnitSystem
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.ArcGauge
import com.harrycodess.fueltracker.ui.components.CockpitCard
import com.harrycodess.fueltracker.ui.components.DecimalField
import com.harrycodess.fueltracker.ui.components.Readout
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.components.SectionTitle
import com.harrycodess.fueltracker.ui.components.SegmentedSelector
import com.harrycodess.fueltracker.ui.components.VehicleChipsRow
import com.harrycodess.fueltracker.ui.components.VehicleDialog
import com.harrycodess.fueltracker.ui.components.budgetColor
import com.harrycodess.fueltracker.ui.preview.PreviewData
import com.harrycodess.fueltracker.ui.theme.FuelTrackerTheme
import com.harrycodess.fueltracker.ui.theme.InstrumentLabel
import com.harrycodess.fueltracker.ui.theme.ReadoutLarge
import com.harrycodess.fueltracker.util.findActivity
import kotlinx.coroutines.CompletableDeferred
import kotlin.math.roundToInt

/** Reference tank size used for the gauge when no monthly budget is set. */
private const val REFERENCE_TANK_LITERS = 60.0

@Composable
fun CalculatorScreen(
    snackbarHostState: SnackbarHostState,
    showInterstitial: (Activity, () -> Unit) -> Unit,
    onViewHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onBrowseCars: () -> Unit,
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
        onBrowseCars = onBrowseCars,
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
    onBrowseCars: () -> Unit,
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
            title = "Trip Cost",
            subtitle = "Trip computer",
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TripGaugeCard(state)

            SegmentedSelector(
                options = UnitSystem.entries,
                selected = units.system,
                label = { if (it == UnitSystem.METRIC) "Metric" else "Imperial" },
                onSelect = onUnitSystemChange,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Vehicle", modifier = Modifier.weight(1f))
                    TextButton(onClick = onBrowseCars) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Car mileage guide")
                    }
                }
                VehicleChipsRow(
                    vehicles = state.vehicles,
                    selectedId = state.form.selectedVehicleId,
                    onSelect = onVehicleSelected,
                    onAdd = onAddVehicle,
                )
                val modelLabel = state.form.modelLabel
                if (modelLabel != null && state.form.selectedVehicleId == null) {
                    ModelBadge(modelLabel)
                }
            }

            SectionTitle("Trip details")
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

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSaveClick()
                },
                enabled = state.canSave,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Save Trip", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** The hero "instrument cluster": cost in the middle of a speedometer arc. */
@Composable
private fun TripGaugeCard(state: CalculatorUiState) {
    val units = state.units
    val currency = state.settings.currency
    val estimate = state.estimate
    val budget = state.settings.monthlyBudget

    val progress: Float
    val caption: String
    val gaugeColor: Color
    when {
        estimate == null -> {
            progress = 0f
            caption = "Enter your trip details below"
            gaugeColor = MaterialTheme.colorScheme.primary
        }
        budget != null && budget > 0 -> {
            progress = ((state.monthSpent + estimate.totalCost) / budget).toFloat()
            caption = "After this trip: ${(progress * 100).roundToInt()}% of your monthly budget"
            gaugeColor = budgetColor(progress)
        }
        else -> {
            progress = (estimate.fuelLiters / REFERENCE_TANK_LITERS).toFloat()
            val tank = Fmt.number(FuelMath.litersToVolume(REFERENCE_TANK_LITERS, units.system), 0)
            caption = "≈ ${(progress * 100).roundToInt()}% of a $tank ${units.volumeUnit} tank"
            gaugeColor = MaterialTheme.colorScheme.primary
        }
    }

    CockpitCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ArcGauge(progress = progress, color = gaugeColor, modifier = Modifier.size(230.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TRIP COST", style = InstrumentLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedContent(
                        targetState = estimate?.let { Fmt.money(it.totalCost, currency) } ?: "—",
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "cost",
                    ) { text ->
                        val size = when {
                            text.length > 10 -> 24.sp
                            text.length > 7 -> 30.sp
                            else -> 40.sp
                        }
                        Text(
                            text,
                            style = ReadoutLarge.copy(fontSize = size),
                            color = gaugeColor,
                            maxLines = 1,
                        )
                    }
                    val sub = estimate?.let {
                        "${Fmt.number(FuelMath.litersToVolume(it.fuelLiters, units.system))} ${units.volumeUnit}"
                    }
                    if (sub != null) {
                        Text(
                            sub,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Readout(
                label = "Fuel",
                value = estimate?.let {
                    "${Fmt.number(FuelMath.litersToVolume(it.fuelLiters, units.system))} ${units.volumeUnit}"
                } ?: "—",
                icon = Icons.Rounded.LocalGasStation,
                modifier = Modifier.weight(1f),
            )
            Readout(
                label = "Per ${units.distanceUnit}",
                value = estimate?.let {
                    Fmt.money(FuelMath.costPerDistance(it.costPerKm, units.system), currency)
                } ?: "—",
                icon = Icons.Rounded.Straighten,
                modifier = Modifier.weight(1f),
            )
            Readout(
                label = units.efficiencyUnit,
                value = state.form.efficiency.ifBlank { "—" },
                icon = Icons.Rounded.Speed,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModelBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Using typical mileage of $label", style = MaterialTheme.typography.labelLarge)
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
                onBrowseCars = {},
                onDistanceChange = {},
                onPriceChange = {},
                onEfficiencyChange = {},
                onSaveClick = {},
                onOpenSettings = {},
            )
        }
    }
}
