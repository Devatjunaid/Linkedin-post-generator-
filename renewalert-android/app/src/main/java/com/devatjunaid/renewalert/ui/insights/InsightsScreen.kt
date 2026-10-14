package com.devatjunaid.renewalert.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devatjunaid.renewalert.R
import com.devatjunaid.renewalert.ads.NativeAdCard
import com.devatjunaid.renewalert.ads.NativeStyle
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.domain.SuggestionType
import com.devatjunaid.renewalert.ui.AppViewModel
import com.devatjunaid.renewalert.ui.components.EmptyState
import com.devatjunaid.renewalert.ui.components.ProgressBar
import com.devatjunaid.renewalert.ui.components.SectionCard
import com.devatjunaid.renewalert.ui.home.CategoryBar
import com.devatjunaid.renewalert.ui.theme.Brand
import com.devatjunaid.renewalert.ui.theme.CategoryColors
import com.devatjunaid.renewalert.ui.theme.NumberStyle
import com.devatjunaid.renewalert.ui.theme.RenewAlertTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(vm: AppViewModel, onOpenDetail: (Long) -> Unit) {
    val p by vm.portfolio.collectAsState()
    val settings by vm.settings.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val s = settings ?: return
    val c = RenewAlertTheme.colors
    val hide = s.hideAmounts
    val cur = s.currency
    var years by rememberSaveable { mutableStateOf(false) }
    val bars: List<Pair<String, Double>> = remember(p, years) {
        if (years) p.yearlyHistory(5).map { it.first.toString() to it.second }
        else p.monthlyHistory(6).map { Formats.shortMonth(it.first) to it.second }
    }
    val suggestions = remember(p) { p.suggestions() }
    val savings = remember(p) { p.potentialSavingsMain }
    val total = bars.sumOf { it.second }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Insights", style = MaterialTheme.typography.headlineLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow(Modifier.width(170.dp)) {
                    SegmentedButton(selected = !years, onClick = { years = false }, shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}) { Text("6 months") }
                    SegmentedButton(selected = years, onClick = { years = true }, shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}) { Text("Years") }
                }
            }
        }

        if (loaded && p.subs.isEmpty()) {
            item { EmptyState("No data yet", "Add subscriptions and your spending trends, categories and saving tips appear here.", R.drawable.ill_insights) }
            return@LazyColumn
        }

        item {
            SectionCard {
                Text(if (years) "Spent per year" else "Spent in the last 6 months", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Text(Formats.money(total, cur, hide), style = MaterialTheme.typography.headlineLarge, color = c.onSurface)
                val first = bars.firstOrNull { it.second > 0 }?.second
                val last = bars.lastOrNull()?.second
                if (first != null && last != null && first > 0 && last != first) {
                    val pct = (last - first) * 100 / first
                    Text(
                        (if (pct > 0) "▲ " else "▼ ") + "%.0f%% since ${bars.first { it.second > 0 }.first}".format(kotlin.math.abs(pct)),
                        style = MaterialTheme.typography.labelMedium, color = if (pct > 0) c.warnText else c.success,
                    )
                }
                Spacer(Modifier.height(14.dp))
                BarChart(bars, cur, hide)
            }
        }

        if (savings > 0) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Brand.Navy, Brand.NavyLight))).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Possible savings found", style = MaterialTheme.typography.bodyMedium, color = c.onHeroMuted)
                    Text("Up to ${Formats.money(savings, cur, hide)} / yr", style = MaterialTheme.typography.headlineMedium, color = c.onHero)
                    Text("You decide. Nothing is ever cancelled for you.", style = MaterialTheme.typography.bodySmall, color = c.onHeroMuted)
                }
            }
        }

        items(suggestions) { sg ->
            val (icon, tint, bg) = when (sg.type) {
                SuggestionType.TRIAL_ENDING -> Triple(Icons.Rounded.Timer, c.warnText, c.warnContainer)
                SuggestionType.RARELY_USED -> Triple(Icons.Rounded.Schedule, c.warnText, c.warnContainer)
                SuggestionType.OVERLAP -> Triple(Icons.Rounded.ContentCopy, c.onAccentSoft, c.accentSoft)
                SuggestionType.PRICE_INCREASE -> Triple(Icons.AutoMirrored.Rounded.TrendingUp, c.onAccentSoft, c.accentSoft)
                SuggestionType.MISSING_RATE -> Triple(Icons.Rounded.CurrencyExchange, c.muted, c.surfaceAlt)
            }
            SuggestionCard(icon, tint, bg, sg.title, sg.body, sg.subscriptionId?.let { id -> { onOpenDetail(id) } })
        }

        item { NativeAdCard(vm.ads, NativeStyle.MEDIUM) }

        if (p.categories.isNotEmpty()) {
            item {
                SectionCard {
                    Text("Categories · per month", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    Spacer(Modifier.height(10.dp))
                    CategoryBar(p.categories.map { it.category to it.monthlyMain })
                    Spacer(Modifier.height(10.dp))
                    p.categories.forEach { cat ->
                        val pct = if (p.monthlyTotal > 0) cat.monthlyMain / p.monthlyTotal * 100 else 0.0
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(CategoryColors.chart(cat.category)))
                            Spacer(Modifier.width(10.dp))
                            Text(cat.category, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                            Text("%.0f%%".format(pct), style = MaterialTheme.typography.bodySmall, color = c.muted)
                            Spacer(Modifier.width(12.dp))
                            Text(Formats.money(cat.monthlyMain, cur, hide), style = NumberStyle, color = c.onSurface)
                        }
                    }
                }
            }
        }

        val top = p.active.sortedByDescending { p.yearlyMain(it) }.take(5)
        if (top.isNotEmpty()) {
            item {
                SectionCard {
                    Text("Most expensive per year", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    val max = p.yearlyMain(top.first()).takeIf { it > 0 } ?: 1.0
                    top.forEach { sub ->
                        Column(Modifier.fillMaxWidth().clickable { onOpenDetail(sub.id) }.padding(vertical = 8.dp)) {
                            Row {
                                Text(sub.name, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                                Text(Formats.money(p.yearlyMain(sub), cur, hide), style = NumberStyle, color = c.onSurface)
                            }
                            Spacer(Modifier.height(6.dp))
                            ProgressBar((p.yearlyMain(sub) / max).toFloat(), CategoryColors.chart(sub.category), c.divider, height = 6.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
    title: String,
    body: String,
    onClick: (() -> Unit)?,
) {
    val c = RenewAlertTheme.colors
    SectionCard(modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
        Row {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = c.muted)
                if (onClick != null) Text("Review", style = MaterialTheme.typography.labelLarge, color = c.accent, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun BarChart(bars: List<Pair<String, Double>>, currency: String, hide: Boolean) {
    val c = RenewAlertTheme.colors
    val max = bars.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    val grow = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) { grow.animateTo(1f, tween(700)) }
    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { i, (label, v) ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(
                    if (hide) "•" else Formats.moneyShort(v, currency),
                    style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                val frac = (v / max).toFloat() * grow.value
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(0.62f * frac.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (i == bars.lastIndex) Brush.verticalGradient(listOf(Brand.Blue, Brand.Violet)) else Brush.verticalGradient(listOf(c.accentSoft, c.accentSoft))),
                )
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1)
            }
        }
    }
}
