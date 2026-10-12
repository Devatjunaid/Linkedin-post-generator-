package app.gigometer.mileage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.Format
import app.gigometer.mileage.data.Category
import app.gigometer.mileage.data.GeoPoint
import app.gigometer.mileage.data.IrsRates
import app.gigometer.mileage.data.RatePeriod
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.ui.theme.LocalPalette
import app.gigometer.mileage.ui.theme.numberStyle
import kotlin.math.cos
import kotlin.math.max

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.card,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(padding.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text.uppercase(), color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
        if (trailing != null) Text(trailing, color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatTile(label: String, value: String, sub: String, modifier: Modifier = Modifier, valueColor: Color? = null, onClick: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(p.card)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(label.uppercase(), color = p.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, maxLines = 1)
        Text(value, style = numberStyle(22), color = valueColor ?: p.ink, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        Text(sub, color = p.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One trip in a list. */
@Composable
fun TripRow(trip: Trip, rates: List<RatePeriod>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val (icon, tint, bg) = when (trip.category) {
        Category.BUSINESS -> Triple(Icons.Filled.DirectionsCar, p.accent, p.accentSoft)
        Category.PERSONAL -> Triple(Icons.Filled.Person, p.muted, p.line)
        else -> Triple(Icons.AutoMirrored.Filled.HelpOutline, p.amberText, p.amberSoft)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(routeTitle(trip), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = p.ink)
            Text(tripMeta(trip), color = p.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${Format.miles(trip.miles)} mi", style = numberStyle(16), color = p.ink)
            val (v, c) = when (trip.category) {
                Category.BUSINESS -> Format.money(IrsRates.deduction(trip, rates)) to p.accent
                Category.PERSONAL -> "Personal" to p.muted
                else -> "Review" to p.amberText
            }
            Text(v, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = c)
        }
    }
}

fun routeTitle(t: Trip): String {
    val a = t.startAddress?.substringBefore(",")
    val b = t.endAddress?.substringBefore(",")
    return when {
        a != null && b != null -> if (a == b) a else "$a → $b"
        b != null -> "→ $b"
        a != null -> "$a →"
        t.isManual -> "Manual entry"
        else -> "Trip at ${Format.time(t.startTime)}"
    }
}

fun tripMeta(t: Trip): String = buildList {
    add(Format.time(t.startTime))
    if (!t.isManual) add(Format.duration(t.durationMs))
    when {
        t.platform != null -> add(t.platform)
        t.category == Category.UNREVIEWED -> add("Needs review")
        t.isManual -> add("Manual")
    }
}.joinToString(" · ")

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onDark: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    val bg = when {
        selected && onDark -> p.bright
        selected -> p.ink
        onDark -> Color.Transparent
        else -> p.card
    }
    val fg = when {
        selected && onDark -> Color(0xFF14211B)
        selected -> p.ground
        onDark -> p.heroInk
        else -> p.ink
    }
    val borderColor = if (selected) bg else if (onDark) Color(0xFF3A4F44) else p.line
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, divider: Boolean = true) {
    val p = LocalPalette.current
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { onChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                if (subtitle != null) Text(subtitle, color = p.muted, fontSize = 12.sp)
            }
            Switch(
                checked = checked, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = Color.White, uncheckedTrackColor = p.neutralBar, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
            )
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 1.dp, max = 1.dp).background(p.line))
    }
}

@Composable
fun NavRow(title: String, subtitle: String?, value: String? = null, divider: Boolean = true, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                if (subtitle != null) Text(subtitle, color = p.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (value != null) Text(value, color = p.accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 1.dp, max = 1.dp).background(p.line))
    }
}

/**
 * Draws a GPS route on a stylised street-grid background — no map tiles, no API key,
 * no network. Coordinates are projected with an equirectangular approximation.
 */
@Composable
fun RouteCanvas(points: List<GeoPoint>, modifier: Modifier = Modifier, lineColor: Color, gridColor: Color, background: Color, live: Boolean = false) {
    Canvas(modifier.background(background)) {
        val step = 64.dp.toPx()
        var x = -step / 3
        while (x < size.width) { drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 8.dp.toPx()); x += step }
        var y = step / 2
        while (y < size.height) { drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 8.dp.toPx()); y += step }
        if (points.isEmpty()) return@Canvas
        val lat0 = points.map { it.lat }.average()
        val k = cos(Math.toRadians(lat0))
        val xs = points.map { it.lng * k }
        val ys = points.map { it.lat }
        val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
        val pad = 36.dp.toPx()
        val spanX = max(maxX - minX, 1e-5); val spanY = max(maxY - minY, 1e-5)
        val scale = minOf((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY).toFloat()
        val offX = (size.width - spanX.toFloat() * scale) / 2
        val offY = (size.height - spanY.toFloat() * scale) / 2
        fun pt(i: Int) = Offset(offX + ((xs[i] - minX) * scale).toFloat(), offY + ((maxY - ys[i]) * scale).toFloat())
        val path = Path().apply {
            moveTo(pt(0).x, pt(0).y)
            for (i in 1 until points.size) lineTo(pt(i).x, pt(i).y)
        }
        drawPath(path, lineColor.copy(alpha = 0.25f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, lineColor, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val start = pt(0)
        drawCircle(background, 7.dp.toPx(), start)
        drawCircle(lineColor, 7.dp.toPx(), start, style = Stroke(3.5.dp.toPx()))
        val end = pt(points.lastIndex)
        if (live) drawCircle(lineColor.copy(alpha = 0.25f), 16.dp.toPx(), end)
        drawCircle(lineColor, 8.dp.toPx(), end)
        drawCircle(Color.White, 8.dp.toPx(), end, style = Stroke(3.dp.toPx()))
    }
}

@Composable
fun ProgressBar(fraction: Float, track: Color, fill: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 8.dp, max = 8.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(CircleShape).background(fill))
    }
}

@Composable
fun Dot(color: Color, size: Int = 8) = Box(Modifier.size(size.dp).clip(CircleShape).background(color))

@Composable
fun HSpace(w: Int) = Spacer(Modifier.width(w.dp))

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, letterSpacing = (-0.5).sp), color = LocalPalette.current.ink, modifier = modifier)
