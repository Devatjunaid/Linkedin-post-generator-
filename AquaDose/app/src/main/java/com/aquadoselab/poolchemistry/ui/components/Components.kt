package com.aquadoselab.poolchemistry.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquadoselab.poolchemistry.R
import com.aquadoselab.poolchemistry.chem.Level
import com.aquadoselab.poolchemistry.ui.theme.AppColors

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_app_logo),
        contentDescription = "AquaDose logo",
        modifier = modifier.size(size)
    )
}

/** Screen wrapper: scrollable column with standard padding. */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun BackHeader(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.Ink)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        }
        actions()
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    border: Color = AppColors.Border,
    background: Color = AppColors.Card,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, border), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun StatusChip(level: Level, modifier: Modifier = Modifier, textOverride: String? = null) {
    val (bg, fg, label) = when (level) {
        Level.LOW -> Triple(AppColors.WarnBg, AppColors.WarnText, "LOW")
        Level.HIGH -> Triple(AppColors.WarnBg, AppColors.WarnText, "HIGH")
        Level.OK -> Triple(AppColors.OkBg, AppColors.OkText, "OK")
        Level.UNKNOWN -> Triple(AppColors.Track, AppColors.Muted, "—")
    }
    Chip(textOverride ?: label, bg, fg, modifier)
}

@Composable
fun Chip(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

/** Horizontal scale with the acceptable zone shaded and a marker at the current value. */
@Composable
fun RangeBar(value: Double?, low: Double, high: Double, scaleMin: Double, scaleMax: Double, modifier: Modifier = Modifier) {
    val span = (scaleMax - scaleMin).takeIf { it > 0 } ?: 1.0
    fun frac(v: Double) = ((v - scaleMin) / span).coerceIn(0.0, 1.0).toFloat()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val w = maxWidth
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.Track)
        )
        val zStart = frac(low)
        val zEnd = frac(high)
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = w * zStart)
                .width(w * (zEnd - zStart).coerceAtLeast(0.02f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.Zone)
        )
        if (value != null) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (w * frac(value) - 7.dp).coerceIn(0.dp, w - 14.dp))
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(AppColors.Navy)
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teal, contentColor = Color.White)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, AppColors.Teal)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppColors.TealDark)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = AppColors.TealDark)
    }
}

/** Pill-style single choice control. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            val isSel = opt == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSel) Color.White else Color.Transparent)
                    .selectable(selected = isSel, role = Role.RadioButton, onClick = { onSelect(opt) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(opt),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSel) AppColors.Ink else AppColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onValueChange(sanitizeNumber(s)) },
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, color = AppColors.Muted) }
        } else null,
        suffix = if (suffix != null) {
            { Text(suffix, color = AppColors.Muted) }
        } else null,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Teal,
    unfocusedBorderColor = AppColors.BorderStrong,
    focusedLabelColor = AppColors.TealDark,
    cursorColor = AppColors.Teal,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

/** Keeps digits and one decimal separator; accepts comma as decimal point. */
fun sanitizeNumber(s: String): String {
    val t = s.replace(',', '.')
    val sb = StringBuilder()
    var dot = false
    for (ch in t) {
        if (ch.isDigit()) sb.append(ch)
        else if (ch == '.' && !dot) { sb.append(ch); dot = true }
    }
    return sb.toString().take(8)
}

/** A test reading row: label + description, then − [value] + controls. */
@Composable
fun StepperRow(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    step: Double,
    min: Double,
    max: Double,
    start: Double,
    decimals: Int,
    placeholder: String?,
    isError: Boolean
) {
    fun fmt(v: Double) = if (decimals == 0) v.toLong().toString() else String.format(java.util.Locale.US, "%.${decimals}f", v)
    fun bump(dir: Int) {
        val cur = value.toDoubleOrNull() ?: placeholder?.toDoubleOrNull() ?: start
        val next = if (value.isBlank() && placeholder?.toDoubleOrNull() == null) start
        else (cur + dir * step).coerceIn(min, max)
        onValueChange(fmt(Math.round(next / step) * step))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = if (isError) AppColors.Danger else AppColors.Muted)
        }
        StepButton("−", "Decrease $label") { bump(-1) }
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(sanitizeNumber(it)) },
            singleLine = true,
            isError = isError,
            placeholder = if (placeholder != null) {
                { Text(placeholder, color = AppColors.Muted.copy(alpha = 0.7f), fontSize = 16.sp) }
            } else null,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(84.dp)
        )
        StepButton("+", "Increase $label") { bump(1) }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.dp, AppColors.BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = AppColors.Ground, contentColor = AppColors.Ink)
    ) {
        Icon(
            if (symbol == "+") Icons.Filled.Add else Icons.Filled.Remove,
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .selectable(selected = opt == selected, role = Role.RadioButton, onClick = { onSelect(opt) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = opt == selected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label(opt), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = Color.White
    )
}

@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = AppColors.Danger, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White
    )
}

@Composable
fun InfoBanner(text: String, warn: Boolean, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (warn) AppColors.WarnBg else AppColors.OkBg)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = if (warn) AppColors.WarnText else AppColors.OkText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (warn) AppColors.WarnText else AppColors.OkText)
    }
}

@Composable
fun Bullet(text: String, color: Color = AppColors.InkSoft) {
    Row {
        Box(
            Modifier
                .padding(top = 8.dp, end = 8.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(AppColors.Teal)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun StepConnector(height: Dp) {
    Box(
        Modifier
            .width(2.dp)
            .height(height)
            .background(AppColors.Zone)
    )
}
