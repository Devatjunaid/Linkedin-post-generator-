package com.inkformlabs.pdfsigner.ui.home

import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inkformlabs.pdfsigner.ads.NativeAdCard
import com.inkformlabs.pdfsigner.data.db.DocumentEntity
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.ButtonKind
import com.inkformlabs.pdfsigner.ui.components.InkButton
import com.inkformlabs.pdfsigner.ui.components.InkformLogo
import com.inkformlabs.pdfsigner.ui.components.OfflineBadge
import com.inkformlabs.pdfsigner.ui.components.ProgressRing
import com.inkformlabs.pdfsigner.ui.components.inkTextFieldColors
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import com.inkformlabs.pdfsigner.util.Intents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DocumentsTab(
    viewModel: DocumentsViewModel,
    onOpenDocument: (String) -> Unit,
    onCreatedFromImages: (String) -> Unit,
    onOpenSignatures: () -> Unit,
) {
    val p = LocalInk.current
    val context = LocalContext.current
    val docs by viewModel.visibleDocuments.collectAsStateWithLifecycle()
    val allDocs by viewModel.documents.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var renameTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<DocumentEntity?>(null) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importPdf(uri, onImported = onOpenDocument)
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)) { uris ->
        if (uris.isNotEmpty()) viewModel.createFromImages(uris, fromCamera = false, onCreated = onCreatedFromImages)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCaptureResult(success)
    }
    val launchCamera: () -> Unit = {
        try {
            takePicture.launch(viewModel.newCaptureUri(context))
        } catch (e: ActivityNotFoundException) {
            viewModel.message = "No camera app found on this device"
        }
    }
    val openPdfPicker: () -> Unit = {
        try {
            pickPdf.launch(arrayOf("application/pdf"))
        } catch (e: ActivityNotFoundException) {
            viewModel.message = "No file picker available"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InkformLogo(Modifier.size(52.dp), playIntro = false, idle = false, showOrbit = false)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Welcome back", style = MaterialTheme.typography.labelMedium, color = p.muted)
                    Text("Your documents", style = MaterialTheme.typography.headlineSmall, color = p.ink)
                }
            }
        }
        item(key = "badge") { OfflineBadge() }
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                placeholder = { Text("Search your PDFs") },
                leadingIcon = { Icon(InkIcons.Search, contentDescription = null, tint = p.muted) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.query.value = "" }) {
                            Icon(InkIcons.Close, contentDescription = "Clear search", tint = p.muted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = inkTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isEmpty()) {
            item(key = "hero") { HeroCard(onOpenPdf = openPdfPicker) }
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(InkIcons.Import, "Open PDF", Modifier.weight(1f), openPdfPicker)
                    QuickAction(InkIcons.Camera, "Scan", Modifier.weight(1f), launchCamera)
                    QuickAction(InkIcons.Image, "Photos", Modifier.weight(1f)) {
                        pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    QuickAction(InkIcons.Signature, "Signatures", Modifier.weight(1f), onOpenSignatures)
                }
            }
        }
        item(key = "recentTitle") {
            Text(
                if (query.isEmpty()) "Recent" else "Results",
                style = MaterialTheme.typography.titleMedium,
                color = p.ink,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (docs.isEmpty()) {
            item(key = "empty") { EmptyState(searching = query.isNotEmpty() && allDocs.isNotEmpty()) }
            item(key = "adEmpty") { NativeAdCard() }
        } else {
            docs.forEachIndexed { index, doc ->
                item(key = doc.id) {
                    DocumentRow(
                        doc = doc,
                        onOpen = { onOpenDocument(doc.id) },
                        onRename = { renameTarget = doc },
                        onDelete = { deleteTarget = doc },
                        onShare = { viewModel.withShareableCopy(doc.id) { file, name -> Intents.sharePdf(context, file, name) } },
                        onPrint = { viewModel.withShareableCopy(doc.id) { file, name -> Intents.printPdf(context, file, name) } },
                    )
                }
                // A native ad in the natural gap after the second document (or after the only one).
                if (index == 1 || (docs.size == 1 && index == 0)) {
                    item(key = "nativeAd") { NativeAdCard() }
                }
            }
        }
    }

    // Camera scan session
    if (viewModel.capturedPages.isNotEmpty()) {
        ScanSessionSheet(
            pages = viewModel.capturedPages,
            onAddPage = launchCamera,
            onCreate = { viewModel.createFromImages(viewModel.capturedPages.toList(), fromCamera = true, onCreated = onCreatedFromImages) },
            onDiscard = { viewModel.discardCapturedPages() },
        )
    }

    viewModel.passwordRequestUri?.let { uri ->
        PasswordDialog(
            wrongPassword = viewModel.passwordWasWrong,
            onSubmit = { password -> viewModel.importPdf(uri, password, onOpenDocument) },
            onCancel = { viewModel.cancelPassword() },
        )
    }

    renameTarget?.let { doc ->
        RenameDialog(
            current = doc.name,
            onConfirm = { viewModel.rename(doc.id, it); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete “${doc.name}”?") },
            text = { Text("The document and its edits will be removed from this phone. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(doc.id); deleteTarget = null }) { Text("Delete", color = p.hi) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = p.ink) } },
            containerColor = p.surface,
        )
    }
}

@Composable
private fun HeroCard(onOpenPdf: () -> Unit) {
    val p = LocalInk.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(186.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(p.hero),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(p.accent2, radius = 108.dp.toPx(), center = Offset(size.width + 8.dp.toPx(), -12.dp.toPx()))
            drawCircle(
                p.onHero.copy(alpha = 0.3f), radius = 62.dp.toPx(), center = Offset(size.width * 0.55f, size.height + 26.dp.toPx()),
                style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
            )
        }
        WritingIllustration(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 6.dp).size(150.dp))
        Column(
            Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.width(190.dp)) {
                Text(
                    "Fill & sign any PDF",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 26.sp),
                    color = p.onHero,
                )
                Spacer(Modifier.height(6.dp))
                Text("Tap a field, type, sign. Done.", style = MaterialTheme.typography.bodyMedium, color = p.onHero.copy(alpha = 0.85f))
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(p.onHero)
                    .clickable(onClick = onOpenPdf)
                    .heightIn(min = 46.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Open a PDF", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = p.hero)
                Spacer(Modifier.width(8.dp))
                Icon(InkIcons.ArrowRight, contentDescription = null, tint = p.hero, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/** Looping illustration: stacked pages and a pen writing a signature over and over. */
@Composable
private fun WritingIllustration(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "write")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "t")
    val floatY by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "float")
    val signature = remember {
        Path().apply {
            moveTo(42f, 106f)
            cubicTo(48f, 96f, 51f, 96f, 53f, 103f)
            cubicTo(55f, 110f, 59f, 107f, 62f, 101f)
            cubicTo(65f, 95f, 69f, 95f, 70f, 101f)
            cubicTo(71f, 107f, 75f, 104f, 79f, 98f)
        }
    }
    val measure = remember { PathMeasure().apply { setPath(signature, false) } }
    val partial = remember { Path() }
    Canvas(modifier) {
        val u = size.minDimension / 150f
        val bob = kotlin.math.sin(floatY * 2 * Math.PI).toFloat() * 3f
        translate(0f, bob * u) {
            rotate(10f, pivot = Offset(82f * u, 69f * u)) {
                drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(44f * u, 20f * u), Size(76f * u, 98f * u), CornerRadius(8f * u))
            }
            drawRoundRect(Color.White, Offset(30f * u, 24f * u), Size(78f * u, 100f * u), CornerRadius(8f * u))
            drawRoundRect(Color(0xFFD9D6CD), Offset(40f * u, 36f * u), Size(40f * u, 6f * u), CornerRadius(3f * u))
            drawRoundRect(Color(0xFFECEAE3), Offset(40f * u, 50f * u), Size(56f * u, 4f * u), CornerRadius(2f * u))
            drawRoundRect(Color(0xFFECEAE3), Offset(40f * u, 60f * u), Size(50f * u, 4f * u), CornerRadius(2f * u))
            drawRoundRect(
                Color(0xFFB9B5AA), Offset(40f * u, 74f * u), Size(58f * u, 12f * u), CornerRadius(3f * u),
                style = Stroke(1.5f * u, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f * u, 3f * u))),
            )
            val write = (t / 0.65f).coerceIn(0f, 1f)
            partial.reset()
            measure.getSegment(0f, measure.length * write, partial, true)
            scale(u, u, pivot = Offset.Zero) { drawPath(partial, Color(0xFF1F3FBF), style = Stroke(2.6f, cap = StrokeCap.Round)) }
            val tip = measure.getPosition(measure.length * write)
            val penAlpha = if (t > 0.9f) (1f - t) * 10f else 1f
            rotate(-45f, pivot = Offset(tip.x * u, tip.y * u)) {
                translate(tip.x * u, tip.y * u) {
                    drawRoundRect(Color(0xFF17181C).copy(alpha = penAlpha), Offset(7f * u, -3.4f * u), Size(26f * u, 6.8f * u), CornerRadius(1.5f * u))
                    drawRoundRect(Color(0xFFFF7A45).copy(alpha = penAlpha), Offset(30f * u, -3.4f * u), Size(8f * u, 6.8f * u), CornerRadius(3f * u))
                    val cone = Path().apply { moveTo(0f, 0f); lineTo(7.5f * u, -3.4f * u); lineTo(7.5f * u, 3.4f * u); close() }
                    drawPath(cone, Color(0xFFF4F2EC).copy(alpha = penAlpha))
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalInk.current
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(p.surface2),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = p.ink, modifier = Modifier.size(21.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.ink, maxLines = 1)
    }
}

@Composable
private fun DocumentRow(
    doc: DocumentEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
) {
    val p = LocalInk.current
    val container = LocalAppContainer.current
    val thumb by produceState<ImageBitmap?>(initialValue = null, doc.id, doc.updatedAt) {
        value = withContext(Dispatchers.IO) {
            val file = container.documents.thumbnailFile(doc.id)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }
    }
    var menu by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 48.dp, height = 60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, p.line, RoundedCornerShape(10.dp)),
        ) {
            thumb?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(doc.name, style = MaterialTheme.typography.titleSmall, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(statusLine(doc), style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                DateUtils.getRelativeTimeSpanString(doc.updatedAt).toString(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = p.muted,
            )
        }
        val progress = when {
            doc.signed || doc.exported -> 1f
            doc.fieldCount > 0 -> doc.filledCount / doc.fieldCount.toFloat()
            else -> 0f
        }
        ProgressRing(progress = progress, label = if (progress >= 1f) "✓" else "${(progress * 100).toInt()}%")
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(InkIcons.More, contentDescription = "More options for ${doc.name}", tint = p.muted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = p.surface) {
                MenuItem("Open", InkIcons.Edit) { menu = false; onOpen() }
                MenuItem("Rename", InkIcons.Text) { menu = false; onRename() }
                MenuItem("Share", InkIcons.Share) { menu = false; onShare() }
                MenuItem("Print", InkIcons.Print) { menu = false; onPrint() }
                MenuItem("Delete", InkIcons.Trash) { menu = false; onDelete() }
            }
        }
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    val p = LocalInk.current
    DropdownMenuItem(
        text = { Text(text, color = p.ink) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = p.ink) },
        onClick = onClick,
    )
}

