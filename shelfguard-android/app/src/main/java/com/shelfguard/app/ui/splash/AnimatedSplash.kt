package com.shelfguard.app.ui.splash

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfguard.app.BuildConfig
import com.shelfguard.app.ui.components.LogoParts
import com.shelfguard.app.ui.components.ShelfGuardLogo
import com.shelfguard.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val APP_NAME = "ShelfGuard"

/**
 * Animated splash. The system splash (same background) hands over to this instantly:
 * the tile springs in, the box faces slide together, the tape seals the lid, the clock
 * badge pops and its hand sweeps, then the name types in letter by letter.
 */
@Composable
fun AnimatedSplash(onFinished: () -> Unit) {
    val tile = remember { Animatable(0f) }
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    val top = remember { Animatable(0f) }
    val tape = remember { Animatable(0f) }
    val badge = remember { Animatable(0f) }
    val hand = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { tile.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        delay(180)
        launch { left.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        delay(90)
        launch { right.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        delay(90)
        launch { top.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        delay(380)
        launch { tape.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
        delay(220)
        launch { badge.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow)) }
        launch { hand.animateTo(360f, tween(1100, easing = FastOutSlowInEasing)) }
        delay(160)
        launch { title.animateTo(1f, tween(650, easing = LinearEasing)) }
        delay(450)
        tagline.animateTo(1f, tween(450))
        delay(450)
        onFinished()
    }

    val pulse = rememberInfiniteTransition(label = "glow")
    val glow by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowScale",
    )
    val shimmer by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmer",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.Deep)
            .semantics { contentDescription = "ShelfGuard is starting" },
        contentAlignment = Alignment.Center,
    ) {
        // Breathing glow behind the logo.
        Canvas(Modifier.size(360.dp).graphicsLayer { scaleX = glow; scaleY = glow; alpha = tile.value }) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0x5529D88F), Color(0x0029D88F)),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.minDimension / 2,
                ),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ShelfGuardLogo(
                modifier = Modifier.size(148.dp),
                parts = LogoParts(
                    tile = tile.value,
                    top = top.value,
                    left = left.value,
                    right = right.value,
                    tape = tape.value,
                    badge = badge.value,
                    handAngle = hand.value,
                ),
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                APP_NAME.forEachIndexed { i, ch ->
                    // Each letter rises and fades in, slightly after the previous one.
                    val p = (title.value * APP_NAME.length - i).coerceIn(0f, 1f)
                    Text(
                        ch.toString(),
                        color = if (i < 5) Color.White else Brand.Mint,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = p
                            translationY = (1f - p) * 18.dp.toPx()
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Stock & expiry, under control",
                color = Brand.MintPale.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 10.dp.toPx()
                },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp)
                .alpha(tagline.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Slim loading track with a moving highlight.
            Box(
                Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.35f)
                        .height(4.dp)
                        .graphicsLayer { translationX = (shimmer * 1.65f - 0.35f) * 120.dp.toPx() }
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brand.Amber),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "v${BuildConfig.VERSION_NAME}",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )
        }
    }
}
