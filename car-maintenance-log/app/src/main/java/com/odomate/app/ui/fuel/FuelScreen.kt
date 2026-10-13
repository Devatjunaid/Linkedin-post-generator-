package com.odomate.app.ui.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.data.db.FuelEntry
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppCard
import com.odomate.app.ui.components.AppTextField
import com.odomate.app.ui.components.ConfirmDialog
import com.odomate.app.ui.components.DateField
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats
import com.odomate.app.util.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FuelViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val entryId: Long = handle.get<Long>("id") ?: 0L
    var loaded by mutableStateOf(false)
        private set
    var date by mutableStateOf(System.currentTimeMillis())
    var odometer by mutableStateOf("")
    var volume by mutableStateOf("")
    var cost by mutableStateOf("")
    var fullTank by mutableStateOf(true)
    var station by mutableStateOf("")
    var notes by mutableStateOf("")
    var currentOdometer by mutableStateOf<Int?>(null)
        private set
    private var original: FuelEntry? = null
    private var vehicleId = 0L

    init {
        viewModelScope.launch {
            val existing = if (entryId != 0L) container.repository.getFuel(entryId) else null
            original = existing
            vehicleId = existing?.vehicleId ?: container.repository.activeVehicle.first()?.id ?: 0L
            currentOdometer = container.repository.highestReading(vehicleId)
            if (existing != null) {
                date = existing.date
                odometer = existing.odometer.toString()
                volume = Formats.decimal(existing.volume, 2)
                cost = existing.cost?.let { Formats.decimal(it, 2) } ?: ""
                fullTank = existing.fullTank
                station = existing.station
                notes = existing.notes
            } else {
                odometer = currentOdometer?.toString() ?: ""
            }
            loaded = true
        }
    }

    val isEdit get() = entryId != 0L

    fun validate(): String? = when {
        (Formats.parseInt(odometer) ?: 0) <= 0 -> "Enter the odometer reading"
        (Formats.parseDecimal(volume) ?: 0.0) <= 0.0 -> "Enter how much fuel you added"
        cost.isNotBlank() && Formats.parseDecimal(cost) == null -> "Cost doesn't look like a number"
        else -> null
    }

    suspend fun save(): Boolean {
        if (validate() != null || vehicleId == 0L) return false
        container.repository.saveFuel(
            FuelEntry(
                id = original?.id ?: 0L,
                vehicleId = vehicleId,
                date = date,
                odometer = Formats.parseInt(odometer) ?: 0,
                volume = Formats.parseDecimal(volume) ?: 0.0,
                cost = Formats.parseDecimal(cost),
                fullTank = fullTank,
                station = station.trim(),
                notes = notes.trim(),
            )
        )
        return true
    }

    suspend fun delete() {
        original?.let { container.repository.deleteFuel(it) }
    }
}

@Composable
fun FuelScreen(
    scannedOdometer: Int?,
    onConsumeScanned: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val vm = appViewModel { c, h -> FuelViewModel(c, h) }
    val settings = LocalSettings.current
    val c = AppTheme.colors
    val context = LocalContext.current
    val ads = LocalAppContainer.current.ads
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(scannedOdometer) {
        if (scannedOdometer != null) {
            vm.odometer = scannedOdometer.toString()
            onConsumeScanned()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (vm.isEdit) "Edit fill-up" else "Add fuel", onBack = onBack) {
            if (vm.isEdit) IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, "Delete fill-up", tint = c.danger) }
        }
        if (!vm.loaded) return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateField("Date", vm.date, { vm.date = it })
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    vm.odometer, { vm.odometer = it.filter(Char::isDigit).take(8) }, "Odometer",
                    Modifier.weight(1f), suffix = settings.distanceUnit, keyboardType = KeyboardType.Number, mono = true,
                    supportingText = vm.currentOdometer?.let { "Current: ${Formats.distance(it, settings.distanceUnit)}" },
                )
                Box(
                    Modifier.padding(top = 8.dp).size(56.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft).clickable(onClick = onScan),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.PhotoCamera, "Scan odometer", tint = c.accentText) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    vm.volume, { vm.volume = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(8) }, "Amount",
                    Modifier.weight(1f), suffix = settings.volumeUnit, keyboardType = KeyboardType.Decimal,
                )
                AppTextField(
                    vm.cost, { vm.cost = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(10) }, "Total cost",
                    Modifier.weight(1f), prefix = settings.currency, keyboardType = KeyboardType.Decimal,
                )
            }
            val v = Formats.parseDecimal(vm.volume)
            val total = Formats.parseDecimal(vm.cost)
            if (v != null && v > 0 && total != null && total > 0) {
                Text(
                    "${Formats.money(total / v, settings.currency)} per ${settings.volumeUnit}",
                    color = c.muted, style = MaterialTheme.typography.bodySmall,
                )
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Filled the tank", color = c.text, style = MaterialTheme.typography.titleSmall)
                        Text("Needed to calculate fuel economy accurately", color = c.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = vm.fullTank, onCheckedChange = { vm.fullTank = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
                    )
                }
            }
            AppTextField(vm.station, { vm.station = it.take(60) }, "Station (optional)")
            AppTextField(vm.notes, { vm.notes = it.take(300) }, "Notes (optional)", singleLine = false)
            error?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton(
            if (vm.isEdit) "Save changes" else "Save fill-up",
            {
                error = vm.validate()
                if (error == null && !saving) {
                    saving = true
                    scope.launch {
                        if (vm.save()) {
                            ads.showInterstitial(context.findActivity()) {
                                onDone()
                                message(if (vm.isEdit) "Fill-up updated" else "Fill-up saved")
                            }
                        } else saving = false
                    }
                }
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = !saving,
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this fill-up?",
            text = "This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { scope.launch { vm.delete(); onDone(); message("Fill-up deleted") } },
            onDismiss = { showDelete = false },
        )
    }
}
