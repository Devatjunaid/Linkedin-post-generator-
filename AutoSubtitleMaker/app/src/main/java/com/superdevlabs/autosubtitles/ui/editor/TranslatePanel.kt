package com.superdevlabs.autosubtitles.ui.editor

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.superdevlabs.autosubtitles.translate.CaptionTranslator
import com.superdevlabs.autosubtitles.translate.TranslateMode
import com.superdevlabs.autosubtitles.ui.components.PillButton
import com.superdevlabs.autosubtitles.ui.components.SectionLabel
import com.superdevlabs.autosubtitles.ui.components.Segmented
import com.superdevlabs.autosubtitles.ui.components.pressable
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import java.util.Locale

/**
 * Translate tab: pick any of ML Kit's ~59 languages. Each user downloads only the language
 * pack they pick, so the app serves every country without shipping all of them.
 */
@Composable
fun TranslatePanel(
    translator: CaptionTranslator,
    sourceCode: String,
    hasTranslation: Boolean,
    state: TranslationUi,
    onTranslate: (String, TranslateMode) -> Unit,
    onRemove: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(TranslateMode.BILINGUAL) }
    var query by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    val sourceName = remember(sourceCode) {
        if (sourceCode.isBlank()) "Unknown" else Locale.forLanguageTag(sourceCode).getDisplayLanguage(Locale.ENGLISH)
    }
    val sourceSupported = remember(sourceCode) { sourceCode.isNotBlank() && translator.isSupported(sourceCode) }
    // Put the phone's own language first: the most likely choice.
    val deviceCode = remember { Locale.getDefault().language }
    val languages = remember(query) {
        val q = query.trim().lowercase()
        translator.targets
            .filter { it.code != sourceCode }
            .filter { q.isEmpty() || it.english.lowercase().contains(q) || it.name.lowercase().contains(q) }
            .sortedBy { if (it.code == deviceCode) 0 else 1 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Translate, contentDescription = null, tint = AppColors.Lime, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                SectionLabel("Spoken: $sourceName · offline translation, free")
            }
        }
        if (!sourceSupported) {
            item {
                Text(
                    "Translation from $sourceName isn't available yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Coral,
                )
            }
            return@LazyColumn
        }
        item {
            Segmented(
                options = TranslateMode.entries,
                selected = mode,
                label = { it.label },
                onSelect = { mode = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { Text(mode.hint, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted) }

        if (state.running) {
            item { TranslationProgress(state) }
        } else {
            state.error?.let { error ->
                item { Text(error, style = MaterialTheme.typography.bodySmall, color = AppColors.Coral) }
            }
            if (hasTranslation) {
                item { PillButton("Remove 2nd-line translation", onRemove, Modifier.fillMaxWidth(), primary = false, height = 44.dp) }
            }
            target?.let { code ->
                val name = translator.targets.firstOrNull { it.code == code }?.english ?: code
                item {
                    PillButton(
                        "Translate to $name",
                        onClick = { onTranslate(code, mode) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Translate,
                        height = 50.dp,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(30) },
                    singleLine = true,
                    placeholder = { Text("Search language") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Lime,
                        unfocusedBorderColor = AppColors.Line,
                        cursorColor = AppColors.Lime,
                    ),
                )
            }
            items(languages, key = { it.code }) { language ->
                val selected = language.code == target
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) AppColors.Lime.copy(alpha = 0.14f) else AppColors.SurfaceHigh)
                        .border(1.5.dp, if (selected) AppColors.Lime else AppColors.Line, RoundedCornerShape(14.dp))
                        .pressable(onClick = { target = language.code }, role = Role.RadioButton, pressedScale = 0.98f)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(if (selected) AppColors.Lime else AppColors.SurfaceTop))
                    Spacer(Modifier.width(12.dp))
                    Text(language.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(language.english, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun TranslationProgress(state: TranslationUi) {
    val progress by animateFloatAsState(state.progress, label = "translate")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(state.status, style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(AppColors.SurfaceTop)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f)).height(6.dp).clip(CircleShape).background(AppColors.Lime))
        }
    }
}
