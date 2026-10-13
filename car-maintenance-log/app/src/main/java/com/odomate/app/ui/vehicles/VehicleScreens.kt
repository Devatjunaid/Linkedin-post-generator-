package com.odomate.app.ui.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.data.db.Vehicle
import com.odomate.app.data.db.subtitle
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppCard
import com.odomate.app.ui.components.AppTextField
import com.odomate.app.ui.components.ConfirmDialog
import com.odomate.app.ui.components.IconBadge
import com.odomate.app.ui.components.LogoMark
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ garage list

class VehiclesViewModel(private val container: AppContainer) : ViewModel() {
    val state = combine(container.repository.vehicles, container.repository.activeVehicle) { list, active -> list to active?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Vehicle>() to null)

    fun select(id: Long) {
        viewModelScope.launch { container.repository.setActiveVehicle(id) }
    }
}

@Composable
fun VehiclesScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, onAdd: () -> Unit) {
    val vm = appViewModel { c, _ -> VehiclesViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val (vehicles, activeId) = state
    val c = AppTheme.colors
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("My garage", subtitle = "${vehicles.size} ${if (vehicles.size == 1) "vehicle" else "vehicles"}", onBack = onBack) {
            IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "Add vehicle", tint = c.text) }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(vehicles, key = { it.id }) { v ->
                val active = v.id == activeId
                AppCard(onClick = { vm.select(v.id) }, color = if (active) c.accentSoft else c.surface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.DirectionsCar, if (active) c.onAccent else c.muted, if (active) c.accent else c.surface2)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, color = c.text, style = MaterialTheme.typography.titleMedium)
                            val sub = listOf(v.subtitle, v.plate).filter { it.isNotBlank() }.joinToString(" · ")
                            if (sub.isNotBlank()) Text(sub, color = c.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (active) Icon(Icons.Outlined.CheckCircle, "Active", tint = c.accentText)
                        IconButton(onClick = { onEdit(v.id) }) { Icon(Icons.Outlined.Edit, "Edit ${v.name}", tint = c.muted) }
                    }
                }
            }
            item {
                Text(
                    "Tap a vehicle to make it active. Each one keeps its own odometer, services and history.",
                    color = c.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ add / edit

class VehicleEditViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val vehicleId: Long = handle.get<Long>("id") ?: 0L
    var name by mutableStateOf("")
    var make by mutableStateOf("")
    var model by mutableStateOf("")
    var year by mutableStateOf("")
    var plate by mutableStateOf("")
    var odometer by mutableStateOf("")
    var loaded by mutableStateOf(vehicleId == 0L)
        private set
    private var original: Vehicle? = null

    init {
        if (vehicleId != 0L) viewModelScope.launch {
            container.repository.getVehicle(vehicleId)?.let { v ->
                original = v
                name = v.name; make = v.make; model = v.model
                year = v.year?.toString() ?: ""; plate = v.plate
            }
            loaded = true
        }
    }

    val isEdit get() = vehicleId != 0L

    fun validate(): String? = when {
        name.isBlank() && make.isBlank() && model.isBlank() -> "Give your vehicle a name"
        !isEdit && (Formats.parseInt(odometer) ?: -1) < 0 -> "Enter the current odometer reading"
        year.isNotBlank() && (year.toIntOrNull() ?: 0) !in 1900..2100 -> "Year doesn't look right"
        else -> null
    }

    suspend fun save(): Long {
        val displayName = name.trim().ifBlank { listOf(make.trim(), model.trim()).filter { it.isNotBlank() }.joinToString(" ") }
        val base = original
        return if (base != null) {
            container.repository.updateVehicle(
                base.copy(name = displayName, make = make.trim(), model = model.trim(), year = year.toIntOrNull(), plate = plate.trim())
            )
            base.id
        } else {
            container.repository.addVehicle(
                Vehicle(name = displayName, make = make.trim(), model = model.trim(), year = year.toIntOrNull(), plate = plate.trim()),
                Formats.parseInt(odometer) ?: 0,
            )
        }
    }

    suspend fun delete(): Boolean {
        original?.let { container.repository.deleteVehicle(it) }
        return container.repository.vehicles.first().isEmpty()
    }

    suspend fun setUnit(unit: String) = container.settings.setDistanceUnit(unit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditScreen(
    onboarding: Boolean,
    scannedOdometer: Int?,
    onConsumeScanned: () -> Unit,
    onScan: () -> Unit,
    onBack: (() -> Unit)?,
    onSaved: () -> Unit,
    onGarageEmpty: () -> Unit,
) {
    val vm = appViewModel { c, h -> VehicleEditViewModel(c, h) }
    val settings = LocalSettings.current
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    val message = LocalMessenger.current
    var error by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(scannedOdometer) {
        if (scannedOdometer != null) {
            vm.odometer = scannedOdometer.toString()
            onConsumeScanned()
        }
    }
    if (!vm.loaded) return

    Column(Modifier.fillMaxSize()) {
        if (onboarding) {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LogoMark(64.dp)
                Text("Let's add your car", style = MaterialTheme.typography.headlineMedium, color = c.text)
                Text(
                    "We'll set up oil changes, tires, brakes, filters, fluids, battery and paperwork reminders. You can change any interval later.",
                    style = MaterialTheme.typography.bodyMedium, color = c.muted,
                )
            }
        } else {
            ScreenHeader(if (vm.isEdit) "Edit vehicle" else "Add vehicle", onBack = onBack) {
                if (vm.isEdit) IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, "Delete vehicle", tint = c.danger) }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(vm.name, { vm.name = it.take(30) }, "Nickname", placeholder = "e.g. My Civic, Family SUV")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(vm.make, { vm.make = it.take(30) }, "Make", Modifier.weight(1f), placeholder = "Honda")
                AppTextField(vm.model, { vm.model = it.take(30) }, "Model", Modifier.weight(1f), placeholder = "Civic")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(vm.year, { vm.year = it.filter(Char::isDigit).take(4) }, "Year", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                AppTextField(vm.plate, { vm.plate = it.uppercase().take(15) }, "Plate (optional)", Modifier.weight(1f))
            }

            if (!vm.isEdit) {
                if (onboarding) {
                    SectionLabel("Distance unit")
                    val units = listOf("km" to "Kilometres", "mi" to "Miles")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        units.forEachIndexed { i, (key, label) ->
                            SegmentedButton(
                                selected = settings.distanceUnit == key,
                                onClick = { scope.launch { vm.setUnit(key) } },
                                shape = SegmentedButtonDefaults.itemShape(i, units.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = c.accent, activeContentColor = c.onAccent,
                                    inactiveContainerColor = c.surface, inactiveContentColor = c.muted,
                                    activeBorderColor = c.accent, inactiveBorderColor = c.outline,
                                ),
                                icon = {},
                            ) { Text(label) }
                        }
                    }
                }
                SectionLabel("Current odometer")
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(
                        vm.odometer, { vm.odometer = it.filter(Char::isDigit).take(8) }, "Odometer",
                        Modifier.weight(1f), suffix = settings.distanceUnit, keyboardType = KeyboardType.Number, mono = true,
                    )
                    Box(
                        Modifier.padding(top = 8.dp).size(56.dp).clip(RoundedCornerShape(14.dp)).background(c.accent).clickable(onClick = onScan),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.PhotoCamera, "Scan odometer", tint = c.onAccent) }
                }
                Text("Tip: tap the camera to read it straight from your dashboard.", color = c.muted, style = MaterialTheme.typography.bodySmall)
            }
            error?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
        }

        PrimaryButton(
            when {
                onboarding -> "Start tracking"
                vm.isEdit -> "Save changes"
                else -> "Add vehicle"
            },
            {
                error = vm.validate()
                if (error == null && !saving) {
                    saving = true
                    scope.launch {
                        vm.save()
                        message(if (vm.isEdit) "Vehicle updated" else "Vehicle added")
                        onSaved()
                    }
                }
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = !saving,
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete ${vm.name.ifBlank { "this vehicle" }}?",
            text = "All of its readings, services, fill-ups and receipts are deleted. This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                scope.launch {
                    val empty = vm.delete()
                    message("Vehicle deleted")
                    if (empty) onGarageEmpty() else onBack?.invoke()
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}
