package com.superdevlabs.autosubtitles.ui.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.ads.NativeAdCard
import com.superdevlabs.autosubtitles.core.captions.StylePresets
import com.superdevlabs.autosubtitles.data.AppSettings
import com.superdevlabs.autosubtitles.data.Project
import com.superdevlabs.autosubtitles.speech.ModelCatalog
import com.superdevlabs.autosubtitles.speech.ModelState
import com.superdevlabs.autosubtitles.ui.components.ExitDialog
import com.superdevlabs.autosubtitles.ui.components.SectionLabel
import com.superdevlabs.autosubtitles.ui.components.Thumbnail
import com.superdevlabs.autosubtitles.ui.components.BrandMark
import com.superdevlabs.autosubtitles.ui.components.WaveDisc
import com.superdevlabs.autosubtitles.ui.components.pressable
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val projects: StateFlow<List<Project>> = container.projects.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val modelStates: StateFlow<Map<String, ModelState>> = container.models.states

    suspend fun createProject(uri: Uri): String {
        val current = container.settings.current()
        val style = StylePresets.byId(current.presetId).style.copy(wordsPerCaption = current.wordsPerCaption)
        return container.projects.create(uri, current.languageId, style)
    }
}

@Composable
fun HomeScreen(
    container: AppContainer,
    contentPadding: PaddingValues,
    onStartProject: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenLanguages: () -> Unit,
    onExit: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container) } })
    val projects by vm.projects.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val modelStates by vm.modelStates.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showExit by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { onStartProject(vm.createProject(uri)) }
    }
    val pickVideo = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    BackHandler { showExit = true }
    if (showExit) {
        ExitDialog(onStay = { showExit = false }, onExit = onExit)
    }

    val model = ModelCatalog.byId(settings.languageId)
    val modelState = modelStates[model.id] ?: ModelState.NotInstalled

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(34.dp)
                Spacer(Modifier.width(8.dp))
                Text("Auto Subtitles", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                LanguageChip(model.language, onOpenLanguages)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EngineStatus(modelState, model.sizeMb)
                Text(
                    buildAnnotatedString {
                        append("Pick a video.\n")
                        withStyle(SpanStyle(color = AppColors.Lime)) { append("Get subtitles.") }
                    },
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    "The app listens to the speech in your video and writes timed subtitles onto it — then you style them and export.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Muted,
                )
            }
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaveDisc(onClick = pickVideo)
            }
        }
        item { HowItWorks() }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent cuts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (projects.isNotEmpty()) SectionLabel("${projects.size} saved")
            }
        }
        item {
            if (projects.isEmpty()) {
                EmptyRecents(Modifier.padding(horizontal = 20.dp))
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(projects.take(12), key = { _, p -> p.id }) { index, project ->
                        FilmCard(project, tilt = if (index % 2 == 0) -2.5f else 2f) { onOpenProject(project.id) }
                    }
                }
            }
        }
        item { NativeAdCard(container.ads, Modifier.padding(horizontal = 20.dp)) }
    }
}

@Composable
private fun LanguageChip(language: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(20.dp))
            .pressable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(language, style = MaterialTheme.typography.titleSmall)
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Change language", tint = AppColors.Muted)
    }
}

@Composable
private fun EngineStatus(state: ModelState, sizeMb: Int) {
    val blink = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by blink.animateFloat(1f, 0.25f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    val (label, color) = when (state) {
        ModelState.Installed -> "OFFLINE ENGINE READY" to AppColors.Lime
        is ModelState.Downloading -> "DOWNLOADING MODEL ${(state.progress * 100).toInt()}%" to AppColors.Ice
        ModelState.Unpacking -> "PREPARING MODEL…" to AppColors.Ice
        is ModelState.Failed -> "MODEL DOWNLOAD FAILED · RETRY IN LANGUAGES" to AppColors.Coral
        ModelState.NotInstalled -> "ONE-TIME ${sizeMb} MB MODEL DOWNLOAD ON FIRST USE" to AppColors.Coral
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).graphicsLayer { alpha = blinkAlpha }.clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        SectionLabel(label)
    }
}

@Composable
private fun HowItWorks() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Step(Icons.Rounded.Movie, "1", "Pick a video", Modifier.weight(1f))
        Step(Icons.Rounded.AutoAwesome, "2", "Auto subtitles", Modifier.weight(1f))
        Step(Icons.Rounded.Palette, "3", "Style & export", Modifier.weight(1f))
    }
}

@Composable
private fun Step(icon: androidx.compose.ui.graphics.vector.ImageVector, number: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AppColors.Lime, modifier = Modifier.size(18.dp))
            Spacer(Modifier.weight(1f))
            Text(number, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
        }
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2)
    }
}

@Composable
private fun EmptyRecents(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, AppColors.Line, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Your subtitled videos will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Muted,
        )
    }
}

/** Project card drawn like a strip of film, slightly tilted; straightens while pressed. */
@Composable
fun FilmCard(project: Project, tilt: Float, onClick: () -> Unit) {
    Column(
        Modifier
            .width(132.dp)
            .graphicsLayer { rotationZ = tilt }
            .pressable(onClick = onClick, pressedScale = 0.95f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.SurfaceHigh)
                .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Sprockets()
            Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(9.dp))) {
                Thumbnail(project.thumbnail, Modifier.fillMaxSize(), contentDescription = project.name)
                val firstLine = project.captions.firstOrNull()?.text
                if (firstLine != null) {
                    Text(
                        firstLine.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.Ink.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Sprockets()
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(project.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatDuration(project.durationMs)} · ${ModelCatalog.byId(project.languageId).id.uppercase()} · " +
                    if (project.isReady) "done" else "draft",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Muted,
            )
        }
    }
}

@Composable
private fun Sprockets() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(6) { Box(Modifier.size(8.dp, 5.dp).clip(RoundedCornerShape(2.dp)).background(AppColors.Ink)) }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
