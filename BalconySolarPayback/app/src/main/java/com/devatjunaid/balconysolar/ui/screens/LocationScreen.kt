package com.devatjunaid.balconysolar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.devatjunaid.balconysolar.data.ClimateTables
import com.devatjunaid.balconysolar.data.Country
import com.devatjunaid.balconysolar.domain.CustomLocation
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.domain.SetupState
import com.devatjunaid.balconysolar.engine.SolarPosition
import com.devatjunaid.balconysolar.ui.components.AppIcons
import com.devatjunaid.balconysolar.ui.components.BorderedBox
import com.devatjunaid.balconysolar.ui.components.CardLabel
import com.devatjunaid.balconysolar.ui.components.Chip
import com.devatjunaid.balconysolar.ui.components.GhostButton
import com.devatjunaid.balconysolar.ui.components.InfoBanner
import com.devatjunaid.balconysolar.ui.components.PrimaryButton
import com.devatjunaid.balconysolar.ui.components.SectionCard
import com.devatjunaid.balconysolar.ui.graphics.AnimatedLogo
import com.devatjunaid.balconysolar.ui.graphics.GlobeGraphic
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import java.time.LocalDate
import java.time.ZoneId

sealed interface RefineStatus {
    data object Idle : RefineStatus
    data object Loading : RefineStatus
    data class Done(val message: String) : RefineStatus
    data class Failed(val message: String) : RefineStatus
}

@Composable
fun LocationScreen(
    state: SetupState,
    gpsBusy: Boolean,
    refine: RefineStatus,
    onState: (SetupState) -> Unit,
    onUseGps: () -> Unit,
    onRefine: () -> Unit,
    onNext: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var pickCountry by rememberSaveable { mutableStateOf(false) }
    var customDialog by rememberSaveable { mutableStateOf(false) }
    val country = state.country
    val noonJune = remember(state.latitude, state.longitude) {
        val date = if (state.southern) LocalDate.of(LocalDate.now().year, 12, 21) else LocalDate.of(LocalDate.now().year, 6, 21)
        SolarPosition.sunTimes(date, state.latitude, state.longitude, ZoneId.of(state.zoneId)).noonElevationDeg
    }

    ScreenColumn {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedLogo(34.dp, playIntro = false)
            Spacer(Modifier.width(10.dp))
            Text("Balcony Solar", style = Type.title.copy(fontSize = Type.bodyMedium.fontSize * 1.15f), color = Palette.Ink, modifier = Modifier.weight(1f))
            HeaderIcon(AppIcons.Folder, "Saved setups", onOpenSaved)
            HeaderIcon(AppIcons.Info, "About and privacy", onOpenAbout)
        }
        Text("STEP 1 / 5", style = Type.mono, color = Palette.Muted, modifier = Modifier.padding(top = 10.dp))
        Text("Where does your balcony see the sky?", style = Type.display, color = Palette.Ink, modifier = Modifier.padding(top = 4.dp))

        BorderedBox(Modifier.fillMaxWidth().height(196.dp)) {
            GlobeGraphic(state.latitude, state.longitude, Modifier.fillMaxWidth().height(196.dp))
            Row(Modifier.align(Alignment.BottomStart).padding(14.dp).fillMaxWidth()) {
                Text(Fmt.coordinate(state.latitude, state.longitude), style = Type.mono, color = Palette.Muted, modifier = Modifier.weight(1f))
                Text("${if (state.southern) "Dec" else "Jun"} noon ${Fmt.degrees(noonJune, 1)}", style = Type.mono, color = Palette.Amber)
            }
        }

        // Country selector.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) { pickCountry = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CodeBadge(country.code)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Country", style = Type.label, color = Palette.Muted)
                Text(country.name, style = Type.bodyMedium, color = Palette.Ink)
            }
            Icon(AppIcons.Search, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
        }

        // City chips plus device location / custom coordinates.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in country.cities) {
                Chip(c.name, selected = state.custom == null && c.name == state.city.name, onClick = { onState(state.withCity(c.name)) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(if (gpsBusy) "Locating…" else "Use my location", onClick = onUseGps, icon = AppIcons.Locate, modifier = Modifier.weight(1f))
            GhostButton("Coordinates", onClick = { customDialog = true }, icon = AppIcons.Pin, modifier = Modifier.weight(1f), color = Palette.Ink)
        }
        state.custom?.let { c ->
            val near = state.climateCity
            InfoBanner(
                "${c.label}: climate from ${near.city.name} (${Fmt.number(near.distanceKm)} km away)" +
                    if (near.distanceKm > 250) ". Refine with PVGIS for this exact spot." else ".",
                icon = AppIcons.Pin, color = if (near.distanceKm > 250) Palette.Amber else Palette.Mint,
            )
        }

        SectionCard {
            CardLabel("Climate & tariff · ${state.refinedClimate?.source ?: "built-in table"}", trailing = country.code)
            Spacer(Modifier.height(10.dp))
            val climate = state.site().climate
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(Fmt.number(climate.annualGhi()), "kWh/m² sun per year", Modifier.weight(1f))
                MiniStat(Fmt.unitPrice(state.finance.pricePerKwh, state.finance.currencyCode), "per kWh, your tariff", Modifier.weight(1f))
                MiniStat("${Fmt.number(country.inverterLimitW)} W", "typical plug-in limit", Modifier.weight(1f), Palette.Mint)
            }
            country.plugInNote?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = Type.label, color = Palette.Muted)
            }
            Spacer(Modifier.height(12.dp))
            when (refine) {
                RefineStatus.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Palette.Mint, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Fetching satellite data from PVGIS…", style = Type.label, color = Palette.Muted)
                }
                else -> {
                    GhostButton(
                        if (state.refinedClimate != null) "Satellite data loaded · refresh" else "Refine with PVGIS satellite data",
                        onClick = onRefine, icon = AppIcons.Satellite, modifier = Modifier.fillMaxWidth(),
                    )
                    (refine as? RefineStatus.Done)?.let { Text(it.message, style = Type.label, color = Palette.Mint, modifier = Modifier.padding(top = 6.dp)) }
                    (refine as? RefineStatus.Failed)?.let { Text(it.message, style = Type.label, color = Palette.Coral, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }

        PrimaryButton("Set up my panels", onClick = onNext, icon = AppIcons.ChevronRight)
    }

    if (pickCountry) CountryPicker(selected = country.code, onDismiss = { pickCountry = false }) {
        onState(state.withCountry(it))
        pickCountry = false
    }
    if (customDialog) CustomLocationDialog(state, onDismiss = { customDialog = false }) {
        onState(state.withCustom(it))
        customDialog = false
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = Palette.Ink)
    }
}

