package com.devatjunaid.isthisai.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.isthisai.ads.NativeAdCard
import com.devatjunaid.isthisai.ads.NativeStyle
import com.devatjunaid.isthisai.data.ImageSource
import com.devatjunaid.isthisai.data.ScanEntity
import com.devatjunaid.isthisai.engine.Verdict
import com.devatjunaid.isthisai.ui.components.AppIcons
import com.devatjunaid.isthisai.ui.components.ApertureHero
import com.devatjunaid.isthisai.ui.components.IconSquareButton
import com.devatjunaid.isthisai.ui.components.MonoText
import com.devatjunaid.isthisai.ui.components.PrimaryButton
import com.devatjunaid.isthisai.ui.components.SecondaryButton
import com.devatjunaid.isthisai.ui.logo.LensMark
import com.devatjunaid.isthisai.ui.logo.Wordmark
import com.devatjunaid.isthisai.ui.theme.Lime
import com.devatjunaid.isthisai.ui.theme.Line
import com.devatjunaid.isthisai.ui.theme.Muted
import com.devatjunaid.isthisai.ui.theme.Paper
import com.devatjunaid.isthisai.ui.theme.Surface
import com.devatjunaid.isthisai.ui.theme.SurfaceHigh
import com.devatjunaid.isthisai.ui.theme.verdictColor
import com.devatjunaid.isthisai.ui.verdictTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    recent: List<ScanEntity>,
    onPickPhoto: () -> Unit,
    onPickFile: () -> Unit,
    onPaste: () -> Unit,
    onOpenScan: (Long) -> Unit,
    onHistory: () -> Unit,
    onAbout: () -> Unit,
) {
    // Staggered rise-in for the page sections.
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(900)) }
    fun Modifier.rise(i: Int) = graphicsLayer {
        val k = ((enter.value * 5f - i) / 1.6f).coerceIn(0f, 1f)
        alpha = k
        translationY = (1f - k) * 40f
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().rise(0), verticalAlignment = Alignment.CenterVertically) {
            LensMark(size = 36.dp)
            Spacer(Modifier.width(8.dp))
            Wordmark(fontSize = 21.sp)
            Spacer(Modifier.weight(1f))
            IconSquareButton(AppIcons.History, "Scan history", onHistory)
            Spacer(Modifier.width(8.dp))
            IconSquareButton(AppIcons.Info, "About and privacy", onAbout)
        }

        Box(Modifier.fillMaxWidth().rise(1), contentAlignment = Alignment.Center) {
            ApertureHero(onClick = onPickPhoto, size = 264.dp)
        }

        Column(Modifier.fillMaxWidth().rise(2), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "What does this image\nsay about itself?",
                style = MaterialTheme.typography.headlineLarge,
                color = Paper,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We read the hidden labels that AI tools and phone cameras write inside the file.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Column(Modifier.rise(3), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Choose an image", AppIcons.Image, onPickPhoto, Modifier.fillMaxWidth(), glint = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Paste", AppIcons.Paste, onPaste, Modifier.weight(1f))
                SecondaryButton("Files", AppIcons.Folder, onPickFile, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Lock, contentDescription = null, tint = Lime, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                MonoText("OFFLINE · YOUR IMAGES NEVER LEAVE THE PHONE")
            }
        }

        NativeAdCard(NativeStyle.SMALL, Modifier.rise(4))

        Column(Modifier.rise(4), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoText("RECENT", color = Muted, size = 12, bold = true)
                Spacer(Modifier.weight(1f))
                if (recent.isNotEmpty()) {
                    Text(
                        "See all",
                        style = MaterialTheme.typography.titleMedium,
                        color = Paper,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onHistory)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }
            }
            if (recent.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Line, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "Your checks appear here. Tip: share an image to this app from your gallery, browser or chat.",
                        style = MaterialTheme.typography.bodyMedium, color = Muted,
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recent, key = { it.id }) { scan -> RecentCard(scan) { onOpenScan(scan.id) } }
                }
            }
        }
    }
}


@Composable
fun rememberPreview(path: String?): Bitmap? {
    val bmp by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { ImageSource.loadPreview(path) }
    }
    return bmp
}

@Composable
private fun RecentCard(scan: ScanEntity, onClick: () -> Unit) {
    val verdict = runCatching { Verdict.valueOf(scan.verdict) }.getOrDefault(Verdict.NO_LABEL)
    val color = verdictColor(verdict)
    val preview = rememberPreview(scan.previewPath)
    Column(
        Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(76.dp).background(SurfaceHigh)) {
            if (preview != null) {
                Image(preview.asImageBitmap(), contentDescription = scan.fileName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.width(6.dp))
            MonoText(verdictTag(verdict), color = color, size = 10)
        }
    }
}
