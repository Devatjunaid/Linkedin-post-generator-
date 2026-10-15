package com.voltora.evcalculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voltora.evcalculator.ads.NativeAdCard
import com.voltora.evcalculator.data.Countries
import com.voltora.evcalculator.data.Country
import com.voltora.evcalculator.domain.Formatter
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Ev

@Composable
fun CountryPickerDialog(
    selected: Country,
    onPick: (Country) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        Countries.all.filter { it.name.contains(query.trim(), ignoreCase = true) || it.code.equals(query.trim(), true) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Ev.Paper),
        ) {
            Column(
                Modifier.fillMaxWidth().background(Ev.Ink).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BigText("Choose country", 30.sp, Ev.Paper)
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search ${Countries.all.size} countries") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    colors = darkFieldColors(),
                )
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(list, key = { it.code }) { c ->
                    val fmt = Formatter(c, c.useMiles)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(c) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(c.flag, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ev.Ink)
                            Text(
                                "Home ${fmt.rate(c.home)} · DC ${fmt.rate(c.dcFast)} /kWh",
                                fontFamily = Body, fontSize = 12.sp, color = Ev.Muted,
                            )
                        }
                        if (c.code == selected.code) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Ev.Ink)
                    }
                }
            }
        }
    }
}

@Composable
fun darkFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Ev.Ink2,
    unfocusedContainerColor = Ev.Ink2,
    focusedTextColor = Ev.Paper,
    unfocusedTextColor = Ev.Paper,
    focusedPlaceholderColor = Ev.MutedOnDark,
    unfocusedPlaceholderColor = Ev.MutedOnDark,
    focusedLeadingIconColor = Ev.MutedOnDark,
    unfocusedLeadingIconColor = Ev.MutedOnDark,
    focusedLabelColor = Ev.MutedOnDark,
    unfocusedLabelColor = Ev.MutedOnDark,
    cursorColor = Ev.Volt,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
fun lightFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Ev.Mist,
    unfocusedContainerColor = Ev.Mist,
    focusedTextColor = Ev.Ink,
    unfocusedTextColor = Ev.Ink,
    focusedLabelColor = Ev.Muted,
    unfocusedLabelColor = Ev.Muted,
    cursorColor = Ev.Ink,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

/** Shown when the user presses back on the Home screen. */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Ev.Paper),
        ) {
            Column(
                Modifier.fillMaxWidth().background(Ev.Ink).padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedLogo(size = 72.dp)
                BigText("LEAVING ALREADY?", 30.sp, Ev.Paper, weight = FontWeight.ExtraBold)
                Text(
                    "Your car and rates are saved. Come back before your next charge.",
                    color = Ev.MutedOnDark, fontFamily = Body, fontSize = 14.sp, textAlign = TextAlign.Center,
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NativeAdCard(showMedia = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoltButton("Exit", onExit, Modifier.weight(1f), dark = true)
                    VoltButton("Stay", onStay, Modifier.weight(1f))
                }
            }
        }
    }
}
