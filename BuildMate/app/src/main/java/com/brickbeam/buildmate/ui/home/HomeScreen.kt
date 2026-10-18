package com.brickbeam.buildmate.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.MasonryUnits
import com.brickbeam.buildmate.calc.PriceKey
import com.brickbeam.buildmate.calc.Regions
import com.brickbeam.buildmate.ui.LocalAppEnv
import com.brickbeam.buildmate.ui.Routes
import com.brickbeam.buildmate.ui.calc.rememberCalcContext
import com.brickbeam.buildmate.ui.components.BmCard
import com.brickbeam.buildmate.ui.components.Chip
import com.brickbeam.buildmate.ui.components.IconSquareButton
import com.brickbeam.buildmate.ui.components.Reveal
import com.brickbeam.buildmate.ui.components.SectionTitle
import com.brickbeam.buildmate.ui.graphics.AnimatedLogo
import com.brickbeam.buildmate.ui.graphics.BrickArt
import com.brickbeam.buildmate.ui.graphics.CementArt
import com.brickbeam.buildmate.ui.graphics.EstimateArt
import com.brickbeam.buildmate.ui.graphics.HeroScene
import com.brickbeam.buildmate.ui.graphics.PaintArt
import com.brickbeam.buildmate.ui.graphics.RulerArt
import com.brickbeam.buildmate.ui.graphics.SteelArt
import com.brickbeam.buildmate.ui.graphics.TileArt
import com.brickbeam.buildmate.ui.theme.BmIcons
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalMotion
import com.brickbeam.buildmate.ui.theme.LocalPalette
import java.text.DateFormat
import java.util.Date

