package com.driveready.testprep.ui.signs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.content.signsFor
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.SignGroup
import com.driveready.testprep.data.model.SignInfo
import com.driveready.testprep.ui.components.AmberButton
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.PrimaryButton
import com.driveready.testprep.ui.components.RoadSign
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.components.SecondaryButton
import com.driveready.testprep.ui.theme.Brand
import com.driveready.testprep.ui.theory.giveWayText

@Composable
fun SignsScreen(
    country: Country,
    mastered: Set<String>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onToggleMastered: (String) -> Unit,
    onQuiz: () -> Unit
) {
    val all = remember(country.id) { signsFor(country) }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<SignInfo?>(null) }
    val visible = all.filter { group == null || it.group.name == group }
    val masteredCount = all.count { "${country.id}:${it.type.name}" in mastered }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        ScreenHeader("Road signs", "${country.name} style · $masteredCount of ${all.size} mastered", onBack = onBack)
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = group == null, onClick = { group = null }, label = { Text("All") })
            SignGroup.entries.forEach { g ->
                FilterChip(selected = group == g.name, onClick = { group = g.name }, label = { Text(g.label) })
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AmberButton("Quiz me on signs", onQuiz, Modifier.fillMaxWidth(), icon = AppIcons.Play)
            }
            items(visible.take(9), key = { it.type.name }) { sign -> SignCell(country, sign, "${country.id}:${sign.type.name}" in mastered) { selected = sign } }
            if (visible.size > 9) {
                item(span = { GridItemSpan(maxLineSpan) }) { NativeAdCard() }
                items(visible.drop(9), key = { it.type.name }) { sign -> SignCell(country, sign, "${country.id}:${sign.type.name}" in mastered) { selected = sign } }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Shape rule: circles give orders, ${if (country.signStyle.name == "DIAMOND") "yellow diamonds" else "triangles"} warn, rectangles inform.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.Muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    selected?.let { sign ->
        val key = "${country.id}:${sign.type.name}"
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = Brand.Surface) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RoadSign(sign.type, country.signStyle, size = 150.dp, value = sign.value, rectangularSpeed = country.id == "us" || country.id == "ca", giveWayText = giveWayText(country), description = sign.name)
                Spacer(Modifier.height(16.dp))
                Text(sign.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(sign.group.label, style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
                Spacer(Modifier.height(12.dp))
                Text(sign.meaning, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Brand.Muted)
                Spacer(Modifier.height(20.dp))
                if (key in mastered) {
                    SecondaryButton("Mastered — mark to review", { onToggleMastered(sign.type.name) }, Modifier.fillMaxWidth(), icon = AppIcons.Check)
                } else {
                    PrimaryButton("I know this sign", { onToggleMastered(sign.type.name) }, Modifier.fillMaxWidth(), icon = AppIcons.Check)
                }
            }
        }
    }
}

@Composable
private fun SignCell(country: Country, sign: SignInfo, mastered: Boolean, onClick: () -> Unit) {
    AppCard(onClick = onClick, contentPadding = PaddingValues(10.dp), color = if (mastered) Brand.GoodSoft else Brand.Card) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            RoadSign(sign.type, country.signStyle, size = 64.dp, value = sign.value, rectangularSpeed = country.id == "us" || country.id == "ca", giveWayText = giveWayText(country), description = sign.name)
            Spacer(Modifier.height(8.dp))
            Text(sign.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
            Text(
                if (mastered) "Mastered" else sign.group.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (mastered) Brand.Good else Brand.Muted
            )
        }
    }
}
