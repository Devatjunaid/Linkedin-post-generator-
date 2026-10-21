package com.feedthewrongthing.game.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Registry of every mesh in the game. Ids are stable indices into [all].
 * Built once on the main thread before the first frame.
 */
object Meshes {
    val all = ArrayList<MeshData>()
    private var built = false

    var sphere = 0; private set
    var sphereLo = 0; private set
    var egg = 0; private set
    var pear = 0; private set
    var fuzzy = 0; private set
    var fuzzySoft = 0; private set
    var teardrop = 0; private set
    var capsule = 0; private set
    var cylinder = 0; private set
    var cylinderOpen = 0; private set
    var cone = 0; private set
    var torus = 0; private set
    var torusThin = 0; private set
    var curve = 0; private set
    var roundCube = 0; private set
    var softCube = 0; private set
    var cube = 0; private set
    var star = 0; private set
    var heart = 0; private set
    var shadowQuad = 0; private set
    var ground = 0; private set
    var bell = 0; private set
    var bottle = 0; private set
    var wrapper = 0; private set
    var wedge = 0; private set
    var halfSphere = 0; private set
    var note = 0; private set
    var zee = 0; private set
    var cloud = 0; private set
    var drop = 0; private set

    /** Raw (untransformed) primitive data, handy for baking composites. */
    lateinit var rawSphere: MeshData; private set
    lateinit var rawSphereLo: MeshData; private set
    lateinit var rawEgg: MeshData; private set
    lateinit var rawCylinder: MeshData; private set
    lateinit var rawCone: MeshData; private set
    lateinit var rawTorus: MeshData; private set
    lateinit var rawTorusThin: MeshData; private set
    lateinit var rawCurve: MeshData; private set
    lateinit var rawCurveUp: MeshData; private set
    lateinit var rawRoundCube: MeshData; private set
    lateinit var rawCube: MeshData; private set
    lateinit var rawTeardrop: MeshData; private set
    lateinit var rawCapsule: MeshData; private set
    lateinit var rawFuzzy: MeshData; private set
    lateinit var rawHalfSphere: MeshData; private set
    lateinit var rawWedge: MeshData; private set
    lateinit var rawBell: MeshData; private set
    lateinit var rawWrapper: MeshData; private set
    lateinit var rawStar: MeshData; private set
    lateinit var rawHeart: MeshData; private set

    fun add(m: MeshData): Int {
        all.add(m)
        return all.size - 1
    }

