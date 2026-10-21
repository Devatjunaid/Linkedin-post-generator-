package com.drawtocompletefun.kids.game

import com.drawtocompletefun.kids.game.Pictures as P

data class World(
    val index: Int,
    val name: String,
    val firstLevel: Int,
    val lastLevel: Int,
    /** How close (on the 0..100 board) a stroke must stay to the missing line. */
    val tolerance: Float,
)

class Level(
    val number: Int,
    val picture: Picture,
    val missingTags: List<String>,
) {
    val world: World get() = Levels.worldOf(number)
    val missing: List<Shape> get() = missingTags.map { picture.shape(it)!! }

    /** True for levels 5, 10, 15 ... that open after a short reward video. */
    val isTreasureLevel: Boolean get() = number % Levels.TREASURE_EVERY == 0

    val title: String
        get() {
            val name = picture.name
            return when (number % 4) {
                0 -> "Fix the $name!"
                1 -> "Finish the $name!"
                2 -> "Help the $name!"
                else -> "Complete the $name!"
            }
        }
}

object Levels {
    const val COUNT = 100
    const val TREASURE_EVERY = 5

    val worlds = listOf(
        World(0, "Shapes", 1, 25, 9.5f),
        World(1, "Animals", 26, 50, 9f),
        World(2, "Things", 51, 75, 8.5f),
        World(3, "Brain Twisters", 76, 100, 8f),
    )

    fun worldOf(level: Int): World = worlds.first { level in it.firstLevel..it.lastLevel }

    private fun lv(p: Picture, vararg missing: String) = p to missing.toList()

