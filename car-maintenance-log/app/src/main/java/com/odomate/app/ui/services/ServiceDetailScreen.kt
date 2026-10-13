package com.odomate.app.ui.services

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.data.VehicleSnapshot
import com.odomate.app.data.db.ServiceRecord
import com.odomate.app.domain.Level
import com.odomate.app.domain.ServiceStatus
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppCard
import com.odomate.app.ui.components.AppTextField
import com.odomate.app.ui.components.DateField
import com.odomate.app.ui.components.EmptyState
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.components.SecondaryButton
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.components.ServiceIconBadge
import com.odomate.app.ui.components.StatusPill
import com.odomate.app.ui.components.intervalText
import com.odomate.app.ui.components.levelLabel
import com.odomate.app.ui.components.statusDetail
import com.odomate.app.ui.components.statusShort
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.ui.theme.Type
import com.odomate.app.util.Formats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServiceDetailState(
    val status: ServiceStatus? = null,
    val history: List<ServiceRecord> = emptyList(),
    val snapshot: VehicleSnapshot? = null,
    val missing: Boolean = false,
)

class ServiceDetailViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val serviceId: Long = handle.get<Long>("id") ?: 0L

    val state = combine(container.repository.observeServiceType(serviceId), container.repository.activeSnapshot) { type, snap ->
        if (type == null) return@combine ServiceDetailState(missing = true)
        val snapshot = if (snap?.vehicle?.id == type.vehicleId) snap else container.repository.snapshotOnce(type.vehicleId)
        val status = snapshot?.statuses?.firstOrNull { it.type.id == type.id }
        ServiceDetailState(
            status = status,
            history = snapshot?.records?.filter { type.id in it.serviceTypeIds } ?: emptyList(),
            snapshot = snapshot,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceDetailState())

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.repository.getServiceType(serviceId)?.let { container.repository.updateServiceType(it.copy(enabled = enabled)) }
        }
    }

    fun setBaseline(odometer: Int?, date: Long?) {
        viewModelScope.launch { container.repository.setBaseline(serviceId, odometer, date) }
    }
}

