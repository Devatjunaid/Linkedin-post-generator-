package com.devatjunaid.renewalert.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devatjunaid.renewalert.ads.AdPlacement
import com.devatjunaid.renewalert.data.CycleUnit
import com.devatjunaid.renewalert.data.SubStatus
import com.devatjunaid.renewalert.data.Subscription
import com.devatjunaid.renewalert.domain.Billing
import com.devatjunaid.renewalert.domain.Catalog
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.domain.Template
import com.devatjunaid.renewalert.ui.AppViewModel
import com.devatjunaid.renewalert.ui.components.ServiceTile
import com.devatjunaid.renewalert.ui.theme.RenewAlertTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class CycleChoice(val label: String, val unit: CycleUnit, val count: Int) {
    WEEKLY("Weekly", CycleUnit.WEEK, 1),
    MONTHLY("Monthly", CycleUnit.MONTH, 1),
    QUARTERLY("3 months", CycleUnit.MONTH, 3),
    YEARLY("Yearly", CycleUnit.YEAR, 1),
    CUSTOM("Custom", CycleUnit.MONTH, 1),
}

private val reminderOptions = listOf(-1 to "No reminder", 0 to "On the day", 1 to "1 day before", 2 to "2 days before", 3 to "3 days before", 7 to "1 week before")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    vm: AppViewModel,
    id: Long?,
    onClose: () -> Unit,
    afterAction: (AdPlacement, () -> Unit) -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    val c = RenewAlertTheme.colors
    val today = LocalDate.now()

    var loadedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var original by remember { mutableStateOf<Subscription?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(s.currency) }
    var cycle by rememberSaveable { mutableStateOf(CycleChoice.MONTHLY) }
    var customCount by rememberSaveable { mutableStateOf("1") }
    var customUnit by rememberSaveable { mutableStateOf(CycleUnit.MONTH) }
    var firstDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var trial by rememberSaveable { mutableStateOf(false) }
    var trialEnd by rememberSaveable { mutableStateOf(today.plusDays(7).toEpochDay()) }
    var reminder by rememberSaveable { mutableStateOf(s.defaultReminderDays) }
    var category by rememberSaveable { mutableStateOf("Other") }
    var payment by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf<String?>(null) }
    var templatesOpen by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null && loadedId != id) {
            val sub = vm.get(id) ?: return@LaunchedEffect
            original = sub
            name = sub.name
            price = Formats.minorToInput(sub.priceMinor)
            currency = sub.currency
            cycle = CycleChoice.entries.firstOrNull { it != CycleChoice.CUSTOM && it.unit == sub.cycleUnit && it.count == sub.cycleCount } ?: CycleChoice.CUSTOM
            customCount = sub.cycleCount.toString()
            customUnit = sub.cycleUnit
            firstDay = sub.firstPaymentEpochDay
            trial = sub.status == SubStatus.TRIAL
            trialEnd = sub.trialEndEpochDay ?: sub.firstPaymentEpochDay
            reminder = sub.reminderDaysBefore
            category = sub.category
            payment = sub.paymentMethod
            notes = sub.notes
            loadedId = id
        } else if (id != null && original == null) {
            original = vm.get(id)
        }
    }

    fun applyTemplate(t: Template) {
        name = t.name
        category = t.category
        cycle = if (t.cycle == CycleUnit.YEAR) CycleChoice.YEARLY else CycleChoice.MONTHLY
    }

    val priceMinor = Formats.parseMinor(price)
    val unit = if (cycle == CycleChoice.CUSTOM) customUnit else cycle.unit
    val count = if (cycle == CycleChoice.CUSTOM) (customCount.toIntOrNull() ?: 0) else cycle.count
    val nameError = name.isBlank()
    val priceError = priceMinor == null
    val countError = count !in 1..365
    val valid = !nameError && !priceError && !countError

    fun save() {
        if (!valid) { showErrors = true; return }
        val base = original
        val status = when {
            trial -> SubStatus.TRIAL
            base != null && base.status == SubStatus.TRIAL -> SubStatus.ACTIVE
            else -> base?.status ?: SubStatus.ACTIVE
        }
        val sub = Subscription(
            id = base?.id ?: 0,
            name = name.trim(),
            priceMinor = priceMinor!!,
            currency = currency,
            cycleUnit = unit,
            cycleCount = count,
            firstPaymentEpochDay = if (trial) trialEnd else firstDay,
            category = category,
            paymentMethod = payment.trim(),
            notes = notes.trim(),
            status = status,
            trialEndEpochDay = if (trial) trialEnd else null,
            reminderDaysBefore = reminder,
            usage = base?.usage ?: com.devatjunaid.renewalert.data.Usage.UNKNOWN,
            createdAtMillis = base?.createdAtMillis ?: System.currentTimeMillis(),
            statusChangedEpochDay = base?.statusChangedEpochDay,
        )
        vm.save(sub) { afterAction(AdPlacement.SUBSCRIPTION_SAVED, onClose) }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, unfocusedBorderColor = c.outline,
    )

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            Text(if (id == null) "New subscription" else "Edit subscription", style = MaterialTheme.typography.headlineSmall, color = c.onSurface)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (id == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick add · ${Catalog.templates.size} templates", style = MaterialTheme.typography.labelLarge, color = c.muted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Catalog.templates.take(12)) { t -> TemplateChip(t) { applyTemplate(t) } }
                        item {
                            Column(
                                Modifier.width(76.dp).clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, c.muted.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .clickable { templatesOpen = true }.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = c.onSurface)
                                Text("More", style = MaterialTheme.typography.labelSmall, color = c.onSurface)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it.take(60) },
                label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = showErrors && nameError,
                supportingText = if (showErrors && nameError) { { Text("Give it a name") } } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(14.dp), colors = fieldColors,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = { v -> price = v.filter { it.isDigit() || it == '.' || it == ',' }.take(12) },
                    label = { Text(if (trial) "Price after trial" else "Price") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                    isError = showErrors && priceError,
                    supportingText = if (showErrors && priceError) { { Text("Enter an amount") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors,
                )
                CurrencyPicker(currency, Modifier.width(120.dp)) { currency = it }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Billing cycle", style = MaterialTheme.typography.labelLarge, color = c.muted)
                val choices = CycleChoice.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    choices.forEachIndexed { i, ch ->
                        SegmentedButton(
                            selected = cycle == ch, onClick = { cycle = ch },
                            shape = SegmentedButtonDefaults.itemShape(i, choices.size), icon = {},
                        ) { Text(ch.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                if (cycle == CycleChoice.CUSTOM) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Every", color = c.onSurface)
                        OutlinedTextField(
                            value = customCount, onValueChange = { v -> customCount = v.filter(Char::isDigit).take(3) },
                            singleLine = true, modifier = Modifier.width(84.dp), isError = countError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp), colors = fieldColors,
                        )
                        UnitPicker(customUnit) { customUnit = it }
                    }
                }
                if (priceMinor != null && !countError) {
                    val probe = Subscription(name = "", priceMinor = priceMinor, currency = currency, cycleUnit = unit, cycleCount = count, firstPaymentEpochDay = 0)
                    Text(
                        "≈ ${Formats.money(Billing.monthlyMinor(probe), currency)} a month · ${Formats.money(Billing.yearlyMinor(probe), currency)} a year",
                        style = MaterialTheme.typography.bodySmall, color = c.muted,
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface)
                    .border(1.dp, c.outline, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { trial = !trial }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Free trial", style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                        Text("We'll warn you before the first charge", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    Switch(checked = trial, onCheckedChange = { trial = it })
                }
                if (trial) {
                    DateRow("Trial ends · first charge", trialEnd) { datePicker = "trial" }
                } else {
                    DateRow(if (id == null) "First payment" else "Billing started", firstDay) { datePicker = "first" }
                }
                ReminderRow(reminder) { reminder = it }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = MaterialTheme.typography.labelLarge, color = c.muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cats = (Catalog.categories + listOf(category)).distinct()
                    cats.forEach { cat ->
                        FilterChip(
                            selected = category == cat, onClick = { category = cat }, label = { Text(cat) },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.onSurface, selectedLabelColor = c.background, containerColor = c.surface,
                            ),
                        )
                    }
                    FilterChip(selected = false, onClick = { newCategory = true }, label = { Text("+ New") }, shape = RoundedCornerShape(999.dp))
                }
            }

            OutlinedTextField(
                value = payment, onValueChange = { payment = it.take(40) },
                label = { Text("Paid with (optional label)") },
                placeholder = { Text("e.g. Visa •• 4821, PayPal, Google Play") },
                supportingText = { Text("Just a label. Never enter a full card number.") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), colors = fieldColors,
            )

            OutlinedTextField(
                value = notes, onValueChange = { notes = it.take(500) },
                label = { Text("Notes · how to cancel") },
                placeholder = { Text("Where to cancel, account email, shared with family…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                shape = RoundedCornerShape(14.dp), colors = fieldColors,
            )
            Spacer(Modifier.height(4.dp))
        }

        Box(Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding()) {
            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent),
            ) { Text(if (id == null) "Save subscription" else "Save changes", style = MaterialTheme.typography.titleMedium) }
        }
    }

    datePicker?.let { which ->
        val initial = if (which == "trial") trialEnd else firstDay
        val state = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(initial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { datePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val day = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        if (which == "trial") trialEnd = day else firstDay = day
                    }
                    datePicker = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePicker = null }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (templatesOpen) TemplatesDialog(onDismiss = { templatesOpen = false }) { applyTemplate(it); templatesOpen = false }

    if (newCategory) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newCategory = false },
            title = { Text("New category") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it.take(24) }, singleLine = true, placeholder = { Text("e.g. Pets") }) },
            confirmButton = {
                TextButton(onClick = { if (text.isNotBlank()) category = text.trim(); newCategory = false }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { newCategory = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TemplateChip(t: Template, onClick: () -> Unit) {
    val c = RenewAlertTheme.colors
    Column(
        Modifier.width(76.dp).clip(RoundedCornerShape(14.dp)).background(c.surface)
            .border(1.dp, c.outline, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ServiceTile(t.name, t.category, 32.dp)
        Text(t.name, style = MaterialTheme.typography.labelSmall, color = c.onSurface, maxLines = 1)
    }
}

@Composable
private fun DateRow(label: String, day: Long, onClick: () -> Unit) {
    val c = RenewAlertTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
        Text(Formats.fullDate(LocalDate.ofEpochDay(day)), style = MaterialTheme.typography.labelLarge, color = c.accent)
    }
}

@Composable
private fun ReminderRow(value: Int, onPick: (Int) -> Unit) {
    val c = RenewAlertTheme.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Remind me", style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
            Text(reminderOptions.firstOrNull { it.first == value }?.second ?: "$value days before", style = MaterialTheme.typography.labelLarge, color = c.accent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            reminderOptions.forEach { (v, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { onPick(v); open = false }) }
        }
    }
}

@Composable
private fun CurrencyPicker(value: String, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
            Text("$value ${Formats.symbol(value)}", maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf(value) + Catalog.currencies.filter { it != value }).forEach { code ->
                DropdownMenuItem(text = { Text("$code  ${Formats.symbol(code)}") }, onClick = { onPick(code); open = false })
            }
        }
    }
}

@Composable
private fun UnitPicker(value: CycleUnit, onPick: (CycleUnit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = { u: CycleUnit -> when (u) { CycleUnit.DAY -> "days"; CycleUnit.WEEK -> "weeks"; CycleUnit.MONTH -> "months"; CycleUnit.YEAR -> "years" } }
    Box {
        OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(12.dp)) { Text(label(value)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CycleUnit.entries.forEach { u -> DropdownMenuItem(text = { Text(label(u)) }, onClick = { onPick(u); open = false }) }
        }
    }
}

@Composable
private fun TemplatesDialog(onDismiss: () -> Unit, onPick: (Template) -> Unit) {
    val c = RenewAlertTheme.colors
    var q by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a template") },
        text = {
            Column {
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search") }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(Catalog.search(q)) { t ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(t) }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ServiceTile(t.name, t.category, 34.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(t.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                                Text(t.category, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
