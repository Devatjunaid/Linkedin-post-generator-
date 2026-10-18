package app.layerline.toolkit3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.layerline.toolkit3d.data.JobStatus
import app.layerline.toolkit3d.data.MachineKind
import app.layerline.toolkit3d.data.MachinePreset
import app.layerline.toolkit3d.data.Presets
import app.layerline.toolkit3d.data.PrintJob
import app.layerline.toolkit3d.data.Printer
import app.layerline.toolkit3d.data.Spool
import app.layerline.toolkit3d.data.Workshop
import app.layerline.toolkit3d.domain.CostCalculator
import app.layerline.toolkit3d.domain.CostInput
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.ui.components.ButtonStyle
import app.layerline.toolkit3d.ui.components.KeyValueRow
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LChip
import app.layerline.toolkit3d.ui.components.LDialog
import app.layerline.toolkit3d.ui.components.LTextField
import app.layerline.toolkit3d.ui.components.NumberField
import app.layerline.toolkit3d.ui.components.PickerDialog
import app.layerline.toolkit3d.ui.components.SectionLabel
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType

@Composable
fun ColorDot(argb: Long, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(Color(argb), CircleShape)
            .border(1.dp, LL.LineStrong, CircleShape),
    )
}

private fun String.num(): Double? = toDoubleOrNull()

/** Production cost of a logged print: filament + electricity + machine wear (no labour or margin). */
fun estimateJobCost(grams: Double, minutes: Double, share: Double, printer: Printer?, spool: Spool?, material: String, w: Workshop): Double {
    val perKg = spool?.pricePerKg?.takeIf { it > 0 } ?: (w.filamentPerKg * Presets.material(material).priceFactor)
    return CostCalculator.calculate(
        CostInput(
            grams = grams * share,
            hours = minutes / 60.0 * share,
            filamentPricePerKg = perKg,
            watts = printer?.watts ?: 100.0,
            kwhRate = w.kwhRate,
            machinePrice = printer?.price ?: 0.0,
            machineLifeHours = printer?.lifeHours ?: 0.0,
        ),
    ).productionCost
}

