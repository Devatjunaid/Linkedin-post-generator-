package com.devatjunaid.kelvin.ui.log

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.kelvin.ads.NativeAdCard
import com.devatjunaid.kelvin.data.Settings
import com.devatjunaid.kelvin.domain.DayType
import com.devatjunaid.kelvin.domain.LogStats
import com.devatjunaid.kelvin.domain.Moods
import com.devatjunaid.kelvin.domain.SessionKind
import com.devatjunaid.kelvin.domain.SessionRecord
import com.devatjunaid.kelvin.domain.Stats
import com.devatjunaid.kelvin.domain.Units
import com.devatjunaid.kelvin.domain.WeekTotals
import com.devatjunaid.kelvin.ui.components.CircleIconButton
import com.devatjunaid.kelvin.ui.components.GlassCard
import com.devatjunaid.kelvin.ui.components.IconBadge
import com.devatjunaid.kelvin.ui.components.KIcons
import com.devatjunaid.kelvin.ui.components.SectionLabel
import com.devatjunaid.kelvin.ui.components.StatTile
import com.devatjunaid.kelvin.ui.components.pressable
import com.devatjunaid.kelvin.ui.home.MoodPill
import com.devatjunaid.kelvin.ui.theme.K
import com.devatjunaid.kelvin.ui.theme.KType
import com.google.android.gms.ads.nativead.NativeAd
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LogScreen(
    sessions: List<SessionRecord>,
    stats: LogStats,
    week: WeekTotals,
    settings: Settings,
    nativeAd: NativeAd?,
    onDelete: (SessionRecord) -> Unit,
    onAddManual: () -> Unit,
    onExport: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var pendingDelete by remember { mutableStateOf<SessionRecord?>(null) }
    val dayTypes = remember(sessions) { Stats.dayTypes(sessions, zone) }
    val shown = remember(sessions, selectedDay) {
        selectedDay?.let { d -> sessions.filter { Stats.dateOf(it.startedAt, zone) == d } } ?: sessions.take(60)
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Log", style = KType.Title, modifier = Modifier.weight(1f))
                CircleIconButton(KIcons.Plus, "Add a session manually", onAddManual)
                CircleIconButton(KIcons.Export, "Export log as CSV", onExport)
            }
        }

        item { WeeklyDoseCard(week, settings) }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(KIcons.ChevronLeft, "Previous month", { month = month.minusMonths(1); selectedDay = null }, size = 38.dp)
                    AnimatedContent(
                        targetState = month,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { it * dir / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 2 } + fadeOut())
                        },
                        modifier = Modifier.weight(1f),
                        label = "month",
                    ) { m ->
                        Text(
                            "${m.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${m.year}",
                            style = KType.Heading,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    CircleIconButton(
                        KIcons.ChevronRight,
                        "Next month",
                        { if (month < YearMonth.from(today)) { month = month.plusMonths(1); selectedDay = null } },
                        size = 38.dp,
                    )
                }
                CalendarGrid(month, today, dayTypes, selectedDay) { d -> selectedDay = if (selectedDay == d) null else d }
                Legend()
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Current streak", "${stats.currentStreak}d", Modifier.weight(1f), K.Ice)
                    StatTile("Best streak", "${stats.bestStreak}d", Modifier.weight(1f), K.Gold)
                    StatTile("Sessions", "${stats.totalSessions}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Most cold", Units.clock(stats.mostColdInOneSessionSec), Modifier.weight(1f), K.Ice)
                    StatTile("Coldest", Units.temp(stats.coldestTempC, settings.fahrenheit), Modifier.weight(1f), K.Ice)
                    StatTile("All heat", Units.duration(stats.totalHeatSec), Modifier.weight(1f), K.Ember)
                }
            }
        }

        if (nativeAd != null) item { NativeAdCard(nativeAd) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(selectedDay?.let { "Sessions · " + it.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())) } ?: "Recent sessions")
                if (selectedDay != null) {
                    Text("Show all", style = KType.Small.copy(color = K.Ice), modifier = Modifier.pressable(onClick = { selectedDay = null }))
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        if (sessions.isEmpty()) "No sessions yet. Start a plunge or sauna from Today — it's logged automatically."
                        else "Nothing logged on this day.",
                        style = KType.Body.copy(color = K.Muted),
                    )
                }
            }
        } else {
            items(shown, key = { it.id }) { s ->
                SessionRow(s, settings.fahrenheit, zone, onDelete = { pendingDelete = s })
            }
        }
    }

    pendingDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = K.Surface2,
            title = { Text("Delete this session?", style = KType.Heading) },
            text = { Text("${s.title} · ${Units.duration(s.totalSec)}. This can't be undone.", style = KType.Body) },
            confirmButton = { TextButton(onClick = { onDelete(s); pendingDelete = null }) { Text("Delete", style = KType.BodyStrong.copy(color = K.EmberDeep)) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", style = KType.BodyStrong) } },
        )
    }
}