@Composable
fun ServiceDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onLog: (Long) -> Unit,
    onOpenRecord: (Long) -> Unit,
) {
    val vm = appViewModel { c, h -> ServiceDetailViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val unit = settings.distanceUnit
    val c = AppTheme.colors
    var showBaseline by remember { mutableStateOf(false) }

    if (state.missing) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Service", onBack = onBack)
            EmptyState(Icons.Outlined.History, "Service not found", "It may have been deleted.")
        }
        return
    }
    val status = state.status ?: return
    val type = status.type

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Service", onBack = onBack) {
            IconButton(onClick = { onEdit(type.id) }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit service", tint = c.text) }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(status, unit)
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(levelLabel(status.level), status.level)
                        Text(type.name, style = MaterialTheme.typography.headlineSmall, color = c.text)
                        Text(statusDetail(status, unit), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        status.estimatedDueDate?.let {
                            if (status.level == Level.OK || status.level == Level.SOON) {
                                Text("At your pace: around ${Formats.shortDate(it)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                    }
                }
            }

            item {
                AppCard {
                    SectionLabel("Last done")
                    val odo = type.lastDoneOdometer
                    val date = type.lastDoneDate
                    if (odo == null && date == null) {
                        Text("Not recorded yet", color = c.text, style = MaterialTheme.typography.titleMedium)
                        Text("Tell us roughly when this was last done so reminders can start.", color = c.muted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            listOfNotNull(odo?.let { Formats.distance(it, unit) }, date?.let { Formats.date(it) }).joinToString(" · "),
                            color = c.text, style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (state.history.isNotEmpty()) "From your service log" else "Entered by you",
                            color = c.muted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(
                        if (state.history.isEmpty()) "Set last service" else "Edit starting point",
                        { showBaseline = true }, Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                AppCard {
                    SectionLabel("Interval & reminders")
                    DetailLine("Interval", intervalText(type.intervalDistance, type.intervalMonths, unit).removePrefix("Every ").replaceFirstChar { it.uppercase() })
                    if (type.intervalDistance != null) DetailLine("Remind me", "${Formats.distance(type.remindDistanceBefore, unit)} before")
                    if (type.intervalMonths != null) DetailLine("Remind me", "${type.remindDaysBefore} days before the date")
                    if (type.notes.isNotBlank()) DetailLine("Notes", type.notes)
                    HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Track this service", color = c.text, style = MaterialTheme.typography.titleSmall)
                            Text("Turn off if your car doesn't need it", color = c.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = type.enabled,
                            onCheckedChange = vm::setEnabled,
                            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
                        )
                    }
                }
            }

            item { SectionLabel("History") }
            if (state.history.isEmpty()) {
                item { Text("No logged services yet.", color = c.muted, style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(state.history, key = { it.id }) { record ->
                    AppCard(onClick = { onOpenRecord(record.id) }, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${Formats.date(record.date)} · ${Formats.distance(record.odometer, unit)}", color = c.text, style = MaterialTheme.typography.titleSmall)
                                val sub = listOf(record.shop, record.notes).filter { it.isNotBlank() }.joinToString(" · ")
                                if (sub.isNotBlank()) Text(sub, color = c.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            record.cost?.let { Text(Formats.money(it, settings.currency), color = c.text, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
        PrimaryButton(
            "Mark as done",
            { onLog(type.id) },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            icon = Icons.Outlined.Check,
        )
    }

    if (showBaseline) {
        BaselineDialog(
            initialOdometer = type.baselineOdometer ?: type.lastDoneOdometer,
            initialDate = type.baselineDate ?: type.lastDoneDate,
            unit = unit,
            hasDistance = type.intervalDistance != null,
            hasTime = type.intervalMonths != null,
            currentOdometer = state.snapshot?.currentOdometer,
            onSave = vm::setBaseline,
            onDismiss = { showBaseline = false },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = c.muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
        Text(value, color = c.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProgressRing(status: ServiceStatus, unit: String) {
    val c = AppTheme.colors
    val color = c.levelColor(status.level)
    val track = c.outline
    val sweep = if (status.level == Level.OVERDUE) 360f else status.progress * 360f
    Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val main = statusShort(status, unit)
            val parts = main.split(" ", limit = 2)
            Text(parts.first(), style = Type.mono.copy(fontSize = if (parts.first().length > 6) 18.sp else 24.sp), color = c.text)
            Text(
                (parts.getOrNull(1) ?: "") + when (status.level) {
                    Level.OVERDUE -> " over"
                    Level.SOON, Level.OK -> " left"
                    else -> ""
                },
                color = c.muted, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BaselineDialog(
    initialOdometer: Int?,
    initialDate: Long?,
    unit: String,
    hasDistance: Boolean,
    hasTime: Boolean,
    currentOdometer: Int?,
    onSave: (Int?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AppTheme.colors
    var odoText by remember { mutableStateOf(initialOdometer?.toString() ?: "") }
    var date by remember { mutableStateOf(initialDate ?: System.currentTimeMillis()) }
    val odo = Formats.parseInt(odoText)
    val tooHigh = odo != null && currentOdometer != null && odo > currentOdometer
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("When was it last done?", color = c.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("A rough guess is fine. Reminders are calculated from this point until you log the service.", color = c.muted, style = MaterialTheme.typography.bodySmall)
                if (hasDistance || !hasTime) {
                    AppTextField(
                        value = odoText,
                        onValueChange = { odoText = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = "Odometer then",
                        suffix = unit,
                        keyboardType = KeyboardType.Number,
                        isError = tooHigh,
                        supportingText = if (tooHigh) "Higher than the current odometer" else currentOdometer?.let { "Now: ${Formats.distance(it, unit)}" },
                    )
                }
                DateField("Date", date, { date = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = !tooHigh,
                onClick = { onSave(odo, date); onDismiss() },
            ) { Text("Save", color = c.accentText, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.muted) } },
    )
}
