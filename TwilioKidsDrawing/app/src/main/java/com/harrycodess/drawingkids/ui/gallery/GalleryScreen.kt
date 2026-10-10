package com.harrycodess.drawingkids.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.harrycodess.drawingkids.ads.BannerAd
import com.harrycodess.drawingkids.data.Artwork
import com.harrycodess.drawingkids.data.Gallery
import com.harrycodess.drawingkids.ui.components.ArtThumb
import com.harrycodess.drawingkids.ui.components.ConfirmDialog
import com.harrycodess.drawingkids.ui.components.IconButton32
import com.harrycodess.drawingkids.ui.components.KidIcons.closeIcon
import com.harrycodess.drawingkids.ui.components.KidIcons.homeIcon
import com.harrycodess.drawingkids.ui.components.KidIcons.trashIcon
import com.harrycodess.drawingkids.ui.components.Showcase
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.kidText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tapes = listOf(Kid.Yellow, Kid.CardMint, Kid.Blush, Kid.CardLilac)
private val mats = listOf(Kid.OrangeSoft, Color(0xFFE6F3FF), Color(0xFFF1FBEA), Color(0xFFFFEFF5))
private val tilts = listOf(-3f, 2f, -1.5f, 3f, 2f, -2.5f, 1.5f, -1f)

@Composable
private fun rememberImage(file: File, maxSide: Int): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { Gallery.loadThumb(file, maxSide)?.asImageBitmap() }
    }
    return image
}

@Composable
fun GalleryScreen(onHome: () -> Unit) {
    var items by remember { mutableStateOf(Gallery.list()) }
    var opened by remember { mutableStateOf<Artwork?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton32(onHome) { homeIcon() }
            Text("My Gallery", style = kidText(26), modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) {
                Text(
                    "${items.size} artworks",
                    style = kidText(16, color = Color.White),
                    modifier = Modifier
                        .background(Kid.Ink, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        if (items.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ArtThumb(Showcase.art("cat"), Showcase.fills("cat"), Modifier.size(150.dp).rotate(-6f))
                Spacer(Modifier.height(12.dp))
                Text("No artworks yet!", style = kidText(26))
                Text("Draw or color something and tap Save.", style = kidText(17, color = Kid.Muted))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(180.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                itemsIndexed(items, key = { _, a -> a.file.path }) { i, artwork ->
                    Polaroid(artwork, i, dateFormat.format(Date(artwork.createdAt))) { opened = artwork }
                }
            }
        }
        BannerAd()
    }

    opened?.let { artwork ->
        Viewer(
            artwork,
            onClose = { opened = null },
            onDelete = {
                Gallery.delete(artwork)
                items = Gallery.list()
                opened = null
            },
        )
    }
}

@Composable
private fun Polaroid(artwork: Artwork, index: Int, date: String, onClick: () -> Unit) {
    val image = rememberImage(artwork.file, 420)
    Box(
        Modifier
            .rotate(tilts[index % tilts.size])
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Column(
            Modifier
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(mats[index % mats.size], RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                image?.let {
                    Image(it, contentDescription = artwork.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(artwork.title, style = kidText(17), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(date, style = kidText(13, color = Kid.Muted))
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .rotate(if (index % 2 == 0) -4f else 4f)
                .size(72.dp, 24.dp)
                .background(tapes[index % tapes.size].copy(alpha = 0.85f)),
        )
    }
}

@Composable
private fun Viewer(artwork: Artwork, onClose: () -> Unit, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val image = rememberImage(artwork.file, 1400)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                image?.let {
                    Image(it, contentDescription = artwork.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton32(onClose, size = 64.dp) { closeIcon() }
                IconButton32({ confirm = true }, size = 64.dp) { trashIcon() }
            }
        }
    }
    if (confirm) {
        ConfirmDialog("Delete this artwork?", "Delete", onYes = { confirm = false; onDelete() }, onDismiss = { confirm = false })
    }
}
