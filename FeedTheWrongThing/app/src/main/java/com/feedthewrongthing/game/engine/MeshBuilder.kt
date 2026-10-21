package com.feedthewrongthing.game.engine

import android.opengl.Matrix
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Interleaved vertex data: position(3) normal(3) uv(2) color(3). */
class MeshData(val vertices: FloatArray, val indices: ShortArray) {
    companion object {
        const val STRIDE = 11
    }
}

/** Grows vertex/index arrays while procedural shapes are generated. */
class MeshBuilder {
    private var v = FloatArray(MeshData.STRIDE * 1024)
    private var vc = 0
    private var ix = ShortArray(6 * 1024)
    private var ic = 0
    private var cr = 1f
    private var cg = 1f
    private var cb = 1f

    val vertexCount: Int get() = vc / MeshData.STRIDE

    fun color(r: Float, g: Float, b: Float): MeshBuilder {
        cr = r; cg = g; cb = b
        return this
    }

    fun color(rgb: Int): MeshBuilder =
        color(((rgb shr 16) and 255) / 255f, ((rgb shr 8) and 255) / 255f, (rgb and 255) / 255f)

    fun vertex(px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float, u: Float, w: Float): Int {
        if (vc + MeshData.STRIDE > v.size) v = v.copyOf(v.size * 2)
        v[vc++] = px; v[vc++] = py; v[vc++] = pz
        v[vc++] = nx; v[vc++] = ny; v[vc++] = nz
        v[vc++] = u; v[vc++] = w
        v[vc++] = cr; v[vc++] = cg; v[vc++] = cb
        return vc / MeshData.STRIDE - 1
    }

    fun tri(a: Int, b: Int, c: Int) {
        if (ic + 3 > ix.size) ix = ix.copyOf(ix.size * 2)
        ix[ic++] = a.toShort(); ix[ic++] = b.toShort(); ix[ic++] = c.toShort()
    }

    /** Appends [other] transformed by [m] (column-major 4x4). Normals use the inverse-transpose. */
    fun append(other: MeshData, m: FloatArray) {
        val inv = FloatArray(16)
        val nm = FloatArray(16)
        if (!Matrix.invertM(inv, 0, m, 0)) Matrix.setIdentityM(inv, 0)
        Matrix.transposeM(nm, 0, inv, 0)
        val base = vertexCount
        val s = other.vertices
        var i = 0
        val st = MeshData.STRIDE
        while (i < s.size) {
            val x = s[i]; val y = s[i + 1]; val z = s[i + 2]
            val nx = s[i + 3]; val ny = s[i + 4]; val nz = s[i + 5]
            val px = m[0] * x + m[4] * y + m[8] * z + m[12]
            val py = m[1] * x + m[5] * y + m[9] * z + m[13]
            val pz = m[2] * x + m[6] * y + m[10] * z + m[14]
            var qx = nm[0] * nx + nm[4] * ny + nm[8] * nz
            var qy = nm[1] * nx + nm[5] * ny + nm[9] * nz
            var qz = nm[2] * nx + nm[6] * ny + nm[10] * nz
            val l = sqrt(qx * qx + qy * qy + qz * qz)
            if (l > 1e-8f) { qx /= l; qy /= l; qz /= l }
            if (vc + st > v.size) v = v.copyOf(max(v.size * 2, vc + st))
            v[vc++] = px; v[vc++] = py; v[vc++] = pz
            v[vc++] = qx; v[vc++] = qy; v[vc++] = qz
            v[vc++] = s[i + 6]; v[vc++] = s[i + 7]
            v[vc++] = s[i + 8]; v[vc++] = s[i + 9]; v[vc++] = s[i + 10]
            i += st
        }
        for (k in other.indices.indices) {
            if (ic + 1 > ix.size) ix = ix.copyOf(ix.size * 2)
            ix[ic++] = (base + (other.indices[k].toInt() and 0xFFFF)).toShort()
        }
    }

    fun build(): MeshData = MeshData(v.copyOf(vc), ix.copyOf(ic))
}

/** Procedural primitive generators. Every shape is roughly unit sized and centred on the origin. */
object Shapes {
    fun interface Param { fun eval(u: Float, v: Float, out: FloatArray) }
    fun interface Center { fun eval(p: FloatArray, u: Float, v: Float, out: FloatArray) }

