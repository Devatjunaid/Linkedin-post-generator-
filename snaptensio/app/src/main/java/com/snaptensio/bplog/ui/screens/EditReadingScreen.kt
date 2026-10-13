package com.snaptensio.bplog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaptensio.bplog.data.AppSettings
import com.snaptensio.bplog.data.Arm
import com.snaptensio.bplog.data.DEFAULT_TAGS
import com.snaptensio.bplog.data.Position
import com.snaptensio.bplog.data.Reading
import com.snaptensio.bplog.data.Source
import com.snaptensio.bplog.data.tagList
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.components.CategoryChip
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.Segmented
import com.snaptensio.bplog.ui.theme.Brand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditReadingScreen(
    vm: MainViewModel,
    readingId: Long,
    initialSys: Int?,
    initialDia: Int?,
    initialPulse: Int?,
    source: String,
    onBack: () -> Unit,
    onSaved: (Int, Int) -> Unit,
    onDeleted: () -> Unit,
    onRescan: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    var loaded by rememberSaveable { mutableStateOf(readingId == 0L) }
    var original by rememberSaveable { mutableStateOf<Long?>(null) }
    var sys by rememberSaveable { mutableStateOf(initialSys?.toString() ?: "") }
    var dia by rememberSaveable { mutableStateOf(initialDia?.toString() ?: "") }
    var pulse by rememberSaveable { mutableStateOf(initialPulse?.toString() ?: "") }
    var time by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var arm by rememberSaveable { mutableStateOf(Arm.LEFT) }
    var position by rememberSaveable { mutableStateOf(Position.SITTING) }
    var tags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var customTags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var note by rememberSaveable { mutableStateOf("") }
    var src by rememberSaveable { mutableStateOf(source) }
    var averaged by rememberSaveable { mutableStateOf(1) }
    var profileId by rememberSaveable { mutableStateOf(0L) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showAddTag by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(readingId) {
        if (readingId != 0L && !loaded) {
            vm.loadReading(readingId)?.let { r ->
                original = r.id
                sys = r.systolic.toString(); dia = r.diastolic.toString(); pulse = r.pulse?.toString() ?: ""
                time = r.timestamp
                arm = runCatching { Arm.valueOf(r.arm) }.getOrDefault(Arm.LEFT)
                position = runCatching { Position.valueOf(r.position) }.getOrDefault(Position.SITTING)
                tags = r.tagList()
                customTags = r.tagList().filter { it !in DEFAULT_TAGS }
                note = r.note; src = r.source; averaged = r.averagedCount; profileId = r.profileId
            }
            loaded = true
        }
    }

    val sysV = sys.toIntOrNull()
    val diaV = dia.toIntOrNull()
    val pulseV = pulse.toIntOrNull()

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(if (readingId == 0L) (if (src == Source.SCAN.name) "Check your reading" else "Add reading") else "Edit reading", onBack = onBack) {
            if (readingId != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete reading") }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (src == Source.SCAN.name && readingId == 0L) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color_GoodTint).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Brand.Good)
                    Spacer(Modifier.width(10.dp))
                    Text("Read from camera. Fix any number the camera got wrong.", style = MaterialTheme.typography.bodyMedium, color = Color_GoodInk, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRescan) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Rescan")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("SYS mmHg", sys, { sys = it }, Modifier.weight(1f))
                NumberField("DIA mmHg", dia, { dia = it }, Modifier.weight(1f))
                NumberField("Pulse bpm", pulse, { pulse = it }, Modifier.weight(1f), last = true)
            }
            if (sysV != null && diaV != null && BpClassifier.validate(sysV, diaV, pulseV) == null) {
                val cat = BpClassifier.classify(sysV, diaV, s.guideline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(cat)
                    Spacer(Modifier.width(8.dp))
                    Text(cat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()).format(Date(time)),
                    onValueChange = {}, readOnly = true, label = { Text("Date") },
                    modifier = Modifier.weight(1.3f),
                    trailingIcon = { TextButton(onClick = { showDate = true }) { Text("Change") } },
                )
                OutlinedTextField(
                    value = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(time)),
                    onValueChange = {}, readOnly = true, label = { Text("Time") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = { TextButton(onClick = { showTime = true }) { Text("Edit") } },
                )
            }
            Text("Arm", style = MaterialTheme.typography.titleSmall)
            Segmented(Arm.entries, arm, { it.label }, { arm = it }, Modifier.fillMaxWidth())
            Text("Position", style = MaterialTheme.typography.titleSmall)
            Segmented(Position.entries, position, { it.label }, { position = it }, Modifier.fillMaxWidth())

            Text("Anything that might affect it?", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                (DEFAULT_TAGS + customTags).distinct().forEach { tag ->
                    val on = tag in tags
                    FilterChip(selected = on, onClick = { tags = if (on) tags - tag else tags + tag }, label = { Text(tag) })
                }
                FilterChip(selected = false, onClick = { showAddTag = true }, label = { Text("Tag") }, leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) })
            }
            OutlinedTextField(
                note, { note = it.take(300) },
                label = { Text("Note") },
                placeholder = { Text("e.g. felt a bit dizzy after waking up") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            if (averaged > 1) Text("This entry is the average of $averaged readings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                val problem = BpClassifier.validate(sysV, diaV, pulseV)
                if (problem != null) {
                    error = problem
                    return@Button
                }
                if (time > System.currentTimeMillis() + 5 * 60_000) {
                    error = "The time can't be in the future"
                    return@Button
                }
                val reading = Reading(
                    id = original ?: 0L,
                    profileId = profileId,
                    timestamp = time,
                    systolic = sysV!!,
                    diastolic = diaV!!,
                    pulse = pulseV,
                    arm = arm.name,
                    position = position.name,
                    tags = tags.joinToString(","),
                    note = note.trim(),
                    source = src,
                    averagedCount = averaged,
                )
                vm.saveReading(reading) { onSaved(sysV, diaV) }
            },
            enabled = loaded,
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text(if (readingId == 0L) "Save reading" else "Save changes") }
    }

    if (showDate) {
        // DatePicker works in UTC; convert so the chosen calendar day is kept in local time.
        val local = Calendar.getInstance().apply { timeInMillis = time }
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val state = rememberDatePickerState(initialSelectedDateMillis = utcMidnight)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = picked }
                        time = Calendar.getInstance().apply {
                            timeInMillis = time
                            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
    if (showTime) {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        val state = rememberTimePickerState(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    time = Calendar.getInstance().apply {
                        timeInMillis = time
                        set(Calendar.HOUR_OF_DAY, state.hour); set(Calendar.MINUTE, state.minute)
                    }.timeInMillis
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }
    if (showAddTag) {
        var text by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTag = false },
            title = { Text("Add a tag") },
            text = { OutlinedTextField(text, { text = it.replace(",", "").take(24) }, singleLine = true, label = { Text("Tag") }) },
            confirmButton = {
                TextButton(onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        customTags = customTags + t
                        tags = tags + t
                    }
                    showAddTag = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddTag = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this reading?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteReading(readingId)
                    confirmDelete = false
                    onDeleted()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private val Color_GoodTint = androidx.compose.ui.graphics.Color(0xFFE7F4EC)
private val Color_GoodInk = androidx.compose.ui.graphics.Color(0xFF14633F)

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, last: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = if (last) ImeAction.Done else ImeAction.Next),
        modifier = modifier,
    )
}
