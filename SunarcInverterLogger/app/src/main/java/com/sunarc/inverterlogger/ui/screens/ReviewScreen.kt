package com.sunarc.inverterlogger.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.domain.Field
import com.sunarc.inverterlogger.domain.FaultLibrary
import com.sunarc.inverterlogger.domain.IssueLevel
import com.sunarc.inverterlogger.domain.Reading
import com.sunarc.inverterlogger.domain.ReadingSource
import com.sunarc.inverterlogger.domain.SanityChecker
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.Route
import com.sunarc.inverterlogger.ui.components.ConfidenceBar
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.components.NumberField
import com.sunarc.inverterlogger.ui.components.Pill
import com.sunarc.inverterlogger.ui.components.PrimaryButton
import com.sunarc.inverterlogger.ui.components.ScreenHeader
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.components.SunChip
import com.sunarc.inverterlogger.ui.components.TextInput
import com.sunarc.inverterlogger.ui.components.parseUserNumber
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class When(val label: String) { NOW("Now"), LAST_EVENING("Yesterday 20:00") }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(vm: AppViewModel, nav: Navigator) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val inverters by vm.inverters.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Draft is cleared after saving; the screen is leaving then, so it just draws nothing.
    val d = draft ?: return
    val r = d.result
    val inverter = inverters.firstOrNull { it.id == d.inverterId }

    var today by remember(d.takenAt) { mutableStateOf(r[Field.ENERGY_TODAY]?.value?.let { Fmt.number(it, if (it < 100) 2 else 1) } ?: "") }
    var total by remember(d.takenAt) { mutableStateOf(r[Field.ENERGY_TOTAL]?.value?.let { Fmt.number(it, 1) } ?: "") }
    var powerKw by remember(d.takenAt) { mutableStateOf(r[Field.POWER_NOW]?.value?.let { Fmt.number(it / 1000.0, 2) } ?: "") }
    var fault by remember(d.takenAt) { mutableStateOf(r.faultCode ?: "") }
    var note by remember(d.takenAt) { mutableStateOf("") }
    var whenTaken by remember(d.takenAt) { mutableStateOf(When.NOW) }
    var saving by remember { mutableStateOf(false) }

    val previous by produceState<Reading?>(null, d.inverterId) { value = vm.previousFor(d.inverterId) }
    val zone = ZoneId.systemDefault()
    val timestamp = when (whenTaken) {
        When.NOW -> d.takenAt
        When.LAST_EVENING -> LocalDate.now(zone).minusDays(1).atTime(20, 0).atZone(zone).toInstant()
    }
    val todayV = parseUserNumber(today)
    val totalV = parseUserNumber(total)
    val powerV = parseUserNumber(powerKw)?.times(1000.0)
    val clearSky = remember { vm.clearSkyToday() }
    val sameDay = previous?.let { Duration.between(it.timestamp, timestamp).abs() < Duration.ofHours(20) &&
        it.timestamp.atZone(zone).toLocalDate() == timestamp.atZone(zone).toLocalDate() } ?: false
    val issues = SanityChecker.check(todayV, totalV, powerV, vm.profile.value?.kwp?.takeIf { it > 0 }, previous, clearSky, sameDay)

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenList(Modifier.weight(1f)) {
            item {
                ScreenHeader(
                    title = if (d.source == ReadingSource.MANUAL) "Type in a reading" else "Check the reading",
                    onBack = { nav.pop() },
                    actions = {
                        Text(Instant.now().atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")), fontFamily = MonoFont, fontSize = 12.sp, color = Palette.Muted)
                    }
                )
            }
            if (d.source != ReadingSource.MANUAL && d.source != ReadingSource.NETWORK) {
                item { PhotoSummary(d.photoPath, r.rawText, inverter?.brand?.name, r.detectedBrand?.name, r.values.size) }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueRow("Energy today", "kWh", today, { today = it }, r[Field.ENERGY_TODAY]?.raw, r[Field.ENERGY_TODAY]?.confidence)
                    ValueRow("Lifetime total", "kWh", total, { total = it }, r[Field.ENERGY_TOTAL]?.raw, r[Field.ENERGY_TOTAL]?.confidence)
                    ValueRow("Live power", "kW", powerKw, { powerKw = it }, r[Field.POWER_NOW]?.raw, r[Field.POWER_NOW]?.confidence)
                }
            }
            item {
                TextInput("Fault code (if shown)", fault, { fault = it.take(16) }, Modifier.fillMaxWidth(), placeholder = "e.g. 102 or E033")
            }
            if (fault.isNotBlank()) {
                item {
                    val info = FaultLibrary.decode(fault, r.rawText + " " + fault, inverter?.brandId)
                    SunCard(Modifier.fillMaxWidth(), color = Palette.SunDeep.copy(alpha = 0.45f), border = Palette.Sun.copy(alpha = 0.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Palette.Sun, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(info.title, fontSize = 14.sp, color = Palette.Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Pill(info.severity.label, Palette.Sun, filled = true)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(info.action, fontSize = 13.sp, color = Palette.InkSoft)
                    }
                }
            }
            item {
                val warnings = issues.filter { it.level == IssueLevel.WARNING }
                val ok = issues.filter { it.level == IssueLevel.INFO }
                SunCard(
                    Modifier.fillMaxWidth(),
                    color = if (warnings.isEmpty()) Palette.SkyNight else Palette.SunDeep.copy(alpha = 0.35f),
                    border = if (warnings.isEmpty()) Palette.SkyDeep else Palette.Sun.copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            if (warnings.isEmpty()) Icons.Rounded.Shield else Icons.Rounded.Info, contentDescription = null,
                            tint = if (warnings.isEmpty()) Palette.Sky else Palette.Sun, modifier = Modifier.size(20.dp)
                        )
                        Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (warnings.isEmpty()) "Passes checks" else "Please check", fontSize = 14.sp, color = Palette.Ink, fontWeight = FontWeight.SemiBold)
                            (warnings + ok).forEach { Text(it.message, fontSize = 13.sp, color = Palette.InkSoft) }
                            if (issues.isEmpty()) Text("Values look plausible for your system.", fontSize = 13.sp, color = Palette.InkSoft)
                        }
                    }
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    When.entries.forEach { w -> SunChip(w.label, whenTaken == w, { whenTaken = w }) }
                    if (inverters.size > 1) inverters.forEach { inv ->
                        SunChip(inv.name, inv.id == d.inverterId, { vm.draft.value = d.copy(inverterId = inv.id) })
                    }
                }
            }
            item { TextInput("Note (optional)", note, { note = it.take(200) }, Modifier.fillMaxWidth(), placeholder = "e.g. cloudy morning, panels cleaned") }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (d.source == ReadingSource.PHOTO || d.source == ReadingSource.SCREENSHOT) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Palette.Surface).border(1.dp, Palette.Line, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.IconButton(onClick = { nav.replace(Route.SCAN) }) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = "Retake photo", tint = Palette.Ink)
                    }
                }
            }
            val canSave = !saving && (todayV != null || totalV != null || powerV != null || fault.isNotBlank())
            PrimaryButton("Save to log", {
                saving = true
                scope.launch {
                    vm.saveReading(
                        Reading(
                            inverterId = d.inverterId, timestamp = timestamp, todayKwh = todayV, totalKwh = totalV, powerW = powerV,
                            source = d.source, faultCode = fault.trim().ifBlank { null }?.uppercase(), note = note.trim().ifBlank { null },
                            confidence = r.values.values.map { it.confidence }.takeIf { it.isNotEmpty() }?.average(),
                            photoPath = d.photoPath
                        )
                    )
                    // Saving a reading is a natural break: an interstitial may show here, at most once per 30 s.
                    vm.ads.interstitials.showAfterAction(context.findActivity()) {
                        nav.tab(Route.HISTORY)
                        vm.draft.value = null
                    }
                }
            }, Modifier.weight(1f), Icons.Rounded.Check, enabled = canSave)
        }
    }
}

