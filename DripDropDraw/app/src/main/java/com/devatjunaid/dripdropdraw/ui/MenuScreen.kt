package com.devatjunaid.dripdropdraw.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.dripdropdraw.ads.BannerAd
import com.devatjunaid.dripdropdraw.audio.Sound
import com.devatjunaid.dripdropdraw.data.Progress
import kotlinx.coroutines.delay

private enum class Gate { NONE, RATE, PARENTS }

@Composable
fun MenuScreen(
    progress: Progress,
    onPlay: () -> Unit,
    onLevels: () -> Unit,
    onExit: () -> Unit,
    onRate: () -> Unit,
) {
    val sfx = LocalSfx.current
    var exitDialog by remember { mutableStateOf(false) }
    var gate by remember { mutableStateOf(Gate.NONE) }
    var parentsInfo by remember { mutableStateOf(false) }
    var laughs by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(Mood.HAPPY) }

    LaunchedEffect(laughs) {
        if (laughs > 0) {
            mood = Mood.LAUGH
            sfx?.play(Sound.GIGGLE)
            delay(1500)
            mood = Mood.HAPPY
        }
    }

    BackHandler { exitDialog = true }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconCircleButton(IconKind.PARENT, "For parents", { gate = Gate.PARENTS }, size = 46.dp)
                Spacer(Modifier.weight(1f))
                Chip {
                    GameIcon(IconKind.COIN, Modifier.size(24.dp))
                    Text("${progress.coins}", style = textStyle(17.sp))
                }
                Chip {
                    GameIcon(IconKind.STAR, Modifier.size(24.dp))
                    Text("${progress.totalStars}", style = textStyle(17.sp))
                }
            }

            Spacer(Modifier.weight(0.5f))
            GameLogo()
            Spacer(Modifier.height(8.dp))
            Mascot(
                mood,
                Modifier
                    .size(170.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { laughs++ },
                fill = 0.65f,
            )
            Spacer(Modifier.weight(0.5f))

            CartoonButton(onPlay, Modifier.width(260.dp), color = Palette.Orange, height = 66.dp, corner = 33.dp) {
                GameIcon(IconKind.PLAY, Modifier.size(26.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Play", style = textStyle(28.sp, FontWeight.Bold))
                    Text("Level ${progress.unlocked}", style = textStyle(14.sp, FontWeight.Medium))
                }
            }
            Spacer(Modifier.height(14.dp))
            CartoonButton(onLevels, Modifier.width(260.dp)) {
                GameIcon(IconKind.GRID, Modifier.size(24.dp))
                ButtonLabel("Levels")
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconCircleButton(
                    if (progress.soundOn) IconKind.SOUND_ON else IconKind.SOUND_OFF,
                    if (progress.soundOn) "Sound off" else "Sound on",
                    {
                        progress.setSound(!progress.soundOn)
                        sfx?.soundOn = progress.soundOn
                    },
                    size = 54.dp,
                )
                IconCircleButton(
                    if (progress.musicOn) IconKind.MUSIC_ON else IconKind.MUSIC_OFF,
                    if (progress.musicOn) "Music off" else "Music on",
                    {
                        progress.setMusic(!progress.musicOn)
                        sfx?.musicOn = progress.musicOn
                    },
                    size = 54.dp,
                )
            }
            Spacer(Modifier.weight(0.4f))
            BannerAd()
        }

        if (exitDialog) {
            ExitDialog(
                onCancel = { exitDialog = false },
                onRate = { gate = Gate.RATE },
                onExit = onExit,
            )
        }
        if (parentsInfo) ParentsInfo(onClose = { parentsInfo = false })
        if (gate != Gate.NONE) {
            ParentalGate(
                onPass = {
                    when (gate) {
                        Gate.RATE -> {
                            exitDialog = false
                            onRate()
                        }
                        Gate.PARENTS -> parentsInfo = true
                        Gate.NONE -> Unit
                    }
                    gate = Gate.NONE
                },
                onCancel = { gate = Gate.NONE },
            )
        }
    }
}
