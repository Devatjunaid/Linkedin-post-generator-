package com.unpin.photoscrub.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unpin.photoscrub.AppViewModel
import com.unpin.photoscrub.Screen
import com.unpin.photoscrub.ads.NativeAdCard
import com.unpin.photoscrub.data.ScanStats
import com.unpin.photoscrub.ui.UnpinActions
import com.unpin.photoscrub.ui.components.CardBox
import com.unpin.photoscrub.ui.components.ExposureMeter
import com.unpin.photoscrub.ui.components.LiveLogoMark
import com.unpin.photoscrub.ui.components.NavItem
import com.unpin.photoscrub.ui.components.PhotoThumb
import com.unpin.photoscrub.ui.components.PrimaryButton
import com.unpin.photoscrub.ui.components.RoundIconButton
import com.unpin.photoscrub.ui.components.SectionLabel
import com.unpin.photoscrub.ui.components.Tag
import com.unpin.photoscrub.ui.components.ToggleRow
import com.unpin.photoscrub.ui.components.UnpinIcons
import com.unpin.photoscrub.ui.theme.Card
import com.unpin.photoscrub.ui.theme.Clean
import com.unpin.photoscrub.ui.theme.Display
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.InkSoft
import com.unpin.photoscrub.ui.theme.Line
import com.unpin.photoscrub.ui.theme.Muted
import com.unpin.photoscrub.ui.theme.MutedOnInk
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.SignalSoft
import com.unpin.photoscrub.ui.theme.UnpinText
import androidx.compose.ui.text.font.FontWeight

@Composable
fun HomeScreen(vm: AppViewModel, actions: UnpinActions) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveLogoMark(Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Text("unpin", style = UnpinText.title.copy(fontSize = 25.sp, fontWeight = FontWeight.ExtraBold), color = Ink)
                Spacer(Modifier.weight(1f))
                RoundIconButton(UnpinIcons.Settings, "Settings", { vm.open(Screen.SETTINGS) })
            }

            ExposureCard(vm.scan, onScan = actions.pickForBatch)

            Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Pick photos to clean", actions.pickForBatch, Modifier.weight(1f), icon = UnpinIcons.Photos)
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Card)
                        .clickable(role = Role.Button, onClickLabel = "Inspect one photo", onClick = actions.pickForInspect),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(UnpinIcons.Search, "Inspect one photo", tint = Ink, modifier = Modifier.size(22.dp))
                }
            }

            CardBox(Modifier.padding(horizontal = 16.dp)) {
                Column {
                    SectionLabel("Auto-protect", Modifier.padding(top = 14.dp, bottom = 2.dp))
                    ToggleRow(
                        "Clean when sharing",
                        "Share photos to Unpin from any app — they're wiped and handed straight back to the share sheet",
                        vm.quickShare, vm::updateQuickShare, showDivider = false,
                    )
                    ToggleRow(
                        "Anonymise file names",
                        "IMG_20261012_0742.jpg → unpin_3f9a1c2e.jpg",
                        vm.anonymizeDefault, vm::updateAnonymizeDefault, showDivider = true,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recently scrubbed", style = UnpinText.section, color = Ink, modifier = Modifier.weight(1f))
                Text(
                    "History", style = UnpinText.bodyStrong.copy(fontSize = 13.sp), color = Ink,
                    modifier = Modifier.clickable(role = Role.Button) { vm.open(Screen.HISTORY) }.padding(8.dp),
                )
            }
            val recent = vm.recentOutputs
            if (recent.isEmpty()) {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Card)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(UnpinIcons.Shield, null, tint = Clean, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Clean copies you make appear here. Originals are never changed.", style = UnpinText.small, color = Muted)
                }
            } else {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    recent.forEach { uri ->
                        Box(Modifier.size(80.dp).clickable(role = Role.Image, onClickLabel = "Open clean copy") { actions.view(uri) }) {
                            PhotoThumb(uri, Modifier.fillMaxSize())
                            Tag("CLEAN", Clean, Card, Modifier.align(Alignment.BottomStart).padding(6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            NativeAdCard(Modifier.padding(horizontal = 16.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Card)
                .padding(horizontal = 8.dp),
        ) {
            NavItem("Home", UnpinIcons.Home, true) { vm.open(Screen.HOME) }
            NavItem("Inspect", UnpinIcons.Search, false) {
                if (vm.inspected != null) vm.open(Screen.INSPECT) else actions.pickForInspect()
            }
            NavItem("Batch", UnpinIcons.Grid, false) { vm.open(Screen.BATCH) }
            NavItem("History", UnpinIcons.Clock, false) { vm.open(Screen.HISTORY) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    }
}

@Composable
private fun ExposureCard(scan: ScanStats?, onScan: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Ink)
    ) {
        Box {
            Canvas(Modifier.size(170.dp).offset(x = 230.dp, y = (-36).dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                listOf(0.47f, 0.33f, 0.19f).forEach { r ->
                    drawCircle(Paper, size.width * r, c, alpha = 0.16f, style = Stroke(1.dp.toPx()))
                }
                drawLine(Paper.copy(alpha = 0.16f), Offset(0f, c.y), Offset(size.width, c.y), 1.dp.toPx())
                drawLine(Paper.copy(alpha = 0.16f), Offset(c.x, 0f), Offset(c.x, size.height), 1.dp.toPx())
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp)) {
                SectionLabel("Exposure scan", color = SignalSoft)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (scan == null || scan.withGps > 0) "Your photos may\nstill be talking." else "Your last picks\nwere quiet.",
                    style = UnpinText.hero, color = Paper,
                )
                Spacer(Modifier.height(16.dp))
                if (scan == null || scan.total == 0) {
                    Text(
                        "Most phone photos carry the exact spot they were taken, your phone model and the minute you pressed the shutter. Pick some to see.",
                        style = UnpinText.body, color = MutedOnInk,
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Check my photos", onScan, Modifier.fillMaxWidth(), icon = UnpinIcons.Search)
                } else {
                    val pct = scan.withGps * 100 / scan.total
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$pct%", style = UnpinText.hero.copy(fontSize = 44.sp, fontFamily = Display, fontWeight = FontWeight.ExtraBold), color = Signal)
                        Spacer(Modifier.weight(1f))
                        Text("${scan.withGps} of ${scan.total} carry GPS", style = UnpinText.small, color = MutedOnInk, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    ExposureMeter(((scan.withGps * 20f) / scan.total).toInt().coerceIn(if (scan.withGps > 0) 1 else 0, 20))
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStat("${scan.withGps}", "exact locations", Modifier.weight(1f))
                        MiniStat("${scan.withDevice}", "device model", Modifier.weight(1f))
                        MiniStat("${scan.withDate}", "timestamps", Modifier.weight(1f))
                    }
                    Text(
                        "From the last ${scan.total} photo${if (scan.total == 1) "" else "s"} you picked",
                        style = UnpinText.small.copy(fontSize = 11.sp), color = MutedOnInk,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(InkSoft).padding(10.dp)) {
        Text(value, style = UnpinText.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = Paper)
        Text(label, style = UnpinText.small.copy(fontSize = 11.sp), color = MutedOnInk)
    }
}
