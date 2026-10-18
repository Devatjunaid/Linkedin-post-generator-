package app.layerline.toolkit3d.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.ads.NativeAdSlot
import app.layerline.toolkit3d.data.JobStatus
import app.layerline.toolkit3d.data.PrintJob
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.domain.Money
import app.layerline.toolkit3d.ui.JobEditorDialog
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ui.components.ChamferS
import app.layerline.toolkit3d.ui.components.ConfirmDialog
import app.layerline.toolkit3d.ui.components.EmptyState
import app.layerline.toolkit3d.ui.components.LChip
import app.layerline.toolkit3d.ui.components.LIcon
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.Panel
import app.layerline.toolkit3d.ui.components.ScreenHeader
import app.layerline.toolkit3d.ui.components.Stat
import app.layerline.toolkit3d.ui.components.SuccessRing
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private enum class LogFilter(val label: String) { All("All"), Done("Done"), Failed("Failed"), Printing("Printing") }

/** Nozzle silhouette for the "Log print" button. */
private val NozzleShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.7f)
    lineTo(size.width * 0.88f, size.height)
    lineTo(size.width * 0.12f, size.height)
    lineTo(0f, size.height * 0.7f)
    close()
}

private fun dayKey(ms: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ms
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun LogScreen(ctx: ScreenContext) {
    val vm: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val workshop = vm.workshop.collectAsStateWithLifecycle().value ?: return
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val printers by vm.printers.collectAsStateWithLifecycle()
    val spools by vm.spools.collectAsStateWithLifecycle()
    val money = workshop.money
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(30_000); value = System.currentTimeMillis() }
    }

    var filter by rememberSaveable { mutableStateOf(LogFilter.All) }
    var editing by remember { mutableStateOf<PrintJob?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PrintJob?>(null) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    val finished = jobs.filter { it.jobStatus != JobStatus.PRINTING }
    val done = finished.count { it.jobStatus == JobStatus.DONE }
    val rate = if (finished.isEmpty()) 0f else done.toFloat() / finished.size
    val hours = finished.sumOf { it.minutes * it.usedFraction } / 60
    val grams = finished.sumOf { it.grams * it.usedFraction }
    val wasted = jobs.filter { it.jobStatus == JobStatus.FAILED }.sumOf { it.cost }
    val shown = jobs.filter {
        when (filter) {
            LogFilter.All -> true
            LogFilter.Done -> it.jobStatus == JobStatus.DONE
            LogFilter.Failed -> it.jobStatus == JobStatus.FAILED
            LogFilter.Printing -> it.jobStatus == JobStatus.PRINTING
        }
    }
    val today = dayKey(now)
    val yesterday = today - 86_400_000L

    Box(Modifier.fillMaxSize()) {
        ScreenScroll {
            ScreenHeader("; M31 — PRINT TIME", "Print log")

            Spacer(Modifier.height(16.dp))
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuccessRing(
                        rate,
                        centerText = if (finished.isEmpty()) "—" else "${(rate * 100).toInt()}%",
                        caption = "SUCCESS",
                        textStyle = LLType.display(24.sp),
                        captionStyle = LLType.mono(8.sp, color = LL.Muted),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row {
                            Stat("PRINTS", "${jobs.size}", Modifier.weight(1f))
                            Stat("HOURS", Fmt.num(hours, 1), Modifier.weight(1f))
                        }
                        Row {
                            Stat("FILAMENT", Fmt.grams(grams), Modifier.weight(1f))
                            Stat("WASTED", money.compact(wasted), Modifier.weight(1f), color = if (wasted > 0) LL.Hot else LL.Ink)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LogFilter.entries.forEach { f -> LChip(f.label, filter == f, { filter = f }) }
            }

            Spacer(Modifier.height(14.dp))
            if (jobs.isEmpty()) {
                EmptyState(
                    "No prints logged",
                    "Every print you log updates spool stock, machine hours, costs and your success rate.",
                    "Log a print",
                    { creating = true },
                    LIcons.Log,
                )
            }

            // Timeline with a Z-axis rail on the left.
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (shown.isNotEmpty()) {
                            var y = 4.dp.toPx()
                            while (y < size.height) {
                                drawLine(LL.LineStrong, Offset(6.dp.toPx(), y), Offset(14.dp.toPx(), y), 1.dp.toPx())
                                y += 6.dp.toPx()
                            }
                        }
                    }
                    .padding(start = 26.dp),
            ) {
                var lastDay = -1L
                shown.forEachIndexed { i, job ->
                    val day = dayKey(job.startedAt)
                    if (day != lastDay) {
                        lastDay = day
                        Box(Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                            Box(Modifier.offset(x = (-24).dp, y = 4.dp).size(width = 12.dp, height = 4.dp).background(LL.Ink))
                            Text(
                                when (day) {
                                    today -> "TODAY"
                                    yesterday -> "YESTERDAY"
                                    else -> dateFormat.format(Date(day)).uppercase()
                                },
                                style = LLType.mono(10.sp, color = LL.Muted),
                            )
                        }
                    }
                    JobCard(job, money, now) { editing = job }
                    Spacer(Modifier.height(8.dp))
                    if (i == 2) {
                        NativeAdSlot(ctx.app.ads, "log")
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
                .height(56.dp)
                .pressable({ creating = true })
                .background(LL.Lime, NozzleShape)
                .padding(start = 16.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LIcon(LIcons.Plus, LL.Graphite, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text("LOG PRINT", style = LLType.display(15.sp, color = LL.Graphite))
        }
    }

    if (creating || editing != null) {
        JobEditorDialog(
            initial = editing,
            printers = printers,
            spools = spools,
            workshop = workshop,
            onDismiss = { creating = false; editing = null },
            onSave = {
                vm.saveJob(it)
                creating = false
                editing = null
                ctx.majorAction()
            },
            onDelete = { deleting = it; editing = null },
        )
    }
    deleting?.let { j ->
        ConfirmDialog("Delete log entry?", "\"${j.name}\" will be removed. Filament already booked stays off the spool.", "Delete", { deleting = null }) {
            vm.deleteJob(j)
            deleting = null
        }
    }
}

@Composable
private fun JobCard(job: PrintJob, money: Money, now: Long, onClick: () -> Unit) {
    val status = job.jobStatus
    val (chipBg, chipFg, label) = when (status) {
        JobStatus.PRINTING -> Triple(Color.Transparent, LL.Lime, "PRINTING ${(job.progressAt(now) * 100).toInt()}%")
        JobStatus.DONE -> Triple(LL.Lime, LL.Graphite, "DONE")
        JobStatus.FAILED -> Triple(LL.Hot, LL.Graphite, "FAILED")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick)
            .background(LL.Panel, ChamferS)
            .padding(12.dp),
    ) {
        // Thumbnail: a little stack of layers in the filament colour; failed prints stop part-way.
        Column(
            Modifier.size(44.dp).background(LL.Graphite).padding(bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val c = Color(job.color)
            val topAlpha = if (status == JobStatus.DONE) 1f else 0.2f
            Box(Modifier.width(14.dp).height(4.dp).background(c.copy(alpha = topAlpha)))
            Box(Modifier.width(22.dp).height(4.dp).background(c.copy(alpha = topAlpha)))
            Box(Modifier.width(30.dp).height(4.dp).background(c))
            Box(Modifier.width(36.dp).height(4.dp).background(c))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.name, style = LLType.body(14.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = LLType.mono(9.sp, FontWeight.SemiBold, chipFg),
                    modifier = Modifier.background(chipBg).border(1.dp, if (chipBg == Color.Transparent) LL.Lime else chipBg).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(
                listOf(job.printerLabel, job.material).filter { it.isNotBlank() }.joinToString(" · "),
                style = LLType.mono(11.sp, color = LL.Muted),
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⏱ ${Fmt.duration(job.minutes)}", style = LLType.mono(11.sp, color = LL.Soft))
                Text("◎ ${Fmt.grams(job.grams * job.usedFraction)}", style = LLType.mono(11.sp, color = LL.Soft))
                Text(
                    money.format(job.cost) + if (status == JobStatus.FAILED) " lost" else "",
                    style = LLType.mono(11.sp, color = if (status == JobStatus.FAILED) LL.Hot else LL.Soft),
                )
            }
            if (status == JobStatus.FAILED && job.failReason.isNotBlank()) {
                Text(
                    "↳ ${job.failReason} at ${job.failedAtPercent.toInt()}%",
                    style = LLType.mono(11.sp, color = LL.Hot),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (job.notes.isNotBlank()) {
                Text(job.notes, style = LLType.body(12.sp, color = LL.Muted), maxLines = 2, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
