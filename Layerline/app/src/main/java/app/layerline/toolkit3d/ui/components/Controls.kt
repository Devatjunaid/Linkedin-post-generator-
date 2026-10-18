package app.layerline.toolkit3d.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType

/** Machined-part corners: only top-left and bottom-right are cut. */
val Chamfer: Shape = CutCornerShape(topStart = 14.dp, topEnd = 0.dp, bottomEnd = 14.dp, bottomStart = 0.dp)
val ChamferS: Shape = CutCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomEnd = 8.dp, bottomStart = 0.dp)

/** Print-bed dot grid. */
fun Modifier.bedDots(color: Color = LL.Dot, spacing: Dp = 16.dp): Modifier = drawBehind {
    val step = spacing.toPx()
    val r = 1.2.dp.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(color, radius = r, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

/** Press feedback shared by every tappable surface: a quick spring squash. */
@Composable
fun Modifier.pressable(onClick: () -> Unit, enabled: Boolean = true, role: Role = Role.Button): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "press")
    return this
        .scale(s)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = LL.Muted) {
    Text(
        text,
        style = LLType.mono(11.sp, color = color).copy(letterSpacing = 0.9.sp),
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun GcodeTag(text: String, color: Color = LL.Lime) {
    Text(text, style = LLType.mono(11.sp, color = color).copy(letterSpacing = 0.9.sp))
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    shape: Shape = Chamfer,
    color: Color = LL.Panel,
    border: BorderStroke? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier
    if (onClick != null) m = m.pressable(onClick)
    m = m.background(color, shape)
    if (border != null) m = m.border(border, shape)
    Column(m.padding(padding), content = content)
}

enum class ButtonStyle { Primary, Ghost, Dark, Hot }

@Composable
fun LButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val (bg, fg) = when (style) {
        ButtonStyle.Primary -> LL.Lime to LL.Graphite
        ButtonStyle.Hot -> LL.Hot to LL.Graphite
        ButtonStyle.Dark -> LL.Panel to LL.Ink
        ButtonStyle.Ghost -> Color.Transparent to LL.Ink
    }
    val shape = if (style == ButtonStyle.Primary || style == ButtonStyle.Hot) Chamfer else ChamferS
    var m = modifier
        .heightIn(min = height)
        .pressable(onClick, enabled)
        .background(if (enabled) bg else LL.Raised, shape)
    if (style == ButtonStyle.Ghost || style == ButtonStyle.Dark) m = m.border(1.dp, LL.LineStrong, shape)
    Row(
        m.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            LIcon(icon, if (enabled) fg else LL.Muted, size = 18.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text.uppercase(),
            style = LLType.display(16.sp, FontWeight.Bold, if (enabled) fg else LL.Muted).copy(letterSpacing = 0.6.sp),
            maxLines = 1,
        )
    }
}

@Composable
fun IconSquare(icon: ImageVector, contentDescription: String, onClick: () -> Unit, tint: Color = LL.Ink, bg: Color = LL.Panel2) {
    Box(
        Modifier
            .size(44.dp)
            .pressable(onClick)
            .background(bg, ChamferS)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { LIcon(icon, tint, size = 20.dp) }
}

/** Selectable chip. Selected = solid ink (or [selectedColor]) with dark text. */
@Composable
fun LChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    selectedColor: Color = LL.Ink,
    minHeight: Dp = 40.dp,
) {
    val bg by animateColorAsState(if (selected) selectedColor else Color.Transparent, label = "chipBg")
    val fg by animateColorAsState(if (selected) LL.Graphite else LL.Soft, label = "chipFg")
    Column(
        modifier
            .heightIn(min = minHeight)
            .pressable(onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .background(bg, ChamferS)
            .border(1.dp, if (selected) selectedColor else LL.LineStrong, ChamferS)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = LLType.mono(12.sp, FontWeight.SemiBold, fg), maxLines = 1)
        if (sub != null) Text(sub, style = LLType.mono(9.sp, color = fg.copy(alpha = 0.75f)), maxLines = 1)
    }
}

/** On/off requirement chip with a check box, as used for "what this print needs". */
@Composable
fun ToggleChip(label: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border by animateColorAsState(if (on) LL.Lime else LL.LineStrong, label = "tgB")
    val bg by animateColorAsState(if (on) LL.LimeDeep else LL.Panel, label = "tgBg")
    Row(
        modifier
            .height(44.dp)
            .pressable(onClick, role = Role.Checkbox)
            .semantics { selected = on }
            .background(bg, ChamferS)
            .border(1.dp, border, ChamferS)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .border(1.5.dp, border)
                .background(if (on) LL.Lime else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { if (on) LIcon(LIcons.Check, LL.Graphite, size = 11.dp) }
        Spacer(Modifier.width(8.dp))
        Text(label, style = LLType.body(13.sp, color = LL.Ink), maxLines = 1)
    }
}

/** Big stepper tile: label, tappable value (opens an editor), − and + buttons. */
@Composable
fun StepperTile(
    label: String,
    value: String,
    unit: String,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(LL.Panel, ChamferS).padding(10.dp)) {
        Text(label, style = LLType.mono(10.sp, color = LL.Muted))
        Row(
            Modifier
                .padding(top = 2.dp, bottom = 8.dp)
                .clickable(onClick = onEdit, role = Role.Button)
                .semantics { contentDescription = "$label $value $unit, tap to type a value" },
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(value, style = LLType.display(24.sp, FontWeight.SemiBold), maxLines = 1)
            Text(" $unit", style = LLType.mono(11.sp, color = LL.Muted), modifier = Modifier.padding(bottom = 3.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton("−", "Decrease $label", onDec, Modifier.weight(1f), plus = false)
            StepButton("+", "Increase $label", onInc, Modifier.weight(1f), plus = true)
        }
    }
}

@Composable
fun StepButton(symbol: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, plus: Boolean) {
    Box(
        modifier
            .height(44.dp)
            .pressable(onClick)
            .semantics { contentDescription = description }
            .then(if (plus) Modifier.background(LL.Raised) else Modifier.border(1.dp, LL.LineStrong)),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = LLType.mono(18.sp, color = if (plus) LL.Lime else LL.Ink))
    }
}

/** Single-line stepper row: label on the left, − value + on the right. */
@Composable
fun StepperRow(label: String, value: String, unit: String, onDec: () -> Unit, onInc: () -> Unit, onEdit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = LLType.body(14.sp), modifier = Modifier.weight(1f))
        StepButton("−", "Decrease $label", onDec, Modifier.width(44.dp), plus = false)
        Column(
            Modifier
                .width(92.dp)
                .clickable(onClick = onEdit, role = Role.Button),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = LLType.mono(16.sp, FontWeight.SemiBold), maxLines = 1)
            if (unit.isNotEmpty()) Text(unit, style = LLType.mono(10.sp, color = LL.Muted))
        }
        StepButton("+", "Increase $label", onInc, Modifier.width(44.dp), plus = true)
    }
}

