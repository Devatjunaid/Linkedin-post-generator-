package com.superdevlabs.petfood.portioncalc.ui.breed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superdevlabs.petfood.portioncalc.ads.BannerAd
import com.superdevlabs.petfood.portioncalc.ads.NativeAdCard
import com.superdevlabs.petfood.portioncalc.domain.ActivityLevel
import com.superdevlabs.petfood.portioncalc.domain.Breeds
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.NutritionCalculator
import com.superdevlabs.petfood.portioncalc.domain.PetProfile
import com.superdevlabs.petfood.portioncalc.domain.Species
import com.superdevlabs.petfood.portioncalc.domain.WeightUnit
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.Pill
import com.superdevlabs.petfood.portioncalc.ui.RealPhoto
import com.superdevlabs.petfood.portioncalc.ui.SectionCard
import com.superdevlabs.petfood.portioncalc.ui.openUrl
import com.superdevlabs.petfood.portioncalc.ui.rememberWikiSummary

@Composable
fun BreedGuideScreen(breedId: String, onBack: () -> Unit) {
    val breed = Breeds.get(breedId) ?: return
    val c = LocalAppContainer.current
    val unit by c.settings.weightUnit.collectAsStateWithLifecycle(initialValue = WeightUnit.KG)
    val summary = rememberWikiSummary(breed.wikiTitle)
    val context = LocalContext.current
    val avgKg = (breed.minKg + breed.maxKg) / 2

    fun adultKcal(kg: Double) = NutritionCalculator.energy(
        PetProfile(breed.species, breed, 36, kg, null, true, ActivityLevel.NORMAL, 5, 0),
    ).dailyKcal

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = breed.name, subtitle = "Breed feeding guide", onBack = onBack) },
        bottomBar = { BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                RealPhoto(
                    wikiTitle = breed.wikiTitle,
                    contentDescription = "Photo of a ${breed.name}",
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                    shape = RoundedCornerShape(28.dp),
                )
            }
            item {
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(breed.species.label)
                        if (!breed.isMixed && breed.species == Species.DOG) Pill("${breed.sizeClassFor(avgKg).label} size")
                    }
                    if (!breed.isMixed) {
                        Text("Typical adult weight: ${Format.weight(breed.minKg, unit)} – ${Format.weight(breed.maxKg, unit)}", style = MaterialTheme.typography.titleMedium)
                    }
                    if (breed.species == Species.DOG) {
                        val size = breed.sizeClassFor(avgKg)
                        Text(
                            "Fully grown at about ${size.adultAtMonths} months (feed puppy food until then); senior from about ${size.seniorAtYears} years.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text("Kitten food until 12 months; senior from about 11 years.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (!breed.isMixed) {
                item {
                    SectionCard {
                        Text("Daily calories for a neutered adult", style = MaterialTheme.typography.titleMedium)
                        listOf(breed.minKg, avgKg, breed.maxKg).forEach { kg ->
                            Row {
                                Text(Format.weight(kg, unit), Modifier.weight(1f))
                                Text(Format.kcal(adultKcal(kg)), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Text(
                            "Normal activity, no treats. Add your own pet for exact cups and grams.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { NativeAdCard(showMedia = false) }
            summary?.extract?.let { extract ->
                item {
                    SectionCard {
                        Text("About the breed", style = MaterialTheme.typography.titleMedium)
                        Text(extract, style = MaterialTheme.typography.bodyMedium)
                        summary.pageUrl?.let { url ->
                            TextButton(onClick = { openUrl(context, url) }) {
                                Text("Text & photo: Wikipedia (CC BY-SA) · view source and author")
                            }
                        }
                    }
                }
            }
        }
    }
}