/** Create or edit a print-log entry. Finished/failed jobs take filament off the chosen spool. */
@Composable
fun JobEditorDialog(
    initial: PrintJob?,
    printers: List<Printer>,
    spools: List<Spool>,
    workshop: Workshop,
    onDismiss: () -> Unit,
    onSave: (PrintJob) -> Unit,
    onDelete: ((PrintJob) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var printerId by remember { mutableStateOf(initial?.printerId ?: printers.firstOrNull()?.id) }
    var spoolId by remember { mutableStateOf(initial?.spoolId) }
    var material by remember { mutableStateOf(initial?.material ?: "PLA") }
    var grams by remember { mutableStateOf(initial?.grams?.let { Fmt.trim(it, 1) } ?: "") }
    var hours by remember { mutableStateOf(initial?.let { (it.minutes / 60).toInt().toString() } ?: "") }
    var mins by remember { mutableStateOf(initial?.let { (it.minutes % 60).toInt().toString() } ?: "") }
    var status by remember { mutableStateOf(initial?.jobStatus ?: JobStatus.DONE) }
    var reason by remember { mutableStateOf(initial?.failReason ?: "") }
    var failedAt by remember { mutableStateOf(initial?.failedAtPercent?.takeIf { it > 0 }?.let { Fmt.trim(it, 0) } ?: "50") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var pickSpool by remember { mutableStateOf(false) }

    val printer = printers.firstOrNull { it.id == printerId }
    val spool = spools.firstOrNull { it.id == spoolId }
    val g = grams.num() ?: 0.0
    val minutes = (hours.num() ?: 0.0) * 60 + (mins.num() ?: 0.0)
    val share = if (status == JobStatus.FAILED) ((failedAt.num() ?: 0.0) / 100).coerceIn(0.0, 1.0) else 1.0
    val cost = estimateJobCost(g, minutes, share, printer, spool, spool?.material ?: material, workshop)
    val valid = name.isNotBlank() && g > 0 && minutes > 0

    LDialog(
        code = if (initial == null) "; M28 — NEW LOG ENTRY" else "; M29 — EDIT ENTRY",
        title = if (initial == null) "Log a print" else "Edit print",
        onDismiss = onDismiss,
        actions = {
            if (initial != null && onDelete != null) {
                LButton("Delete", { onDelete(initial) }, Modifier.weight(1f), style = ButtonStyle.Ghost)
            } else {
                LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            }
            LButton(
                "Save",
                {
                    val base = initial ?: PrintJob(name = name, grams = g, minutes = minutes)
                    onSave(
                        base.copy(
                            name = name.trim(),
                            printerId = printer?.id,
                            printerLabel = printer?.let { "${it.nickname} · ${it.model}" } ?: "",
                            spoolId = spool?.id,
                            material = spool?.material ?: material,
                            color = spool?.color ?: base.color,
                            grams = g,
                            minutes = minutes,
                            status = status.name,
                            failReason = if (status == JobStatus.FAILED) reason else "",
                            failedAtPercent = if (status == JobStatus.FAILED) share * 100 else 0.0,
                            cost = cost,
                            notes = notes.trim(),
                            startedAt = if (initial == null && status == JobStatus.PRINTING) System.currentTimeMillis() else base.startedAt,
                        ),
                    )
                },
                Modifier.weight(1f),
                enabled = valid,
            )
        },
    ) {
        LTextField("Part name", name, { name = it }, placeholder = "e.g. Gridfinity bin 2×3")

        SectionLabel("; STATUS")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            JobStatus.entries.forEach { s ->
                LChip(
                    when (s) { JobStatus.PRINTING -> "Printing"; JobStatus.DONE -> "Done"; JobStatus.FAILED -> "Failed" },
                    status == s,
                    { status = s },
                    Modifier.weight(1f),
                    selectedColor = if (s == JobStatus.FAILED) LL.Hot else LL.Lime,
                )
            }
        }

        if (printers.isNotEmpty()) {
            SectionLabel("; PRINTER")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                printers.forEach { p -> LChip(p.nickname, p.id == printerId, { printerId = p.id }) }
                LChip("None", printerId == null, { printerId = null })
            }
        }

        SectionLabel("; FILAMENT")
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pressable({ pickSpool = true })
                .background(LL.Graphite)
                .border(1.dp, LL.LineStrong)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (spool != null) {
                ColorDot(spool.color)
                Spacer(Modifier.width(8.dp))
                Text("${spool.name} · ${spool.material}", style = LLType.body(14.sp), modifier = Modifier.weight(1f))
                Text("${Fmt.grams(spool.remainingGrams)} left", style = LLType.mono(11.sp, color = LL.Muted))
            } else {
                Text("No spool linked (tap to pick)", style = LLType.body(14.sp, color = LL.Soft), modifier = Modifier.weight(1f))
            }
        }
        if (spool == null) {
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Presets.materials.take(6).forEach { m -> LChip(m.name, material == m.name, { material = m.name }) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Filament", grams, { grams = it }, Modifier.weight(1.2f), suffix = "g")
            NumberField("Hours", hours, { hours = it }, Modifier.weight(1f), suffix = "h", decimal = false)
            NumberField("Minutes", mins, { mins = it }, Modifier.weight(1f), suffix = "m", decimal = false)
        }

        if (status == JobStatus.FAILED) {
            SectionLabel("; WHAT WENT WRONG", color = LL.Hot)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Presets.failReasons.forEach { r -> LChip(r, reason == r, { reason = r }, selectedColor = LL.Hot) }
            }
            Spacer(Modifier.height(10.dp))
            NumberField("Failed at", failedAt, { failedAt = it }, suffix = "% done")
        }

        Spacer(Modifier.height(10.dp))
        LTextField("Notes", notes, { notes = it }, placeholder = "Settings, slicer profile, fixes…")

        SectionLabel("; ESTIMATED COST")
        KeyValueRow("Filament + power + wear", workshop.money.format(cost), LL.Lime)
        if (status == JobStatus.PRINTING) {
            Text("Stock and machine hours are booked when you mark it done or failed.", style = LLType.mono(11.sp, color = LL.Muted))
        }
    }

    if (pickSpool) {
        PickerDialog(
            code = "; M600 — PICK SPOOL",
            title = "Which spool?",
            items = listOf<Spool?>(null) + spools,
            label = { it?.name ?: "No spool" },
            sub = { s -> s?.let { "${it.material} · ${it.brand} · ${Fmt.grams(it.remainingGrams)} left" } ?: "Track cost only" },
            isSelected = { it?.id == spoolId },
            onDismiss = { pickSpool = false },
            onPick = { spoolId = it?.id; pickSpool = false },
        )
    }
}

