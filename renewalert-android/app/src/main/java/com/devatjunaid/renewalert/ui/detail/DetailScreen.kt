package com.devatjunaid.renewalert.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devatjunaid.renewalert.ads.AdPlacement
import com.devatjunaid.renewalert.data.PricePoint
import com.devatjunaid.renewalert.data.SubStatus
import com.devatjunaid.renewalert.data.Usage
import com.devatjunaid.renewalert.domain.Billing
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.ui.AppViewModel
import com.devatjunaid.renewalert.ui.components.Pill
import com.devatjunaid.renewalert.ui.components.SectionCard
import com.devatjunaid.renewalert.ui.components.ServiceTile
import com.devatjunaid.renewalert.ui.components.SettingRow
import com.devatjunaid.renewalert.ui.theme.NumberStyle
import com.devatjunaid.renewalert.ui.theme.RenewAlertTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    vm: AppViewModel,
    id: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    afterAction: (AdPlacement, () -> Unit) -> Unit,
) {
    val p by vm.portfolio.collectAsState()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    val c = RenewAlertTheme.colors
    val sub = p.subs.firstOrNull { it.id == id }
    if (sub == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onBack) { Text("This subscription no longer exists. Go back") }
        }
        return
    }
    val hide = s.hideAmounts
    val history = remember(p, id) { p.historyFor(id) }
    val next = p.nextCharge(sub)
    val paid = remember(p, id) { Billing.paidCharges(sub, p.today).reversed() }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { onEdit(id) }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit")
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ServiceTile(sub.name, sub.category, 64.dp)
                Text(sub.name, style = MaterialTheme.typography.headlineMedium, color = c.onSurface, textAlign = TextAlign.Center)
                Text(
                    "${sub.category} · ${Billing.cycleLabel(sub.cycleUnit, sub.cycleCount)} · ${statusLabel(sub.status)}",
                    style = MaterialTheme.typography.bodyMedium, color = c.muted,
                )
                Text(Formats.money(sub.priceMinor, sub.currency, hide), style = MaterialTheme.typography.displayMedium, color = c.onSurface)
                when {
                    sub.status == SubStatus.TRIAL -> {
                        val end = LocalDate.ofEpochDay(sub.trialEndEpochDay ?: sub.firstPaymentEpochDay)
                        Pill("Free trial ends ${Formats.shortDate(end)} · ${Formats.relativeDay(p.today, end)}", c.warnContainer, c.warnText)
                    }
                    next != null -> Pill("Next charge ${Formats.shortDate(next)} · ${Formats.relativeDay(p.today, next)}", c.accentSoft, c.onAccentSoft)
                    sub.statusChangedEpochDay != null -> Pill(
                        "${statusLabel(sub.status)} since ${Formats.shortDate(LocalDate.ofEpochDay(sub.statusChangedEpochDay))}", c.surfaceAlt, c.muted,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("Per year", Formats.money(Billing.yearlyMinor(sub), sub.currency, hide), Modifier.weight(1f))
                MiniStat("Spent so far", Formats.money(p.spentSoFar(sub), sub.currency, hide), Modifier.weight(1f))
                MiniStat("Since", Formats.monthYear(LocalDate.ofEpochDay(sub.firstPaymentEpochDay)), Modifier.weight(1f))
            }

            if (sub.status == SubStatus.CANCELLED) {
                val saved = Billing.savedSoFar(sub, p.today)
                SectionCard {
                    Text("Saved by cancelling", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    Text(Formats.money(saved, sub.currency, hide), style = MaterialTheme.typography.headlineMedium, color = c.success)
                    Text("${Formats.money(Billing.yearlyMinor(sub), sub.currency, hide)} a year no longer charged.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }

            if (history.size >= 2) PriceHistory(history, sub.currency, hide)

            SectionCard {
                Text("How often do you use it?", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                Spacer(Modifier.height(10.dp))
                val opts = listOf(Usage.DAILY to "Daily", Usage.WEEKLY to "Weekly", Usage.MONTHLY to "Monthly", Usage.RARELY to "Rarely")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opts.forEachIndexed { i, (u, label) ->
                        SegmentedButton(
                            selected = sub.usage == u,
                            onClick = { vm.setUsage(id, if (sub.usage == u) Usage.UNKNOWN else u) },
                            shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                            icon = {},
                        ) { Text(label, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(usageNote(sub.usage, Billing.monthlyMinor(sub), sub.currency, hide), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }

            SectionCard(padding = PaddingValues(0.dp)) {
                SettingRow(
                    "Reminder",
                    icon = Icons.Rounded.Notifications,
                    value = when (sub.reminderDaysBefore) {
                        -1 -> "Off"; 0 -> "On the day"; 1 -> "1 day before"; else -> "${sub.reminderDaysBefore} days before"
                    },
                )
                SettingRow("Paid with", icon = Icons.Rounded.CreditCard, value = sub.paymentMethod.ifBlank { "Not set" }, showDivider = true)
                SettingRow("Billing", icon = Icons.Rounded.EventRepeat, value = Billing.cycleLabel(sub.cycleUnit, sub.cycleCount), showDivider = true)
            }

            if (sub.notes.isNotBlank()) {
                SectionCard {
                    Text("Notes · how to cancel", style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(sub.notes, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                }
            }

            if (paid.isNotEmpty()) {
                Text("Payments", style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                paid.take(12).forEach { d ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(Formats.fullDate(d), style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                        Text(Formats.money(Billing.priceAt(sub, history, d), sub.currency, hide), style = NumberStyle, color = c.onSurface)
                    }
                }
                if (paid.size > 12) Text("+ ${paid.size - 12} earlier payments", style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (sub.status) {
                SubStatus.ACTIVE -> {
                    OutlinedButton(onClick = { vm.setStatus(id, SubStatus.PAUSED) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Pause") }
                    Button(
                        onClick = { vm.setStatus(id, SubStatus.CANCELLED); afterAction(AdPlacement.SUBSCRIPTION_CANCELLED) {} },
                        modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.onSurface, contentColor = c.background),
                    ) { Text("Mark as cancelled") }
                }
                SubStatus.TRIAL -> {
                    OutlinedButton(
                        onClick = { vm.setStatus(id, SubStatus.ACTIVE); afterAction(AdPlacement.TRIAL_DECIDED) {} },
                        modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp),
                    ) { Text("Keep it") }
                    Button(
                        onClick = { vm.setStatus(id, SubStatus.CANCELLED); afterAction(AdPlacement.TRIAL_DECIDED) {} },
                        modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.onSurface, contentColor = c.background),
                    ) { Text("I cancelled it") }
                }
                SubStatus.PAUSED, SubStatus.CANCELLED -> {
                    Button(
                        onClick = { vm.setStatus(id, SubStatus.ACTIVE) },
                        modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                    ) { Text(if (sub.status == SubStatus.PAUSED) "Resume" else "Reactivate") }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${sub.name}?") },
            text = { Text("Its payment history is removed too. To keep the savings record, mark it as cancelled instead.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(id)
                    afterAction(AdPlacement.SUBSCRIPTION_DELETED) {}
                    onBack()
                }) { Text("Delete", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

private fun statusLabel(s: SubStatus) = when (s) {
    SubStatus.ACTIVE -> "Active"; SubStatus.TRIAL -> "Free trial"; SubStatus.PAUSED -> "Paused"; SubStatus.CANCELLED -> "Cancelled"
}

private fun usageNote(u: Usage, monthlyMinor: Double, currency: String, hide: Boolean): String = when (u) {
    Usage.DAILY -> "About ${Formats.money(monthlyMinor / 30.0, currency, hide)} per use. Good value."
    Usage.WEEKLY -> "About ${Formats.money(monthlyMinor / 4.35, currency, hide)} per use."
    Usage.MONTHLY -> "${Formats.money(monthlyMinor, currency, hide)} per use. Worth a second look."
    Usage.RARELY -> "Flagged in Insights as a cancel candidate."
    Usage.UNKNOWN -> "Tell us and we'll show the cost per use and flag poor value."
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = RenewAlertTheme.colors
    SectionCard(modifier, padding = PaddingValues(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.muted)
        Text(value, style = NumberStyle.merge(MaterialTheme.typography.titleSmall), color = c.onSurface, maxLines = 1)
    }
}

@Composable
private fun PriceHistory(history: List<PricePoint>, currency: String, hide: Boolean) {
    val c = RenewAlertTheme.colors
    val first = history.first()
    val last = history.last()
    val change = if (first.priceMinor > 0) (last.priceMinor - first.priceMinor) * 100.0 / first.priceMinor else 0.0
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Price history", style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.weight(1f))
            if (change != 0.0) {
                Pill(
                    (if (change > 0) "▲ " else "▼ ") + "%.1f%%".format(kotlin.math.abs(change)),
                    if (change > 0) c.warnContainer else c.successContainer,
                    if (change > 0) c.warnText else c.successText,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val accent = c.accent
        val dot = c.warn
        val base = c.outline
        Canvas(Modifier.fillMaxWidth().height(70.dp)) {
            val today = LocalDate.now().toEpochDay()
            val minDay = history.first().effectiveEpochDay
            val span = (today - minDay).coerceAtLeast(1)
            val maxP = history.maxOf { it.priceMinor }.toFloat()
            val minP = history.minOf { it.priceMinor }.toFloat()
            fun x(day: Long) = ((day - minDay).toFloat() / span) * size.width
            fun y(pr: Long): Float {
                val range = (maxP - minP).takeIf { it > 0f } ?: 1f
                return size.height - 8f - ((pr - minP) / range) * (size.height - 20f)
            }
            val path = Path()
            history.forEachIndexed { i, pt ->
                val px = x(pt.effectiveEpochDay)
                val py = y(pt.priceMinor)
                if (i == 0) path.moveTo(px, py) else {
                    path.lineTo(px, y(history[i - 1].priceMinor))
                    path.lineTo(px, py)
                }
            }
            path.lineTo(size.width, y(last.priceMinor))
            drawLine(base, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 2f)
            drawPath(path, accent, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            history.drop(1).forEach { pt -> drawCircle(dot, 9f, Offset(x(pt.effectiveEpochDay), y(pt.priceMinor))) }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                "${Formats.monthYear(LocalDate.ofEpochDay(first.effectiveEpochDay))} · ${Formats.money(first.priceMinor, currency, hide)}",
                style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.weight(1f),
            )
            Text(
                "${Formats.monthYear(LocalDate.ofEpochDay(last.effectiveEpochDay))} · ${Formats.money(last.priceMinor, currency, hide)}",
                style = MaterialTheme.typography.bodySmall, color = c.muted,
            )
        }
    }
}
