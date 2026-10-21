package com.devatjunaid.dripdropdraw.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.dripdropdraw.ads.BannerAd
import com.devatjunaid.dripdropdraw.audio.Sound
import com.devatjunaid.dripdropdraw.data.Progress
import com.devatjunaid.dripdropdraw.engine.Levels

private const val PER_WORLD = 20
private const val COLUMNS = 5

/**
 * The level map. Only worlds the player has reached are shown, so the full size of the game
 * stays a surprise.
 */
@Composable
fun LevelsScreen(progress: Progress, onBack: () -> Unit, onPick: (Int) -> Unit) {
    BackHandler(onBack = onBack)
    val currentWorld = (progress.unlocked - 1) / PER_WORLD
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentWorld)
    val sfx = LocalSfx.current

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconCircleButton(IconKind.HOME, "Back to menu", onBack, size = 46.dp)
            Text("Levels", style = textStyle(30.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
            Chip {
                GameIcon(IconKind.STAR, Modifier.size(24.dp))
                Text("${progress.totalStars}", style = textStyle(17.sp))
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items((0..currentWorld).toList()) { w ->
                WorldSection(w, progress) { level ->
                    sfx?.play(Sound.POP)
                    onPick(level)
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        BannerAd()
    }
}

@Composable
private fun WorldSection(world: Int, progress: Progress, onPick: (Int) -> Unit) {
    val theme = Worlds[world.coerceIn(0, Worlds.size - 1)]
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(theme.paper, RoundedCornerShape(18.dp))
                .border(2.5.dp, Palette.Ink, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                theme.name,
                style = textStyle(22.sp, FontWeight.Bold, if (theme.dark) Palette.White else Palette.Ink),
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0..world) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(Palette.Orange, RoundedCornerShape(6.dp))
                            .border(2.dp, Palette.Ink, RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
        val first = world * PER_WORLD + 1
        val last = minOf(first + PER_WORLD - 1, Levels.COUNT)
        for (row in (first..last).chunked(COLUMNS)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (level in row) {
                    LevelTile(
                        level = level,
                        unlocked = level <= progress.unlocked,
                        current = level == progress.unlocked,
                        stars = progress.starsFor(level),
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(level) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelTile(level: Int, unlocked: Boolean, current: Boolean, stars: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = when {
        current -> Palette.Orange
        unlocked -> Palette.WaterDark
        else -> Palette.Locked
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(color, RoundedCornerShape(16.dp))
                .border(3.dp, if (unlocked) Palette.Ink else Palette.LockedLine, RoundedCornerShape(16.dp))
                .semantics { contentDescription = if (unlocked) "Level $level" else "Level $level, locked" }
                .then(if (unlocked) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) {
                Text(
                    "$level",
                    style = textStyle(22.sp, FontWeight.Bold, if (current) Palette.Ink else Palette.White),
                )
            } else {
                GameIcon(IconKind.LOCK, Modifier.size(24.dp), color = Palette.LockedLine)
            }
        }
        Row(Modifier.padding(top = 3.dp)) {
            for (i in 0 until 3) {
                GameIcon(
                    IconKind.STAR,
                    Modifier.size(14.dp),
                    color = if (unlocked) Palette.Ink else Palette.LockedLine,
                    fill = if (i < stars) Palette.Star else Palette.StarOff,
                )
            }
        }
    }
}
