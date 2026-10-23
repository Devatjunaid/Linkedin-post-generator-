package com.driveready.testprep.ui.country

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.content.Countries
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.Region
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.Disclaimer
import com.driveready.testprep.ui.components.FlagBadge
import com.driveready.testprep.ui.components.Pill
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.theme.Brand

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Brand.Card,
            unfocusedContainerColor = Brand.Card,
            focusedBorderColor = Brand.Ink,
            unfocusedBorderColor = Brand.Line
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

@Composable
fun CountryPickerScreen(
    selectedId: String?,
    onBack: (() -> Unit)?,
    onPick: (Country) -> Unit,
    contentPadding: PaddingValues
) {
    var query by rememberSaveable { mutableStateOf("") }
    val list = Countries.filter { it.name.contains(query.trim(), ignoreCase = true) || it.code.contains(query.trim(), ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        ScreenHeader("Where is your test?", "Rules, signs and test format follow your licensing authority.", onBack = onBack)
        SearchField(query, { query = it }, "Search country")
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(list, key = { _, c -> c.id }) { index, country ->
                CountryRow(country, country.id == selectedId) { onPick(country) }
                if (index == 4) {
                    Spacer(Modifier.size(10.dp))
                    NativeAdCard()
                }
            }
            item { Disclaimer(Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun CountryRow(country: Country, selected: Boolean, onClick: () -> Unit) {
    AppCard(onClick = onClick, color = if (selected) Brand.AmberSoft else Brand.Card, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagBadge(country, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(country.name, style = MaterialTheme.typography.titleSmall)
                Text(country.authority, style = MaterialTheme.typography.bodySmall, color = Brand.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (country.hasRegions) Pill("${country.regions.size} ${country.regionKind?.plural ?: "regions"}", Brand.BlueSoft, Brand.Blue)
                    if (country.hazard?.isOfficialPart == true) Pill("Hazard test")
                    Pill(if (country.drivingSide.name == "LEFT") "Drive left" else "Drive right", Brand.Surface, Brand.Muted)
                }
            }
            Icon(if (selected) AppIcons.Check else AppIcons.ChevronRight, contentDescription = null, tint = Brand.Ink)
        }
    }
}

@Composable
fun RegionPickerScreen(
    country: Country,
    selectedId: String?,
    onBack: () -> Unit,
    onPick: (Region) -> Unit,
    contentPadding: PaddingValues
) {
    var query by rememberSaveable { mutableStateOf("") }
    val list = country.regions.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val kind = country.regionKind?.singular ?: "region"
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        ScreenHeader("Choose your $kind", "${country.name}: each $kind sets its own knowledge test.", onBack = onBack)
        SearchField(query, { query = it }, "Search $kind")
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(list, key = { _, r -> r.id }) { index, region ->
                AppCard(
                    onClick = { onPick(region) },
                    color = if (region.id == selectedId) Brand.AmberSoft else Brand.Card,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(region.name, style = MaterialTheme.typography.titleSmall)
                            Text(region.agency, style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${region.passMark}/${region.questions}", style = MaterialTheme.typography.titleSmall)
                            Text("to pass · ${region.passPercent}%", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                        }
                    }
                }
                if (index % 12 == 5) {
                    Spacer(Modifier.size(10.dp))
                    NativeAdCard()
                }
            }
            item { Disclaimer(Modifier.padding(top = 8.dp)) }
        }
    }
}
