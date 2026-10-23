package com.devatjunaid.examclock.ui.exams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.data.catalog.ExamCatalog
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.onboarding.TemplateCard

/** Browse the exams of the user's country (plus international ones) and add papers in one go. */
@Composable
fun CatalogScreen(onBack: () -> Unit, onAdded: (Int) -> Unit) {
    val vm = appViewModel { CatalogViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val country = ExamCatalog.country(state.countryCode)

    ScreenScaffold(
        title = stringResource(R.string.catalog_title),
        onBack = onBack,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = { vm.addSelected(onAdded) },
                    enabled = state.selectedCount > 0 && !state.saving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(54.dp),
                ) { Text(stringResource(R.string.catalog_add_count, state.selectedCount)) }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.catalog_body, country.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    selectedPapers = state.selection[template.id].orEmpty(),
                    expanded = state.expanded == template.id,
                    onToggle = { vm.toggle(template) },
                    onExpand = { vm.expand(template.id) },
                    onTogglePaper = { vm.togglePaper(template, it) },
                )
            }
            item {
                Text(
                    stringResource(R.string.setup_exams_dates_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
