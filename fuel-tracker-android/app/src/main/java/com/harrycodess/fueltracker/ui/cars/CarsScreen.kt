package com.harrycodess.fueltracker.ui.cars

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.data.CarCategory
import com.harrycodess.fueltracker.data.CatalogCar
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.EmptyState
import com.harrycodess.fueltracker.ui.components.IconTile
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.theme.InstrumentLabel
import com.harrycodess.fueltracker.ui.theme.ReadoutMedium

@Composable
fun CarsScreen(
    snackbarHostState: SnackbarHostState,
    onUseInCalculator: () -> Unit,
    viewModel: CarsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CarsEvent.AddedToVehicles ->
                    snackbarHostState.showSnackbar("${event.name} added to your vehicles")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(title = "Car Mileage Guide", subtitle = "Typical real-world averages")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search make or model, e.g. Corolla") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "efficient") {
                        FilterChip(
                            selected = state.sort == CarSort.MOST_EFFICIENT,
                            onClick = {
                                viewModel.onSortChange(
                                    if (state.sort == CarSort.MOST_EFFICIENT) CarSort.POPULAR else CarSort.MOST_EFFICIENT,
                                )
                            },
                            label = { Text("Most efficient") },
                            leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                    item(key = "all") {
                        FilterChip(
                            selected = state.category == null,
                            onClick = { viewModel.onCategoryChange(null) },
                            label = { Text("All") },
                        )
                    }
                    items(CarCategory.entries, key = { it.name }) { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.onCategoryChange(category) },
                            label = { Text(category.label) },
                        )
                    }
                }
            }
            item(key = "count") {
                Text(
                    "${state.results.size} VEHICLES · ${state.units.efficiencyUnit.uppercase()}",
                    style = InstrumentLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            if (state.results.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = "No vehicles found",
                        message = "Try a different name or category.",
                    )
                }
            }
            items(state.results, key = { it.id }) { car ->
                CarRow(
                    car = car,
                    units = state.units,
                    onUse = {
                        viewModel.useInCalculator(car)
                        onUseInCalculator()
                    },
                    onAdd = { viewModel.addToMyVehicles(car) },
                )
            }
            item(key = "disclaimer") {
                Text(
                    "Figures are typical real-world combined averages for guidance only. " +
                        "Your mileage depends on driving style, traffic, load, maintenance and the exact variant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CarRow(
    car: CatalogCar,
    units: UnitPrefs,
    onUse: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = car.category.icon(),
                    tint = if (car.category == CarCategory.HYBRID) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        car.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${car.engine} · ${car.category.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Fmt.number(FuelMath.kmPerLToEfficiency(car.kmPerL, units)),
                        style = ReadoutMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        units.efficiencyUnit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = onUse,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Use in calculator")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedIconButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add ${car.name} to my vehicles")
                }
            }
        }
    }
}

private fun CarCategory.icon(): ImageVector = when (this) {
    CarCategory.MOTORCYCLE -> Icons.Rounded.TwoWheeler
    CarCategory.PICKUP -> Icons.Rounded.LocalShipping
    CarCategory.HYBRID -> Icons.Rounded.Eco
    else -> Icons.Rounded.DirectionsCar
}
