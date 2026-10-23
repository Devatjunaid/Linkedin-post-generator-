package com.driveready.testprep.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driveready.testprep.R
import com.driveready.testprep.data.model.SignStyle
import com.driveready.testprep.data.model.SignType
import com.driveready.testprep.ui.components.AmberButton
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.BrandLogo
import com.driveready.testprep.ui.components.PrimaryButton
import com.driveready.testprep.ui.components.RoadSign
import com.driveready.testprep.ui.theme.Brand
import kotlinx.coroutines.launch

private data class GuidePage(
    val title: String,
    val body: String,
    val accent: Color,
    val art: @Composable () -> Unit
)

/** Guide pages shown once after the splash: what the app does, step by step. */
@Composable
fun GuideScreen(onDone: () -> Unit) {
    val pages = listOf(
        GuidePage(
            "Your country's real test format",
            "Pick your country — and your state, province or emirate — and DriveReady matches the question count, pass mark and test stages to your licensing authority.",
            Brand.Blue
        ) { GuideArt(listOf(AppIcons.Globe, AppIcons.Doc, AppIcons.Check)) },
        GuidePage(
            "Theory, signs & hazard perception",
            "Timed mock exams, practice by topic, a sign library drawn in your country's style and animated hazard clips scored like the real thing.",
            Brand.Amber
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                RoadSign(SignType.STOP, SignStyle.VIENNA, size = 78.dp)
                RoadSign(SignType.PEDESTRIAN_CROSSING, SignStyle.VIENNA, size = 84.dp)
                RoadSign(SignType.SPEED_LIMIT, SignStyle.VIENNA, size = 78.dp, value = "50")
            }
        },
        GuidePage(
            "Oral test coaching",
            "Practise the spoken part of your test — UK 'show me, tell me', RTO viva, K53 pre-trip and more — with an examiner voice and model answers.",
            Color(0xFF7C4DFF)
        ) { GuideArt(listOf(AppIcons.Mic, AppIcons.Speaker, AppIcons.Star)) },
        GuidePage(
            "Practical test techniques",
            "Step-by-step animated guides for parallel parking, bay parking, the figure-8, hill starts and every maneuver your examiner can ask for.",
            Brand.Good
        ) { GuideArt(listOf(AppIcons.Wheel, AppIcons.Flag, AppIcons.Calendar)) }
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(Brand.Surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandLogo(size = 36.dp, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Skip", color = Brand.Muted) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Brush.linearGradient(listOf(page.accent.copy(alpha = 0.18f), page.accent.copy(alpha = 0.05f)))),
                    contentAlignment = Alignment.Center
                ) { page.art() }
                Spacer(Modifier.height(32.dp))
                Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(page.body, style = MaterialTheme.typography.bodyLarge, color = Brand.Muted, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { i ->
                val selected = i == pager.currentPage
                val width by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot")
                val color by animateColorAsState(if (selected) Brand.Ink else Color(0xFFCFCCC3), label = "dotColor")
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
        PrimaryButton(
            text = if (isLast) "Continue" else "Next",
            onClick = { if (isLast) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun GuideArt(icons: List<ImageVector>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        icons.forEachIndexed { i, icon ->
            val big = i == 1
            Box(
                Modifier
                    .size(if (big) 104.dp else 76.dp)
                    .clip(RoundedCornerShape(if (big) 30.dp else 22.dp))
                    .background(if (big) Brand.Ink else Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (big) Brand.Amber else Brand.Ink, modifier = Modifier.size(if (big) 52.dp else 36.dp))
            }
        }
    }
}

/** Final "Get started" screen before choosing a country. */
@Composable
fun GetStartedScreen(onGetStarted: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand.NavyLight, Brand.Navy)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        BrandLogo(size = 112.dp)
        Spacer(Modifier.height(28.dp))
        Text("Pass your driving test,\nfirst time.", style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(
            "One app for every part of the test: theory, road signs, hazard perception, the oral questions and the practical drive.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFC9CBD1),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Feature(AppIcons.Globe, "Country & state specific syllabus")
            Feature(AppIcons.Timer, "Real-format timed mock exams")
            Feature(AppIcons.Eye, "Hazard perception clips")
            Feature(AppIcons.Mic, "Oral test examiner practice")
        }
        Spacer(Modifier.weight(1f))
        AmberButton("Get started", onGetStarted, Modifier.fillMaxWidth(), icon = AppIcons.ChevronRight)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9EA2AA),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.titleSmall)
    }
}
