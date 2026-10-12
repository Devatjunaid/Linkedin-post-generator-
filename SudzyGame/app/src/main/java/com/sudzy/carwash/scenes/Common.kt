package com.sudzy.carwash.scenes

import android.graphics.Canvas
import com.sudzy.carwash.art.Icon
import com.sudzy.carwash.art.IconArt
import com.sudzy.carwash.engine.Button
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.Rng
import kotlin.math.PI

/** Shared sunburst + floating bubbles backdrop for menu screens. */
object MenuBg {
    fun draw(c: Canvas, W: Float, H: Float, time: Float, inner: Int = 0xFF7FE3FF.toInt(), mid: Int = 0xFF2AA8FF.toInt(), outer: Int = 0xFF1053E0.toInt(), cy: Float = H * 0.45f) {
        c.drawRect(0f, 0f, W, H, Gfx.radial(W / 2, cy, H * 0.75f, inner, outer))
        c.drawCircle(W / 2, cy, H * 0.3f, Gfx.radial(W / 2, cy, H * 0.3f, mid and 0x00FFFFFF or 0x55000000, 0x00000000))
        Gfx.rays(c, W / 2, cy, H * 1.2f, 22, (time * 0.08f) % (2 * PI.toFloat()), 0x1FFFFFFF)
        bubbles(c, W, H, time)
    }

    fun bubbles(c: Canvas, W: Float, H: Float, time: Float, n: Int = 22) {
        val rng = Rng(42)
        repeat(n) {
            val x = rng.range(0f, W); val sp = rng.range(40f, 110f); val r = rng.range(14f, 64f)
            val y = H + 80f - ((rng.range(0f, H + 160f) + time * sp) % (H + 160f))
            Gfx.bubble(c, x + kotlin.math.sin(time * 1.3f + it) * 24f, y, r, 0.9f)
        }
    }
}

fun backButton(onClick: () -> Unit) = Button(30f, 30f, 120f, 120f, 0xFF5AA8FF.toInt(), 0xFF2D6CF0.toInt(), round = true,
    art = { c, _, _ -> IconArt.draw(c, Icon.BACK, 0f, 0f, 64f) }, onClick = onClick)
