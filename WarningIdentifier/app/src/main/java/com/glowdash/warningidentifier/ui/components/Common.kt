package com.glowdash.warningidentifier.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.ads.AdsManager
import com.glowdash.warningidentifier.data.Severity
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Soft coloured light behind content (radial gradient, works on every Android version). */
@Composable
fun GlowBlob(color: Color, size: Dp, modifier: Modifier = Modifier, alpha: Float = 0.30f) {
    Box(
        modifier
            .size(size)
            .background(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent)), CircleShape),
    )
}

/** Dark screen with two subtle glows. */
@Composable
fun ScreenBackground(
    topGlow: Color = WiColors.Violet,
    bottomGlow: Color = WiColors.Cyan,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(WiColors.Bg)) {
        GlowBlob(topGlow, 420.dp, Modifier.align(Alignment.TopEnd).offset(x = 150.dp, y = (-170).dp), alpha = 0.28f)
        GlowBlob(bottomGlow, 380.dp, Modifier.align(Alignment.BottomStart).offset(x = (-170).dp, y = (-60).dp), alpha = 0.16f)
        content()
    }
}

/** Gentle press-to-shrink feedback used on every tappable card and button. */
@Composable
fun rememberBounce(): Pair<MutableInteractionSource, Float> {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")
    return source to scale
}

@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    background: Color = WiColors.Surface,
    border: Color? = WiColors.Stroke,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val (source, scale) = rememberBounce()
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(BorderStroke(1.dp, border), shape) else Modifier)
            .clickable(
                interactionSource = source,
                indication = androidx.compose.material3.ripple(color = WiColors.TextPrimary),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        content = content,
    )
}

@Composable
fun RoundIconButton(
    path: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = WiColors.TextPrimary,
) {
    Pressable(onClick = onClick, modifier = modifier.size(44.dp), shape = RoundedCornerShape(14.dp)) {
        PathIcon(
            path = path, color = tint, size = 20.dp, strokeWidth = 2.2f,
            contentDescription = contentDescription, modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun SeverityPill(severity: Severity, modifier: Modifier = Modifier, blink: Boolean = false) {
    val alpha = if (blink) {
        val t = rememberInfiniteTransition(label = "blink")
        t.animateFloat(1f, 0.45f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a").value
    } else 1f
    Text(
        text = severity.label,
        style = WiType.Label.copy(color = severity.onColor),
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(99.dp))
            .background(severity.color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    background: Color = WiColors.Surface,
    border: Color = WiColors.Stroke,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) { content() }
}

/** Big call-to-action button with a light sweep animation. */
@Composable
fun ShineButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = WiColors.Cyan,
    contentColor: Color = WiColors.OnCyan,
) {
    val t = rememberInfiniteTransition(label = "shine")
    val sweep by t.animateFloat(
        -0.4f, 1.4f,
        infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "sweep",
    )
    Pressable(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .shadow(18.dp, RoundedCornerShape(20.dp), ambientColor = color, spotColor = color),
        shape = RoundedCornerShape(20.dp),
        background = color,
        border = null,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val x = size.width * sweep
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                            start = Offset(x - size.width * 0.15f, 0f),
                            end = Offset(x + size.width * 0.15f, size.height),
                        ),
                    )
                },
        )
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PathIcon(icon, contentColor, size = 22.dp, strokeWidth = 2.2f)
            Text(text, style = WiType.Button.copy(color = contentColor))
        }
    }
}

/** Full-screen "thinking" overlay with a spinning ring. */
@Composable
fun AnalyzingOverlay(visible: Boolean, text: String = "Analyzing…") {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        val t = rememberInfiniteTransition(label = "spin")
        val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "a")
        Box(
            Modifier
                .fillMaxSize()
                .background(WiColors.Bg.copy(alpha = 0.82f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(64.dp)) {
                    drawCircle(WiColors.Stroke, style = Stroke(5.dp.toPx()))
                    drawArc(
                        WiColors.Cyan, angle, 100f, false,
                        style = Stroke(5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(text, style = WiType.Section)
                Text("On-device AI · no internet needed", style = WiType.Caption)
            }
        }
    }
}

enum class NavTab { HOME, LIBRARY, HISTORY }

/** Floating bottom navigation with a raised scan button. */
@Composable
fun FloatingNavBar(
    current: NavTab,
    onTab: (NavTab) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val t = rememberInfiniteTransition(label = "float")
    val bob by t.animateFloat(0f, -5f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob")

    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(WiColors.Surface.copy(alpha = 0.96f))
                .border(1.dp, WiColors.Stroke, RoundedCornerShape(24.dp)),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(UiIcons.Home, "Home", current == NavTab.HOME) { onTab(NavTab.HOME) }
            NavItem(UiIcons.Grid, "Library", current == NavTab.LIBRARY) { onTab(NavTab.LIBRARY) }
            Spacer(Modifier.size(58.dp))
            NavItem(UiIcons.History, "History", current == NavTab.HISTORY) { onTab(NavTab.HISTORY) }
            NavItem(UiIcons.Settings, "Settings", false) { showSettings = true }
        }
        Pressable(
            onClick = onScan,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp + bob.dp)
                .size(58.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = WiColors.Pink, spotColor = WiColors.Pink),
            shape = RoundedCornerShape(20.dp),
            background = WiColors.Pink,
            border = null,
        ) {
            PathIcon(
                UiIcons.Scan, Color.White, size = 26.dp, strokeWidth = 2.2f,
                contentDescription = "Scan", modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    if (showSettings) SettingsDialog(onDismiss = { showSettings = false })
}

@Composable
private fun NavItem(path: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) WiColors.Cyan else WiColors.TextSecondary
    Pressable(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(16.dp),
        background = if (selected) WiColors.Cyan.copy(alpha = 0.14f) else Color.Transparent,
        border = null,
    ) {
        PathIcon(path, tint, size = 22.dp, contentDescription = label, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WiColors.Surface,
        title = { Text("Warning Identifier", style = WiType.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Identifies dashboard warning lights with on-device AI. Photos never leave your phone and the app works offline.",
                    style = WiType.BodySmall,
                )
                Text(
                    "Information is general guidance only. Always check your owner's manual and consult a qualified mechanic.",
                    style = WiType.BodySmall.copy(color = WiColors.TextMuted),
                )
                if (AdsManager.privacyOptionsRequired && activity != null) {
                    TextButton(onClick = { AdsManager.showPrivacyOptions(activity) }) {
                        Text("Privacy & ad choices", style = WiType.CardTitle.copy(color = WiColors.Cyan))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", style = WiType.CardTitle.copy(color = WiColors.Cyan))
            }
        },
    )
}
