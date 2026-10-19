package com.dailycash.tracker.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailycash.tracker.ads.NativeAdCard
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.data.Stats
import com.dailycash.tracker.data.isSpend
import com.dailycash.tracker.ui.MainViewModel
import com.dailycash.tracker.ui.components.AddWalletNote
import com.dailycash.tracker.ui.components.Banknote
import com.dailycash.tracker.ui.components.EmptyState
import com.dailycash.tracker.ui.components.EntryRow
import com.dailycash.tracker.ui.components.ExitDialog
import com.dailycash.tracker.ui.components.PulseDot
import com.dailycash.tracker.ui.components.RollingText
import com.dailycash.tracker.ui.components.SectionHeader
import com.dailycash.tracker.ui.theme.Mono
import com.dailycash.tracker.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAdd: (mode: String, currency: String?) -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    onExit: () -> Unit,
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val home by vm.homeCurrency.collectAsStateWithLifecycle()
    val rates by vm.rates.collectAsStateWithLifecycle()
    val place = vm.place

    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val walletCodes = balances.map { it.currency }
    val selected = picked?.takeIf { it in walletCodes }
        ?: walletCodes.firstOrNull { it == place.currency }
        ?: walletCodes.firstOrNull()
        ?: place.currency
    val balance = balances.firstOrNull { it.currency == selected }?.balance ?: 0L

    val today = LocalDate.now()
    val todays = remember(entries) { entries.filter { Stats.dateOf(it) == today } }
    val todaySpent = remember(todays, home, rates) { Stats.totalHome(todays.filter { it.isSpend() }, home, rates) }
    val runway = remember(entries, selected, balance) { Stats.runwayDays(entries, selected, balance) }
    val avg = remember(entries, selected) { Stats.avgDailySpend(entries, selected) }

    var showExit by remember { mutableStateOf(false) }
    BackHandler { showExit = true }
    if (showExit) {
        ExitDialog(
            todaySpent = if (todaySpent > 0) Money.format(todaySpent, home) else null,
            onStay = { showExit = false },
            onExit = onExit,
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(today),
                        color = Palette.Muted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Palette.Surface)
                            .border(1.dp, Palette.Line, RoundedCornerShape(50))
                            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PulseDot()
                        Text("You're in ${place.countryName}", color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            place.currency,
                            color = Palette.Ink,
                            fontFamily = Mono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Palette.Mint).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings and home currency", tint = Palette.Text)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("CASH IN POCKET · $selected", color = Palette.Muted, fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                RollingText(
                    Money.format(balance, selected),
                    style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
                )
                if (selected != home) {
                    val homeValue = rates.convert(balance, selected, home)
                    val estimate = if (rates.live) "" else " (offline estimate)"
                    Text(
                        if (homeValue != null) "≈ ${Money.format(homeValue, home)} in $home$estimate" else "No rate for $selected yet",
                        color = Palette.Muted,
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
                RunwayCard(runway, balance, avg, selected)
            }
        }

        item {
            Column {
            SectionHeader("Your wallets", Modifier.padding(horizontal = 20.dp), "+ Add cash") { onAdd("in", null) }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(balances, key = { it.currency }) { b ->
                    Banknote(
                        code = b.currency,
                        balanceMinor = b.balance,
                        caption = when (b.currency) {
                            place.currency -> "Local · ${place.countryName}"
                            home -> "Home currency"
                            else -> Money.displayName(b.currency)
                        },
                        selected = b.currency == selected,
                        onClick = { picked = b.currency },
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "add") { AddWalletNote(onClick = { onAdd("in", null) }) }
            }
            }
        }

        item { NativeAdCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }

        item {
            val todayTitle = if (todaySpent > 0) "Today · −${Money.format(todaySpent, home)}" else "Today"
            SectionHeader(
                todayTitle,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                "See all",
                onHistory,
            )
        }
        if (todays.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.Savings,
                    "Nothing logged today",
                    "Tap + and count the notes you hand over.\nIt takes about 3 seconds.",
                )
            }
        } else {
            items(todays, key = { it.id }) { e ->
                EntryRow(e, home, rates, Modifier.padding(horizontal = 20.dp).animateItem())
            }
        }
        item {
            Row(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onAdd("out", selected) }
                    .background(Palette.Surface2)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Public, contentDescription = null, tint = Palette.Sky)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Spending in $selected? Log it in seconds",
                    color = Palette.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RunwayCard(days: Double?, balance: Long, avg: Long, code: String) {
    val fraction = ((days ?: if (balance > 0) 1.0 else 0.0) / 7.0).coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(fraction, tween(900), label = "runway")
    val tone = when {
        days == null -> if (balance > 0) Palette.Sky else Palette.Muted
        days < 2 -> Palette.Coral
        days < 5 -> Palette.Amber
        else -> Palette.Mint
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cash runway", color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                when {
                    balance < 0 -> "Overspent"
                    days == null -> if (balance > 0) "Not enough data" else "Empty"
                    days >= 60 -> "60+ days"
                    else -> String.format(Locale.getDefault(), "%.1f days left", days)
                },
                color = tone,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.Raised)) {
            Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(tone))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                balance < 0 -> "You logged more spending than cash added. Add the cash you withdrew to fix the balance."
                balance == 0L -> "Add the cash you're carrying to start tracking this wallet."
                days == null -> "Log a few spends and we'll tell you how long this cash lasts."
                else -> "At about ${Money.format(avg, code)} a day, this cash lasts until " +
                    LocalDate.now().plusDays(days.toLong()).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())) + "."
            },
            color = Palette.Muted,
            fontSize = 12.sp,
        )
    }
}
