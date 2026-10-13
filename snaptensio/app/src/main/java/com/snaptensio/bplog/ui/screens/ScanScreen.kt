package com.snaptensio.bplog.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as MlText
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptensio.bplog.ocr.BpResult
import com.snaptensio.bplog.ocr.BpTextParser
import com.snaptensio.bplog.ocr.ConsensusTracker
import com.snaptensio.bplog.ocr.NumberToken
import com.snaptensio.bplog.ui.theme.Brand
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

private const val TAG = "SnapTensioScan"
private const val FRAME_INTERVAL_MS = 250L

/** Turns ML Kit's output into geometry-only tokens the pure parser understands. */
fun MlText.toTokens(): List<NumberToken> = textBlocks.flatMap { b -> b.lines }.flatMap { l -> l.elements }.mapNotNull { e ->
    val box = e.boundingBox ?: return@mapNotNull null
    NumberToken(e.text, box.top, box.bottom, box.left, box.right)
}

/** Live analyzer: recognises text on throttled frames and feeds the consensus tracker. */
private class BpAnalyzer(
    private val recognizer: TextRecognizer,
    private val onFrame: (BpResult?) -> Unit,
) : ImageAnalysis.Analyzer {
    private var last = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val now = System.currentTimeMillis()
        val media = proxy.image
        if (media == null || now - last < FRAME_INTERVAL_MS) {
            proxy.close()
            return
        }
        last = now
        try {
            val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { text -> onFrame(BpTextParser.parse(text.toTokens())) }
                .addOnFailureListener { Log.d(TAG, "OCR failed", it) }
                .addOnCompleteListener { proxy.close() }
        } catch (e: Exception) {
            // Recognizer already closed while the screen was leaving.
            proxy.close()
        }
    }
}

