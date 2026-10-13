package com.odomate.app.ui.services

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.odomate.app.AppContainer
import com.odomate.app.data.Category
import com.odomate.app.data.ServiceTemplates
import com.odomate.app.data.db.ServiceType
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppTextField
import com.odomate.app.ui.components.ConfirmDialog
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.components.serviceIcon
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ServiceEditViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val serviceId: Long = handle.get<Long>("id") ?: 0L

    suspend fun load(): ServiceType? = if (serviceId == 0L) null else container.repository.getServiceType(serviceId)

    suspend fun activeVehicleId(): Long? = container.repository.activeVehicle.first()?.id

    suspend fun save(type: ServiceType): Long = container.repository.saveServiceType(type)

    suspend fun delete(type: ServiceType) = container.repository.deleteServiceType(type)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceEditScreen(onBack: () -> Unit, onDeleted: () -> Unit) {
    val vm = appViewModel { c, h -> ServiceEditViewModel(c, h) }
    val settings = LocalSettings.current
    val unit = settings.distanceUnit
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    val message = LocalMessenger.current

    var original by remember { mutableStateOf<ServiceType?>(null) }
    var loaded by remember { mutableStateOf(vm.serviceId == 0L) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("wrench") }
    var distance by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var remindDistance by remember { mutableStateOf(ServiceTemplates.defaultReminderDistance(settings.miles).toString()) }
    var remindDays by remember { mutableStateOf("7") }
    var notes by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vm.serviceId) {
        vm.load()?.let { t ->
            original = t
            name = t.name
            icon = t.iconKey
            distance = t.intervalDistance?.toString() ?: ""
            months = t.intervalMonths?.toString() ?: ""
            remindDistance = t.remindDistanceBefore.toString()
            remindDays = t.remindDaysBefore.toString()
            notes = t.notes
        }
        loaded = true
    }
    if (!loaded) return

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (vm.serviceId == 0L) "New service" else "Edit service", onBack = onBack) {
            if (original != null) {
                IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete service", tint = c.danger) }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(name, { name = it.take(40) }, "Name", placeholder = "e.g. Timing belt, Car wash")

            SectionLabel("Icon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceTemplates.customIconChoices.forEach { key ->
                    val selected = key == icon
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) c.accent else c.surface)
                            .border(1.dp, if (selected) c.accent else c.outline, RoundedCornerShape(14.dp))
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(serviceIcon(key), contentDescription = key, tint = if (selected) c.onAccent else c.muted)
                    }
                }
            }

            SectionLabel("Repeat every")
            Text("Fill one or both. Whichever comes first triggers the reminder.", color = c.muted, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(distance, { distance = it.filter(Char::isDigit).take(7) }, "Distance", Modifier.weight(1f), suffix = unit, keyboardType = KeyboardType.Number)
                AppTextField(months, { months = it.filter(Char::isDigit).take(3) }, "Time", Modifier.weight(1f), suffix = "months", keyboardType = KeyboardType.Number)
            }

            SectionLabel("Remind me before")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(remindDistance, { remindDistance = it.filter(Char::isDigit).take(6) }, "Distance", Modifier.weight(1f), suffix = unit, keyboardType = KeyboardType.Number)
                AppTextField(remindDays, { remindDays = it.filter(Char::isDigit).take(3) }, "Days", Modifier.weight(1f), suffix = "days", keyboardType = KeyboardType.Number)
            }

            AppTextField(notes, { notes = it.take(300) }, "Notes (optional)", placeholder = "Oil grade, part numbers, preferred shop…", singleLine = false)

            error?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton(
            "Save",
            {
                val dist = Formats.parseInt(distance)?.takeIf { it > 0 }
                val mon = Formats.parseInt(months)?.takeIf { it > 0 }
                when {
                    name.isBlank() -> error = "Give the service a name"
                    dist == null && mon == null -> error = "Set a distance, a time, or both"
                    else -> scope.launch {
                        val base = original
                        val type = if (base != null) {
                            base.copy(
                                name = name.trim(), iconKey = icon, intervalDistance = dist, intervalMonths = mon,
                                remindDistanceBefore = Formats.parseInt(remindDistance) ?: base.remindDistanceBefore,
                                remindDaysBefore = Formats.parseInt(remindDays) ?: base.remindDaysBefore,
                                notes = notes.trim(), notifiedLevel = 0, notifiedAt = 0,
                            )
                        } else {
                            val vehicleId = vm.activeVehicleId() ?: return@launch
                            ServiceType(
                                vehicleId = vehicleId, name = name.trim(), iconKey = icon, category = Category.CUSTOM,
                                intervalDistance = dist, intervalMonths = mon,
                                remindDistanceBefore = Formats.parseInt(remindDistance) ?: ServiceTemplates.defaultReminderDistance(settings.miles),
                                remindDaysBefore = Formats.parseInt(remindDays) ?: 7,
                                isCustom = true, notes = notes.trim(),
                            )
                        }
                        vm.save(type)
                        message("Saved ${type.name}")
                        onBack()
                    }
                }
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }

    if (showDelete) {
        original?.let { t ->
            ConfirmDialog(
                title = "Delete ${t.name}?",
                text = "The reminder is removed. Past service records stay in your history.",
                confirmLabel = "Delete",
                destructive = true,
                onConfirm = { scope.launch { vm.delete(t); onDeleted() } },
                onDismiss = { showDelete = false },
            )
        }
    }
}
