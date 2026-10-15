package com.glowdash.warningidentifier.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.glowdash.warningidentifier.ads.AdsManager
import com.glowdash.warningidentifier.ai.BitmapUtils
import com.glowdash.warningidentifier.ai.LightClassifier
import com.glowdash.warningidentifier.ai.ScanSession
import com.glowdash.warningidentifier.ui.components.AnalyzingOverlay
import com.glowdash.warningidentifier.ui.components.PathIcon
import com.glowdash.warningidentifier.ui.components.Pressable
import com.glowdash.warningidentifier.ui.components.RoundIconButton
import com.glowdash.warningidentifier.ui.components.ShineButton
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.components.findActivity
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LiveHint(val text: String, val color: Color)

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onResult: () -> Unit,
    onManualSearch: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val classifier = remember { LightClassifier.get(context) }
    val main = remember { Handler(Looper.getMainLooper()) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) requestPermission.launch(Manifest.permission.CAMERA) }

    var torchOn by remember { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }
    var hints by remember { mutableStateOf<List<LiveHint>>(emptyList()) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    fun finishWith(bitmap: Bitmap?) {
        if (bitmap == null) {
            analyzing = false
            Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val analysis = withContext(Dispatchers.Default) { classifier.analyze(bitmap) }
            ScanSession.latest = analysis
            analyzing = false
            AdsManager.showInterstitialThen(activity) { onResult() }
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        analyzing = true
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { BitmapUtils.decode(context, uri) }
            finishWith(bmp)
        }
    }

    fun capture() {
        if (analyzing) return
        analyzing = true
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = runCatching {
                        BitmapUtils.cropCenter(BitmapUtils.rotate(image.toBitmap(), image.imageInfo.rotationDegrees))
                    }.getOrNull()
                    image.close()
                    finishWith(bmp)
                }

                override fun onError(exception: ImageCaptureException) {
                    analyzing = false
                    Toast.makeText(context, "Capture failed, try again", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreview(
                torchOn = torchOn,
                imageCapture = imageCapture,
                onFrame = { frame ->
                    val a = classifier.analyze(BitmapUtils.cropCenter(frame))
                    val list = if (a.isConfident) {
                        val best = a.best!!
                        listOf(LiveHint("${best.light.name} · ${(best.confidence * 100).toInt()}%", best.light.color.color))
                    } else {
                        a.colors.take(3).map { LiveHint("${it.label} light spotted", it.color) }
                    }
                    main.post { hints = list }
                },
                modifier = Modifier.fillMaxSize(),
            )
            ScanFrameOverlay(Modifier.fillMaxSize())
        } else {
            PermissionCard(
                onGrant = { requestPermission.launch(Manifest.permission.CAMERA) },
                onGallery = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(UiIcons.Back, "Back", onBack)
            Spacer(Modifier.weight(1f))
            OfflineBadge()
            Spacer(Modifier.weight(1f))
            RoundIconButton(
                UiIcons.Flash, if (torchOn) "Turn flash off" else "Turn flash on",
                onClick = { torchOn = !torchOn },
                tint = if (torchOn) WiColors.Amber else WiColors.TextPrimary,
            )
        }

        if (hasPermission) {
            Text(
                "Hold steady over the lit symbols",
                style = WiType.Section.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 78.dp),
            )
        }

        BottomPanel(
            hints = hints,
            onGallery = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onCapture = { if (hasPermission) capture() else requestPermission.launch(Manifest.permission.CAMERA) },
            onSearch = onManualSearch,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnalyzingOverlay(analyzing)
    }
}

@Composable
private fun OfflineBadge() {
    val t = rememberInfiniteTransition(label = "badge")
    val a by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(WiColors.Lime.copy(alpha = 0.12f))
            .border(1.dp, WiColors.Lime.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(WiColors.Lime))
        Spacer(Modifier.width(8.dp))
        Text("ON-DEVICE AI · OFFLINE", style = WiType.Label.copy(color = Color(0xFFC5E59A)))
    }
}

