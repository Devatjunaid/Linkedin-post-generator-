@file:OptIn(ExperimentalMaterial3Api::class)

package com.harrycodess.fueltracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.data.local.Trip
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.export.CsvExporter
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.CockpitCard
import com.harrycodess.fueltracker.ui.components.EmptyState
import com.harrycodess.fueltracker.ui.components.Readout
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.components.TripCard
import com.harrycodess.fueltracker.ui.preview.PreviewData
import com.harrycodess.fueltracker.ui.theme.FuelTrackerTheme
import com.harrycodess.fueltracker.ui.theme.InstrumentLabel
import com.harrycodess.fueltracker.ui.theme.ReadoutLarge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@Composable
fun HistoryScreen(
    snackbarHostState: SnackbarHostState,
    onTripClick: (Trip) -> Unit,
    onOpenSettings: () -> Unit,
    onGoToCalculator: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    HistoryContent(
        state = state,
        onTripClick = onTripClick,
        onDeleteTrip = { trip ->
            viewModel.deleteTrip(trip)
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "Trip deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restoreTrip(trip)
            }
        },
        onVehicleFilter = viewModel::setVehicleFilter,
        onMonthFilter = viewModel::setMonthFilter,
        onClearFilters = viewModel::clearFilters,
        onExport = {
            scope.launch {
                val trips = state.filteredTrips
                if (trips.isEmpty()) {
                    snackbarHostState.showSnackbar("No trips to export")
                    return@launch
                }
                val intent = withContext(Dispatchers.IO) {
                    CsvExporter.createShareIntent(context, trips, state.settings)
                }
                context.startActivity(intent)
            }
        },
        onOpenSettings = onOpenSettings,
        onGoToCalculator = onGoToCalculator,
    )
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    onTripClick: (Trip) -> Unit,
    onDeleteTrip: (Trip) -> Unit,
    onVehicleFilter: (Long?) -> Unit,
    onMonthFilter: (YearMonth?) -> Unit,
    onClearFilters: () -> Unit,
    onExport: () -> Unit,
    onOpenSettings: () -> Unit,
    onGoToCalculator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = state.settings.units
    val currency = state.settings.currency

    Column(modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "Trip History",
            subtitle = when (state.totalTripCount) {
                0 -> "No trips yet"
                1 -> "1 trip saved"
                else -> "${state.totalTripCount} trips saved"
            },
            actions = {
                IconButton(onClick = onExport, enabled = state.totalTripCount > 0) {
                    Icon(Icons.Rounded.Share, contentDescription = "Export CSV")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            },
        )

        if (!state.isLoading && state.totalTripCount == 0) {
            EmptyState(
                icon = Icons.Rounded.History,
                title = "No trips yet",
                message = "Calculate a trip and tap Save Trip. Your history and stats will show up here.",
                action = { TextButton(onClick = onGoToCalculator) { Text("Open calculator") } },
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "stats") {
                StatsCard(state = state, units = units, currency = currency)
            }
            item(key = "filters") {
                HistoryFilters(
                    state = state,
                    onVehicleFilter = onVehicleFilter,
                    onMonthFilter = onMonthFilter,
                )
            }
            if (state.groups.isEmpty()) {
                item(key = "no-match") {
                    EmptyState(
                        icon = Icons.Rounded.History,
                        title = "No matching trips",
                        message = "Try another vehicle or month.",
                        action = { TextButton(onClick = onClearFilters) { Text("Clear filters") } },
                    )
                }
            }
            state.groups.forEach { group ->
                item(key = "month-${group.month}") {
                    MonthHeader(group, currency, Modifier.animateItem())
                }
                items(group.trips, key = { it.id }) { trip ->
                    SwipeToDeleteTrip(
                        trip = trip,
                        units = units,
                        currency = currency,
                        onClick = { onTripClick(trip) },
                        onDelete = { onDeleteTrip(trip) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(state: HistoryUiState, units: UnitPrefs, currency: String) {
    val stats = state.stats
    val period = state.filter.month?.let { Fmt.shortMonth(it) } ?: "All time"
    CockpitCard {
        Text(
            "SPENT · ${period.uppercase()}",
            style = InstrumentLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            Fmt.money(stats.totalCost, currency),
            style = ReadoutLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "${stats.tripCount} trips",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Readout(
                label = "Distance",
                value = "${Fmt.number(FuelMath.kmToDistance(stats.totalKm, units.system), 0)} ${units.distanceUnit}",
                icon = Icons.Rounded.Place,
                modifier = Modifier.weight(1f),
            )
            Readout(
                label = "Avg ${units.efficiencyUnit}",
                value = if (stats.avgKmPerL > 0) Fmt.number(FuelMath.kmPerLToEfficiency(stats.avgKmPerL, units)) else "—",
                icon = Icons.Rounded.Speed,
                modifier = Modifier.weight(1f),
            )
            Readout(
                label = "Per ${units.distanceUnit}",
                value = Fmt.money(FuelMath.costPerDistance(stats.costPerKm, units.system), currency),
                icon = Icons.Rounded.LocalGasStation,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HistoryFilters(
    state: HistoryUiState,
    onVehicleFilter: (Long?) -> Unit,
    onMonthFilter: (YearMonth?) -> Unit,
) {
    var monthMenuOpen by remember { mutableStateOf(false) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "month") {
            Box {
                FilterChip(
                    selected = state.filter.month != null,
                    onClick = { monthMenuOpen = true },
                    label = { Text(state.filter.month?.let { Fmt.shortMonth(it) } ?: "All months") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                )
                DropdownMenu(expanded = monthMenuOpen, onDismissRequest = { monthMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("All months") },
                        onClick = {
                            onMonthFilter(null)
                            monthMenuOpen = false
                        },
                    )
                    state.availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(Fmt.month(month)) },
                            onClick = {
                                onMonthFilter(month)
                                monthMenuOpen = false
                            },
                        )
                    }
                }
            }
        }
        item(key = "all-vehicles") {
            FilterChip(
                selected = state.filter.vehicleId == null,
                onClick = { onVehicleFilter(null) },
                label = { Text("All vehicles") },
            )
        }
        items(state.vehicles, key = { "v-${it.id}" }) { vehicle ->
            FilterChip(
                selected = state.filter.vehicleId == vehicle.id,
                onClick = { onVehicleFilter(vehicle.id) },
                label = { Text(vehicle.name) },
            )
        }
    }
}

@Composable
private fun MonthHeader(group: MonthGroup, currency: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Fmt.month(group.month),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            Fmt.money(group.total, currency),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwipeToDeleteTrip(
    trip: Trip,
    units: UnitPrefs,
    currency: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        TripCard(trip = trip, units = units, currency = currency, onClick = onClick)
    }
}

@PreviewLightDark
@Composable
private fun HistoryPreview() {
    FuelTrackerTheme {
        Surface {
            HistoryContent(
                state = PreviewData.historyState,
                onTripClick = {},
                onDeleteTrip = {},
                onVehicleFilter = {},
                onMonthFilter = {},
                onClearFilters = {},
                onExport = {},
                onOpenSettings = {},
                onGoToCalculator = {},
            )
        }
    }
}
