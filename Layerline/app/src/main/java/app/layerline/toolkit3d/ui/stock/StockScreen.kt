package app.layerline.toolkit3d.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.ads.NativeAdSlot
import app.layerline.toolkit3d.data.Presets
import app.layerline.toolkit3d.data.Spool
import app.layerline.toolkit3d.domain.FilamentMath
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.SpoolEditorDialog
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ui.components.Chamfer
import app.layerline.toolkit3d.ui.components.ConfirmDialog
import app.layerline.toolkit3d.ui.components.EmptyState
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LChip
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.NumberEntryDialog
import app.layerline.toolkit3d.ui.components.Panel
import app.layerline.toolkit3d.ui.components.ScreenHeader
import app.layerline.toolkit3d.ui.components.SectionLabel
import app.layerline.toolkit3d.ui.components.SpoolRing
import app.layerline.toolkit3d.ui.components.Stat
import app.layerline.toolkit3d.ui.components.StepButton
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType

@Composable
fun StockScreen(ctx: ScreenContext) {
    val vm: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val workshop = vm.workshop.collectAsStateWithLifecycle().value ?: return
    val spools by vm.spools.collectAsStateWithLifecycle()
    val money = workshop.money

    var filter by rememberSaveable { mutableStateOf("All") }
    var editing by remember { mutableStateOf<Spool?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Spool?>(null) }

    // Weigh tool state.
    var weighSpoolId by rememberSaveable { mutableStateOf<Long?>(null) }
    var gross by rememberSaveable { mutableDoubleStateOf(812.0) }
    var tare by rememberSaveable { mutableDoubleStateOf(Presets.tares.first().second) }
    var editGross by remember { mutableStateOf(false) }

    val totalG = spools.sumOf { it.remainingGrams }
    val value = spools.sumOf { it.value }
    val low = spools.count { it.isLow }
    val materials = listOf("All") + spools.map { it.material }.distinct()
    val shown = spools.filter { filter == "All" || it.material == filter }

    ScreenScroll {
        ScreenHeader("; M600 — FILAMENT CHANGE", "Spool shelf") {
            LButton("Spool", { creating = true }, icon = LIcons.Plus, height = 44.dp)
        }

        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Stat("ON SHELF", Fmt.grams(totalG), Modifier.weight(1f))
            Stat("VALUE", money.compact(value), Modifier.weight(1f))
            Stat("RUNNING LOW", "$low spool" + if (low == 1) "" else "s", Modifier.weight(1f), color = if (low > 0) LL.Hot else LL.Ink)
        }

        // ---- Weigh a spool -----------------------------------------------------------------
        val weighSpool = spools.firstOrNull { it.id == weighSpoolId }
        val net = FilamentMath.remainingFromGross(gross, tare)
        val density = Presets.material(weighSpool?.material ?: "PLA").density
        val meters = FilamentMath.metersFromGrams(net, weighSpool?.diameter ?: 1.75, density)
        Spacer(Modifier.height(16.dp))
        Panel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                Text("⚖ WEIGH A SPOOL", style = LLType.mono(11.sp, color = LL.Muted), modifier = Modifier.weight(1f))
                Text("gross − tare = left", style = LLType.mono(11.sp, color = LL.Muted))
            }
            if (spools.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { LChip("Any", weighSpoolId == null, { weighSpoolId = null }) }
                    items(spools, key = { it.id }) { s ->
                        LChip(s.name, s.id == weighSpoolId, {
                            weighSpoolId = s.id
                            tare = s.tareGrams
                        })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", "Decrease gross weight", { gross = (gross - 10).coerceAtLeast(0.0) }, Modifier.width(44.dp), plus = false)
                Column(
                    Modifier.weight(1f).pressable({ editGross = true }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${Fmt.trim(gross, 0)} g", style = LLType.display(30.sp))
                    Text("ON THE SCALE · TAP TO TYPE", style = LLType.mono(9.sp, color = LL.Muted))
                }
                StepButton("+", "Increase gross weight", { gross += 10 }, Modifier.width(44.dp), plus = true)
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (weighSpool != null) {
                    item { LChip("This spool", tare == weighSpool.tareGrams, { tare = weighSpool.tareGrams }, sub = "${Fmt.trim(weighSpool.tareGrams, 0)} g", selectedColor = LL.Lime) }
                }
                items(Presets.tares, key = { it.first }) { (name, g) ->
                    LChip(name, tare == g, { tare = g }, sub = "${Fmt.trim(g, 0)} g tare", selectedColor = LL.Lime)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("REMAINING", style = LLType.mono(10.sp, color = LL.Muted))
                    Text("${Fmt.trim(net, 0)} g", style = LLType.display(30.sp, color = LL.Lime))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("≈ LENGTH · ${weighSpool?.diameter ?: 1.75} ${weighSpool?.material ?: "PLA"}", style = LLType.mono(10.sp, color = LL.Muted))
                    Text("${Fmt.trim(meters, 0)} m", style = LLType.display(22.sp, FontWeight.SemiBold))
                }
            }
            if (weighSpool != null) {
                Spacer(Modifier.height(12.dp))
                LButton("Update ${weighSpool.name}", {
                    vm.saveSpool(weighSpool.copy(remainingGrams = net, tareGrams = tare))
                    ctx.majorAction()
                }, Modifier.fillMaxWidth(), icon = LIcons.Check, height = 46.dp)
            }
        }

        // ---- Shelf -------------------------------------------------------------------------
        if (spools.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            EmptyState(
                "Your shelf is empty",
                "Add the spools you own to track what's left, what it's worth and when to reorder.",
                "Add spool",
                { creating = true },
                LIcons.Spool,
            )
        } else {
            SectionLabel("; SHELF")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(materials, key = { it }) { m ->
                    val n = if (m == "All") spools.size else spools.count { it.material == m }
                    LChip("$m $n", filter == m, { filter = m })
                }
            }
            Spacer(Modifier.height(12.dp))
            shown.chunked(2).forEachIndexed { rowIndex, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { s -> SpoolCard(s, Modifier.weight(1f)) { editing = s } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                if (rowIndex == 1) {
                    NativeAdSlot(ctx.app.ads, "stock")
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    if (creating || editing != null) {
        SpoolEditorDialog(
            initial = editing,
            workshop = workshop,
            onDismiss = { creating = false; editing = null },
            onSave = {
                val isNew = editing == null
                vm.saveSpool(it)
                creating = false
                editing = null
                if (isNew) ctx.majorAction()
            },
            onDelete = { deleting = it; editing = null },
        )
    }
    deleting?.let { s ->
        ConfirmDialog("Remove spool?", "\"${s.name}\" will be removed from the shelf. Print log entries keep their history.", "Remove", { deleting = null }) {
            vm.deleteSpool(s)
            deleting = null
        }
    }
    if (editGross) {
        NumberEntryDialog("Gross weight", gross, "g", { editGross = false }) { gross = it; editGross = false }
    }
}

@Composable
private fun SpoolCard(s: Spool, modifier: Modifier, onClick: () -> Unit) {
    val fraction = if (s.totalGrams > 0) (s.remainingGrams / s.totalGrams).toFloat() else 0f
    Box(modifier.pressable(onClick).background(LL.Panel, Chamfer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SpoolRing(fraction, Color(s.color), size = 60.dp, label = "${s.name}, ${(fraction * 100).toInt()} percent left")
            Spacer(Modifier.width(10.dp))
            Column {
                Text(s.name, style = LLType.body(14.sp, FontWeight.SemiBold), maxLines = 2)
                Text("${s.material} · ${s.brand.ifBlank { "—" }}", style = LLType.mono(10.sp, color = LL.Muted), maxLines = 1)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(Fmt.trim(s.remainingGrams, 0), style = LLType.mono(13.sp, FontWeight.SemiBold, if (s.isLow) LL.Hot else LL.Ink))
                    Text("/${Fmt.trim(s.totalGrams, 0)}g", style = LLType.mono(11.sp, color = LL.Muted))
                }
            }
        }
        if (s.isLow) {
            Text(
                "LOW",
                style = LLType.mono(9.sp, FontWeight.SemiBold, LL.Graphite),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(LL.Hot).padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}
