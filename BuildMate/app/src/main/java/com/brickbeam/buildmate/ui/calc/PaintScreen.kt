package com.brickbeam.buildmate.ui.calc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.CanPlan
import com.brickbeam.buildmate.calc.Category
import com.brickbeam.buildmate.calc.Conv
import com.brickbeam.buildmate.calc.PaintCalculator
import com.brickbeam.buildmate.calc.PaintInput
import com.brickbeam.buildmate.calc.Surface
import com.brickbeam.buildmate.calc.UnitSystem
import com.brickbeam.buildmate.data.DraftItem
import com.brickbeam.buildmate.ui.components.BmCard
import com.brickbeam.buildmate.ui.components.InfoNote
import com.brickbeam.buildmate.ui.components.InputField
import com.brickbeam.buildmate.ui.components.PickerField
import com.brickbeam.buildmate.ui.components.Reveal
import com.brickbeam.buildmate.ui.components.ResultHero
import com.brickbeam.buildmate.ui.components.SectionTitle
import com.brickbeam.buildmate.ui.components.Segmented
import com.brickbeam.buildmate.ui.components.Stat
import com.brickbeam.buildmate.ui.components.Stepper
import com.brickbeam.buildmate.ui.components.ToggleRow
import com.brickbeam.buildmate.ui.graphics.PaintRoomPreview
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette

private data class Swatch(val name: String, val color: Color)

private val SWATCHES = listOf(
    Swatch("Sage", Color(0xFF8FA58A)), Swatch("Clay", Color(0xFFC9755B)), Swatch("Ocean", Color(0xFF3E6E8E)),
    Swatch("Plum", Color(0xFF7A4E7E)), Swatch("Chalk", Color(0xFFEDE8DF)), Swatch("Charcoal", Color(0xFF3B3F45)),
)

