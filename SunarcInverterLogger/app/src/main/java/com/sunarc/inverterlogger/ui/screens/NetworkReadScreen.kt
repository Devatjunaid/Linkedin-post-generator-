package com.sunarc.inverterlogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.domain.Field
import com.sunarc.inverterlogger.domain.ParseResult
import com.sunarc.inverterlogger.domain.ParsedValue
import com.sunarc.inverterlogger.domain.ReadingSource
import com.sunarc.inverterlogger.domain.SunSpecReading
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.Route
import com.sunarc.inverterlogger.ui.ScanDraft
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.components.NumberField
import com.sunarc.inverterlogger.ui.components.PrimaryButton
import com.sunarc.inverterlogger.ui.components.ScreenHeader
import com.sunarc.inverterlogger.ui.components.SecondaryButton
import com.sunarc.inverterlogger.ui.components.StatTile
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.components.SunChip
import com.sunarc.inverterlogger.ui.components.TextInput
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * Reads live power and the lifetime counter straight from the inverter over the home Wi-Fi
 * (SunSpec Modbus TCP, port 502). No cloud service or account involved.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetworkReadScreen(vm: AppViewModel, nav: Navigator) {
    val inverters by vm.inverters.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf(vm.activeInverter()?.id) }
    val inverter = inverters.firstOrNull { it.id == selectedId } ?: inverters.firstOrNull()
    var host by remember(inverter?.id) { mutableStateOf(inverter?.host ?: "") }
    var port by remember(inverter?.id) { mutableStateOf((inverter?.port ?: 502).toString()) }
    var unit by remember(inverter?.id) { mutableStateOf((inverter?.unitId ?: inverter?.brand?.modbusUnitId ?: 1).toString()) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SunSpecReading?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenList(Modifier.fillMaxSize()) {
        item { ScreenHeader("Read over Wi-Fi", onBack = { nav.pop() }) }
        item {
            SunCard(Modifier.fillMaxWidth(), color = Palette.SkyNight, border = Palette.SkyDeep) {
                Text("Works with inverters that offer SunSpec Modbus TCP on your home network: many SMA, Fronius, SolarEdge, Kostal, Sungrow and FIMER/ABB models.", fontSize = 13.sp, color = Palette.InkSoft)
                Text("Turn on \"Modbus TCP\" in the inverter's web page or installer app and give it a fixed IP address in your router. Your phone must be on the same Wi-Fi.", fontSize = 12.sp, color = Palette.Muted)
            }
        }
        if (inverters.size > 1) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    inverters.forEach { inv -> SunChip(inv.name, inv.id == inverter?.id, { selectedId = inv.id; result = null }) }
                }
            }
        }
        item { TextInput("Inverter IP address", host, { host = it.trim() }, Modifier.fillMaxWidth(), placeholder = "192.168.1.50", keyboard = KeyboardType.Uri) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("Port", port, { port = it }, Modifier.weight(1f), decimal = false)
                NumberField("Unit ID", unit, { unit = it }, Modifier.weight(1f), decimal = false)
            }
        }
        item {
            PrimaryButton(if (busy) "Connecting…" else "Read now", {
                busy = true; error = null; result = null
                scope.launch {
                    vm.readOverNetwork(host, port.toIntOrNull() ?: 502, unit.toIntOrNull() ?: 1)
                        .onSuccess { result = it }
                        .onFailure { error = it.message ?: "Could not reach the inverter" }
                    busy = false
                }
            }, icon = Icons.Rounded.Wifi, enabled = !busy && host.isNotBlank() && inverter != null)
        }
        error?.let { e ->
            item {
                SunCard(Modifier.fillMaxWidth(), color = Palette.SunDeep.copy(alpha = 0.35f), border = Palette.Sun.copy(alpha = 0.5f)) {
                    Text("Could not read the inverter", fontSize = 14.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
                    Text(e, fontSize = 13.sp, color = Palette.InkSoft)
                    Text("Check the IP address, that Modbus TCP is on, and the unit ID (SMA usually 3, most others 1).", fontSize = 12.sp, color = Palette.Muted)
                }
            }
        }
        result?.let { r ->
            item {
                SunCard(Modifier.fillMaxWidth()) {
                    Text(listOfNotNull(r.manufacturer, r.model).joinToString(" ").ifBlank { "Inverter" }, fontSize = 15.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
                    r.serial?.let { Text("Serial $it", fontFamily = MonoFont, fontSize = 11.sp, color = Palette.Muted) }
                    Text("State: ${r.state.label}", fontSize = 12.sp, color = Palette.InkSoft)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val (pv, pu) = Fmt.power(r.powerW)
                    StatTile("Live power", pv, pu, Modifier.weight(1f))
                    val (lv, lu) = Fmt.energy(r.lifetimeKwh)
                    StatTile("Lifetime", lv, lu, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("PV voltage", r.dcVoltage?.let { Fmt.number(it, 0) } ?: "–", "V", Modifier.weight(1f))
                    StatTile("Grid", r.acFrequency?.let { Fmt.number(it, 2) } ?: "–", "Hz", Modifier.weight(1f))
                    StatTile("Temp", r.temperatureC?.let { Fmt.number(it, 0) } ?: "–", "°C", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Remember address", {
                        inverter?.let { vm.updateInverter(it.copy(host = host, port = port.toIntOrNull() ?: 502, unitId = unit.toIntOrNull() ?: it.unitId)) }
                    }, Modifier.weight(1f))
                    PrimaryButton("Use reading", {
                        val inv = inverter ?: return@PrimaryButton
                        val values = buildMap {
                            r.powerW?.let { put(Field.POWER_NOW, ParsedValue(Field.POWER_NOW, it, "Modbus W", 0.99)) }
                            r.lifetimeKwh?.let { put(Field.ENERGY_TOTAL, ParsedValue(Field.ENERGY_TOTAL, it, "Modbus WH", 0.99)) }
                        }
                        vm.draft.value = ScanDraft(inv.id, ReadingSource.NETWORK, ParseResult(values, null, inv.brand, ""))
                        nav.replace(Route.REVIEW)
                    }, Modifier.weight(1f), Icons.Rounded.Check)
                }
            }
        }
    }
}
