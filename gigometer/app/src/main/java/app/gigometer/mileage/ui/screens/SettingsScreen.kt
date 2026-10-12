package app.gigometer.mileage.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.BuildConfig
import app.gigometer.mileage.Format
import app.gigometer.mileage.R
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.DEFAULT_PLATFORMS
import app.gigometer.mileage.data.RatePeriod
import app.gigometer.mileage.ui.components.AppCard
import app.gigometer.mileage.ui.components.NavRow
import app.gigometer.mileage.ui.components.ScreenTitle
import app.gigometer.mileage.ui.components.SectionLabel
import app.gigometer.mileage.ui.components.ToggleRow
import app.gigometer.mileage.ui.theme.LocalPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SettingsActions(
    val setAutoDetect: (Boolean) -> Unit,
    val setBatterySaver: (Boolean) -> Unit,
    val setReminder: (Boolean) -> Unit,
    val setLookup: (Boolean) -> Unit,
    val setWorkAuto: (Boolean) -> Unit,
    val setWorkHours: (Int, Int) -> Unit,
    val setPlatforms: (List<String>) -> Unit,
    val setName: (String) -> Unit,
    val addRate: (LocalDate, Double) -> Unit,
    val removeRate: (RatePeriod) -> Unit,
    val resetRates: () -> Unit,
    val backup: (Uri) -> Unit,
    val restore: (Uri) -> Unit,
    val setupPermissions: () -> Unit,
    val privacyOptions: (() -> Unit)?,
)

private enum class Dialog { NONE, HOURS, RATES, PLATFORMS, NAME }

@Composable
fun SettingsScreen(settings: AppSettings, autoReady: Boolean, a: SettingsActions) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var dialog by remember { mutableStateOf(Dialog.NONE) }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(a.backup) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(a.restore) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Settings")

        SectionLabel("Tracking")
        Group {
            ToggleRow(
                "Auto-detect drives",
                if (settings.autoDetect && !autoReady) "Needs permissions — tap to finish setup" else "Starts recording when you drive, stops after you park",
                settings.autoDetect && autoReady,
                { on -> a.setAutoDetect(on); if (on && !autoReady) a.setupPermissions() },
            )
            ToggleRow("Battery saver", "Lower GPS rate when speed is steady", settings.batterySaver, a.setBatterySaver)
            ToggleRow("Daily review reminder", "9 PM nudge to classify new trips", settings.reviewReminder, a.setReminder)
            ToggleRow("Work hours are Business", "Auto-detected trips inside work hours are saved as Business", settings.workHoursAutoBusiness, a.setWorkAuto)
            NavRow("Work hours", "Trips outside these hours wait for your review", "${Format.minuteOfDay(settings.workStart)}–${Format.minuteOfDay(settings.workEnd)}", divider = false) { dialog = Dialog.HOURS }
        }

        SectionLabel("Tax")
        Group {
            val today = settings.rates.lastOrNull { !it.start.isAfter(LocalDate.now()) }
            NavRow("IRS rate per mile", "Rates by date — updated when the IRS changes them", today?.let { Format.rate(it.dollarsPerMile) }) { dialog = Dialog.RATES }
            NavRow("My gig apps", settings.platforms.joinToString(", ").ifBlank { "None selected" }) { dialog = Dialog.PLATFORMS }
            NavRow("Name on reports", "Printed on your PDF mileage log", settings.driverName.ifBlank { "Add" }) { dialog = Dialog.NAME }
            ToggleRow("Street addresses", "Show start/end addresses using your phone's geocoder", settings.lookupAddresses, a.setLookup, divider = false)
        }

        SectionLabel("Your data")
        AppCard {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(p.accentSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Lock, null, tint = p.accent, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your trips stay on your phone. No account, no sign-up, and we never sell or upload your location. Free forever — supported by ads.",
                        color = p.ink, fontSize = 13.sp, lineHeight = 19.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { backupLauncher.launch("gigometer-backup-${LocalDate.now()}.json") }, shape = RoundedCornerShape(12.dp)) {
                            Text("Back up file", color = p.ink, fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, shape = RoundedCornerShape(12.dp)) {
                            Text("Restore", color = p.ink, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        SectionLabel("About")
        Group {
            if (a.privacyOptions != null) NavRow("Ad privacy choices", "Change your ad consent") { a.privacyOptions.invoke() }
            NavRow("Privacy policy", null) {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ctx.getString(R.string.privacy_policy_url))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            NavRow("Gigometer", "Version ${BuildConfig.VERSION_NAME} · not tax advice", divider = false) {}
        }
        Spacer(Modifier.height(8.dp))
    }

    when (dialog) {
        Dialog.HOURS -> WorkHoursDialog(settings, onDismiss = { dialog = Dialog.NONE }) { s, e -> a.setWorkHours(s, e); dialog = Dialog.NONE }
        Dialog.RATES -> RatesDialog(settings.rates, a, onDismiss = { dialog = Dialog.NONE })
        Dialog.PLATFORMS -> PlatformsDialog(settings.platforms, onDismiss = { dialog = Dialog.NONE }) { a.setPlatforms(it); dialog = Dialog.NONE }
        Dialog.NAME -> NameDialog(settings.driverName, onDismiss = { dialog = Dialog.NONE }) { a.setName(it); dialog = Dialog.NONE }
        Dialog.NONE -> Unit
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.card)) { content() }
}

@Composable
private fun TimeDropdown(label: String, minute: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.muted)
        Box {
            OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(12.dp)) { Text(Format.minuteOfDay(minute), fontWeight = FontWeight.ExtraBold) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                (0 until 48).map { it * 30 }.forEach { m ->
                    DropdownMenuItem(text = { Text(Format.minuteOfDay(m)) }, onClick = { onPick(m); open = false })
                }
            }
        }
    }
}

