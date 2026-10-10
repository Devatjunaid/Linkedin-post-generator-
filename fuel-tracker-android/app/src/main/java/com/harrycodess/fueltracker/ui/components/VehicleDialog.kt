package com.harrycodess.fueltracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.harrycodess.fueltracker.data.local.Vehicle
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.domain.parseDecimal

/**
 * Add / edit vehicle dialog. Efficiency is entered in the user's units and
 * returned to [onConfirm] in km/L.
 */
@Composable
fun VehicleDialog(
    units: UnitPrefs,
    onDismiss: () -> Unit,
    onConfirm: (name: String, efficiencyKmPerL: Double) -> Unit,
    vehicle: Vehicle? = null,
) {
    var name by rememberSaveable { mutableStateOf(vehicle?.name.orEmpty()) }
    var efficiency by rememberSaveable {
        mutableStateOf(vehicle?.let { Fmt.input(FuelMath.kmPerLToEfficiency(it.efficiencyKmPerL, units)) }.orEmpty())
    }
    val kmPerL = parseDecimal(efficiency)?.let { FuelMath.efficiencyToKmPerL(it, units) }
    val valid = name.isNotBlank() && kmPerL != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vehicle == null) "Add vehicle" else "Edit vehicle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Civic") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                DecimalField(
                    value = efficiency,
                    onValueChange = { efficiency = it },
                    label = "Typical efficiency",
                    icon = Icons.Rounded.Speed,
                    unit = units.efficiencyUnit,
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (kmPerL != null) onConfirm(name.trim(), kmPerL)
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
