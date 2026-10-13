package com.snaptensio.bplog.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaptensio.bplog.ads.NativeAdCard
import com.snaptensio.bplog.data.Medication
import com.snaptensio.bplog.data.Reminder
import com.snaptensio.bplog.data.timeList
import com.snaptensio.bplog.domain.DAY_MS
import com.snaptensio.bplog.domain.dayKey
import com.snaptensio.bplog.domain.startOfDay
import com.snaptensio.bplog.reminders.Notifications
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.SectionCard
import com.snaptensio.bplog.ui.components.SmallLabel
import com.snaptensio.bplog.ui.theme.Brand
import java.util.Locale
import kotlin.math.roundToInt

fun hhmm(h: Int, m: Int) = String.format(Locale.US, "%02d:%02d", h, m)

@Composable
fun MedsScreen(vm: MainViewModel, onSaved: () -> Unit) {
    val context = LocalContext.current
    val meds by vm.medications.collectAsStateWithLifecycle()
    val logs by vm.doseLogs.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Medication?>(null) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var pendingEnable by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingEnable?.invoke()
        pendingEnable = null
    }

    /** Asks for notification permission (Android 13+) before turning a reminder on. */
    fun withNotifications(action: () -> Unit) {
        if (Notifications.canPost(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) action()
        else {
            pendingEnable = action
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val today = dayKey(System.currentTimeMillis())
    val active = meds.filter { it.active }
    val taken = logs.map { Triple(it.medicationId, it.dateKey, it.time) }.toSet()

    // Adherence over the last 7 days (only doses whose time has passed and after the start date).
    val adherence = remember(active, logs) {
        val now = System.currentTimeMillis()
        var due = 0; var done = 0
        for (d in 0 until 7) {
            val dayStart = startOfDay(now - d * DAY_MS)
            val key = dayKey(dayStart)
            for (m in active) {
                if (startOfDay(m.startDate) > dayStart) continue
                for (t in m.timeList()) {
                    val (h, mi) = t.split(':').map { it.toInt() }
                    if (dayStart + h * 3_600_000L + mi * 60_000L > now) continue
                    due++
                    if (Triple(m.id, key, t) in taken) done++
                }
            }
        }
        if (due == 0) null else Triple((done * 100f / due).roundToInt(), done, due)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Meds & reminders") {
            FilledIconButton(onClick = { editing = Medication(profileId = 0, name = "", dose = "", times = "09:00", startDate = System.currentTimeMillis()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add medication")
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            adherence?.let { (pct, done, due) -> AdherenceCard(pct, done, due) }

            if (active.isNotEmpty()) {
                SmallLabel("Today")
                SectionCard(padding = 6) {
                    active.flatMap { m -> m.timeList().map { m to it } }.sortedBy { it.second }.forEach { (m, t) ->
                        val isTaken = Triple(m.id, today, t) in taken
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (isTaken) Color(0xFFE7F4EC) else MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (isTaken) Icons.Filled.Check else Icons.Filled.Medication,
                                    contentDescription = null,
                                    tint = if (isTaken) Brand.Good else MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${m.name} ${m.dose}".trim(), style = MaterialTheme.typography.titleSmall)
                                Text(t + (m.pillsLeft?.let { " · $it left" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { vm.setDoseTaken(m, today, t, !isTaken) }) { Text(if (isTaken) "Undo" else "Mark taken") }
                        }
                    }
                }
                active.filter { m -> m.pillsLeft != null && m.timeList().isNotEmpty() && m.pillsLeft / m.timeList().size < 7 }.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFDF0D5)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFF7A4D06))
                        Spacer(Modifier.width(10.dp))
                        Text("${m.name} runs out in about ${m.pillsLeft!! / m.timeList().size} days. Time to refill.", color = Color(0xFF4D3204), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SmallLabel("Medications")
            if (meds.isEmpty()) {
                Text("Add the blood pressure medicines you take to get reminders, track doses, and see their effect on your readings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SectionCard(padding = 6) {
                    meds.forEach { m ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${m.name} ${m.dose}".trim(), style = MaterialTheme.typography.titleSmall)
                                Text(if (m.active) m.timeList().joinToString(", ") else "Stopped", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { editing = m }) { Text("Edit") }
                        }
                    }
                }
            }

            NativeAdCard()

            SmallLabel("Measurement reminders")
            SectionCard(padding = 6) {
                reminders.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { editingReminder = r }) { Text(hhmm(r.hour, r.minute), style = MaterialTheme.typography.titleMedium) }
                        Text(r.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(r.enabled, { on ->
                            vm.saveReminder(r.copy(enabled = on))
                            if (on) withNotifications {}
                        })
                    }
                }
                TextButton(onClick = { editingReminder = Reminder(hour = 12, minute = 0, label = "Reading", enabled = true) }, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add reminder")
                }
            }
            Text("Reminders may arrive a few minutes late to save battery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
    }

    editing?.let { m ->
        MedicationDialog(
            initial = m,
            onDismiss = { editing = null },
            onSave = { saved ->
                editing = null
                vm.saveMedication(saved)
                // Reminders still save without permission; they just can't notify until it's granted.
                withNotifications {}
                onSaved()
            },
            onDelete = if (m.id != 0L) ({ vm.deleteMedication(m); editing = null }) else null,
        )
    }
    editingReminder?.let { r ->
        ReminderDialog(
            initial = r,
            onDismiss = { editingReminder = null },
            onSave = { saved ->
                editingReminder = null
                vm.saveReminder(saved)
                withNotifications {}
            },
            onDelete = if (r.id != 0L) ({ vm.deleteReminder(r); editingReminder = null }) else null,
        )
    }
}

@Composable
private fun AdherenceCard(pct: Int, done: Int, due: Int) {
    val sweep by animateFloatAsState(pct / 100f, tween(900), label = "adherence")
    val track = MaterialTheme.colorScheme.surfaceVariant
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(64.dp)) {
                    val st = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(track, 0f, 360f, false, style = st)
                    drawArc(Brand.Good, -90f, 360f * sweep, false, style = st)
                }
                Text("$pct%", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Taken this week", style = MaterialTheme.typography.titleMedium)
                Text("$done of $due doses so far", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicationDialog(initial: Medication, onDismiss: () -> Unit, onSave: (Medication) -> Unit, onDelete: (() -> Unit)?) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var dose by rememberSaveable { mutableStateOf(initial.dose) }
    var times by rememberSaveable { mutableStateOf(initial.timeList()) }
    var pills by rememberSaveable { mutableStateOf(initial.pillsLeft?.toString() ?: "") }
    var active by rememberSaveable { mutableStateOf(initial.active) }
    var picking by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Add medication" else "Edit medication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(dose, { dose = it.take(20) }, label = { Text("Dose (e.g. 5 mg)") }, singleLine = true)
                Text("Times", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    times.sorted().forEach { t ->
                        InputChip(selected = false, onClick = { times = times - t }, label = { Text(t) }, trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $t", modifier = Modifier.size(16.dp)) })
                    }
                    TextButton(onClick = { picking = true }) { Text("+ Time") }
                }
                OutlinedTextField(pills, { pills = it.filter(Char::isDigit).take(4) }, label = { Text("Pills left (optional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (initial.id != 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Still taking it", modifier = Modifier.weight(1f))
                        Switch(active, { active = it })
                    }
                }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete medication", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && times.isNotEmpty(),
                onClick = { onSave(initial.copy(name = name.trim(), dose = dose.trim(), times = times.sorted().joinToString(","), pillsLeft = pills.toIntOrNull(), active = active)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (picking) {
        val state = rememberTimePickerState(9, 0)
        AlertDialog(
            onDismissRequest = { picking = false },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { times = (times + hhmm(state.hour, state.minute)).distinct(); picking = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(initial: Reminder, onDismiss: () -> Unit, onSave: (Reminder) -> Unit, onDelete: (() -> Unit)?) {
    val state = rememberTimePickerState(initial.hour, initial.minute)
    var label by rememberSaveable { mutableStateOf(initial.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Measurement reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TimePicker(state)
                OutlinedTextField(label, { label = it.take(20) }, label = { Text("Label") }, singleLine = true)
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete reminder", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(initial.copy(hour = state.hour, minute = state.minute, label = label.ifBlank { "Reading" }, enabled = true)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
