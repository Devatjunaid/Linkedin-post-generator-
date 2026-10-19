package com.inkformlabs.pdfsigner.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inkformlabs.pdfsigner.data.db.SignatureEntity
import com.inkformlabs.pdfsigner.pdf.FieldType
import com.inkformlabs.pdfsigner.pdf.FieldWidget
import com.inkformlabs.pdfsigner.pdf.FormField
import com.inkformlabs.pdfsigner.pdf.Overlay
import com.inkformlabs.pdfsigner.pdf.OverlayKind
import com.inkformlabs.pdfsigner.pdf.PageGeom
import com.inkformlabs.pdfsigner.pdf.PdfEngine
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.BusyOverlay
import com.inkformlabs.pdfsigner.ui.components.ButtonKind
import com.inkformlabs.pdfsigner.ui.components.CircleIconButton
import com.inkformlabs.pdfsigner.ui.components.InkButton
import com.inkformlabs.pdfsigner.ui.components.InkformLogo
import com.inkformlabs.pdfsigner.ui.components.inkTextFieldColors
import com.inkformlabs.pdfsigner.ui.home.MenuItem
import com.inkformlabs.pdfsigner.ui.nav.AppViewModel
import com.inkformlabs.pdfsigner.ui.nav.Route
import com.inkformlabs.pdfsigner.ui.signature.SignatureThumb
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val FieldFill = Color(0xFFE8F0FF)
private val FieldBorder = Color(0xFF7D93C9)
private val PageInk = Color(0xFF17181C)

