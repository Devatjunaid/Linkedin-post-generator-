package com.walkies.dogwalk.ui.reminders

import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkies.dogwalk.R
import com.walkies.dogwalk.data.Reminder
import com.walkies.dogwalk.data.ReminderKind

private val DAY_LABELS = listOf(
    R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
    R.string.day_fri, R.string.day_sat, R.string.day_sun,
)
private val DURATIONS = listOf(15, 20, 30, 45, 60)

/** Add (when [existing] is null) or edit a reminder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorDialog(
    existing: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    val context = LocalContext.current
    val defaultName = stringResource(R.string.reminder_kind_walk)
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: ReminderKind.WALK) }
    var name by rememberSaveable { mutableStateOf(existing?.label ?: "") }
    var days by rememberSaveable { mutableIntStateOf(existing?.daysMask ?: Reminder.ALL_DAYS) }
    var duration by rememberSaveable { mutableIntStateOf(existing?.durationMinutes ?: 30) }
    val time = rememberTimePickerState(
        initialHour = existing?.hour ?: 12,
        initialMinute = existing?.minute ?: 0,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    val potty = stringResource(R.string.reminder_kind_potty)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.reminders_add else R.string.reminders_edit)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == ReminderKind.WALK,
                        onClick = { kind = ReminderKind.WALK },
                        label = { Text(stringResource(R.string.reminder_kind_walk)) },
                    )
                    FilterChip(
                        selected = kind == ReminderKind.POTTY,
                        onClick = { kind = ReminderKind.POTTY },
                        label = { Text(potty) },
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.reminder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TimeInput(state = time)
                Text(stringResource(R.string.reminder_days), style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DAY_LABELS.forEachIndexed { i, label ->
                        val bit = 1 shl i
                        FilterChip(
                            selected = days and bit != 0,
                            onClick = { days = days xor bit },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                if (kind == ReminderKind.WALK) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DURATIONS.forEach { d ->
                            FilterChip(
                                selected = duration == d,
                                onClick = { duration = d },
                                label = { Text(stringResource(R.string.reminder_duration, d)) },
                            )
                        }
                    }
                }
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text(stringResource(R.string.reminder_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = days != 0,
                onClick = {
                    val label = name.trim().ifEmpty { if (kind == ReminderKind.WALK) defaultName else potty }
                    onSave(
                        Reminder(
                            id = existing?.id ?: 0,
                            kind = kind,
                            label = label,
                            hour = time.hour,
                            minute = time.minute,
                            daysMask = days,
                            durationMinutes = if (kind == ReminderKind.WALK) duration else null,
                            enabled = existing?.enabled ?: true,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
