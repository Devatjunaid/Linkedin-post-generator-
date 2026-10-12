package com.sudzy.carwash.game

import com.sudzy.carwash.art.CarModel
import com.sudzy.carwash.art.CarType
import com.sudzy.carwash.art.Icon

enum class Tool(val icon: Icon, val label: String, val word: String, val loop: String?) {
    HOSE(Icon.HOSE, "Hose", "SPLASH", "spray"),
    FOAM(Icon.FOAM, "Foam", "FOAMY", "foam"),
    SPONGE(Icon.SPONGE, "Sponge", "SCRUB", "scrub"),
    BRUSH(Icon.BRUSH, "Brush", "SCRUB", "scrub"),
    DRYER(Icon.DRYER, "Dryer", "WHOOSH", "dryer"),
    POLISH(Icon.POLISH, "Polish", "GLEAM", "polish"),
    WAX(Icon.WAX, "Wax", "SHINY", "polish"),
    TIRE(Icon.TIRE, "Tires", "SLICK", "foam"),
    STICKER(Icon.STICKER, "Stickers", "COOL", null),
}

enum class Step(val tool: Tool, val hint: String) {
    RINSE_DIRT(Tool.HOSE, "Spray off the dirt!"),
    FOAM(Tool.FOAM, "Cover it in foam!"),
    SCRUB(Tool.SPONGE, "Scrub the spots!"),
    SCRUB_TOUGH(Tool.BRUSH, "Brush the tough stuff!"),
    RINSE_FOAM(Tool.HOSE, "Rinse off the suds!"),
    DRY(Tool.DRYER, "Blow it dry!"),
    POLISH(Tool.POLISH, "Polish the scratches!"),
    WAX(Tool.WAX, "Wax for super shine!"),
    TIRES(Tool.TIRE, "Make the tires shine!"),
    STICKERS(Tool.STICKER, "Add 3 cool stickers!"),
}

enum class SpotKind { LEAF, BIRD, MUD, BUG, ICE, GRAFFITI, GOO }

class World(
    val name: String,
    val dirt: Int, val dirt2: Int,
    val spot: SpotKind, val tough: SpotKind,
    val hp: Float,
    val sky1: Int, val sky2: Int,
    val intro: List<String>,
)

class Level(
    val index: Int,
    val world: Int,
    val car: CarType,
    val color: Int,
    val stripe: Int?,
    val steps: List<Step>,
    val spots: Int,
    val toughSpots: Int,
    val scratches: Int,
    val drops: Int,
    val customer: Int,
    val goldenBubbles: Boolean,
    val rush: Boolean,
    val vip: Boolean,
) {
    fun model() = CarModel(car, color, stripe)
    val w: World get() = Levels.worlds[world]
}

/**
 * 30 hidden stages: 6 worlds x 5 cars. Each stage adds a little more: new tools,
 * tougher grime, more spots, golden bubbles, rush-hour tips and VIP finales.
 */
object Levels {
    const val COUNT = 30

    val worlds = listOf(
        World("Sunny Suburbs", 0xFF9A7650.toInt(), 0xFF7A5A38.toInt(), SpotKind.LEAF, SpotKind.BIRD, 1.0f, 0xFF8FE6FF.toInt(), 0xFF4FB8FF.toInt(),
            listOf("Oops! I drove through a puddle!", "Can you make it shiny?", "It's soooo dusty!", "My car needs a bath!", "Big day! Make it sparkle!")),
        World("Mud Farm", 0xFF6B4220.toInt(), 0xFF8A5A2B.toInt(), SpotKind.MUD, SpotKind.BIRD, 1.5f, 0xFFB8E986.toInt(), 0xFF6FBF4A.toInt(),
            listOf("Yeehaw! Mud everywhere!", "The pigs splashed me!", "Muddy muddy mess!", "Tractor trail trouble!", "Monster mud bath!")),
        World("Dune Desert", 0xFFD9B16A.toInt(), 0xFFB8894A.toInt(), SpotKind.BUG, SpotKind.MUD, 1.8f, 0xFFFFE3A0.toInt(), 0xFFFFB84D.toInt(),
            listOf("Sand in every crack!", "Bugs on my windshield!", "Quick! Rush hour!", "Sandstorm surprise!", "Rally day! Make it gleam!")),
        World("Frosty Peaks", 0xFFF4F9FF.toInt(), 0xFFC9E2F7.toInt(), SpotKind.ICE, SpotKind.ICE, 2.1f, 0xFFDDF2FF.toInt(), 0xFF8CC8F5.toInt(),
            listOf("Brrr! Snowed in!", "Icicles on my car!", "Hurry before it freezes!", "Frosty and salty!", "Snow monster truck!")),
        World("Neon City", 0xFF6DFF4A.toInt(), 0xFF2FD27A.toInt(), SpotKind.GOO, SpotKind.GRAFFITI, 2.4f, 0xFF7A3DFF.toInt(), 0xFF2A0E6B.toInt(),
            listOf("Slime from the sewer!", "Who drew on my car?!", "Quick! Party tonight!", "Glow-goo attack!", "VIP rocket! Make it pop!")),
        World("Galaxy Garage", 0xFF8A3DFF.toInt(), 0xFFC46BFF.toInt(), SpotKind.GOO, SpotKind.ICE, 2.7f, 0xFF2A0E6B.toInt(), 0xFF0B0630.toInt(),
            listOf("Bleep! Space goo!", "Comet dust everywhere!", "Warp speed wash, please!", "Moon mud! Bleep bloop!", "The Galaxy Cup! Shine it!")),
    )

