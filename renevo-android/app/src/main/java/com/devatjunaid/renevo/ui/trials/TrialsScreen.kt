package com.devatjunaid.renevo.ui.trials

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devatjunaid.renevo.R
import com.devatjunaid.renevo.ads.AdPlacement
import com.devatjunaid.renevo.ads.NativeAdCard
import com.devatjunaid.renevo.ads.NativeStyle
import com.devatjunaid.renevo.data.SubStatus
import com.devatjunaid.renevo.data.Subscription
import com.devatjunaid.renevo.domain.Billing
import com.devatjunaid.renevo.domain.Formats
import com.devatjunaid.renevo.ui.AppViewModel
import com.devatjunaid.renevo.ui.components.EmptyState
import com.devatjunaid.renevo.ui.components.ProgressBar
import com.devatjunaid.renevo.ui.components.SectionLabel
import com.devatjunaid.renevo.ui.components.ServiceTile
import com.devatjunaid.renevo.ui.theme.Brand
import com.devatjunaid.renevo.ui.theme.RenevoTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TrialsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    afterAction: (AdPlacement, () -> Unit) -> Unit,
) {
    val p by vm.portfolio.collectAsState()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    val c = RenevoTheme.colors
    val hide = s.hideAmounts
    val trials = p.trials.sortedBy { it.trialEndEpochDay ?: it.firstPaymentEpochDay }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Trials & savings", style = MaterialTheme.typography.headlineSmall, color = c.onSurface)
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionLabel("Active trials") }
            if (trials.isEmpty()) {
                item {
                    Text(
                        "No free trials running. When you add one, Renevo warns you 3 days, 1 day and the morning before it turns paid.",
                        style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(trials, key = { it.id }) { t ->
                TrialCard(t, p.today, hide, onOpen = { onOpenDetail(t.id) },
                    onKeep = { vm.setStatus(t.id, SubStatus.ACTIVE); afterAction(AdPlacement.TRIAL_DECIDED) {} },
                    onCancelled = { vm.setStatus(t.id, SubStatus.CANCELLED); afterAction(AdPlacement.TRIAL_DECIDED) {} })
            }

            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Brand.Navy, Brand.NavyLight))).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Saved by cancelling", style = MaterialTheme.typography.bodyMedium, color = c.onHeroMuted)
                    Text(Formats.money(p.savedTotalMain, s.currency, hide), style = MaterialTheme.typography.displayMedium, color = c.onHero)
                    Text(
                        "${Formats.money(p.savedYearlyMain, s.currency, hide)} a year no longer charged",
                        style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color(0xFF9FE0B9),
                    )
                }
            }

            item { NativeAdCard(vm.ads, NativeStyle.SMALL) }

            item { SectionLabel("Cancelled") }
            if (p.cancelled.isEmpty()) {
                item { EmptyState("Nothing cancelled yet", "Mark a subscription as cancelled to track what you save.", R.drawable.ill_empty) }
            }
            items(p.cancelled.sortedByDescending { it.statusChangedEpochDay ?: 0L }, key = { "c${it.id}" }) { sub ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface)
                        .border(1.dp, c.outline, RoundedCornerShape(16.dp)).clickable { onOpenDetail(sub.id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ServiceTile(sub.name, sub.category, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sub.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Cancelled " + (sub.statusChangedEpochDay?.let { Formats.dayMonth(LocalDate.ofEpochDay(it)) } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = c.muted,
                        )
                    }
                    Text(Formats.money(p.savedSoFarMain(sub), s.currency, hide), style = MaterialTheme.typography.labelLarge, color = c.success)
                }
            }
            item {
                Text(
                    "Cancelled subscriptions stay here so you can see what you saved. Delete them from their detail page.",
                    style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TrialCard(
    t: Subscription,
    today: LocalDate,
    hide: Boolean,
    onOpen: () -> Unit,
    onKeep: () -> Unit,
    onCancelled: () -> Unit,
) {
    val c = RenevoTheme.colors
    val end = LocalDate.ofEpochDay(t.trialEndEpochDay ?: t.firstPaymentEpochDay)
    val start = Instant.ofEpochMilli(t.createdAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val total = Billing.daysUntil(start, end).coerceAtLeast(1)
    val used = Billing.daysUntil(start, today).coerceIn(0, total)
    val left = Billing.daysUntil(today, end).coerceAtLeast(0)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface)
            .border(1.dp, c.warn.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            ServiceTile(t.name, t.category, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                Text(
                    "Then ${Formats.money(t.priceMinor, t.currency, hide)} ${Billing.cycleLabel(t.cycleUnit, t.cycleCount).lowercase()}" +
                        if (t.paymentMethod.isNotBlank()) " · ${t.paymentMethod}" else "",
                    style = MaterialTheme.typography.bodySmall, color = c.muted,
                )
            }
        }
        Row {
            Text(if (left == 0L) "Ends today" else "$left day${if (left == 1L) "" else "s"} left", style = MaterialTheme.typography.labelLarge, color = c.warnText, modifier = Modifier.weight(1f))
            Text("Ends ${Formats.shortDate(end)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        ProgressBar(used.toFloat() / total, c.warn, c.warnContainer, height = 10.dp)
        listOf(3L to "3 days before", 1L to "1 day before", 0L to "Morning it ends").forEach { (d, label) ->
            val alertDay = end.minusDays(d)
            val sent = !alertDay.isAfter(today)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (sent) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule, contentDescription = null,
                    tint = if (sent) c.success else c.muted, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("$label · ${Formats.shortDate(alertDay)}", style = MaterialTheme.typography.bodySmall, color = c.onSurface)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onKeep, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("Keep it") }
            Button(
                onClick = onCancelled, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.onSurface, contentColor = c.background),
            ) { Text("I cancelled it") }
        }
    }
}
