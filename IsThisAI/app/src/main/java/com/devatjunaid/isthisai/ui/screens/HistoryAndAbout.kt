package com.devatjunaid.isthisai.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devatjunaid.isthisai.BuildConfig
import com.devatjunaid.isthisai.ads.NativeAdCard
import com.devatjunaid.isthisai.ads.NativeStyle
import com.devatjunaid.isthisai.data.ScanEntity
import com.devatjunaid.isthisai.engine.Verdict
import com.devatjunaid.isthisai.ui.components.AppIcons
import com.devatjunaid.isthisai.ui.components.IconSquareButton
import com.devatjunaid.isthisai.ui.components.MonoText
import com.devatjunaid.isthisai.ui.components.PrimaryButton
import com.devatjunaid.isthisai.ui.components.SecondaryButton
import com.devatjunaid.isthisai.ui.logo.LensMark
import com.devatjunaid.isthisai.ui.theme.AiOrange
import com.devatjunaid.isthisai.ui.theme.CameraBlue
import com.devatjunaid.isthisai.ui.theme.Danger
import com.devatjunaid.isthisai.ui.theme.Ink
import com.devatjunaid.isthisai.ui.theme.Lime
import com.devatjunaid.isthisai.ui.theme.Line
import com.devatjunaid.isthisai.ui.theme.Muted
import com.devatjunaid.isthisai.ui.theme.Paper
import com.devatjunaid.isthisai.ui.theme.Surface
import com.devatjunaid.isthisai.ui.theme.SurfaceHigh
import com.devatjunaid.isthisai.ui.theme.verdictColor
import com.devatjunaid.isthisai.ui.verdictTag
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    scans: List<ScanEntity>,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    onScan: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconSquareButton(AppIcons.Back, "Back", onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("History", style = MaterialTheme.typography.headlineMedium, color = Paper)
                MonoText("${scans.size} CHECKS · STORED ONLY ON THIS PHONE", size = 10)
            }
            if (scans.isNotEmpty()) IconSquareButton(AppIcons.Trash, "Clear history", { confirmClear = true }, tint = Danger)
        }
        if (scans.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LensMark(size = 120.dp)
                Spacer(Modifier.height(20.dp))
                Text("No checks yet", style = MaterialTheme.typography.titleLarge, color = Paper)
                Spacer(Modifier.height(6.dp))
                Text("Images you check appear here, with their full report.", style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Check an image", AppIcons.Image, onScan, Modifier.fillMaxWidth())
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(scans, key = { _, s -> s.id }) { index, scan ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HistoryRow(scan, { onOpen(scan.id) }, { onDelete(scan.id) })
                        if (index == 2 || (index > 2 && (index - 2) % 8 == 0)) NativeAdCard(NativeStyle.SMALL)
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Surface,
            title = { Text("Clear all history?", color = Paper) },
            text = { Text("This deletes every saved check and its preview from this phone.", color = Muted) },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear", color = Danger) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Paper) } },
        )
    }
}

@Composable
private fun HistoryRow(scan: ScanEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    val verdict = runCatching { Verdict.valueOf(scan.verdict) }.getOrDefault(Verdict.NO_LABEL)
    val color = verdictColor(verdict)
    val preview = rememberPreview(scan.previewPath)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceHigh)
                .border(1.5.dp, color.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        ) {
            if (preview != null) Image(preview.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(scan.fileName, style = MaterialTheme.typography.titleMedium, color = Paper, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(color))
                Spacer(Modifier.width(6.dp))
                MonoText(verdictTag(verdict).uppercase() + (scan.aiTool?.let { " · $it" } ?: ""), color = color, size = 10)
            }
            MonoText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(scan.scannedAt)), size = 10)
        }
        IconSquareButton(AppIcons.Trash, "Delete ${scan.fileName}", onDelete, tint = Muted)
    }
}

// ------------------------------------------------------------------- About

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSquareButton(AppIcons.Back, "Back", onBack)
            Spacer(Modifier.width(12.dp))
            Text("How it works", style = MaterialTheme.typography.headlineMedium, color = Paper)
        }

        InfoCard(AppIcons.Eye, Lime, "It reads, it doesn't guess",
            "Many AI tools and newer cameras write a label inside the image file. This app reads those labels on your phone. It does not use AI to guess, and it never uploads your images.")

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MonoText("WHAT IT LOOKS FOR", color = Muted, size = 11, bold = true)
            LabelLine("Content Credentials (C2PA)", "Signed history from tools such as Adobe, OpenAI, Google, Microsoft and newer phone cameras. The app checks the signature and whether the image changed after signing.")
            LabelLine("IPTC digital source type", "A standard field that can say \"trained algorithmic media\" (AI-generated) or \"digital capture\" (camera).")
            LabelLine("XMP and EXIF", "Creator tool, software, camera make and model, and edit history.")
            LabelLine("Generator settings", "Prompts and settings that Stable Diffusion, ComfyUI, NovelAI and similar tools store in PNG files.")
        }

        InfoCard(AppIcons.Warning, AiOrange, "What it can't tell you",
            "Screenshots, chat apps and many websites strip these labels, so \"No label\" never proves an image is real. Invisible watermarks such as SynthID can only be checked by the company that made them. A valid signature shows who signed, not that they are trustworthy. Tip: if a photo from your gallery shows no label, try Files to open the original file.")

        NativeAdCard(NativeStyle.MEDIUM)

        InfoCard(AppIcons.Shield, CameraBlue, "Your privacy",
            "Images are read only on this phone and are never uploaded. Your history (file name, result and a small preview) stays on this phone and is excluded from cloud backup. You can delete it any time.\n\nThe app shows ads from Google AdMob, which may use your device's advertising ID and approximate location to show and measure ads, depending on your consent choices.")

        if (privacyOptionsRequired) {
            SecondaryButton("Ad privacy choices", AppIcons.Settings, onPrivacyOptions, Modifier.fillMaxWidth())
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ink).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MonoText("PRIVACY POLICY", color = Muted, size = 11, bold = true)
            Text(PRIVACY_POLICY, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LensMark(size = 28.dp)
            Spacer(Modifier.width(8.dp))
            MonoText("IS THIS AI? · VERSION ${BuildConfig.VERSION_NAME}", size = 10)
        }
        MonoText(
            "Fonts: Bricolage Grotesque and JetBrains Mono, SIL Open Font License 1.1.",
            size = 10, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoCard(icon: ImageVector, color: androidx.compose.ui.graphics.Color, title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Paper)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun LabelLine(title: String, body: String) {
    Row {
        Box(Modifier.padding(top = 6.dp).size(6.dp).clip(RoundedCornerShape(3.dp)).background(Lime))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Paper)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

const val PRIVACY_POLICY = """Is This AI? checks images entirely on your device.

• Images: read only on your phone to find labels. They are never uploaded, sold or shared.
• History: the file name, result, report and a small preview of each image you check are saved on your phone only, and excluded from backups. Delete them in History at any time, or by uninstalling the app.
• Ads: the app uses Google AdMob. Google may collect your device's advertising ID, IP address and app usage to show and measure ads, as described at policies.google.com/technologies/ads. In regions where consent is required, you choose this in the consent form, and you can change it under "Ad privacy choices". You can also reset or delete your advertising ID in your phone's settings.
• Children: the app is not directed at children under 13.
• Contact: [YOUR SUPPORT EMAIL]"""
