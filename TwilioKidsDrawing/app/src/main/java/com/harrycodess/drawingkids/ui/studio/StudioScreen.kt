package com.harrycodess.drawingkids.ui.studio

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harrycodess.drawingkids.ads.BannerAd
import com.harrycodess.drawingkids.art.AnimalArt
import com.harrycodess.drawingkids.audio.Sfx
import com.harrycodess.drawingkids.data.Gallery
import com.harrycodess.drawingkids.drawing.ArtworkRenderer
import com.harrycodess.drawingkids.drawing.BrushSizesDp
import com.harrycodess.drawingkids.drawing.CanvasDocument
import com.harrycodess.drawingkids.drawing.DrawingSurface
import com.harrycodess.drawingkids.drawing.Tool
import com.harrycodess.drawingkids.ui.components.Chunky
import com.harrycodess.drawingkids.ui.components.ConfirmDialog
import com.harrycodess.drawingkids.ui.components.Confetti
import com.harrycodess.drawingkids.ui.components.Icon32
import com.harrycodess.drawingkids.ui.components.IconButton32
import com.harrycodess.drawingkids.ui.components.KidIcons.backIcon
import com.harrycodess.drawingkids.ui.components.KidIcons.homeIcon
import com.harrycodess.drawingkids.ui.components.KidIcons.redoIcon
import com.harrycodess.drawingkids.ui.components.KidIcons.saveIcon
import com.harrycodess.drawingkids.ui.components.KidIcons.trashIcon
import com.harrycodess.drawingkids.ui.components.KidIcons.undoIcon
import com.harrycodess.drawingkids.ui.components.StickerLabel
import com.harrycodess.drawingkids.ui.components.Swatch
import com.harrycodess.drawingkids.ui.components.ToolIcon
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.kidText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Tool.label() = when (this) {
    Tool.PENCIL -> "Pencil"
    Tool.MARKER -> "Marker"
    Tool.CRAYON -> "Crayon"
    Tool.ERASER -> "Eraser"
    Tool.STAMP -> "Stamp"
    Tool.FILL -> "Fill"
}

/**
 * Shared screen for Free Draw and Coloring: top bar, tool rail, paper, crayon box.
 * Lays out side panels in landscape and stacked bars in portrait.
 */
