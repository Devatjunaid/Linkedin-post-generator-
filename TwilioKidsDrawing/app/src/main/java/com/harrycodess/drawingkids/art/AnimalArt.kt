package com.harrycodess.drawingkids.art

import android.graphics.Matrix
import android.graphics.Region
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.harrycodess.drawingkids.ui.theme.Kid
import android.graphics.Path as APath

enum class Category(val label: String) {
    PETS("Pets"), FARM("Farm"), WILD("Wild"), OCEAN("Ocean"), BIRDS("Birds"), BUGS("Bugs"),
}

/** One piece of a coloring page, drawn in order like layers of paper. */
sealed class ArtPart(val path: APath) {
    val composePath = path.asComposePath()
}

/** A colorable area. Parts sharing a [group] are filled together. */
class FillPart(path: APath, val group: String, val outline: Float) : ArtPart(path)

/** Solid ink (eyes, noses). */
class InkPart(path: APath) : ArtPart(path)

/** Outline-only detail (mouths, whiskers). */
class LinePart(path: APath, val width: Float) : ArtPart(path)

/** Always-white detail (eye whites, sparkles). */
class WhitePart(path: APath, val outline: Float) : ArtPart(path)

class AnimalArt(val id: String, val name: String, val category: Category, val parts: List<ArtPart>) {

    private val hitAreas: List<Pair<FillPart, Region>> by lazy {
        val clip = Region(-HIT * 50, -HIT * 50, HIT * 260, HIT * 260)
        val m = Matrix().apply { setScale(HIT.toFloat(), HIT.toFloat()) }
        parts.filterIsInstance<FillPart>().map { part ->
            val scaled = APath(part.path).apply { transform(m) }
            part to Region().apply { setPath(scaled, clip) }
        }
    }

    /** The colorable group under a point (in art coordinates), honoring which part is on top. */
    fun groupAt(p: Offset): String? {
        val x = (p.x * HIT).toInt()
        val y = (p.y * HIT).toInt()
        for (i in hitAreas.indices.reversed()) {
            val (part, region) = hitAreas[i]
            if (region.contains(x, y)) return part.group
        }
        return null
    }

    companion object {
        private const val HIT = 4

        /** Every animal is authored in a 200x200 box; this frame leaves room for tails and feet. */
        val ORIGIN = Offset(-8f, -8f)
        val SIZE = Size(216f, 216f)
    }
}

class ArtBuilder {
    val parts = mutableListOf<ArtPart>()

    fun fill(group: String, vararg shapes: APath, outline: Float = 4f) {
        shapes.forEach { parts += FillPart(it, group, outline) }
    }

    fun ink(vararg shapes: APath) {
        shapes.forEach { parts += InkPart(it) }
    }

    fun line(vararg shapes: APath, width: Float = 4f) {
        shapes.forEach { parts += LinePart(it, width) }
    }

    fun white(vararg shapes: APath, outline: Float = 4f) {
        shapes.forEach { parts += WhitePart(it, outline) }
    }

    /** A shiny cartoon eye. */
    fun eye(x: Number, y: Number, r: Number = 7) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fr = r.toFloat()
        ink(circle(fx, fy, fr))
        white(circle(fx + fr * 0.35f, fy - fr * 0.35f, fr * 0.32f), outline = 0f)
    }
}

fun animal(id: String, name: String, category: Category, block: ArtBuilder.() -> Unit) =
    AnimalArt(id, name, category, ArtBuilder().apply(block).parts)

/**
 * Draws a coloring page in art coordinates.
 * With [overlayOnly], colorable areas punch holes so only the outlines remain - this layer sits above
 * the kid's crayon strokes so the lines always stay crisp.
 */
fun DrawScope.drawArt(art: AnimalArt, fills: Map<String, Color>, overlayOnly: Boolean = false) {
    for (part in art.parts) {
        when (part) {
            is FillPart -> {
                if (overlayOnly) {
                    drawPath(part.composePath, Color.Black, blendMode = BlendMode.Clear)
                } else {
                    drawPath(part.composePath, fills[part.group] ?: Color.White)
                }
                if (part.outline > 0f) drawPath(part.composePath, Kid.Ink, style = inkStroke(part.outline))
            }
            is WhitePart -> {
                drawPath(part.composePath, Color.White)
                if (part.outline > 0f) drawPath(part.composePath, Kid.Ink, style = inkStroke(part.outline))
            }
            is InkPart -> drawPath(part.composePath, Kid.Ink)
            is LinePart -> drawPath(part.composePath, Kid.Ink, style = inkStroke(part.width))
        }
    }
}

/** Draws the animal centered in the current draw area. */
fun DrawScope.drawArtFitted(art: AnimalArt, fills: Map<String, Color>, padding: Float = 0f) {
    val v = Viewport.fit(size, AnimalArt.ORIGIN, AnimalArt.SIZE, padding)
    inViewport(v) { drawArt(art, fills) }
}
