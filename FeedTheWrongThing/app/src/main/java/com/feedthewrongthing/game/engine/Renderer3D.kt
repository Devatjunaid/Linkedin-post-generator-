package com.feedthewrongthing.game.engine

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

/** Offscreen render request used to make item / creature icons for the 2D UI. */
class ThumbJob(val key: String, val frame: RenderFrame, val size: Int)

/**
 * OpenGL ES 3 renderer. Only the GL thread touches GL state; frames arrive from the
 * main thread through [submit].
 */
class Renderer3D(private val meshes: List<MeshData>) : GLSurfaceView.Renderer {
    private val lock = Any()
    private var pending = RenderFrame()
    private var reading = RenderFrame()
    private var hasNew = false

    private var program = 0
    private var vaos = IntArray(0)
    private var counts = IntArray(0)
    private var skyVao = 0
    private var skyCount = 0
    private var width = 1
    private var height = 1
    private var lastVao = -1

    private var uVP = 0; private var uModel = 0; private var uNormal = 0; private var uMode = 0
    private var uColor = 0; private var uColor2 = 0; private var uMat = 0; private var uEye = 0
    private var uLightDir = 0; private var uLightCol = 0; private var uSkyCol = 0; private var uGroundCol = 0
    private var uFogCol = 0; private var uFog = 0; private var uSkyTop = 0; private var uSkyBot = 0

