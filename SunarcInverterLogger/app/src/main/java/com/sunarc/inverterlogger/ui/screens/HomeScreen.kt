package com.sunarc.inverterlogger.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.ads.NativeAdSlot
import com.sunarc.inverterlogger.domain.Dashboard
import com.sunarc.inverterlogger.domain.SystemProfile
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.Route
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.components.Pill
import com.sunarc.inverterlogger.ui.components.SecondaryButton
import com.sunarc.inverterlogger.ui.components.SquareIconButton
import com.sunarc.inverterlogger.ui.components.StatTile
import com.sunarc.inverterlogger.ui.components.SunArc
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.theme.DisplayFont
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: AppViewModel, nav: Navigator) {
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val d = dashboard ?: return
    val p = profile ?: return

    ScreenList(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${d.today.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))} · ${p.country.name}",
                        fontSize = 13.sp, color = Palette.Muted
                    )
                    Text(
                        p.systemName + if (p.kwp > 0) " · ${Fmt.number(p.kwp)} kWp" else "",
                        fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Palette.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                SquareIconButton(Icons.Rounded.Tune, "System settings", { nav.push(Route.SETTINGS) })
            }
        }
        item { Rise(0) { SunArcCard(d) } }
        item { Rise(1) { StatsGrid(d, p) } }
        item { Rise(2) { LastScanCard(d, nav) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Type in", { if (vm.startManualEntry()) nav.push(Route.REVIEW) }, Modifier.weight(1f), Icons.Rounded.Edit)
                SecondaryButton("Screenshot", { vm.pendingGallery.value = true; nav.push(Route.SCAN) }, Modifier.weight(1f), Icons.Rounded.Image)
                SecondaryButton("Wi-Fi read", { nav.push(Route.NETWORK) }, Modifier.weight(1f), Icons.Rounded.Wifi)
            }
        }
        item { NativeAdSlot(vm.ads, Modifier.padding(top = 4.dp)) }
    }
}

/** Cards rise into place one after another when the screen opens. */
@Composable
fun Rise(index: Int, content: @Composable () -> Unit) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60L * index)
        t.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    Box(Modifier.graphicsLayer { alpha = t.value; translationY = (1f - t.value) * 40f }) { content() }
}

@Composable
private fun SunArcCard(d: Dashboard) {
    val hm = DateTimeFormatter.ofPattern("HH:mm")
    val desc = buildString {
        append("Sun arc. ")
        d.todayKwh?.let { append("Today ${Fmt.kwh(it)} kilowatt hours. ") }
        d.sunrise?.let { append("Sunrise ${it.format(hm)}. ") }
        d.sunset?.let { append("Sunset ${it.format(hm)}.") }
    }
    Box(
        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp))
            .background(Palette.Panel).border(1.dp, Palette.LineSoft, RoundedCornerShape(28.dp))
    ) {
        SunArc(d.curve, d.dayProgress, Modifier.fillMaxSize().padding(top = 36.dp, bottom = 26.dp), desc)
        Column(Modifier.padding(start = 20.dp, top = 18.dp)) {
            Text("TODAY", fontSize = 12.sp, letterSpacing = 1.sp, color = Palette.Muted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.kwh(d.todayKwh), fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 46.sp, color = Palette.Ink, letterSpacing = (-1.5).sp)
                Spacer(Modifier.width(6.dp))
                Text("kWh", fontSize = 16.sp, color = Palette.InkSoft, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        Column(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 18.dp), horizontalAlignment = Alignment.End) {
            Legend(Palette.SunSoft, "Logged", dashed = false)
            Spacer(Modifier.height(4.dp))
            Legend(Palette.Sky, "Clear-sky", dashed = true)
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(d.sunrise?.format(hm) ?: "--:--", fontFamily = MonoFont, fontSize = 11.sp, color = Palette.Muted)
            Spacer(Modifier.weight(1f))
            val middle = when {
                d.expectedBySunset != null -> "Expected by sunset ≈ ${Fmt.kwh(d.expectedBySunset)} kWh"
                d.clearSkyKwh != null -> "Clear-sky potential ${Fmt.kwh(d.clearSkyKwh)} kWh"
                else -> ""
            }
            Text(middle, fontSize = 12.sp, color = Palette.InkSoft)
            Spacer(Modifier.weight(1f))
            Text(d.sunset?.format(hm) ?: "--:--", fontFamily = MonoFont, fontSize = 11.sp, color = Palette.Muted)
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dashed) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(3) { Box(Modifier.width(3.dp).height(2.dp).background(color)) } }
        } else Box(Modifier.width(14.dp).height(2.dp).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = Palette.Muted)
    }
}

@Composable
private fun StatsGrid(d: Dashboard, p: SystemProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val (pw, pu) = Fmt.power(d.livePowerW)
            StatTile("Live power", pw, pu, Modifier.weight(1f), Icons.Rounded.Bolt)
            val (lv, lu) = Fmt.energy(d.lifetimeKwh)
            StatTile("Lifetime", lv, lu, Modifier.weight(1f), Icons.Rounded.ShowChart, Palette.Sky)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (d.todayMoney != null) StatTile("Saved today", Fmt.money(p.currency, d.todayMoney), null, Modifier.weight(1f), Icons.Rounded.Savings)
            else StatTile("Saved today", "–", null, Modifier.weight(1f), Icons.Rounded.Savings, footnote = "Add prices in settings")
            StatTile("CO₂ avoided", d.co2TodayKg?.let { Fmt.number(it) } ?: "–", "kg", Modifier.weight(1f), Icons.Rounded.Eco, Palette.Sky)
        }
    }
}

@Composable
private fun LastScanCard(d: Dashboard, nav: Navigator) {
    val last = d.lastReading
    if (last == null) {
        SunCard(Modifier.fillMaxWidth(), onClick = { nav.push(Route.SCAN) }, border = Palette.Line) {
            Text("Scan your first reading", fontSize = 16.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
            Text("Tap the amber button and hold the phone in front of the inverter display. Evening scans capture the whole day.", fontSize = 13.sp, color = Palette.InkSoft)
        }
        return
    }
    val zone = java.time.ZoneId.systemDefault()
    val t = last.timestamp.atZone(zone)
    SunCard(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Lcd).border(2.dp, Palette.SurfaceHigh, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(Fmt.kwh(last.todayKwh ?: last.totalKwh), fontFamily = MonoFont, fontSize = 11.sp, color = Palette.LcdInk, fontWeight = FontWeight.SemiBold) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                val day = if (t.toLocalDate() == d.today) "Today" else t.format(DateTimeFormatter.ofPattern("d MMM"))
                Text("Last scan $day ${t.format(DateTimeFormatter.ofPattern("HH:mm"))} · ${last.source.label}", fontSize = 14.sp, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val next = d.sunset?.minusMinutes(20)?.takeIf { it.isAfter(java.time.ZonedDateTime.now(zone)) }
                Text(
                    if (next != null) "Best time for the next scan: ${next.format(DateTimeFormatter.ofPattern("HH:mm"))}, just before sunset" else "Scan again tomorrow evening for a full day",
                    fontSize = 12.sp, color = Palette.Muted
                )
            }
            if (last.faultCode != null) Pill("FAULT ${last.faultCode}", Palette.Sun) else Pill("OK", Palette.Sky)
        }
    }
}
