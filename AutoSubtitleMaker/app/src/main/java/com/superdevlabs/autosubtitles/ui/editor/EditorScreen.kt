package com.superdevlabs.autosubtitles.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.render.CaptionRenderer
import com.superdevlabs.autosubtitles.ui.components.PillButton
import com.superdevlabs.autosubtitles.ui.components.RoundIconButton
import com.superdevlabs.autosubtitles.ui.components.SectionLabel
import com.superdevlabs.autosubtitles.ui.components.pressable
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import java.util.Locale
import kotlin.math.roundToInt

enum class EditorTab(val label: String) { CAPTIONS("Captions"), STYLE("Style"), FONT("Font"), COLOR("Color"), MOTION("Motion"), LAYOUT("Layout") }

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(container: AppContainer, projectId: String, onBack: () -> Unit, onExport: () -> Unit) {
    val vm: EditorViewModel = viewModel(
        key = "editor-$projectId",
        factory = viewModelFactory { initializer { EditorViewModel(container, projectId) } },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val project = ui.project
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    // ---- Player -------------------------------------------------------------------------
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
        }
    }
    // Read only inside small children / draw lambdas so the editor doesn't recompose every frame.
    val position = remember { mutableLongStateOf(0L) }
    val positionMs: () -> Long = remember { { position.longValue } }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(project?.videoFile) {
        val file = project?.videoFile ?: return@LaunchedEffect
        if (file.exists()) {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) player.pause() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(player) {
        while (true) {
            withFrameMillis { }
            position.longValue = player.currentPosition
        }
    }
    val seekTo: (Long) -> Unit = { ms ->
        val target = ms.coerceIn(0L, (project?.durationMs ?: 0L).coerceAtLeast(0L))
        player.seekTo(target)
        position.longValue = target
    }

    BackHandler {
        vm.flush()
        onBack()
    }

    var tab by remember { mutableStateOf(EditorTab.CAPTIONS) }
    var editing by remember { mutableStateOf<Caption?>(null) }
    var renaming by remember { mutableStateOf(false) }
    val renderer = remember { CaptionRenderer(container.fonts) }

    Box(Modifier.fillMaxSize().background(AppColors.Ink)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // ---- Top bar ------------------------------------------------------------------
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = {
                    vm.flush()
                    onBack()
                })
                Column(Modifier.weight(1f).pressable(onClick = { renaming = true }, pressedScale = 0.98f)) {
                    Text(project?.name ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${ui.captions.size} captions · autosaved", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                }
                RoundIconButton(
                    Icons.AutoMirrored.Rounded.Undo, "Undo", vm::undo,
                    tint = if (ui.canUndo) AppColors.Text else AppColors.Muted.copy(alpha = 0.4f),
                )
                PillButton(
                    text = "Export",
                    icon = Icons.Rounded.FileDownload,
                    onClick = {
                        player.pause()
                        vm.flush()
                        onExport()
                    },
                    height = 44.dp,
                    enabled = ui.captions.isNotEmpty(),
                )
            }

            if (ui.loading || project == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Lime) }
            } else {

            // ---- Preview ------------------------------------------------------------------
            val aspect = if (project.width > 0 && project.height > 0) project.width.toFloat() / project.height else 9f / 16f
            BoxWithConstraints(
                Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .aspectRatio(aspect, matchHeightConstraintsFirst = aspect < 1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color0)
                        .border(1.dp, AppColors.Line, RoundedCornerShape(20.dp)),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                                this.player = player
                            }
                        },
                    )
                    CaptionLayer(
                        renderer = renderer,
                        captions = ui.captions,
                        style = ui.style,
                        timeMs = positionMs,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { if (player.isPlaying) player.pause() else player.play() }
                            }
                            .pointerInput(Unit) {
                                // Drag up/down on the video to move the subtitles.
                                detectVerticalDragGestures(onDragStart = { vm.beginGesture() }) { _, dragAmount ->
                                    val heightPx = size.height.toFloat().coerceAtLeast(1f)
                                    vm.updateStyleLive { s -> s.copy(positionY = (s.positionY + dragAmount / heightPx).coerceIn(0.06f, 0.94f)) }
                                }
                            },
                    )
                    if (!isPlaying) {
                        Box(
                            Modifier.align(Alignment.Center).size(54.dp).clip(CircleShape).background(AppColors.Ink.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = AppColors.Text, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }

            // ---- Transport + timeline -----------------------------------------------------
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeLabel(positionMs, project.durationMs, Modifier.weight(1f))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.Text)
                        .pressable(onClick = { if (player.isPlaying) player.pause() else player.play() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = AppColors.Ink,
                    )
                }
                Spacer(Modifier.weight(1f))
                SectionLabel("Drag video ↕ to move")
            }
            Timeline(
                captions = ui.captions,
                durationMs = project.durationMs,
                positionProvider = positionMs,
                selectedId = ui.selectedId,
                onScrub = { ms ->
                    if (player.isPlaying) player.pause()
                    seekTo(ms)
                },
                onSelect = { caption ->
                    vm.select(caption.id)
                    seekTo(caption.startMs)
                },
            )

            // ---- Tabs + panels -----------------------------------------------------------
            EditorTabs(tab) { tab = it }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(AppColors.Surface),
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(tween(260)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(200)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(160)))
                    },
                    label = "panel",
                ) { current ->
                    when (current) {
                        EditorTab.CAPTIONS -> CaptionsPanel(
                            captions = ui.captions,
                            positionMs = positionMs,
                            selectedId = ui.selectedId,
                            style = ui.style,
                            onSeek = { caption ->
                                vm.select(caption.id)
                                seekTo(caption.startMs)
                            },
                            onEdit = { editing = it },
                        )
                        EditorTab.STYLE -> StylePanel(container.fonts, ui.style, vm::applyPreset)
                        EditorTab.FONT -> FontPanel(container.fonts, ui.style, vm::updateStyle, vm::importFont)
                        EditorTab.COLOR -> ColorPanel(ui.style, vm::updateStyle)
                        EditorTab.MOTION -> MotionPanel(container.fonts, ui.style, vm::updateStyle)
                        EditorTab.LAYOUT -> LayoutPanel(ui.style, vm::updateStyle, vm::setWordsPerCaption)
                    }
                }
            }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }

    editing?.let { caption ->
        EditCaptionDialog(
            caption = caption,
            onDismiss = { editing = null },
            onSave = { text ->
                vm.editCaption(caption.id, text)
                editing = null
            },
            onDelete = {
                vm.deleteCaption(caption.id)
                editing = null
            },
            onNudge = { ds, de ->
                vm.nudge(caption.id, ds, de)
                editing = vm.ui.value.captions.firstOrNull { it.id == caption.id }
            },
        )
    }

    if (renaming && project != null) {
        var name by remember { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename project") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    vm.rename(name)
                    renaming = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
            containerColor = AppColors.Surface,
        )
    }
}