private suspend fun recognizeUri(context: Context, recognizer: TextRecognizer, uri: Uri): BpResult? =
    suspendCancellableCoroutine { cont ->
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
        if (image == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        recognizer.process(image)
            .addOnSuccessListener { cont.resume(BpTextParser.parse(it.toTokens())) }
            .addOnFailureListener { cont.resume(null) }
    }

@Composable
fun ScanScreen(
    onClose: () -> Unit,
    onResult: (sys: Int, dia: Int, pulse: Int?) -> Unit,
    onTypeInstead: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(Unit) { onDispose { recognizer.close() } }

    val tracker = remember { ConsensusTracker() }
    var confirmed by remember { mutableStateOf<BpResult?>(null) }
    var latest by remember { mutableStateOf<BpResult?>(null) }
    var agreement by remember { mutableStateOf(0) }
    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var galleryBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        galleryBusy = true
        scope.launch {
            val r = recognizeUri(context, recognizer, uri)
            galleryBusy = false
            if (r != null) onResult(r.systolic, r.diastolic, r.pulse)
            else message = "Couldn't find blood pressure numbers in that photo. Try a sharper, straight-on shot, or type them in."
        }
    }

    Box(Modifier.fillMaxSize().background(Brand.Night)) {
        if (hasPermission) {
            CameraPreview(
                recognizer = recognizer,
                onCamera = { camera = it },
                onFrame = { r ->
                    // Called on the main thread by ML Kit's task listeners.
                    if (confirmed == null) {
                        latest = r ?: latest
                        confirmed = tracker.add(r)
                        agreement = tracker.agreement()
                    }
                },
            )
            ViewfinderOverlay(locked = confirmed != null)
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Camera access needed", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(
                    "SnapTensio uses the camera only to read the numbers on your monitor's screen. Nothing is recorded or uploaded.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text(if (askedOnce) "Try again" else "Allow camera") }
                TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("Pick a photo instead", color = Color.White)
                }
            }
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White)) {
                Icon(Icons.Filled.Close, contentDescription = "Close camera")
            }
            Text("Scan your monitor", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                IconButton(
                    onClick = {
                        torch = !torch
                        camera?.cameraControl?.enableTorch(torch)
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (torch) Color(0xFFFFD34D) else Color.White.copy(alpha = 0.14f),
                        contentColor = if (torch) Brand.Night else Color.White,
                    ),
                ) { Icon(if (torch) Icons.Filled.FlashOn else Icons.Filled.FlashOff, contentDescription = if (torch) "Turn torch off" else "Turn torch on") }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        // Bottom sheet
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Brand.Night)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val status = when {
                galleryBusy -> "Reading photo…"
                confirmed != null -> "Got it. All numbers matched on several frames."
                latest != null -> "Hold steady… matching ${agreement.coerceAtMost(3)} of 3 frames"
                else -> "Fill the frame with the monitor's screen. Avoid glare."
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (confirmed != null) Color(0xFF3DDC84) else Color(0xFFFFD34D)))
                Spacer(Modifier.width(8.dp))
                Text(status, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                if (galleryBusy) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
            val shown = confirmed ?: latest
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueBox("Systolic", shown?.systolic, Modifier.weight(1f))
                ValueBox("Diastolic", shown?.diastolic, Modifier.weight(1f))
                ValueBox("Pulse", shown?.pulse, Modifier.weight(1f))
            }
            message?.let { Text(it, color = Color(0xFFFFB4B4), style = MaterialTheme.typography.bodySmall) }
            AnimatedVisibility(confirmed != null, enter = fadeIn() + slideInVertically { it / 2 }) {
                Button(
                    onClick = { confirmed?.let { onResult(it.systolic, it.diastolic, it.pulse) } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use these numbers")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18223A), contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("From photo")
                }
                Button(
                    onClick = {
                        if (confirmed != null) { tracker.reset(); confirmed = null; latest = null; agreement = 0 } else onTypeInstead()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18223A), contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                ) { Text(if (confirmed != null) "Scan again" else "Type instead") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFAEB9CB), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Read on your phone. Works offline. Photo is not saved.", color = Color(0xFFAEB9CB), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ValueBox(label: String, value: Int?, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF18223A)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, color = Color(0xFFAEB9CB), style = MaterialTheme.typography.labelSmall)
        Text(value?.toString() ?: "—", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun CameraPreview(recognizer: TextRecognizer, onCamera: (Camera) -> Unit, onFrame: (BpResult?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, BpAnalyzer(recognizer, onFrame)) }
                p.unbindAll()
                onCamera(p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis))
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

/** Viewfinder corners plus a sweeping scan line; turns green when the reading is locked. */
@Composable
private fun ViewfinderOverlay(locked: Boolean) {
    val t = rememberInfiniteTransition(label = "viewfinder")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sweep")
    val color = if (locked) Color(0xFF3DDC84) else Color.White
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width * 0.78f
        val h = size.height * 0.36f
        val left = (size.width - w) / 2
        val top = size.height * 0.14f
        val l = 34.dp.toPx()
        val sw = 4.dp.toPx()
        val corners = listOf(
            Offset(left, top) to Offset(1f, 1f),
            Offset(left + w, top) to Offset(-1f, 1f),
            Offset(left, top + h) to Offset(1f, -1f),
            Offset(left + w, top + h) to Offset(-1f, -1f),
        )
        corners.forEach { (p, d) ->
            drawLine(color, p, Offset(p.x + l * d.x, p.y), sw, StrokeCap.Round)
            drawLine(color, p, Offset(p.x, p.y + l * d.y), sw, StrokeCap.Round)
        }
        if (!locked) {
            val y = top + 12.dp.toPx() + (h - 24.dp.toPx()) * sweep
            drawLine(Color(0xFF3DDC84).copy(alpha = 0.85f), Offset(left + 16.dp.toPx(), y), Offset(left + w - 16.dp.toPx(), y), 3.dp.toPx(), StrokeCap.Round)
        }
    }
}
