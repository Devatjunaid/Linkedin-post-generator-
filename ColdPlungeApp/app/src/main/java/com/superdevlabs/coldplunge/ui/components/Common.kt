package com.superdevlabs.coldplunge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.KType

/** Springy press feedback: scales the element down while pressed. */
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    label: String? = null,
): Modifier = this.pressableImpl(onClick, enabled, role, label)

private fun Modifier.pressableImpl(onClick: () -> Unit, enabled: Boolean, role: Role, label: String?): Modifier =
    composed {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press")
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = role,
                onClickLabel = label,
                onClick = onClick,
            )
    }

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    border: Color = K.Line,
    background: Brush = Brush.linearGradient(listOf(Color(0x12FFFFFF), Color(0x08FFFFFF))),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(26.dp))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = K.Muted) {
    Text(text.uppercase(), style = KType.Label.copy(color = color), modifier = modifier)
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = K.Text,
    background: Color = Color(0x10FFFFFF),
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
            .pressable(onClick = onClick, label = contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

/** Solid light pill — the main call to action on every screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) K.Text else Color(0x33EEF3F7))
            .pressable(onClick = onClick, enabled = enabled)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = K.Ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = KType.BodyStrong.copy(color = K.Ink, fontSize = KType.BodyStrong.fontSize * 1.1f))
    }
}

/** Gradient-rimmed pill used for start actions. */
@Composable
fun ThermalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = KIcons.Play) {
    Box(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(K.EmberDeep, K.Ember, K.Ice, K.IceDeep)))
            .padding(2.dp)
            .pressable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(K.Text),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = K.Ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = KType.BodyStrong.copy(color = K.Ink, fontSize = KType.BodyStrong.fontSize * 1.15f))
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x10FFFFFF))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = K.Text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = KType.BodyStrong)
    }
}

/** − value + control. */
@Composable
fun Stepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseLabel: String,
    increaseLabel: String,
    modifier: Modifier = Modifier,
    valueWidth: Dp = 64.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(KIcons.Minus, decreaseLabel, onDecrease, size = 40.dp)
        Text(value, style = KType.Mono.copy(fontSize = KType.Mono.fontSize * 0.95f), textAlign = TextAlign.Center, modifier = Modifier.width(valueWidth))
        CircleIconButton(KIcons.Plus, increaseLabel, onIncrease, size = 40.dp)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = K.Text) {
    val bg by animateColorAsState(if (selected) accent else Color(0x0AFFFFFF), label = "chipBg")
    val fg by animateColorAsState(if (selected) K.Ink else Color(0xFFC9D3DC), label = "chipFg")
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (selected) accent else Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
            .pressable(onClick = onClick, role = Role.Tab)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = KType.BodyStrong.copy(color = fg, fontSize = KType.Small.fontSize * 1.08f))
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) IconBadge(icon, K.Ice)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = KType.BodyStrong)
            if (subtitle != null) Text(subtitle, style = KType.Small)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = K.Ink,
                checkedTrackColor = K.Ice,
                checkedBorderColor = K.Ice,
                uncheckedThumbColor = K.Muted,
                uncheckedTrackColor = Color(0x14FFFFFF),
                uncheckedBorderColor = Color(0x33FFFFFF),
            ),
        )
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 38.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = K.Text) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = KType.Small.copy(fontSize = KType.Small.fontSize * 0.92f))
        Text(value, style = KType.Mono.copy(color = valueColor))
    }
}

/** Thin labelled progress bar used for weekly goals. */
@Composable
fun GoalBar(label: String, valueText: String, progress: Float, brush: Brush, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 60f), label = "goal")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = KType.Small)
            Text(valueText, style = KType.Small.copy(color = K.Text))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x14FFFFFF)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush),
            )
        }
    }
}

@Composable
fun NoteLine(text: String, tint: Color = K.Gold, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(KIcons.Info, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(text, style = KType.Small.copy(color = Color(0xFFD5DDE4)))
    }
}