private fun statusLine(doc: DocumentEntity): String {
    val pages = "${doc.pageCount} page${if (doc.pageCount == 1) "" else "s"}"
    return when {
        doc.exported -> "Exported · $pages"
        doc.signed -> "Signed · $pages"
        doc.fieldCount > 0 -> "${doc.filledCount} of ${doc.fieldCount} fields · $pages"
        else -> "Ready to fill · $pages"
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    val p = LocalInk.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InkformLogo(Modifier.size(96.dp), playIntro = true, idle = true, introMillis = 1400, showOrbit = false)
        Text(
            if (searching) "No matching documents" else "No documents yet",
            style = MaterialTheme.typography.titleMedium,
            color = p.ink,
        )
        Text(
            if (searching) "Try a different name." else "Open a PDF form, scan a paper or turn photos into a PDF to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = p.muted,
        )
    }
}

@Composable
private fun ScanSessionSheet(pages: List<Uri>, onAddPage: () -> Unit, onCreate: () -> Unit, onDiscard: () -> Unit) {
    val p = LocalInk.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDiscard, sheetState = sheetState, containerColor = p.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Scanned pages (${pages.size})", style = MaterialTheme.typography.titleLarge, color = p.ink)
            Text("Add more pages or turn them into one PDF.", style = MaterialTheme.typography.bodyMedium, color = p.muted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(pages) { uri ->
                    val bitmap by produceState<ImageBitmap?>(null, uri) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
                            }.getOrNull()
                        }
                    }
                    Box(
                        Modifier
                            .size(width = 78.dp, height = 104.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface2),
                    ) {
                        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InkButton("Add page", onClick = onAddPage, kind = ButtonKind.Secondary, icon = InkIcons.Camera, modifier = Modifier.weight(1f))
                InkButton("Create PDF", onClick = onCreate, kind = ButtonKind.Primary, icon = InkIcons.Check, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onDiscard, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Discard scan", color = p.muted)
            }
        }
    }
}

@Composable
private fun PasswordDialog(wrongPassword: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    val p = LocalInk.current
    var password by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(InkIcons.Lock, contentDescription = null, tint = p.hi) },
        title = { Text("Password protected") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the password to open this PDF. An unlocked copy is kept privately on this phone.", color = p.muted)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    isError = wrongPassword,
                    supportingText = { if (wrongPassword) Text("Wrong password, try again") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = inkTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) { Text("Open", color = p.hi) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = p.ink) } },
        containerColor = p.surface,
    )
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val p = LocalInk.current
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename document") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(120) },
                singleLine = true,
                colors = inkTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save", color = p.hi) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = p.ink) } },
        containerColor = p.surface,
    )
}