private data class CalcTile(
    val title: String,
    val sub: String,
    val route: String,
    val keywords: String,
    val art: @Composable (Modifier) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    val ctx = rememberCalcContext()
    val region = ctx.region
    val fmt = ctx.fmt
    val projects by env.vm.projects.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    val tiles = listOf(
        CalcTile("Bricks", "Walls · blocks", Routes.BRICK, "brick block wall cmu mortar masonry veneer") { BrickArt(it) },
        CalcTile("Concrete", "Slab · footing", Routes.CONCRETE, "concrete cement slab footing column steps pad ready mix bag") { CementArt(it) },
        CalcTile("Steel", "Rebar · mesh", Routes.STEEL, "steel rebar mesh bar reinforcement") { SteelArt(it) },
        CalcTile("Tiles", "Floor · grout", Routes.TILES, "tile tiles floor wall grout adhesive bathroom kitchen") { TileArt(it) },
        CalcTile("Paint", "Coats · primer", Routes.PAINT, "paint wall ceiling primer room colour color") { PaintArt(it, p.mute) },
        CalcTile("Converter", "Units", Routes.CONVERTER, "convert converter units feet metre psi mpa gallon litre") { RulerArt(it, p.ink, p.accentText) },
    )
    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) tiles else tiles.filter { q in it.title.lowercase() || q.split(' ').all { w -> w in it.keywords } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedLogo(Modifier.size(44.dp), badge = p.surface2, roof = p.accent, animate = LocalMotion.current)
            Column(Modifier.weight(1f)) {
                Text("BuildMate", style = BmType.title, color = p.ink)
                Text("Material calculator", style = BmType.tiny, color = p.mute)
            }
            IconSquareButton(BmIcons.Ruler, "Unit converter", { env.go(Routes.CONVERTER) })
            IconSquareButton(BmIcons.Tune, "Settings", { env.go(Routes.SETTINGS) })
        }

        // Hero card with search.
        Reveal(0) {
            BmCard(Modifier.fillMaxWidth(), radius = 26.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("NEW PROJECT", style = BmType.tiny.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = p.mute)
                        Text("What are you\nbuilding today?", style = BmType.display, color = p.ink)
                    }
                    Text(
                        "Offline · no account", style = BmType.tiny.copy(fontWeight = FontWeight.Bold), color = p.accentText,
                        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(p.glow).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                HeroScene(Modifier.fillMaxWidth().height(92.dp).padding(top = 4.dp), ground = p.surface2, ink = p.ink)
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.surface2)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(BmIcons.Search, null, tint = p.mute, modifier = Modifier.size(20.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Wall, slab, bathroom floor…", style = BmType.body, color = p.mute)
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = BmType.body.copy(color = p.ink), cursorBrush = SolidColor(p.accentText),
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search calculators" },
                        )
                    }
                }
            }
        }

        // Region.
        Reveal(1) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Where are you building?", trailing = "All regions", onTrailing = { env.go(Routes.REGION) })
                val quick = (listOf(region) + listOf("US-TX", "US-CA", "US-NY", "US-FL", "EU-DE", "EU-GB", "EU-FR")
                    .mapNotNull { Regions.byCode(it) }).distinctBy { it.code }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { r -> Chip(r.name, r.code == region.code, { env.vm.setRegion(r.code) }, leading = r.shortCode) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoTile("Units", if (fmt.imperial) "ft · in · yd³" else "m · mm · m³", Modifier.weight(1f))
                    InfoTile("Std. unit", MasonryUnits.byId(region.defaultBrickId)?.name?.substringAfter(' ')?.take(18) ?: "—", Modifier.weight(1f))
                    InfoTile(region.taxLabel, "${fmt.num(region.taxRatePct, 3)}%${if (region.taxIncludedInPrices) " incl." else " + local"}", Modifier.weight(1f))
                }
            }
        }

        // Calculators.
        Reveal(2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Calculators", trailing = "Projects", onTrailing = { env.go(Routes.PROJECTS) })
                shown.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t -> CalcCard(t, Modifier.weight(1f)) { env.go(t.route) } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (shown.isEmpty()) Text("No calculator matches “$query”.", style = BmType.label, color = p.mute)
            }
        }

        NativeAdSlot("home")

        // Recent projects.
        Reveal(3) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Recent projects", trailing = "See all", onTrailing = { env.go(Routes.PROJECTS) })
                if (projects.isEmpty()) {
                    BmCard(Modifier.fillMaxWidth().clickable(role = Role.Button) { env.go(Routes.PROJECTS) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            EstimateArt(Modifier.size(56.dp, 40.dp), p.accentText)
                            Column(Modifier.weight(1f)) {
                                Text("Start an estimate", style = BmType.section, color = p.ink)
                                Text("Add results from any calculator; tax and totals are worked out for you.", style = BmType.tiny, color = p.mute)
                            }
                        }
                    }
                } else {
                    projects.take(3).forEach { pr ->
                        val pRegion = Regions.byCode(pr.regionCode) ?: region
                        val pf = com.brickbeam.buildmate.ui.components.Fmt(ctx.system, pRegion)
                        BmCard(Modifier.fillMaxWidth().clickable(role = Role.Button) { env.go(Routes.estimate(pr.id)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(pr.name, style = BmType.section, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${pRegion.name} · ${pr.itemCount} items · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(pr.updatedAt))}",
                                        style = BmType.tiny, color = p.mute,
                                    )
                                }
                                Text(pf.money(pr.materials), style = BmType.figure, color = p.ink)
                                Icon(BmIcons.Chevron, null, tint = p.mute)
                            }
                        }
                    }
                }
            }
        }

        // Price ticker.
        BmCard(Modifier.fillMaxWidth().clickable(role = Role.Button) { env.go(Routes.PRICES) }, padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Row(Modifier.height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    Text("Your prices", style = BmType.tiny, color = p.mute)
                    Text("${region.shortCode} · tap to edit", style = BmType.label.copy(fontWeight = FontWeight.Bold), color = p.ink)
                }
                Box(Modifier.size(1.dp, 38.dp).background(p.line))
                val keys = listOf(PriceKey.BRICK, PriceKey.CEMENT_BAG, PriceKey.READYMIX, PriceKey.REBAR, PriceKey.TILE, PriceKey.PAINT_M)
                Row(
                    Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE).padding(start = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    keys.forEach { k ->
                        Column {
                            Text("${k.label} / ${k.unit(region.market)}", style = BmType.tiny, color = p.mute, maxLines = 1)
                            Text(fmt.moneyPrecise(ctx.price(k)), style = BmType.figure.copy(fontSize = 13.sp), color = p.ink, maxLines = 1)
                        }
                    }
                }
            }
        }
        Text(
            "Prices are editable sample values, not live quotes. Always confirm with your supplier.",
            style = BmType.tiny, color = p.mute,
        )
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        Text(label, style = BmType.tiny.copy(fontSize = 10.sp), color = p.mute, maxLines = 1)
        Text(value, style = BmType.tiny.copy(fontWeight = FontWeight.Bold), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CalcCard(t: CalcTile, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier
            .height(116.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClickLabel = "Open ${t.title}", onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        t.art(Modifier.size(64.dp, 44.dp))
        Column {
            Text(t.title, style = BmType.section, color = p.ink, maxLines = 1)
            Text(t.sub, style = BmType.tiny.copy(fontSize = 10.5.sp), color = p.mute, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
