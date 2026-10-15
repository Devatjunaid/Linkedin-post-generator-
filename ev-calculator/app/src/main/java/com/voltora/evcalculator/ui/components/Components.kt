package com.voltora.evcalculator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Display
import com.voltora.evcalculator.ui.theme.Ev

/** The black block at the top of every screen, with rounded bottom corners. */
@Composable
fun TopBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Ev.Ink)
            .statusBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Ev.Muted) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = Body,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.6.sp,
    )
}

@Composable
fun BigText(
    text: String,
    size: TextUnit,
    color: Color = Ev.Ink,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Bold,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontFamily = Display,
        fontWeight = weight,
        fontSize = size,
        lineHeight = size,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Pill chip used on both dark and light backgrounds. */
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onDark: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        selected -> if (onDark) Ev.Volt else Ev.Ink
        else -> Color.Transparent
    }
    val fg = when {
        selected -> if (onDark) Ev.Ink else Ev.Paper
        else -> if (onDark) Ev.Paper else Ev.Ink
    }
    val border = if (selected) Color.Transparent else if (onDark) Ev.Line else Ev.Stroke
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.5.dp, border), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontFamily = Body, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
    }
}

/** Light card on the white middle section. */
@Composable
fun MistCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ev.Mist)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Selectable tile (charger location, rate tiles...). */
@Composable
fun Tile(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) Ev.Ink else Ev.Mist)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = if (selected) Ev.Paper else Ev.Ink, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
        Text(subtitle, color = if (selected) Color(0xFFC9C9CC) else Ev.Muted, fontFamily = Body, fontSize = 12.sp, maxLines = 1)
    }
}

/** Outlined value tile used for the country rates. */
@Composable
fun RateTile(label: String, value: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .border(1.5.dp, if (highlighted) Ev.Ink else Ev.Stroke, shape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = Ev.Muted, fontFamily = Body, fontSize = 12.sp, maxLines = 1)
        BigText(value, 26.sp)
    }
}

@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, keyColor: Color = Ev.Muted, valueColor: Color = Ev.Ink) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = keyColor, fontFamily = Body, fontSize = 14.sp)
        Text(value, color = valueColor, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun VoltButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (dark) Ev.Ink else Ev.Volt)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (dark) Ev.Volt else Ev.Ink, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun VoltSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = Ev.Volt,
            activeTrackColor = if (onDark) Ev.Volt else Ev.Ink,
            inactiveTrackColor = if (onDark) Ev.Ink3 else Ev.Stroke,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}