@Composable
fun PaintScreen() {
    val ctx = rememberCalcContext()
    val p = LocalPalette.current
    val sys = ctx.system
    val fmt = ctx.fmt
    val imperial = sys == UnitSystem.IMPERIAL
    val spec = ctx.spec

    var roomMode by rememberSaveable { mutableStateOf(true) }
    var length by rememberSaveable(sys) { mutableStateOf(if (imperial) "14'" else "4.2") }
    var width by rememberSaveable(sys) { mutableStateOf(if (imperial) "12'" else "3.6") }
    var height by rememberSaveable(sys) { mutableStateOf(if (imperial) "8'" else "2.4") }
    var wallArea by rememberSaveable(sys) { mutableStateOf(if (imperial) "400" else "35") }
    var doors by rememberSaveable { mutableStateOf(1) }
    var windows by rememberSaveable { mutableStateOf(1) }
    var doorW by rememberSaveable(sys) { mutableStateOf(if (imperial) "3'" else "0.84") }
    var doorH by rememberSaveable(sys) { mutableStateOf(if (imperial) "6' 8\"" else "2.0") }
    var winW by rememberSaveable(sys) { mutableStateOf(if (imperial) "3'" else "1.5") }
    var winH by rememberSaveable(sys) { mutableStateOf(if (imperial) "4'" else "1.2") }
    var wallCoats by rememberSaveable { mutableStateOf(2) }
    var ceiling by rememberSaveable { mutableStateOf(false) }
    var ceilingCoats by rememberSaveable { mutableStateOf(2) }
    var coverage by rememberSaveable(sys) { mutableStateOf(if (imperial) "350" else "10") }
    var surface by rememberSaveable { mutableStateOf(Surface.SMOOTH) }
    var primer by rememberSaveable { mutableStateOf(false) }
    var primerCov by rememberSaveable(sys) { mutableStateOf(if (imperial) "300" else "8") }
    var swatchIdx by rememberSaveable { mutableStateOf(0) }
    val swatch = SWATCHES[swatchIdx]

    fun covToMetric(v: Double) = if (imperial) v * Conv.M2_PER_FT2 / Conv.L_PER_GAL else v
    val lenM = parseLen(length, sys)
    val widM = parseLen(width, sys)
    val hgtM = parseLen(height, sys)
    val areaUser = parseNum(wallArea)?.takeIf { it > 0 }
    val dW = parseLen(doorW, sys); val dH = parseLen(doorH, sys)
    val wW = parseLen(winW, sys); val wH = parseLen(winH, sys)
    val cov = parseNum(coverage)?.takeIf { it > 0 }
    val pCov = parseNum(primerCov)?.takeIf { it > 0 }
    val dimsOk = if (roomMode) lenM != null && widM != null && hgtM != null else areaUser != null
    val valid = dimsOk && cov != null && (!primer || pCov != null) &&
        (doors == 0 || (dW != null && dH != null)) && (windows == 0 || (wW != null && wH != null))
    val result = if (valid) PaintCalculator.calculate(
        PaintInput(
            roomLengthM = lenM ?: 0.0, roomWidthM = widM ?: 0.0, wallHeightM = hgtM ?: 0.0,
            directWallAreaM2 = if (roomMode) null else fmt.areaFromUser(areaUser!!),
            doors = doors, doorAreaM2 = (dW ?: 0.0) * (dH ?: 0.0),
            windows = windows, windowAreaM2 = (wW ?: 0.0) * (wH ?: 0.0),
            wallCoats = wallCoats, includeCeiling = ceiling && roomMode, ceilingCoats = ceilingCoats,
            coverageM2PerL = covToMetric(cov!!), surface = surface, primer = primer,
            primerCoverageM2PerL = covToMetric(pCov ?: 1.0),
        ),
    ) else null

    val wallPlan = result?.let { PaintCalculator.planCans(it.wallLitres, spec.paintCans, ctx.prices) }
    val ceilPlan = result?.let { PaintCalculator.planCans(it.ceilingLitres, spec.paintCans, ctx.prices) }
    val primerPlan = result?.let { PaintCalculator.planCans(it.primerLitres, spec.paintCans, ctx.prices) }
    val totalCost = (wallPlan?.cost ?: 0.0) + (ceilPlan?.cost ?: 0.0) + (primerPlan?.cost ?: 0.0)

    fun planDrafts(plan: CanPlan?, what: String, note: String): List<DraftItem> =
        plan?.cans?.filter { it.count > 0 }?.map {
            DraftItem(Category.PAINT, "$what · ${it.can.label}", it.count.toDouble(), "can", ctx.price(it.can.priceKey), note)
        } ?: emptyList()

    CalcPage(
        title = "Paint",
        subtitle = "${swatch.name} · ${wallCoats} coats · ${ctx.region.name}",
        canAdd = result != null && (result.wallLitres + result.ceilingLitres + result.primerLitres) > 0,
        drafts = {
            planDrafts(wallPlan, "Wall paint (${swatch.name})", "${fmt.area(result?.paintableWallM2 ?: 0.0)} × $wallCoats coats") +
                planDrafts(ceilPlan, "Ceiling paint", "${fmt.area(result?.ceilingM2 ?: 0.0)} × $ceilingCoats coats") +
                planDrafts(primerPlan, "Primer", "1 coat")
        },
    ) {
        Reveal(0) {
            BmCard(Modifier.fillMaxWidth(), padding = PaddingValues(8.dp)) {
                PaintRoomPreview(swatch.color, Color(0xFFD9C7A7), p.ink, Modifier.fillMaxWidth().height(180.dp))
                if (result != null) {
                    Text(
                        "Walls ${fmt.area(result.wallAreaM2)} − openings ${fmt.area(result.openingsM2)} = ${fmt.area(result.paintableWallM2)}" +
                            if (result.ceilingM2 > 0) " · ceiling ${fmt.area(result.ceilingM2)}" else "",
                        style = BmType.tiny, color = p.mute, modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SWATCHES.forEachIndexed { i, s ->
                val on = i == swatchIdx
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(s.color)
                        .border(if (on) 3.dp else 1.dp, if (on) p.ink else p.line, RoundedCornerShape(14.dp))
                        .clickable(role = Role.RadioButton) { swatchIdx = i }
                        .semantics { contentDescription = s.name; selected = on },
                )
            }
        }
        Segmented(listOf(true, false), roomMode, { if (it) "Room" else "Wall area" }, { roomMode = it }, Modifier.fillMaxWidth())
        if (roomMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField("Length", length, { length = it }, Modifier.weight(1f), isError = lenM == null)
                InputField("Width", width, { width = it }, Modifier.weight(1f), isError = widM == null)
                InputField("Wall height", height, { height = it }, Modifier.weight(1f), isError = hgtM == null)
            }
        } else {
            InputField("Total wall area", wallArea, { wallArea = it }, Modifier.fillMaxWidth(), suffix = fmt.areaUnit, isError = areaUser == null)
        }

        SectionTitle("Openings")
        BmCard(Modifier.fillMaxWidth()) {
            Stepper("Doors", doors, { doors = it }, 0..20)
            if (doors > 0) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField("Door width", doorW, { doorW = it }, Modifier.weight(1f), isError = dW == null)
                InputField("Door height", doorH, { doorH = it }, Modifier.weight(1f), isError = dH == null)
            }
            Stepper("Windows", windows, { windows = it }, 0..30, Modifier.padding(top = 8.dp))
            if (windows > 0) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField("Window width", winW, { winW = it }, Modifier.weight(1f), isError = wW == null)
                InputField("Window height", winH, { winH = it }, Modifier.weight(1f), isError = wH == null)
            }
        }

        SectionTitle("Coats & coverage")
        BmCard(Modifier.fillMaxWidth()) {
            Stepper("Wall coats", wallCoats, { wallCoats = it }, 1..4)
            if (roomMode) {
                ToggleRow("Paint the ceiling too", ceiling, { ceiling = it })
                if (ceiling) Stepper("Ceiling coats", ceilingCoats, { ceilingCoats = it }, 1..4)
            }
            ToggleRow("Add a primer coat", primer, { primer = it }, sub = "Recommended on new plaster, drywall or big colour changes")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Paint coverage", coverage, { coverage = it }, Modifier.weight(1f), suffix = fmt.coverageUnit(), isError = cov == null, hint = "From the can label")
            if (primer) InputField("Primer coverage", primerCov, { primerCov = it }, Modifier.weight(1f), suffix = fmt.coverageUnit(), isError = pCov == null)
        }
        PickerField("Surface", surface, Surface.entries, { "${it.label} (×${it.factor})" }, { surface = it }, Modifier.fillMaxWidth())

        if (result != null && wallPlan != null) {
            Reveal(1) {
                ResultHero(
                    caption = "Wall paint",
                    figure = fmt.litres(result.wallLitres),
                    unit = "needed",
                    detail = "Cheapest can mix: ${wallPlan.description} = ${fmt.litres(wallPlan.totalLitres)} · ${fmt.litres((wallPlan.totalLitres - result.wallLitres).coerceAtLeast(0.0))} spare",
                ) {
                    val on = p.onAccent
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (ceilPlan != null && result.ceilingLitres > 0) Stat("Ceiling", ceilPlan.description, Modifier.weight(1f), on)
                        if (primerPlan != null && result.primerLitres > 0) Stat("Primer", primerPlan.description, Modifier.weight(1f), on)
                        Stat("Est. cost", fmt.money(totalCost), Modifier.weight(1f), on)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoNote("Can sizes and prices come from your region (${spec.paintCans.joinToString { it.label }}). Edit prices in Prices; the mix updates automatically.")
                Text("Ask for all cans from the same batch number so the colour matches.", style = BmType.tiny.copy(fontWeight = FontWeight.Bold), color = p.mute)
            }
        } else {
            InfoNote("Check the highlighted fields.")
        }
        NativeAdSlot("calc_paint")
    }
}
