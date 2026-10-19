package com.sunarc.inverterlogger.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.BuildConfig
import com.sunarc.inverterlogger.data.Inverter
import com.sunarc.inverterlogger.domain.Countries
import com.sunarc.inverterlogger.domain.InverterBrands
import com.sunarc.inverterlogger.domain.Region
import com.sunarc.inverterlogger.domain.TariffModel
import com.sunarc.inverterlogger.reminders.ReminderScheduler
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.components.NumberField
import com.sunarc.inverterlogger.ui.components.PrimaryButton
import com.sunarc.inverterlogger.ui.components.ScreenHeader
import com.sunarc.inverterlogger.ui.components.SecondaryButton
import com.sunarc.inverterlogger.ui.components.SectionLabel
import com.sunarc.inverterlogger.ui.components.Segmented
import com.sunarc.inverterlogger.ui.components.SettingSwitch
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.components.SunChip
import com.sunarc.inverterlogger.ui.components.TextInput
import com.sunarc.inverterlogger.ui.components.parseUserNumber
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: Navigator) {
    val stored by vm.profile.collectAsStateWithLifecycle()
    val inverters by vm.inverters.collectAsStateWithLifecycle()
    val privacyRequired by vm.ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val p0 = stored ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var region by remember { mutableStateOf(p0.country.region) }
    var country by remember { mutableStateOf(p0.countryCode) }
    var name by remember { mutableStateOf(p0.systemName) }
    var kwp by remember { mutableStateOf(if (p0.kwp > 0) Fmt.number(p0.kwp, 2) else "") }
    var tilt by remember { mutableFloatStateOf(p0.tiltDeg.toFloat()) }
    var azimuth by remember { mutableStateOf(p0.azimuthDeg) }
    var lat by remember { mutableStateOf(p0.latitude?.let { Fmt.number(it, 4) } ?: "") }
    var lon by remember { mutableStateOf(p0.longitude?.let { Fmt.number(it, 4) } ?: "") }
    var tariff by remember { mutableStateOf(p0.effectiveTariffModel) }
    var buy by remember { mutableStateOf(if (p0.buyPrice > 0) Fmt.number(p0.buyPrice, 3) else "") }
    var export by remember { mutableStateOf(if (p0.exportPrice > 0) Fmt.number(p0.exportPrice, 3) else "") }
    var selfUse by remember { mutableFloatStateOf(p0.selfUseShare.toFloat()) }
    var cost by remember { mutableStateOf(p0.systemCost?.let { Fmt.number(it, 0) } ?: "") }
    var co2 by remember { mutableStateOf(p0.effectiveCo2.toString()) }
    var editing by remember { mutableStateOf<Inverter?>(null) }
    var adding by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) context.toast("Reminders need notification permission")
    }
    fun wantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.canNotify(context)) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val backupTarget = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { vm.backup(uri) }.onSuccess { context.toast("Backup saved") }.onFailure { context.toast("Backup failed: ${it.message}") }
        }
    }
    val restoreSource = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> confirmRestore = uri }

    fun share(kind: String) = scope.launch {
        val result = runCatching {
            when (kind) {
                "csv" -> vm.csv() to "text/csv"
                "daily" -> vm.dailyCsv() to "text/csv"
                else -> vm.report() to "application/pdf"
            }
        }.getOrNull() ?: return@launch context.toast("Export failed")
        vm.ads.interstitials.showAfterAction(context.findActivity()) {
            context.share(vm.exports.shareIntent(result.first, result.second, "Share"))
        }
    }

    fun save() {
        vm.updateProfile { old ->
            old.copy(
                countryCode = country,
                systemName = name.ifBlank { old.systemName },
                kwp = parseUserNumber(kwp)?.coerceIn(0.0, 999.0) ?: old.kwp,
                tiltDeg = tilt.toDouble(),
                azimuthDeg = azimuth,
                latitude = parseUserNumber(lat)?.coerceIn(-90.0, 90.0),
                longitude = parseUserNumber(lon)?.coerceIn(-180.0, 180.0),
                tariffModel = tariff,
                buyPrice = parseUserNumber(buy) ?: 0.0,
                exportPrice = parseUserNumber(export) ?: 0.0,
                selfUseShare = selfUse.toDouble(),
                systemCost = parseUserNumber(cost),
                co2Grams = co2.toIntOrNull()?.coerceIn(0, 2000)
            )
        }
        context.toast("Saved")
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader("My system", onBack = { nav.pop() })

            SectionLabel("System")
            TextInput("Name", name, { name = it }, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("Size", kwp, { kwp = it }, Modifier.weight(1f), suffix = "kWp")
                NumberField("Tilt", tilt.toInt().toString(), { s -> s.toIntOrNull()?.let { tilt = it.coerceIn(0, 90).toFloat() } }, Modifier.weight(1f), suffix = "°", decimal = false)
            }
            Text("Panels face", fontSize = 13.sp, color = Palette.InkSoft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Directions.forEach { (label, deg) -> SunChip(label, azimuth == deg, { azimuth = deg }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("Latitude", lat, { lat = it }, Modifier.weight(1f), signed = true)
                NumberField("Longitude", lon, { lon = it }, Modifier.weight(1f), signed = true)
            }
            Text("Optional. Leave empty to use the centre of ${Countries.byCode(country).name}; your exact location makes sunrise, sunset and expected output more accurate. It never leaves the phone.", fontSize = 12.sp, color = Palette.Muted)

            SectionLabel("Country")
            Segmented(Region.entries, region, { it.label }, { region = it })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Countries.inRegion(region).forEach { c ->
                    SunChip(c.name, c.code == country, {
                        country = c.code
                        tariff = c.tariffModel
                        co2 = c.gridCo2GramsPerKwh.toString()
                    })
                }
            }

            SectionLabel("Inverters")
            SunCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                inverters.forEachIndexed { i, inv ->
                    if (i > 0) HorizontalDivider(color = Palette.LineSoft)
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(inv.name, fontSize = 14.sp, color = Palette.Ink)
                            Text(
                                listOfNotNull(inv.brand.name, inv.model.ifBlank { null }, inv.host?.let { "network $it" }).joinToString(" · "),
                                fontSize = 12.sp, color = Palette.Muted
                            )
                        }
                        IconButton(onClick = { editing = inv }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit ${inv.name}", tint = Palette.Muted) }
                    }
                }
                TextButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Palette.Sun)
                    Text(" Add inverter or meter", color = Palette.Sun)
                }
            }

            SectionLabel("Money")
            Segmented(TariffModel.entries, tariff, {
                when (it) {
                    TariffModel.NET_METERING -> "Net meter"
                    TariffModel.NET_BILLING -> "Net billing"
                    TariffModel.FEED_IN -> "Feed-in"
                }
            }, { tariff = it })
            Text(tariff.description, fontSize = 12.sp, color = Palette.Muted)
            val cur = Countries.byCode(country).currencySymbol.trim()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("Buy price / kWh", buy, { buy = it }, Modifier.weight(1f), suffix = cur)
                if (tariff != TariffModel.NET_METERING) NumberField("Export / kWh", export, { export = it }, Modifier.weight(1f), suffix = cur)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Used at home", fontSize = 13.sp, color = Palette.InkSoft, modifier = Modifier.weight(1f))
                Text("${(selfUse * 100).toInt()}%", fontFamily = MonoFont, color = Palette.SunSoft)
            }
            Slider(
                selfUse, { selfUse = it }, valueRange = 0f..1f, steps = 19,
                colors = SliderDefaults.colors(thumbColor = Palette.Sun, activeTrackColor = Palette.Sun, inactiveTrackColor = Palette.Line)
            )
            Text("Share of solar energy you use yourself rather than export. Without a battery it is usually 25–40%, with one 50–80%.", fontSize = 12.sp, color = Palette.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("System cost", cost, { cost = it }, Modifier.weight(1f), suffix = cur)
                NumberField("Grid CO₂", co2, { co2 = it }, Modifier.weight(1f), suffix = "g/kWh", decimal = false)
            }

            SectionLabel("Reminders")
            SunCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingSwitch("Remind me to scan at sunset", p0.sunsetReminder, { on ->
                    if (on) wantNotifications()
                    vm.updateProfile { it.copy(sunsetReminder = on) }
                }, "20 minutes before sunset, worked out for your location")
                HorizontalDivider(color = Palette.LineSoft)
                SettingSwitch("Monthly summary", p0.monthlySummary, { on ->
                    if (on) wantNotifications()
                    vm.updateProfile { it.copy(monthlySummary = on) }
                }, "On the 1st of each month")
            }

            SectionLabel("Your data")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Readings CSV", { share("csv") }, Modifier.weight(1f), Icons.Rounded.FileDownload)
                SecondaryButton("Daily CSV", { share("daily") }, Modifier.weight(1f), Icons.Rounded.TableChart)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("PDF report", { share("pdf") }, Modifier.weight(1f), Icons.Rounded.Description)
                SecondaryButton("Backup", { backupTarget.launch("sunarc-backup-${LocalDate.now()}.json") }, Modifier.weight(1f), Icons.Rounded.Backup)
                SecondaryButton("Restore", { restoreSource.launch(arrayOf("application/json", "text/plain", "*/*")) }, Modifier.weight(1f), Icons.Rounded.Restore)
            }
            Text("Everything is stored on this phone. Backups are files you keep wherever you like.", fontSize = 12.sp, color = Palette.Muted)

            SectionLabel("Privacy")
            SunCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                if (privacyRequired) {
                    TextButton(onClick = { context.findActivity()?.let { vm.ads.showPrivacyOptions(it) {} } }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = Palette.Sun)
                        Text(" Ad privacy choices", color = Palette.Ink)
                    }
                    HorizontalDivider(color = Palette.LineSoft)
                }
                TextButton(onClick = { context.openUrl(PRIVACY_POLICY_URL) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Policy, contentDescription = null, tint = Palette.Sun)
                    Text(" Privacy policy", color = Palette.Ink)
                }
            }
            Text(
                "Sunarc shows ads from Google AdMob to stay free. Photos, readings and your location are never sent anywhere. Text recognition is Google ML Kit running on the device. Fonts: Space Grotesk and IBM Plex (SIL Open Font License).",
                fontSize = 12.sp, color = Palette.Muted
            )
            Text("Version ${BuildConfig.VERSION_NAME}", fontFamily = MonoFont, fontSize = 11.sp, color = Palette.Faint)
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton("Save changes", { save() }, Modifier.padding(horizontal = 20.dp, vertical = 10.dp), Icons.Rounded.Check)
    }

    if (adding || editing != null) {
        InverterDialog(
            initial = editing,
            region = region,
            onDismiss = { adding = false; editing = null },
            onSave = { inv ->
                if (inv.id == 0L) vm.addInverter(inv) else vm.updateInverter(inv)
                adding = false; editing = null
            },
            onDelete = if (editing != null && inverters.size > 1) ({ vm.deleteInverter(editing!!.id); editing = null }) else null
        )
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces your current log and settings with the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    scope.launch {
                        runCatching { vm.restore(uri) }
                            .onSuccess { n -> context.toast("Restored $n readings") }
                            .onFailure { context.toast(it.message ?: "Restore failed") }
                    }
                }) { Text("Restore", color = Palette.Sun) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } },
            containerColor = Palette.Surface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InverterDialog(initial: Inverter?, region: Region, onDismiss: () -> Unit, onSave: (Inverter) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var brandId by remember { mutableStateOf(initial?.brandId ?: InverterBrands.forRegion(region).first().id) }
    var model by remember { mutableStateOf(initial?.model ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf((initial?.port ?: 502).toString()) }
    var unit by remember { mutableStateOf((initial?.unitId ?: InverterBrands.byId(brandId).modbusUnitId).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text(if (initial == null) "Add inverter" else "Edit inverter") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput("Name", name, { name = it }, Modifier.fillMaxWidth(), placeholder = "e.g. Garage roof")
                Text("Brand", fontSize = 12.sp, color = Palette.Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InverterBrands.forRegion(region).forEach { b ->
                        SunChip(b.name, b.id == brandId, {
                            brandId = b.id
                            unit = b.modbusUnitId.toString()
                        })
                    }
                }
                TextInput("Model (optional)", model, { model = it }, Modifier.fillMaxWidth())
                Text("Local network (optional, SunSpec Modbus TCP)", fontSize = 12.sp, color = Palette.Muted, fontWeight = FontWeight.Medium)
                TextInput("IP address", host, { host = it.trim() }, Modifier.fillMaxWidth(), placeholder = "192.168.1.50", keyboard = KeyboardType.Uri)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Port", port, { port = it }, Modifier.weight(1f), decimal = false)
                    NumberField("Unit ID", unit, { unit = it }, Modifier.weight(1f), decimal = false)
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete this inverter", color = Palette.Sun) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val brand = InverterBrands.byId(brandId)
                onSave(
                    Inverter(
                        id = initial?.id ?: 0L,
                        name = name.ifBlank { brand.name },
                        brandId = brandId,
                        model = model.trim(),
                        host = host.ifBlank { null },
                        port = port.toIntOrNull()?.coerceIn(1, 65535) ?: 502,
                        unitId = unit.toIntOrNull()?.coerceIn(0, 255) ?: brand.modbusUnitId
                    )
                )
            }) { Text("Save", color = Palette.Sun) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
