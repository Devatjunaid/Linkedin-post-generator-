package com.snaptensio.bplog.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptensio.bplog.ui.components.AnimatedLogo
import com.snaptensio.bplog.ui.theme.Brand
import com.snaptensio.bplog.ui.theme.Manrope
import kotlinx.coroutines.delay

/**
 * Branded splash: expanding "sonar" rings, the animated mark, then the name and tagline rising in.
 * No ads are shown here.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(450)
        title.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(700)
        tagline.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        bar.animateTo(1f, tween(1700, easing = LinearEasing))
        delay(150)
        onFinished()
    }
    val rings = rememberInfiniteTransition(label = "rings")
    val ring by rings.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "ring")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand.BlueBright, Brand.Blue, Brand.BlueDeep))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2 - 60.dp.toPx())
            for (i in 0..2) {
                val p = (ring + i / 3f) % 1f
                drawCircle(Color.White.copy(alpha = 0.18f * (1 - p)), radius = 70.dp.toPx() + p * size.width * 0.6f, center = c, style = Stroke(2.dp.toPx()))
            }
            // Subtle ECG trace along the bottom.
            val y = size.height * 0.82f
            val w = size.width
            val pts = listOf(0f to 0f, 0.38f to 0f, 0.42f to -18f, 0.46f to 22f, 0.5f to -40f, 0.54f to 12f, 0.58f to 0f, 1f to 0f)
            for (k in 0 until pts.size - 1) {
                drawLine(
                    Color.White.copy(alpha = 0.16f),
                    Offset(w * pts[k].first, y + pts[k].second.dp.toPx()),
                    Offset(w * pts[k + 1].first, y + pts[k + 1].second.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 120.dp)) {
            AnimatedLogo(size = 150.dp, tile = false)
            Spacer(Modifier.height(18.dp))
            Text(
                "SnapTensio",
                color = Color.White,
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1 - title.value) * 24.dp.toPx()
                },
            )
            Text(
                "Snap your monitor. Know your numbers.",
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = Manrope,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1 - tagline.value) * 18.dp.toPx()
                },
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f))) {
                Box(Modifier.fillMaxWidth(bar.value).height(4.dp).background(Color.White))
            }
            Text(
                "Private · Offline · Free",
                color = Color.White.copy(alpha = 0.75f),
                fontFamily = Manrope,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(tagline.value),
            )
        }
    }
}
