package com.sunarc.inverterlogger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.sunarc.inverterlogger.ui.theme.DisplayFont
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import java.util.Locale

val CardShape = RoundedCornerShape(22.dp)

@Composable
fun SunCard(
    modifier: Modifier = Modifier,
    color: Color = Palette.Surface,
    border: Color = Palette.LineSoft,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(CardShape)
            .background(color)
            .border(1.dp, border, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            SquareIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        }
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun SquareIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Palette.Ink, filled: Boolean = false) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) Palette.Sun else Palette.Surface)
            .border(1.dp, if (filled) Palette.Sun else Palette.Line, RoundedCornerShape(14.dp))
    ) {
        Icon(icon, contentDescription = description, tint = if (filled) Palette.OnSun else tint)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Sun, contentColor = Palette.OnSun,
            disabledContainerColor = Palette.SunDeep, disabledContentColor = Palette.Muted
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Palette.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Surface, contentColor = Palette.Ink)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Palette.Sun)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SunChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) Palette.Sun.copy(alpha = 0.14f) else Palette.Surface, tween(200), label = "chipBg")
    val fg by animateColorAsState(if (selected) Palette.SunSoft else Palette.InkSoft, tween(200), label = "chipFg")
    val stroke by animateColorAsState(if (selected) Palette.Sun else Palette.Line, tween(200), label = "chipStroke")
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 13.sp)
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            val on = opt == selected
            val bg by animateColorAsState(if (on) Palette.Sun else Color.Transparent, tween(250), label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(opt) },
                contentAlignment = Alignment.Center
            ) {
                Text(label(opt), color = if (on) Palette.OnSun else Palette.InkSoft, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, unit: String? = null, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color = Palette.Sun, footnote: String? = null) {
    SunCard(modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
            Text(label, fontSize = 12.sp, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Palette.Ink, maxLines = 1)
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 13.sp, color = Palette.Muted, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        if (footnote != null) Text(footnote, fontSize = 11.sp, color = Palette.Muted)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(Locale.ROOT), modifier = modifier.padding(top = 6.dp, bottom = 2.dp), fontSize = 12.sp, letterSpacing = 1.sp, color = Palette.Muted)
}

@Composable
fun Pill(text: String, color: Color = Palette.Sky, filled: Boolean = false) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) color else Color.Transparent)
            .border(1.dp, if (filled) color else color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        fontFamily = MonoFont, fontSize = 11.sp, fontWeight = FontWeight.Medium,
        color = if (filled) Palette.OnSun else color
    )
}

@Composable
fun ConfidenceBar(confidence: Double, modifier: Modifier = Modifier) {
    val target = confidence.toFloat().coerceIn(0f, 1f)
    val anim by animateFloatAsState(target, tween(700), label = "conf")
    Box(modifier.width(54.dp).height(6.dp).clip(CircleShape).background(Palette.Line)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(anim).clip(CircleShape)
                .background(if (confidence >= 0.85) Palette.Sun else Palette.Sky)
        )
    }
}

@Composable
fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Switch) { onChange(!checked) }.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = Palette.Ink)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Palette.Muted)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.OnSun, checkedTrackColor = Palette.Sun,
                uncheckedThumbColor = Palette.Muted, uncheckedTrackColor = Palette.Line, uncheckedBorderColor = Palette.Line
            )
        )
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    decimal: Boolean = true,
    signed: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onValueChange(s.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) || (signed && it == '-') }) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, color = Palette.Muted) } },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Palette.Ink),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
        modifier = modifier
    )
}

@Composable
fun TextInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String? = null, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true,
        placeholder = placeholder?.let { { Text(it, color = Palette.Faint) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(16.dp), colors = fieldColors(), modifier = modifier
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Sun, unfocusedBorderColor = Palette.Line,
    focusedLabelColor = Palette.SunSoft, unfocusedLabelColor = Palette.Muted,
    cursorColor = Palette.Sun, focusedContainerColor = Palette.Surface, unfocusedContainerColor = Palette.Surface,
    focusedTextColor = Palette.Ink, unfocusedTextColor = Palette.Ink
)

/** Parses user-typed numbers in either "12.5" or "12,5" form. */
fun parseUserNumber(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

object Fmt {
    fun kwh(v: Double?): String = when {
        v == null -> "–"
        v >= 10_000 -> String.format(Locale.getDefault(), "%,.0f", v)
        v >= 100 -> String.format(Locale.getDefault(), "%,.0f", v)
        else -> String.format(Locale.getDefault(), "%.1f", v)
    }

    /** Energy with a unit that keeps the number short (kWh or MWh). */
    fun energy(v: Double?): Pair<String, String> = when {
        v == null -> "–" to "kWh"
        v >= 100_000 -> String.format(Locale.getDefault(), "%.1f", v / 1000) to "MWh"
        else -> kwh(v) to "kWh"
    }

    fun power(w: Double?): Pair<String, String> = when {
        w == null -> "–" to "kW"
        w >= 1000 -> String.format(Locale.getDefault(), "%.2f", w / 1000) to "kW"
        else -> String.format(Locale.getDefault(), "%.0f", w) to "W"
    }

    fun money(symbol: String, v: Double): String = symbol + String.format(Locale.getDefault(), if (v >= 1000) "%,.0f" else "%.2f", v)

    fun number(v: Double?, decimals: Int = 1): String = v?.let { String.format(Locale.getDefault(), "%.${decimals}f", it) } ?: ""
}