@Composable
private fun WorkHoursDialog(settings: AppSettings, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var start by remember { mutableStateOf(settings.workStart) }
    var end by remember { mutableStateOf(settings.workEnd) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Work hours") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Overnight shifts work too (e.g. 6p – 2a).", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeDropdown("Start", start) { start = it }
                    TimeDropdown("End", end) { end = it }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(start, end) }, enabled = start != end) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatesDialog(rates: List<RatePeriod>, a: SettingsActions, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    var rateText by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    val rate = rateText.replace(",", ".").toDoubleOrNull()?.let { if (it > 5) it / 100 else it } // accept "76" (cents) or "0.76"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("IRS business mileage rates") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rates.sortedByDescending { it.start }.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("From ${Format.date(r.start)}", modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text(Format.rate(r.dollarsPerMile), fontWeight = FontWeight.ExtraBold, color = p.accent)
                        IconButton(onClick = { a.removeRate(r) }, enabled = rates.size > 1) { Icon(Icons.Filled.Close, contentDescription = "Remove rate") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Add a new rate", fontWeight = FontWeight.ExtraBold)
                OutlinedButton(onClick = { picking = true }, shape = RoundedCornerShape(12.dp)) { Text("Starts ${Format.date(date)}") }
                OutlinedTextField(
                    rateText, { rateText = it.take(6) }, label = { Text("Dollars per mile, e.g. 0.76") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    "Reset to IRS defaults", color = p.accent, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { a.resetRates() }.padding(vertical = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { rate?.let { a.addRate(date, it) }; rateText = "" }, enabled = rate != null && rate > 0) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    picking = false
                }) { Text("OK") }
            },
        ) { DatePicker(state) }
    }
}

@Composable
private fun PlatformsDialog(current: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(current) } }
    val options = remember { mutableStateListOf<String>().apply { addAll((DEFAULT_PLATFORMS + current).distinct()) } }
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My gig apps") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().clickable { if (o in selected) selected.remove(o) else selected.add(o) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = o in selected, onCheckedChange = { if (it) selected.add(o) else selected.remove(o) })
                        Text(o)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(custom, { custom = it.take(24) }, label = { Text("Other app") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val c = custom.trim()
                        if (c.isNotEmpty()) { if (c !in options) options.add(c); if (c !in selected) selected.add(c); custom = "" }
                    }) { Text("Add") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(options.filter { it in selected }) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name on reports") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.name_hint), fontSize = 13.sp)
                OutlinedTextField(name, { name = it.take(60) }, singleLine = true, label = { Text("Your name") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
