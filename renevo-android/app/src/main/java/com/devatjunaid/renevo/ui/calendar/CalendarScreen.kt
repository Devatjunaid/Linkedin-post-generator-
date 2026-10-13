package com.devatjunaid.renevo.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devatjunaid.renevo.ads.NativeAdCard
import com.devatjunaid.renevo.ads.NativeStyle
import com.devatjunaid.renevo.domain.Formats
import com.devatjunaid.renevo.ui.AppViewModel
import com.devatjunaid.renevo.ui.components.SectionCard
import com.devatjunaid.renevo.ui.components.StatTile
import com.devatjunaid.renevo.ui.home.ChargeRow
import com.devatjunaid.renevo.ui.theme.CategoryColors
import com.devatjunaid.renevo.ui.theme.RenevoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(vm: AppViewModel, onOpenDetail: (Long) -> Unit) {
    val p by vm.portfolio.collectAsState()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    val c = RenevoTheme.colors
    val hide = s.hideAmounts
    var monthOffset by rememberSaveable { mutableStateOf(0L) }
    var selectedDay by rememberSaveable { mutableStateOf(p.today.toEpochDay()) }
    val month = YearMonth.from(p.today).plusMonths(monthOffset)
    val charges = remember(p, month) { p.charges(month.atDay(1), month.atEndOfMonth()) }
    val byDay = remember(charges) { charges.groupBy { it.date } }
    val selected = LocalDate.ofEpochDay(selectedDay)

    val firstDow: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val lead = ((month.atDay(1).dayOfWeek.value - firstDow.value) + 7) % 7
    val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val padded = cells + List((7 - cells.size % 7) % 7) { null }

    val paid = charges.filter { !it.date.isAfter(p.today) && !it.isTrialConversion }.sumOf { it.amountMain }
    val remaining = charges.filter { it.date.isAfter(p.today) && !it.isTrialConversion }.sumOf { it.amountMain }
    val trialAmount = charges.filter { it.isTrialConversion }.sumOf { it.amountMain }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Calendar", style = MaterialTheme.typography.headlineLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                if (monthOffset != 0L) TextButton(onClick = { monthOffset = 0; selectedDay = p.today.toEpochDay() }) { Text("Today") }
                IconButton(onClick = { monthOffset-- }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month") }
                IconButton(onClick = { monthOffset++ }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month") }
            }
        }
        item {
            SectionCard(padding = PaddingValues(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text(Formats.longMonth(month), style = MaterialTheme.typography.titleLarge, color = c.onSurface, modifier = Modifier.weight(1f))
                    Text("${charges.size} charge${if (charges.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { i ->
                        val dow = firstDow.plus(i.toLong())
                        Text(
                            dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                padded.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                                if (day != null) {
                                    val list = byDay[day].orEmpty()
                                    val isSel = day == selected
                                    val isToday = day == p.today
                                    Column(
                                        Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) c.onSurface else if (isToday) c.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
                                            .clickable { selectedDay = day.toEpochDay() }
                                            .semantics { contentDescription = "${Formats.fullDate(day)}, ${list.size} charges" },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            day.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSel -> c.background
                                                day.isBefore(p.today) -> c.muted
                                                else -> c.onSurface
                                            },
                                        )
                                        Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            list.take(3).forEach { ch ->
                                                val col = if (ch.isTrialConversion) c.warn else if (isSel) c.background else CategoryColors.chart(ch.sub.category)
                                                Box(Modifier.size(6.dp).clip(CircleShape).background(col))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Paid", Formats.money(paid, s.currency, hide), Modifier.weight(1f))
                StatTile("Still to pay", Formats.money(remaining, s.currency, hide), Modifier.weight(1f))
                if (trialAmount > 0) StatTile("If trials convert", "+" + Formats.money(trialAmount, s.currency, hide), Modifier.weight(1f), warn = true)
            }
        }
        item {
            val list = byDay[selected].orEmpty()
            SectionCard(padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    Formats.shortDate(selected) + if (selected == p.today) " · Today" else "",
                    style = MaterialTheme.typography.titleMedium, color = c.onSurface,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                if (list.isEmpty()) {
                    Text("No charges on this day.", style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
                } else list.forEach { ch -> ChargeRow(ch, p.today, hide) { onOpenDetail(ch.sub.id) } }
            }
        }
        item { NativeAdCard(vm.ads, NativeStyle.SMALL) }
    }
}
