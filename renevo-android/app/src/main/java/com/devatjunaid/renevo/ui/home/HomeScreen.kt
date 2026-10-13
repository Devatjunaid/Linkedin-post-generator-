package com.devatjunaid.renevo.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devatjunaid.renevo.R
import com.devatjunaid.renevo.ads.NativeAdCard
import com.devatjunaid.renevo.ads.NativeStyle
import com.devatjunaid.renevo.data.Subscription
import com.devatjunaid.renevo.domain.Billing
import com.devatjunaid.renevo.domain.Charge
import com.devatjunaid.renevo.domain.Formats
import com.devatjunaid.renevo.ui.AppViewModel
import com.devatjunaid.renevo.ui.components.EmptyState
import com.devatjunaid.renevo.ui.components.ProgressBar
import com.devatjunaid.renevo.ui.components.SectionCard
import com.devatjunaid.renevo.ui.components.ServiceTile
import com.devatjunaid.renevo.ui.components.StatTile
import com.devatjunaid.renevo.ui.theme.Brand
import com.devatjunaid.renevo.ui.theme.CategoryColors
import com.devatjunaid.renevo.ui.theme.NumberStyle
import com.devatjunaid.renevo.ui.theme.RenevoTheme
import java.time.LocalDate

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenDetail: (Long) -> Unit,
    onAdd: () -> Unit,
    onOpenTrials: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSubs: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    val p by vm.portfolio.collectAsState()
    val settings by vm.settings.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val s = settings ?: return
    val c = RenevoTheme.colors
    val hide = s.hideAmounts
    val cur = s.currency
    val upcoming = remember(p) { p.upcoming(30) }
    val week = remember(p) { p.upcoming(7) }
    val endingTrial = remember(p) {
        p.trials.minByOrNull { it.trialEndEpochDay ?: it.firstPaymentEpochDay }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Formats.shortDate(p.today), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        Text("Overview", style = MaterialTheme.typography.headlineLarge, color = c.onSurface)
                    }
                    RoundIcon(
                        if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        if (hide) "Show amounts" else "Hide amounts",
                    ) { vm.setHideAmounts(!hide) }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        RoundIcon(Icons.Rounded.Notifications, "Trials and alerts", onClick = onOpenTrials)
                        if (p.trials.isNotEmpty()) {
                            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(8.dp).clip(CircleShape).background(c.warn))
                        }
                    }
                }
            }

            item {
                HeroCard(
                    monthly = p.monthlyTotal,
                    yearly = p.yearlyTotal,
                    showYearly = s.showYearly,
                    activeCount = p.active.size,
                    trialCount = p.trials.size,
                    budget = s.budgetMinor,
                    currency = cur,
                    hide = hide,
                    onToggle = { vm.setShowYearly(!s.showYearly) },
                )
            }

            if (loaded && p.subs.isEmpty()) {
                item {
                    EmptyState(
                        title = "Add your first subscription",
                        body = "Start with the ones you know: streaming, music, cloud storage, your gym. Templates make it quick.",
                        illustration = R.drawable.ill_empty,
                    ) {
                        Button(onClick = onAdd, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = c.accent)) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add subscription")
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "Due next 7 days",
                        value = Formats.money(week.filterNot { it.isTrialConversion }.sumOf { it.amountMain }, cur, hide),
                        caption = "${week.size} charge${if (week.size == 1) "" else "s"}",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCalendar,
                    )
                    if (endingTrial != null) {
                        val end = LocalDate.ofEpochDay(endingTrial.trialEndEpochDay ?: endingTrial.firstPaymentEpochDay)
                        val days = Billing.daysUntil(p.today, end).coerceAtLeast(0)
                        StatTile(
                            label = "Trial ending",
                            value = if (days == 0L) "Today" else "$days day${if (days == 1L) "" else "s"}",
                            caption = endingTrial.name,
                            warn = true,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenTrials,
                        )
                    } else {
                        StatTile(
                            label = "Saved by cancelling",
                            value = Formats.money(p.savedTotalMain, cur, hide),
                            caption = "${p.cancelled.size} cancelled",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenTrials,
                        )
                    }
                }
            }

            if (endingTrial != null) {
                val end = LocalDate.ofEpochDay(endingTrial.trialEndEpochDay ?: endingTrial.firstPaymentEpochDay)
                if (Billing.daysUntil(p.today, end) <= 7) {
                    item { TrialAlert(endingTrial, end, p.today, hide, onOpenTrials) }
                }
            }

            item {
                SectionCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Upcoming", style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                        TextButton(onClick = onOpenSubs) { Text("See all") }
                    }
                    if (upcoming.isEmpty()) {
                        Text("Nothing due in the next 30 days.", style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    upcoming.take(5).forEach { ch ->
                        ChargeRow(ch, p.today, hide) { onOpenDetail(ch.sub.id) }
                    }
                }
            }

            item { NativeAdCard(vm.ads, NativeStyle.MEDIUM) }

            if (p.categories.isNotEmpty()) {
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("By category", style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                            TextButton(onClick = onOpenInsights) { Text("Insights") }
                        }
                        Spacer(Modifier.height(6.dp))
                        CategoryBar(p.categories.map { it.category to it.monthlyMain })
                        Spacer(Modifier.height(12.dp))
                        p.categories.take(6).forEach { cat ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(CategoryColors.chart(cat.category)))
                                Spacer(Modifier.width(10.dp))
                                Text("${cat.category} (${cat.count})", style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                                Text(Formats.money(cat.monthlyMain, cur, hide), style = NumberStyle, color = c.onSurface)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            containerColor = c.accent,
            contentColor = androidx.compose.ui.graphics.Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add subscription")
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = RenevoTheme.colors
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.surface)
            .border(1.dp, c.outline, RoundedCornerShape(12.dp)),
    ) {
        Icon(icon, contentDescription = label, tint = c.onSurface)
    }
}

