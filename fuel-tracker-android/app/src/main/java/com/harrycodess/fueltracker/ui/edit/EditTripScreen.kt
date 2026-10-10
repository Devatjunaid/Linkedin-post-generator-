package com.harrycodess.fueltracker.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.ConfirmDialog
import com.harrycodess.fueltracker.ui.components.DatePickerChip
import com.harrycodess.fueltracker.ui.components.DecimalField
import com.harrycodess.fueltracker.ui.components.EmptyState
import com.harrycodess.fueltracker.ui.components.ResultCard
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.components.SectionLabel
import com.harrycodess.fueltracker.ui.components.VehicleChipsRow
import java.time.LocalDate

@Composable
fun EditTripScreen(
    onDone: () -> Unit,
    viewModel: EditTripViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { currentOnDone() }
    }

    val form = state.form
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "Edit trip",
            onBack = onDone,
            actions = {
                if (state.isLoaded) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete trip")
                    }
                }
            },
        )

        when {
            form.notFound -> EmptyState(
                icon = Icons.Rounded.Delete,
                title = "Trip not found",
                message = "This trip may have been deleted.",
            )

            !state.isLoaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column {
                    SectionLabel("Vehicle")
                    VehicleChipsRow(
                        vehicles = state.vehicles,
                        selectedId = form.vehicleId,
                        onSelect = viewModel::onVehicleSelected,
                    )
                }
                DecimalField(
                    value = form.distance,
                    onValueChange = viewModel::onDistanceChange,
                    label = "Distance",
                    icon = Icons.Rounded.Place,
                    unit = form.units.distanceUnit,
                )
                DecimalField(
                    value = form.price,
                    onValueChange = viewModel::onPriceChange,
                    label = "Fuel price",
                    icon = Icons.Rounded.LocalGasStation,
                    unit = form.units.pricePerVolumeUnit(form.currency),
                )
                DecimalField(
                    value = form.efficiency,
                    onValueChange = viewModel::onEfficiencyChange,
                    label = "Efficiency",
                    icon = Icons.Rounded.Speed,
                    unit = form.units.efficiencyUnit,
                )
                OutlinedTextField(
                    value = form.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerChip(
                    date = LocalDate.ofEpochDay(form.epochDay),
                    onDateChange = viewModel::onDateChange,
                )
                ResultCard(
                    estimate = state.estimate,
                    units = form.units,
                    currency = form.currency,
                    title = "Trip cost",
                )
                Button(
                    onClick = viewModel::save,
                    enabled = state.estimate != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Save changes")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete trip?",
            message = "This trip will be removed from your history.",
            confirmLabel = "Delete",
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}
