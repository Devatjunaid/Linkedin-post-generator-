package com.odomate.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odomate.app.domain.Level
import com.odomate.app.ocr.OdometerAnalyzer
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppTextField
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.SecondaryButton
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats
import com.odomate.app.util.findActivity
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

private val Amber = Color(0xFFF5A524)
private val Scrim = Color(0xD9181D21)

@Composable
fun ScanScreen(onClose: () -> Unit, onPicked: (Int) -> Unit, onSaved: () -> Unit) {
    val vm = appViewModel { c, h -> ScanViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val ads = LocalAppContainer.current.ads
    val message = LocalMessenger.current
    val unit = LocalSettings.current.distanceUnit
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.scanPhoto(context, uri)
    }
    var torch by remember { mutableStateOf(false) }

    val analyzer = remember { OdometerAnalyzer { tokens, w, h -> vm.onFrame(tokens, w, h) } }
    DisposableEffect(Unit) { onDispose { analyzer.close() } }
    LaunchedEffect(state.locked) {
        analyzer.paused = state.locked
        if (state.locked && !state.fromPhoto && state.text.isNotEmpty()) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreview(analyzer, torch, Modifier.fillMaxSize())
            ScanFrame(Modifier.align(Alignment.Center).padding(bottom = 140.dp), found = state.hits > 0 || state.locked)
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.PhotoCamera, null, tint = Amber, modifier = Modifier.size(48.dp))
                Text("Camera access needed", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    "OdoMate reads the number from your dashboard. Nothing leaves your phone. You can also pick a photo or type the reading.",
                    color = Color(0xFFC9D1D7), style = MaterialTheme.typography.bodyMedium,
                )
                if (askedOnce) {
                    PrimaryButton("Allow camera", { permissionLauncher.launch(Manifest.permission.CAMERA) })
                }
            }
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(onClick = onClose, icon = Icons.Outlined.Close, description = "Close")
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    state.processingPhoto -> "Reading photo…"
                    state.locked -> "Check the number"
                    state.liveValue != null -> "Hold steady…"
                    else -> "Point at your odometer"
                },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clip(CircleShape).background(Scrim).padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Spacer(Modifier.weight(1f))
            RoundIcon(
                onClick = { torch = !torch },
                icon = if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                description = if (torch) "Turn flash off" else "Turn flash on",
                tint = if (torch) Amber else Color.White,
            )
        }

        // Live controls
        if (!state.locked) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                state.liveValue?.let { v ->
                    Text(
                        "${Formats.number(v)} $unit · seen ${state.hits}×",
                        color = Color(0xFF1A1204), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clip(CircleShape).background(Amber).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                if (state.processingPhoto) CircularProgressIndicator(color = Amber)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RoundIcon(
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        icon = Icons.Outlined.PhotoLibrary,
                        description = "Pick a photo",
                        size = 52,
                    )
                    IconButton(
                        onClick = { vm.lock(state.liveValue) },
                        modifier = Modifier.size(78.dp).clip(CircleShape).background(Amber),
                    ) { Icon(Icons.Outlined.PhotoCamera, contentDescription = "Capture reading", tint = Color(0xFF1A1204), modifier = Modifier.size(32.dp)) }
                    RoundIcon(onClick = { vm.lock(null) }, icon = Icons.Outlined.Keyboard, description = "Type the reading", size = 52)
                }
            }
        }

        AnimatedVisibility(
            visible = state.locked,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            ConfirmPanel(
                vm = vm,
                unit = unit,
                onRetake = vm::retake,
                onSave = {
                    if (vm.pickMode) {
                        vm.value?.let(onPicked)
                    } else {
                        scope.launch {
                            val saved = vm.save() ?: return@launch
                            ads.showInterstitial(context.findActivity()) {
                                onSaved()
                                message("Odometer saved: ${Formats.distance(saved, unit)}")
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ConfirmPanel(vm: ScanViewModel, unit: String, onRetake: () -> Unit, onSave: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = AppTheme.colors
    val value = vm.value
    val last = vm.lastReading
    val snapshot = vm.snapshot
    Surface(
        Modifier.fillMaxWidth().imePadding(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = c.surface,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 5.dp).clip(CircleShape).background(c.outline))
            Column {
                Text(
                    when {
                        state.photoFailed -> "Couldn't read that photo"
                        state.text.isEmpty() -> "Type the reading"
                        else -> "Is this right?"
                    },
                    style = MaterialTheme.typography.titleLarge, color = c.text,
                )
                Text(
                    if (state.photoFailed) "Try a sharper, closer photo, or type the number below."
                    else "Tap the number to fix any digit.",
                    color = c.muted, style = MaterialTheme.typography.bodyMedium,
                )
            }
            AppTextField(
                value = state.text,
                onValueChange = vm::setText,
                label = "Odometer",
                suffix = unit,
                keyboardType = KeyboardType.Number,
                mono = true,
            )
            if (value != null && last != null) {
                val diff = value - last
                val lastDate = snapshot?.lastReading?.date
                when {
                    diff < 0 -> InfoRow(
                        Icons.Outlined.WarningAmber, c.danger, c.dangerSoft,
                        "That's ${Formats.distance(abs(diff), unit)} lower than your last reading. Check the digits.",
                    )
                    diff > 30_000 -> InfoRow(
                        Icons.Outlined.WarningAmber, c.warn, c.warnSoft,
                        "That's ${Formats.distance(diff, unit)} more than last time. Make sure it isn't the trip meter.",
                    )
                    else -> InfoRow(
                        Icons.Outlined.Check, c.ok, c.okSoft,
                        "+${Formats.distance(diff, unit)} since your last reading" +
                            (lastDate?.let { " (${Formats.shortDate(it)})" } ?: "") + ". Looks normal.",
                    )
                }
            }
            if (value != null && snapshot != null) {
                val next = snapshot.statuses
                    .filter { it.dueOdometer != null && it.level != Level.DISABLED && it.level != Level.UNKNOWN }
                    .minByOrNull { (it.dueOdometer ?: 0) - value }
                next?.let { s ->
                    val left = (s.dueOdometer ?: 0) - value
                    InfoRow(
                        com.odomate.app.ui.components.serviceIcon(s.type.iconKey),
                        if (left < 0) c.danger else c.warn,
                        if (left < 0) c.dangerSoft else c.warnSoft,
                        if (left < 0) "${s.type.name} would be ${Formats.distance(-left, unit)} overdue."
                        else "This puts your ${s.type.name.lowercase()} ${Formats.distance(left, unit)} away.",
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Retake", onRetake)
                PrimaryButton(
                    if (vm.pickMode) "Use reading" else "Save reading",
                    onSave,
                    Modifier.weight(1f),
                    enabled = value != null && value > 0,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color, text: String) {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = c.text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RoundIcon(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = Color.White,
    size: Int = 46,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Scrim),
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

/** Corner-bracket scan window with a sweeping scan line. */
@Composable
private fun ScanFrame(modifier: Modifier, found: Boolean) {
    val transition = rememberInfiniteTransition(label = "scan")
    val sweep by transition.animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "sweep",
    )
    val color = if (found) Amber else Color.White
    Canvas(modifier.size(width = 300.dp, height = 150.dp)) {
        val len = 28.dp.toPx()
        val w = 4.dp.toPx()
        val r = size
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + dx * len, y), w, StrokeCap.Round)
            drawLine(color, Offset(x, y), Offset(x, y + dy * len), w, StrokeCap.Round)
        }
        corner(0f, 0f, 1f, 1f)
        corner(r.width, 0f, -1f, 1f)
        corner(0f, r.height, 1f, -1f)
        corner(r.width, r.height, -1f, -1f)
        val y = r.height * sweep
        drawLine(color.copy(alpha = 0.55f), Offset(16.dp.toPx(), y), Offset(r.width - 16.dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun CameraPreview(analyzer: OdometerAnalyzer, torch: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            val p = runCatching { future.get() }.getOrNull() ?: return@addListener
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                        )
                        .build()
                )
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            runCatching {
                p.unbindAll()
                camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
        }
    }
    LaunchedEffect(camera, torch) {
        camera?.let { cam -> if (cam.cameraInfo.hasFlashUnit()) cam.cameraControl.enableTorch(torch) }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
