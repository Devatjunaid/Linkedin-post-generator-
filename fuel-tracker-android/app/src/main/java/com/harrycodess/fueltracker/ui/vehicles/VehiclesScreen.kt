package com.harrycodess.fueltracker.ui.vehicles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.ConfirmDialog
import com.harrycodess.fueltracker.ui.components.EmptyState
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.components.VehicleDialog

@Composable
fun VehiclesScreen(
    onBack: () -> Unit,
    viewModel: VehiclesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Vehicle?>(null) }
    var deleting by remember { mutableStateOf<Vehicle?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(title = "Vehicles", subtitle = "Tap a vehicle to edit it", onBack = onBack)
        Box(Modifier.fillMaxSize()) {
            if (state.items.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.DirectionsCar,
                    title = "No vehicles",
                    message = "Add a vehicle to pre-fill its efficiency in the calculator.",
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = { it.vehicle.id }) { item ->
                    VehicleRow(
                        item = item,
                        efficiencyText = "${Fmt.number(FuelMath.kmPerLToEfficiency(item.vehicle.efficiencyKmPerL, state.units))} ${state.units.efficiencyUnit}",
                        costText = Fmt.money(item.totalCost, state.currency),
                        onEdit = { editing = item.vehicle },
                        onDelete = { deleting = item.vehicle },
                    )
                }
            }
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add vehicle") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }

    if (showAdd) {
        VehicleDialog(
            units = state.units,
            onDismiss = { showAdd = false },
            onConfirm = { name, kmPerL ->
                viewModel.add(name, kmPerL)
                showAdd = false
            },
        )
    }
    editing?.let { vehicle ->
        VehicleDialog(
            units = state.units,
            vehicle = vehicle,
            onDismiss = { editing = null },
            onConfirm = { name, kmPerL ->
                viewModel.update(vehicle, name, kmPerL)
                editing = null
            },
        )
    }
    deleting?.let { vehicle ->
        ConfirmDialog(
            title = "Delete ${vehicle.name}?",
            message = "Saved trips are kept and still show this vehicle's name.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.delete(vehicle) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun VehicleRow(
    item: VehicleItem,
    efficiencyText: String,
    costText: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.vehicle.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "$efficiencyText · ${item.tripCount} trips · $costText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete") }
        }
    }
}
