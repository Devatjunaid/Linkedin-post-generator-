package com.harrycodess.drawingkids.ui.coloring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harrycodess.drawingkids.ads.BannerAd
import com.harrycodess.drawingkids.art.AnimalArt
import com.harrycodess.drawingkids.art.Animals
import com.harrycodess.drawingkids.art.Category
import com.harrycodess.drawingkids.data.Prefs
import com.harrycodess.drawingkids.drawing.Tool
import com.harrycodess.drawingkids.ui.components.ArtThumb
import com.harrycodess.drawingkids.ui.components.Chunky
import com.harrycodess.drawingkids.ui.components.IconButton32
import com.harrycodess.drawingkids.ui.components.KidIcons.backIcon
import com.harrycodess.drawingkids.ui.components.Pill
import com.harrycodess.drawingkids.ui.components.ToolIcon
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.kidText

@Composable
fun PickerScreen(onBack: () -> Unit, onPick: (AnimalArt) -> Unit) {
    var category by remember { mutableStateOf<Category?>(null) }
    val progress = remember { Animals.all.associate { it.id to Prefs.loadFills(it.id) } }
    val shown = Animals.all.filter { category == null || it.category == category }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton32(onBack) { backIcon() }
            Text("Pick an Animal", style = kidText(26))
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Pill("All", category == null, { category = null }) }
            items(Category.entries) { c -> Pill(c.label, category == c, { category = c }) }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shown, key = { it.id }) { art ->
                val fills = progress[art.id].orEmpty()
                Chunky(Modifier.height(186.dp), corner = 26.dp, depth = 6.dp, onClick = { onPick(art) }) {
                    Column(
                        Modifier.fillMaxSize().padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ArtThumb(art, fills, Modifier.weight(1f).fillMaxWidth())
                        Text(art.name, style = kidText(18))
                    }
                    if (fills.isNotEmpty()) {
                        Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            ToolIcon(Tool.STAMP, Kid.Yellow, Modifier.size(30.dp))
                        }
                    }
                }
            }
        }
        BannerAd()
    }
}
