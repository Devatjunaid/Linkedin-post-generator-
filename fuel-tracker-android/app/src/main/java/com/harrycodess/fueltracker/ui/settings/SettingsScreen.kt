package com.harrycodess.fueltracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.BuildConfig
import com.harrycodess.fueltracker.data.settings.ThemeMode
import com.harrycodess.fueltracker.domain.EfficiencyFormat
import com.harrycodess.fueltracker.domain.UnitSystem
import com.harrycodess.fueltracker.export.CsvExporter
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.components.SegmentedSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val currencyOptions = listOf("$", "€", "£", "₹", "Rs ", "¥", "A$", "C$", "R$", "AED ", "SAR ")

@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
    onOpenVehicles: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(title = "Settings", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection("Units") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Measurement system", style = MaterialTheme.typography.bodyMedium)
                    SegmentedSelector(
                        options = UnitSystem.entries,
                        selected = settings.units.system,
                        label = { if (it == UnitSystem.METRIC) "Metric (km, L)" else "Imperial (mi, gal)" },
                        onSelect = viewModel::setUnitSystem,
                    )
                    Text("Fuel efficiency (metric)", style = MaterialTheme.typography.bodyMedium)
                    SegmentedSelector(
                        options = EfficiencyFormat.entries,
                        selected = settings.units.efficiencyFormat,
                        label = { if (it == EfficiencyFormat.KM_PER_L) "km/L" else "L/100km" },
                        onSelect = viewModel::setEfficiencyFormat,
                        enabled = settings.units.isMetric,
                    )
                    if (!settings.units.isMetric) {
                        Text(
                            "Imperial always uses miles per US gallon (mpg).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Rounded.AttachMoney,
                    title = "Currency symbol",
                    subtitle = settings.currency.trim(),
                    onClick = { showCurrencyDialog = true },
                )
            }

            SettingsSection("Appearance") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium)
                    SegmentedSelector(
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        label = {
                            when (it) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        },
                        onSelect = viewModel::setThemeMode,
                    )
                }
            }

            SettingsSection("Data") {
                SettingsRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = "Vehicles",
                    subtitle = "${state.vehicleCount} saved · default efficiency per vehicle",
                    onClick = onOpenVehicles,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Rounded.FileDownload,
                    title = "Export all trips (CSV)",
                    subtitle = "${state.tripCount} trips · opens the share sheet",
                    onClick = {
                        scope.launch {
                            val trips = viewModel.allTrips()
                            if (trips.isEmpty()) {
                                snackbarHostState.showSnackbar("No trips to export")
                                return@launch
                            }
                            val intent = withContext(Dispatchers.IO) {
                                CsvExporter.createShareIntent(context, trips, settings)
                            }
                            context.startActivity(intent)
                        }
                    },
                )
            }

            SettingsSection("About") {
                if (privacyOptionsRequired) {
                    SettingsRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Ad privacy options",
                        subtitle = "Review or change your ad consent",
                        onClick = onPrivacyOptions,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = "Fuel & Trip Cost Tracker",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    onClick = null,
                )
            }
        }
    }

    if (showCurrencyDialog) {
        CurrencyDialog(
            current = settings.currency,
            onDismiss = { showCurrencyDialog = false },
            onSelect = {
                viewModel.setCurrency(it)
                showCurrencyDialog = false
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = if (onClick != null) {
            { Icon(Icons.Rounded.ChevronRight, contentDescription = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun CurrencyDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var custom by rememberSaveable { mutableStateOf(if (current in currencyOptions) "" else current.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency symbol") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                currencyOptions.forEach { symbol ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = symbol == current,
                                onClick = { onSelect(symbol) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = symbol == current, onClick = null)
                        Text(
                            "${symbol.trim()}   (e.g. ${symbol}12.50)",
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.take(4) },
                    label = { Text("Custom symbol") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = custom.isNotBlank(),
                onClick = { onSelect(custom.trim().let { if (it.length > 1) "$it " else it }) },
            ) { Text("Use custom") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