    private val defs: List<Pair<Picture, List<String>>> = listOf(
        // World 1: Shapes
        lv(P.square, "s2"),
        lv(P.triangle, "t2"),
        lv(P.circle, "q1"),
        lv(P.rectangle, "r1"),
        lv(P.diamond, "d2"),
        lv(P.heart, "sideR"),
        lv(P.house, "roofR"),
        lv(P.ladder, "r3"),
        lv(P.window, "v"),
        lv(P.star, "st3"),
        lv(P.smiley, "mouth"),
        lv(P.sun, "ray2", "ray6"),
        lv(P.tree, "trunkR"),
        lv(P.hexagon, "h2", "h5"),
        lv(P.arrow, "headT", "headB"),
        lv(P.flag, "ftop"),
        lv(P.envelope, "vL", "vR"),
        lv(P.cup, "handle"),
        lv(P.ticTacToe, "v2", "h1"),
        lv(P.balloon, "string"),
        lv(P.iceCream, "coneL", "coneR"),
        lv(P.umbrella, "stick", "hook"),
        lv(P.present, "ribV", "bowR"),
        lv(P.kite, "kv", "kh"),
        lv(P.house, "roofL", "wallR", "door"),
        // World 2: Animals
        lv(P.fish, "tailT", "tailB"),
        lv(P.cat, "earR1", "earR2"),
        lv(P.bird, "beak", "tail"),
        lv(P.snail, "antL", "antR"),
        lv(P.rabbit, "earR"),
        lv(P.cat, "whiskL1", "whiskR1", "whiskR2"),
        lv(P.turtle, "legB", "legD"),
        lv(P.butterfly, "antL", "antR", "wingBR"),
        lv(P.mouse, "tail"),
        lv(P.pig, "earR1", "earR2", "snout"),
        lv(P.owl, "beak", "tuftL1", "tuftL2"),
        lv(P.duck, "beak", "wing"),
        lv(P.bee, "st1", "st3", "sting"),
        lv(P.dog, "earL", "earR"),
        lv(P.spider, "lL2", "lR1", "lR3"),
        lv(P.bear, "earL", "snout"),
        lv(P.frog, "legL", "legR", "mouth"),
        lv(P.penguin, "footL", "footR", "wingL"),
        lv(P.fish2, "finTop", "finBot"),
        lv(P.flyingBirds, "b2L", "b2R", "b3R"),
        lv(P.snake, "z2", "z4"),
        lv(P.caterpillar, "leg2", "leg4", "ant1"),
        lv(P.jellyfish, "t1", "t4"),
        lv(P.crab, "clawR", "armR", "lgR1"),
        lv(P.lion, "ray2", "ray5", "ray8", "ray11"),
        // World 3: Things
        lv(P.car, "wheelF"),
        lv(P.bus, "w2", "wh2"),
        lv(P.sailboat, "sailT", "sailB"),
        lv(P.rocket, "finR", "finRb"),
        lv(P.plane, "wingRf", "wingRe", "wingRb"),
        lv(P.train, "chimL", "chimR", "chimT"),
        lv(P.bicycle, "fr2", "fork"),
        lv(P.truck, "cab2", "wh2"),
        lv(P.hotAirBalloon, "ropeL", "ropeR"),
        lv(P.clock, "hour", "min"),
        lv(P.lamp, "shadeL", "shadeR"),
        lv(P.chair, "s1", "lR"),
        lv(P.table, "lL", "lR"),
        lv(P.pencil, "tipT", "tipB"),
        lv(P.glasses, "lensR", "bridge"),
        lv(P.key, "t1", "t2", "t3"),
        lv(P.lollipop, "stick", "sw"),
        lv(P.drum, "stickL", "stickR", "sideR"),
        lv(P.bell, "sideR", "clap"),
        lv(P.candle, "flame", "wick"),
        lv(P.robot, "armL", "eyeR", "ant"),
        lv(P.car, "wheelB", "winDiv", "belt"),
        lv(P.bus, "w1", "w3", "wh1"),
        lv(P.rocket, "noseL", "noseR", "flame"),
        lv(P.train, "wh1", "wh2", "win1", "win3"),
        // World 4: Brain twisters (mirror halves and patterns)
        lv(P.butterfly, "wingTR", "wingBR", "antR"),
        lv(P.snowflake, "arm2", "brA4", "brB4", "arm5"),
        lv(P.castle, "roofR1", "roofR2", "doorR", "towerR3"),
        lv(P.stairs, "st7", "st8", "st9", "st10"),
        lv(P.robot, "armR", "eyeR", "mouth", "ant"),
        lv(P.ticTacToe, "v1", "v2", "h2", "x2"),
        lv(P.spider, "lR1", "lR2", "lR3", "lR4"),
        lv(P.bicycle, "wheelF", "fork", "fr2", "bar"),
        lv(P.owl, "tuftR1", "tuftR2", "eyeR", "beak"),
        lv(P.lion, "ray1", "ray3", "ray6", "ray9", "ray12"),
        lv(P.train, "wh3", "chimL", "chimR", "chimT"),
        lv(P.flower, "p2", "p4", "p6", "leafL"),
        lv(P.house, "roofR", "wallL", "floor", "chimney"),
        lv(P.caterpillar, "leg1", "leg3", "seg2", "ant2"),
        lv(P.ufo, "dome", "beamL", "beamR"),
        lv(P.crab, "clawR", "armR", "lgR1", "lgR2", "stalkR"),
        lv(P.jellyfish, "t2", "t3", "t4", "domeB"),
        lv(P.sailboat, "sailT", "sailB", "jib", "jibB"),
        lv(P.fence, "p2", "p4", "p6", "railB"),
        lv(P.pyramid, "i1", "i2", "i3", "o2"),
        lv(P.cube, "c1", "c2", "c3", "b2"),
        lv(P.clock, "hour", "min", "t1", "t4", "t7", "t10"),
        lv(P.snowflake, "brA1", "brB1", "brA3", "brB3", "brA5", "brB5"),
        lv(P.rocket, "finR", "finRb", "noseR", "flame", "sideR"),
        lv(P.castle, "roofL1", "roofL2", "roofR1", "roofR2", "doorArc", "wallTop"),
    )

    val all: List<Level> = defs.mapIndexed { i, (pic, miss) -> Level(i + 1, pic, miss) }

    fun get(number: Int): Level = all[number - 1]

    /** Throws if any level is broken. Used by tests. */
    fun validate() {
        check(all.size == COUNT) { "Expected $COUNT levels, found ${all.size}" }
        all.forEach { lvl ->
            check(lvl.missingTags.isNotEmpty()) { "Level ${lvl.number} has nothing missing" }
            lvl.missingTags.forEach { tag ->
                val s = lvl.picture.shape(tag)
                checkNotNull(s) { "Level ${lvl.number}: no line '$tag' in ${lvl.picture.name}" }
                check(!s.isDecor) { "Level ${lvl.number}: '$tag' is decoration" }
            }
        }
    }
}