    private const val TAU = (PI * 2).toFloat()
    private const val PIF = PI.toFloat()

    val AXIS = Center { p, _, _, out -> out[0] = 0f; out[1] = p[1]; out[2] = 0f }
    val ORIGIN = Center { _, _, _, out -> out[0] = 0f; out[1] = 0f; out[2] = 0f }

    /**
     * Generic parametric surface. u runs around (0..1), v runs bottom to top (0..1).
     * Normals are found numerically and flipped to point away from [center].
     */
    fun surface(b: MeshBuilder, us: Int, vs: Int, f: Param, center: Center = AXIS) {
        val base = b.vertexCount
        val p = FloatArray(3)
        val a = FloatArray(3)
        val c = FloatArray(3)
        val cc = FloatArray(3)
        val h = 1e-3f
        for (j in 0..vs) {
            val vv = j.toFloat() / vs
            for (i in 0..us) {
                val uu = i.toFloat() / us
                f.eval(uu, vv, p)
                f.eval(uu + h, vv, a); f.eval(uu - h, vv, c)
                val dux = a[0] - c[0]; val duy = a[1] - c[1]; val duz = a[2] - c[2]
                f.eval(uu, min(1f, vv + h), a); f.eval(uu, max(0f, vv - h), c)
                val dvx = a[0] - c[0]; val dvy = a[1] - c[1]; val dvz = a[2] - c[2]
                var nx = dvy * duz - dvz * duy
                var ny = dvz * dux - dvx * duz
                var nz = dvx * duy - dvy * dux
                var l = sqrt(nx * nx + ny * ny + nz * nz)
                if (l < 1e-10f) {
                    center.eval(p, uu, vv, cc)
                    nx = p[0] - cc[0]; ny = p[1] - cc[1]; nz = p[2] - cc[2]
                    l = sqrt(nx * nx + ny * ny + nz * nz)
                    if (l < 1e-6f) { nx = 0f; ny = if (vv < 0.5f) -1f else 1f; nz = 0f; l = 1f }
                }
                nx /= l; ny /= l; nz /= l
                center.eval(p, uu, vv, cc)
                val d = nx * (p[0] - cc[0]) + ny * (p[1] - cc[1]) + nz * (p[2] - cc[2])
                if (d < 0f) { nx = -nx; ny = -ny; nz = -nz }
                if (abs(d) < 1e-7f && (vv == 0f || vv == 1f)) { nx = 0f; ny = if (vv < 0.5f) -1f else 1f; nz = 0f }
                b.vertex(p[0], p[1], p[2], nx, ny, nz, uu, vv)
            }
        }
        val row = us + 1
        for (j in 0 until vs) for (i in 0 until us) {
            val i0 = base + j * row + i
            val i1 = i0 + 1
            val i2 = i0 + row + 1
            val i3 = i0 + row
            b.tri(i0, i2, i1)
            b.tri(i0, i3, i2)
        }
    }

    fun sphere(b: MeshBuilder, us: Int = 32, vs: Int = 20) = surface(b, us, vs, { u, v, o ->
        val t = u * TAU; val ph = v * PIF
        val r = sin(ph)
        o[0] = r * cos(t); o[1] = -cos(ph); o[2] = r * sin(t)
    })

    /** Egg: wider at the bottom. */
    fun egg(b: MeshBuilder, bulge: Float = 0.14f, us: Int = 32, vs: Int = 22) = surface(b, us, vs, { u, v, o ->
        val t = u * TAU; val ph = v * PIF
        val r = sin(ph) * (1f + bulge * cos(ph))
        o[0] = r * cos(t); o[1] = -cos(ph); o[2] = r * sin(t)
    })

    /** Pear: narrow top, round bottom. */
    fun pear(b: MeshBuilder, us: Int = 32, vs: Int = 24) = surface(b, us, vs, { u, v, o ->
        val t = u * TAU; val ph = v * PIF
        val r = sin(ph) * (1.05f - 0.42f * v)
        o[0] = r * cos(t); o[1] = -cos(ph) * 1.0f; o[2] = r * sin(t)
    })

