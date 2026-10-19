package com.dailycash.tracker.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailycash.tracker.ads.NativeAdCard
import com.dailycash.tracker.data.CountryDetector
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.data.Stats
import com.dailycash.tracker.ui.MainViewModel
import com.dailycash.tracker.ui.components.CategoryBadge
import com.dailycash.tracker.ui.components.EmptyState
import com.dailycash.tracker.ui.components.SectionHeader
import com.dailycash.tracker.ui.components.Segmented
import com.dailycash.tracker.ui.theme.Mono
import com.dailycash.tracker.ui.theme.Palette
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.sin

private enum class Range(val label: String) { WEEK("7 days"), MONTH("Month") }

@Composable
fun InsightsScreen(vm: MainViewModel) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val home by vm.homeCurrency.collectAsStateWithLifecycle()
    val rates by vm.rates.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    var range by rememberSaveable { mutableStateOf(Range.MONTH) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var day by rememberSaveable { mutableStateOf(today.dayOfMonth) }

    val (from, to) = when (range) {
        Range.WEEK -> today.minusDays(6) to today
        Range.MONTH -> month.atDay(1) to minOf(month.atEndOfMonth(), today)
    }
    val inRange = remember(entries, from, to) { Stats.spendsBetween(entries, from, to) }
    val total = remember(inRange, home, rates) { Stats.totalHome(inRange, home, rates) }
    val dayCount = (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1).coerceAtLeast(1)
    val cats = remember(inRange, home, rates) { Stats.byCategory(inRange, home, rates) }
    val daily = remember(entries, month, home, rates) { Stats.dailyTotals(entries, month, home, rates) }
    val leftovers = remember(balances, home, rates) { Stats.leftovers(balances, home, rates) }
    val currencyMix = remember(inRange, home, rates) {
        inRange.groupBy { it.currency }.mapValues { (_, l) -> Stats.totalHome(l, home, rates) }.entries.sortedByDescending { it.value }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Insights", color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp, modifier = Modifier.weight(1f))
                Segmented(Range.entries.toList(), range, { it.label }, { range = it })
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Spent", Money.format(total, home), Modifier.weight(1f))
                StatTile("Per day", Money.format(total / dayCount, home), Modifier.weight(1f))
                StatTile("Top", cats.firstOrNull()?.category?.name ?: "—", Modifier.weight(1f))
            }
        }
        item {
            CalendarCard(
                month = month,
                today = today,
                daily = daily,
                selectedDay = day,
                onDay = { day = it },
                onMonth = { month = it; day = if (it == YearMonth.now()) today.dayOfMonth else 1 },
                home = home,
                countriesOn = { d ->
                    entries.filter { Stats.dateOf(it) == month.atDay(d) }.map { it.countryCode }.distinct()
                        .joinToString { CountryDetector.countryName(it) }
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        if (leftovers.isNotEmpty()) {
            item { LeftoverCard(leftovers, home, Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) }
        }
        item { NativeAdCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        item {
            SectionHeader("Where the cash went", Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
        }
        if (cats.isEmpty()) {
            item { EmptyState(Icons.Rounded.Insights, "No spending in this period", "Your categories will line up here.") }
        } else {
            val max = cats.first().totalHome.coerceAtLeast(1)
            items(cats, key = { it.category.id }) { ct ->
                CategoryBar(ct, max, total, home, Modifier.padding(horizontal = 20.dp, vertical = 6.dp).animateItem())
            }
        }
        if (currencyMix.size > 1) {
            item {
                Column {
                SectionHeader("Currency mix", Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp))
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                ) {
                    currencyMix.forEachIndexed { i, (_, v) ->
                        Box(
                            Modifier
                                .weight((v.toFloat() / total.coerceAtLeast(1)).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Palette.notes[i % Palette.notes.size].ink),
                        )
                    }
                }
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    currencyMix.forEachIndexed { i, (code, v) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Palette.notes[i % Palette.notes.size].ink))
                            Spacer(Modifier.width(4.dp))
                            Text("$code ${(v * 100 / total.coerceAtLeast(1))}%", color = Palette.Muted, fontSize = 12.sp, fontFamily = Mono)
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(label.uppercase(), color = Palette.Muted, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    today: LocalDate,
    daily: Map<Int, Long>,
    selectedDay: Int,
    onDay: (Int) -> Unit,
    onMonth: (YearMonth) -> Unit,
    home: String,
    countriesOn: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val max = (daily.values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val offset = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value)
    val days = month.lengthOfMonth()
    val cells = List(offset) { 0 } + (1..days).toList()
    val reveal = remember(month) { Animatable(0f) }
    LaunchedEffect(month) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonth(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", tint = Palette.Text)
            }
            Text(
                "Cash calendar · " + month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year,
                color = Palette.Text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onMonth(month.plusMonths(1)) }, enabled = month < YearMonth.from(today)) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", tint = if (month < YearMonth.from(today)) Palette.Text else Palette.Faint)
            }
        }
        Row(Modifier.padding(vertical = 6.dp)) {
            DayOfWeek.entries.forEach {
                Text(
                    it.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    color = Palette.Muted,
                    fontSize = 11.sp,
                    fontFamily = Mono,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEachIndexed { week, row ->
            Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { col, d ->
                    val idx = week * 7 + col
                    val appear = ((reveal.value * 1.6f) - idx / 50f).coerceIn(0f, 1f)
                    val date = if (d > 0) month.atDay(d) else null
                    val future = date != null && date.isAfter(today)
                    val v = daily[d] ?: 0L
                    val level = if (v == 0L) 0 else (1 + (v * 3 / max)).toInt().coerceAtMost(4)
                    val (bg, fg) = when {
                        d == 0 -> Color.Transparent to Color.Transparent
                        future -> Color.Transparent to Palette.Faint
                        else -> heat[level]
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                alpha = appear
                                scaleX = 0.6f + 0.4f * appear
                                scaleY = 0.6f + 0.4f * appear
                            }
                            .clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .border(
                                if (d == selectedDay) 2.dp else 1.dp,
                                when {
                                    d == 0 -> Color.Transparent
                                    d == selectedDay -> Palette.Text
                                    future -> Palette.Line
                                    else -> Color.Transparent
                                },
                                RoundedCornerShape(9.dp),
                            )
                            .then(
                                if (d > 0 && !future) {
                                    Modifier
                                        .semantics { contentDescription = "Day $d, ${Money.format(v, home)}" }
                                        .clickable(role = Role.Button) { onDay(d) }
                                } else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (d > 0) Text("$d", color = fg, fontSize = 12.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        val v = daily[selectedDay] ?: 0L
        val selectedDate = month.atDay(selectedDay.coerceIn(1, days))
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.Surface2).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.getDefault())), color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val where = countriesOn(selectedDay.coerceIn(1, days))
                if (where.isNotBlank()) Text(where, color = Palette.Muted, fontSize = 12.sp, maxLines = 1)
            }
            Text(if (v > 0) Money.format(v, home) else "No cash spent", color = Palette.Amber, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

private val heat = listOf(
    Palette.Surface2 to Palette.Muted,
    Color(0xFF1D3A31) to Palette.Text,
    Color(0xFF2F7A62) to Palette.Text,
    Palette.Mint to Palette.Ink,
    Palette.Amber to Palette.Ink,
)

@Composable
private fun CategoryBar(ct: Stats.CategoryTotal, max: Long, total: Long, home: String, modifier: Modifier = Modifier) {
    val target = ct.totalHome.toFloat() / max
    val grow = remember(ct.category.id) { Animatable(0f) }
    LaunchedEffect(ct.category.id, target) { grow.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 120f)) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CategoryBadge(ct.category, 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(ct.category.name, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    "${Money.format(ct.totalHome, home)} · ${ct.totalHome * 100 / total.coerceAtLeast(1)}%",
                    color = Palette.Muted,
                    fontFamily = Mono,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.Raised)) {
                Box(Modifier.fillMaxWidth(grow.value.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ct.category.group.tint))
            }
        }
    }
}

@Composable
private fun LeftoverCard(items: List<Stats.Leftover>, home: String, modifier: Modifier = Modifier) {
    val loop = rememberInfiniteTransition(label = "jar")
    val jingle by loop.animateFloat(-1f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "j")
    val shine by animateFloatAsState(1f, tween(600), label = "s")
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2A1712))
            .border(1.dp, Color(0xFF6B3426), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(54.dp, 62.dp).graphicsLayer { rotationZ = jingle * 5f; alpha = shine }) {
            val w = size.width
            val h = size.height
            drawRoundRect(Color(0xFF6B3426), Offset(w * 0.2f, 0f), androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.12f), androidx.compose.ui.geometry.CornerRadius(4f))
            drawRoundRect(Color(0xFFFFA48E), Offset(w * 0.12f, h * 0.18f), androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.8f), androidx.compose.ui.geometry.CornerRadius(14f), style = Stroke(2.dp.toPx()))
            val coins = listOf(Color(0xFFE8C873), Color(0xFFD9DDE0), Color(0xFFE0A36B), Color(0xFFE8C873))
            coins.forEachIndexed { i, col ->
                val x = w * (0.32f + (i % 2) * 0.32f)
                val y = h * (0.82f - (i / 2) * 0.2f) + sin(jingle * 3f + i) * 2f
                drawCircle(col, w * 0.11f, Offset(x, y))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Leftover coin alert", color = Color(0xFFFFD3C6), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "You still hold " + items.joinToString(" and ") { Money.format(it.balance, it.currency) } +
                    (items.mapNotNull { it.homeValue }.sum().takeIf { it > 0 }?.let { " (≈ ${Money.format(it, home)})" } ?: "") +
                    ". Banks rarely exchange coins — spend them before you leave.",
                color = Color(0xFFF1C5B9),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}