@Composable
fun CodeBadge(code: String) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Palette.SurfaceHigh).border(1.dp, Palette.LineStrong, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(code, style = Type.mono, color = Palette.Amber) }
}

@Composable
fun MiniStat(value: String, label: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Palette.Ink) {
    Column(modifier) {
        Text(value, style = Type.stat, color = color, maxLines = 1)
        Text(label, style = Type.label, color = Palette.Muted, maxLines = 2)
    }
}

@Composable
private fun CountryPicker(selected: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        val q = query.trim().lowercase()
        ClimateTables.countries.filter { c: Country ->
            q.isEmpty() || c.name.lowercase().contains(q) || c.code.lowercase() == q || c.cities.any { it.name.lowercase().contains(q) }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 560.dp).clip(RoundedCornerShape(24.dp)).background(Palette.SurfaceHigh).padding(16.dp),
        ) {
            Text("Choose your country", style = Type.title, color = Palette.Ink)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Search country or city", color = Palette.Faint) },
                leadingIcon = { Icon(AppIcons.Search, null, tint = Palette.Muted) },
                colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth()) {
                items(list, key = { it.code }) { c ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (c.code == selected) Palette.Surface else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable(role = Role.Button) { onPick(c.code) }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CodeBadge(c.code)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = Type.bodyMedium, color = Palette.Ink)
                            Text(c.cities.joinToString(" · ") { it.name }, style = Type.label, color = Palette.Muted, maxLines = 1)
                        }
                        if (c.code == selected) Icon(AppIcons.Check, null, tint = Palette.Amber)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomLocationDialog(state: SetupState, onDismiss: () -> Unit, onApply: (CustomLocation) -> Unit) {
    var lat by remember { mutableStateOf(state.latitude.toString()) }
    var lon by remember { mutableStateOf(state.longitude.toString()) }
    var name by remember { mutableStateOf(state.custom?.label ?: "My balcony") }
    val la = Fmt.parse(lat)
    val lo = Fmt.parse(lon)
    val valid = la != null && lo != null && la in -66.0..66.0 && lo in -180.0..180.0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = { Text("Exact coordinates", style = Type.title, color = Palette.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(lat, { lat = it }, label = { Text("Latitude (−66 to 66)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(lon, { lon = it }, label = { Text("Longitude (−180 to 180)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Text("Tip: long-press your building in a maps app to copy its coordinates. Climate comes from the nearest table city until you refine with PVGIS.", style = Type.label, color = Palette.Muted)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val near = ClimateTables.nearest(la!!, lo!!)
                onApply(CustomLocation(name.ifBlank { "My balcony" }, la, lo, near.city.zoneId))
            }) { Text("Use", color = if (valid) Palette.Amber else Palette.Faint) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Muted) } },
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Amber,
    unfocusedBorderColor = Palette.LineStrong,
    focusedTextColor = Palette.Ink,
    unfocusedTextColor = Palette.Ink,
    cursorColor = Palette.Amber,
    focusedLabelColor = Palette.Amber,
    unfocusedLabelColor = Palette.Muted,
)
