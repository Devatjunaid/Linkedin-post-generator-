package com.brickbeam.buildmate.ui.calc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.Category
import com.brickbeam.buildmate.calc.ConcreteCalculator
import com.brickbeam.buildmate.calc.ConcreteDims
import com.brickbeam.buildmate.calc.ConcreteShape
import com.brickbeam.buildmate.calc.Conv
import com.brickbeam.buildmate.calc.Market
import com.brickbeam.buildmate.calc.MixGrades
import com.brickbeam.buildmate.calc.PriceKey
import com.brickbeam.buildmate.calc.UnitSystem
import com.brickbeam.buildmate.data.DraftItem
import com.brickbeam.buildmate.ui.components.Badge
import com.brickbeam.buildmate.ui.components.BmCard
import com.brickbeam.buildmate.ui.components.Chip
import com.brickbeam.buildmate.ui.components.InfoNote
import com.brickbeam.buildmate.ui.components.InputField
import com.brickbeam.buildmate.ui.components.PickerField
import com.brickbeam.buildmate.ui.components.Reveal
import com.brickbeam.buildmate.ui.components.SectionTitle
import com.brickbeam.buildmate.ui.components.Segmented
import com.brickbeam.buildmate.ui.components.Stat
import com.brickbeam.buildmate.ui.graphics.CementArt
import com.brickbeam.buildmate.ui.graphics.SlabPreview
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette

private enum class Method(val label: String) { MIX("Mix yourself"), PREMIX("Pre-mixed bags"), READY("Ready-mix truck") }

private data class DimField(val label: String, val small: Boolean)

private fun fieldsFor(shape: ConcreteShape): List<DimField?> = when (shape) {
    ConcreteShape.SLAB -> listOf(DimField("Length", false), DimField("Width", false), DimField("Thickness", true))
    ConcreteShape.FOOTING -> listOf(DimField("Length", false), DimField("Width", true), DimField("Depth", true))
    ConcreteShape.WALL -> listOf(DimField("Length", false), DimField("Height", false), DimField("Thickness", true))
    ConcreteShape.COLUMN_ROUND -> listOf(DimField("Diameter", true), DimField("Height", false), null)
    ConcreteShape.COLUMN_SQUARE -> listOf(DimField("Side", true), DimField("Height", false), null)
    ConcreteShape.ROUND_PAD -> listOf(DimField("Diameter", false), null, DimField("Thickness", true))
    ConcreteShape.STEPS -> listOf(DimField("Step width", false), DimField("Riser", true), DimField("Tread", true))
}

private fun defaultsFor(shape: ConcreteShape, imperial: Boolean): List<String> = if (imperial) when (shape) {
    ConcreteShape.SLAB -> listOf("20'", "12'", "4", "1")
    ConcreteShape.FOOTING -> listOf("40'", "20", "12", "1")
    ConcreteShape.WALL -> listOf("20'", "4'", "8", "1")
    ConcreteShape.COLUMN_ROUND -> listOf("12", "8'", "", "4")
    ConcreteShape.COLUMN_SQUARE -> listOf("12", "8'", "", "4")
    ConcreteShape.ROUND_PAD -> listOf("10'", "", "4", "1")
    ConcreteShape.STEPS -> listOf("4'", "7", "11", "3")
} else when (shape) {
    ConcreteShape.SLAB -> listOf("6", "4", "150", "1")
    ConcreteShape.FOOTING -> listOf("12", "500", "300", "1")
    ConcreteShape.WALL -> listOf("6", "1.2", "200", "1")
    ConcreteShape.COLUMN_ROUND -> listOf("300", "2.5", "", "4")
    ConcreteShape.COLUMN_SQUARE -> listOf("300", "2.5", "", "4")
    ConcreteShape.ROUND_PAD -> listOf("3", "", "100", "1")
    ConcreteShape.STEPS -> listOf("1.2", "175", "280", "3")
}

