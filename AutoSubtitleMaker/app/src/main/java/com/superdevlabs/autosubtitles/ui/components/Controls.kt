package com.superdevlabs.autosubtitles.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.superdevlabs.autosubtitles.ui.theme.AppColors

/** Springy press feedback shared by every tappable surface. */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    pressedScale: Float = 0.94f,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role, onClick = onClick)
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val bg = when {
        !enabled -> AppColors.SurfaceHigh
        primary -> AppColors.Lime
        else -> AppColors.Surface
    }
    val fg = when {
        !enabled -> AppColors.Muted
        primary -> AppColors.Ink
        else -> AppColors.Text
    }
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .then(if (primary && enabled) Modifier else Modifier.border(1.dp, AppColors.Line, RoundedCornerShape(18.dp)))
            .pressable(onClick = onClick, enabled = enabled)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.Text,
    background: Color = AppColors.Surface,
    bordered: Boolean = true,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, AppColors.Line, RoundedCornerShape(14.dp)) else Modifier)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted, maxLines = 1)
            }
        }
        actions()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = AppColors.Muted, modifier = modifier)
}

/** Segmented control with a sliding thumb. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Ink)
            .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        val itemWidth = maxWidth / options.size.coerceAtLeast(1)
        val thumbOffset by animateDpAsState(
            targetValue = itemWidth * index,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "thumb",
        )
        Box(
            Modifier
                .offset(x = thumbOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Text),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { option ->
                val isSelected = option == selected
                val color by animateColorAsState(if (isSelected) AppColors.Ink else AppColors.Muted, label = "segColor")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(role = Role.Tab) { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(option), style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun ColorSwatch(color: Color, selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ring by animateColorAsState(if (selected) AppColors.Text else AppColors.Line, label = "ring")
    Box(
        modifier
            .size(44.dp)
            .border(BorderStroke(2.dp, ring), CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .pressable(onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = label,
                tint = if (color.luminance() > 0.5f) AppColors.Ink else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Selectable chip-card used by the style panels. */
@Composable
fun ChoiceCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val border by animateColorAsState(if (selected) AppColors.Lime else AppColors.Line, label = "choiceBorder")
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceHigh)
            .border(2.dp, border, RoundedCornerShape(16.dp))
            .pressable(onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(24.dp))
            .padding(padding),
    ) { content() }
}
