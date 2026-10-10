package com.dotdash.kids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotdash.kids.game.GOLDEN_ZIP_STARS
import com.dotdash.kids.game.Level
import com.dotdash.kids.game.Levels
import com.dotdash.kids.game.Pictures

@Composable
fun StickersScreen(state: AppState, world: Int) {
    val game = state.game
    val p = game.progress
    var tab by remember { mutableIntStateOf(world) }
    LaunchedEffect(Unit) { game.audio.music(world) }

    Box(Modifier.fillMaxSize()) {
        Sky(14, seed = 31)
        Column(Modifier.fillMaxSize().padding(bottom = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton({ game.audio.tap(); state.go(Screen.Home) }, "Back home") { Icon24(Ic.Back, 24.dp, C.White, stroke = 3f) }
                BasicText("Sticker Book", Modifier.weight(1f), style = display(26.sp).copy(textAlign = TextAlign.Center))
                Pill { BasicText("${p.completedCount()}/60", style = display(18.sp)) }
            }
            // world tabs
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(C.Night2).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Pictures.worlds.forEachIndexed { i, w ->
                    val sel = i == tab
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(50)).background(if (sel) C.Sun else C.Night2)
                            .clickable(role = Role.Tab) { tab = i; game.audio.tap() },
                        contentAlignment = Alignment.Center,
                    ) { BasicText(w.short, style = body(15.sp, if (sel) C.Ink else C.Lilac, FontWeight.Black)) }
                }
            }
            // the book page
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).weight(1f).fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)).background(C.Paper)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Levels.inWorld(tab).chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { lv -> Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { StickerSlot(state, lv) } }
                    }
                }
            }
            // Golden Zip goal
            val total = p.totalStars()
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Night2)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ZipView(54.dp, gold = true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    BasicText(if (p.goldenZip()) "Golden Zip is yours!" else "Unlock Golden Zip", style = display(17.sp))
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(C.Deep)) {
                        Box(Modifier.fillMaxWidth((total / GOLDEN_ZIP_STARS.toFloat()).coerceAtMost(1f)).height(12.dp).clip(RoundedCornerShape(50)).background(C.Sun))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText("${total.coerceAtMost(GOLDEN_ZIP_STARS)}/$GOLDEN_ZIP_STARS ", style = body(13.sp, C.Muted, FontWeight.Black))
                        StarIcon(14.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerSlot(state: AppState, lv: Level) {
    val p = state.game.progress
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (p.isDone(lv.index)) {
            val tilt = ((lv.index * 37) % 15) - 7f
            Box(
                Modifier.size(74.dp).graphicsLayer { rotationZ = tilt }.clip(CircleShape).background(C.White)
                    .clickable(role = Role.Button) { state.game.audio.pop(); state.open(Overlay.Sticker(lv.picture)) }
                    .semantics { contentDescription = "${lv.picture.name} sticker" },
                contentAlignment = Alignment.Center,
            ) { PictureIcon(lv.picture, 62.dp, hex(p.colorOf(lv.picture))) }
            BasicText(lv.picture.name, style = display(13.sp, C.Ink, FontWeight.SemiBold).copy(textAlign = TextAlign.Center))
        } else {
            Box(Modifier.size(74.dp).border(4.dp, hex("#D9C49A"), CircleShape), contentAlignment = Alignment.Center) {
                BasicText("?", style = display(34.sp, hex("#C9AE7C")))
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(26.dp).clip(CircleShape).background(C.Ink),
                    contentAlignment = Alignment.Center,
                ) { Icon24(Ic.Lock, 14.dp, C.White) }
            }
            BasicText("Level ${lv.number}", style = body(12.sp, hex("#7A6A4A"), FontWeight.Black))
        }
    }
}
