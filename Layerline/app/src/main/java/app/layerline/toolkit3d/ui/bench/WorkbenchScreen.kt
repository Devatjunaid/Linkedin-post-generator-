package app.layerline.toolkit3d.ui.bench

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.data.JobStatus
import app.layerline.toolkit3d.data.PrintJob
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.ui.JobEditorDialog
import app.layerline.toolkit3d.ui.Routes
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ads.NativeAdSlot
import app.layerline.toolkit3d.ui.components.ButtonStyle
import app.layerline.toolkit3d.ui.components.Chamfer
import app.layerline.toolkit3d.ui.components.ChamferS
import app.layerline.toolkit3d.ui.components.ExtrusionLine
import app.layerline.toolkit3d.ui.components.GcodeTag
import app.layerline.toolkit3d.ui.components.IconSquare
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LIcon
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.LayerStack
import app.layerline.toolkit3d.ui.components.LayerlineMark
import app.layerline.toolkit3d.ui.components.Panel
import app.layerline.toolkit3d.ui.components.SectionLabel
import app.layerline.toolkit3d.ui.components.Stat
import app.layerline.toolkit3d.ui.components.Tab
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.roundToInt

private fun startOfMonth(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun WorkbenchScreen(ctx: ScreenContext) {
    val vm: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val workshop = vm.workshop.collectAsStateWithLifecycle().value ?: return
    val printers by vm.printers.collectAsStateWithLifecycle()
    val spools by vm.spools.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000)
            value = System.currentTimeMillis()
        }
    }
    var editor by remember { mutableStateOf<PrintJob?>(null) }
    var creating by remember { mutableStateOf(false) }
    val money = workshop.money

    val active = jobs.firstOrNull { it.jobStatus == JobStatus.PRINTING }
    val month = startOfMonth()
    val monthJobs = jobs.filter { it.startedAt >= month && it.jobStatus != JobStatus.PRINTING }
    val monthGrams = monthJobs.sumOf { it.grams * it.usedFraction }
    val monthSpend = monthJobs.sumOf { it.cost }
    val monthHours = monthJobs.sumOf { it.minutes * it.usedFraction } / 60.0
    val finished = jobs.filter { it.jobStatus != JobStatus.PRINTING }
    val success = if (finished.isEmpty()) null else finished.count { it.jobStatus == JobStatus.DONE } * 100 / finished.size
    val lowSpools = spools.count { it.isLow }

    ScreenScroll {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            LayerlineMark(1f, Modifier.size(44.dp), showDots = false)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                GcodeTag("; G28 — ALL AXES HOMED")
                Text("WORKBENCH", style = LLType.display(30.sp))
            }
            Row(
                Modifier
                    .height(44.dp)
                    .pressable({ ctx.open(Routes.SETTINGS) })
                    .background(LL.Panel2, ChamferS)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(LL.Lime))
                Spacer(Modifier.width(6.dp))
                Text("${workshop.regionCode} · ${workshop.region.currency}", style = LLType.mono(12.sp))
            }
            Spacer(Modifier.width(8.dp))
            IconSquare(LIcons.Settings, "Settings", { ctx.open(Routes.SETTINGS) })
        }

        Spacer(Modifier.height(18.dp))
        if (active != null) {
            val p = active.progressAt(now)
            val leftMin = (active.minutes * (1 - p)).coerceAtLeast(0.0)
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(active.printerLabel.ifEmpty { "No printer linked" }.uppercase(), style = LLType.mono(11.sp, color = LL.Muted))
                        Text(active.name, style = LLType.display(22.sp, FontWeight.SemiBold), maxLines = 1)
                    }
                    Text(
                        if (p >= 1.0) "FINISHED?" else "PRINTING",
                        style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Graphite),
                        modifier = Modifier.background(if (p >= 1.0) LL.Hot else LL.Lime, ChamferS).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    LayerStack(p.toFloat(), Modifier.width(92.dp).height(96.dp))
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${(p * 100).roundToInt()}", style = LLType.display(46.sp, color = LL.Lime))
                            Text("%", style = LLType.display(20.sp, color = LL.Lime), modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Text("${active.material} · ${Fmt.grams(active.grams)}", style = LLType.mono(12.sp, color = LL.Muted))
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Stat("LEFT", Fmt.duration(leftMin), Modifier.weight(1f), size = 15)
                            Stat("TOTAL", Fmt.duration(active.minutes), Modifier.weight(1f), size = 15)
                            Stat("COST", money.format(active.cost), Modifier.weight(1.2f), size = 15)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LButton("Done", {
                        vm.saveJob(active.copy(status = JobStatus.DONE.name))
                        ctx.majorAction()
                    }, Modifier.weight(1f), icon = LIcons.Check, height = 46.dp)
                    LButton("Failed", { editor = active.copy(status = JobStatus.FAILED.name, failedAtPercent = p * 100) }, Modifier.weight(1f), style = ButtonStyle.Dark, height = 46.dp)
                }
            }
        } else {
            Panel(Modifier.fillMaxWidth()) {
                Text("BED IS CLEAR", style = LLType.mono(11.sp, color = LL.Muted))
                Text("Nothing printing right now", style = LLType.display(22.sp, FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                ExtrusionLine()
                Spacer(Modifier.height(10.dp))
                Text(
                    "Log a print as \"Printing\" to follow it here with a live progress stack and time left.",
                    style = LLType.body(14.sp, color = LL.Soft),
                )
                Spacer(Modifier.height(12.dp))
                LButton("Log a print", { creating = true }, icon = LIcons.Plus, height = 46.dp)
            }
        }

        SectionLabel("; TOOLS")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolTile("01", "Cost Quote", "${printers.size.coerceAtLeast(1)} machines · ${money.symbol.trim()}", LIcons.Cost, true, Modifier.weight(1f)) { ctx.open(Tab.Cost.route) }
            ToolTile(
                "02", "Spools",
                if (spools.isEmpty()) "Add your first spool" else "${spools.size} on shelf" + if (lowSpools > 0) " · $lowSpools low" else "",
                LIcons.Spool, false, Modifier.weight(1f), subColor = if (lowSpools > 0) LL.Hot else LL.Muted,
            ) { ctx.open(Tab.Stock.route) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolTile("03", "Calibrate", "8 calculators", LIcons.Calib, false, Modifier.weight(1f)) { ctx.open(Tab.Calib.route) }
            ToolTile("04", "Print Log", success?.let { "$it% success" } ?: "No prints yet", LIcons.Log, false, Modifier.weight(1f)) { ctx.open(Tab.Log.route) }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .pressable({ ctx.open(Routes.MACHINES) })
                .background(Color(0xFF14171A), ChamferS)
                .border(1.dp, LL.LineStrong, ChamferS)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LIcon(LIcons.Printer, LL.Ink, size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Text("Machine garage", style = LLType.body(15.sp, FontWeight.SemiBold))
            Spacer(Modifier.width(8.dp))
            Text(
                if (printers.isEmpty()) "add your printers" else "${printers.size} printer" + if (printers.size == 1) "" else "s",
                style = LLType.mono(11.sp, color = LL.Muted),
                modifier = Modifier.weight(1f),
            )
            Text("→", style = LLType.mono(16.sp, color = LL.Lime))
        }

        SectionLabel("; THIS MONTH")
        Row(Modifier.fillMaxWidth()) {
            Stat("FILAMENT", Fmt.grams(monthGrams), Modifier.weight(1f))
            Stat("SPEND", money.compact(monthSpend), Modifier.weight(1f))
            Stat("PRINT HOURS", "${Fmt.num(monthHours, 1)} h", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        NativeAdSlot(ctx.app.ads, "bench")
    }

    if (creating || editor != null) {
        JobEditorDialog(
            initial = editor,
            printers = printers,
            spools = spools,
            workshop = workshop,
            onDismiss = { creating = false; editor = null },
            onSave = {
                vm.saveJob(it)
                creating = false
                editor = null
                ctx.majorAction()
            },
        )
    }
}

@Composable
private fun ToolTile(
    index: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Boolean,
    modifier: Modifier = Modifier,
    subColor: Color = LL.Muted,
    onClick: () -> Unit,
) {
    val bg = if (accent) LL.Lime else LL.Panel2
    val fg = if (accent) LL.Graphite else LL.Ink
    Box(
        modifier
            .height(118.dp)
            .pressable(onClick)
            .clip(Chamfer)
            .background(bg),
    ) {
        Text(
            index,
            style = LLType.display(92.sp, color = if (accent) LL.Graphite.copy(alpha = 0.12f) else LL.Ink.copy(alpha = 0.05f)),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 22.dp),
        )
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
            LIcon(icon, fg, size = 26.dp)
            Spacer(Modifier.weight(1f))
            Text(title, style = LLType.display(19.sp, color = fg))
            Text(subtitle, style = LLType.mono(11.sp, color = if (accent) LL.Graphite else subColor), maxLines = 1)
        }
    }
}
