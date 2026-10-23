package com.devatjunaid.examclock.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ui.theme.Brand
import com.devatjunaid.examclock.ui.theme.CountdownStyle
import com.devatjunaid.examclock.ui.theme.subjectColor
import kotlinx.coroutines.launch

private data class GuidePage(val title: Int, val body: Int, val art: @Composable () -> Unit)

/** Four-step "how it works" guide shown once before setup. */
@Composable
fun GuideScreen(onDone: () -> Unit) {
    val pages = listOf(
        GuidePage(R.string.guide_1_title, R.string.guide_1_body) { CountdownArt() },
        GuidePage(R.string.guide_2_title, R.string.guide_2_body) { PlannerArt() },
        GuidePage(R.string.guide_3_title, R.string.guide_3_body) { FocusArt() },
        GuidePage(R.string.guide_4_title, R.string.guide_4_body) { ProgressArt() },
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (!last) TextButton(onClick = onDone) { Text(stringResource(R.string.action_skip)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) { page.art() }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            val position = stringResource(R.string.guide_position, pager.currentPage + 1, pages.size)
            Row(
                Modifier
                    .weight(1f)
                    .semantics { contentDescription = position },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                    val color by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        label = "dotColor",
                    )
                    Box(Modifier.size(width = width, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                }
            }
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(if (last) R.string.action_get_started else R.string.action_next))
            }
        }
    }
}

@Composable
private fun CountdownArt() {
    Column(
        Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brand.Ink)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.next_exam).uppercase(), color = Brand.OnInkMuted, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("42", style = CountdownStyle, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.unit_days), color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Brand.InkLine)) {
            Box(Modifier.fillMaxWidth(0.58f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brand.BlueLight))
        }
    }
}

@Composable
private fun PlannerArt() {
    val heights = listOf(listOf(40, 26), listOf(30, 40, 20), listOf(46), listOf(26, 30), listOf(40, 24, 30), listOf(56, 30), listOf())
    Row(Modifier.width(260.dp).height(200.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        heights.forEachIndexed { day, blocks ->
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                blocks.forEachIndexed { i, h ->
                    Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(6.dp)).background(subjectColor(day + i)))
                }
            }
        }
    }
}

@Composable
private fun FocusArt() {
    val ring = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            drawArc(track, 0f, 360f, false, style = Stroke(stroke))
            drawArc(ring, -90f, 250f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("18:42", style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
private fun ProgressArt() {
    val values = listOf(0.5f, 0.6f, 0.15f, 0.6f, 0.35f, 0.9f, 0.7f)
    Row(Modifier.width(240.dp).height(180.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(v)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(if (i == 5) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
            )
        }
    }
}
