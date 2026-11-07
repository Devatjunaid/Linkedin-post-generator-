package com.superdevlabs.autosubtitles.ui.languages

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import android.app.ActivityManager
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.data.AppSettings
import com.superdevlabs.autosubtitles.speech.ModelCatalog
import com.superdevlabs.autosubtitles.speech.ModelState
import com.superdevlabs.autosubtitles.speech.SpeechEngine
import com.superdevlabs.autosubtitles.speech.SpokenLanguage
import com.superdevlabs.autosubtitles.speech.WhisperCatalog
import com.superdevlabs.autosubtitles.ui.components.SectionLabel
import com.superdevlabs.autosubtitles.ui.components.Segmented
import com.superdevlabs.autosubtitles.ui.components.pressable
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguagesViewModel(private val container: AppContainer) : ViewModel() {
    val states: StateFlow<Map<String, ModelState>> = container.models.states
    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setEngine(engine: SpeechEngine) {
        viewModelScope.launch { container.settings.setEngine(engine) }
        container.telemetry.event("engine_selected", "engine" to engine.id)
    }

    fun select(id: String) {
        viewModelScope.launch { container.settings.setLanguage(id) }
        if (!container.models.isInstalled(id)) container.models.startInstall(id)
    }

    fun selectWhisperModel(id: String) {
        viewModelScope.launch { container.settings.setWhisperModel(id) }
        if (!container.models.isInstalled(id)) container.models.startInstall(id)
    }

    fun selectWhisperLanguage(code: String) {
        viewModelScope.launch { container.settings.setWhisperLanguage(code) }
    }

    fun download(id: String) = container.models.startInstall(id)

    fun delete(id: String) {
        viewModelScope.launch {
            if (WhisperCatalog.isWhisper(id)) container.whisper.release() else container.transcriber.release()
            container.models.delete(id)
        }
    }
}

@Composable
fun LanguagesScreen(container: AppContainer, contentPadding: PaddingValues) {
    val vm: LanguagesViewModel = viewModel(factory = viewModelFactory { initializer { LanguagesViewModel(container) } })
    val states by vm.states.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ramGb = remember {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        // totalMem is slightly below the marketing size (e.g. 7.4 GB on an "8 GB" phone).
        (info.totalMem / 1_000_000_000.0 + 0.6).toInt()
    }
    val accurate = settings.engine == SpeechEngine.ACCURATE

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Speech engine", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Everything runs offline on your phone — free, private, no limits. Models download once.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Muted,
                )
            }
        }
        item {
            Segmented(
                options = SpeechEngine.entries,
                selected = settings.engine,
                label = { if (it == SpeechEngine.ACCURATE) "✨ Accurate" else "⚡ Fast" },
                onSelect = vm::setEngine,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                if (accurate) {
                    "Whisper AI: punctuation, capital letters, names and accents. 50+ languages, auto-detect. " +
                        "Takes longer on long videos."
                } else {
                    "Vosk: very fast with exact word timing. No punctuation. Best for quick clips in one language."
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Soft,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (accurate) {
            item { SectionLabel("Model", Modifier.padding(top = 8.dp)) }
            items(WhisperCatalog.models, key = { it.id }) { model ->
                ModelRow(
                    title = model.name,
                    detail = if (ramGb in 1 until model.minRamGb) "${model.detail} · may be slow on this phone" else model.detail,
                    sizeMb = model.sizeMb,
                    state = states[model.id] ?: ModelState.NotInstalled,
                    selected = settings.whisperModelId == model.id,
                    onSelect = { vm.selectWhisperModel(model.id) },
                    onDownload = { vm.download(model.id) },
                    onDelete = { vm.delete(model.id) },
                )
            }
            item { SectionLabel("Spoken language", Modifier.padding(top = 8.dp)) }
            items(WhisperCatalog.languages, key = { "w-" + it.code }) { language ->
                LanguageRow(
                    language = language,
                    selected = settings.whisperLanguage == language.code,
                    onSelect = { vm.selectWhisperLanguage(language.code) },
                )
            }
        } else {
            item { SectionLabel("Spoken language", Modifier.padding(top = 8.dp)) }
            items(ModelCatalog.models, key = { it.id }) { model ->
                ModelRow(
                    title = model.language,
                    detail = model.detail,
                    sizeMb = model.sizeMb,
                    state = states[model.id] ?: ModelState.NotInstalled,
                    selected = settings.languageId == model.id,
                    onSelect = { vm.select(model.id) },
                    onDownload = { vm.download(model.id) },
                    onDelete = { vm.delete(model.id) },
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(language: SpokenLanguage, selected: Boolean, onSelect: () -> Unit) {
    val border by animateColorAsState(if (selected) AppColors.Lime else AppColors.Line, label = "border")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .pressable(onClick = onSelect, role = Role.RadioButton, pressedScale = 0.97f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(if (selected) AppColors.Lime else AppColors.SurfaceHigh))
        Spacer(Modifier.width(14.dp))
        Text(language.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(language.english, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
    }
}

@Composable
private fun ModelRow(
    title: String,
    detail: String,
    sizeMb: Int,
    state: ModelState,
    selected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val border by animateColorAsState(if (selected) AppColors.Lime else AppColors.Line, label = "border")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .border(1.5.dp, border, RoundedCornerShape(20.dp))
            .pressable(onClick = onSelect, role = Role.RadioButton, pressedScale = 0.97f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (selected) AppColors.Lime else AppColors.SurfaceHigh),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            val status = when (state) {
                ModelState.Installed -> "Ready offline"
                is ModelState.Downloading -> "Downloading ${(state.progress * 100).toInt()}%"
                ModelState.Unpacking -> "Preparing…"
                is ModelState.Failed -> "Failed: ${state.message}"
                ModelState.NotInstalled -> "$sizeMb MB download"
            }
            SectionLabel("$detail · $status")
            if (state is ModelState.Downloading) {
                val progress by animateFloatAsState(state.progress, label = "dl")
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(AppColors.SurfaceHigh)) {
                    Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(AppColors.Lime))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        when (state) {
            ModelState.Installed -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Installed", tint = AppColors.Lime)
                Box(Modifier.size(44.dp).pressable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete $title model", tint = AppColors.Muted)
                }
            }
            is ModelState.Downloading, ModelState.Unpacking ->
                CircularProgressIndicator(Modifier.size(24.dp), color = AppColors.Lime, strokeWidth = 3.dp)
            is ModelState.Failed -> Box(Modifier.size(44.dp).pressable(onClick = onDownload), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Retry download", tint = AppColors.Coral)
            }
            ModelState.NotInstalled -> Box(Modifier.size(44.dp).pressable(onClick = onDownload), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Download, contentDescription = "Download $title", tint = AppColors.Text)
            }
        }
    }
}
