package com.driveready.testprep.ui.syllabus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.Region
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.Disclaimer
import com.driveready.testprep.ui.components.LinearMeter
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.theme.Brand

/** The country's full syllabus with tick-off progress per topic. */
@Composable
fun SyllabusScreen(
    country: Country,
    region: Region?,
    done: Set<String>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onToggle: (String) -> Unit
) {
    val expanded = remember { mutableStateMapOf<Int, Boolean>().apply { put(0, true) } }
    val totalTopics = country.syllabus.sumOf { it.topics.size }
    val doneCount = country.syllabus.sumOf { m -> m.topics.count { "${country.id}:${m.title}|$it" in done } }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        ScreenHeader("Syllabus", listOfNotNull(region?.name, country.name).joinToString(", "), onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AppCard(color = Brand.Navy, border = false) {
                    Text("$doneCount of $totalTopics topics covered", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    LinearMeter(if (totalTopics == 0) 0f else doneCount.toFloat() / totalTopics, color = Brand.Amber, track = androidx.compose.ui.graphics.Color(0xFF2B3446))
                    Spacer(Modifier.height(10.dp))
                    Text("Licence path", color = Brand.Amber, style = MaterialTheme.typography.labelMedium)
                    country.licenceStages.forEachIndexed { i, stage ->
                        Text("${i + 1}. $stage", color = androidx.compose.ui.graphics.Color(0xFFD4D6DB), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Official source: ${country.officialSite}", color = Brand.Amber, style = MaterialTheme.typography.bodySmall)
                    region?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("${it.name}: ${it.questions} questions, ${it.passMark} to pass (${it.passPercent}%). Permit age ${it.permitAge}.", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodySmall)
                        if (it.notes.isNotBlank()) Text(it.notes, color = androidx.compose.ui.graphics.Color(0xFFD4D6DB), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            itemsIndexed(country.syllabus) { index, module ->
                val isOpen = expanded[index] == true
                val moduleDone = module.topics.count { "${country.id}:${module.title}|$it" in done }
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded[index] = !isOpen }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(module.title, style = MaterialTheme.typography.titleSmall)
                            Text("$moduleDone/${module.topics.size} topics", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                        }
                        Icon(if (isOpen) AppIcons.ChevronDown else AppIcons.ChevronRight, contentDescription = if (isOpen) "Collapse" else "Expand")
                    }
                    AnimatedVisibility(isOpen) {
                        Column(Modifier.padding(start = 8.dp, end = 16.dp, bottom = 10.dp)) {
                            module.topics.forEach { topic ->
                                val key = "${module.title}|$topic"
                                val checked = "${country.id}:$key" in done
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable { onToggle(key) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = { onToggle(key) }, colors = CheckboxDefaults.colors(checkedColor = Brand.Ink))
                                    Spacer(Modifier.width(4.dp))
                                    Text(topic, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                if (index == 2) NativeAdCard()
            }
            item { Disclaimer() }
        }
    }
}