    /** Bumpy sphere for fuzzy creatures and broccoli. */
    fun fuzzy(b: MeshBuilder, amp: Float, fu: Float, fv: Float, us: Int = 48, vs: Int = 32) = surface(b, us, vs, { u, v, o ->
        val t = u * TAU; val ph = v * PIF
        val bump = 1f + amp * sin(t * fu) * sin(ph * fv)
        val r = sin(ph) * bump
        o[0] = r * cos(t); o[1] = -cos(ph) * bump; o[2] = r * sin(t)
    }, ORIGIN)

    /** Rounded drop pointing up (flames, leaves, wings). */
    fun teardrop(b: MeshBuilder, us: Int = 24, vs: Int = 18) = surface(b, us, vs, { u, v, o ->
        val t = u * TAU; val ph = v * PIF
        val r = sin(ph) * (1f - 0.78f * v)
        o[0] = r * cos(t); o[1] = -cos(ph) + 0.7f * v * v; o[2] = r * sin(t)
    })

    /** Capsule with total height 2 + 2*halfLen and radius 1. */
    fun capsule(b: MeshBuilder, halfLen: Float, us: Int = 24, vs: Int = 20) = surface(b, us, vs, { u, v, o ->
        val t = u * TAU
        val r: Float
        val y: Float
        when {
            v < 0.4f -> { val ph = (v / 0.4f) * PIF * 0.5f; r = sin(ph); y = -cos(ph) - halfLen }
            v > 0.6f -> { val ph = PIF * 0.5f + ((v - 0.6f) / 0.4f) * PIF * 0.5f; r = sin(ph); y = -cos(ph) + halfLen }
            else -> { r = 1f; y = -halfLen + (v - 0.4f) / 0.2f * 2f * halfLen }
        }
        o[0] = r * cos(t); o[1] = y; o[2] = r * sin(t)
    })

    /** Torus arc in the XZ plane. With [taper] the tube closes at both ends (banana, chili). */
    fun torusArc(b: MeshBuilder, ringR: Float, tubeR: Float, arc: Float, taper: Boolean, us: Int = 36, vs: Int = 16) {
        surface(b, us, vs, { u, v, o ->
            val cu = u.coerceIn(0f, 1f)
            val ph = -arc * 0.5f + arc * cu
            val th = v * TAU
            val rr = if (taper) tubeR * sin(PIF * cu).coerceAtLeast(0f).pow(0.55f) else tubeR
            val k = ringR + rr * cos(th)
            o[0] = k * cos(ph); o[1] = rr * sin(th); o[2] = k * sin(ph)
        }, { _, u, _, out ->
            val ph = -arc * 0.5f + arc * u.coerceIn(0f, 1f)
            out[0] = ringR * cos(ph); out[1] = 0f; out[2] = ringR * sin(ph)
        })
    }

    /** Puffy five-pointed star in the XY plane, facing +Z. */
    fun puffyStar(b: MeshBuilder, thickness: Float = 0.42f, points: Int = 5) = surface(b, 80, 14, { u, v, o ->
        val t = u * TAU + PIF * 0.5f
        val k = ((cos(points * (t - PIF * 0.5f)) + 1f) * 0.5f)
        val rr = 0.48f + 0.52f * k * k
        val s = sin(v * PIF)
        o[0] = rr * s * cos(t); o[1] = rr * s * sin(t); o[2] = -thickness * cos(v * PIF)
    }, ORIGIN)

    /** Puffy heart in the XY plane, facing +Z. */
    fun puffyHeart(b: MeshBuilder, thickness: Float = 0.4f) = surface(b, 72, 14, { u, v, o ->
        val t = u * TAU
        val st = sin(t)
        val hx = 16f * st * st * st / 17f
        val hy = (13f * cos(t) - 5f * cos(2 * t) - 2f * cos(3 * t) - cos(4 * t)) / 17f + 0.15f
        val s = sin(v * PIF)
        o[0] = hx * s; o[1] = hy * s; o[2] = -thickness * cos(v * PIF)
    }, ORIGIN)

    /** Lathe from a radius profile r(v) with y(v); open ends allowed. */
    fun lathe(b: MeshBuilder, us: Int, vs: Int, r: (Float) -> Float, y: (Float) -> Float) = surface(b, us, vs, { u, v, o ->
        val t = u * TAU
        val rr = r(v)
        o[0] = rr * cos(t); o[1] = y(v); o[2] = rr * sin(t)
    })

