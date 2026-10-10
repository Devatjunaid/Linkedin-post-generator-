package com.harrycodess.drawingkids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.harrycodess.drawingkids.art.AnimalArt
import com.harrycodess.drawingkids.art.Animals
import com.harrycodess.drawingkids.art.drawArtFitted
import com.harrycodess.drawingkids.ui.theme.Kid

@Composable
fun ArtThumb(art: AnimalArt, fills: Map<String, Color>, modifier: Modifier) {
    Canvas(modifier) { drawArtFitted(art, fills) }
}

/** Pre-colored versions of a few animals, used to decorate the home and splash screens. */
object Showcase {
    private val looks: Map<String, Map<String, Color>> = mapOf(
        "cat" to mapOf("ears" to Kid.Orange, "innerEars" to Kid.Blush, "head" to Kid.Yellow, "muzzle" to Color.White),
        "fish" to mapOf("body" to Kid.Blue, "fins" to Kid.Pink),
        "turtle" to mapOf("skin" to Kid.Lime, "shell" to Kid.Green, "pattern" to Kid.Yellow, "rim" to Kid.Teal),
        "butterfly" to mapOf("topWings" to Kid.Pink, "bottomWings" to Kid.Purple, "dots" to Kid.Yellow, "body" to Kid.Ink),
        "owl" to mapOf("body" to Kid.Brown, "wings" to Color(0xFF6B4226), "belly" to Kid.Sand, "eyeRings" to Kid.Yellow, "beak" to Kid.Orange),
        "lion" to mapOf("mane" to Kid.Orange, "ears" to Kid.Yellow, "face" to Kid.Yellow, "muzzle" to Kid.Sand),
        "pig" to mapOf("ears" to Kid.Pink, "head" to Kid.Blush, "snout" to Kid.Pink, "cheeks" to Kid.Pink),
        "whale" to mapOf("body" to Kid.Royal, "belly" to Color(0xFFBFE0FF), "cheeks" to Kid.Blush),
        "frog" to mapOf("body" to Kid.Green, "belly" to Kid.Lime, "cheeks" to Kid.Pink),
        "bee" to mapOf("body" to Kid.Yellow, "stripes" to Kid.Ink, "wings" to Color(0xFFBFE0FF), "head" to Kid.Yellow),
        "chick" to mapOf("body" to Kid.Yellow, "wings" to Kid.Gold, "beak" to Kid.Orange, "cheeks" to Kid.Pink),
        "octopus" to mapOf("body" to Kid.Purple, "spots" to Kid.Pink, "cheeks" to Kid.Pink),
    )

    fun fills(id: String) = looks[id] ?: emptyMap()
    fun art(id: String) = Animals.byId(id)!!
}