@Composable
fun StudioScreen(
    title: String,
    isHome: Boolean,
    onLeading: () -> Unit,
    doc: CanvasDocument,
    tools: List<Tool>,
    initialTool: Tool,
    initialColor: Color,
    art: AnimalArt?,
    saveLabel: String,
    onSaved: () -> Unit = {},
) {
    var tool by remember { mutableStateOf(initialTool) }
    var color by remember { mutableStateOf(initialColor) }
    var sizeIndex by remember { mutableIntStateOf(1) }
    var askClear by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var savedBadge by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun save() {
        if (saving) return
        saving = true
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) { ArtworkRenderer.render(doc, art) }
            withContext(Dispatchers.IO) { Gallery.save(bitmap, art?.name ?: "Drawing") }
            onSaved()
            Sfx.cheer()
            confetti++
            savedBadge = true
            saving = false
            delay(1700)
            savedBadge = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 560.dp
            val btn = if (compact) 46.dp else 56.dp
            Row(
                Modifier.fillMaxWidth().padding(horizontal = if (compact) 8.dp else 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
            ) {
                IconButton32(onLeading, size = btn) { if (isHome) homeIcon() else backIcon() }
                Text(
                    title, style = kidText(if (compact) 19 else 26), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton32({ doc.undo() }, size = btn, enabled = doc.canUndo) { undoIcon(doc.canUndo) }
                IconButton32({ doc.redo() }, size = btn, enabled = doc.canRedo) { redoIcon(doc.canRedo) }
                IconButton32({ askClear = true }, size = btn) { trashIcon() }
                Chunky(Modifier.height(btn), corner = 18.dp, onClick = { save() }) {
                    Row(
                        Modifier.padding(horizontal = if (compact) 9.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon32(Modifier.size(if (compact) 24.dp else 28.dp)) { saveIcon() }
                        if (!compact) Text(saveLabel, style = kidText(19))
                    }
                }
            }
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight
            val paper = @Composable { modifier: Modifier ->
                Box(
                    modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .border(4.dp, Kid.Ink, RoundedCornerShape(26.dp)),
                ) {
                    DrawingSurface(
                        doc = doc, tool = tool, color = color, sizeIndex = sizeIndex, art = art,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        onFill = { Sfx.bloop() },
                        onStamp = { Sfx.stamp() },
                    )
                    if (tool == Tool.FILL) {
                        Text(
                            "Tap a shape to fill it!",
                            style = kidText(15),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .background(Kid.OrangeSoft, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            if (landscape) {
                Row(
                    Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ToolPanel(tools, tool, color, vertical = true) { tool = it }
                    paper(Modifier.weight(1f).fillMaxHeight())
                    ColorPanel(color, sizeIndex, vertical = true, onColor = { color = it }, onSize = { sizeIndex = it })
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToolPanel(tools, tool, color, vertical = false) { tool = it }
                    paper(Modifier.weight(1f).fillMaxWidth())
                    ColorPanel(color, sizeIndex, vertical = false, onColor = { color = it }, onSize = { sizeIndex = it })
                }
            }

            Confetti(confetti)
            androidx.compose.animation.AnimatedVisibility(
                savedBadge,
                modifier = Modifier.align(Alignment.Center),
                enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                StickerLabel("Saved to Gallery!", Kid.Yellow)
            }
        }

        BannerAd()
    }

    if (askClear) {
        ConfirmDialog(
            title = "Start over?",
            yes = "Clear",
            onYes = {
                doc.clear()
                askClear = false
            },
            onDismiss = { askClear = false },
        )
    }
}

@Composable
private fun ToolPanel(tools: List<Tool>, selected: Tool, color: Color, vertical: Boolean, onSelect: (Tool) -> Unit) {
    val buttons = @Composable {
        tools.forEach { t ->
            val on = t == selected
            Chunky(
                Modifier.size(width = 78.dp, height = 82.dp),
                color = if (on) Kid.PinkSoft else Color.White,
                border = if (on) Kid.Pink else null,
                corner = 22.dp,
                depth = 4.dp,
                onClick = { onSelect(t) },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ToolIcon(t, if (t == Tool.ERASER) Kid.Blush else color, Modifier.size(44.dp))
                    Text(t.label(), style = kidText(13))
                }
            }
        }
    }
    val panel = Modifier
        .background(Color.White, RoundedCornerShape(28.dp))
        .border(2.dp, Kid.Line, RoundedCornerShape(28.dp))
    if (vertical) {
        Column(
            panel.fillMaxHeight().verticalScroll(rememberScrollState()).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { buttons() }
    } else {
        Row(
            panel.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) { buttons() }
    }
}

@Composable
private fun SizeDot(index: Int, selected: Boolean, onClick: () -> Unit) {
    val dot = (BrushSizesDp[index] * 0.9f + 4f).dp
    Box(
        Modifier
            .size(40.dp)
            .background(if (selected) Kid.PinkSoft else Color.Transparent, RoundedCornerShape(12.dp))
            .border(3.dp, if (selected) Kid.Pink else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                Sfx.tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(dot).background(Kid.Ink, CircleShape))
    }
}

@Composable
private fun ColorPanel(
    color: Color,
    sizeIndex: Int,
    vertical: Boolean,
    onColor: (Color) -> Unit,
    onSize: (Int) -> Unit,
) {
    val panel = Modifier
        .background(Color.White, RoundedCornerShape(28.dp))
        .border(2.dp, Kid.Line, RoundedCornerShape(28.dp))
    if (vertical) {
        Column(
            panel.width(112.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Colors", style = kidText(16))
            Kid.Palette.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { c -> Swatch(c, c == color, 38.dp) { onColor(c) } }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Size", style = kidText(16))
            listOf(0, 2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SizeDot(row, sizeIndex == row) { onSize(row) }
                    SizeDot(row + 1, sizeIndex == row + 1) { onSize(row + 1) }
                }
            }
        }
    } else {
        Column(panel.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Kid.Palette.forEach { c -> Swatch(c, c == color, 36.dp) { onColor(c) } }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Size", style = kidText(15))
                BrushSizesDp.indices.forEach { i -> SizeDot(i, sizeIndex == i) { onSize(i) } }
            }
        }
    }
}
