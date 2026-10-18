package com.devatjunaid.balconysolar.ui.components

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type

val CardShape = RoundedCornerShape(20.dp)

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    color: Color = Palette.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color)
            .border(1.dp, Palette.Line, CardShape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun CardLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null, trailingColor: Color = Palette.Mint) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Type.label, color = Palette.Muted, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = Type.mono, color = trailingColor)
    }
}

@Composable
fun ScreenHeader(
    step: Int?,
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(AppIcons.Back, contentDescription = "Back", tint = Palette.Ink)
                }
            }
            Spacer(Modifier.weight(1f))
            if (step != null) Text("STEP $step / 5", style = Type.mono, color = Palette.Muted, modifier = Modifier.padding(end = 4.dp))
            actions()
        }
        Text(title, style = Type.display, color = Palette.Ink, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Palette.Ink) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = Type.label, color = Palette.Muted, maxLines = 1)
        Text(value, style = Type.stat, color = valueColor, maxLines = 1)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Palette.Amber,
    contentColor: Color = Palette.OnAmber,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(if (enabled) color else Palette.SurfaceHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Type.button, color = if (enabled) contentColor else Palette.Muted)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = if (enabled) contentColor else Palette.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = Palette.Mint) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Palette.LineStrong, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.labelStrong, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Palette.Amber, onAccent: Color = Palette.OnAmber) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accent else Palette.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, Palette.Line, RoundedCornerShape(20.dp)))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.labelStrong, color = if (selected) onAccent else Palette.Ink, maxLines = 1)
    }
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (o in options) {
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) Palette.Amber else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(o) }
                    .semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = Type.labelStrong, color = if (on) Palette.OnAmber else Palette.Muted, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int, max: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(AppIcons.Minus, "Fewer $label", enabled = value > min) { onChange(value - 1) }
        Text(value.toString(), style = Type.title, color = Palette.Ink, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
        RoundIconButton(AppIcons.Plus, "More $label", enabled = value < max) { onChange(value + 1) }
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, description: String, size: Dp = 44.dp, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, if (enabled) Palette.LineStrong else Palette.Line, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Palette.Ink else Palette.Faint, modifier = Modifier.size(20.dp))
    }
}

/** A tappable row showing a value; tapping opens a number editor. */
@Composable
fun ValueRow(label: String, value: String, onClick: () -> Unit, showDivider: Boolean = true, hint: String? = null) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = Type.body, color = Palette.Ink)
                if (hint != null) Text(hint, style = Type.label, color = Palette.Muted)
            }
            Text(value, style = Type.mono.copy(fontSize = Type.labelStrong.fontSize), color = Palette.Ink)
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = Palette.Faint, modifier = Modifier.padding(start = 6.dp).size(16.dp))
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
    }
}

@Composable
fun InfoBanner(text: String, icon: ImageVector = AppIcons.Bolt, color: Color = Palette.Mint, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = Type.label.copy(fontSize = Type.labelStrong.fontSize), color = Palette.Ink)
    }
}

data class NumberEdit(
    val title: String,
    val unit: String,
    val initial: Double,
    val min: Double,
    val max: Double,
    val decimals: Int,
    val help: String? = null,
    val apply: (Double) -> Unit,
)

@Composable
fun NumberEditDialog(edit: NumberEdit, onDismiss: () -> Unit) {
    var text by remember(edit) {
        mutableStateOf(java.math.BigDecimal(edit.initial).setScale(edit.decimals, java.math.RoundingMode.HALF_UP).toPlainString())
    }
    val parsed = Fmt.parse(text)
    val valid = parsed != null && parsed >= edit.min && parsed <= edit.max
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = { Text(edit.title, style = Type.title, color = Palette.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    suffix = { Text(edit.unit, color = Palette.Muted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !valid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Palette.Amber,
                        unfocusedBorderColor = Palette.LineStrong,
                        focusedTextColor = Palette.Ink,
                        unfocusedTextColor = Palette.Ink,
                        cursorColor = Palette.Amber,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Allowed: ${Fmt.number(edit.min, edit.decimals)} – ${Fmt.number(edit.max, edit.decimals)} ${edit.unit}",
                    style = Type.label, color = if (valid) Palette.Muted else Palette.Coral,
                )
                if (edit.help != null) Text(edit.help, style = Type.label, color = Palette.Muted)
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(edit.apply); onDismiss() }, enabled = valid) {
                Text("Apply", color = if (valid) Palette.Amber else Palette.Faint, style = Type.labelStrong)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Muted, style = Type.labelStrong) } },
    )
}

@Composable
fun BorderedBox(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Palette.Panel).border(BorderStroke(1.dp, Palette.Line), RoundedCornerShape(24.dp)), content = content)
}
