package com.sudzy.carwash.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import com.sudzy.carwash.engine.Gfx

/** Stickers kids can slap on their freshly washed car. */
object StickerArt {
    private val path = Path()

    fun draw(c: Canvas, kind: Int, x: Float, y: Float, r: Float, rot: Float) {
        if (r <= 0f) return
        c.save(); c.translate(x, y); c.rotate(rot)
        val sw = r * 0.16f
        when (kind) {
            0 -> { Gfx.starPath(0f, 0f, r, r * 0.45f, path); c.drawPath(path, Gfx.p(0xFFFFD23F.toInt())); c.drawPath(path, Gfx.s(Color.WHITE, sw)) }
            1 -> {
                path.reset(); path.moveTo(0f, r * 0.9f); path.cubicTo(-r * 1.4f, -r * 0.1f, -r * 0.6f, -r * 1.1f, 0f, -r * 0.4f); path.cubicTo(r * 0.6f, -r * 1.1f, r * 1.4f, -r * 0.1f, 0f, r * 0.9f); path.close()
                c.drawPath(path, Gfx.p(0xFFFF4F8A.toInt())); c.drawPath(path, Gfx.s(Color.WHITE, sw))
            }
            2 -> {
                path.reset(); path.moveTo(r * 0.2f, -r); path.lineTo(-r * 0.6f, r * 0.1f); path.lineTo(-r * 0.05f, r * 0.1f); path.lineTo(-r * 0.25f, r); path.lineTo(r * 0.6f, -r * 0.15f); path.lineTo(r * 0.05f, -r * 0.15f); path.close()
                c.drawPath(path, Gfx.p(0xFF5EF0FF.toInt())); c.drawPath(path, Gfx.s(Color.WHITE, sw))
            }
            3 -> {
                path.reset(); path.moveTo(0f, -r); path.quadTo(r * 0.9f, -r * 0.1f, r * 0.6f, r * 0.5f); path.quadTo(r * 0.3f, r, 0f, r); path.quadTo(-r * 0.6f, r, -r * 0.7f, r * 0.3f); path.quadTo(-r * 0.6f, -r * 0.2f, -r * 0.2f, -r * 0.3f); path.quadTo(-r * 0.1f, -r * 0.7f, 0f, -r); path.close()
                c.drawPath(path, Gfx.p(0xFFFF8A2B.toInt())); c.drawPath(path, Gfx.s(Color.WHITE, sw))
                c.drawCircle(0f, r * 0.45f, r * 0.35f, Gfx.p(0xFFFFE14D.toInt()))
            }
            else -> {
                c.drawCircle(0f, 0f, r, Gfx.p(0xFF7DFF5C.toInt())); c.drawCircle(0f, 0f, r, Gfx.s(Color.WHITE, sw))
                c.drawCircle(-r * 0.35f, -r * 0.2f, r * 0.13f, Gfx.p(0xFF1A3010.toInt())); c.drawCircle(r * 0.35f, -r * 0.2f, r * 0.13f, Gfx.p(0xFF1A3010.toInt()))
                c.drawArc(-r * 0.5f, -r * 0.3f, r * 0.5f, r * 0.6f, 20f, 140f, false, Gfx.s(0xFF1A3010.toInt(), r * 0.12f))
            }
        }
        c.restore()
    }
}