private val Color0 = androidx.compose.ui.graphics.Color.Black

@Composable
private fun TimeLabel(positionMs: () -> Long, durationMs: Long, modifier: Modifier = Modifier) {
    Text(
        "${clock(positionMs())} / ${clock(durationMs)}",
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.Muted,
        modifier = modifier,
    )
}

@Composable
private fun EditorTabs(current: EditorTab, onSelect: (EditorTab) -> Unit) {
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EditorTab.entries.forEach { item ->
            val selected = item == current
            val bg by animateColorAsState(if (selected) AppColors.Lime else AppColors.Surface, label = "tabBg")
            val fg by animateColorAsState(if (selected) AppColors.Ink else AppColors.Muted, label = "tabFg")
            Text(
                item.label,
                style = MaterialTheme.typography.titleSmall,
                color = fg,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .pressable(onClick = { onSelect(item) }, role = androidx.compose.ui.semantics.Role.Tab)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/**
 * Scrubbable timeline: the playhead is fixed in the centre and the caption blocks slide under it.
 * Drag the strip to scrub; during playback it follows the video.
 *
 * Only the part of the timeline that is on screen is drawn. (Drawing the whole video as one strip
 * crashed long videos: Compose can't lay out anything wider than 262,143 px.)
 */
@Composable
private fun Timeline(
    captions: List<Caption>,
    durationMs: Long,
    positionProvider: () -> Long,
    selectedId: Long?,
    onScrub: (Long) -> Unit,
    onSelect: (Caption) -> Unit,
) {
    val positionMs = positionProvider()
    val density = LocalDensity.current
    val pxPerMs = with(density) { 90.dp.toPx() } / 1_000f
    val minChipPx = with(density) { 14.dp.toPx() }
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestPosition by rememberUpdatedState(positionProvider)
    // Time under the playhead while the user drags; null = follow playback.
    var dragMs by remember { mutableStateOf<Float?>(null) }
    val centerMs = dragMs ?: positionMs.toFloat()

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clipToBounds()
            .pointerInput(durationMs, pxPerMs) {
                detectHorizontalDragGestures(
                    onDragStart = { dragMs = latestPosition().toFloat() },
                    onDragEnd = { dragMs = null },
                    onDragCancel = { dragMs = null },
                ) { change, dragAmount ->
                    change.consume()
                    val next = ((dragMs ?: latestPosition().toFloat()) - dragAmount / pxPerMs)
                        .coerceIn(0f, durationMs.coerceAtLeast(0L).toFloat())
                    dragMs = next
                    latestOnScrub(next.toLong())
                }
            },
    ) {
        val viewportPx = constraints.maxWidth.toFloat()
        val halfPx = viewportPx / 2f
        val windowStartMs = centerMs - halfPx / pxPerMs
        val windowEndMs = centerMs + halfPx / pxPerMs

        // Second ticks for the visible window only.
        Canvas(Modifier.fillMaxSize()) {
            val first = kotlin.math.floor(windowStartMs / 1_000f).toInt().coerceAtLeast(0)
            val last = kotlin.math.ceil(windowEndMs / 1_000f).toInt().coerceAtMost((durationMs / 1_000).toInt())
            for (sec in first..last) {
                val x = halfPx + (sec * 1_000f - centerMs) * pxPerMs
                val tick = if (sec % 5 == 0) 12.dp.toPx() else 6.dp.toPx()
                drawLine(
                    AppColors.SurfaceTop,
                    androidx.compose.ui.geometry.Offset(x, size.height - tick),
                    androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        // Caption blocks that overlap the visible window (plus a little margin).
        val marginMs = 1_000f
        captions.forEach { caption ->
            if (caption.endMs < windowStartMs - marginMs || caption.startMs > windowEndMs + marginMs) return@forEach
            key(caption.id) {
                val xPx = halfPx + (caption.startMs - centerMs) * pxPerMs
                val widthPx = ((caption.endMs - caption.startMs) * pxPerMs).coerceIn(minChipPx, viewportPx * 2f)
                val active = positionMs in caption.startMs until caption.endMs
                val selected = caption.id == selectedId
                Text(
                    caption.text,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) AppColors.Ink else AppColors.Soft,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .offset { IntOffset(xPx.roundToInt(), 8.dp.roundToPx()) }
                        .width(with(density) { widthPx.toDp() } - 2.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) AppColors.Lime else AppColors.SurfaceHigh)
                        .border(1.5.dp, if (selected) AppColors.Coral else AppColors.Line, RoundedCornerShape(9.dp))
                        .pressable(onClick = { onSelect(caption) }, pressedScale = 0.96f)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }

        // Fixed playhead.
        val headX = maxWidth / 2 - 1.dp
        Box(Modifier.offset(x = headX).width(2.dp).fillMaxHeight().background(AppColors.Lime))
        Box(Modifier.offset(x = headX - 5.dp).size(12.dp).clip(CircleShape).background(AppColors.Lime))
    }
}

@Composable
private fun EditCaptionDialog(
    caption: Caption,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onNudge: (Long, Long) -> Unit,
) {
    var text by remember(caption.id) { mutableStateOf(caption.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        title = { Text("Edit caption") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, minLines = 2, modifier = Modifier.fillMaxWidth())
                SectionLabel("${clockPrecise(caption.startMs)} → ${clockPrecise(caption.endMs)}")
                NudgeRow("Start", onEarlier = { onNudge(-100, 0) }, onLater = { onNudge(100, 0) })
                NudgeRow("End", onEarlier = { onNudge(0, -100) }, onLater = { onNudge(0, 100) })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = AppColors.Coral) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun NudgeRow(label: String, onEarlier: () -> Unit, onLater: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(48.dp))
        PillButton("−0.1s", onEarlier, Modifier.weight(1f), primary = false, height = 40.dp)
        PillButton("+0.1s", onLater, Modifier.weight(1f), primary = false, height = 40.dp)
    }
}

fun clock(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1_000
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

fun clockPrecise(ms: Long): String {
    val t = ms.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d.%d", t / 60_000, (t / 1_000) % 60, (t % 1_000) / 100)
}