    /** Frustum/cylinder centred on origin, height h, with optional caps. */
    fun cylinder(b: MeshBuilder, rBottom: Float, rTop: Float, h: Float, us: Int = 28, caps: Boolean = true) {
        val y0 = -h / 2f
        val y1 = h / 2f
        val slope = (rBottom - rTop) / h
        val base = b.vertexCount
        for (i in 0..us) {
            val u = i.toFloat() / us
            val t = u * TAU
            val c = cos(t); val s = sin(t)
            val l = sqrt(1f + slope * slope)
            b.vertex(c * rBottom, y0, s * rBottom, c / l, slope / l, s / l, u, 0f)
            b.vertex(c * rTop, y1, s * rTop, c / l, slope / l, s / l, u, 1f)
        }
        for (i in 0 until us) {
            val a = base + i * 2
            b.tri(a, a + 1, a + 3)
            b.tri(a, a + 3, a + 2)
        }
        if (caps) {
            if (rBottom > 0f) disc(b, rBottom, y0, -1f, us)
            if (rTop > 0f) disc(b, rTop, y1, 1f, us)
        }
    }

    private fun disc(b: MeshBuilder, r: Float, y: Float, ny: Float, us: Int) {
        val c = b.vertex(0f, y, 0f, 0f, ny, 0f, 0.5f, 0.5f)
        val first = b.vertexCount
        for (i in 0..us) {
            val t = i.toFloat() / us * TAU
            b.vertex(cos(t) * r, y, sin(t) * r, 0f, ny, 0f, 0.5f + 0.5f * cos(t), 0.5f + 0.5f * sin(t))
        }
        for (i in 0 until us) b.tri(c, first + i, first + i + 1)
    }

    /** Box with rounded edges, half extents h*, edge radius r. */
    fun roundBox(b: MeshBuilder, hx: Float, hy: Float, hz: Float, r: Float, edgeSteps: Int = 4) {
        val h = floatArrayOf(hx, hy, hz)
        val q = FloatArray(3)
        for (axis in 0..2) for (sg in intArrayOf(-1, 1)) {
            val a1 = (axis + 1) % 3
            val a2 = (axis + 2) % 3
            val s1 = steps(h[a1], r, edgeSteps)
            val s2 = steps(h[a2], r, edgeSteps)
            val base = b.vertexCount
            for (j in s2.indices) for (i in s1.indices) {
                q[axis] = sg * h[axis]; q[a1] = s1[i]; q[a2] = s2[j]
                var dx = 0f; var dy = 0f; var dz = 0f
                val inner = FloatArray(3)
                for (k in 0..2) {
                    val lim = max(0f, h[k] - r)
                    inner[k] = q[k].coerceIn(-lim, lim)
                }
                dx = q[0] - inner[0]; dy = q[1] - inner[1]; dz = q[2] - inner[2]
                var l = sqrt(dx * dx + dy * dy + dz * dz)
                if (l < 1e-6f) {
                    dx = 0f; dy = 0f; dz = 0f
                    when (axis) { 0 -> dx = sg.toFloat(); 1 -> dy = sg.toFloat(); else -> dz = sg.toFloat() }
                    l = 1f
                }
                dx /= l; dy /= l; dz /= l
                b.vertex(inner[0] + dx * r, inner[1] + dy * r, inner[2] + dz * r, dx, dy, dz,
                    i.toFloat() / (s1.size - 1), j.toFloat() / (s2.size - 1))
            }
            val row = s1.size
            for (j in 0 until s2.size - 1) for (i in 0 until s1.size - 1) {
                val i0 = base + j * row + i
                b.tri(i0, i0 + 1, i0 + row + 1)
                b.tri(i0, i0 + row + 1, i0 + row)
            }
        }
    }

    private fun steps(h: Float, r: Float, n: Int): FloatArray {
        val inner = max(0f, h - r)
        val out = ArrayList<Float>()
        for (i in 0..n) out.add(-h + (h - inner) * i / n)
        out.add(0f)
        for (i in 0..n) out.add(inner + (h - inner) * i / n)
        return out.distinct().sorted().toFloatArray()
    }

