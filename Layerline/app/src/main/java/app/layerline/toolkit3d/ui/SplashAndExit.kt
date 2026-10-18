package app.layerline.toolkit3d.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.layerline.toolkit3d.BuildConfig
import app.layerline.toolkit3d.ui.components.ButtonStyle
import app.layerline.toolkit3d.ui.components.Chamfer
import app.layerline.toolkit3d.ui.components.CoolingHotend
import app.layerline.toolkit3d.ui.components.GcodeTag
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LayerlineMark
import app.layerline.toolkit3d.ui.components.bedDots
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val bootLines = listOf(
    "; G28 — homing all axes",
    "; M104 S220 — heating nozzle",
    "; M190 S60 — warming bed",
    "; M73 P100 — workshop ready",
)

/**
 * Animated launch screen: the nozzle builds the logo layer by layer, the wordmark is "printed"
 * letter by letter, and a boot log types out G-code while a filament bar fills.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val build = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val glowT = rememberInfiniteTransition(label = "splashGlow")
    val glow by glowT.animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "g")

    LaunchedEffect(Unit) {
        launch { build.animateTo(1f, tween(2000, easing = LinearEasing)) }
        delay(1250)
        word.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        delay(450)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(LL.Graphite),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        val s = 0.85f + 0.15f * build.value.coerceAtMost(0.3f) / 0.3f
                        scaleX = s
                        scaleY = s
                    },
            ) { LayerlineMark(build.value, Modifier.fillMaxSize(), glow = glow) }

            Spacer(Modifier.height(18.dp))
            Row {
                val letters = "LAYERLINE"
                letters.forEachIndexed { i, c ->
                    val start = i / letters.length.toFloat() * 0.6f
                    val a = ((word.value - start) / 0.4f).coerceIn(0f, 1f)
                    Text(
                        c.toString(),
                        style = LLType.display(40.sp).copy(letterSpacing = 2.sp),
                        modifier = Modifier.graphicsLayer {
                            alpha = a
                            translationY = (1f - a) * 28f
                        },
                    )
                }
            }
            Text(
                "3D PRINT TOOLKIT",
                style = LLType.mono(12.sp, FontWeight.Medium, LL.Lime).copy(letterSpacing = (2 + 4 * word.value).sp),
                modifier = Modifier.graphicsLayer { alpha = word.value },
            )

            Spacer(Modifier.height(36.dp))
            val lineIndex = (build.value * bootLines.size).toInt().coerceAtMost(bootLines.lastIndex)
            val line = bootLines[lineIndex]
            val local = (build.value * bootLines.size - lineIndex).coerceIn(0f, 1f)
            val shown = line.take((line.length * (local * 1.6f).coerceAtMost(1f)).roundToInt())
            Text(shown, style = LLType.mono(12.sp, color = LL.Muted), modifier = Modifier.height(18.dp))
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.width(180.dp).height(4.dp)) {
                drawRect(LL.Line, size = size)
                drawRect(LL.Lime, size = Size(size.width * build.value, size.height))
                drawCircle(LL.Hot, radius = 3.dp.toPx(), center = Offset(size.width * build.value, size.height / 2))
            }
        }
        Text(
            "v${BuildConfig.VERSION_NAME}",
            style = LLType.mono(10.sp, color = LL.LineStrong),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        )
    }
}

/**
 * Shown when the user presses back on the Workbench. The hotend "cools down" while the user
 * decides. No ads are shown here or on exit.
 */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val appear = remember { Animatable(0f) }
        val heat = remember { Animatable(1f) }
        val wave = rememberInfiniteTransition(label = "shimmer")
        val phase by wave.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "phase")
        LaunchedEffect(Unit) {
            launch { appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
            heat.animateTo(0f, tween(2600, easing = FastOutSlowInEasing))
        }
        val temp = (40 + 180 * heat.value).roundToInt()

        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .graphicsLayer {
                    alpha = appear.value.coerceIn(0f, 1f)
                    val s = 0.85f + 0.15f * appear.value
                    scaleX = s
                    scaleY = s
                    rotationZ = (1f - appear.value) * -3f
                }
                .clip(Chamfer)
                .background(LL.Panel, Chamfer)
                .border(1.dp, LL.LineStrong, Chamfer)
                .bedDots(LL.Dot.copy(alpha = 0.6f))
                .padding(20.dp),
        ) {
            GcodeTag("; M84 — PARK & POWER DOWN")
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CoolingHotend(heat.value, phase, Modifier.size(96.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("NOZZLE", style = LLType.mono(10.sp, color = LL.Muted))
                    Text(
                        "$temp°",
                        style = LLType.display(44.sp, color = if (heat.value > 0.3f) LL.Hot else LL.Soft),
                    )
                    Text(if (heat.value > 0.05f) "cooling down…" else "safe to leave", style = LLType.mono(11.sp, color = LL.Muted))
                }
            }
            Text("Leaving the workshop?", style = LLType.display(24.sp), modifier = Modifier.padding(top = 14.dp))
            Text(
                "Your spools, quotes, calibrations and print log stay saved on this phone.",
                style = LLType.body(14.sp, color = LL.Soft),
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LButton("Stay", onStay, Modifier.weight(1f))
                LButton("Exit", onExit, Modifier.weight(1f), style = ButtonStyle.Ghost)
            }
        }
    }
}
