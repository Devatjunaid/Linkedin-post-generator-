package com.feedthewrongthing.game.engine

import android.opengl.Matrix
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.tan

/**
 * Everything the GL thread needs to draw one frame. Filled on the main thread,
 * then handed to [Renderer3D] through a triple buffer.
 */
class RenderFrame(val capacity: Int = 3000) {
    var count = 0
    val mesh = IntArray(capacity)
    val mode = IntArray(capacity)
    val model = FloatArray(capacity * 16)
    val color = FloatArray(capacity * 4)
    val color2 = FloatArray(capacity * 4)
    val mat = FloatArray(capacity * 4)

    val view = FloatArray(16)
    val proj = FloatArray(16)
    val eye = FloatArray(3)
    val lightDir = floatArrayOf(-0.4f, 0.85f, 0.45f)
    val lightCol = floatArrayOf(1f, 0.96f, 0.88f)
    val skyCol = floatArrayOf(0.75f, 0.86f, 1f)
    val groundCol = floatArrayOf(0.55f, 0.48f, 0.42f)
    val fogCol = floatArrayOf(0.8f, 0.9f, 1f)
    val fog = floatArrayOf(30f, 80f)
    val skyTop = floatArrayOf(0.42f, 0.72f, 1f)
    val skyBottom = floatArrayOf(0.85f, 0.94f, 1f)
    var drawSky = true
    var clearAlpha = 1f

    fun reset() {
        count = 0
        drawSky = true
        clearAlpha = 1f
    }

    companion object {
        const val LIT = 0
        const val SHADOW = 1
        const val UNLIT = 2
        const val GLASS = 3
    }
}

/** Matrix stack plus helpers for pushing draw instances into a [RenderFrame]. */
class DrawCtx {
    lateinit var frame: RenderFrame
    private val stack = FloatArray(16 * 48)
    private var sp = 0
    private val tmp = FloatArray(16)
    private val tmp2 = FloatArray(16)

    /** Silhouette / tint override (used for locked creatures and freeze effects). */
    var tintColor = 0x000000
    var tintAmount = 0f
    var alphaMul = 1f
    var time = 0f

    fun begin(f: RenderFrame) {
        frame = f
        sp = 0
        Matrix.setIdentityM(stack, 0)
        tintAmount = 0f
        alphaMul = 1f
    }

    val top: FloatArray get() = stack
    val topOffset: Int get() = sp * 16

    fun push() {
        System.arraycopy(stack, sp * 16, stack, (sp + 1) * 16, 16)
        sp++
    }

    fun pop() { if (sp > 0) sp-- }

    fun identity() { Matrix.setIdentityM(stack, sp * 16) }

    fun translate(x: Float, y: Float, z: Float) { Matrix.translateM(stack, sp * 16, x, y, z) }

    fun rotateX(deg: Float) { if (deg != 0f) Matrix.rotateM(stack, sp * 16, deg, 1f, 0f, 0f) }
    fun rotateY(deg: Float) { if (deg != 0f) Matrix.rotateM(stack, sp * 16, deg, 0f, 1f, 0f) }
    fun rotateZ(deg: Float) { if (deg != 0f) Matrix.rotateM(stack, sp * 16, deg, 0f, 0f, 1f) }
    fun rotate(deg: Float, x: Float, y: Float, z: Float) { if (deg != 0f) Matrix.rotateM(stack, sp * 16, deg, x, y, z) }

    fun scale(x: Float, y: Float, z: Float) { Matrix.scaleM(stack, sp * 16, x, y, z) }
    fun scale(s: Float) { Matrix.scaleM(stack, sp * 16, s, s, s) }

    /** Transforms a local point by the current matrix. */
    fun toWorld(x: Float, y: Float, z: Float, out: FloatArray) {
        val o = sp * 16
        out[0] = stack[o] * x + stack[o + 4] * y + stack[o + 8] * z + stack[o + 12]
        out[1] = stack[o + 1] * x + stack[o + 5] * y + stack[o + 9] * z + stack[o + 13]
        out[2] = stack[o + 2] * x + stack[o + 6] * y + stack[o + 10] * z + stack[o + 14]
    }

    fun draw(
        mesh: Int, color: Int, alpha: Float = 1f, gloss: Float = 0.45f, rim: Float = 0.35f,
        emissive: Float = 0f, mode: Int = RenderFrame.LIT, pattern: Int = 0, color2: Int = 0xFFFFFF,
        patternScale: Float = 1f,
    ) {
        val f = frame
        if (f.count >= f.capacity) return
        val i = f.count++
        f.mesh[i] = mesh
        f.mode[i] = mode
        System.arraycopy(stack, sp * 16, f.model, i * 16, 16)
        var r = ((color shr 16) and 255) / 255f
        var g = ((color shr 8) and 255) / 255f
        var b = (color and 255) / 255f
        if (tintAmount > 0f && mode != RenderFrame.SHADOW) {
            val tr = ((tintColor shr 16) and 255) / 255f
            val tg = ((tintColor shr 8) and 255) / 255f
            val tb = (tintColor and 255) / 255f
            r += (tr - r) * tintAmount; g += (tg - g) * tintAmount; b += (tb - b) * tintAmount
        }
        val c = i * 4
        f.color[c] = r; f.color[c + 1] = g; f.color[c + 2] = b; f.color[c + 3] = alpha * alphaMul
        f.color2[c] = ((color2 shr 16) and 255) / 255f
        f.color2[c + 1] = ((color2 shr 8) and 255) / 255f
        f.color2[c + 2] = (color2 and 255) / 255f
        f.color2[c + 3] = patternScale
        f.mat[c] = gloss; f.mat[c + 1] = rim; f.mat[c + 2] = pattern.toFloat()
        f.mat[c + 3] = if (tintAmount > 0.9f) 0f else emissive
    }

