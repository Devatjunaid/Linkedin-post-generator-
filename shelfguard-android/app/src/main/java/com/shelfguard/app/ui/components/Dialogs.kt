package com.shelfguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shelfguard.app.ads.NativeAdCard
import com.shelfguard.app.util.Format
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = if (destructive) {
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else androidx.compose.material3.ButtonDefaults.buttonColors(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Read-only field that opens a Material date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "No expiry",
    allowClear: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = date?.let(Format::date) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            trailingIcon = if (allowClear && date != null) {
                {
                    IconButton(onClick = { onDateChange(null) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear date")
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        // Tap target over the text area (leaves the clear button usable).
        Box(
            Modifier
                .matchParentSize()
                .padding(end = if (allowClear && date != null) 52.dp else 0.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Choose date",
                ) { open = true },
        )
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now().plusDays(7))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

/** Parses user input for a whole-number quantity. */
fun String.toQty(): Int? = trim().toIntOrNull()?.takeIf { it >= 0 }

/** Parses user input for a price, accepting "1,250.50". */
fun String.toPrice(): Double? = trim().replace(",", "").toDoubleOrNull()?.takeIf { it >= 0 }

fun Double.asInput(): String = if (this == 0.0) "" else if (this % 1.0 == 0.0) toLong().toString() else toString()

/** Records a sale. */
@Composable
fun SaleDialog(
    productName: String,
    unit: String,
    available: Int,
    defaultPrice: Double,
    currency: String,
    onConfirm: (qty: Int, price: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var qty by rememberSaveable { mutableStateOf("1") }
    var price by rememberSaveable { mutableStateOf(defaultPrice.asInput()) }
    val q = qty.toQty()
    val p = price.toPrice() ?: 0.0
    val error = when {
        q == null || q == 0 -> "Enter a quantity"
        q > available -> "Only $available $unit available"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record sale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$productName · $available $unit available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it.filter(Char::isDigit).take(6) },
                    label = { Text("Quantity ($unit)") },
                    isError = error != null && qty.isNotEmpty(),
                    supportingText = { if (error != null && qty.isNotEmpty()) Text(error) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                    label = { Text("Price per $unit") },
                    prefix = { if (currency.isNotBlank()) Text("$currency ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
                if (q != null && q > 0) {
                    Text(
                        "Total: ${Format.money(q * p, currency)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = error == null, onClick = { onConfirm(q!!, p) }) { Text("Record sale") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Adds stock (a new batch). */
@Composable
fun RestockDialog(
    productName: String,
    unit: String,
    defaultCost: Double,
    currency: String,
    suggestedQty: Int,
    onConfirm: (qty: Int, expiry: LocalDate?, cost: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var qty by rememberSaveable { mutableStateOf(if (suggestedQty > 0) suggestedQty.toString() else "") }
    var cost by rememberSaveable { mutableStateOf(defaultCost.asInput()) }
    var expiry by remember { mutableStateOf<LocalDate?>(null) }
    val q = qty.toQty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(productName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it.filter(Char::isDigit).take(6) },
                    label = { Text("Quantity received ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
                DateField(label = "Expiry date", date = expiry, onDateChange = { expiry = it })
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                    label = { Text("Cost per $unit") },
                    prefix = { if (currency.isNotBlank()) Text("$currency ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
            }
        },
        confirmButton = {
            Button(enabled = q != null && q > 0, onClick = { onConfirm(q!!, expiry, cost.toPrice()) }) { Text("Add stock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Corrects a batch's quantity and expiry after a stock count. */
@Composable
fun EditBatchDialog(
    unit: String,
    quantity: Int,
    expiry: LocalDate?,
    onConfirm: (qty: Int, expiry: LocalDate?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var qty by rememberSaveable { mutableStateOf(quantity.toString()) }
    var date by remember { mutableStateOf(expiry) }
    val q = qty.toQty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit batch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it.filter(Char::isDigit).take(6) },
                    label = { Text("Quantity ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
                DateField(label = "Expiry date", date = date, onDateChange = { date = it })
                TextButton(
                    onClick = { onDelete(); onDismiss() },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove this batch") }
            }
        },
        confirmButton = {
            Button(enabled = q != null, onClick = { onConfirm(q!!, date); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * The exit confirmation: branded, calm, with an optional small native ad in the
 * spare space between the message and the buttons.
 */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit, alertsEnabled: Boolean) {
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ShelfGuardLogo(Modifier.size(72.dp))
                Spacer(Modifier.height(16.dp))
                Text("Leaving ShelfGuard?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (alertsEnabled) "Your stock is saved. Daily expiry and low-stock alerts will keep running."
                    else "Your stock is saved on this device. Turn on daily alerts in Settings so nothing expires unnoticed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                NativeAdCard(Modifier.padding(top = 18.dp))
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onStay,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("Stay") }
                    Button(
                        onClick = onExit,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("Exit") }
                }
            }
        }
    }
}

/** Small coloured pill used for selectable options such as "3 days". */
@Composable
fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerLowest
    val fg = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                1.dp,
                if (selected) bg else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}
