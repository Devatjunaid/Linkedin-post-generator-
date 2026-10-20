package com.superdevlabs.autosubtitles.ui.projects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.ads.NativeAdCard
import com.superdevlabs.autosubtitles.data.Project
import com.superdevlabs.autosubtitles.data.ProjectStatus
import com.superdevlabs.autosubtitles.ui.components.SectionLabel
import com.superdevlabs.autosubtitles.ui.components.Thumbnail
import com.superdevlabs.autosubtitles.ui.components.pressable
import com.superdevlabs.autosubtitles.ui.home.formatDuration
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(private val container: AppContainer) : ViewModel() {
    val projects: StateFlow<List<Project>> = container.projects.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { container.projects.delete(id) }
    }
}

private sealed interface GridEntry {
    data class Item(val project: Project, val index: Int) : GridEntry
    data class Ad(val slot: Int) : GridEntry
}

@Composable
fun ProjectsScreen(
    container: AppContainer,
    contentPadding: PaddingValues,
    onOpenProject: (id: String, ready: Boolean) -> Unit,
) {
    val vm: ProjectsViewModel = viewModel(factory = viewModelFactory { initializer { ProjectsViewModel(container) } })
    val projects by vm.projects.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Project?>(null) }

    // A native ad sits in the free space after every 6th project.
    val entries = buildList {
        projects.forEachIndexed { i, p ->
            add(GridEntry.Item(p, i))
            if ((i + 1) % 6 == 0) add(GridEntry.Ad(i))
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Projects", style = MaterialTheme.typography.headlineMedium)
            SectionLabel("${projects.size} on this device")
        }
        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                Text("No projects yet — pick a video on Home.", color = AppColors.Muted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                entries.forEach { entry ->
                    when (entry) {
                        is GridEntry.Item -> item(key = entry.project.id) {
                            ProjectTile(
                                project = entry.project,
                                index = entry.index,
                                onOpen = { onOpenProject(entry.project.id, entry.project.isReady) },
                                onDelete = { pendingDelete = entry.project },
                            )
                        }
                        is GridEntry.Ad -> item(key = "ad-${entry.slot}", span = { GridItemSpan(maxLineSpan) }) {
                            NativeAdCard(container.ads)
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete project?") },
            text = { Text("“${project.name}” and its subtitles will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(project.id)
                    pendingDelete = null
                }) { Text("Delete", color = AppColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            containerColor = AppColors.Surface,
        )
    }
}

@Composable
private fun ProjectTile(project: Project, index: Int, onOpen: () -> Unit, onDelete: () -> Unit) {
    // Staggered entrance: tiles drop in one after another.
    val appear = remember(project.id) { Animatable(0f) }
    LaunchedEffect(project.id) {
        appear.animateTo(1f, tween(durationMillis = 420, delayMillis = (index % 8) * 60))
    }
    Column(
        Modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 40f
                rotationX = (1f - appear.value) * 18f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(20.dp))
            .pressable(onClick = onOpen, pressedScale = 0.96f)
            .padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(14.dp))) {
            Thumbnail(project.thumbnail, Modifier.fillMaxSize(), contentDescription = project.name)
            val (label, color) = when (project.status) {
                ProjectStatus.READY -> "READY" to AppColors.Lime
                ProjectStatus.FAILED -> "FAILED" to AppColors.Coral
                else -> "DRAFT" to AppColors.Ice
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Ink,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatDuration(project.durationMs)} · ${project.captions.size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Muted,
                )
            }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).pressable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${project.name}", tint = AppColors.Muted)
            }
        }
    }
}