@Composable
fun EditorScreen(documentId: String, appViewModel: AppViewModel) {
    val p = LocalInk.current
    val container = LocalAppContainer.current
    val activity = LocalActivity.current
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(documentId, container))
    val state by vm.state.collectAsStateWithLifecycle()
    val signatureBitmaps by vm.signatureBitmaps.collectAsStateWithLifecycle()
    val savedSignatures by container.signatures.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showDiscard by remember { mutableStateOf(false) }
    var showSignatureSheet by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmRevert by remember { mutableStateOf(false) }

    val currentPage = listState.firstVisibleItemIndex

    // Signature created on the pad for this editor
    val pendingSignature = appViewModel.pendingSignatureId
    LaunchedEffect(pendingSignature) {
        if (pendingSignature != null) {
            appViewModel.pendingSignatureId = null
            vm.placeSignature(pendingSignature, currentPage)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    BackHandler {
        when {
            state.activeField != null -> vm.activateField(null)
            state.selectedOverlay != null -> vm.selectOverlay(null)
            state.tool != EditorTool.None -> vm.setTool(EditorTool.None)
            state.dirty -> showDiscard = true
            else -> appViewModel.pop()
        }
    }

    val finish: () -> Unit = {
        vm.save {
            // Finishing a document is a major task: natural break for a rate-limited interstitial.
            container.ads.showInterstitialThen(activity) { appViewModel.push(Route.Export(documentId)) }
        }
    }

    val openSignatureFlow: () -> Unit = {
        if (savedSignatures.isEmpty()) {
            appViewModel.push(Route.SignaturePad(forEditor = true))
        } else {
            showSignatureSheet = true
        }
    }

    Scaffold(
        containerColor = p.surface2,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.background(p.bg).statusBarsPadding()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(InkIcons.Back, "Back", onClick = {
                        if (state.dirty) showDiscard = true else appViewModel.pop()
                    })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.title, style = MaterialTheme.typography.titleSmall, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                state.fillable.isNotEmpty() -> "${state.filledCount} of ${state.fillable.size} fields · ${state.pages.size} pages"
                                state.pages.isNotEmpty() -> "${state.pages.size} page${if (state.pages.size == 1) "" else "s"} · add text & signatures"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = p.muted,
                            maxLines = 1,
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(InkIcons.More, contentDescription = "More options", tint = p.ink) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (state.fillable.isNotEmpty()) {
                                MenuItem("Auto-fill from my details", InkIcons.Sparkle) {
                                    menu = false
                                    scope.launch {
                                        val profile = container.profile.profile.first()
                                        if (profile.isEmpty) {
                                            vm.showMessage("Add your details in the “My details” tab first")
                                        } else {
                                            val n = vm.autoFill(profile)
                                            vm.showMessage(if (n == 0) "No empty fields matched your details" else "Filled $n field${if (n == 1) "" else "s"}")
                                        }
                                    }
                                }
                            }
                            MenuItem("Zoom in", InkIcons.ZoomIn) { menu = false; vm.zoom(0.5f) }
                            MenuItem("Zoom out", InkIcons.ZoomOut) { menu = false; vm.zoom(-0.5f) }
                            MenuItem("Revert to original", InkIcons.Revert) { menu = false; confirmRevert = true }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    InkButton("Done", onClick = finish, kind = ButtonKind.Accent, icon = InkIcons.Check, height = 44.dp, enabled = !state.loading)
                }
                if (state.fillable.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { state.filledCount / state.fillable.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = p.hi,
                        trackColor = p.line,
                    )
                }
            }
        },
        bottomBar = {
            Box(Modifier.background(p.bg).navigationBarsPadding().imePadding()) {
                val active = state.activeFieldObject
                val selected = state.overlays.firstOrNull { it.id == state.selectedOverlay }
                AnimatedContent(
                    targetState = when {
                        active != null -> "field"
                        selected != null -> "overlay"
                        else -> "tools"
                    },
                    transitionSpec = {
                        (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(160)))
                    },
                    label = "bottomPanel",
                ) { panel ->
                    when (panel) {
                        "field" -> {
                            if (active != null) FieldPanel(
                                field = active,
                                value = state.values[active.name].orEmpty(),
                                index = state.fillable.indexOfFirst { it.name == active.name },
                                total = state.fillable.size,
                                onValue = { vm.setValue(active.name, it) },
                                onToggle = { vm.toggleCheckbox(active) },
                                onRadio = { vm.selectRadio(active, it) },
                                onPrev = { vm.stepField(false) },
                                onNext = { vm.stepField(true) },
                                onClose = { vm.activateField(null) },
                            )
                        }

                        "overlay" -> {
                            if (selected != null) OverlayBar(
                                overlay = selected,
                                onEdit = { vm.startEditingText(selected.id) },
                                onColor = { vm.setOverlayColor(selected.id, it) },
                                onSmaller = { vm.resizeOverlay(selected.id, 0.85f) },
                                onBigger = { vm.resizeOverlay(selected.id, 1.18f) },
                                onDelete = { vm.deleteOverlay(selected.id) },
                                onDone = { vm.selectOverlay(null) },
                            )
                        }

                        else -> Toolbar(
                            state = state,
                            onNextField = {
                                if (!vm.nextEmptyField()) vm.showMessage("All fields are filled. Tap Done when you're ready.")
                            },
                            onTool = vm::setTool,
                            onSign = openSignatureFlow,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.pages.isEmpty() -> LoadingState()
                state.error != null -> ErrorState(state.error.orEmpty()) { appViewModel.pop() }
                else -> PagesList(
                    state = state,
                    vm = vm,
                    signatureBitmaps = signatureBitmaps,
                    listState = listState,
                    onSignatureField = { field ->
                        vm.targetSignatureField(field)
                        openSignatureFlow()
                    },
                )
            }

            AnimatedVisibility(
                visible = state.tool != EditorTool.None,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(p.ink)
                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Tap on the page to place the ${toolName(state.tool)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = p.bg,
                    )
                    IconButton(onClick = { vm.setTool(EditorTool.None) }) {
                        Icon(InkIcons.Close, contentDescription = "Cancel", tint = p.bg, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Keep the active field in view
    val density = LocalDensity.current
    LaunchedEffect(state.activeField) {
        val field = state.activeFieldObject ?: return@LaunchedEffect
        val widget = field.firstWidget
        val layout = listState.layoutInfo
        val pageItem = layout.visibleItemsInfo.firstOrNull { it.index == widget.page }
        val pageHeight = pageItem?.size ?: layout.visibleItemsInfo.firstOrNull()?.size ?: 0
        val margin = with(density) { 96.dp.toPx() }
        val offset = (widget.rect.top * pageHeight - margin).roundToInt().coerceAtLeast(0)
        listState.animateScrollToItem(widget.page, offset)
    }

    if (state.saving) BusyOverlay("Saving your PDF…")

    state.editingTextOverlay?.let { id ->
        val overlay = state.overlays.firstOrNull { it.id == id }
        if (overlay != null) {
            TextOverlayDialog(
                initial = overlay.text,
                onConfirm = { vm.finishEditingText(id, it) },
                onDismiss = { vm.finishEditingText(id, null) },
            )
        }
    }

    if (showSignatureSheet) {
        SignatureSheet(
            signatures = savedSignatures,
            onPick = {
                showSignatureSheet = false
                vm.placeSignature(it.id, currentPage)
            },
            onCreateNew = {
                showSignatureSheet = false
                appViewModel.push(Route.SignaturePad(forEditor = true))
            },
            onDismiss = {
                showSignatureSheet = false
                vm.clearSignatureTarget()
            },
        )
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Save your changes?") },
            text = { Text("You have unsaved edits in this document.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    vm.save { appViewModel.pop() }
                }) { Text("Save", color = p.hi) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscard = false
                    appViewModel.pop()
                }) { Text("Discard", color = p.ink) }
            },
            containerColor = p.surface,
        )
    }

    if (confirmRevert) {
        AlertDialog(
            onDismissRequest = { confirmRevert = false },
            title = { Text("Revert to the original?") },
            text = { Text("All filled values and signatures added in Inkform will be removed from this document.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRevert = false
                    vm.revertToOriginal()
                }) { Text("Revert", color = p.hi) }
            },
            dismissButton = { TextButton(onClick = { confirmRevert = false }) { Text("Cancel", color = p.ink) } },
            containerColor = p.surface,
        )
    }
}

private fun toolName(tool: EditorTool) = when (tool) {
    EditorTool.Text -> "text"
    EditorTool.Date -> "date"
    EditorTool.Check -> "tick"
    EditorTool.Cross -> "cross"
    EditorTool.None -> ""
}

@Composable
private fun LoadingState() {
    val p = LocalInk.current
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        InkformLogo(Modifier.size(120.dp), playIntro = true, idle = true, introMillis = 1300)
        Spacer(Modifier.height(12.dp))
        Text("Opening document…", style = MaterialTheme.typography.titleSmall, color = p.muted)
    }
}