@Composable
private fun ValueRow(label: String, unit: String, value: String, onChange: (String) -> Unit, raw: String?, confidence: Double?) {
    SunCard(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberField(label, value, onChange, Modifier.weight(1f), suffix = unit)
            if (confidence != null) {
                Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ConfidenceBar(confidence)
                    Text("${(confidence * 100).toInt()}%", fontFamily = MonoFont, fontSize = 11.sp, color = Palette.Muted)
                }
            }
        }
        if (raw != null) Text("Read as “$raw”", fontFamily = MonoFont, fontSize = 11.sp, color = Palette.Faint, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

@Composable
private fun PhotoSummary(path: String?, rawText: String, brand: String?, detected: String?, found: Int) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
        value = path?.let { withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() } }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(width = 132.dp, height = 96.dp).clip(RoundedCornerShape(16.dp)).background(Palette.Lcd)
                .border(2.dp, Palette.Sun, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) Image(bmp.asImageBitmap(), contentDescription = "Scanned photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(rawText.lines().take(4).joinToString("\n"), fontFamily = MonoFont, fontSize = 9.sp, color = Palette.LcdInk, modifier = Modifier.padding(8.dp), maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(detected ?: brand ?: "Inverter", fontSize = 15.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
            Text(if (detected != null) "Brand detected from the screen" else "Using your inverter's label set", fontSize = 12.sp, color = Palette.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("OFFLINE OCR", Palette.Sky)
                Pill("$found FIELD${if (found == 1) "" else "S"}", Palette.SunSoft)
            }
        }
    }
}
