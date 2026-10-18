package com.brickbeam.buildmate.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.Regions
import com.brickbeam.buildmate.data.AppSettings
import com.brickbeam.buildmate.data.db.ProjectSummary
import com.brickbeam.buildmate.ui.LocalAppEnv
import com.brickbeam.buildmate.ui.Routes
import com.brickbeam.buildmate.ui.components.AccentButton
import com.brickbeam.buildmate.ui.components.BmCard
import com.brickbeam.buildmate.ui.components.Fmt
import com.brickbeam.buildmate.ui.components.InputField
import com.brickbeam.buildmate.ui.components.Reveal
import com.brickbeam.buildmate.ui.components.ScreenHeader
import com.brickbeam.buildmate.ui.graphics.EstimateArt
import com.brickbeam.buildmate.ui.theme.BmIcons
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette
import java.text.DateFormat
import java.util.Date

@Composable
fun ProjectsScreen() {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    val projects by env.vm.projects.collectAsStateWithLifecycle()
    val settings by env.vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProjectSummary?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenHeader("Projects", "${projects.size} saved on this device") }
        item {
            AccentButton("New project", { creating = true }, Modifier.fillMaxWidth(), icon = BmIcons.Add)
        }
        if (projects.isEmpty()) {
            item {
                BmCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EstimateArt(Modifier.size(64.dp, 46.dp), p.accentText)
                        Column(Modifier.weight(1f)) {
                            Text("No projects yet", style = BmType.section, color = p.ink)
                            Text("Run any calculator and tap “Add to estimate”, or create a project here.", style = BmType.tiny, color = p.mute)
                        }
                    }
                }
            }
            item { NativeAdSlot("projects") }
        }
        itemsIndexed(projects, key = { _, pr -> pr.id }) { i, pr ->
            val region = Regions.byCode(pr.regionCode) ?: s.region
            val fmt = Fmt(s.unitSystem, region)
            Reveal(i.coerceAtMost(6)) {
                BmCard(Modifier.fillMaxWidth().clickable(role = Role.Button) { env.go(Routes.estimate(pr.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(pr.name, style = BmType.section, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${region.name} · ${pr.itemCount} items · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(pr.updatedAt))}",
                                style = BmType.tiny, color = p.mute,
                            )
                            Text("${fmt.money(pr.materials)} materials", style = BmType.figure.copy(fontSize = BmType.label.fontSize), color = p.ink)
                        }
                        IconButton(onClick = { deleting = pr }) { Icon(BmIcons.Delete, "Delete ${pr.name}", tint = p.mute) }
                    }
                }
            }
            if (i == 1 || (i == projects.lastIndex && projects.size < 2)) NativeAdSlot("projects")
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField("Project name", name, { name = it }, Modifier.fillMaxWidth(), keyboardType = KeyboardType.Text, hint = "e.g. Garage slab")
                    Text("Region: ${s.region.name} (${s.region.currency})", style = BmType.tiny, color = p.mute)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    creating = false
                    env.vm.createProject(name) { id -> env.go(Routes.estimate(id)) }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
        )
    }

    deleting?.let { pr ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${pr.name}”?") },
            text = { Text("The project and its ${pr.itemCount} items will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = { env.vm.deleteProject(pr.id); deleting = null }) { Text("Delete", color = p.danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } },
        )
    }
}
