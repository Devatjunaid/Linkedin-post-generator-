package com.verba.subtitles.ui.export

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verba.subtitles.AppContainer
import com.verba.subtitles.ads.BannerAd
import com.verba.subtitles.ads.NativeAdCard
import com.verba.subtitles.core.captions.StylePresets
import com.verba.subtitles.core.captions.SubtitleFormat
import com.verba.subtitles.core.render.CaptionRenderer
import com.verba.subtitles.export.ExportQuality
import com.verba.subtitles.export.MediaSaver
import com.verba.subtitles.ui.components.ChoiceCard
import com.verba.subtitles.ui.components.PillButton
import com.verba.subtitles.ui.components.SectionLabel
import com.verba.subtitles.ui.components.Segmented
import com.verba.subtitles.ui.components.Thumbnail
import com.verba.subtitles.ui.components.TopBar
import com.verba.subtitles.ui.editor.CaptionLayer
import com.verba.subtitles.ui.home.formatDuration
import com.verba.subtitles.ui.theme.Verba
import kotlin.math.sin

@Composable
fun ExportScreen(container: AppContainer, projectId: String, onBack: () -> Unit, onHome: () -> Unit) {
    val vm: ExportViewModel = viewModel(
        key = "export-$projectId",
        factory = viewModelFactory { initializer { ExportViewModel(container, projectId) } },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    var confirmCancel by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(ui.phase) {
        view.keepScreenOn = ui.phase == ExportPhase.RUNNING
        onDispose { view.keepScreenOn = false }
    }

    // Subtitle files are saved with the system "Save as" picker: no storage permission needed.
    val createSrt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SubtitleFormat.SRT.mimeType)) { uri -> if (uri != null) vm.writeSubtitle(context, uri) }
    val createVtt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SubtitleFormat.VTT.mimeType)) { uri -> if (uri != null) vm.writeSubtitle(context, uri) }
    val createAss = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SubtitleFormat.ASS.mimeType)) { uri -> if (uri != null) vm.writeSubtitle(context, uri) }
    val createTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SubtitleFormat.TXT.mimeType)) { uri -> if (uri != null) vm.writeSubtitle(context, uri) }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startVideoExport(context)
    }

    fun startVideo() {
        val legacyNeedsPermission = MediaSaver.needsLegacyPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (legacyNeedsPermission) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            vm.startVideoExport(context)
        }
    }

    fun export() {
        // Major action → interstitial (respects the 30 s cap), then continue.
        container.ads.runWithInterstitial(activity) {
            if (ui.output.wantsSubtitles) {
                val name = vm.suggestedSubtitleName()
                when (ui.format) {
                    SubtitleFormat.SRT -> createSrt.launch(name)
                    SubtitleFormat.VTT -> createVtt.launch(name)
                    SubtitleFormat.ASS -> createAss.launch(name)
                    SubtitleFormat.TXT -> createTxt.launch(name)
                }
            } else {
                startVideo()
            }
        }
    }
    // When both are requested, the video starts right after the subtitle file is saved.
    if (ui.output == OutputKind.BOTH && ui.savedSubtitle && ui.phase == ExportPhase.IDLE) {
        LaunchedEffect(Unit) { startVideo() }
    }

    BackHandler(enabled = ui.phase == ExportPhase.RUNNING) { confirmCancel = true }

    Column(Modifier.fillMaxSize().background(Verba.Ink).statusBarsPadding().navigationBarsPadding()) {
        TopBar(
            title = "Export",
            subtitle = ui.project?.name,
            onBack = { if (ui.phase == ExportPhase.RUNNING) confirmCancel = true else onBack() },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Summary(container, ui)

            SectionLabel("Output")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutputKind.entries.forEach { kind ->
                    ChoiceCard(
                        selected = kind == ui.output,
                        onClick = { if (ui.phase != ExportPhase.RUNNING) vm.setOutput(kind) },
                        modifier = Modifier.weight(1f).height(80.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                when (kind) {
                                    OutputKind.VIDEO -> Icons.Rounded.Movie
                                    OutputKind.SUBTITLES -> Icons.Rounded.ClosedCaption
                                    OutputKind.BOTH -> Icons.Rounded.Layers
                                },
                                contentDescription = null,
                                tint = if (kind == ui.output) Verba.Lime else Verba.Muted,
                            )
                            Text(kind.label, style = MaterialTheme.typography.labelMedium, color = Verba.Text)
                        }
                    }
                }
            }
            AnimatedVisibility(ui.output.wantsVideo) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Quality")
                    Segmented(ExportQuality.entries, ui.quality, { it.label }, vm::setQuality, Modifier.fillMaxWidth())
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Verba.Surface)
                            .border(1.dp, Verba.Line, RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Keep original audio", style = MaterialTheme.typography.titleSmall)
                            Text("Copied without re-encoding", style = MaterialTheme.typography.bodySmall, color = Verba.Muted)
                        }
                        Switch(
                            checked = ui.keepAudio,
                            onCheckedChange = vm::setKeepAudio,
                            colors = SwitchDefaults.colors(checkedTrackColor = Verba.Lime, checkedThumbColor = Verba.Ink),
                        )
                    }
                }
            }
            AnimatedVisibility(ui.output.wantsSubtitles) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Subtitle format")
                    Segmented(SubtitleFormat.entries, ui.format, { it.label }, vm::setFormat, Modifier.fillMaxWidth())
                    Text(
                        when (ui.format) {
                            SubtitleFormat.SRT -> "Works everywhere: YouTube, VLC, Premiere, CapCut."
                            SubtitleFormat.VTT -> "For web players and HTML5 video."
                            SubtitleFormat.ASS -> "Keeps your font, colours, outline and karaoke timing."
                            SubtitleFormat.TXT -> "Plain transcript, one caption per line."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Verba.Muted,
                    )
                }
            }
            if (ui.phase != ExportPhase.IDLE) NativeAdCard(container.ads)
            Spacer(Modifier.height(4.dp))
        }

        // ---- Action area: idle → running → done, animated between states ------------------
        AnimatedContent(
            targetState = ui.phase,
            transitionSpec = { (fadeIn(tween(250)) + scaleIn(spring(dampingRatio = 0.7f), initialScale = 0.92f)).togetherWith(fadeOut(tween(150))) },
            label = "action",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) { phase ->
            when (phase) {
                ExportPhase.IDLE -> PillButton(
                    text = when (ui.output) {
                        OutputKind.VIDEO -> "Export ${ui.quality.label} video"
                        OutputKind.SUBTITLES -> "Save .${ui.format.extension} file"
                        OutputKind.BOTH -> "Export video + .${ui.format.extension}"
                    },
                    onClick = ::export,
                    icon = Icons.Rounded.FileDownload,
                    modifier = Modifier.fillMaxWidth(),
                    height = 58.dp,
                    enabled = ui.project?.captions?.isNotEmpty() == true,
                )
                ExportPhase.RUNNING -> RunningBar(ui.progress, onCancel = { confirmCancel = true })
                ExportPhase.DONE -> DonePanel(
                    ui = ui,
                    onShare = {
                        ui.videoFile?.let { file -> context.startActivity(MediaSaver.shareIntent(context, file, "video/mp4")) }
                    },
                    onAgain = vm::reset,
                    onHome = onHome,
                )
                ExportPhase.ERROR -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(ui.error ?: "Export failed", style = MaterialTheme.typography.bodyMedium, color = Verba.Coral)
                    PillButton("Try again", ::startVideo, Modifier.fillMaxWidth())
                }
            }
        }
        BannerAd(container.ads)
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel export?") },
            text = { Text("The video rendered so far will be discarded.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancel()
                    confirmCancel = false
                }) { Text("Cancel export", color = Verba.Coral) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep rendering") } },
            containerColor = Verba.Surface,
        )
    }
}

