package com.harrycodess.fueltracker.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.ui.components.DatePickerChip
import java.time.LocalDate

/** Confirms the save and lets the user add a short note and pick the trip date. */
@Composable
fun SaveTripDialog(
    state: CalculatorUiState,
    onDismiss: () -> Unit,
    onConfirm: (note: String, date: LocalDate) -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }
    var epochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val estimate = state.estimate
    val distanceKm = state.distanceKm
    val units = state.units

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save trip") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (estimate != null && distanceKm != null) {
                    Text(
                        buildString {
                            append(Fmt.money(estimate.totalCost, state.settings.currency))
                            append(" · ")
                            append(Fmt.number(FuelMath.kmToDistance(distanceKm, units.system)))
                            append(" ${units.distanceUnit}")
                            state.selectedVehicle?.let { append(" · ${it.name}") }
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Commute, Road trip") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerChip(
                    date = LocalDate.ofEpochDay(epochDay),
                    onDateChange = { epochDay = it.toEpochDay() },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note, LocalDate.ofEpochDay(epochDay)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