@Composable
private fun WeeklyDoseCard(week: WeekTotals, settings: Settings) {
    val coldGoal = settings.weeklyColdGoalMin * 60f
    val heatGoal = settings.weeklyHeatGoalMin * 60f
    val coldP = (week.coldSec / coldGoal).coerceIn(0f, 1f)
    val heatP = (week.heatSec / heatGoal).coerceIn(0f, 1f)
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier
                    .size(124.dp)
                    .semantics { contentDescription = "Heat ${week.heatSec / 60} of ${settings.weeklyHeatGoalMin} minutes, cold ${Units.clock(week.coldSec)} of ${settings.weeklyColdGoalMin} minutes" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    fun ring(radius: Float, progress: Float, colors: List<Color>) {
                        val tl = Offset(center.x - radius, center.y - radius)
                        val sz = Size(radius * 2, radius * 2)
                        drawArc(colors.first().copy(alpha = 0.14f), -90f, 360f, false, tl, sz, style = Stroke(stroke))
                        drawArc(Brush.sweepGradient(colors, center), -90f, 360f * progress * draw.value, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    ring(size.minDimension / 2f - stroke / 2f, heatP, listOf(K.Gold, K.Ember, K.EmberDeep, K.Gold))
                    ring(size.minDimension / 2f - stroke * 2f, coldP, listOf(K.IceLight, K.Ice, K.IceDeep, K.IceLight))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(((coldP + heatP) / 2f) * 100).toInt()}%", style = KType.Heading.copy(fontSize = 20.sp))
                    Text("of goal", style = KType.Small.copy(fontSize = 10.sp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("This week")
                Column {
                    Text("Heat", style = KType.Small)
                    Text("${week.heatSec / 60} / ${settings.weeklyHeatGoalMin} min", style = KType.Mono.copy(color = K.Ember))
                }
                Column {
                    Text("Cold", style = KType.Small)
                    Text("${Units.clock(week.coldSec)} / ${settings.weeklyColdGoalMin}:00", style = KType.Mono.copy(color = K.Ice))
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    dayTypes: Map<LocalDate, DayType>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1 // Monday-first
    val days = month.lengthOfMonth()
    val cells = (List(leading) { null } + (1..days).map { month.atDay(it) }).let { list ->
        list + List((7 - list.size % 7) % 7) { null }
    }
    val pop = remember(month) { Animatable(0f) }
    LaunchedEffect(month) { pop.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = KType.Label, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1.15f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val local = ((pop.value * 1.6f) - date.dayOfMonth / 40f).coerceIn(0f, 1f)
                            DayCell(date, today, dayTypes[date] ?: DayType.NONE, date == selected, local) { onSelect(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, type: DayType, selected: Boolean, appear: Float, onClick: () -> Unit) {
    val future = date.isAfter(today)
    val bg: Brush = when (type) {
        DayType.CONTRAST -> Brush.linearGradient(0f to K.Ice, 0.5f to K.Ice, 0.5f to K.Ember, 1f to K.Ember)
        DayType.COLD -> Brush.linearGradient(listOf(K.Ice, K.Ice))
        DayType.HEAT -> Brush.linearGradient(listOf(K.Ember, K.Ember))
        DayType.NONE -> Brush.linearGradient(listOf(Color(0x0DFFFFFF), Color(0x0DFFFFFF)))
    }
    val fg = when {
        type != DayType.NONE -> K.Ink
        future -> Color(0xFF4A5763)
        else -> K.Faint
    }
    val ringColor = when {
        selected -> K.Gold
        date == today -> K.Text
        else -> Color.Transparent
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = appear
                scaleX = 0.5f + 0.5f * appear
                scaleY = 0.5f + 0.5f * appear
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (future) Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)) else bg)
            .border(if (ringColor == Color.Transparent) 0.dp else 2.dp, ringColor, RoundedCornerShape(10.dp))
            .pressable(onClick = onClick, enabled = !future, label = date.toString()),
        contentAlignment = Alignment.Center,
    ) {
        Text("${date.dayOfMonth}", style = KType.Label.copy(color = fg, fontSize = 11.sp, letterSpacing = 0.sp))
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(Brush.linearGradient(listOf(K.Ice, K.Ice)), "Cold")
        LegendDot(Brush.linearGradient(listOf(K.Ember, K.Ember)), "Heat")
        LegendDot(Brush.linearGradient(0f to K.Ice, 0.5f to K.Ice, 0.5f to K.Ember, 1f to K.Ember), "Contrast")
    }
}

@Composable
private fun LegendDot(brush: Brush, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(brush))
        Text(label, style = KType.Small)
    }
}

@Composable
private fun SessionRow(s: SessionRecord, fahrenheit: Boolean, zone: ZoneId, onDelete: () -> Unit) {
    val time = remember(s.startedAt) {
        DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.getDefault()).format(Instant.ofEpochMilli(s.startedAt).atZone(zone))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (s.kind) {
            SessionKind.COLD -> IconBadge(KIcons.Snowflake, K.Ice)
            SessionKind.HEAT -> IconBadge(KIcons.Flame, K.Ember)
            SessionKind.CONTRAST -> IconBadge(KIcons.Thermal, K.Violet)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(s.title, style = KType.BodyStrong)
            Text(time, style = KType.Label.copy(letterSpacing = 0.sp, fontSize = 11.sp))
            Text(
                buildString {
                    append(Units.duration(s.totalSec))
                    if (s.coldSec > 0) {
                        append(" · cold ${Units.clock(s.coldSec)}")
                        s.coldTempC?.let { append(" @ ${Units.temp(it, fahrenheit)}") }
                    }
                    if (s.heatSec > 0) {
                        append(" · heat ${Units.duration(s.heatSec)}")
                        s.heatTempC?.let { append(" @ ${Units.temp(it, fahrenheit)}") }
                    }
                },
                style = KType.Small,
            )
            if (s.note.isNotBlank()) Text("“${s.note}”", style = KType.Small.copy(color = Color(0xFFC9D3DC)))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Moods.label(s.mood)?.let { MoodPill(it) }
            CircleIconButton(KIcons.Trash, "Delete session", onDelete, size = 36.dp, tint = K.Muted)
        }
    }
}
