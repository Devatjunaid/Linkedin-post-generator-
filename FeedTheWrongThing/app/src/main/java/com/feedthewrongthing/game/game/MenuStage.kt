package com.feedthewrongthing.game.game

import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.models.ItemModels

/** A tiny scene used behind menus: creatures that get snacks tossed into their mouths. */
class MenuStage(private val fx: Fx) {
    private class Flyer(val item: ItemType, val sx: Float, val sy: Float, val sz: Float, val target: CreatureActor, val dur: Float) {
        var t = 0f
    }

    val actors = ArrayList<CreatureActor>()
    private val flyers = ArrayList<Flyer>()
    private val m = FloatArray(3)
    var time = 0f
        private set
    var onEat: ((CreatureActor, ItemType) -> Unit)? = null

    fun clear() {
        actors.clear()
        flyers.clear()
    }

    fun add(type: CreatureType, x: Float, y: Float, z: Float, scale: Float = 1f): CreatureActor {
        val a = CreatureActor(type, x, z, Move.STATIC, scale, sleeper = false, beat = false)
        a.baseY = y
        actors.add(a)
        return a
    }

    fun toss(target: CreatureActor, item: ItemType, fromX: Float, fromY: Float, fromZ: Float, dur: Float = 0.75f) {
        flyers.add(Flyer(item, fromX, fromY, fromZ, target, dur))
    }

    val busy: Boolean get() = flyers.isNotEmpty() || actors.any { it.reaction != null }

    fun update(dt: Float, lookX: Float, lookY: Float, lookZ: Float) {
        time += dt
        for (a in actors) {
            val f = flyers.firstOrNull { it.target === a }
            if (f != null) {
                f.target.mouthWorld(m)
                val (x, y, z) = pos(f)
                a.lookAtX = x; a.lookAtY = y; a.lookAtZ = z
                if (f.t / f.dur > 0.4f) a.anticipate = 1.3f
            } else {
                a.lookAtX = lookX; a.lookAtY = lookY; a.lookAtZ = lookZ
            }
            a.update(dt, time, fx)
        }
        val it = flyers.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.t += dt
            if (f.t >= f.dur) {
                it.remove()
                val r = Surprises.reaction(f.target.type, f.item)
                f.target.eat(f.item, r, fx)
                onEat?.invoke(f.target, f.item)
            }
        }
    }

    private fun pos(f: Flyer): Triple<Float, Float, Float> {
        f.target.mouthWorld(m)
        val k = (f.t / f.dur).coerceIn(0f, 1f)
        val x = f.sx + (m[0] - f.sx) * k
        val z = f.sz + (m[2] - f.sz) * k
        val y = f.sy + (m[1] - f.sy) * k + 2.4f * 4f * k * (1f - k)
        return Triple(x, y, z)
    }

    fun draw(ctx: DrawCtx, hatFor: (CreatureType) -> Hat, silhouette: (CreatureType) -> Boolean = { false }) {
        for (a in actors) a.draw(ctx, hatFor(a.type), silhouette(a.type))
        for (f in flyers) {
            val (x, y, z) = pos(f)
            ctx.push()
            ctx.translate(x, y, z)
            ctx.rotateX(f.t * 500f); ctx.rotateY(f.t * 300f)
            ItemModels.draw(ctx, f.item)
            ctx.pop()
        }
    }
}
