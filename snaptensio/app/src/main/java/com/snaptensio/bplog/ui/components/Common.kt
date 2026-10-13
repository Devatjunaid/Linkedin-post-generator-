package com.snaptensio.bplog.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptensio.bplog.data.Guideline
import com.snaptensio.bplog.domain.BpCategory
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.domain.Severity

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(padding.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surface, border = border, content = body)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surface, border = border, content = body)
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = if (onBack == null) 20.dp else 4.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Text(
            title,
            style = if (onBack == null) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun CategoryChip(category: BpCategory, modifier: Modifier = Modifier) {
    Text(
        category.label,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(category.severity.tint)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = category.severity.onColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
    )
}

/** Five-band scale with an animated marker showing where a reading sits. */
@Composable
fun CategoryBar(sys: Int, dia: Int, guideline: Guideline, modifier: Modifier = Modifier) {
    val bands = listOf(Severity.NORMAL, Severity.ELEVATED, Severity.STAGE1, Severity.STAGE2, Severity.CRISIS)
    val cat = BpClassifier.classify(sys, dia, guideline)
    val index = when (cat.severity) {
        Severity.LOW -> 0f
        Severity.NORMAL -> 0f
        Severity.ELEVATED -> 1f
        Severity.STAGE1 -> 2f
        Severity.STAGE2 -> 3f
        Severity.CRISIS -> 4f
    }
    val target by animateFloatAsState((index + 0.5f) / bands.size, tween(900), label = "marker")
    val markerColor = MaterialTheme.colorScheme.onSurface
    val gap = MaterialTheme.colorScheme.surface
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Range: ${cat.label}" }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val bandW = size.width / bands.size
            bands.forEachIndexed { i, s ->
                drawRoundRect(
                    s.color,
                    topLeft = Offset(i * bandW + 1.5f, 5.dp.toPx()),
                    size = Size(bandW - 3f, 8.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
            val x = size.width * target
            drawRoundRect(gap, Offset(x - 4.dp.toPx(), 0f), Size(8.dp.toPx(), size.height), CornerRadius(4.dp.toPx()))
            drawRoundRect(markerColor, Offset(x - 2.dp.toPx(), 1.dp.toPx()), Size(4.dp.toPx(), size.height - 2.dp.toPx()), CornerRadius(2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth()) {
            val labels = if (guideline == Guideline.AHA) listOf("Normal", "Elevated", "Stage 1", "Stage 2", "Very high")
            else listOf("Normal", "High norm.", "Grade 1", "Grade 2–3", "Very high")
            labels.forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Counts up to [value] — used for the big reading numbers. */
@Composable
fun AnimatedCount(value: Int, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val shown by animateIntAsState(value, tween(700), label = "count")
    Text(shown.toString(), style = style, modifier = modifier, color = color)
}

@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
    ) {
        options.forEach { o ->
            val isSel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .semantics { this.selected = isSel }
                    .clickable(role = Role.Tab) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SeverityBarRow(distribution: List<Pair<Severity, Int>>, labelFor: (Severity) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            distribution.filter { it.second > 0 }.forEach { (s, pct) ->
                Box(Modifier.weight(pct.toFloat()).height(14.dp).background(s.color))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            distribution.forEach { (s, pct) ->
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(s.color))
                        Spacer(Modifier.width(4.dp))
                        Text("$pct%", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(labelFor(s), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedLogo(size = 96.dp, intro = false)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action()
    }
}

@Composable
fun SmallLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 4.dp, top = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.6.sp,
    )
}

/** Severity colour that keeps 4.5:1 contrast as text on the current surface (dark ink in light mode). */
@Composable
fun Severity.readableColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) onColor else color
