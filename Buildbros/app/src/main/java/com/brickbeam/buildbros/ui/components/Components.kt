package com.brickbeam.buildbros.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickbeam.buildbros.ui.theme.BmIcons
import com.brickbeam.buildbros.ui.theme.BmType
import com.brickbeam.buildbros.ui.theme.LocalMotion
import com.brickbeam.buildbros.ui.theme.LocalPalette

@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Box(modifier.fillMaxSize().background(p.bg)) {
        if (p.blueprintGrid) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 22.dp.toPx()
                val c = p.accent.copy(alpha = 0.07f)
                var x = 0f
                while (x < size.width) { drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                var y = 0f
                while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y), 1f); y += step }
            }
        }
        content()
    }
}

@Composable
fun BmCard(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.surface,
    radius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(14.dp),
    border: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .clip(shape)
            .background(color)
            .then(if (border) Modifier.border(1.dp, p.line, shape) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Cards fade and rise in with a small stagger, like the mockups. */
@Composable
fun Reveal(index: Int = 0, content: @Composable () -> Unit) {
    val motion = LocalMotion.current
    var visible by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450, delayMillis = index * 70)) +
            slideInVertically(tween(500, delayMillis = index * 70)) { it / 6 },
    ) { content() }
}

@Composable
fun IconSquareButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = p.ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) IconSquareButton(BmIcons.Back, "Back", onBack)
        Column(Modifier.weight(1f)) {
            Text(title, style = BmType.title.copy(fontSize = 24.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = BmType.label, color = p.mute, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = BmType.section, color = p.ink, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing, style = BmType.label.copy(fontWeight = FontWeight.Bold), color = p.accentText,
                modifier = if (onTrailing != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onTrailing).padding(6.dp) else Modifier,
            )
        }
    }
}

/**
 * Labelled numeric/text input in a card, as in the mockups. [isError] outlines it in red and
 * [hint] shows under the value (e.g. "12' 6\" or 12.5").
 */
@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    hint: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    val p = LocalPalette.current
    val borderColor by animateColorAsState(if (isError) p.danger else p.line, label = "border")
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(p.surface)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(label, style = BmType.tiny, color = p.mute, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = BmType.figure.copy(color = p.ink, fontSize = 17.sp),
                cursorBrush = SolidColor(p.accentText),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f).semantics { contentDescription = label },
            )
            if (suffix != null) Text(suffix, style = BmType.label, color = p.mute, modifier = Modifier.padding(start = 4.dp))
        }
        if (hint != null) Text(hint, style = BmType.tiny, color = if (isError) p.danger else p.mute)
    }
}