    /** Flat quad in XZ, size 2x2, normal +Y. */
    fun quadXZ(b: MeshBuilder) {
        val a = b.vertex(-1f, 0f, -1f, 0f, 1f, 0f, 0f, 0f)
        val c = b.vertex(1f, 0f, -1f, 0f, 1f, 0f, 1f, 0f)
        val d = b.vertex(1f, 0f, 1f, 0f, 1f, 0f, 1f, 1f)
        val e = b.vertex(-1f, 0f, 1f, 0f, 1f, 0f, 0f, 1f)
        b.tri(a, d, c); b.tri(a, e, d)
    }

    /** Subdivided ground plane (size 2x2) so fog and light vary smoothly. */
    fun grid(b: MeshBuilder, n: Int) {
        val base = b.vertexCount
        for (j in 0..n) for (i in 0..n) {
            val u = i.toFloat() / n; val w = j.toFloat() / n
            b.vertex(u * 2f - 1f, 0f, w * 2f - 1f, 0f, 1f, 0f, u, w)
        }
        val row = n + 1
        for (j in 0 until n) for (i in 0 until n) {
            val i0 = base + j * row + i
            b.tri(i0, i0 + row + 1, i0 + 1)
            b.tri(i0, i0 + row, i0 + row + 1)
        }
    }

    /** Full-screen triangle for the sky gradient. uv.y holds 0 (bottom) .. 1 (top). */
    fun skyTriangle(b: MeshBuilder) {
        val a = b.vertex(-1f, -1f, 0f, 0f, 0f, 1f, 0f, 0f)
        val c = b.vertex(3f, -1f, 0f, 0f, 0f, 1f, 2f, 0f)
        val d = b.vertex(-1f, 3f, 0f, 0f, 0f, 1f, 0f, 2f)
        b.tri(a, c, d)
    }

    /** Hard edged box (used for confetti and thin bars). */
    fun box(b: MeshBuilder, hx: Float, hy: Float, hz: Float) = roundBox(b, hx, hy, hz, 0.0001f, 1)
}

/** Bakes several coloured primitives into one static mesh to keep draw calls low. */
class Bake {
    val builder = MeshBuilder()
    private val m = FloatArray(16)
    private val tmp = FloatArray(16)

    private fun xf(tx: Float, ty: Float, tz: Float, sx: Float, sy: Float, sz: Float, rx: Float, ry: Float, rz: Float): FloatArray {
        Matrix.setIdentityM(m, 0)
        Matrix.translateM(m, 0, tx, ty, tz)
        if (ry != 0f) { Matrix.rotateM(m, 0, ry, 0f, 1f, 0f) }
        if (rx != 0f) { Matrix.rotateM(m, 0, rx, 1f, 0f, 0f) }
        if (rz != 0f) { Matrix.rotateM(m, 0, rz, 0f, 0f, 1f) }
        Matrix.scaleM(m, 0, sx, sy, sz)
        System.arraycopy(m, 0, tmp, 0, 16)
        return tmp
    }

    fun add(shape: MeshData, color: Int, tx: Float = 0f, ty: Float = 0f, tz: Float = 0f,
            sx: Float = 1f, sy: Float = sx, sz: Float = sx, rx: Float = 0f, ry: Float = 0f, rz: Float = 0f): Bake {
        val tinted = tint(shape, color)
        builder.append(tinted, xf(tx, ty, tz, sx, sy, sz, rx, ry, rz))
        return this
    }

    fun build(): MeshData = builder.build()

    companion object {
        private val cache = HashMap<Long, MeshData>()

        /** Returns a copy of [shape] with every vertex colour set to [color]. */
        fun tint(shape: MeshData, color: Int): MeshData {
            val key = (System.identityHashCode(shape).toLong() shl 32) or (color.toLong() and 0xFFFFFFFFL)
            return cache.getOrPut(key) {
                val v = shape.vertices.copyOf()
                val r = ((color shr 16) and 255) / 255f
                val g = ((color shr 8) and 255) / 255f
                val bl = (color and 255) / 255f
                var i = 0
                while (i < v.size) {
                    v[i + 8] = r; v[i + 9] = g; v[i + 10] = bl
                    i += MeshData.STRIDE
                }
                MeshData(v, shape.indices)
            }
        }

        fun clearCache() = cache.clear()
    }
}

/** Convenience to build a single primitive mesh. */
inline fun mesh(block: (MeshBuilder) -> Unit): MeshData {
    val b = MeshBuilder()
    block(b)
    return b.build()
}
