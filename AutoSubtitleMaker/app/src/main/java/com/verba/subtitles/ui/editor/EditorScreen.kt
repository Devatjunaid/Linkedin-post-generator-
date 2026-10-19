package com.verba.subtitles.ui.editor

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
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
import com.verba.subtitles.AppContainer
import com.verba.subtitles.core.model.Caption
import com.verba.subtitles.core.render.CaptionRenderer
import com.verba.subtitles.ui.components.PillButton
import com.verba.subtitles.ui.components.RoundIconButton
import com.verba.subtitles.ui.components.SectionLabel
import com.verba.subtitles.ui.components.pressable
import com.verba.subtitles.ui.theme.Verba
import java.util.Locale

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

    Box(Modifier.fillMaxSize().background(Verba.Ink)) {
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
                    Text("${ui.captions.size} captions · autosaved", style = MaterialTheme.typography.labelSmall, color = Verba.Muted)
                }
                RoundIconButton(
                    Icons.AutoMirrored.Rounded.Undo, "Undo", vm::undo,
                    tint = if (ui.canUndo) Verba.Text else Verba.Muted.copy(alpha = 0.4f),
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Verba.Lime) }
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
                        .border(1.dp, Verba.Line, RoundedCornerShape(20.dp)),
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
                            Modifier.align(Alignment.Center).size(54.dp).clip(CircleShape).background(Verba.Ink.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Verba.Text, modifier = Modifier.size(30.dp))
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
                        .background(Verba.Text)
                        .pressable(onClick = { if (player.isPlaying) player.pause() else player.play() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Verba.Ink,
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
                    .background(Verba.Surface),
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
            containerColor = Verba.Surface,
        )
    }
}

private val Color0 = androidx.compose.ui.graphics.Color.Black

@Composable
private fun TimeLabel(positionMs: () -> Long, durationMs: Long, modifier: Modifier = Modifier) {
    Text(
        "${clock(positionMs())} / ${clock(durationMs)}",
        style = MaterialTheme.typography.labelMedium,
        color = Verba.Muted,
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
            val bg by animateColorAsState(if (selected) Verba.Lime else Verba.Surface, label = "tabBg")
            val fg by animateColorAsState(if (selected) Verba.Ink else Verba.Muted, label = "tabFg")
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
 * Scrubbable timeline: the playhead is fixed in the centre and the strip of caption blocks
 * scrolls under it. Dragging the strip scrubs the video; playing scrolls it automatically.
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
    val scroll = rememberScrollState()
    val dragged by scroll.interactionSource.collectIsDraggedAsState()
    var userScrubbing by remember { mutableStateOf(false) }
    val latestOnScrub by rememberUpdatedState(onScrub)

    // A finger on the strip starts scrubbing; it ends when the fling settles.
    LaunchedEffect(dragged) { if (dragged) userScrubbing = true }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }.collect { moving -> if (!moving && !dragged) userScrubbing = false }
    }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.value }.collect { value -> if (userScrubbing) latestOnScrub((value / pxPerMs).toLong()) }
    }
    // Otherwise follow playback and taps on captions.
    LaunchedEffect(positionMs, userScrubbing) {
        if (!userScrubbing) {
            val target = (positionMs * pxPerMs).toInt()
            if (kotlin.math.abs(scroll.value - target) > 1) scroll.scrollTo(target)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp)) {
        val half = maxWidth / 2
        val contentWidth = with(density) { (durationMs * pxPerMs).toDp() }
        Row(Modifier.fillMaxSize().horizontalScroll(scroll)) {
            Spacer(Modifier.width(half))
            Box(Modifier.width(contentWidth).fillMaxHeight()) {
                // Second ticks (one canvas, cheap even for long videos).
                Canvas(Modifier.fillMaxSize()) {
                    val seconds = (durationMs / 1_000).toInt()
                    for (sec in 0..seconds) {
                        val x = sec * 1_000 * pxPerMs
                        val tick = if (sec % 5 == 0) 12.dp.toPx() else 6.dp.toPx()
                        drawLine(
                            Verba.SurfaceTop,
                            androidx.compose.ui.geometry.Offset(x, size.height - tick),
                            androidx.compose.ui.geometry.Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
                captions.forEach { caption ->
                    val x = with(density) { (caption.startMs * pxPerMs).toDp() }
                    val w = with(density) { ((caption.endMs - caption.startMs) * pxPerMs).toDp() }.coerceAtLeast(14.dp)
                    val active = positionMs in caption.startMs until caption.endMs
                    val selected = caption.id == selectedId
                    Text(
                        caption.text,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Verba.Ink else Verba.Soft,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .offset(x = x, y = 8.dp)
                            .width(w - 2.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) Verba.Lime else Verba.SurfaceHigh)
                            .border(1.5.dp, if (selected) Verba.Coral else Verba.Line, RoundedCornerShape(9.dp))
                            .pressable(onClick = { onSelect(caption) }, pressedScale = 0.96f)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.width(half))
        }
        // Fixed playhead.
        val headX = half - 1.dp
        Box(Modifier.offset(x = headX).width(2.dp).fillMaxHeight().background(Verba.Lime))
        Box(Modifier.offset(x = headX - 5.dp).size(12.dp).clip(CircleShape).background(Verba.Lime))
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
        containerColor = Verba.Surface,
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
                TextButton(onClick = onDelete) { Text("Delete", color = Verba.Coral) }
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
