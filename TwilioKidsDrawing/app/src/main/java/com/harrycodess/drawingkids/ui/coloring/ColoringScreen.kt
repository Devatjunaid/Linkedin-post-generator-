package com.harrycodess.drawingkids.ui.coloring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.harrycodess.drawingkids.art.AnimalArt
import com.harrycodess.drawingkids.data.Prefs
import com.harrycodess.drawingkids.drawing.CanvasDocument
import com.harrycodess.drawingkids.drawing.Tool
import com.harrycodess.drawingkids.ui.studio.StudioScreen
import com.harrycodess.drawingkids.ui.theme.Kid

@Composable
fun ColoringScreen(art: AnimalArt, onBack: () -> Unit) {
    val doc = remember(art.id) { CanvasDocument(Prefs.loadFills(art.id)) }
    DisposableEffect(art.id) {
        onDispose { Prefs.saveFills(art.id, doc.fills.toMap()) }
    }
    StudioScreen(
        title = "Color the ${art.name}",
        isHome = false,
        onLeading = onBack,
        doc = doc,
        tools = listOf(Tool.FILL, Tool.CRAYON, Tool.MARKER, Tool.PENCIL, Tool.ERASER),
        initialTool = Tool.FILL,
        initialColor = Kid.Orange,
        art = art,
        saveLabel = "Done!",
        onSaved = { Prefs.saveFills(art.id, doc.fills.toMap()) },
    )
}

@Composable
fun FreeDrawScreen(doc: CanvasDocument, onHome: () -> Unit) {
    StudioScreen(
        title = "Free Draw",
        isHome = true,
        onLeading = onHome,
        doc = doc,
        tools = listOf(Tool.PENCIL, Tool.MARKER, Tool.CRAYON, Tool.ERASER, Tool.STAMP),
        initialTool = Tool.MARKER,
        initialColor = Kid.Pink,
        art = null,
        saveLabel = "Save",
    )
}
