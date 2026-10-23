package com.devatjunaid.examclock.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.data.catalog.Country
import com.devatjunaid.examclock.data.catalog.ExamCatalog
import com.devatjunaid.examclock.data.catalog.ExamCategory
import com.devatjunaid.examclock.data.catalog.ExamTemplate
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.Format

/** Three-step setup: country → exams → goal & reminders. */
@Composable
fun SetupScreen(onFinished: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { SetupViewModel(it, context) }
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler { if (state.step > 0) vm.goTo(state.step - 1) else onExit() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.setReminders(false)
        vm.finish(onFinished)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (state.step > 0) vm.goTo(state.step - 1) else onExit() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.setup_step, state.step + 1, 3),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= state.step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }

        AnimatedContent(targetState = state.step, modifier = Modifier.weight(1f), label = "setupStep") { step ->
            when (step) {
                0 -> CountryStep(state, vm)
                1 -> ExamsStep(state, vm)
                else -> ProfileStep(state, vm)
            }
        }

        val buttonText = when (state.step) {
            0 -> stringResource(R.string.setup_continue_with, state.country.name)
            1 -> if (state.selectedPaperCount == 0) {
                stringResource(R.string.setup_skip_exams)
            } else {
                stringResource(R.string.setup_continue_count, state.selectedPaperCount)
            }
            else -> stringResource(R.string.setup_finish)
        }
        Button(
            onClick = {
                when (state.step) {
                    0, 1 -> vm.goTo(state.step + 1)
                    else -> {
                        if (state.remindersEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.finish(onFinished)
                        }
                    }
                }
            },
            enabled = (state.step != 0 || state.countryCode.isNotEmpty()) && !state.saving,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(54.dp),
        ) { Text(buttonText) }
    }
}

@Composable
private fun StepTitle(title: String, body: String) {
    Column(Modifier.padding(top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CountryStep(state: SetupState, vm: SetupViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { StepTitle(stringResource(R.string.setup_country_title), stringResource(R.string.setup_country_body)) }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                label = { Text(stringResource(R.string.setup_search_country)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val detected = state.detected
        if (detected != null && state.query.isBlank()) {
            item {
                Text(
                    stringResource(R.string.setup_detected).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item { CountryRow(detected, selected = state.countryCode == detected.code) { vm.chooseCountry(detected.code) } }
            item {
                Text(
                    stringResource(R.string.setup_all_countries).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        items(state.filteredCountries, key = { it.code }) { country ->
            CountryRow(country, selected = state.countryCode == country.code) { vm.chooseCountry(country.code) }
        }
    }
}

@Composable
private fun CountryRow(country: Country, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) scheme.surface else scheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (country.code == ExamCatalog.other.code) "··" else country.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(country.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (country.templates.isEmpty()) stringResource(R.string.setup_other_country_hint) else country.highlights,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (selected) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.cd_selected), tint = scheme.primary)
        }
    }
}

@Composable
private fun ExamsStep(state: SetupState, vm: SetupViewModel) {
    var category by remember { mutableStateOf<ExamCategory?>(null) }
    val all = ExamCatalog.templatesFor(state.countryCode)
    val shown = all.filter { category == null || it.category == category }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { StepTitle(stringResource(R.string.setup_exams_title), stringResource(R.string.setup_exams_body)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text(stringResource(R.string.category_all)) })
                ExamCategory.entries.filter { c -> all.any { it.category == c } }.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(stringResource(categoryLabel(c))) })
                }
            }
        }
        items(shown, key = { it.id }) { template ->
            TemplateCard(
                template = template,
                selectedPapers = state.selection[template.id].orEmpty(),
                expanded = state.expanded == template.id,
                onToggle = { vm.toggleTemplate(template) },
                onExpand = { vm.expand(template.id) },
                onTogglePaper = { vm.togglePaper(template, it) },
            )
        }
        item {
            Text(
                stringResource(R.string.setup_exams_dates_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

fun categoryLabel(c: ExamCategory): Int = when (c) {
    ExamCategory.SCHOOL -> R.string.category_school
    ExamCategory.ENTRANCE -> R.string.category_entrance
    ExamCategory.INTERNATIONAL -> R.string.category_international
}

@Composable
fun TemplateCard(
    template: ExamTemplate,
    selectedPapers: Set<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onTogglePaper: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val selected = selectedPapers.isNotEmpty()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier
                    .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    val count = if (selected) selectedPapers.size else template.papers.size
                    Text(
                        stringResource(R.string.template_subtitle, template.board, count),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onExpand) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_choose_papers),
                    )
                }
            }
            AnimatedVisibility(expanded) {
                FlowRow(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    template.papers.forEach { paper ->
                        FilterChip(selected = paper in selectedPapers, onClick = { onTogglePaper(paper) }, label = { Text(paper) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStep(state: SetupState, vm: SetupViewModel) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepTitle(stringResource(R.string.setup_profile_title), stringResource(R.string.setup_profile_body))
        OutlinedTextField(
            value = state.name,
            onValueChange = vm::setName,
            label = { Text(stringResource(R.string.setup_name_label)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.setup_goal_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(Format.minutes(context, state.dailyGoalMinutes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = state.dailyGoalMinutes.toFloat(),
                    onValueChange = { vm.setGoal(((it / 15f).toInt() * 15).coerceIn(30, 600)) },
                    valueRange = 30f..600f,
                    steps = (600 - 30) / 15 - 1,
                )
                Text(stringResource(R.string.setup_goal_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(
                Modifier
                    .toggleable(value = state.remindersEnabled, role = Role.Switch, onValueChange = vm::setReminders)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.setup_reminders_label), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.setup_reminders_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.remindersEnabled, onCheckedChange = null)
            }
        }
    }
}
