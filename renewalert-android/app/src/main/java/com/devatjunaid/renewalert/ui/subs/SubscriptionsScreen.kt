package com.devatjunaid.renewalert.ui.subs

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devatjunaid.renewalert.R
import com.devatjunaid.renewalert.ads.NativeAdCard
import com.devatjunaid.renewalert.ads.NativeStyle
import com.devatjunaid.renewalert.data.CycleUnit
import com.devatjunaid.renewalert.data.SubStatus
import com.devatjunaid.renewalert.data.Subscription
import com.devatjunaid.renewalert.domain.Billing
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.domain.Portfolio
import com.devatjunaid.renewalert.ui.AppViewModel
import com.devatjunaid.renewalert.ui.components.EmptyState
import com.devatjunaid.renewalert.ui.components.Pill
import com.devatjunaid.renewalert.ui.components.ServiceTile
import com.devatjunaid.renewalert.ui.theme.NumberStyle
import com.devatjunaid.renewalert.ui.theme.RenewAlertTheme
import java.time.LocalDate

private enum class Filter(val label: String) { ALL("Active"), MONTHLY("Monthly"), YEARLY("Yearly"), TRIALS("Trials"), PAUSED("Paused"), CANCELLED("Cancelled") }
private enum class Sort(val label: String) { NEXT("Next due"), PRICE("Highest cost"), NAME("Name") }

@Composable
fun SubscriptionsScreen(vm: AppViewModel, onOpenDetail: (Long) -> Unit, onAdd: () -> Unit) {
    val p by vm.portfolio.collectAsState()
    val settings by vm.settings.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val s = settings ?: return
    val c = RenewAlertTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var sort by rememberSaveable { mutableStateOf(Sort.NEXT) }
    var sortOpen by remember { mutableStateOf(false) }

    val list = remember(p, query, filter, sort) { filterAndSort(p, query, filter, sort) }
    val monthly = list.filter { it.status == SubStatus.ACTIVE }.sumOf { p.monthlyMain(it) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Subscriptions", style = MaterialTheme.typography.headlineLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { sortOpen = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(sort.label)
                        }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            Sort.entries.forEach { o ->
                                DropdownMenuItem(text = { Text(o.label) }, onClick = { sort = o; sortOpen = false })
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search name, category, card…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                        unfocusedBorderColor = c.outline,
                    ),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Filter.entries) { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.onSurface,
                                selectedLabelColor = c.background,
                                containerColor = c.surface,
                            ),
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text("${list.size} subscription${if (list.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.weight(1f))
                    if (filter != Filter.CANCELLED && filter != Filter.PAUSED && filter != Filter.TRIALS) {
                        Text("${Formats.money(monthly, s.currency, s.hideAmounts)} / month", style = MaterialTheme.typography.labelLarge, color = c.onSurface)
                    }
                }
            }
            if (loaded && list.isEmpty()) {
                item {
                    EmptyState(
                        title = if (query.isNotBlank()) "No matches" else "Nothing here yet",
                        body = if (query.isNotBlank()) "Try another name or category." else "Tap + to add a subscription.",
                        illustration = R.drawable.ill_empty,
                    )
                }
            }
            itemsIndexed(list, key = { _, it -> it.id }) { index, sub ->
                SubscriptionRow(sub, p, s.hideAmounts) { onOpenDetail(sub.id) }
                // One native ad after the 4th row, kept away from the FAB and nav bar.
                if (index == 3 && list.size > 4) {
                    Spacer(Modifier.padding(top = 10.dp))
                    NativeAdCard(vm.ads, NativeStyle.SMALL)
                }
            }
            if (list.size in 1..4) {
                item { NativeAdCard(vm.ads, NativeStyle.SMALL) }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            containerColor = c.accent,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Add subscription") }
    }
}

private fun filterAndSort(p: Portfolio, query: String, filter: Filter, sort: Sort): List<Subscription> {
    val q = query.trim()
    val filtered = p.subs.filter { s ->
        val match = q.isEmpty() || s.name.contains(q, true) || s.category.contains(q, true) ||
            s.paymentMethod.contains(q, true) || s.notes.contains(q, true)
        val inFilter = when (filter) {
            Filter.ALL -> s.status == SubStatus.ACTIVE || s.status == SubStatus.TRIAL
            Filter.MONTHLY -> s.status == SubStatus.ACTIVE && s.cycleUnit != CycleUnit.YEAR
            Filter.YEARLY -> s.status == SubStatus.ACTIVE && s.cycleUnit == CycleUnit.YEAR
            Filter.TRIALS -> s.status == SubStatus.TRIAL
            Filter.PAUSED -> s.status == SubStatus.PAUSED
            Filter.CANCELLED -> s.status == SubStatus.CANCELLED
        }
        match && inFilter
    }
    return when (sort) {
        Sort.NEXT -> filtered.sortedBy { p.nextCharge(it)?.toEpochDay() ?: Long.MAX_VALUE }
        Sort.PRICE -> filtered.sortedByDescending { p.monthlyMain(it) }
        Sort.NAME -> filtered.sortedBy { it.name.lowercase() }
    }
}

@Composable
fun SubscriptionRow(sub: Subscription, p: Portfolio, hide: Boolean, onClick: () -> Unit) {
    val c = RenewAlertTheme.colors
    val next = p.nextCharge(sub)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServiceTile(sub.name, sub.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sub.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = buildList {
                add(sub.category)
                if (sub.paymentMethod.isNotBlank()) add(sub.paymentMethod)
            }.joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (sub.status == SubStatus.TRIAL) "Free" else Formats.money(sub.priceMinor, sub.currency, hide) + Billing.perCycleSuffix(sub.cycleUnit, sub.cycleCount),
                style = NumberStyle, color = c.onSurface,
            )
            Spacer(Modifier.padding(top = 3.dp))
            StatusBadge(sub, next, p.today)
        }
    }
}

@Composable
fun StatusBadge(sub: Subscription, next: LocalDate?, today: LocalDate) {
    val c = RenewAlertTheme.colors
    when (sub.status) {
        SubStatus.TRIAL -> {
            val end = LocalDate.ofEpochDay(sub.trialEndEpochDay ?: sub.firstPaymentEpochDay)
            Pill("Ends ${Formats.relativeDay(today, end)}", c.warnContainer, c.warnText)
        }
        SubStatus.PAUSED -> Pill("Paused", c.surfaceAlt, c.muted)
        SubStatus.CANCELLED -> Pill("Cancelled", c.surfaceAlt, c.muted)
        SubStatus.ACTIVE -> {
            if (next == null) return
            val soon = Billing.daysUntil(today, next) <= 7
            if (soon) Pill(Formats.dayMonth(next), c.accentSoft, c.onAccentSoft)
            else Text(Formats.dayMonth(next), style = MaterialTheme.typography.labelMedium, color = c.muted)
        }
    }
}