    @Synchronized
    fun init() {
        if (built) return
        built = true
        rawSphere = mesh { Shapes.sphere(it, 32, 22) }
        sphere = add(rawSphere)
        rawSphereLo = mesh { Shapes.sphere(it, 14, 9) }
        sphereLo = add(rawSphereLo)
        rawEgg = mesh { Shapes.egg(it) }
        egg = add(rawEgg)
        pear = add(mesh { Shapes.pear(it) })
        rawFuzzy = mesh { Shapes.fuzzy(it, 0.075f, 14f, 13f) }
        fuzzy = add(rawFuzzy)
        fuzzySoft = add(mesh { Shapes.fuzzy(it, 0.04f, 7f, 6f, 36, 24) })
        rawTeardrop = mesh { Shapes.teardrop(it) }
        teardrop = add(rawTeardrop)
        rawCapsule = mesh { Shapes.capsule(it, 1f) }
        capsule = add(rawCapsule)
        rawCylinder = mesh { Shapes.cylinder(it, 1f, 1f, 2f, 28, true) }
        cylinder = add(rawCylinder)
        cylinderOpen = add(mesh { Shapes.cylinder(it, 1f, 1f, 2f, 28, false) })
        rawCone = mesh { Shapes.cylinder(it, 1f, 0f, 2f, 28, true) }
        cone = add(rawCone)
        rawTorus = mesh { Shapes.torusArc(it, 1f, 0.32f, (Math.PI * 2).toFloat(), false, 36, 16) }
        torus = add(rawTorus)
        rawTorusThin = mesh { Shapes.torusArc(it, 1f, 0.07f, (Math.PI * 2).toFloat(), false, 40, 8) }
        torusThin = add(rawTorusThin)
        rawCurve = mesh { Shapes.torusArc(it, 1f, 0.3f, 2.3f, true, 30, 14) }
        curve = add(rawCurve)
        // Same arc re-oriented into the XY plane, bulging down and opening up (banana / chili).
        rawCurveUp = MeshBuilder().also {
            it.append(rawCurve, floatArrayOf(0f, -1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 0.7f, 0f, 1f))
        }.build()
        rawRoundCube = mesh { Shapes.roundBox(it, 1f, 1f, 1f, 0.28f, 4) }
        roundCube = add(rawRoundCube)
        softCube = add(mesh { Shapes.roundBox(it, 1f, 1f, 1f, 0.6f, 5) })
        rawCube = mesh { Shapes.box(it, 1f, 1f, 1f) }
        cube = add(rawCube)
        rawStar = mesh { Shapes.puffyStar(it) }
        star = add(rawStar)
        rawHeart = mesh { Shapes.puffyHeart(it) }
        heart = add(rawHeart)
        shadowQuad = add(mesh { Shapes.quadXZ(it) })
        ground = add(mesh { Shapes.grid(it, 24) })
        rawBell = mesh {
            Shapes.lathe(it, 28, 14, { v -> 0.12f + 0.88f * v * v * v }, { v -> v * 1.6f - 0.8f })
        }
        bell = add(rawBell)
        bottle = add(mesh {
            Shapes.lathe(it, 30, 26, { v ->
                when {
                    v < 0.06f -> sin(v / 0.06f * 1.5708f) * 0.95f
                    v < 0.62f -> 0.95f + 0.05f * sin((v - 0.06f) * 9f)
                    v < 0.82f -> 0.95f - (v - 0.62f) / 0.2f * 0.5f
                    v < 0.97f -> 0.45f
                    else -> 0.45f * cos((v - 0.97f) / 0.03f * 1.5708f)
                }
            }, { v -> v * 2.4f - 1.0f })
        })
        rawWrapper = mesh {
            Shapes.surface(it, 48, 6, { u, v, o ->
                val t = u * 6.2831855f
                val r = (0.72f + 0.28f * v) * (1f + 0.045f * cos(t * 16f))
                o[0] = r * cos(t); o[1] = v * 1.2f - 0.6f; o[2] = r * sin(t)
            })
        }
        wrapper = add(rawWrapper)
        rawWedge = mesh { prism(it, floatArrayOf(0f, 1.0f, -0.78f, -0.62f, 0.78f, -0.62f), 0.12f) }
        wedge = add(rawWedge)
        rawHalfSphere = mesh {
            Shapes.surface(it, 32, 12, { u, v, o ->
                val t = u * 6.2831855f
                val ph = 1.5708f + v * 1.5708f
                val r = sin(ph)
                o[0] = r * cos(t); o[1] = -cos(ph); o[2] = r * sin(t)
            })
        }
        halfSphere = add(rawHalfSphere)
        drop = add(mesh {
            Shapes.surface(it, 18, 12, { u, v, o ->
                val t = u * 6.2831855f
                val ph = v * Math.PI.toFloat()
                val r = sin(ph) * (0.35f + 0.65f * (1f - v))
                o[0] = r * cos(t); o[1] = -cos(ph) * 1.2f; o[2] = r * sin(t)
            })
        })

        note = add(Bake()
            .add(rawSphereLo, 0xFFFFFF, -0.25f, -0.55f, 0f, 0.32f, 0.24f, 0.24f, rz = -20f)
            .add(rawCylinder, 0xFFFFFF, 0.04f, 0.1f, 0f, 0.06f, 0.62f, 0.06f)
            .add(rawCube, 0xFFFFFF, 0.24f, 0.62f, 0f, 0.22f, 0.07f, 0.06f, rz = -25f)
            .build())
        zee = add(Bake()
            .add(rawCube, 0xFFFFFF, 0f, 0.42f, 0f, 0.42f, 0.09f, 0.09f)
            .add(rawCube, 0xFFFFFF, 0f, 0f, 0f, 0.55f, 0.08f, 0.08f, rz = 45f)
            .add(rawCube, 0xFFFFFF, 0f, -0.42f, 0f, 0.42f, 0.09f, 0.09f)
            .build())
        cloud = add(Bake()
            .add(rawSphereLo, 0xFFFFFF, 0f, 0f, 0f, 1.0f, 0.8f, 0.9f)
            .add(rawSphereLo, 0xFFFFFF, 0.95f, -0.15f, 0.1f, 0.75f, 0.6f, 0.7f)
            .add(rawSphereLo, 0xFFFFFF, -0.95f, -0.2f, 0f, 0.7f, 0.55f, 0.65f)
            .add(rawSphereLo, 0xFFFFFF, 0.35f, 0.45f, -0.1f, 0.7f, 0.6f, 0.65f)
            .build())
    }

    /** Extruded convex polygon in XZ (pairs of x,z) with hard edges. */
    private fun prism(b: MeshBuilder, pts: FloatArray, halfH: Float) {
        val n = pts.size / 2
        var cx = 0f; var cz = 0f
        for (i in 0 until n) { cx += pts[i * 2]; cz += pts[i * 2 + 1] }
        cx /= n; cz /= n
        for (side in intArrayOf(-1, 1)) {
            val y = halfH * side
            val c = b.vertex(cx, y, cz, 0f, side.toFloat(), 0f, 0.5f, 0.5f)
            val first = b.vertexCount
            for (i in 0..n) {
                val k = i % n
                b.vertex(pts[k * 2], y, pts[k * 2 + 1], 0f, side.toFloat(), 0f, 0f, 0f)
            }
            for (i in 0 until n) b.tri(c, first + i, first + i + 1)
        }
        for (i in 0 until n) {
            val ax = pts[i * 2]; val az = pts[i * 2 + 1]
            val bx = pts[((i + 1) % n) * 2]; val bz = pts[((i + 1) % n) * 2 + 1]
            var nx = bz - az; var nz = -(bx - ax)
            val mx = (ax + bx) / 2 - cx; val mz = (az + bz) / 2 - cz
            if (nx * mx + nz * mz < 0) { nx = -nx; nz = -nz }
            val l = sqrt(nx * nx + nz * nz)
            nx /= l; nz /= l
            val v0 = b.vertex(ax, -halfH, az, nx, 0f, nz, 0f, 0f)
            val v1 = b.vertex(bx, -halfH, bz, nx, 0f, nz, 1f, 0f)
            val v2 = b.vertex(bx, halfH, bz, nx, 0f, nz, 1f, 1f)
            val v3 = b.vertex(ax, halfH, az, nx, 0f, nz, 0f, 1f)
            b.tri(v0, v1, v2); b.tri(v0, v2, v3)
        }
    }
}
