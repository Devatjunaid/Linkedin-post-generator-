package com.odomate.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odomate.app.R
import com.odomate.app.domain.Level
import com.odomate.app.domain.ServiceStatus
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.ui.theme.Type
import com.odomate.app.util.Formats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs

// ------------------------------------------------------------------ layout

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) 8.dp else 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = c.text)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    color: Color = AppTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = color,
        border = BorderStroke(1.dp, c.outline),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = Type.overline, color = AppTheme.colors.muted, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, background: Color, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun ServiceIconBadge(iconKey: String, level: Level, size: Dp = 40.dp) {
    val c = AppTheme.colors
    IconBadge(serviceIcon(iconKey), c.levelColor(level), c.levelSoft(level), size)
}

@Composable
fun LinearMeter(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "meter")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(AppTheme.colors.outline)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun StatusPill(text: String, level: Level, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Text(
        text.uppercase(),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.levelSoft(level))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = c.levelColor(level),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
    )
}

/** Odometer shown as individual digit tiles, like the real thing. */
@Composable
fun OdometerDigits(value: Int?, unit: String, modifier: Modifier = Modifier, digits: Int = 6, fontSize: Int = 32) {
    val c = AppTheme.colors
    val text = value?.toString()?.padStart(digits, '0') ?: "-".repeat(digits)
    val leading = if (value == null) 0 else text.length - value.toString().length
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            text.forEachIndexed { i, ch ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.bg)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        ch.toString(),
                        style = Type.mono.copy(fontSize = fontSize.sp),
                        color = if (i < leading) c.faint else c.text,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(unit, color = c.muted, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = modifier.size(size))
}

// ------------------------------------------------------------------ buttons

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = AppTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent, contentColor = c.onAccent,
            disabledContainerColor = c.surface2, disabledContentColor = c.faint,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = AppTheme.colors.text,
) {
    val c = AppTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, c.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = c.surface2, contentColor = contentColor),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ------------------------------------------------------------------ inputs

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    mono: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = c.faint) } },
        prefix = prefix?.let { { Text(it, color = c.muted) } },
        suffix = suffix?.let { { Text(it, color = c.muted) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = if (mono) Type.mono.copy(fontSize = 20.sp, color = c.text) else MaterialTheme.typography.bodyLarge.copy(color = c.text),
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.outline,
            focusedLabelColor = c.accentText,
            unfocusedLabelColor = c.muted,
            cursorColor = c.accent,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
        ),
    )
}

/** Converts between a local calendar date and the UTC-midnight millis the Material date picker uses. */
private fun localToPickerMillis(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun pickerToLocalMillis(pickerMillis: Long): Long =
    Instant.ofEpochMilli(pickerMillis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    millis: Long?,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    allowFuture: Boolean = false,
    placeholder: String = "Pick a date",
) {
    val c = AppTheme.colors
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AppTextField(
            value = millis?.let { Formats.date(it) } ?: "",
            onValueChange = {},
            label = label,
            placeholder = placeholder,
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = c.muted) },
        )
        // Transparent overlay so the whole field opens the picker.
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { open = true }
        )
    }
    if (open) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localToPickerMillis(millis ?: System.currentTimeMillis()),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = allowFuture || utcTimeMillis <= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(pickerToLocalMillis(it)) }
                    open = false
                }) { Text("OK", color = c.accentText) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel", color = c.muted) } },
        ) {
            DatePicker(state = state)
        }
    }
}

// ------------------------------------------------------------------ dialogs & states

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.text) },
        text = { Text(text, color = c.muted) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) c.danger else c.accentText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.muted) } },
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    val c = AppTheme.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBadge(icon, c.accentText, c.accentSoft, 56.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action?.invoke()
    }
}

// ------------------------------------------------------------------ status text

/** Short label for the right-hand side of a service row, e.g. "585 km" or "18 days". */
fun statusShort(status: ServiceStatus, unit: String): String = when (status.level) {
    Level.DISABLED -> "Off"
    Level.UNKNOWN -> "Set up"
    else -> if (status.distanceDominant && status.remainingDistance != null) {
        Formats.distance(abs(status.remainingDistance), unit)
    } else if (status.remainingDays != null) {
        if (status.remainingDays == 0L) "Today" else Formats.days(status.remainingDays)
    } else "-"
}

/** One-line explanation under a service name. */
fun statusDetail(status: ServiceStatus, unit: String): String {
    val t = status.type
    return when (status.level) {
        Level.DISABLED -> "Reminders off"
        Level.UNKNOWN -> "Tap to set when it was last done"
        Level.OVERDUE -> {
            val what = if (status.distanceDominant && status.remainingDistance != null) {
                "${Formats.distance(abs(status.remainingDistance), unit)} past due"
            } else "${Formats.days(status.remainingDays ?: 0)} past due"
            what + (status.dueOdometer?.let { " · was due at ${Formats.distance(it, unit)}" } ?: "")
        }
        else -> buildList {
            status.dueOdometer?.let { add("Due at ${Formats.distance(it, unit)}") }
            status.dueDate?.let { add((if (isEmpty()) "Due " else "or by ") + Formats.shortDate(it)) }
        }.joinToString(" ").ifBlank { intervalText(t.intervalDistance, t.intervalMonths, unit) }
    }
}

fun intervalText(distance: Int?, months: Int?, unit: String): String {
    val parts = buildList {
        distance?.let { add(Formats.distance(it, unit)) }
        months?.let { add(if (it % 12 == 0) "${it / 12} ${if (it == 12) "year" else "years"}" else "$it ${if (it == 1) "month" else "months"}") }
    }
    return if (parts.isEmpty()) "No interval set" else "Every " + parts.joinToString(" or ")
}

fun levelLabel(level: Level): String = when (level) {
    Level.OVERDUE -> "Overdue"
    Level.SOON -> "Due soon"
    Level.OK -> "On track"
    Level.UNKNOWN -> "Not set up"
    Level.DISABLED -> "Off"
}