@Composable
fun ConcreteScreen() {
    val ctx = rememberCalcContext()
    val p = LocalPalette.current
    val sys = ctx.system
    val fmt = ctx.fmt
    val imperial = sys == UnitSystem.IMPERIAL
    val spec = ctx.spec

    var shape by rememberSaveable { mutableStateOf(ConcreteShape.SLAB) }
    val defaults = defaultsFor(shape, imperial)
    var a by rememberSaveable(sys, shape) { mutableStateOf(defaults[0]) }
    var b by rememberSaveable(sys, shape) { mutableStateOf(defaults[1]) }
    var c by rememberSaveable(sys, shape) { mutableStateOf(defaults[2]) }
    var count by rememberSaveable(sys, shape) { mutableStateOf(defaults[3]) }
    val grades = MixGrades.forMarket(ctx.market)
    var gradeId by rememberSaveable(ctx.market) { mutableStateOf(MixGrades.default(ctx.market).id) }
    val grade = grades.firstOrNull { it.id == gradeId } ?: grades[1]
    var waste by rememberSaveable { mutableStateOf("5") }
    var premixIdx by rememberSaveable(ctx.market) { mutableStateOf(0) }
    val premix = spec.premixBags.getOrElse(premixIdx) { spec.premixBags[0] }

    val fields = fieldsFor(shape)
    fun parse(i: Int, text: String): Double? {
        val f = fields[i] ?: return 0.0
        return if (f.small) parseSmall(text, sys)?.div(1000.0) else parseLen(text, sys)
    }
    val av = parse(0, a)
    val bv = parse(1, b)
    val cv = parse(2, c)
    val countV = count.toIntOrNull()?.takeIf { it in 1..999 }
    val needsCount = shape == ConcreteShape.COLUMN_ROUND || shape == ConcreteShape.COLUMN_SQUARE || shape == ConcreteShape.STEPS
    val wasteV = parseNum(waste)?.coerceIn(0.0, 100.0)
    val valid = av != null && bv != null && cv != null && wasteV != null && (!needsCount || countV != null)
    val result = if (valid) ConcreteCalculator.calculate(
        ConcreteDims(shape, av!!, bv!!, cv!!, if (needsCount) countV!! else 1), grade, wasteV!!, spec, premix,
    ) else null

    val recommended = when {
        result == null -> Method.MIX
        result.recommendReadyMix -> Method.READY
        result.orderVolumeM3 <= 0.25 -> Method.PREMIX
        else -> Method.MIX
    }
    var methodName by rememberSaveable { mutableStateOf<String?>(null) }
    val method = methodName?.let { Method.valueOf(it) } ?: recommended

    val sandQty = (result?.sandKg ?: 0.0) / spec.bulkTonKg
    val gravelQty = (result?.gravelKg ?: 0.0) / spec.bulkTonKg
    val premixPrice = ctx.price(PriceKey.PREMIX_BAG) * premix.massKg / spec.premixBags[0].massKg
    val readyQty = (result?.readyMixOrderM3 ?: 0.0).let { if (ctx.market == Market.US) it / Conv.M3_PER_YD3 else it }
    val mixCost = result?.let { it.cementBags * ctx.price(PriceKey.CEMENT_BAG) + sandQty * ctx.price(PriceKey.SAND) + gravelQty * ctx.price(PriceKey.GRAVEL) } ?: 0.0
    val premixCost = (result?.premixBags ?: 0) * premixPrice
    val readyCost = readyQty * ctx.price(PriceKey.READYMIX)
    val term = ctx.region.localTerms["slab"]

    CalcPage(
        title = "Concrete",
        subtitle = "${shape.label}${if (term != null && shape == ConcreteShape.SLAB) " · $term" else ""} · ${ctx.region.name}",
        canAdd = result != null && result.orderVolumeM3 > 0,
        drafts = drafts@{
            val r = result ?: return@drafts emptyList()
            val note = "${shape.label} ${fmt.volume(r.netVolumeM3)} · ${grade.label}"
            when (method) {
                Method.MIX -> listOf(
                    DraftItem(Category.CEMENT, spec.concreteCement.label, r.cementBags.toDouble(), ctx.unitOf(PriceKey.CEMENT_BAG), ctx.price(PriceKey.CEMENT_BAG), note),
                    DraftItem(Category.CEMENT, "Sand", round2(sandQty), ctx.unitOf(PriceKey.SAND), ctx.price(PriceKey.SAND), note),
                    DraftItem(Category.CEMENT, "Gravel / aggregate", round2(gravelQty), ctx.unitOf(PriceKey.GRAVEL), ctx.price(PriceKey.GRAVEL), note),
                )
                Method.PREMIX -> listOf(
                    DraftItem(Category.CEMENT, "Pre-mixed concrete, ${premix.label}", r.premixBags.toDouble(), premix.label, round2(premixPrice), note),
                )
                Method.READY -> listOf(
                    DraftItem(Category.CEMENT, "Ready-mix ${grade.label}", round2(readyQty), ctx.unitOf(PriceKey.READYMIX), ctx.price(PriceKey.READYMIX), note),
                )
            }
        },
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConcreteShape.entries.forEach { s -> Chip(s.label, s == shape, { shape = s }) }
        }

        Reveal(0) {
            Box(Modifier.fillMaxWidth()) {
                BmCard(Modifier.fillMaxWidth(), padding = PaddingValues(8.dp)) {
                    val slabLike = shape == ConcreteShape.SLAB || shape == ConcreteShape.FOOTING || shape == ConcreteShape.ROUND_PAD || shape == ConcreteShape.WALL
                    if (slabLike) {
                        SlabPreview(
                            lengthM = av ?: 1.0,
                            widthM = when (shape) {
                                ConcreteShape.ROUND_PAD -> av ?: 1.0
                                ConcreteShape.WALL -> cv ?: 0.2
                                else -> bv ?: 1.0
                            },
                            thicknessM = cv ?: 0.1,
                            rebarSpacingM = if (imperial) 18 * Conv.M_PER_IN else 0.15,
                            showRebar = shape != ConcreteShape.WALL,
                            ink = p.ink,
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                        )
                    } else {
                        CementArt(Modifier.fillMaxWidth().height(150.dp))
                    }
                }
                if (result != null) {
                    Text(
                        fmt.volume(result.netVolumeM3), style = BmType.figure.copy(fontSize = 20.sp), color = p.ink,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to a, 1 to b, 2 to c).forEach { (i, v) ->
                val f = fields[i] ?: return@forEach
                InputField(
                    f.label, v,
                    { when (i) { 0 -> a = it; 1 -> b = it; else -> c = it } },
                    Modifier.weight(1f),
                    suffix = if (f.small) fmt.smallUnit else null,
                    isError = parse(i, v) == null,
                )
            }
            if (needsCount) {
                InputField(
                    if (shape == ConcreteShape.STEPS) "Steps" else "Count", count, { count = it.filter(Char::isDigit).take(3) },
                    Modifier.weight(0.8f), keyboardType = KeyboardType.Number, isError = countV == null,
                )
            }
        }
        Text(lengthHint(sys) + if (imperial) " · thickness in inches" else " · thickness in mm", style = BmType.tiny, color = p.mute)

        SectionTitle("Strength")
        Segmented(grades, grade, { it.label }, { gradeId = it.id }, Modifier.fillMaxWidth(), sublabel = { "≈ ${it.equivalent.removePrefix("≈")}" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Spill & over-dig allowance", waste, { waste = it }, Modifier.weight(1f), suffix = "%", isError = wasteV == null)
            PickerField("Pre-mix bag", premix, spec.premixBags, { it.label }, { premixIdx = spec.premixBags.indexOf(it) }, Modifier.weight(1f))
        }

        if (result != null) {
            SectionTitle("Three ways to get ${fmt.volume(result.orderVolumeM3)}")
            Reveal(1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MethodCard(
                        title = Method.MIX.label, selected = method == Method.MIX, recommended = recommended == Method.MIX,
                        headline = "${result.cementBags} × ${spec.concreteCement.label}", cost = fmt.money(mixCost),
                        lines = listOf(
                            "Sand ${fmt.bulk(result.sandKg)} · gravel ${fmt.bulk(result.gravelKg)}",
                            "Water ≈ ${fmt.litres(result.waterL)} (w/c ${grade.waterCementRatio})",
                        ),
                    ) { methodName = Method.MIX.name }
                    MethodCard(
                        title = Method.PREMIX.label, selected = method == Method.PREMIX, recommended = recommended == Method.PREMIX,
                        headline = "${fmt.int(result.premixBags)} × ${premix.label}", cost = fmt.money(premixCost),
                        lines = listOf("Just add water · best for posts and small pads"),
                    ) { methodName = Method.PREMIX.name }
                    MethodCard(
                        title = Method.READY.label, selected = method == Method.READY, recommended = recommended == Method.READY,
                        headline = "Order ${fmt.num(readyQty, 2)} ${ctx.unitOf(PriceKey.READYMIX)}", cost = fmt.money(readyCost),
                        lines = listOf(
                            "${result.trucks} truck${if (result.trucks == 1) "" else "s"} · rounded up to the plant's ${if (ctx.market == Market.US) "¼ yd³" else "0.25 m³"} step",
                        ) + if (result.shortLoad) listOf("Small load: many plants add a short-load fee") else emptyList(),
                    ) { methodName = Method.READY.name }
                }
            }
            InfoNote("Tap a card to choose what goes into your estimate. Mix design: ${fmt.num(grade.cementKgPerM3, 0)} kg cement per m³, 40/60 sand/gravel, 2,400 kg/m³ fresh concrete.")
        } else {
            InfoNote("Check the highlighted fields.")
        }
        NativeAdSlot("calc_concrete")
    }
}

@Composable
private fun MethodCard(
    title: String,
    selected: Boolean,
    recommended: Boolean,
    headline: String,
    cost: String,
    lines: List<String>,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val bg = if (selected) p.accent else p.surface
    val fg = if (selected) p.onAccent else p.ink
    BmCard(
        Modifier.fillMaxWidth().clickableCard(onClick, title),
        color = bg, border = !selected,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = BmType.label.copy(fontWeight = FontWeight.Bold), color = fg.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
            if (recommended) Badge("RECOMMENDED")
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(headline, style = BmType.figure.copy(fontSize = 18.sp), color = fg, modifier = Modifier.weight(1f))
            Text(cost, style = BmType.figure, color = fg)
        }
        lines.forEach { Text(it, style = BmType.tiny, color = fg.copy(alpha = 0.8f)) }
    }
}

internal fun round2(v: Double) = Math.round(v * 100) / 100.0

internal fun Modifier.clickableCard(onClick: () -> Unit, label: String): Modifier =
    this
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
