package com.brickbeam.buildbros.ui.calc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildbros.calc.LengthParser
import com.brickbeam.buildbros.calc.UnitSystem
import com.brickbeam.buildbros.data.AppSettings
import com.brickbeam.buildbros.data.DraftItem
import com.brickbeam.buildbros.ui.CalcContext
import com.brickbeam.buildbros.ui.LocalAppEnv
import com.brickbeam.buildbros.ui.Routes
import com.brickbeam.buildbros.ui.components.InputField
import com.brickbeam.buildbros.ui.components.PrimaryButton
import com.brickbeam.buildbros.ui.components.ScreenHeader
import com.brickbeam.buildbros.ui.theme.BmIcons
import com.brickbeam.buildbros.ui.theme.BmType
import com.brickbeam.buildbros.ui.theme.LocalPalette

@Composable
fun rememberCalcContext(): CalcContext {
    val env = LocalAppEnv.current
    val settings by env.vm.settings.collectAsStateWithLifecycle()
    val prices by env.vm.prices.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    return remember(s, prices) { CalcContext(s, prices) }
}

/**
 * Standard calculator page: header, scrolling body and a pinned action row with
 * "Add to estimate", which opens the project picker sheet.
 */
@Composable
fun CalcPage(
    title: String,
    subtitle: String,
    drafts: () -> List<DraftItem>,
    canAdd: Boolean,
    secondary: @Composable RowScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    val env = LocalAppEnv.current
    var sheet by remember { mutableStateOf<List<DraftItem>?>(null) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(title, subtitle, onBack = { env.back() })
            body()
            Spacer(Modifier.height(4.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            secondary()
            PrimaryButton(
                "Add to estimate",
                onClick = { sheet = drafts() },
                modifier = Modifier.weight(1f),
                icon = BmIcons.Add,
                enabled = canAdd,
            )
        }
    }
    sheet?.let { items -> AddToProjectSheet(items, onDismiss = { sheet = null }) }
}

@Composable
fun AddToProjectSheet(drafts: List<DraftItem>, onDismiss: () -> Unit) {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    val projects by env.vm.projects.collectAsStateWithLifecycle()
    var picked by remember { mutableStateOf<Long?>(null) }
    var newName by remember { mutableStateOf("") }
    // Until the user taps something, default to the most recent project (or "New").
    val selected = picked ?: projects.firstOrNull()?.id ?: NEW
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = p.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add ${drafts.size} item${if (drafts.size == 1) "" else "s"} to…", style = BmType.title, color = p.ink)
            projects.take(6).forEach { pr ->
                ProjectRadio(pr.name, "${pr.itemCount} items", pr.id == selected) { picked = pr.id }
            }
            ProjectRadio("New project", "Creates a project in your current region", selected == NEW) { picked = NEW }
            if (selected == NEW || projects.isEmpty()) {
                InputField("Project name", newName, { newName = it }, keyboardType = KeyboardType.Text, hint = "e.g. Backyard studio")
            }
            PrimaryButton(
                "Save to project",
                onClick = {
                    val target = selected.takeIf { it != NEW }
                    env.vm.saveDrafts(target, newName, drafts) { id, name ->
                        env.afterMajorAction("Added ${drafts.size} items to $name", "Open") { env.go(Routes.estimate(id)) }
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                icon = BmIcons.Check,
            )
        }
    }
}

private const val NEW = -1L

@Composable
private fun ProjectRadio(title: String, sub: String, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) p.glow else p.surface2)
            .border(1.dp, if (on) p.accentText else p.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = on }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) p.accentText else p.mute, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(p.accentText)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = BmType.section, color = p.ink)
            Text(sub, style = BmType.tiny, color = p.mute)
        }
        if (on) Icon(BmIcons.Check, null, tint = p.accentText)
    }
}

/** Parses a big length field; returns metres or null. */
fun parseLen(text: String, system: UnitSystem): Double? = LengthParser.parse(text, system)?.takeIf { it > 0 }

/** Parses a small dimension field; returns millimetres or null. */
fun parseSmall(text: String, system: UnitSystem): Double? = LengthParser.parseSmallMm(text, system)?.takeIf { it > 0 }

fun parseNum(text: String): Double? = LengthParser.parseNumber(text)

fun lengthHint(system: UnitSystem) = if (system == UnitSystem.IMPERIAL) "e.g. 12' 6\" or 12.5" else "e.g. 3.6 or 360 cm"
