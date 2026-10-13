package com.odomate.app.ui.log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.odomate.app.AppContainer
import com.odomate.app.data.db.ServiceRecord
import com.odomate.app.data.db.ServiceType
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppTextField
import com.odomate.app.ui.components.ConfirmDialog
import com.odomate.app.ui.components.DateField
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.components.intervalText
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats
import com.odomate.app.util.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class LogServiceViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val recordId: Long = handle.get<Long>("recordId") ?: 0L
    private val preselectId: Long = handle.get<Long>("serviceId") ?: 0L

    var loaded by mutableStateOf(false)

        private set
    var types by mutableStateOf<List<ServiceType>>(emptyList())
        private set
    var selected by mutableStateOf<Set<Long>>(emptySet())
    var date by mutableStateOf(System.currentTimeMillis())
    var odometer by mutableStateOf("")
    var cost by mutableStateOf("")
    var shop by mutableStateOf("")
    var notes by mutableStateOf("")
    var receiptPath by mutableStateOf<String?>(null)
        private set
    var currentOdometer by mutableStateOf<Int?>(null)
        private set
    private var original: ServiceRecord? = null
    private var vehicleId: Long = 0
    private var saved = false

    init {
        viewModelScope.launch {
            val existing = if (recordId != 0L) container.repository.getRecord(recordId) else null
            original = existing
            vehicleId = existing?.vehicleId ?: container.repository.activeVehicle.first()?.id ?: 0L
            types = container.repository.serviceTypes(vehicleId).sortedWith(compareBy<ServiceType> { !it.enabled }.thenBy { it.sortOrder })
            currentOdometer = container.repository.highestReading(vehicleId)
            if (existing != null) {
                selected = existing.serviceTypeIds.toSet()
                date = existing.date
                odometer = existing.odometer.toString()
                cost = existing.cost?.let { Formats.decimal(it, 2) } ?: ""
                shop = existing.shop
                notes = existing.notes
                receiptPath = existing.receiptPath
            } else {
                if (preselectId != 0L) selected = setOf(preselectId)
                odometer = currentOdometer?.toString() ?: ""
            }
            loaded = true
        }
    }

    fun toggle(id: Long) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun attachReceipt(uri: Uri) {
        viewModelScope.launch {
            val path = container.repository.importReceipt(uri) ?: return@launch
            discardNewReceipt()
            receiptPath = path
        }
    }

    fun removeReceipt() {
        discardNewReceipt()
        receiptPath = null
    }

    private fun discardNewReceipt() {
        val current = receiptPath
        if (current != null && current != original?.receiptPath) container.repository.deleteReceiptFile(current)
    }

    val isEdit: Boolean get() = recordId != 0L

    fun validate(): String? = when {
        selected.isEmpty() -> "Pick at least one service"
        (Formats.parseInt(odometer) ?: 0) <= 0 -> "Enter the odometer reading"
        cost.isNotBlank() && Formats.parseDecimal(cost) == null -> "Cost doesn't look like a number"
        else -> null
    }

    suspend fun save(): Boolean {
        if (validate() != null || vehicleId == 0L) return false
        val names = types.filter { it.id in selected }.joinToString(", ") { it.name }
        val record = ServiceRecord(
            id = original?.id ?: 0L,
            vehicleId = vehicleId,
            date = date,
            odometer = Formats.parseInt(odometer) ?: 0,
            cost = Formats.parseDecimal(cost),
            shop = shop.trim(),
            notes = notes.trim(),
            receiptPath = receiptPath,
            serviceTypeIds = selected.toList(),
            serviceNames = names.ifBlank { original?.serviceNames ?: "Service" },
        )
        container.repository.saveRecord(record)
        saved = true
        return true
    }

    suspend fun delete() {
        original?.let { container.repository.deleteRecord(it) }
        saved = true
    }

    override fun onCleared() {
        if (!saved) discardNewReceipt()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogServiceScreen(
    scannedOdometer: Int?,
    onConsumeScanned: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val vm = appViewModel { c, h -> LogServiceViewModel(c, h) }
    val settings = LocalSettings.current
    val unit = settings.distanceUnit
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
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.attachReceipt(uri)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (vm.isEdit) "Edit service log" else "Log a service", onBack = onBack) {
            if (vm.isEdit) {
                IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, "Delete entry", tint = c.danger) }
            }
        }
        if (!vm.loaded) return@Column

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("What was done?")
            Text("Pick everything done in this visit. Each reminder resets.", color = c.muted, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                vm.types.forEach { t ->
                    val on = t.id in vm.selected
                    FilterChip(
                        selected = on,
                        onClick = { vm.toggle(t.id) },
                        label = { Text(t.name) },
                        modifier = Modifier.heightIn(min = 40.dp),
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = c.surface, labelColor = if (t.enabled) c.text else c.muted,
                            selectedContainerColor = c.accent, selectedLabelColor = c.onAccent,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = on,
                            borderColor = c.outline, selectedBorderColor = c.accent,
                        ),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField("Date", vm.date, { vm.date = it }, Modifier.weight(1f))
                AppTextField(
                    vm.cost, { vm.cost = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(10) }, "Cost",
                    Modifier.weight(1f), prefix = settings.currency, keyboardType = KeyboardType.Decimal,
                )
            }

            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    vm.odometer, { vm.odometer = it.filter(Char::isDigit).take(8) }, "Odometer",
                    Modifier.weight(1f), suffix = unit, keyboardType = KeyboardType.Number, mono = true,
                    supportingText = vm.currentOdometer?.let { "Current: ${Formats.distance(it, unit)}" },
                )
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.accentSoft)
                        .clickable(onClick = onScan),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.PhotoCamera, "Scan odometer", tint = c.accentText) }
            }

            AppTextField(vm.shop, { vm.shop = it.take(60) }, "Shop (optional)", placeholder = "Where was it done? Or DIY")
            AppTextField(vm.notes, { vm.notes = it.take(500) }, "Notes (optional)", placeholder = "Oil grade, parts, warranty…", singleLine = false)

            val receipt = vm.receiptPath
            if (receipt == null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.5.dp, c.outline), RoundedCornerShape(14.dp))
                        .clickable { receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, tint = c.muted)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach receipt photo", color = c.text, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp))) {
                    AsyncImage(model = File(receipt), contentDescription = "Receipt", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = vm::removeReceipt,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp).clip(CircleShape).background(c.surface),
                    ) { Icon(Icons.Outlined.Close, "Remove receipt", tint = c.text) }
                }
            }

            val selectedTypes = vm.types.filter { it.id in vm.selected }
            if (selectedTypes.isNotEmpty()) {
                val odo = Formats.parseInt(vm.odometer)
                val oil = selectedTypes.first()
                val next = buildList {
                    if (odo != null && oil.intervalDistance != null) add(Formats.distance(odo + oil.intervalDistance, unit))
                    if (oil.intervalMonths != null) {
                        val d = com.odomate.app.domain.StatusCalculator.toLocalDate(vm.date).plusMonths(oil.intervalMonths.toLong())
                        add(Formats.date(d))
                    }
                }.joinToString(" or ")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.okSoft).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.NotificationsActive, null, tint = c.ok, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Saving resets ${selectedTypes.size} ${if (selectedTypes.size == 1) "reminder" else "reminders"}." +
                            if (next.isNotBlank()) " Next ${oil.name.lowercase()}: $next." else " ${intervalText(oil.intervalDistance, oil.intervalMonths, unit)}.",
                        color = c.text, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            error?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
        }

        PrimaryButton(
            text = when {
                vm.isEdit -> "Save changes"
                vm.selected.size > 1 -> "Save ${vm.selected.size} services"
                else -> "Save service"
            },
            onClick = {
                error = vm.validate()
                if (error == null && !saving) {
                    saving = true
                    scope.launch {
                        if (vm.save()) {
                            ads.showInterstitial(context.findActivity()) {
                                onDone()
                                message(if (vm.isEdit) "Service updated" else "Service logged. Reminders reset.")
                            }
                        } else saving = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = !saving,
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this entry?",
            text = "Reminders for these services will go back to the previous record.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { scope.launch { vm.delete(); onDone(); message("Entry deleted") } },
            onDismiss = { showDelete = false },
        )
    }
}
