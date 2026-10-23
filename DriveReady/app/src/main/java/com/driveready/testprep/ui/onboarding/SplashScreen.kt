package com.driveready.testprep.ui.onboarding

import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveready.testprep.R
import com.driveready.testprep.ads.AdsManager
import com.driveready.testprep.data.content.Countries
import com.driveready.testprep.data.content.UsStates
import com.driveready.testprep.ui.components.BrandLogo
import com.driveready.testprep.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val MIN_SPLASH_MS = 2_400L
private const val MAX_CONSENT_WAIT_MS = 8_000L

/**
 * Animated brand splash. While it plays, consent is gathered and the ads SDK is initialised;
 * it continues after both the minimum display time and consent (or a timeout) have passed.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val activity = LocalActivity.current
    var consentDone by remember { mutableStateOf(false) }
    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val chipsAlpha = remember { Animatable(0f) }
    val loader = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (activity != null) AdsManager.gatherConsentAndInit(activity) { consentDone = true } else consentDone = true
    }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(450)
        textAlpha.animateTo(1f, tween(500))
        chipsAlpha.animateTo(1f, tween(500))
    }
    LaunchedEffect(Unit) {
        loader.animateTo(0.85f, tween(MIN_SPLASH_MS.toInt(), easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_MS)
        withTimeoutOrNull(MAX_CONSENT_WAIT_MS) {
            while (!consentDone) delay(100)
        }
        loader.animateTo(1f, tween(250))
        onFinished()
    }

    val roadShift by rememberInfiniteTransition(label = "road").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "roadShift"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand.NavyLight, Brand.Navy, Color(0xFF070D18))))
    ) {
        // Perspective road receding to the horizon with moving lane dashes.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizon = h * 0.58f
            val road = Path().apply {
                moveTo(w * 0.47f, horizon); lineTo(w * 0.53f, horizon)
                lineTo(w * 1.1f, h); lineTo(-w * 0.1f, h); close()
            }
            drawPath(road, Color(0x332A3650))
            drawLine(Color(0x55FFFFFF), Offset(w * 0.47f, horizon), Offset(-w * 0.1f, h), 3f)
            drawLine(Color(0x55FFFFFF), Offset(w * 0.53f, horizon), Offset(w * 1.1f, h), 3f)
            for (i in 0 until 8) {
                val t0 = ((i + roadShift) / 8f)
                val t1 = t0 + 0.05f
                val y0 = horizon + (h - horizon) * t0 * t0
                val y1 = horizon + (h - horizon) * (t1 * t1).coerceAtMost(1f)
                drawLine(Brand.Amber.copy(alpha = 0.75f), Offset(w / 2, y0), Offset(w / 2, y1), 2f + 10f * t0)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.8f))
            BrandLogo(
                size = 132.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value; scaleY = logoScale.value; alpha = logoAlpha.value
                }
            )
            Spacer(Modifier.height(26.dp))
            Column(Modifier.graphicsLayer { alpha = textAlpha.value }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.app_tagline).uppercase(),
                    color = Brand.Amber,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(22.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = chipsAlpha.value }
            ) {
                listOf("Theory", "Road signs", "Hazard", "Oral", "Practical").forEach {
                    Text(
                        it,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x26FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${Countries.size} countries · ${UsStates.size - 1} US states & DC",
                color = Color(0xFFC9CBD1),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.graphicsLayer { alpha = chipsAlpha.value }
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(loader.value)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brand.Amber)
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
