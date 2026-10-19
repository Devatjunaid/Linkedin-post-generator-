package com.devatjunaid.isthisai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.devatjunaid.isthisai.ads.NativeAdCard
import com.devatjunaid.isthisai.ads.NativeStyle
import com.devatjunaid.isthisai.engine.Evidence
import com.devatjunaid.isthisai.engine.SignalKind
import com.devatjunaid.isthisai.engine.Strength
import com.devatjunaid.isthisai.ui.OpenScan
import com.devatjunaid.isthisai.ui.components.AppIcons
import com.devatjunaid.isthisai.ui.components.IconSquareButton
import com.devatjunaid.isthisai.ui.components.MonoText
import com.devatjunaid.isthisai.ui.components.Pill
import com.devatjunaid.isthisai.ui.components.PrimaryButton
import com.devatjunaid.isthisai.ui.components.SecondaryButton
import com.devatjunaid.isthisai.ui.components.VerdictStamp
import com.devatjunaid.isthisai.ui.evidenceTitle
import com.devatjunaid.isthisai.ui.theme.AiAmber
import com.devatjunaid.isthisai.ui.theme.AiOrange
import com.devatjunaid.isthisai.ui.theme.CameraBlue
import com.devatjunaid.isthisai.ui.theme.Danger
import com.devatjunaid.isthisai.ui.theme.Dim
import com.devatjunaid.isthisai.ui.theme.LineStrong
import com.devatjunaid.isthisai.ui.theme.Lavender
import com.devatjunaid.isthisai.ui.theme.Line
import com.devatjunaid.isthisai.ui.theme.Muted
import com.devatjunaid.isthisai.ui.theme.Paper
import com.devatjunaid.isthisai.ui.theme.Surface
import com.devatjunaid.isthisai.ui.theme.SurfaceHigh
import com.devatjunaid.isthisai.ui.theme.verdictColor
import com.devatjunaid.isthisai.ui.verdictCopy

fun kindColor(k: SignalKind): Color = when (k) {
    SignalKind.AI_GENERATED -> AiOrange
    SignalKind.AI_EDITED -> AiAmber
    SignalKind.CAMERA -> CameraBlue
    SignalKind.DIGITAL -> Lavender
    SignalKind.EDIT, SignalKind.INFO -> Dim
}

fun Modifier.dashedBorder(color: Color, radius: Float = 16f): Modifier = drawBehind {
    val r = radius * density
    drawRoundRect(
        color, cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
    )
}

@Composable
fun ResultScreen(
    scan: OpenScan?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onTrail: () -> Unit,
    onScanAnother: () -> Unit,
) {
    if (scan == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MonoText("LOADING…") }
    } else {
        ResultContent(scan, onBack, onShare, onTrail, onScanAnother)
    }
}

@Composable
private fun ResultContent(
    scan: OpenScan,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onTrail: () -> Unit,
    onScanAnother: () -> Unit,
) {
    val report = scan.report
    val copy = remember(report) { verdictCopy(report) }
    val color = verdictColor(report.verdict)

    val drop = remember(scan.id) { Animatable(0f) }
    val cards = remember(scan.id) { Animatable(0f) }
    LaunchedEffect(scan.id) {
        drop.animateTo(1f, tween(950, easing = LinearEasing))
    }
    LaunchedEffect(scan.id) {
        kotlinx.coroutines.delay(600)
        cards.animateTo(1f, tween(900))
    }
    fun Modifier.rise(i: Int) = graphicsLayer {
        val k = ((cards.value * 6f - i) / 2f).coerceIn(0f, 1f)
        alpha = k
        translationY = (1f - k) * 48f
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSquareButton(AppIcons.Back, "Back", onBack)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .border(1.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            ) {
                scan.preview?.let {
                    Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(report.fileName, style = MaterialTheme.typography.titleMedium, color = Paper, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MonoText("${report.format.label} · ${com.devatjunaid.isthisai.engine.Analyzer.formatSize(report.sizeBytes)}", size = 11)
            }
            IconSquareButton(AppIcons.Share, "Share report", onShare)
        }

        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            VerdictStamp(copy.word, copy.sub, copy.ring, color, drop.value, Modifier.size(220.dp))
        }

        Column(Modifier.fillMaxWidth().rise(0), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(copy.headline, style = MaterialTheme.typography.headlineMedium, color = Paper, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(copy.detail, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(copy.chip, color)
                report.aiTool?.let { Pill(it.take(28), Muted) }
            }
        }

        if (report.warnings.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .rise(1)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Danger.copy(alpha = 0.08f))
                    .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                report.warnings.forEach { w ->
                    Row {
                        Icon(AppIcons.Warning, null, tint = Danger, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(w, style = MaterialTheme.typography.bodyMedium, color = Paper)
                    }
                }
            }
        }

        val shown = report.evidence.filter { it.kind != SignalKind.INFO || it.field == "signature" }.take(5)
        if (shown.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEachIndexed { i, e -> EvidenceCard(e, onTrail, Modifier.rise(2 + i.coerceAtMost(3))) }
            }
        }

        report.aiPrompt?.let { prompt ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .rise(4)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                MonoText("PROMPT STORED IN THE FILE", color = AiOrange, size = 10, bold = true)
                Spacer(Modifier.height(6.dp))
                Text("“$prompt”", style = MaterialTheme.typography.bodyMedium, color = Paper, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .rise(5)
                .dashedBorder(LineStrong)
                .padding(14.dp)
        ) {
            Icon(AppIcons.Info, null, tint = Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    append("Labels can be lost when an image is screenshotted, sent through chat apps or re-saved. ")
                    withStyle(SpanStyle(color = Paper, fontWeight = FontWeight.SemiBold)) {
                        append(if (report.strength == Strength.SIGNED) "A signed label shows who signed it, not that the signer is trustworthy." else "No label does not prove an image is real.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium, color = Muted,
            )
        }

        NativeAdCard(NativeStyle.MEDIUM, Modifier.rise(5))

        Row(Modifier.fillMaxWidth().rise(5), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("See full trail", null, onTrail, Modifier.weight(1f))
            SecondaryButton("Scan another", null, onScanAnother, Modifier.weight(1f), height = 58.dp)
        }
    }
}

@Composable
private fun EvidenceCard(e: Evidence, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(6.dp).height(36.dp).clip(RoundedCornerShape(3.dp)).background(kindColor(e.kind)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(evidenceTitle(e), style = MaterialTheme.typography.titleMedium, color = Paper, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MonoText(e.value, size = 11, modifier = Modifier)
        }
        Spacer(Modifier.width(8.dp))
        Icon(AppIcons.Chevron, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}