@Composable
private fun Summary(container: AppContainer, ui: ExportUi) {
    val project = ui.project ?: return
    val renderer = remember { CaptionRenderer(container.fonts) }
    val float = rememberInfiniteTransition(label = "float")
    val t by float.animateFloat(0f, 6.283f, infiniteRepeatable(tween(4_000, easing = LinearEasing)), label = "t")
    val sample = project.captions.getOrNull(minOf(1, project.captions.lastIndex))

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .size(width = 112.dp, height = 196.dp)
                .graphicsLayer {
                    translationY = sin(t) * 6.dp.toPx()
                    rotationZ = -2f + sin(t) * 1f
                }
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Verba.Line, RoundedCornerShape(20.dp)),
        ) {
            Thumbnail(project.thumbnail, Modifier.fillMaxSize())
            if (sample != null) {
                CaptionLayer(
                    renderer = renderer,
                    captions = listOf(sample),
                    style = project.style.copy(textSize = project.style.textSize * 1.4f),
                    timeMs = { sample.startMs + 700 },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryRow("Length", formatDuration(project.durationMs))
            SummaryRow("Captions", "${project.captions.size} lines")
            SummaryRow("Style", StylePresets.byId(project.style.presetId).name + " · " + container.fonts.option(project.style.fontId).name)
            SummaryRow("Size", ui.estimate, highlight = true)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Verba.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = if (highlight) Verba.Lime else Verba.Text, maxLines = 1)
    }
}

@Composable
private fun RunningBar(progress: Float, onCancel: () -> Unit) {
    val animated by animateFloatAsState(progress, spring(stiffness = Spring.StiffnessLow), label = "p")
    val sheen = rememberInfiniteTransition(label = "sheen")
    val x by sheen.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "x")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Rendering on your phone…", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = Verba.Lime)
        }
        Canvas(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
            drawRect(Verba.SurfaceHigh)
            drawRect(Verba.Lime, size = Size(size.width * animated, size.height))
            val sx = size.width * x
            drawRect(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
                topLeft = Offset(sx, 0f),
                size = Size(28.dp.toPx(), size.height),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Keep the app open · nothing is uploaded", style = MaterialTheme.typography.labelSmall, color = Verba.Muted, modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancel", color = Verba.Text) }
        }
    }
}

@Composable
private fun DonePanel(ui: ExportUi, onShare: () -> Unit, onAgain: () -> Unit, onHome: () -> Unit) {
    val popAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { popAnim.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)) }
    val pop = popAnim.value
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(CircleShape).background(Verba.Lime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Verba.Ink)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    when {
                        ui.savedVideo && ui.savedSubtitle -> "Video and subtitles saved"
                        ui.savedVideo -> "Saved to Movies/${MediaSaver.ALBUM}"
                        else -> "Subtitle file saved"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Find it in your gallery or files app", style = MaterialTheme.typography.labelSmall, color = Verba.Muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ui.videoFile != null) {
                PillButton("Share", onShare, Modifier.weight(1f), icon = Icons.Rounded.Share, primary = false)
            } else {
                PillButton("Export again", onAgain, Modifier.weight(1f), primary = false)
            }
            PillButton("New video", onHome, Modifier.weight(1f))
        }
    }
}
