package com.dailycash.tracker.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailycash.tracker.ads.NativeAdCard
import com.dailycash.tracker.data.CountryDetector
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.data.Stats
import com.dailycash.tracker.ui.MainViewModel
import com.dailycash.tracker.ui.components.EmptyState
import com.dailycash.tracker.ui.components.SectionHeader
import com.dailycash.tracker.ui.theme.Mono
import com.dailycash.tracker.ui.theme.Palette
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val stampInks = listOf(Palette.Mint, Palette.Amber, Palette.Coral, Palette.Sky, androidx.compose.ui.graphics.Color(0xFFC6A8FF))

@Composable
fun AtlasScreen(vm: MainViewModel) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val home by vm.homeCurrency.collectAsStateWithLifecycle()
    val rates by vm.rates.collectAsStateWithLifecycle()
    val countries = remember(entries, home, rates) { Stats.countries(entries, home, rates) }
    val total = countries.sumOf { it.totalHome }
    val currencyCount = countries.flatMap { it.currencies }.distinct().size
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = countries.firstOrNull { it.countryCode == picked } ?: countries.lastOrNull()

    // Baseline: an average day in a country that uses the home currency.
    val homeStats = countries.filter { CountryDetector.placeFor(it.countryCode).currency == home }
    val baseline = if (homeStats.isNotEmpty()) homeStats.sumOf { it.totalHome } / homeStats.sumOf { it.days }.coerceAtLeast(1) else null

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("WHERE YOUR CASH WENT", color = Palette.Muted, fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    Text("Spend Atlas", color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Money.format(total, home), color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${countries.size} countries · $currencyCount currencies", color = Palette.Muted, fontSize = 12.sp)
                }
            }
        }
        item {
            Globe(
                codes = countries.map { it.countryCode },
                current = vm.place.countryCode,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
        if (countries.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.TravelExplore,
                    "Your atlas is empty",
                    "Every cash spend is stamped with the country you're in.\nLog one and your first stamp appears here.",
                )
            }
            return@LazyColumn
        }
        item { NativeAdCard(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        item {
            SectionHeader("Passport stamps", Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
        }
        val rows = countries.chunked(2)
        itemsIndexed(rows, key = { _, row -> row.joinToString { it.countryCode } }) { rowIndex, row ->
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { i, s ->
                    val index = rowIndex * 2 + i
                    Stamp(
                        stat = s,
                        index = index,
                        selected = s.countryCode == selected?.countryCode,
                        onClick = { picked = s.countryCode },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (selected != null) {
            item {
                DayCompare(
                    name = selected.name,
                    perDay = selected.perDayHome,
                    baseline = baseline,
                    home = home,
                    isHomeCountry = CountryDetector.placeFor(selected.countryCode).currency == home,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Globe(codes: List<String>, current: String, modifier: Modifier = Modifier) {
    val loop = rememberInfiniteTransition(label = "globe")
    val turn by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "turn")
    val ping by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1_800, easing = LinearOutSlowInEasing)), label = "ping")
    val draw = remember(codes) { Animatable(0f) }
    LaunchedEffect(codes) { draw.animateTo(1f, tween(2_200, easing = FastOutSlowInEasing)) }
    val measurer = rememberTextMeasurer()
    val label = TextStyle(color = Palette.Text, fontSize = 10.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)

    Box(
        modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface2)
            .border(1.dp, Palette.Line, RoundedCornerShape(24.dp))
            .semantics { contentDescription = "Globe with ${codes.size} visited countries" },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.height * 0.42f
            drawCircle(Palette.Raised, r, c)
            val sphere = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
            clipPath(sphere) {
                // Meridians at turning longitudes, projected as ellipses.
                for (k in 0 until 6) {
                    val lon = (turn + k / 6f) * Math.PI
                    val rx = r * kotlin.math.abs(sin(lon)).toFloat()
                    drawOval(
                        Palette.Line,
                        topLeft = Offset(c.x - rx, c.y - r),
                        size = Size(rx * 2f + 1f, r * 2f),
                        style = Stroke(1.dp.toPx()),
                    )
                }
                for (lat in listOf(-0.6f, -0.3f, 0f, 0.3f, 0.6f)) {
                    val y = c.y + r * lat
                    val half = r * kotlin.math.sqrt(1f - lat * lat)
                    drawLine(Palette.Line, Offset(c.x - half, y), Offset(c.x + half, y), strokeWidth = 1.dp.toPx())
                }
            }
            drawCircle(Palette.Line, r, c, style = Stroke(1.dp.toPx()))

            if (codes.isEmpty()) return@Canvas
            val pins = codes.map { pinFor(it, c, r) }
            val route = Path().apply {
                moveTo(pins[0].x, pins[0].y)
                for (i in 1 until pins.size) {
                    val a = pins[i - 1]
                    val b = pins[i]
                    val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f - r * 0.35f)
                    quadraticTo(mid.x, mid.y, b.x, b.y)
                }
            }
            val pm = PathMeasure().apply { setPath(route, false) }
            val partial = Path()
            pm.getSegment(0f, pm.length * draw.value, partial, true)
            drawPath(partial, Palette.Mint, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

            pins.forEachIndexed { i, p ->
                val ink = stampInks[i % stampInks.size]
                if (codes[i] == current) {
                    drawCircle(ink.copy(alpha = (1f - ping) * 0.6f), 4.dp.toPx() + 12.dp.toPx() * ping, p)
                }
                drawCircle(ink, 4.dp.toPx(), p)
                val t = measurer.measure(codes[i], label)
                drawText(t, topLeft = Offset(p.x + 7.dp.toPx(), p.y - t.size.height / 2f))
            }
        }
        Text(
            "Route of your spending, in order",
            color = Palette.Muted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        )
    }
}

/** Stable, spread-out pin position per country code inside the globe. */
private fun pinFor(code: String, c: Offset, r: Float): Offset {
    val h = Math.floorMod(code.hashCode() * 31 + 17, 10_000)
    val angle = (h % 360) * Math.PI / 180.0
    val dist = r * (0.25f + (h / 360 % 10) / 14f)
    return Offset(c.x + (dist * cos(angle)).toFloat(), c.y + (dist * sin(angle) * 0.8).toFloat())
}

@Composable
private fun Stamp(stat: Stats.CountryStat, index: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = stampInks[index % stampInks.size]
    val rot = if (index % 2 == 0) -2.5f else 2f
    val enter = remember(stat.countryCode) { Animatable(0f) }
    LaunchedEffect(stat.countryCode) {
        kotlinx.coroutines.delay(index * 90L)
        enter.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
    }
    val lift by animateFloatAsState(if (selected) 1.04f else 1f, label = "stamp")
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                val s = (1.4f - 0.4f * e) * lift
                scaleX = s
                scaleY = s
                rotationZ = if (selected) 0f else rot * e + (1f - e) * -12f
            }
            .clip(shape)
            .background(if (selected) ink.copy(alpha = 0.12f) else Palette.Surface2)
            .border(2.dp, ink.copy(alpha = if (selected) 1f else 0.7f), shape)
            .clickable(role = Role.Button, onClickLabel = "Show ${stat.name}", onClick = onClick)
            .padding(12.dp),
    ) {
        Row {
            Text("${stat.countryCode} · ${stat.days} ${if (stat.days == 1) "day" else "days"}", color = ink, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(stat.currencies.joinToString(" "), color = ink, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(stat.name, color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        stat.localTotals.entries.take(2).forEach { (code, minor) ->
            Text(Money.format(minor, code), color = ink, fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DayCompare(name: String, perDay: Long, baseline: Long?, home: String, isHomeCountry: Boolean, modifier: Modifier = Modifier) {
    val max = maxOf(perDay, baseline ?: 0L).coerceAtLeast(1L)
    val there by animateFloatAsState(perDay.toFloat() / max, tween(700), label = "there")
    val base by animateFloatAsState((baseline ?: 0L).toFloat() / max, tween(700), label = "base")
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("An average day in $name", color = Palette.Text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(Money.format(perDay, home), color = Palette.Mint, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        CompareBar(if (isHomeCountry) "Here" else "There", there, Money.format(perDay, home), Palette.Mint)
        if (baseline != null && !isHomeCountry) {
            Spacer(Modifier.height(8.dp))
            CompareBar("Home", base, Money.format(baseline, home), Palette.Muted)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                isHomeCountry -> "This is your baseline. Other countries are compared with it."
                baseline == null || baseline == 0L -> "Log some spending at home to compare a day abroad with a normal day."
                else -> {
                    val ratio = perDay.toDouble() / baseline
                    if (ratio >= 1) String.format(Locale.getDefault(), "%.1f× what a normal day at home costs you.", ratio)
                    else String.format(Locale.getDefault(), "%d%% cheaper than a normal day at home.", ((1 - ratio) * 100).toInt())
                }
            },
            color = Palette.Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun CompareBar(label: String, fraction: Float, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.Muted, fontSize = 11.sp, modifier = Modifier.width(44.dp))
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Palette.Raised)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(color))
        }
        Text(value, color = Palette.Text, fontFamily = Mono, fontSize = 11.sp, modifier = Modifier.width(84.dp).padding(start = 8.dp), maxLines = 1)
    }
}
