package app.layerline.toolkit3d.ui.calib

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.ads.NativeAdSlot
import app.layerline.toolkit3d.data.CalibrationRecord
import app.layerline.toolkit3d.domain.CalcField
import app.layerline.toolkit3d.domain.Calibration
import app.layerline.toolkit3d.domain.Calibrations
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ui.components.ButtonStyle
import app.layerline.toolkit3d.ui.components.CalibrationRuler
import app.layerline.toolkit3d.ui.components.ChamferS
import app.layerline.toolkit3d.ui.components.ConfirmDialog
import app.layerline.toolkit3d.ui.components.IconSquare
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LChip
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.NumberEntryDialog
import app.layerline.toolkit3d.ui.components.Panel
import app.layerline.toolkit3d.ui.components.ScreenHeader
import app.layerline.toolkit3d.ui.components.SectionLabel
import app.layerline.toolkit3d.ui.components.StepperRow
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.max

@Composable
fun CalibrateScreen(ctx: ScreenContext) {
    val vm: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val printers by vm.printers.collectAsStateWithLifecycle()
    val history by vm.calibrations.collectAsStateWithLifecycle()

    var calcId by rememberSaveable { mutableStateOf(Calibrations.all.first().id) }
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val values = remember { mutableStateMapOf<String, Double>() }
    var editing by remember { mutableStateOf<CalcField?>(null) }
    var deleting by remember { mutableStateOf<CalibrationRecord?>(null) }
    val context = LocalContext.current
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    val calc = Calibrations.byId(calcId)
    fun key(f: CalcField) = "${calc.id}.${f.key}"
    val current = calc.fields.associate { it.key to (values[key(it)] ?: it.default) }
    val outcome = calc.compute(current)
    val target = printers.firstOrNull { it.id == targetId }

    ScreenScroll {
        ScreenHeader("; M92 — SET AXIS STEPS", "Calibration", subtitle = target?.let { "For ${it.nickname} · ${it.model}" } ?: "Pick a printer to tag results")

        if (printers.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { LChip("Any printer", targetId == null, { targetId = null }) }
                items(printers, key = { it.id }) { p -> LChip(p.nickname, p.id == targetId, { targetId = p.id }) }
            }
        }

        Spacer(Modifier.height(14.dp))
        Calibrations.all.chunked(4).forEach { rowCalcs ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowCalcs.forEach { c -> CalcTab(c, c.id == calcId, Modifier.weight(1f)) { calcId = c.id } }
            }
            Spacer(Modifier.height(6.dp))
        }

        AnimatedContent(
            targetState = calc.id,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "calc",
        ) { _ ->
            Column {
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Text(calc.hint, style = LLType.mono(11.sp, color = LL.Muted))
                    Spacer(Modifier.height(6.dp))
                    calc.fields.forEach { f ->
                        val v = current.getValue(f.key)
                        StepperRow(
                            label = f.label,
                            value = Fmt.num(v, f.decimals),
                            unit = f.unit,
                            onDec = { values[key(f)] = max(0.0, round(v - f.step)) },
                            onInc = { values[key(f)] = round(v + f.step) },
                            onEdit = { editing = f },
                        )
                    }
                    Text(
                        "RESET TO DEFAULTS",
                        style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Lime),
                        modifier = Modifier.padding(top = 6.dp).pressable({ calc.fields.forEach { values.remove(key(it)) } }).padding(vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(LL.Nav)
                .border(1.dp, LL.Line)
                .padding(16.dp),
        ) {
            Text("RESULT · ${calc.resultLabel}", style = LLType.mono(10.sp, color = LL.Muted))
            if (outcome != null) {
                val shown by animateFloatAsState(outcome.value.toFloat(), tween(500), label = "result")
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                    Text(Fmt.num(shown.toDouble(), calc.decimals), style = LLType.display(52.sp, color = LL.Lime))
                    Text(" ${calc.unit}", style = LLType.mono(15.sp, color = LL.Muted), modifier = Modifier.padding(bottom = 8.dp))
                }
                val diff = if (outcome.previous != 0.0) (outcome.value - outcome.previous) / abs(outcome.previous) * 100 else 0.0
                Text(
                    "was ${Fmt.num(outcome.previous, calc.decimals)} · ${if (diff >= 0) "+" else ""}${Fmt.num(diff, 2)}%" +
                        if (outcome.note.isNotEmpty()) " · ${outcome.note}" else "",
                    style = LLType.mono(12.sp, color = LL.Soft),
                )
                val span = max(abs(outcome.value - outcome.previous) * 1.6, max(abs(outcome.previous) * 0.02, 0.01))
                val delta = ((outcome.value - outcome.previous) / span).toFloat()
                Spacer(Modifier.height(14.dp))
                CalibrationRuler(oldPos = 0.5f - delta * 0.25f, newPos = 0.5f + delta * 0.2f)
                Text(calc.formula, style = LLType.mono(11.sp, color = LL.Muted), modifier = Modifier.padding(top = 8.dp))
            } else {
                Text("Check the inputs — this combination has no valid result.", style = LLType.body(14.sp, color = LL.Hot), modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (outcome != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(LL.Paper, ChamferS)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SEND TO PRINTER", style = LLType.mono(10.sp, color = LL.PaperMuted))
                    Text(outcome.gcode, style = LLType.mono(14.sp, FontWeight.SemiBold, LL.Graphite), modifier = Modifier.padding(top = 4.dp))
                }
                IconSquare(LIcons.Copy, "Copy G-code", { copy(context, outcome.gcode) }, tint = LL.Lime, bg = LL.Graphite)
            }
            Spacer(Modifier.height(10.dp))
            LButton("Save to history", {
                vm.saveCalibration(
                    CalibrationRecord(
                        calcId = calc.id,
                        name = calc.name,
                        target = target?.nickname ?: "",
                        previous = outcome.previous,
                        result = outcome.value,
                        unit = calc.unit,
                        decimals = calc.decimals,
                        gcode = outcome.gcode,
                    ),
                )
                ctx.majorAction()
            }, Modifier.fillMaxWidth(), icon = LIcons.Check, style = ButtonStyle.Dark)
        }

        SectionLabel("; HISTORY")
        if (history.isEmpty()) {
            Text("Saved results show up here so you can roll back a bad change.", style = LLType.body(14.sp, color = LL.Soft))
        }
        history.forEach { h ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(h.name + if (h.target.isNotEmpty()) " · ${h.target}" else "", style = LLType.body(14.sp))
                    Text(
                        "${dateFormat.format(Date(h.createdAt))} · ${Fmt.num(h.previous, h.decimals)} → ${Fmt.num(h.result, h.decimals)} ${h.unit}",
                        style = LLType.mono(11.sp, color = LL.Muted),
                    )
                }
                IconSquare(LIcons.Copy, "Copy G-code", { copy(context, h.gcode) }, bg = LL.Graphite)
                Spacer(Modifier.width(6.dp))
                IconSquare(LIcons.Trash, "Delete entry", { deleting = h }, tint = LL.Hot, bg = LL.Graphite)
            }
        }

        Spacer(Modifier.height(14.dp))
        NativeAdSlot(ctx.app.ads, "calib")
    }

    editing?.let { f ->
        NumberEntryDialog(f.label, current.getValue(f.key), f.unit, { editing = null }) {
            values[key(f)] = it
            editing = null
        }
    }
    deleting?.let { h ->
        ConfirmDialog("Delete entry?", "${h.name} result from history.", "Delete", { deleting = null }) {
            vm.deleteCalibration(h)
            deleting = null
        }
    }
}

private fun round(v: Double) = Math.round(v * 10_000.0) / 10_000.0

@Composable
private fun CalcTab(c: Calibration, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 54.dp)
            .pressable(onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .background(if (selected) LL.Lime else LL.Panel, ChamferS)
            .border(1.dp, if (selected) LL.Lime else LL.Line, ChamferS)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(c.code, style = LLType.mono(9.sp, color = if (selected) LL.Graphite else LL.Muted))
        Text(c.name, style = LLType.display(13.sp, FontWeight.SemiBold, if (selected) LL.Graphite else LL.Ink), maxLines = 1)
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm?.setPrimaryClip(ClipData.newPlainText("G-code", text))
    // Android 13+ shows its own clipboard confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "G-code copied", Toast.LENGTH_SHORT).show()
    }
}
