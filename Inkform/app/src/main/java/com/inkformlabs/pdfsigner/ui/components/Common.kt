package com.inkformlabs.pdfsigner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk

enum class ButtonKind { Primary, Accent, Secondary }

@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    height: Dp = 54.dp,
) {
    val p = LocalInk.current
    val (container, content) = when (kind) {
        ButtonKind.Primary -> p.ink to p.bg
        ButtonKind.Accent -> p.accent to p.onAccent
        ButtonKind.Secondary -> p.surface to p.ink
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(18.dp),
        border = if (kind == ButtonKind.Secondary) BorderStroke(1.dp, p.line) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = p.surface2,
            disabledContentColor = p.muted,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalInk.current.ink,
    background: Color = LocalInk.current.surface,
    enabled: Boolean = true,
) {
    val p = LocalInk.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, p.line, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint, disabledContentColor = p.muted),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun InkCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    color: Color = LocalInk.current.surface,
    content: @Composable () -> Unit,
) {
    val p = LocalInk.current
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .border(1.dp, p.line, RoundedCornerShape(22.dp))
            .padding(padding),
    ) { content() }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
        color = LocalInk.current.muted,
    )
}

@Composable
fun OfflineBadge(modifier: Modifier = Modifier) {
    val p = LocalInk.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(p.surface2)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(InkIcons.Shield, contentDescription = null, tint = p.hi, modifier = Modifier.size(16.dp))
        Text(
            "Offline · your files never leave this phone",
            style = MaterialTheme.typography.labelMedium,
            color = p.ink,
        )
    }
}

/** Circular progress with a label in the middle; animates when [progress] changes. */
@Composable
fun ProgressRing(progress: Float, label: String, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val p = LocalInk.current
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier.size(size).semantics { contentDescription = "${(progress * 100).toInt()} percent complete" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(p.surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(p.hi, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = p.ink, textAlign = TextAlign.Center)
    }
}

@Composable
fun inkTextFieldColors(): TextFieldColors {
    val p = LocalInk.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.ink,
        unfocusedTextColor = p.ink,
        focusedContainerColor = p.surface,
        unfocusedContainerColor = p.surface,
        focusedBorderColor = p.hi,
        unfocusedBorderColor = p.line,
        focusedLabelColor = p.hi,
        unfocusedLabelColor = p.muted,
        cursorColor = p.hi,
        focusedPlaceholderColor = p.muted,
        unfocusedPlaceholderColor = p.muted,
    )
}

@Composable
fun InkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder != null) Text(placeholder) },
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        colors = inkTextFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalInk.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = p.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = p.muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.bg,
                checkedTrackColor = p.hi,
                uncheckedThumbColor = p.muted,
                uncheckedTrackColor = p.surface2,
                uncheckedBorderColor = p.line,
            ),
        )
    }
}

/** Blocking progress overlay used while a PDF is being processed. */
@Composable
fun BusyOverlay(message: String) {
    val p = LocalInk.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(p.surface)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InkformLogo(Modifier.size(84.dp), playIntro = true, idle = true, introMillis = 1200, showOrbit = false)
            Text(message, style = MaterialTheme.typography.titleSmall, color = p.ink, textAlign = TextAlign.Center)
            CircularProgressIndicator(color = p.hi, trackColor = p.surface2, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
        }
    }
}
