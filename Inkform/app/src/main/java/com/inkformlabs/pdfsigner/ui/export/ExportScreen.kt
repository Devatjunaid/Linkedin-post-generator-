package com.inkformlabs.pdfsigner.ui.export

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inkformlabs.pdfsigner.ads.NativeAdCard
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.BusyOverlay
import com.inkformlabs.pdfsigner.ui.components.ButtonKind
import com.inkformlabs.pdfsigner.ui.components.CircleIconButton
import com.inkformlabs.pdfsigner.ui.components.InkButton
import com.inkformlabs.pdfsigner.ui.components.InkCard
import com.inkformlabs.pdfsigner.ui.components.ToggleRow
import com.inkformlabs.pdfsigner.ui.components.inkTextFieldColors
import com.inkformlabs.pdfsigner.ui.nav.AppViewModel
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ExportScreen(documentId: String, appViewModel: AppViewModel) {
    val p = LocalInk.current
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val vm: ExportViewModel = viewModel(factory = ExportViewModel.factory(documentId, container))
    val doc by vm.document.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    BackHandler { appViewModel.pop() }

    // Circular reveal growing from the bottom, where the "Done" action was.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(850, easing = CubicBezierEasing(0.65f, 0f, 0.25f, 1f))) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            vm.saveTo(context, uri) {
                // Saving the final file is a major task: rate-limited interstitial at this natural break.
                container.ads.showInterstitialThen(activity) {}
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                clip = true
                shape = GenericShape { size, _ ->
                    val radius = hypot(size.width, size.height) * reveal.value
                    val center = Offset(size.width / 2f, size.height * 0.92f)
                    addOval(Rect(center, radius))
                }
            }
            .background(p.bg),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(InkIcons.Back, "Back to editor", onClick = { appViewModel.pop() })
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.surface2)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(InkIcons.Lock, contentDescription = null, tint = p.hi, modifier = Modifier.size(15.dp))
                    Text("0 bytes uploaded", style = MaterialTheme.typography.labelMedium, color = p.ink)
                }
            }

            SuccessBurst(Modifier.fillMaxWidth().height(200.dp), replayKey = vm.savedCount)

            Text(
                if (doc?.signed == true) "Signed & ready" else "Ready to save",
                style = MaterialTheme.typography.headlineMedium,
                color = p.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Everything happened on this phone. Choose how to save your PDF.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = vm.fileName,
                onValueChange = { vm.fileName = it.take(100) },
                label = { Text("File name") },
                suffix = { Text(".pdf") },
                singleLine = true,
                colors = inkTextFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            InkCard(padding = 14.dp) {
                Column {
                    ToggleRow("Flatten form fields", "Lock values so they can't be edited", vm.flatten, { vm.flatten = it })
                    HorizontalDivider(color = p.line)
                    ToggleRow("Remove hidden metadata", "Strip author, app and edit details", vm.removeMetadata, { vm.removeMetadata = it })
                    HorizontalDivider(color = p.line)
                    ToggleRow("Password protect", "AES-256 encryption; the password is never stored", vm.protect, { vm.protect = it })
                    AnimatedVisibility(visible = vm.protect, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column(Modifier.padding(top = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vm.password,
                                onValueChange = { vm.password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = inkTextFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = vm.confirmPassword,
                                onValueChange = { vm.confirmPassword = it },
                                label = { Text("Confirm password") },
                                singleLine = true,
                                isError = vm.passwordProblem != null && vm.confirmPassword.isNotEmpty(),
                                supportingText = { vm.passwordProblem?.let { Text(it) } },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = inkTextFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "Keep this password safe. Inkform can't recover it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = p.muted,
                            )
                        }
                    }
                }
            }

            InkButton(
                "Save to device",
                onClick = { createDocument.launch(vm.outputName) },
                enabled = vm.canExport,
                icon = InkIcons.Download,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InkButton("Share", onClick = { vm.share(context) }, enabled = vm.canExport, kind = ButtonKind.Secondary, icon = InkIcons.Share, modifier = Modifier.weight(1f))
                InkButton("Print", onClick = { vm.print(context) }, enabled = !vm.busy, kind = ButtonKind.Secondary, icon = InkIcons.Print, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = { appViewModel.popToMain() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Back to documents", color = p.hi, style = MaterialTheme.typography.labelLarge)
            }

            NativeAdCard()
            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        if (vm.busy) BusyOverlay("Preparing your PDF…")
    }
}

/** Check mark that pops and draws itself, with burst ring, orbiting ink dots and a floating signed page. */
@Composable
private fun SuccessBurst(modifier: Modifier, replayKey: Int) {
    val p = LocalInk.current
    val pop = remember(replayKey) { Animatable(0f) }
    val draw = remember(replayKey) { Animatable(0f) }
    val burst = remember(replayKey) { Animatable(0f) }
    LaunchedEffect(replayKey) {
        kotlinx.coroutines.delay(if (replayKey == 0) 450 else 0)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(replayKey) {
        kotlinx.coroutines.delay(if (replayKey == 0) 800 else 250)
        draw.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(replayKey) {
        kotlinx.coroutines.delay(if (replayKey == 0) 600 else 100)
        burst.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }
    val infinite = rememberInfiniteTransition(label = "orbit")
    val angle by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(16_000, easing = LinearEasing)), label = "angle")
    val floatY by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(4_500, easing = LinearEasing)), label = "float")
    val checkPath = remember { Path() }
    val measure = remember { PathMeasure() }
    val partial = remember { Path() }

    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.height * 0.48f
        val inner = outer * 0.72f
        val core = outer * 0.47f

        rotate(angle, pivot = c) {
            drawCircle(p.line, outer, c, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
            drawCircle(p.accent2, 6.dp.toPx(), Offset(c.x, c.y - outer))
            drawCircle(p.hi, 4.5.dp.toPx(), Offset(c.x - outer * 0.87f, c.y + outer * 0.5f))
            drawRect(p.accent, topLeft = Offset(c.x + outer * 0.8f, c.y + outer * 0.45f), size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 12.dp.toPx()))
        }
        rotate(-angle * 1.5f, pivot = c) {
            drawCircle(p.line, inner, c, style = Stroke(1.dp.toPx()))
            drawCircle(p.ink, 4.dp.toPx(), Offset(c.x + inner, c.y))
        }
        // Burst ring
        if (burst.value in 0.01f..0.99f) {
            drawCircle(p.accent.copy(alpha = 0.45f * (1f - burst.value)), core * (1f + burst.value * 1.2f), c)
        }
        // Core with check
        scale(pop.value, pivot = c) {
            drawCircle(p.accent, core, c)
            checkPath.reset()
            checkPath.moveTo(c.x - core * 0.42f, c.y + core * 0.02f)
            checkPath.lineTo(c.x - core * 0.1f, c.y + core * 0.32f)
            checkPath.lineTo(c.x + core * 0.45f, c.y - core * 0.32f)
            measure.setPath(checkPath, false)
            partial.reset()
            measure.getSegment(0f, measure.length * draw.value, partial, true)
            drawPath(partial, p.onAccent, style = Stroke(core * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        // Floating signed page
        val bob = sin(floatY * 2 * Math.PI).toFloat() * 6.dp.toPx()
        val pageW = 46.dp.toPx()
        val pageH = 58.dp.toPx()
        val pageTopLeft = Offset(c.x + outer * 0.62f, c.y - outer * 0.9f + bob)
        rotate(-8f + 3f * cos(floatY * 2 * Math.PI).toFloat(), pivot = pageTopLeft) {
            drawRoundRect(Color.White, pageTopLeft, androidx.compose.ui.geometry.Size(pageW, pageH), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            drawRect(Color(0xFFD9D6CD), Offset(pageTopLeft.x + 7.dp.toPx(), pageTopLeft.y + 9.dp.toPx()), androidx.compose.ui.geometry.Size(26.dp.toPx(), 4.dp.toPx()))
            drawRect(Color(0xFFECEAE3), Offset(pageTopLeft.x + 7.dp.toPx(), pageTopLeft.y + 18.dp.toPx()), androidx.compose.ui.geometry.Size(32.dp.toPx(), 3.dp.toPx()))
            val sig = Path().apply {
                val x0 = pageTopLeft.x + 7.dp.toPx()
                val y0 = pageTopLeft.y + 42.dp.toPx()
                val u = 1.dp.toPx()
                moveTo(x0, y0)
                cubicTo(x0 + 4 * u, y0 - 9 * u, x0 + 7 * u, y0 - 9 * u, x0 + 9 * u, y0 - 2 * u)
                cubicTo(x0 + 11 * u, y0 + 4 * u, x0 + 15 * u, y0 + 1 * u, x0 + 18 * u, y0 - 5 * u)
                cubicTo(x0 + 21 * u, y0 - 10 * u, x0 + 25 * u, y0 - 4 * u, x0 + 31 * u, y0 - 8 * u)
            }
            drawPath(sig, Color(0xFF1F3FBF), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
