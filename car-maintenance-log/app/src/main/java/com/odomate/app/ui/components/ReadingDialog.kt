package com.odomate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats

/** Manual odometer entry with a sanity check against the last reading. */
@Composable
fun ReadingDialog(lastValue: Int?, unit: String, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    var text by remember { mutableStateOf("") }
    val value = Formats.parseInt(text)
    val lower = value != null && lastValue != null && value < lastValue
    val bigJump = value != null && lastValue != null && value - lastValue > 30_000
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Enter odometer", color = c.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() }.take(8) },
                    label = "Odometer",
                    suffix = unit,
                    keyboardType = KeyboardType.Number,
                    mono = true,
                    isError = lower,
                    supportingText = when {
                        lower -> "Lower than your last reading (${Formats.distance(lastValue!!, unit)}). Save only if the odometer was replaced."
                        bigJump -> "That's ${Formats.distance(value!! - lastValue!!, unit)} more than last time. Double-check the digits."
                        lastValue != null -> "Last reading: ${Formats.distance(lastValue, unit)}"
                        else -> null
                    },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = value != null && value > 0, onClick = { value?.let(onSave); onDismiss() }) {
                Text("Save", color = c.accentText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.muted) } },
    )
}
