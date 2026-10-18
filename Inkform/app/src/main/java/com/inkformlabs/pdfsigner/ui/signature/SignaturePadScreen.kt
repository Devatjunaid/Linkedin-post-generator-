package com.inkformlabs.pdfsigner.ui.signature

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.ButtonKind
import com.inkformlabs.pdfsigner.ui.components.CircleIconButton
import com.inkformlabs.pdfsigner.ui.components.InkButton
import com.inkformlabs.pdfsigner.ui.components.inkTextFieldColors
import com.inkformlabs.pdfsigner.ui.nav.AppViewModel
import com.inkformlabs.pdfsigner.ui.nav.Route
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import com.inkformlabs.pdfsigner.ui.theme.Paper

private val PenColors = listOf(
    Paper.inkBlue to "Blue ink",
    Paper.inkBlack to "Black ink",
    Paper.inkBurgundy to "Burgundy ink",
)
private val PenWidths = listOf(2.2f to "Fine pen", 3.4f to "Medium pen", 5f to "Bold pen")

@Composable
fun SignaturePadScreen(route: Route.SignaturePad, appViewModel: AppViewModel) {
    val p = LocalInk.current
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val vm: SignaturePadViewModel = viewModel(factory = SignaturePadViewModel.factory(container, route.initials))
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importImage(context, uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(InkIcons.Close, "Close", onClick = { appViewModel.pop() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (vm.isInitials) "Add your initials" else "Add your signature", style = MaterialTheme.typography.titleMedium, color = p.ink)
                Text("Saved only on this phone", style = MaterialTheme.typography.bodySmall, color = p.muted)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Segmented(
                options = listOf("Signature", "Initials"),
                selected = if (vm.isInitials) 1 else 0,
                onSelect = { vm.isInitials = it == 1 },
            )
            Segmented(
                options = listOf("Draw", "Type", "Image"),
                selected = vm.tab.ordinal,
                onSelect = { vm.tab = PadTab.entries[it] },
            )

            AnimatedContent(
                targetState = vm.tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "padTab",
            ) { tab ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Paper.sheet)
                        .border(1.dp, p.line, RoundedCornerShape(26.dp)),
                ) {
                    when (tab) {
                        PadTab.Draw -> DrawPad(vm, onSize = { padSize = it })
                        PadTab.Type -> TypedPreview(vm)
                        PadTab.Image -> ImagePreview(vm, onPick = {
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                    }
                }
            }

            // Pen colour (all tabs) and tab-specific controls
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PenColors.forEach { (color, name) ->
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (vm.penColor == color) p.ink else Color.Transparent, CircleShape)
                            .clickable {
                                vm.penColor = color
                                if (vm.tab == PadTab.Image) vm.reprocess()
                            }
                            .semantics {
                                contentDescription = name
                                role = Role.RadioButton
                            }
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (vm.tab == PadTab.Draw) {
                    CircleIconButton(InkIcons.Undo, "Undo last stroke", onClick = vm::undo, enabled = vm.strokes.isNotEmpty())
                    CircleIconButton(InkIcons.Trash, "Clear", onClick = vm::clear, enabled = vm.strokes.isNotEmpty())
                }
            }

            when (vm.tab) {
                PadTab.Draw -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PenWidths.forEach { (width, name) ->
                        val selected = vm.penWidthDp == width
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) p.ink else p.surface)
                                .border(1.dp, p.line, RoundedCornerShape(14.dp))
                                .clickable { vm.penWidthDp = width }
                                .semantics {
                                    contentDescription = name
                                    role = Role.RadioButton
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .width(34.dp)
                                    .height((width * 1.4f).dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selected) p.bg else p.ink),
                            )
                        }
                    }
                }

                PadTab.Type -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vm.typedText,
                        onValueChange = { vm.typedText = it.take(if (vm.isInitials) 6 else 40) },
                        label = { Text(if (vm.isInitials) "Your initials" else "Your full name") },
                        singleLine = true,
                        colors = inkTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SignaturePadViewModel.TypeStyleNames.forEachIndexed { i, name ->
                            FilterChip(
                                selected = vm.typeStyle == i,
                                onClick = { vm.typeStyle = i },
                                label = { Text(name, fontFamily = FontFamily(SignaturePadViewModel.typefaceFor(i)), fontSize = 17.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = p.ink,
                                    selectedLabelColor = p.bg,
                                    labelColor = p.ink,
                                ),
                            )
                        }
                    }
                }

                PadTab.Image -> Column {
                    Text("Background removal", style = MaterialTheme.typography.titleSmall, color = p.ink)
                    Slider(
                        value = vm.threshold,
                        onValueChange = { vm.threshold = it },
                        onValueChangeFinished = { vm.reprocess() },
                        valueRange = 0.3f..0.9f,
                        colors = SliderDefaults.colors(thumbColor = p.hi, activeTrackColor = p.hi, inactiveTrackColor = p.surface2),
                    )
                    Text(
                        "Use a photo of your signature on white paper. Slide right if faint strokes disappear.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.muted,
                    )
                }
            }

            vm.error?.let { Text(it, color = p.hi, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
        }

        InkButton(
            text = if (vm.saving) "Saving…" else if (route.forEditor) "Save & place on document" else "Save",
            onClick = {
                vm.save(padSize.width, padSize.height, density.density) { saved ->
                    if (route.forEditor) appViewModel.pendingSignatureId = saved.id
                    appViewModel.pop()
                }
            },
            enabled = vm.canSave && !vm.saving,
            icon = InkIcons.Check,
            kind = ButtonKind.Primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalInk.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.surface2)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) p.surface else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) p.ink else p.muted)
            }
        }
    }
}

