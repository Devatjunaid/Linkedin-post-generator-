package app.layerline.toolkit3d.ui.machines

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.ads.NativeAdSlot
import app.layerline.toolkit3d.data.JobStatus
import app.layerline.toolkit3d.data.MaintenanceTask
import app.layerline.toolkit3d.data.Presets
import app.layerline.toolkit3d.data.Printer
import app.layerline.toolkit3d.data.MachineKind
import app.layerline.toolkit3d.domain.CostCalculator
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.ui.PrinterEditorDialog
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ui.components.ButtonStyle
import app.layerline.toolkit3d.ui.components.Chamfer
import app.layerline.toolkit3d.ui.components.ChamferS
import app.layerline.toolkit3d.ui.components.ConfirmDialog
import app.layerline.toolkit3d.ui.components.EmptyState
import app.layerline.toolkit3d.ui.components.IconSquare
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LDialog
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.LTextField
import app.layerline.toolkit3d.ui.components.NumberField
import app.layerline.toolkit3d.ui.components.ScreenHeader
import app.layerline.toolkit3d.ui.components.SegmentBar
import app.layerline.toolkit3d.ui.components.Stat
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType

@Composable
fun MachinesScreen(ctx: ScreenContext) {
    val vm: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val workshop = vm.workshop.collectAsStateWithLifecycle().value ?: return
    val printers by vm.printers.collectAsStateWithLifecycle()
    val tasks by vm.maintenance.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val money = workshop.money

    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<Printer?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Printer?>(null) }
    var addingTaskFor by remember { mutableStateOf<Printer?>(null) }

    ScreenScroll {
        ScreenHeader("; M115 — FIRMWARE INFO", "Machine garage", onBack = ctx.back)
        Spacer(Modifier.height(18.dp))

        if (printers.isEmpty()) {
            EmptyState(
                "No printers yet",
                "Add your printers to price prints with their real power draw, track hours and get service reminders.",
                "Add printer",
                { creating = true },
                LIcons.Printer,
            )
        }

        printers.forEachIndexed { index, p ->
            val myTasks = tasks.filter { it.printerId == p.id }
            val busy = jobs.any { it.printerId == p.id && it.jobStatus == JobStatus.PRINTING }
            val due = myTasks.count { (p.hoursRun - it.hoursAtService) >= it.intervalHours * 0.8 }
            val (stateText, stateColor) = when {
                busy -> "PRINTING" to LL.Lime
                due > 0 -> "NEEDS SERVICE" to LL.Hot
                else -> "IDLE" to LL.Ink
            }
            val perHour = CostCalculator.machineCostPerHour(p.price, p.lifeHours, p.watts, workshop.kwhRate)
            val expanded = open == p.id

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (expanded) LL.Panel2 else LL.Panel, Chamfer),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable({ open = if (expanded) null else p.id })
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PrinterGlyph(stateColor, index, Modifier.size(54.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.nickname, style = LLType.display(20.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 1)
                            Spacer(Modifier.size(7.dp).background(stateColor))
                            Spacer(Modifier.width(5.dp))
                            Text(stateText, style = LLType.mono(10.sp, color = stateColor))
                        }
                        Text("${p.brand} ${p.model}", style = LLType.mono(11.sp, color = LL.Muted))
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${Fmt.trim(p.hoursRun, 0)} h run", style = LLType.mono(11.sp, color = LL.Soft))
                            Text("${Fmt.trim(p.watts, 0)} W", style = LLType.mono(11.sp, color = LL.Soft))
                            Text("${money.format(perHour)}/h", style = LLType.mono(11.sp, color = LL.Lime))
                        }
                    }
                }
                AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp)) {
                        Text("MAINTENANCE COUNTERS", style = LLType.mono(10.sp, color = LL.Muted), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        if (myTasks.isEmpty()) Text("No service jobs yet.", style = LLType.body(13.sp, color = LL.Soft))
                        myTasks.forEach { t -> TaskRow(t, p, onService = { vm.markServiced(t, p) }, onDelete = { vm.deleteTask(t) }) }
                        Text(
                            "+ ADD SERVICE JOB",
                            style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Lime),
                            modifier = Modifier.pressable({ addingTaskFor = p }).padding(vertical = 10.dp),
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .border(1.dp, LL.Line)
                                .padding(10.dp),
                        ) {
                            Stat("BOUGHT FOR", money.compact(p.price), Modifier.weight(1f), size = 13)
                            Stat("WEAR / H", money.format(if (p.lifeHours > 0) p.price / p.lifeHours else 0.0), Modifier.weight(1f), size = 13)
                            Stat(
                                "PAID OFF",
                                "${(if (p.lifeHours > 0) p.hoursRun / p.lifeHours * 100 else 0.0).coerceAtMost(100.0).toInt()}%",
                                Modifier.weight(1f), color = LL.Lime, size = 13,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LButton("Edit", { editing = p }, Modifier.weight(1f), icon = LIcons.Edit, style = ButtonStyle.Dark, height = 44.dp)
                            IconSquare(LIcons.Trash, "Delete printer", { deleting = p }, tint = LL.Hot)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (printers.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .pressable({ creating = true })
                    .border(1.dp, LL.LineStrong, ChamferS)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+ Add a printer · ${Presets.machines.size - 1} model presets", style = LLType.body(15.sp, FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.height(16.dp))
        NativeAdSlot(ctx.app.ads, "machines")
    }

    if (creating || editing != null) {
        PrinterEditorDialog(
            initial = editing,
            workshop = workshop,
            onDismiss = { creating = false; editing = null },
            onSave = { printer, preset ->
                if (editing == null) {
                    val kind = preset?.kind ?: MachineKind.BedSlinger
                    vm.addPrinter(printer, Presets.defaultMaintenance(kind))
                    ctx.majorAction()
                } else {
                    vm.updatePrinter(printer)
                }
                creating = false
                editing = null
            },
        )
    }
    deleting?.let { p ->
        ConfirmDialog("Remove ${p.nickname}?", "Its service counters are deleted. Logged prints stay in the log.", "Remove", { deleting = null }) {
            vm.deletePrinter(p)
            deleting = null
        }
    }
    addingTaskFor?.let { p ->
        AddTaskDialog(onDismiss = { addingTaskFor = null }) { name, interval ->
            vm.addTask(p, name, interval)
            addingTaskFor = null
        }
    }
}

@Composable
private fun TaskRow(t: MaintenanceTask, p: Printer, onService: () -> Unit, onDelete: () -> Unit) {
    val used = (p.hoursRun - t.hoursAtService).coerceAtLeast(0.0)
    val ratio = if (t.intervalHours > 0) used / t.intervalHours else 0.0
    val warn = ratio >= 0.8
    Column(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.name, style = LLType.mono(11.sp, color = LL.Ink), modifier = Modifier.weight(1f))
            Text("${Fmt.trim(used, 0)} / ${Fmt.trim(t.intervalHours, 0)} h", style = LLType.mono(11.sp, color = if (warn) LL.Hot else LL.Muted))
            Spacer(Modifier.width(8.dp))
            Text(
                "DONE",
                style = LLType.mono(10.sp, FontWeight.SemiBold, if (warn) LL.Graphite else LL.Lime),
                modifier = Modifier
                    .pressable(onService)
                    .background(if (warn) LL.Hot else Color.Transparent)
                    .border(1.dp, if (warn) LL.Hot else LL.LineStrong)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("✕", style = LLType.mono(12.sp, color = LL.Muted), modifier = Modifier.pressable(onDelete).padding(6.dp))
        }
        Spacer(Modifier.height(4.dp))
        SegmentBar(ratio.toFloat(), color = if (warn) LL.Hot else LL.Lime)
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("100") }
    val hours = interval.toDoubleOrNull()
    LDialog(
        code = "; M500 — SERVICE JOB",
        title = "New service job",
        onDismiss = onDismiss,
        actions = {
            LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            LButton("Add", { onAdd(name.trim(), hours ?: 100.0) }, Modifier.weight(1f), enabled = name.isNotBlank() && (hours ?: 0.0) > 0)
        },
    ) {
        LTextField("Job", name, { name = it }, placeholder = "e.g. Replace PTFE tube")
        Spacer(Modifier.height(10.dp))
        NumberField("Every", interval, { interval = it }, suffix = "print hours")
    }
}

/** Front view of a printer: frame, gantry, toolhead and a part on the bed. */
@Composable
private fun PrinterGlyph(statusColor: Color, seed: Int, modifier: Modifier) {
    val partColors = listOf(Color(0xFFFF7A3D), Color(0xFF5B7BF0), Color(0xFFEDEDE6), Color(0xFFD9B44A))
    Canvas(modifier) {
        val s = size.minDimension / 54f
        drawRect(LL.Graphite, Offset(3 * s, 3 * s), Size(48 * s, 48 * s))
        drawRect(LL.LineStrong, Offset(3 * s, 3 * s), Size(48 * s, 48 * s), style = Stroke(1 * s))
        drawLine(LL.LineStrong, Offset(3 * s, 13 * s), Offset(51 * s, 13 * s), 1 * s)
        val headX = 12f + (seed * 9 % 24)
        drawRect(statusColor, Offset(headX * s, 13 * s), Size(12 * s, 9 * s))
        val tip = Path().apply {
            moveTo((headX + 3) * s, 22 * s); lineTo((headX + 9) * s, 22 * s); lineTo((headX + 6) * s, 27 * s); close()
        }
        drawPath(tip, statusColor)
        drawRect(LL.LineStrong, Offset(11 * s, 40 * s), Size(32 * s, 4 * s))
        drawRect(partColors[seed % partColors.size], Offset(20 * s, 32 * s), Size(14 * s, 8 * s))
    }
}