    /** Draws [mesh] with a local transform relative to the current matrix. */
    fun part(
        mesh: Int, color: Int,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float = sx, sz: Float = sx,
        rx: Float = 0f, ry: Float = 0f, rz: Float = 0f,
        gloss: Float = 0.45f, rim: Float = 0.35f, emissive: Float = 0f, alpha: Float = 1f,
        mode: Int = RenderFrame.LIT, pattern: Int = 0, color2: Int = 0xFFFFFF, patternScale: Float = 1f,
    ) {
        push()
        translate(tx, ty, tz)
        rotateY(ry); rotateX(rx); rotateZ(rz)
        scale(sx, sy, sz)
        draw(mesh, color, alpha, gloss, rim, emissive, mode, pattern, color2, patternScale)
        pop()
    }

    /** Blob shadow on a horizontal surface, in world space. */
    fun shadow(x: Float, y: Float, z: Float, radius: Float, alpha: Float, color: Int = 0x2B1E3A) {
        if (alpha <= 0.01f) return
        push()
        identity()
        translate(x, y + 0.012f, z)
        scale(radius, 1f, radius)
        val keep = tintAmount
        tintAmount = 0f
        draw(Meshes.shadowQuad, color, alpha = alpha, mode = RenderFrame.SHADOW)
        tintAmount = keep
        pop()
    }

    fun copyTop(out: FloatArray) { System.arraycopy(stack, sp * 16, out, 0, 16) }

    fun multiply(m: FloatArray) {
        Matrix.multiplyMM(tmp, 0, stack, sp * 16, m, 0)
        System.arraycopy(tmp, 0, stack, sp * 16, 16)
    }

    @Suppress("unused")
    private fun unusedTmp() = tmp2
}

/** Perspective camera shared by the renderer (via RenderFrame) and touch picking. */
class Camera {
    val view = FloatArray(16)
    val proj = FloatArray(16)
    val vp = FloatArray(16)
    val eye = FloatArray(3)
    val target = FloatArray(3)
    var width = 1
    var height = 1
    var fovY = 45f
    private val v4 = FloatArray(4)
    private val r4 = FloatArray(4)
    private val inv = FloatArray(16)

    /**
     * Sets up the camera. [baseFovY] is the vertical FOV for a 16:9 screen; on narrower
     * screens the FOV widens so the same horizontal span stays visible.
     */
    fun setup(ex: Float, ey: Float, ez: Float, tx: Float, ty: Float, tz: Float, baseFovY: Float, w: Int, h: Int) {
        width = max(1, w); height = max(1, h)
        eye[0] = ex; eye[1] = ey; eye[2] = ez
        target[0] = tx; target[1] = ty; target[2] = tz
        val aspect = width.toFloat() / height
        val halfH = Math.toRadians(baseFovY / 2.0)
        val halfW = atan(tan(halfH) * (16.0 / 9.0))
        val needHalfV = atan(tan(halfW) / aspect)
        fovY = Math.toDegrees(max(halfH, needHalfV) * 2.0).toFloat().coerceAtMost(100f)
        Matrix.setLookAtM(view, 0, ex, ey, ez, tx, ty, tz, 0f, 1f, 0f)
        Matrix.perspectiveM(proj, 0, fovY, aspect, 0.3f, 220f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
    }

    /** Projects a world point to screen pixels. Returns false when behind the camera. */
    fun project(x: Float, y: Float, z: Float, out: FloatArray): Boolean {
        v4[0] = x; v4[1] = y; v4[2] = z; v4[3] = 1f
        Matrix.multiplyMV(r4, 0, vp, 0, v4, 0)
        if (r4[3] <= 0.0001f) return false
        val nx = r4[0] / r4[3]
        val ny = r4[1] / r4[3]
        out[0] = (nx * 0.5f + 0.5f) * width
        out[1] = (1f - (ny * 0.5f + 0.5f)) * height
        if (out.size > 2) out[2] = r4[3]
        return true
    }

    /** Screen-space size in pixels of a world-space length at distance [w] (from [project]). */
    fun pixelsPerUnit(w: Float): Float = (height / 2f) / (tan(Math.toRadians(fovY / 2.0)).toFloat() * w)

    /** Casts a ray from screen pixel and intersects plane y = [planeY]. */
    fun rayToPlane(sx: Float, sy: Float, planeY: Float, out: FloatArray): Boolean {
        if (!Matrix.invertM(inv, 0, vp, 0)) return false
        val nx = sx / width * 2f - 1f
        val ny = 1f - sy / height * 2f
        v4[0] = nx; v4[1] = ny; v4[2] = -1f; v4[3] = 1f
        Matrix.multiplyMV(r4, 0, inv, 0, v4, 0)
        val ax = r4[0] / r4[3]; val ay = r4[1] / r4[3]; val az = r4[2] / r4[3]
        v4[2] = 1f
        Matrix.multiplyMV(r4, 0, inv, 0, v4, 0)
        val bx = r4[0] / r4[3]; val by = r4[1] / r4[3]; val bz = r4[2] / r4[3]
        val dy = by - ay
        if (kotlin.math.abs(dy) < 1e-6f) return false
        val t = (planeY - ay) / dy
        if (t < 0f) return false
        out[0] = ax + (bx - ax) * t; out[1] = planeY; out[2] = az + (bz - az) * t
        return true
    }

    fun copyInto(f: RenderFrame) {
        System.arraycopy(view, 0, f.view, 0, 16)
        System.arraycopy(proj, 0, f.proj, 0, 16)
        f.eye[0] = eye[0]; f.eye[1] = eye[1]; f.eye[2] = eye[2]
    }
}