/** Corner brackets + sweeping scan line. */
@Composable
private fun ScanFrameOverlay(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "y")
    Canvas(modifier) {
        val left = 28.dp.toPx()
        val top = size.height * 0.20f
        val w = size.width - left * 2
        val h = size.height * 0.46f
        val arm = 40.dp.toPx()
        val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
        val c = WiColors.Cyan
        // Dim everything outside the window.
        drawRect(Color.Black.copy(alpha = 0.35f), size = Size(size.width, top))
        drawRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, top + h), size = Size(size.width, size.height - top - h))
        // Brackets
        listOf(
            Triple(Offset(left, top), 1f, 1f),
            Triple(Offset(left + w, top), -1f, 1f),
            Triple(Offset(left, top + h), 1f, -1f),
            Triple(Offset(left + w, top + h), -1f, -1f),
        ).forEach { (p, sx, sy) ->
            drawLine(c, p, Offset(p.x + arm * sx, p.y), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(c, p, Offset(p.x, p.y + arm * sy), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        // Scan line with a soft trail
        val ly = top + 12.dp.toPx() + (h - 24.dp.toPx()) * y
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, c.copy(alpha = 0.22f)), startY = ly - 60.dp.toPx(), endY = ly),
            topLeft = Offset(left + 8.dp.toPx(), ly - 60.dp.toPx()),
            size = Size(w - 16.dp.toPx(), 60.dp.toPx()),
        )
        drawRoundRect(
            c, topLeft = Offset(left + 8.dp.toPx(), ly - 1.5.dp.toPx()),
            size = Size(w - 16.dp.toPx(), 3.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()),
        )
    }
}

@Composable
private fun BottomPanel(
    hints: List<LiveHint>,
    onGallery: () -> Unit,
    onCapture: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = rememberInfiniteTransition(label = "shutter")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ring")

    Column(
        modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(WiColors.Surface.copy(alpha = 0.95f))
            .border(1.dp, WiColors.Stroke, RoundedCornerShape(28.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (hints.isEmpty()) "Looking for lit symbols…" else "Live detection",
                style = WiType.Section.copy(fontSize = WiType.CardTitle.fontSize),
                modifier = Modifier.weight(1f),
            )
            Text("Tap the shutter to identify", style = WiType.Caption)
        }
        AnimatedVisibility(hints.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                hints.forEach { hint ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(hint.color.copy(alpha = 0.12f))
                            .border(1.dp, hint.color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(hint.color))
                        Spacer(Modifier.width(7.dp))
                        Text(hint.text, style = WiType.Caption.copy(color = WiColors.TextPrimary), maxLines = 1)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(UiIcons.Gallery, "Pick from gallery", onGallery)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        WiColors.Pink.copy(alpha = (1f - ring) * 0.8f),
                        radius = size.minDimension / 2f * (0.8f + 0.2f * ring),
                        style = Stroke(3.dp.toPx()),
                    )
                }
                Pressable(
                    onClick = onCapture,
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    background = Color.Transparent,
                    border = WiColors.TextPrimary,
                ) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .shadow(14.dp, CircleShape, ambientColor = WiColors.Pink, spotColor = WiColors.Pink)
                            .clip(CircleShape)
                            .background(WiColors.Pink),
                    )
                }
            }
            RoundIconButton(UiIcons.Search, "Search manually", onSearch)
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PathIcon(UiIcons.Scan, WiColors.Cyan, size = 56.dp, strokeWidth = 1.8f)
        Text("Camera access needed", style = WiType.Title)
        Text(
            "Warning Identifier uses the camera to recognise dashboard lights. Photos are analysed on your phone and never uploaded.",
            style = WiType.BodySmall.copy(textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(4.dp))
        ShineButton("Allow camera", UiIcons.Check, onGrant, Modifier.fillMaxWidth())
        Pressable(onClick = onGallery, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Choose a photo instead", style = WiType.CardTitle, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(160.dp))
    }
}
