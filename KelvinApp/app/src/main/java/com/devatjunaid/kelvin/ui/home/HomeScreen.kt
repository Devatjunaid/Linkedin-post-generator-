package com.devatjunaid.kelvin.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.kelvin.ads.NativeAdCard
import com.devatjunaid.kelvin.data.Settings
import com.devatjunaid.kelvin.domain.LogStats
import com.devatjunaid.kelvin.domain.Moods
import com.devatjunaid.kelvin.domain.PhaseType
import com.devatjunaid.kelvin.domain.Protocol
import com.devatjunaid.kelvin.domain.ProtocolPlanner
import com.devatjunaid.kelvin.domain.Safety
import com.devatjunaid.kelvin.domain.SessionRecord
import com.devatjunaid.kelvin.domain.Units
import com.devatjunaid.kelvin.domain.WeekTotals
import com.devatjunaid.kelvin.ui.components.CircleIconButton
import com.devatjunaid.kelvin.ui.components.GlassCard
import com.devatjunaid.kelvin.ui.components.GoalBar
import com.devatjunaid.kelvin.ui.components.HeatFill
import com.devatjunaid.kelvin.ui.components.IconBadge
import com.devatjunaid.kelvin.ui.components.KIcons
import com.devatjunaid.kelvin.ui.components.LogoMark
import com.devatjunaid.kelvin.ui.components.NoteLine
import com.devatjunaid.kelvin.ui.components.SectionLabel
import com.devatjunaid.kelvin.ui.components.ThermalButton
import com.devatjunaid.kelvin.ui.components.ThermalOrb
import com.devatjunaid.kelvin.ui.components.ThermalRibbon
import com.devatjunaid.kelvin.ui.components.WaterFill
import com.devatjunaid.kelvin.ui.components.pressable
import com.devatjunaid.kelvin.ui.theme.K
import com.devatjunaid.kelvin.ui.theme.KType
import com.google.android.gms.ads.nativead.NativeAd
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    settings: Settings,
    stats: LogStats,
    week: WeekTotals,
    featured: Protocol,
    lastSession: SessionRecord?,
    nativeAd: NativeAd?,
    onQuickStart: (PhaseType) -> Unit,
    onStartProtocol: (Protocol) -> Unit,
    onEditProtocol: (Protocol) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenLog: () -> Unit,
) {
    val now = remember { LocalDateTime.now() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    fun Modifier.stagger(i: Int) = graphicsLayer {
        val local = ((enter.value * 6f) - i * 0.7f).coerceIn(0f, 1f)
        alpha = local
        translationY = (1f - local) * 60f
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().stagger(0), verticalAlignment = Alignment.CenterVertically) {
                LogoMark(size = 44.dp)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(now.format(DateTimeFormatter.ofPattern("EEE · MMM d", Locale.getDefault())).uppercase(), style = KType.Label)
                    Text(greeting(now.hour), style = KType.Title.copy(fontSize = 21.sp))
                }
                CircleIconButton(KIcons.Bell, "Daily reminder settings", onOpenReminders)
            }
        }

        item {
            Box(Modifier.fillMaxWidth().stagger(1), contentAlignment = Alignment.Center) {
                ThermalOrb(size = 236.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DAY STREAK", style = KType.Label)
                        Text("${stats.currentStreak}", style = KType.Hero.copy(fontSize = 66.sp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(K.Ice.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(KIcons.Star, contentDescription = null, tint = K.Ice, modifier = Modifier.size(12.dp))
                            Text(
                                if (stats.bestStreak > 0) "Best ${stats.bestStreak} days" else "Start today",
                                style = KType.Small.copy(color = K.Ice),
                            )
                        }
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.stagger(2).pressable(onClick = onOpenLog, label = "Open log")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel("Weekly dose")
                    Text("${week.sessions} sessions", style = KType.Small)
                }
                val coldGoal = settings.weeklyColdGoalMin * 60
                val heatGoal = settings.weeklyHeatGoalMin * 60
                GoalBar(
                    label = "Cold",
                    valueText = "${Units.clock(week.coldSec)} / ${Units.clock(coldGoal)}",
                    progress = week.coldSec / coldGoal.toFloat(),
                    brush = Brush.horizontalGradient(listOf(K.IceLight, K.Ice, K.IceDeep)),
                )
                GoalBar(
                    label = "Heat",
                    valueText = "${week.heatSec / 60} / ${settings.weeklyHeatGoalMin} min",
                    progress = week.heatSec / heatGoal.toFloat(),
                    brush = Brush.horizontalGradient(listOf(K.Gold, K.Ember, K.EmberDeep)),
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth().stagger(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(
                    title = "Cold Plunge",
                    subtitle = "${Units.temp(settings.lastColdTempC, settings.fahrenheit)} · ${Units.clock(settings.lastColdSec)}",
                    cold = true,
                    onClick = { onQuickStart(PhaseType.COLD) },
                    modifier = Modifier.weight(1f),
                )
                QuickTile(
                    title = "Sauna",
                    subtitle = "${Units.temp(settings.lastHeatTempC, settings.fahrenheit)} · ${Units.clock(settings.lastHeatSec)}",
                    cold = false,
                    onClick = { onQuickStart(PhaseType.HEAT) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (nativeAd != null) {
            item { NativeAdCard(nativeAd) }
        }

        item {
            val phases = remember(featured) { ProtocolPlanner.phases(featured) }
            val totals = remember(phases) { ProtocolPlanner.totals(phases) }
            GlassCard(Modifier.stagger(4)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SectionLabel("Today's protocol")
                        Text(featured.name, style = KType.Heading)
                        Text(
                            "${featured.rounds} rounds · ${Units.duration(totals.totalSec)} · ${if (featured.endOnCold) "ends cold" else "ends warm"}",
                            style = KType.Small,
                        )
                    }
                    CircleIconButton(KIcons.Sliders, "Edit protocol", { onEditProtocol(featured) })
                }
                ThermalRibbon(phases, height = 12.dp, showLabels = false)
                ThermalButton("Start session", onClick = { onStartProtocol(featured) }, modifier = Modifier.fillMaxWidth())
            }
        }

        if (lastSession != null) {
            item {
                GlassCard(Modifier.pressable(onClick = onOpenLog, label = "Open log")) {
                    SectionLabel("Last session")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(
                            if (lastSession.heatSec > 0 && lastSession.coldSec == 0) KIcons.Flame else KIcons.Snowflake,
                            if (lastSession.heatSec > 0 && lastSession.coldSec == 0) K.Ember else K.Ice,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(lastSession.title, style = KType.BodyStrong)
                            Text(
                                buildString {
                                    append(Units.duration(lastSession.totalSec))
                                    if (lastSession.coldSec > 0) append(" · cold ${Units.clock(lastSession.coldSec)}")
                                    if (lastSession.heatSec > 0) append(" · heat ${Units.duration(lastSession.heatSec)}")
                                },
                                style = KType.Small,
                            )
                        }
                        Moods.label(lastSession.mood)?.let { MoodPill(it) }
                    }
                }
            }
        }

        item {
            val tip = Safety.essentials[now.dayOfYear % Safety.essentials.size]
            GlassCard(border = K.Gold.copy(alpha = 0.25f)) {
                SectionLabel("Safety tip", color = K.Gold)
                NoteLine(tip)
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, cold: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = if (cold) K.Ice else K.Ember
    Box(
        modifier
            .height(140.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (cold) Brush.linearGradient(listOf(Color(0xFF122333), Color(0xFF0E1620)))
                else Brush.linearGradient(listOf(Color(0xFF2A1810), Color(0xFF160F0D))),
            )
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(26.dp))
            .pressable(onClick = onClick, label = "Start $title"),
    ) {
        if (cold) WaterFill(level = 0.34f, bubbles = 5) else HeatFill(level = 0.4f)
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            IconBadge(if (cold) KIcons.Snowflake else KIcons.Flame, accent, size = 38.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = KType.Heading.copy(fontSize = 15.sp))
                Text(subtitle, style = KType.Label.copy(color = if (cold) Color(0xFFB9E9F5) else Color(0xFFFFD3BD), fontSize = 12.sp, letterSpacing = 0.sp))
            }
        }
    }
}

@Composable
fun MoodPill(text: String) {
    Text(
        text,
        style = KType.Small.copy(color = K.Ice),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(K.Ice.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Late session?"
}