@Composable
private fun HeroCard(
    monthly: Double,
    yearly: Double,
    showYearly: Boolean,
    activeCount: Int,
    trialCount: Int,
    budget: Long,
    currency: String,
    hide: Boolean,
    onToggle: () -> Unit,
) {
    val c = RenevoTheme.colors
    val shown by animateFloatAsState(if (showYearly) yearly.toFloat() else monthly.toFloat(), tween(600), label = "total")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(c.hero, if (c.isDark) Brand.NavyLight else androidx.compose.ui.graphics.Color(0xFF232845))))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (showYearly) "Yearly spend" else "Monthly spend", style = MaterialTheme.typography.bodyMedium, color = c.onHeroMuted, modifier = Modifier.weight(1f))
            Text(
                "$currency · ${if (showYearly) "Yearly" else "Monthly"} ⇄",
                style = MaterialTheme.typography.labelMedium,
                color = c.onHeroMuted,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, c.onHeroMuted.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Text(Formats.money(shown.toDouble(), currency, hide), style = MaterialTheme.typography.displayLarge, color = c.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            (if (showYearly) "≈ ${Formats.money(monthly, currency, hide)} per month" else "≈ ${Formats.money(yearly, currency, hide)} per year") +
                " · $activeCount active" + if (trialCount > 0) " · $trialCount trial" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onHeroMuted,
        )
        if (budget > 0) {
            val frac = (monthly / budget).toFloat()
            ProgressBar(frac.coerceAtMost(1f), if (frac > 1f) Brand.Orange else c.accent, c.onHeroMuted.copy(alpha = 0.2f))
            Row {
                Text("Budget ${Formats.money(budget, currency, hide)}", style = MaterialTheme.typography.bodySmall, color = c.onHeroMuted, modifier = Modifier.weight(1f))
                val diff = budget - monthly
                Text(
                    if (diff >= 0) "${Formats.money(diff, currency, hide)} left" else "${Formats.money(-diff, currency, hide)} over",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (diff >= 0) c.onHeroMuted else Brand.Gold,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TrialAlert(sub: Subscription, end: LocalDate, today: LocalDate, hide: Boolean, onOpen: () -> Unit) {
    val c = RenevoTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.warnContainer)
            .clickable(onClick = onOpen).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Timer, contentDescription = null, tint = c.warnText)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${sub.name} trial ends ${Formats.relativeDay(today, end)}", style = MaterialTheme.typography.titleSmall, color = c.warnText)
            Text(
                "Then ${Formats.money(sub.priceMinor, sub.currency, hide)} ${Billing.cycleLabel(sub.cycleUnit, sub.cycleCount).lowercase()}. Decide now.",
                style = MaterialTheme.typography.bodySmall, color = c.warnText,
            )
        }
    }
}

@Composable
fun ChargeRow(ch: Charge, today: LocalDate, hide: Boolean, onClick: () -> Unit) {
    val c = RenevoTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServiceTile(ch.sub.name, ch.sub.category, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ch.sub.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${Formats.shortDate(ch.date)} · ${Formats.relativeDay(today, ch.date)}" + if (ch.isTrialConversion) " · trial ends" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (ch.isTrialConversion) c.warnText else c.muted,
            )
        }
        Text(Formats.money(ch.amountMinor, ch.sub.currency, hide), style = NumberStyle, color = c.onSurface)
    }
}

@Composable
fun CategoryBar(parts: List<Pair<String, Double>>) {
    val total = parts.sumOf { it.second }.takeIf { it > 0 } ?: return
    Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        parts.forEach { (cat, v) ->
            val w = (v / total).toFloat()
            if (w > 0.005f) Box(Modifier.weight(w).height(12.dp).background(CategoryColors.chart(cat)))
        }
    }
}
