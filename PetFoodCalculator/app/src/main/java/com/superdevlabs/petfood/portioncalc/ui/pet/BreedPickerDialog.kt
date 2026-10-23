package com.superdevlabs.petfood.portioncalc.ui.pet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.domain.Breed
import com.superdevlabs.petfood.portioncalc.domain.Breeds
import com.superdevlabs.petfood.portioncalc.domain.Species
import com.superdevlabs.petfood.portioncalc.ui.AppIcon
import com.superdevlabs.petfood.portioncalc.ui.RealPhoto

/** Searchable breed list with a real photo per breed. */
@Composable
fun BreedPickerDialog(species: Species, onPick: (Breed) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val breeds = remember(species, query) {
        Breeds.forSpecies(species).filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.86f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Choose ${species.label.lowercase()} breed", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { AppIcon(R.drawable.ic_close, "Close") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    label = { Text("Search ${Breeds.forSpecies(species).size} breeds") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.padding(top = 8.dp)) {
                    items(breeds, key = { it.id }) { breed ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onPick(breed) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RealPhoto(breed.wikiTitle, null, size = 56.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(breed.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (breed.isMixed) "Any size" else "${formatKg(breed.minKg)}–${formatKg(breed.maxKg)} kg adult",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (breeds.isEmpty()) {
                        item {
                            Text(
                                "No match. Choose \"${Breeds.defaultFor(species).name}\" and the size comes from your pet's weight.",
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatKg(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