/** Labelled text input in the app's flat style. */
@Composable
fun LTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    placeholder: String = "",
) {
    Column(modifier) {
        Text(label.uppercase(), style = LLType.mono(10.sp, color = LL.Muted), modifier = Modifier.padding(bottom = 4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(LL.Graphite, ChamferS)
                .border(1.dp, LL.LineStrong, ChamferS)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = LLType.body(15.sp, color = LL.Muted))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = if (keyboardType == KeyboardType.Text) LLType.body(15.sp) else LLType.mono(15.sp, FontWeight.Medium),
                    cursorBrush = SolidColor(LL.Lime),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (suffix != null) Text(suffix, style = LLType.mono(12.sp, color = LL.Muted))
        }
    }
}

/** Number input that keeps the user's typed text and reports parsed values. */
@Composable
fun NumberField(
    label: String,
    text: String,
    onText: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    decimal: Boolean = true,
) {
    LTextField(
        label = label,
        value = text,
        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("""\d*[.,]?\d*"""))) onText(new.replace(',', '.')) },
        modifier = modifier,
        keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        suffix = suffix,
    )
}

/** Small stat block: mono caption over a display figure. */
@Composable
fun Stat(caption: String, value: String, modifier: Modifier = Modifier, color: Color = LL.Ink, size: Int = 20) {
    Column(modifier) {
        Text(caption, style = LLType.mono(10.sp, color = LL.Muted), maxLines = 1)
        Text(value, style = LLType.display(size.sp, FontWeight.SemiBold, color), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun EmptyState(title: String, body: String, action: String, onAction: () -> Unit, icon: ImageVector) {
    Panel(Modifier.fillMaxWidth(), color = LL.Panel, padding = PaddingValues(20.dp)) {
        LIcon(icon, LL.Lime, size = 30.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, style = LLType.display(20.sp, FontWeight.SemiBold))
        Spacer(Modifier.height(4.dp))
        Text(body, style = LLType.body(14.sp, color = LL.Soft))
        Spacer(Modifier.height(14.dp))
        LButton(action, onAction, icon = LIcons.Plus)
    }
}

@Composable
fun KeyValueRow(key: String, value: String, valueColor: Color = LL.Ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(key, style = LLType.mono(12.sp, color = LL.Muted), modifier = Modifier.weight(1f))
        Text(value, style = LLType.mono(12.sp, FontWeight.Medium, valueColor), textAlign = TextAlign.End)
    }
}