@Composable
private fun DrawPad(vm: SignaturePadViewModel, onSize: (IntSize) -> Unit) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        PaperGuides()
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged(onSize)
                .semantics { contentDescription = "Signature drawing area" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        vm.startStroke(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            vm.extendStroke(change.position)
                            change.consume()
                        }
                        vm.endStroke()
                    }
                },
        ) {
            vm.strokes.forEach { drawInk(it.points, it.color, it.widthDp * density.density) }
            if (vm.currentPoints.isNotEmpty()) {
                drawInk(vm.currentPoints.toList(), vm.penColor, vm.penWidthDp * density.density)
            }
        }
        if (vm.strokes.isEmpty() && vm.currentPoints.isEmpty()) {
            Text(
                "Sign here with your finger or a stylus",
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.hint,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
            )
        }
    }
}

private fun DrawScope.drawInk(points: List<Offset>, color: Color, width: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, width / 2f, points[0])
        return
    }
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    var last = points[0]
    for (i in 1 until points.size) {
        val q = points[i - 1]
        val mid = Offset((q.x + points[i].x) / 2f, (q.y + points[i].y) / 2f)
        // Quadratic (last -> mid, control q) expressed as a cubic
        val c1 = Offset(last.x + 2f / 3f * (q.x - last.x), last.y + 2f / 3f * (q.y - last.y))
        val c2 = Offset(mid.x + 2f / 3f * (q.x - mid.x), mid.y + 2f / 3f * (q.y - mid.y))
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, mid.x, mid.y)
        last = mid
    }
    path.lineTo(points.last().x, points.last().y)
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun PaperGuides() {
    Canvas(Modifier.fillMaxSize()) {
        val y = size.height - 64.dp.toPx()
        drawLine(
            Paper.line, Offset(24.dp.toPx(), y), Offset(size.width - 24.dp.toPx(), y), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
    }
    Box(Modifier.fillMaxSize()) {
        Text(
            "×",
            style = TextStyle(fontSize = 22.sp, color = Paper.line),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 66.dp),
        )
    }
}

@Composable
private fun TypedPreview(vm: SignaturePadViewModel) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        PaperGuides()
        Text(
            vm.typedText.ifBlank { if (vm.isInitials) "A.K." else "Your name" },
            fontFamily = FontFamily(SignaturePadViewModel.typefaceFor(vm.typeStyle)),
            fontSize = 54.sp,
            color = if (vm.typedText.isBlank()) Paper.line else vm.penColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ImagePreview(vm: SignaturePadViewModel, onPick: () -> Unit) {
    val p = LocalInk.current
    LaunchedEffect(Unit) { vm.error = null }
    val image = vm.processedImage
    Box(Modifier.fillMaxSize().clickable(onClick = onPick), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(image, contentDescription = "Imported signature", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(16.dp))
            Text(
                "Tap to choose another photo",
                style = MaterialTheme.typography.labelSmall,
                color = Paper.hint,
                modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(InkIcons.Image, contentDescription = null, tint = Paper.hint, modifier = Modifier.size(36.dp))
                Text("Import a photo of your signature", style = MaterialTheme.typography.titleSmall, color = Paper.inkBlack)
                Text("The paper background is removed on this phone", style = MaterialTheme.typography.bodySmall, color = Paper.hint)
            }
        }
        if (vm.isProcessing) CircularProgressIndicator(color = p.hi)
    }
}

@Suppress("unused")
private fun iconFor(tab: PadTab): ImageVector = when (tab) {
    PadTab.Draw -> InkIcons.Pen
    PadTab.Type -> InkIcons.Text
    PadTab.Image -> InkIcons.Image
}