    private val vp = FloatArray(16)
    private val nm = FloatArray(9)
    private val thumbJobs = ArrayList<ThumbJob>()
    private var thumbCallback: ((String, Bitmap) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Main thread: hands over a filled frame and receives a free one to fill next. */
    fun submit(frame: RenderFrame): RenderFrame {
        synchronized(lock) {
            val old = pending
            pending = frame
            hasNew = true
            return old
        }
    }

    fun requestThumbnails(jobs: List<ThumbJob>, callback: (String, Bitmap) -> Unit) {
        synchronized(lock) {
            thumbJobs.addAll(jobs)
            thumbCallback = callback
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = buildProgram(VS, FS)
        GLES30.glUseProgram(program)
        uVP = GLES30.glGetUniformLocation(program, "uVP")
        uModel = GLES30.glGetUniformLocation(program, "uModel")
        uNormal = GLES30.glGetUniformLocation(program, "uNormal")
        uMode = GLES30.glGetUniformLocation(program, "uMode")
        uColor = GLES30.glGetUniformLocation(program, "uColor")
        uColor2 = GLES30.glGetUniformLocation(program, "uColor2")
        uMat = GLES30.glGetUniformLocation(program, "uMat")
        uEye = GLES30.glGetUniformLocation(program, "uEye")
        uLightDir = GLES30.glGetUniformLocation(program, "uLightDir")
        uLightCol = GLES30.glGetUniformLocation(program, "uLightCol")
        uSkyCol = GLES30.glGetUniformLocation(program, "uSkyCol")
        uGroundCol = GLES30.glGetUniformLocation(program, "uGroundCol")
        uFogCol = GLES30.glGetUniformLocation(program, "uFogCol")
        uFog = GLES30.glGetUniformLocation(program, "uFog")
        uSkyTop = GLES30.glGetUniformLocation(program, "uSkyTop")
        uSkyBot = GLES30.glGetUniformLocation(program, "uSkyBot")

        vaos = IntArray(meshes.size)
        counts = IntArray(meshes.size)
        for (i in meshes.indices) {
            vaos[i] = upload(meshes[i])
            counts[i] = meshes[i].indices.size
        }
        val sky = mesh { Shapes.skyTriangle(it) }
        skyVao = upload(sky)
        skyCount = sky.indices.size
        lastVao = -1
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFuncSeparate(
            GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA,
            GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA,
        )
    }

    private fun upload(m: MeshData): Int {
        val ids = IntArray(1)
        GLES30.glGenVertexArrays(1, ids, 0)
        val vao = ids[0]
        GLES30.glBindVertexArray(vao)
        val bufs = IntArray(2)
        GLES30.glGenBuffers(2, bufs, 0)
        val vb = ByteBuffer.allocateDirect(m.vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(m.vertices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bufs[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, m.vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)
        val ib = ByteBuffer.allocateDirect(m.indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(m.indices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, bufs[1])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, m.indices.size * 2, ib, GLES30.GL_STATIC_DRAW)
        val stride = MeshData.STRIDE * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 24)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 3, GLES30.GL_FLOAT, false, stride, 32)
        GLES30.glBindVertexArray(0)
        return vao
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w; height = h
        GLES30.glViewport(0, 0, w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        var jobs: List<ThumbJob>? = null
        synchronized(lock) {
            if (hasNew) {
                val t = reading
                reading = pending
                pending = t
                hasNew = false
            }
            if (thumbJobs.isNotEmpty()) {
                jobs = ArrayList(thumbJobs)
                thumbJobs.clear()
            }
        }
        jobs?.let { renderThumbs(it) }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        drawFrame(reading)
    }

    private fun drawFrame(f: RenderFrame) {
        GLES30.glClearColor(f.fogCol[0], f.fogCol[1], f.fogCol[2], f.clearAlpha)
        GLES30.glDepthMask(true)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(program)
        Matrix.multiplyMM(vp, 0, f.proj, 0, f.view, 0)
        GLES30.glUniformMatrix4fv(uVP, 1, false, vp, 0)
        GLES30.glUniform3fv(uEye, 1, f.eye, 0)
        GLES30.glUniform3fv(uLightDir, 1, f.lightDir, 0)
        GLES30.glUniform3fv(uLightCol, 1, f.lightCol, 0)
        GLES30.glUniform3fv(uSkyCol, 1, f.skyCol, 0)
        GLES30.glUniform3fv(uGroundCol, 1, f.groundCol, 0)
        GLES30.glUniform3fv(uFogCol, 1, f.fogCol, 0)
        GLES30.glUniform2fv(uFog, 1, f.fog, 0)
        GLES30.glUniform3fv(uSkyTop, 1, f.skyTop, 0)
        GLES30.glUniform3fv(uSkyBot, 1, f.skyBottom, 0)
        lastVao = -1

        if (f.drawSky) {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLES30.glDepthMask(false)
            GLES30.glUniform1i(uMode, 4)
            GLES30.glBindVertexArray(skyVao)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, skyCount, GLES30.GL_UNSIGNED_SHORT, 0)
            lastVao = skyVao
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glDepthMask(true)
        }

        GLES30.glDisable(GLES30.GL_BLEND)
        for (i in 0 until f.count) {
            val m = f.mode[i]
            if ((m == RenderFrame.LIT || m == RenderFrame.UNLIT) && f.color[i * 4 + 3] >= 0.995f) drawInstance(f, i)
        }
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glDepthMask(false)
        GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
        GLES30.glPolygonOffset(-2f, -2f)
        for (i in 0 until f.count) if (f.mode[i] == RenderFrame.SHADOW) drawInstance(f, i)
        GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)
        for (i in 0 until f.count) {
            val m = f.mode[i]
            val transparent = m == RenderFrame.GLASS ||
                ((m == RenderFrame.LIT || m == RenderFrame.UNLIT) && f.color[i * 4 + 3] < 0.995f)
            if (transparent) drawInstance(f, i)
        }
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun drawInstance(f: RenderFrame, i: Int) {
        val meshId = f.mesh[i]
        if (meshId < 0 || meshId >= vaos.size) return
        val vao = vaos[meshId]
        if (vao != lastVao) {
            GLES30.glBindVertexArray(vao)
            lastVao = vao
        }
        val o = i * 16
        val md = f.model
        GLES30.glUniformMatrix4fv(uModel, 1, false, md, o)
        val a00 = md[o]; val a10 = md[o + 1]; val a20 = md[o + 2]
        val a01 = md[o + 4]; val a11 = md[o + 5]; val a21 = md[o + 6]
        val a02 = md[o + 8]; val a12 = md[o + 9]; val a22 = md[o + 10]
        val c00 = a11 * a22 - a12 * a21
        val c01 = -(a10 * a22 - a12 * a20)
        val c02 = a10 * a21 - a11 * a20
        val c10 = -(a01 * a22 - a02 * a21)
        val c11 = a00 * a22 - a02 * a20
        val c12 = -(a00 * a21 - a01 * a20)
        val c20 = a01 * a12 - a02 * a11
        val c21 = -(a00 * a12 - a02 * a10)
        val c22 = a00 * a11 - a01 * a10
        val det = a00 * c00 + a01 * c01 + a02 * c02
        val s = if (det < 0f) -1f else 1f
        nm[0] = c00 * s; nm[1] = c10 * s; nm[2] = c20 * s
        nm[3] = c01 * s; nm[4] = c11 * s; nm[5] = c21 * s
        nm[6] = c02 * s; nm[7] = c12 * s; nm[8] = c22 * s
        GLES30.glUniformMatrix3fv(uNormal, 1, false, nm, 0)
        GLES30.glUniform4fv(uColor, 1, f.color, i * 4)
        GLES30.glUniform4fv(uColor2, 1, f.color2, i * 4)
        GLES30.glUniform4fv(uMat, 1, f.mat, i * 4)
        GLES30.glUniform1i(uMode, f.mode[i])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, counts[meshId], GLES30.GL_UNSIGNED_SHORT, 0)
    }

    private fun renderThumbs(jobs: List<ThumbJob>) {
        val cb = thumbCallback ?: return
        for (job in jobs) {
            val size = job.size * 2
            val fbo = IntArray(1)
            val tex = IntArray(1)
            val rbo = IntArray(1)
            GLES30.glGenFramebuffers(1, fbo, 0)
            GLES30.glGenTextures(1, tex, 0)
            GLES30.glGenRenderbuffers(1, rbo, 0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, size, size, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rbo[0])
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, size, size)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0])
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, tex[0], 0)
            GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_RENDERBUFFER, rbo[0])
            if (GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE) {
                GLES30.glViewport(0, 0, size, size)
                job.frame.drawSky = false
                job.frame.clearAlpha = 0f
                job.frame.fogCol[0] = 0f; job.frame.fogCol[1] = 0f; job.frame.fogCol[2] = 0f
                drawFrame(job.frame)
                val buf = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder())
                GLES30.glReadPixels(0, 0, size, size, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)
                val px = IntArray(size * size)
                buf.position(0)
                for (y in 0 until size) {
                    val row = (size - 1 - y) * size
                    for (x in 0 until size) {
                        val r = buf.get().toInt() and 255
                        val g = buf.get().toInt() and 255
                        val b = buf.get().toInt() and 255
                        val a = buf.get().toInt() and 255
                        // Pixels were blended into a premultiplied-like target; un-premultiply.
                        val ur = if (a > 0) (r * 255 / a).coerceAtMost(255) else 0
                        val ug = if (a > 0) (g * 255 / a).coerceAtMost(255) else 0
                        val ub = if (a > 0) (b * 255 / a).coerceAtMost(255) else 0
                        px[row + x] = (a shl 24) or (ur shl 16) or (ug shl 8) or ub
                    }
                }
                val full = Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
                val small = Bitmap.createScaledBitmap(full, job.size, job.size, true)
                if (small !== full) full.recycle()
                val key = job.key
                mainHandler.post { cb(key, small) }
            } else {
                Log.w("Renderer3D", "Thumbnail framebuffer incomplete")
            }
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glDeleteFramebuffers(1, fbo, 0)
            GLES30.glDeleteTextures(1, tex, 0)
            GLES30.glDeleteRenderbuffers(1, rbo, 0)
        }
    }

    private fun buildProgram(vs: String, fs: String): Int {
        val v = compile(GLES30.GL_VERTEX_SHADER, vs)
        val f = compile(GLES30.GL_FRAGMENT_SHADER, fs)
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, v)
        GLES30.glAttachShader(p, f)
        GLES30.glLinkProgram(p)
        val ok = IntArray(1)
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) Log.e("Renderer3D", "Link failed: " + GLES30.glGetProgramInfoLog(p))
        return p
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, src)
        GLES30.glCompileShader(s)
        val ok = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) Log.e("Renderer3D", "Shader compile failed: " + GLES30.glGetShaderInfoLog(s))
        return s
    }

    companion object {
        private const val VS = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNor;
layout(location=2) in vec2 aUv;
layout(location=3) in vec3 aCol;
uniform mat4 uVP;
uniform mat4 uModel;
uniform mat3 uNormal;
uniform int uMode;
out vec3 vWorld;
out vec3 vNor;
out vec2 vUv;
out vec3 vLocal;
out vec3 vCol;
void main() {
    vUv = aUv;
    vLocal = aPos;
    vCol = aCol;
    if (uMode == 4) {
        gl_Position = vec4(aPos.xy, 0.9999, 1.0);
        vWorld = aPos;
        vNor = vec3(0.0, 0.0, 1.0);
        return;
    }
    vec4 w = uModel * vec4(aPos, 1.0);
    vWorld = w.xyz;
    vNor = uNormal * aNor;
    gl_Position = uVP * w;
}
"""

        private const val FS = """#version 300 es
precision highp float;
in vec3 vWorld;
in vec3 vNor;
in vec2 vUv;
in vec3 vLocal;
in vec3 vCol;
uniform vec4 uColor;
uniform vec4 uColor2;
uniform vec4 uMat;
uniform int uMode;
uniform vec3 uEye;
uniform vec3 uLightDir;
uniform vec3 uLightCol;
uniform vec3 uSkyCol;
uniform vec3 uGroundCol;
uniform vec3 uFogCol;
uniform vec2 uFog;
uniform vec3 uSkyTop;
uniform vec3 uSkyBot;
out vec4 frag;
void main() {
    if (uMode == 4) {
        float t = clamp(vUv.y, 0.0, 1.0);
        vec3 c = mix(uSkyBot, uSkyTop, smoothstep(0.05, 0.95, t));
        frag = vec4(c, 1.0);
        return;
    }
    if (uMode == 1) {
        vec2 d = vUv * 2.0 - 1.0;
        float a = 1.0 - smoothstep(0.15, 1.0, length(d));
        frag = vec4(uColor.rgb, uColor.a * a);
        return;
    }
    vec3 base = uColor.rgb * vCol;
    int pat = int(uMat.z + 0.5);
    float ps = uColor2.a;
    if (pat == 1) {
        vec2 g = floor(vLocal.xz * ps);
        float s = mod(g.x, 2.0) + mod(g.y, 2.0);
        base = mix(base, uColor2.rgb, s * 0.5);
    } else if (pat == 2) {
        float s = step(0.5, fract(vLocal.y * ps));
        base = mix(base, uColor2.rgb, s);
    } else if (pat == 3) {
        float s = step(0.5, fract((vLocal.x + vLocal.y) * ps));
        base = mix(base, uColor2.rgb, s);
    } else if (pat == 4) {
        float s = step(0.5, fract(length(vLocal.xy) * ps + atan(vLocal.y, vLocal.x) * 0.159));
        base = mix(base, uColor2.rgb, s);
    } else if (pat == 5) {
        vec2 q = fract(vLocal.xz * ps) - 0.5;
        float s = 1.0 - step(0.22, length(q));
        base = mix(base, uColor2.rgb, s);
    } else if (pat == 6) {
        float s = step(0.5, fract(vLocal.x * ps));
        base = mix(base, uColor2.rgb, s * 0.35);
    }
    if (uMode == 2) {
        frag = vec4(base, uColor.a);
        return;
    }
    vec3 N = normalize(vNor);
    vec3 L = normalize(uLightDir);
    vec3 V = normalize(uEye - vWorld);
    vec3 H = normalize(L + V);
    float ndl = dot(N, L);
    float wrap = clamp((ndl + 0.4) / 1.4, 0.0, 1.0);
    float hemi = N.y * 0.5 + 0.5;
    vec3 amb = mix(uGroundCol, uSkyCol, hemi);
    vec3 col = base * (amb * 0.62 + uLightCol * wrap * 0.78);
    float gloss = uMat.x;
    float spec = pow(max(dot(N, H), 0.0), mix(10.0, 96.0, gloss)) * gloss * 0.85;
    float rim = pow(1.0 - max(dot(N, V), 0.0), 2.6) * uMat.y;
    col += vec3(spec) * uLightCol + rim * mix(uSkyCol, vec3(1.0), 0.5) * 0.55;
    col += base * uMat.w;
    float d = length(uEye - vWorld);
    float fg = clamp((d - uFog.x) / max(uFog.y - uFog.x, 0.001), 0.0, 1.0);
    if (uMode == 3) {
        float fr = pow(1.0 - max(dot(N, V), 0.0), 2.0);
        col = mix(col + fr * 0.35, uFogCol, fg * 0.8);
        frag = vec4(col, clamp(uColor.a + fr * 0.55, 0.0, 1.0));
    } else {
        col = mix(col, uFogCol, fg * 0.85);
        frag = vec4(col, uColor.a);
    }
}
"""
    }
}

/** Picks an RGB888 + depth config with 4x MSAA when available, falling back gracefully. */
class MsaaConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val es3 = 0x40 // EGL_OPENGL_ES3_BIT_KHR
        val tries = arrayOf(
            intArrayOf(EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, es3,
                EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, 4, EGL10.EGL_NONE),
            intArrayOf(EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, es3, EGL10.EGL_NONE),
            intArrayOf(EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6, EGL10.EGL_BLUE_SIZE, 5,
                EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, es3, EGL10.EGL_NONE),
        )
        for (attribs in tries) {
            val num = IntArray(1)
            if (!egl.eglChooseConfig(display, attribs, null, 0, num) || num[0] <= 0) continue
            val configs = arrayOfNulls<EGLConfig>(num[0])
            if (egl.eglChooseConfig(display, attribs, configs, num[0], num)) {
                configs.firstOrNull()?.let { return it }
            }
        }
        throw IllegalStateException("No suitable EGL config")
    }
}