@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    sublabel: ((T) -> String)? = null,
) {
    val p = LocalPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(p.surface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            val bg by animateColorAsState(if (on) p.surface else Color.Transparent, label = "seg")
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(o) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label(o), style = BmType.label.copy(fontWeight = FontWeight.Bold),
                    color = if (on) p.ink else p.mute, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (sublabel != null) Text(sublabel(o), style = BmType.tiny.copy(fontSize = 10.sp), color = p.mute, maxLines = 1)
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, leading: String? = null) {
    val p = LocalPalette.current
    val bg by animateColorAsState(if (selected) p.accent else p.surface, label = "chip")
    Row(
        modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, if (selected) p.accent else p.line, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val fg = if (selected) p.onAccent else p.ink
        if (leading != null) {
            Text(
                leading, style = BmType.tiny.copy(fontFamily = BmType.figure.fontFamily, fontWeight = FontWeight.Bold),
                color = fg,
                modifier = Modifier.border(1.dp, fg, RoundedCornerShape(5.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        Text(text, style = BmType.label.copy(fontWeight = FontWeight.Bold), color = fg, maxLines = 1)
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, color: Color = LocalPalette.current.ink) {
    val p = LocalPalette.current
    Column(modifier) {
        Text(label, style = BmType.tiny, color = if (color == p.ink) p.mute else color.copy(alpha = 0.8f), maxLines = 1)
        Text(value, style = BmType.figure.copy(fontSize = 15.sp), color = color, maxLines = 2)
    }
}

/** The big accent "You need" card. */
@Composable
fun ResultHero(
    caption: String,
    figure: String,
    unit: String,
    detail: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val p = LocalPalette.current
    BmCard(modifier.fillMaxWidth(), color = p.accent, radius = 24.dp, border = false, padding = PaddingValues(16.dp)) {
        Text(caption, style = BmType.label.copy(fontWeight = FontWeight.Bold), color = p.onAccent.copy(alpha = 0.85f))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedNumberText(figure, BmType.bigFigure, p.onAccent)
            Text(unit, style = BmType.section.copy(fontSize = 16.sp), color = p.onAccent, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(detail, style = BmType.tiny, color = p.onAccent.copy(alpha = 0.85f))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.onAccent.copy(alpha = 0.2f)))
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** Number text that pops slightly whenever its value changes. */
@Composable
fun AnimatedNumberText(text: String, style: TextStyle, color: Color) {
    val motion = LocalMotion.current
    var pulse by remember { mutableStateOf(false) }
    LaunchedEffect(text) { if (motion) { pulse = true; kotlinx.coroutines.delay(140); pulse = false } }
    val scale by animateFloatAsState(if (pulse) 1.06f else 1f, tween(160), label = "pulse")
    Text(text, style = style, color = color, maxLines = 1, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val p = LocalPalette.current
    Row(
        modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) p.ink else p.line)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = p.bg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = BmType.section.copy(fontSize = 15.sp), color = p.bg, maxLines = 1)
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val p = LocalPalette.current
    Row(
        modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = p.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = BmType.section.copy(fontSize = 15.sp), color = p.onAccent, maxLines = 1)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val p = LocalPalette.current
    Row(
        modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = p.ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = BmType.section, color = p.ink, maxLines = 1)
    }
}

@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, range: IntRange, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = BmType.label, color = p.ink, modifier = Modifier.weight(1f))
        SmallSquare("−", "Decrease $label", enabled = value > range.first) { onChange(value - 1) }
        Text("$value", style = BmType.figure, color = p.ink, modifier = Modifier.widthIn(min = 28.dp))
        SmallSquare("+", "Increase $label", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun SmallSquare(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(p.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Text(text, style = BmType.section.copy(fontSize = 18.sp), color = if (enabled) p.ink else p.line) }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, sub: String? = null) {
    val p = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = BmType.body, color = p.ink)
            if (sub != null) Text(sub, style = BmType.tiny, color = p.mute)
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = p.accent, checkedThumbColor = p.onAccent,
                uncheckedTrackColor = p.surface2, uncheckedThumbColor = p.mute, uncheckedBorderColor = p.line,
            ),
        )
    }
}

/** A tappable field that opens a dropdown of options. */
@Composable
fun <T> PickerField(
    label: String,
    current: T,
    options: List<T>,
    display: (T) -> String,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.surface)
                .border(1.dp, p.line, RoundedCornerShape(16.dp))
                .clickable(role = Role.DropdownList, onClickLabel = "Choose $label") { open = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text("$label ▾", style = BmType.tiny, color = p.mute)
            Text(display(current), style = BmType.label.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = p.ink, maxLines = 2)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(display(o), style = BmType.body) },
                    onClick = { onPick(o); open = false },
                    trailingIcon = { if (o == current) Icon(BmIcons.Check, null) },
                )
            }
        }
    }
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.glow)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(BmIcons.Info, null, tint = p.accentText, modifier = Modifier.size(18.dp))
        Text(text, style = BmType.tiny, color = p.ink)
    }
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Text(
        text,
        style = BmType.tiny.copy(fontWeight = FontWeight.Bold),
        color = p.accentText,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(p.glow).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