    private val CARS = listOf(
        listOf(CarType.SEDAN, CarType.HATCH, CarType.SEDAN, CarType.HATCH, CarType.SPORTS),
        listOf(CarType.JEEP, CarType.PICKUP, CarType.HATCH, CarType.JEEP, CarType.MONSTER),
        listOf(CarType.PICKUP, CarType.SPORTS, CarType.JEEP, CarType.SEDAN, CarType.SPORTS),
        listOf(CarType.JEEP, CarType.HATCH, CarType.PICKUP, CarType.SEDAN, CarType.MONSTER),
        listOf(CarType.SPORTS, CarType.SEDAN, CarType.SPORTS, CarType.HATCH, CarType.ROCKET),
        listOf(CarType.ROCKET, CarType.MONSTER, CarType.SPORTS, CarType.JEEP, CarType.ROCKET),
    )
    private val COLORS = intArrayOf(
        0xFFFF3B6B.toInt(), 0xFF2FD27A.toInt(), 0xFF00B8FF.toInt(), 0xFFFFD23F.toInt(), 0xFFA46BFF.toInt(),
        0xFFFF8A2B.toInt(), 0xFFFF6FB5.toInt(), 0xFF22C1B0.toInt(), 0xFF3F6FF0.toInt(), 0xFFE84040.toInt(),
    )
    private val STRIPES = arrayOf<Int?>(0xFFFFFFFF.toInt(), null, 0xFFFFE14D.toInt(), 0xFF222244.toInt(), 0xFF5EF0FF.toInt(), null, 0xFFFFFFFF.toInt())

    fun get(i: Int): Level {
        val idx = i.coerceIn(0, COUNT - 1)
        val w = idx / 5; val k = idx % 5
        val steps = mutableListOf(Step.RINSE_DIRT, Step.FOAM)
        if (idx >= 1) steps += Step.SCRUB
        if (idx >= 15) steps += Step.SCRUB_TOUGH
        steps += Step.RINSE_FOAM
        if (idx >= 2) steps += Step.DRY
        if (idx >= 10) steps += Step.POLISH
        if (idx >= 3) steps += Step.WAX
        if (idx >= 5) steps += Step.TIRES
        if (idx >= 20 || k == 4 && idx >= 4) steps += Step.STICKERS
        return Level(
            index = idx, world = w,
            car = CARS[w][k],
            color = COLORS[(idx * 3 + w) % COLORS.size],
            stripe = STRIPES[(idx + w) % STRIPES.size],
            steps = steps,
            spots = if (Step.SCRUB in steps) 4 + idx / 3 else 0,
            toughSpots = if (Step.SCRUB_TOUGH in steps) 3 + (idx - 15) / 3 else 0,
            scratches = if (Step.POLISH in steps) 3 + (idx - 10) / 4 else 0,
            drops = if (Step.DRY in steps) 14 + idx / 2 else 0,
            customer = idx * 7 + 3,
            goldenBubbles = idx >= 4,
            rush = k == 2 && w >= 2,
            vip = k == 4,
        )
    }
}
