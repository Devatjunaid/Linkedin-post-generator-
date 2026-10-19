package com.sunarc.inverterlogger.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.domain.Field
import com.sunarc.inverterlogger.domain.ParseResult
import com.sunarc.inverterlogger.domain.ReadingSource
import com.sunarc.inverterlogger.ocr.OcrEngine
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.Route
import com.sunarc.inverterlogger.ui.ScanDraft
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.components.PrimaryButton
import com.sunarc.inverterlogger.ui.components.SecondaryButton
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Composable
fun ScanScreen(vm: AppViewModel, nav: Navigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val inverters by vm.inverters.collectAsStateWithLifecycle()
    val activeId by vm.activeInverterId.collectAsStateWithLifecycle()
    val inverter = inverters.firstOrNull { it.id == activeId } ?: inverters.firstOrNull()

    var hasCamera by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var asked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf(ParseResult.EMPTY) }
    var torch by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun finish(result: ParseResult, source: ReadingSource, photo: Bitmap?) {
        val target = inverter ?: return
        scope.launch {
            val path = photo?.let { savePhoto(context, it) }
            vm.draft.value = ScanDraft(target.id, source, result, path)
            nav.replace(Route.REVIEW)
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted
        asked = true
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { OcrEngine.decode(context, uri) }
            if (bmp == null) {
                busy = false; error = "Could not open that image."
                return@launch
            }
            val result = runCatching { vm.ocr.read(bmp, vm.parseContext(inverter)) }.getOrElse { ParseResult.EMPTY }
            busy = false
            finish(result, ReadingSource.SCREENSHOT, bmp)
        }
    }
    fun pickImage() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    LaunchedEffect(Unit) {
        if (vm.pendingGallery.value) {
            vm.pendingGallery.value = false
            pickImage()
        } else if (!hasCamera && !asked) {
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050608))) {
        var viewSize by remember { mutableStateOf(IntSize.Zero) }
        var guide by remember { mutableStateOf(RectF()) }
        val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
        var camera by remember { mutableStateOf<Camera?>(null) }

        if (hasCamera) {
            CameraPreview(
                capture = capture,
                onCamera = { camera = it },
                onFrame = { bmp, rotation ->
                    val result = vm.ocr.quickParse(bmp, rotation, vm.parseContext(inverter))
                    if (result.values.isNotEmpty() || result.detectedBrand != null) live = result
                },
                modifier = Modifier.fillMaxSize().onSizeChanged { viewSize = it }
            )
            LaunchedEffect(torch, camera) { camera?.cameraControl?.enableTorch(torch) }
        } else {
            NoCameraCard(
                onAllow = { permission.launch(Manifest.permission.CAMERA) },
                onGallery = { pickImage() },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (hasCamera) {
            ScanGuide(
                Modifier.align(Alignment.Center).fillMaxWidth(0.82f).height(190.dp)
                    .onGloballyPositioned { c ->
                        val b = c.boundsInParent()
                        guide = RectF(b.left, b.top, b.right, b.bottom)
                    }
            )
        }

        // Top bar: close, detected brand, torch.
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.Close, "Close scanner") { nav.pop() }
            Spacer(Modifier.weight(1f))
            val brandName = live.detectedBrand?.name ?: inverter?.brand?.name ?: "Inverter"
            Row(
                Modifier.clip(RoundedCornerShape(18.dp)).background(Palette.Surface.copy(alpha = 0.88f)).border(1.dp, Palette.Line, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.Sun))
                Spacer(Modifier.width(8.dp))
                Text(if (live.detectedBrand != null) "$brandName · detected" else brandName, fontSize = 13.sp, color = Palette.Ink)
            }
            Spacer(Modifier.weight(1f))
            if (hasCamera) RoundIcon(if (torch) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, if (torch) "Turn torch off" else "Turn torch on", highlighted = torch) { torch = !torch }
            else Spacer(Modifier.size(44.dp))
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top = 76.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Palette.Sky, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("ON-DEVICE · NO INTERNET NEEDED", fontFamily = MonoFont, fontSize = 11.sp, color = Palette.InkSoft, letterSpacing = 0.5.sp)
        }

        // Bottom: live values, then gallery / shutter / inverter switch.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val chips = listOf(Field.ENERGY_TODAY to "Today", Field.POWER_NOW to "Live", Field.ENERGY_TOTAL to "Total")
            val found = chips.count { live[it.first] != null }
            Text(
                when {
                    error != null -> error!!
                    !hasCamera -> ""
                    found == 0 -> inverter?.brand?.tip ?: "Fit the display inside the frame and hold steady"
                    else -> "Hold steady · $found value${if (found > 1) "s" else ""} found"
                },
                fontSize = 12.sp, color = Palette.Muted, modifier = Modifier.padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                chips.forEach { (field, label) ->
                    val v = live[field]
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Palette.Surface.copy(alpha = 0.92f))
                            .border(1.dp, if (v != null) Palette.Sun.copy(alpha = 0.6f) else Palette.Line, RoundedCornerShape(14.dp)).padding(10.dp)
                    ) {
                        Text(label, fontSize = 11.sp, color = Palette.Muted)
                        AnimatedVisibility(v != null, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
                            val text = when (field) {
                                Field.POWER_NOW -> Fmt.power(v?.value).let { "${it.first} ${it.second}" }
                                else -> Fmt.energy(v?.value).let { "${it.first} ${it.second}" }
                            }
                            Text(text, fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Palette.Ink)
                        }
                        if (v == null) Text("—", fontFamily = MonoFont, fontSize = 15.sp, color = Palette.Faint)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(0.8f), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Rounded.PhotoLibrary, "Scan a screenshot or photo from the gallery", square = true) { pickImage() }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(82.dp).clip(CircleShape).border(4.dp, Palette.Sun, CircleShape)
                        .clickable(enabled = hasCamera && !busy && inverter != null, role = Role.Button) {
                            busy = true
                            error = null
                            capture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val rotation = image.imageInfo.rotationDegrees
                                    val full = OcrEngine.rotate(image.toBitmap(), rotation)
                                    image.close()
                                    scope.launch {
                                        val cropped = withContext(Dispatchers.Default) {
                                            if (viewSize.width > 0 && guide.width() > 0) OcrEngine.cropToGuide(full, viewSize.width.toFloat(), viewSize.height.toFloat(), guide) else full
                                        }
                                        val result = runCatching { vm.ocr.read(cropped, vm.parseContext(inverter), extraFullImage = full) }.getOrElse { ParseResult.EMPTY }
                                        busy = false
                                        finish(result, ReadingSource.PHOTO, cropped)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    busy = false
                                    error = "Could not take the photo. Try again."
                                }
                            })
                        }
                        .semantics { contentDescription = "Take photo and read values" },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(if (hasCamera) Palette.Sun else Palette.SunDeep))
                }
                Spacer(Modifier.weight(1f))
                RoundIcon(Icons.Rounded.SwapHoriz, "Switch inverter", square = true) {
                    if (inverters.size > 1) {
                        val i = inverters.indexOfFirst { it.id == inverter?.id }
                        vm.activeInverterId.value = inverters[(i + 1) % inverters.size].id
                    }
                }
            }
            if (inverters.size > 1 && inverter != null) {
                Text("For: ${inverter.name}", fontSize = 12.sp, color = Palette.InkSoft, modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (busy) {
            Box(Modifier.fillMaxSize().background(Palette.Night.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
                BusyOverlay("Reading the display…")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    capture: ImageCapture,
    onCamera: (Camera) -> Unit,
    onFrame: suspend (Bitmap, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val busy = AtomicBoolean(false)
        val lastRun = AtomicLong(0)
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        analysis.setAnalyzer(executor) { proxy ->
            val now = System.currentTimeMillis()
            if (busy.get() || now - lastRun.get() < 700) {
                proxy.close()
                return@setAnalyzer
            }
            busy.set(true)
            lastRun.set(now)
            val bmp = runCatching { proxy.toBitmap() }.getOrNull()
            val rotation = proxy.imageInfo.rotationDegrees
            proxy.close()
            if (bmp == null) {
                busy.set(false)
                return@setAnalyzer
            }
            scope.launch {
                try {
                    onFrame(bmp, rotation)
                } catch (_: Exception) {
                    // A failed live frame is ignored; the next one is tried.
                } finally {
                    busy.set(false)
                }
            }
        }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                onCamera(provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis))
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            analysis.clearAnalyzer()
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Amber corner brackets that snap onto the display, a scan beam sweeping inside them. */
@Composable
private fun ScanGuide(modifier: Modifier) {
    val loop = rememberInfiniteTransition(label = "beam")
    val beam by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "beamPos")
    val breathe by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "breathe")
    Canvas(modifier.semantics { contentDescription = "Fit the inverter display inside this frame" }) {
        val len = 30.dp.toPx()
        val r = 10.dp.toPx()
        val inset = breathe * 4.dp.toPx()
        val w = size.width
        val h = size.height
        val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
        fun corner(path: Path) = drawPath(path, Palette.Sun, style = stroke)
        corner(Path().apply { moveTo(inset, len + inset); lineTo(inset, r + inset); quadraticTo(inset, inset, r + inset, inset); lineTo(len + inset, inset) })
        corner(Path().apply { moveTo(w - len - inset, inset); lineTo(w - r - inset, inset); quadraticTo(w - inset, inset, w - inset, r + inset); lineTo(w - inset, len + inset) })
        corner(Path().apply { moveTo(w - inset, h - len - inset); lineTo(w - inset, h - r - inset); quadraticTo(w - inset, h - inset, w - r - inset, h - inset); lineTo(w - len - inset, h - inset) })
        corner(Path().apply { moveTo(len + inset, h - inset); lineTo(r + inset, h - inset); quadraticTo(inset, h - inset, inset, h - r - inset); lineTo(inset, h - len - inset) })
        val y = h * beam
        val fade = (if (beam < 0.1f) beam / 0.1f else if (beam > 0.9f) (1f - beam) / 0.1f else 1f)
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Palette.Sun.copy(alpha = 0.28f * fade)), startY = y - 40.dp.toPx(), endY = y),
            topLeft = Offset(12.dp.toPx(), y - 40.dp.toPx()), size = androidx.compose.ui.geometry.Size(w - 24.dp.toPx(), 40.dp.toPx())
        )
        drawLine(Palette.Sun.copy(alpha = fade), Offset(12.dp.toPx(), y), Offset(w - 12.dp.toPx(), y), strokeWidth = 2.dp.toPx())
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, highlighted: Boolean = false, square: Boolean = false, onClick: () -> Unit) {
    val shape = if (square) RoundedCornerShape(14.dp) else CircleShape
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (square) 50.dp else 44.dp).clip(shape)
            .background(if (highlighted) Palette.Sun else Palette.Surface.copy(alpha = 0.85f))
            .border(1.dp, if (highlighted) Palette.Sun else Palette.Line, shape)
    ) {
        Icon(icon, contentDescription = description, tint = if (highlighted) Palette.OnSun else Palette.Ink)
    }
}

@Composable
private fun NoCameraCard(onAllow: () -> Unit, onGallery: () -> Unit, modifier: Modifier) {
    Column(
        modifier.padding(24.dp).clip(RoundedCornerShape(24.dp)).background(Palette.Surface).border(1.dp, Palette.Line, RoundedCornerShape(24.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera access", fontSize = 18.sp, color = Palette.Ink, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Sunarc uses the camera only to photograph your inverter display. Photos stay on this phone.",
            fontSize = 13.sp, color = Palette.InkSoft
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Allow camera", onAllow)
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Scan a screenshot instead", onGallery, Modifier.fillMaxWidth(), Icons.Rounded.PhotoLibrary)
    }
}

/** Keeps a small copy of the scanned photo on the device so the user can check it later. */
private suspend fun savePhoto(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "scan-${System.currentTimeMillis()}.jpg")
        val small = OcrEngine.scaleDown(bitmap, 1280)
        file.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        file.absolutePath
    }.getOrNull()
}
