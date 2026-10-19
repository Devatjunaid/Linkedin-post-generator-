package com.dailycash.tracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycash.tracker.data.Categories
import com.dailycash.tracker.data.Category
import com.dailycash.tracker.data.CategoryGroup
import com.dailycash.tracker.data.CustomCategory
import com.dailycash.tracker.ui.components.AppTextField
import com.dailycash.tracker.ui.components.CategoryBadge
import com.dailycash.tracker.ui.theme.Palette

/** One tappable category: icon tile plus name. */
@Composable
fun CategoryTile(category: Category, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, label = "tile")
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) category.group.tile else Color.Transparent)
            .border(1.5.dp, if (selected) category.group.tint else Color.Transparent, RoundedCornerShape(16.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.scale(scale)) { CategoryBadge(category, 46.dp) }
        Spacer(Modifier.height(6.dp))
        Text(
            category.name,
            color = Palette.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp,
        )
    }
}

/** All 60 everyday categories, grouped and searchable, plus the "name your own" box. */
@Composable
fun CategorySheet(
    selectedId: String?,
    custom: List<CustomCategory>,
    onPick: (Category) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    var ownName by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val customCats = remember(custom) { custom.map { Categories.custom(it.name) } }
    val groups = remember(q, customCats) {
        val all = customCats + Categories.spend
        val filtered = if (q.isEmpty()) all else all.filter { it.name.contains(q, ignoreCase = true) || it.group.title.contains(q, true) }
        filtered.groupBy { it.group }.toList().sortedBy { if (it.first == CategoryGroup.CUSTOM) -1 else it.first.ordinal }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Palette.Surface) {
        Column(Modifier.fillMaxHeight(0.92f).padding(horizontal = 16.dp)) {
            Text("What did you pay for?", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            AppTextField(query, { query = it }, "Search 60 categories", leading = Icons.Rounded.Search, imeAction = ImeAction.Search)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 78.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "own") {
                    Column(
                        Modifier
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Palette.Surface2)
                            .border(1.dp, Palette.Line, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                    ) {
                        Text("Something else?", color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Name it once — it stays in your list.", color = Palette.Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppTextField(
                                ownName,
                                { if (it.length <= 32) ownName = it },
                                "Your own category",
                                modifier = Modifier.weight(1f),
                                placeholder = if (q.isNotEmpty()) q else "e.g. Rickshaw, Laundry tokens",
                                leading = Icons.Rounded.Edit,
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val name = ownName.ifBlank { q }
                                    if (name.isNotBlank()) {
                                        onCreate(name)
                                        ownName = ""
                                    }
                                },
                                enabled = ownName.isNotBlank() || q.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Palette.Mint, contentColor = Palette.Ink),
                                modifier = Modifier.height(56.dp),
                            ) { Text("Add", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
                groups.forEach { (group, cats) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h_" + group.name) {
                        Row(Modifier.padding(top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(
                                Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(3.dp)).background(group.tint),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(group.title, color = Palette.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    items(cats, key = { it.id }) { c ->
                        CategoryTile(c, c.id == selectedId, onClick = { onPick(c) })
                    }
                }
                if (groups.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "none") {
                        Text(
                            "No match for “$q”. Add it as your own category above.",
                            color = Palette.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "end") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
