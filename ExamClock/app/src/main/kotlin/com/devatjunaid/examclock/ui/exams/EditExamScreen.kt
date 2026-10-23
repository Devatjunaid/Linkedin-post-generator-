package com.devatjunaid.examclock.ui.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.theme.onSubjectColor
import com.devatjunaid.examclock.ui.theme.subjectColor
import com.devatjunaid.examclock.ui.theme.subjectColorCount
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun EditExamScreen(examId: Long?, onBack: () -> Unit, onSaved: (isNew: Boolean) -> Unit) {
    val vm = appViewModel(key = "edit-${examId ?: "new"}") { EditExamViewModel(it.repository, examId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(if (examId == null) R.string.action_add_exam else R.string.action_edit_exam),
        onBack = onBack,
    ) { padding ->
        if (!state.loaded) return@ScreenScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { v -> vm.update { it.copy(title = v.take(80)) } },
                label = { Text(stringResource(R.string.field_exam_title)) },
                placeholder = { Text(stringResource(R.string.field_exam_title_hint)) },
                isError = state.showTitleError,
                supportingText = if (state.showTitleError) ({ Text(stringResource(R.string.error_title_required)) }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.group,
                onValueChange = { v -> vm.update { it.copy(group = v.take(60)) } },
                label = { Text(stringResource(R.string.field_exam_group)) },
                placeholder = { Text(stringResource(R.string.field_exam_group_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.field_exam_when), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1.4f).height(52.dp)) {
                    Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(state.date?.let { Format.date(it) } ?: stringResource(R.string.action_pick_date), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(Format.time(context, state.time), modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(stringResource(R.string.field_date_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.date != null) {
                TextButton(onClick = { vm.update { it.copy(date = null) } }) { Text(stringResource(R.string.action_clear_date)) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.durationMinutes,
                    onValueChange = { v -> vm.update { it.copy(durationMinutes = v.filter(Char::isDigit).take(3)) } },
                    label = { Text(stringResource(R.string.field_duration_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.location,
                    onValueChange = { v -> vm.update { it.copy(location = v.take(80)) } },
                    label = { Text(stringResource(R.string.field_location)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1.4f),
                )
            }

            Text(stringResource(R.string.field_color), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(subjectColorCount) { index ->
                    val selected = state.colorIndex == index
                    val label = stringResource(R.string.cd_color_option, index + 1)
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(subjectColor(index))
                            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.update { it.copy(colorIndex = index) } })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = onSubjectColor(index), modifier = Modifier.size(18.dp))
                    }
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { v -> vm.update { it.copy(notes = v.take(1000)) } },
                label = { Text(stringResource(R.string.field_notes)) },
                placeholder = { Text(stringResource(R.string.field_notes_hint)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.save(onSaved) },
                enabled = !state.saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(stringResource(R.string.action_save_exam)) }
        }
    }

    if (pickDate) {
        val initial = state.date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.update { it.copy(date = date) }
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (pickTime) {
        TimePickerDialog(
            initial = state.time,
            onDismiss = { pickTime = false },
            onConfirm = { time ->
                vm.update { it.copy(time = time) }
                pickTime = false
            },
        )
    }
}

/** Material 3 time picker inside a dialog; follows the device's 12/24-hour setting. */
@Composable
fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = pickerState) },
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