@Composable
private fun ErrorState(message: String, onBack: () -> Unit) {
    val p = LocalInk.current
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Icon(InkIcons.Info, contentDescription = null, tint = p.hi, modifier = Modifier.size(40.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, color = p.ink)
        InkButton("Back to documents", onClick = onBack)
    }
}

@Composable
private fun PagesList(
    state: EditorState,
    vm: EditorViewModel,
    signatureBitmaps: Map<String, ImageBitmap>,
    listState: LazyListState,
    onSignatureField: (FormField) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = maxWidth
        val pageWidth = (viewport - 24.dp) * state.zoom
        val scrollX = rememberScrollState()
        Box(Modifier.fillMaxSize().horizontalScroll(scrollX, enabled = state.zoom > 1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.requiredWidth(pageWidth + 24.dp).fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(state.pages, key = { index, _ -> "page$index" }) { index, geom ->
                    PageView(
                        index = index,
                        geom = geom,
                        pageWidth = pageWidth,
                        state = state,
                        vm = vm,
                        signatureBitmaps = signatureBitmaps,
                        onSignatureField = onSignatureField,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageView(
    index: Int,
    geom: PageGeom,
    pageWidth: Dp,
    state: EditorState,
    vm: EditorViewModel,
    signatureBitmaps: Map<String, ImageBitmap>,
    onSignatureField: (FormField) -> Unit,
) {
    val density = LocalDensity.current
    val pageWpx = with(density) { pageWidth.toPx() }
    val pageHpx = pageWpx * geom.displayH / geom.displayW
    val pageHeight = with(density) { pageHpx.toDp() }
    val renderWidth = pageWpx.roundToInt().coerceIn(200, 2400)
    val bitmap by produceState<ImageBitmap?>(null, index, renderWidth, state.renderVersion) {
        value = vm.renderPage(index, renderWidth)?.asImageBitmap()
    }

    Box(
        Modifier
            .width(pageWidth)
            .height(pageHeight)
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .background(Color.White)
            .pointerInput(index) {
                detectTapGestures(onTap = { offset -> vm.placeAt(index, offset.x / size.width, offset.y / size.height) })
            },
    ) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = "Page ${index + 1}", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        }

        state.fields.forEach { field ->
            field.widgets.forEach { widget ->
                if (widget.page == index) {
                    FieldBox(
                        field = field,
                        widget = widget,
                        value = state.values[field.name].orEmpty(),
                        active = field.name == state.activeField,
                        pageWpx = pageWpx,
                        pageHpx = pageHpx,
                        onTap = {
                            when (field.type) {
                                FieldType.CHECKBOX -> vm.toggleCheckbox(field)
                                FieldType.RADIO -> vm.selectRadio(field, widget.onValue)
                                FieldType.SIGNATURE -> onSignatureField(field)
                                else -> vm.activateField(field.name)
                            }
                        },
                    )
                }
            }
        }

        state.overlays.forEach { overlay ->
            if (overlay.page == index) {
                OverlayView(
                    overlay = overlay,
                    selected = overlay.id == state.selectedOverlay,
                    bitmap = overlay.signatureId?.let { signatureBitmaps[it] },
                    pageWpx = pageWpx,
                    pageHpx = pageHpx,
                    vm = vm,
                )
            }
        }

        Text(
            "${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8A877E),
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
        )
    }
}

@Composable
private fun FieldBox(
    field: FormField,
    widget: FieldWidget,
    value: String,
    active: Boolean,
    pageWpx: Float,
    pageHpx: Float,
    onTap: () -> Unit,
) {
    val p = LocalInk.current
    val density = LocalDensity.current
    val r = widget.rect
    val wPx = r.width * pageWpx
    val hPx = r.height * pageHpx
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseWidth by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseWidth",
    )
    val pulse = if (active) pulseWidth else 1f
    val shape = if (field.type == FieldType.RADIO) CircleShape else RoundedCornerShape(2.dp)

    Box(
        Modifier
            .offset { IntOffset((r.left * pageWpx).roundToInt(), (r.top * pageHpx).roundToInt()) }
            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
            .clip(shape)
            .background(if (active) FieldFill.copy(alpha = 0.95f) else FieldFill.copy(alpha = 0.82f))
            .border(if (active) pulse.dp else 1.dp, if (active) p.hi else FieldBorder, shape)
            .clickable(onClick = onTap),
        contentAlignment = if (field.multiline) Alignment.TopStart else Alignment.CenterStart,
    ) {
        when (field.type) {
            FieldType.TEXT, FieldType.CHOICE -> {
                val shown = if (field.type == FieldType.CHOICE) {
                    field.options.firstOrNull { it.export == value }?.display ?: value
                } else {
                    value
                }
                val sizePx = if (field.multiline) (hPx * 0.62f).coerceAtMost(with(density) { 13.sp.toPx() }) else hPx * 0.62f
                Text(
                    shown,
                    style = TextStyle(fontSize = with(density) { sizePx.toSp() }, color = PageInk),
                    maxLines = if (field.multiline) 50 else 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }

            FieldType.CHECKBOX -> {
                if (value.isNotEmpty()) {
                    Icon(InkIcons.Tick, contentDescription = "Checked", tint = PageInk, modifier = Modifier.fillMaxSize().padding(1.dp))
                }
            }

            FieldType.RADIO -> {
                if (value.isNotEmpty() && value == widget.onValue) {
                    Box(Modifier.fillMaxSize().padding(3.dp).clip(CircleShape).background(PageInk))
                }
            }

            FieldType.SIGNATURE -> Row(
                Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(InkIcons.Signature, contentDescription = null, tint = FieldBorder, modifier = Modifier.size(with(density) { (hPx * 0.6f).toDp() }))
                Text(
                    "Sign here",
                    style = TextStyle(fontSize = with(density) { (hPx * 0.4f).toSp() }, color = FieldBorder),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun OverlayView(
    overlay: Overlay,
    selected: Boolean,
    bitmap: ImageBitmap?,
    pageWpx: Float,
    pageHpx: Float,
    vm: EditorViewModel,
) {
    val p = LocalInk.current
    val density = LocalDensity.current
    val isText = overlay.kind == OverlayKind.TEXT || overlay.kind == OverlayKind.DATE
    val hPx = overlay.h * pageHpx
    val wPx = overlay.w * pageWpx
    val color = Color(overlay.color)
    val currentSelected by rememberUpdatedState(selected)
    val currentW by rememberUpdatedState(wPx)

    val sizeModifier = if (isText) {
        Modifier.height(with(density) { hPx.toDp() }).wrapContentWidth(unbounded = true)
    } else {
        Modifier.size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
    }

    Box(
        Modifier
            .offset { IntOffset((overlay.x * pageWpx).roundToInt(), (overlay.y * pageHpx).roundToInt()) }
            .then(sizeModifier)
            .border(if (selected) 1.5.dp else 0.dp, if (selected) p.hi else Color.Transparent, RoundedCornerShape(3.dp))
            .pointerInput(overlay.id, pageWpx, pageHpx) {
                detectDragGestures(
                    onDragStart = { _ -> vm.selectOverlay(overlay.id) },
                    onDrag = { change, drag ->
                        change.consume()
                        vm.moveOverlay(overlay.id, drag.x / pageWpx, drag.y / pageHpx)
                    },
                )
            }
            .pointerInput(overlay.id) {
                detectTapGestures(onTap = { _ ->
                    if (currentSelected && isText) vm.startEditingText(overlay.id) else vm.selectOverlay(overlay.id)
                })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        when (overlay.kind) {
            OverlayKind.TEXT, OverlayKind.DATE -> Text(
                overlay.text.ifEmpty { " " },
                style = TextStyle(
                    fontSize = with(density) { (hPx * Overlay.TEXT_SIZE_RATIO).toSp() },
                    color = color,
                    lineHeight = with(density) { (hPx * Overlay.TEXT_SIZE_RATIO).toSp() },
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.onSizeChanged { size ->
                    if (size.width > 0) vm.reportOverlayWidth(overlay.id, size.width / pageWpx)
                },
            )

            OverlayKind.CHECK -> Canvas(Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.16f, size.height * 0.55f)
                    lineTo(size.width * 0.42f, size.height * 0.80f)
                    lineTo(size.width * 0.86f, size.height * 0.22f)
                }
                drawPath(path, color, style = Stroke(size.height * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            OverlayKind.CROSS -> Canvas(Modifier.fillMaxSize()) {
                val stroke = size.height * 0.13f
                drawLine(color, Offset(size.width * 0.2f, size.height * 0.2f), Offset(size.width * 0.8f, size.height * 0.8f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.8f, size.height * 0.2f), Offset(size.width * 0.2f, size.height * 0.8f), stroke, StrokeCap.Round)
            }

            OverlayKind.SIGNATURE -> {
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = "Signature", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                }
            }
        }

        if (selected) {
            // Resize handle
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(10.dp, 10.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(p.hi)
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(overlay.id) {
                        detectDragGestures(
                            onDrag = { change, drag ->
                                change.consume()
                                val base = currentW.coerceAtLeast(1f)
                                vm.resizeOverlay(overlay.id, 1f + drag.x / base)
                            },
                        )
                    },
            )
        }
    }
}

@Composable
private fun Toolbar(state: EditorState, onNextField: () -> Unit, onTool: (EditorTool) -> Unit, onSign: () -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state.fillable.isNotEmpty()) {
            item { ToolButton(InkIcons.Fields, "Next field", selected = false, onClick = onNextField) }
        }
        item { ToolButton(InkIcons.Text, "Text", state.tool == EditorTool.Text) { onTool(EditorTool.Text) } }
        item { ToolButton(InkIcons.Calendar, "Date", state.tool == EditorTool.Date) { onTool(EditorTool.Date) } }
        item { ToolButton(InkIcons.Tick, "Tick", state.tool == EditorTool.Check) { onTool(EditorTool.Check) } }
        item { ToolButton(InkIcons.Cross, "Cross", state.tool == EditorTool.Cross) { onTool(EditorTool.Cross) } }
        item { ToolButton(InkIcons.Signature, "Sign", selected = false, highlight = true, onClick = onSign) }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, selected: Boolean, highlight: Boolean = false, onClick: () -> Unit) {
    val p = LocalInk.current
    val bg = when {
        selected -> p.ink
        highlight -> p.accent
        else -> p.surface
    }
    val fg = when {
        selected -> p.bg
        highlight -> p.onAccent
        else -> p.ink
    }
    Column(
        Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, if (selected || highlight) Color.Transparent else p.line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}

@Composable
private fun FieldPanel(
    field: FormField,
    value: String,
    index: Int,
    total: Int,
    onValue: (String) -> Unit,
    onToggle: () -> Unit,
    onRadio: (String?) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val p = LocalInk.current
    val focus = remember { FocusRequester() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                field.label + if (field.required) " *" else "",
                style = MaterialTheme.typography.titleSmall,
                color = p.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (index >= 0) Text("${index + 1} / $total", style = MaterialTheme.typography.labelMedium, color = p.muted)
            IconButton(onClick = onClose) { Icon(InkIcons.Close, contentDescription = "Close field", tint = p.muted) }
        }

        when (field.type) {
            FieldType.TEXT -> {
                var text by remember(field.name) { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
                LaunchedEffect(field.name) { runCatching { focus.requestFocus() } }
                LaunchedEffect(value) {
                    if (value != text.text) text = TextFieldValue(value, TextRange(value.length))
                }
                val hint = (field.name + " " + field.label).lowercase()
                val keyboardType = when {
                    "mail" in hint -> KeyboardType.Email
                    "phone" in hint || "tel" in hint || "mobile" in hint -> KeyboardType.Phone
                    "zip" in hint || "postal" in hint || "amount" in hint || "number" in hint -> KeyboardType.Number
                    else -> KeyboardType.Text
                }
                val counter: (@Composable () -> Unit)? = if (field.maxLength > 0) {
                    { Text("${text.text.length} / ${field.maxLength}") }
                } else {
                    null
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        val limited = if (field.maxLength > 0 && it.text.length > field.maxLength) {
                            it.copy(text = it.text.take(field.maxLength), selection = TextRange(field.maxLength))
                        } else {
                            it
                        }
                        text = limited
                        onValue(limited.text)
                    },
                    singleLine = !field.multiline,
                    minLines = if (field.multiline) 2 else 1,
                    maxLines = if (field.multiline) 4 else 1,
                    placeholder = { Text("Type here") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                        keyboardType = keyboardType,
                        imeAction = if (field.multiline) ImeAction.Default else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onNext() }),
                    supportingText = counter,
                    colors = inkTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }

            FieldType.CHOICE -> {
                var open by remember { mutableStateOf(false) }
                val shown = field.options.firstOrNull { it.export == value }?.display ?: value.ifEmpty { "Choose an option" }
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.surface)
                            .border(1.dp, p.line, RoundedCornerShape(14.dp))
                            .clickable { open = true }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(shown, style = MaterialTheme.typography.bodyLarge, color = p.ink, modifier = Modifier.weight(1f))
                        Icon(InkIcons.Forward, contentDescription = null, tint = p.muted)
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        field.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.display, color = p.ink) },
                                onClick = {
                                    open = false
                                    onValue(option.export)
                                },
                            )
                        }
                    }
                }
            }

            FieldType.CHECKBOX -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (value.isNotEmpty()) "Checked" else "Not checked",
                    style = MaterialTheme.typography.bodyLarge,
                    color = p.ink,
                    modifier = Modifier.weight(1f),
                )
                InkButton(if (value.isNotEmpty()) "Uncheck" else "Check", onClick = onToggle, kind = ButtonKind.Secondary, height = 44.dp)
            }

            FieldType.RADIO -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                field.widgets.mapNotNull { it.onValue }.distinct().forEach { option ->
                    FilterChip(
                        selected = value == option,
                        onClick = { onRadio(option) },
                        label = { Text(PdfEngine.prettify(option)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = p.ink,
                            selectedLabelColor = p.bg,
                            labelColor = p.ink,
                        ),
                    )
                }
            }

            FieldType.SIGNATURE -> Unit
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InkButton("Previous", onClick = onPrev, kind = ButtonKind.Secondary, icon = InkIcons.Back, enabled = index > 0, height = 48.dp, modifier = Modifier.weight(1f))
            val last = index >= total - 1
            InkButton(
                if (last) "Finish" else "Next",
                onClick = if (last) onClose else onNext,
                kind = ButtonKind.Primary,
                icon = if (last) InkIcons.Check else InkIcons.Forward,
                height = 48.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverlayBar(
    overlay: Overlay,
    onEdit: () -> Unit,
    onColor: (Int) -> Unit,
    onSmaller: () -> Unit,
    onBigger: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val p = LocalInk.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Drag to move · use the round handle or buttons to resize",
            style = MaterialTheme.typography.bodySmall,
            color = p.muted,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (overlay.kind == OverlayKind.TEXT || overlay.kind == OverlayKind.DATE) {
                CircleIconButton(InkIcons.Edit, "Edit text", onEdit)
            }
            if (overlay.kind != OverlayKind.SIGNATURE) {
                listOf(Overlay.INK_BLACK to "Black ink", Overlay.INK_BLUE to "Blue ink").forEach { (c, name) ->
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (overlay.color == c) p.ink else Color.Transparent, CircleShape)
                            .clickable { onColor(c) }
                            .padding(9.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .semantics {
                                contentDescription = name
                                role = Role.Button
                            },
                    )
                }
            }
            CircleIconButton(InkIcons.Minus, "Smaller", onSmaller)
            CircleIconButton(InkIcons.Plus, "Bigger", onBigger)
            CircleIconButton(InkIcons.Trash, "Delete", onDelete)
            Spacer(Modifier.weight(1f))
            InkButton("Done", onClick = onDone, height = 46.dp)
        }
    }
}

@Composable
private fun TextOverlayDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val p = LocalInk.current
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add text") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.copy(text = it.text.replace("\n", " ").take(200)) },
                singleLine = true,
                placeholder = { Text("Type the text to place on the page") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(text.text) }),
                colors = inkTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.text) }) { Text("Place", color = p.hi) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = p.ink) } },
        containerColor = p.surface,
    )
}

@Composable
private fun SignatureSheet(
    signatures: List<SignatureEntity>,
    onPick: (SignatureEntity) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalInk.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = p.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Choose a signature", style = MaterialTheme.typography.titleLarge, color = p.ink)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(signatures, key = { it.id }) { sig ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SignatureThumb(
                            sig,
                            Modifier
                                .size(width = 150.dp, height = 90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, p.line, RoundedCornerShape(16.dp))
                                .clickable { onPick(sig) },
                        )
                        Text(if (sig.kind == "initials") "Initials" else "Signature", style = MaterialTheme.typography.labelSmall, color = p.muted)
                    }
                }
            }
            InkButton("Create a new signature", onClick = onCreateNew, icon = InkIcons.Plus, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
        }
    }
}