/** Create or edit a spool. */
@Composable
fun SpoolEditorDialog(
    initial: Spool?,
    workshop: Workshop,
    onDismiss: () -> Unit,
    onSave: (Spool) -> Unit,
    onDelete: ((Spool) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var brand by remember { mutableStateOf(initial?.brand ?: "") }
    var material by remember { mutableStateOf(initial?.material ?: "PLA") }
    var color by remember { mutableStateOf(initial?.color ?: Presets.spoolColors.first()) }
    var diameter by remember { mutableStateOf(initial?.diameter ?: 1.75) }
    var total by remember { mutableStateOf(Fmt.trim(initial?.totalGrams ?: 1000.0, 0)) }
    var remaining by remember { mutableStateOf(Fmt.trim(initial?.remainingGrams ?: 1000.0, 0)) }
    var price by remember { mutableStateOf(Fmt.trim(initial?.price ?: workshop.filamentPerKg, workshop.decimals)) }
    var tare by remember { mutableStateOf(Fmt.trim(initial?.tareGrams ?: 230.0, 0)) }
    var low by remember { mutableStateOf(Fmt.trim(initial?.lowThreshold ?: 200.0, 0)) }

    val valid = name.isNotBlank() && (total.num() ?: 0.0) > 0
    LDialog(
        code = if (initial == null) "; M600 — LOAD SPOOL" else "; M600 — EDIT SPOOL",
        title = if (initial == null) "New spool" else initial.name,
        onDismiss = onDismiss,
        actions = {
            if (initial != null && onDelete != null) {
                LButton("Delete", { onDelete(initial) }, Modifier.weight(1f), style = ButtonStyle.Ghost)
            } else {
                LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            }
            LButton(
                "Save",
                {
                    val t = total.num() ?: 1000.0
                    val base = initial ?: Spool(name = name, brand = brand, material = material, color = color, price = 0.0)
                    onSave(
                        base.copy(
                            name = name.trim(), brand = brand.trim(), material = material, color = color, diameter = diameter,
                            totalGrams = t, remainingGrams = (remaining.num() ?: t).coerceIn(0.0, t * 1.5),
                            price = price.num() ?: 0.0, tareGrams = tare.num() ?: 0.0, lowThreshold = low.num() ?: 200.0,
                        ),
                    )
                },
                Modifier.weight(1f),
                enabled = valid,
            )
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LTextField("Colour name", name, { name = it }, Modifier.weight(1.3f), placeholder = "Signal Orange")
            LTextField("Brand", brand, { brand = it }, Modifier.weight(1f), placeholder = "eSUN")
        }
        SectionLabel("; MATERIAL")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Presets.materials.forEach { m -> LChip(m.name, material == m.name, { material = m.name }) }
        }
        SectionLabel("; COLOUR")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Presets.spoolColors.forEach { c ->
                Box(
                    Modifier
                        .size(36.dp)
                        .pressable({ color = c })
                        .semantics { contentDescription = "Colour swatch" }
                        .background(Color(c), CircleShape)
                        .border(if (c == color) 3.dp else 1.dp, if (c == color) LL.Lime else LL.LineStrong, CircleShape),
                )
            }
        }
        SectionLabel("; DIAMETER")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1.75, 2.85).forEach { d -> LChip("$d mm", diameter == d, { diameter = d }, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Net weight", total, { total = it }, Modifier.weight(1f), suffix = "g")
            NumberField("Left", remaining, { remaining = it }, Modifier.weight(1f), suffix = "g")
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Price paid", price, { price = it }, Modifier.weight(1f), suffix = workshop.symbol.trim())
            NumberField("Empty spool", tare, { tare = it }, Modifier.weight(1f), suffix = "g")
        }
        Spacer(Modifier.height(10.dp))
        NumberField("Warn me below", low, { low = it }, suffix = "g")
    }
}

/** Add or edit a printer. New printers start from a model preset. */
@Composable
fun PrinterEditorDialog(
    initial: Printer?,
    workshop: Workshop,
    onDismiss: () -> Unit,
    onSave: (Printer, MachinePreset?) -> Unit,
    onDelete: ((Printer) -> Unit)? = null,
) {
    var preset by remember { mutableStateOf<MachinePreset?>(if (initial == null) Presets.machines[2] else null) }
    var nickname by remember { mutableStateOf(initial?.nickname ?: "") }
    var watts by remember { mutableStateOf(Fmt.trim(initial?.watts ?: Presets.machines[2].avgWatts, 0)) }
    var price by remember { mutableStateOf(Fmt.trim(initial?.price ?: (Presets.machines[2].priceUsd * workshop.perUsd), 0)) }
    var life by remember { mutableStateOf(Fmt.trim(initial?.lifeHours ?: Presets.machines[2].lifeHours, 0)) }
    var hoursRun by remember { mutableStateOf(Fmt.trim(initial?.hoursRun ?: 0.0, 1)) }
    var pickModel by remember { mutableStateOf(false) }

    fun applyPreset(p: MachinePreset) {
        preset = p
        watts = Fmt.trim(p.avgWatts, 0)
        price = Fmt.trim(p.priceUsd * workshop.perUsd, 0)
        life = Fmt.trim(p.lifeHours, 0)
    }

    val brand = preset?.brand ?: initial?.brand ?: ""
    val model = preset?.model ?: initial?.model ?: ""
    LDialog(
        code = if (initial == null) "; M115 — ADD PRINTER" else "; M115 — EDIT PRINTER",
        title = if (initial == null) "Add a printer" else initial.nickname,
        onDismiss = onDismiss,
        actions = {
            if (initial != null && onDelete != null) {
                LButton("Delete", { onDelete(initial) }, Modifier.weight(1f), style = ButtonStyle.Ghost)
            } else {
                LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            }
            LButton(
                "Save",
                {
                    val base = initial ?: Printer(nickname = "", brand = brand, model = model, watts = 0.0, price = 0.0, lifeHours = 0.0)
                    onSave(
                        base.copy(
                            nickname = nickname.trim().ifEmpty { model },
                            brand = brand,
                            model = model,
                            kind = preset?.kind?.name ?: base.kind,
                            watts = watts.num() ?: 100.0,
                            price = price.num() ?: 0.0,
                            lifeHours = life.num() ?: 5000.0,
                            hoursRun = hoursRun.num() ?: 0.0,
                        ),
                        preset,
                    )
                },
                Modifier.weight(1f),
                enabled = model.isNotBlank(),
            )
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pressable({ pickModel = true })
                .background(LL.Graphite)
                .border(1.dp, LL.LineStrong)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column2(brand.uppercase(), model)
            Spacer(Modifier.weight(1f))
            Text("CHANGE", style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Lime))
        }
        Spacer(Modifier.height(10.dp))
        LTextField("Nickname", nickname, { nickname = it }, placeholder = "Big Orange")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Avg power", watts, { watts = it }, Modifier.weight(1f), suffix = "W")
            NumberField("Price", price, { price = it }, Modifier.weight(1f), suffix = workshop.symbol.trim())
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Expected life", life, { life = it }, Modifier.weight(1f), suffix = "h")
            NumberField("Hours run", hoursRun, { hoursRun = it }, Modifier.weight(1f), suffix = "h")
        }
        Text(
            "Average draw while printing, not the PSU rating. A smart plug gives the real number.",
            style = LLType.mono(11.sp, color = LL.Muted),
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (pickModel) {
        PickerDialog(
            code = "; M115 — MODEL",
            title = "Printer model",
            items = Presets.machines,
            label = { "${it.brand} ${it.model}" },
            sub = { "${Fmt.trim(it.avgWatts, 0)} W avg · ${it.kind.label()}" },
            isSelected = { it == preset },
            onDismiss = { pickModel = false },
            onPick = { applyPreset(it); pickModel = false },
        )
    }
}

fun MachineKind.label(): String = when (this) {
    MachineKind.CoreXY -> "CoreXY"
    MachineKind.BedSlinger -> "bed-slinger"
    MachineKind.Enclosed -> "enclosed"
}

@Composable
private fun Column2(top: String, bottom: String) {
    androidx.compose.foundation.layout.Column {
        Text(top, style = LLType.mono(10.sp, color = LL.Muted))
        Text(bottom, style = LLType.display(18.sp, FontWeight.SemiBold))
    }
}
