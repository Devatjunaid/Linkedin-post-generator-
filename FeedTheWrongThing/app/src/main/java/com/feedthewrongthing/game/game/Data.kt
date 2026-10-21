package com.feedthewrongthing.game.game

import com.feedthewrongthing.game.audio.Music
import com.feedthewrongthing.game.audio.Sfx

/** Every funny thing a creature can do after eating. */
enum class ReactionType(val title: String, val verb: String, val sfx: Sfx, val duration: Float) {
    GIANT_CHEEKS("Giant Cheeks", "puff up giant cheeks", Sfx.MUNCH, 2.3f),
    SPINNY_EYES("Spinny Eyes", "get spinny eyes", Sfx.WOBBLE, 2.2f),
    BUBBLE_BURP("Bubble Burp", "burp bubbles", Sfx.BURP, 2.4f),
    FIRE_BREATH("Dragon Breath", "breathe fire", Sfx.FLAME, 2.4f),
    SPARKLE_DANCE("Sparkle Dance", "dance", Sfx.SPARKLE, 2.6f),
    DUCKIFY("Duck-ify!", "turn into a duck", Sfx.QUACK, 2.6f),
    SPIN_OUT("Spin Out", "spin around", Sfx.WHOOSH, 2.0f),
    HEART_EYES("Heart Eyes", "fall in love", Sfx.HEART, 2.3f),
    SUPER_SIZE("Super Size", "grow HUGE", Sfx.GROW, 2.6f),
    SHRINK("Teeny Tiny", "shrink", Sfx.SHRINK, 2.4f),
    FROZEN("Brrr Freeze", "freeze", Sfx.FREEZE, 2.6f),
    RAINBOW("Rainbow Glow", "glow like a rainbow", Sfx.RAINBOW, 2.6f),
    STINKY("Stinky Cloud", "get stinky", Sfx.STINK, 2.4f),
    BLOCKY("Blocky Body", "turn into a block", Sfx.POP, 2.6f),
    JUMP_SCARE("Jump Scare", "jump super high", Sfx.ALARM, 2.1f),
    BALLOON_FLOAT("Float Away", "float in the air", Sfx.SQUEAK, 3.0f),
    TOOT("Toot Toot", "toot like a trumpet", Sfx.TOOT, 2.2f),
    GIGGLES("Giggle Fit", "giggle", Sfx.GIGGLE, 2.2f),
    HULA_HOOP("Hula Hoop", "hula hoop", Sfx.HULA, 2.6f),
    BUNNY_EARS("Bunny Ears", "grow bunny ears", Sfx.BOING, 2.6f),
    ROCKET("Rocket Burp", "blast off", Sfx.ROCKET, 2.6f),
    SLEEPY("Sleepy Time", "fall asleep", Sfx.SNORE, 2.6f),
    JELLY_WOBBLE("Jelly Wobble", "wobble like jelly", Sfx.WOBBLE, 2.4f),
    SEED_SPIT("Seed Spitter", "spit seeds", Sfx.SPIT, 2.4f),
    CONFETTI_SNEEZE("Confetti Sneeze", "sneeze", Sfx.SNEEZE, 2.4f),
}

/** Things you can throw. [food] separates real snacks from silly objects. */
enum class ItemType(
    val title: String,
    val reaction: ReactionType,
    val food: Boolean,
    val noisy: Boolean = false,
    val radius: Float = 0.36f,
    val bounce: Float = 0.35f,
) {
    PIZZA("Pizza", ReactionType.GIANT_CHEEKS, true),
    SOCK("Sock", ReactionType.SPINNY_EYES, false, bounce = 0.2f),
    RUBBER_DUCK("Rubber Duck", ReactionType.DUCKIFY, false, noisy = true, bounce = 0.55f),
    FISH("Fish", ReactionType.BUBBLE_BURP, true),
    CHILI("Chili", ReactionType.FIRE_BREATH, true, radius = 0.32f),
    TOOTHBRUSH("Toothbrush", ReactionType.SPARKLE_DANCE, false, radius = 0.4f),
    BANANA("Banana", ReactionType.SPIN_OUT, true),
    CUPCAKE("Cupcake", ReactionType.HEART_EYES, true),
    BROCCOLI("Broccoli", ReactionType.SUPER_SIZE, true),
    CHEESE("Cheese", ReactionType.SHRINK, true),
    ICE_CUBE("Ice Cube", ReactionType.FROZEN, true, bounce = 0.5f, radius = 0.32f),
    LOLLIPOP("Lollipop", ReactionType.RAINBOW, true),
    SNEAKER("Sneaker", ReactionType.STINKY, false, bounce = 0.45f, radius = 0.4f),
    TOY_BLOCK("Toy Block", ReactionType.BLOCKY, false, bounce = 0.4f),
    ALARM_CLOCK("Alarm Clock", ReactionType.JUMP_SCARE, false, noisy = true),
    BALLOON("Balloon", ReactionType.BALLOON_FLOAT, false, bounce = 0.7f, radius = 0.38f),
    TRUMPET("Trumpet", ReactionType.TOOT, false, noisy = true, radius = 0.4f),
    FEATHER("Feather", ReactionType.GIGGLES, false, bounce = 0.1f),
    DONUT("Donut", ReactionType.HULA_HOOP, true),
    CARROT("Carrot", ReactionType.BUNNY_EARS, true),
    SODA("Fizzy Soda", ReactionType.ROCKET, true),
    PILLOW("Pillow", ReactionType.SLEEPY, false, bounce = 0.15f, radius = 0.4f),
    JELLY("Jelly", ReactionType.JELLY_WOBBLE, true, bounce = 0.6f),
    WATERMELON("Watermelon", ReactionType.SEED_SPIT, true),
    PEPPER("Pepper Shaker", ReactionType.CONFETTI_SNEEZE, false),
}

/** The hungry crew. Geometry values are in creature-local units (feet at y = 0). */
enum class CreatureType(
    val title: String,
    val color: Int,
    val dark: Int,
    val light: Int,
    val favorite: ItemType,
    val mouthY: Float,
    val mouthZ: Float,
    val hatY: Float,
    val hatScale: Float,
    val bodyY: Float,
    val bodyR: Float,
    val blurb: String,
) {
    MUNCHY("Munchy", 0x4FD1BA, 0x2FAE98, 0xBFF5E9, ItemType.PIZZA, 0.78f, 0.86f, 1.86f, 1f, 0.95f, 0.95f, "Eats anything. Twice."),
    GULPO("Gulpo", 0xA98BFF, 0x7A55E0, 0xE4DBFF, ItemType.SOCK, 0.95f, 0.84f, 2.34f, 0.8f, 1.15f, 0.92f, "Thinks socks are snacks."),
    SLURPO("Slurpo", 0x4FA9F0, 0x2C7CC6, 0xCDEBFF, ItemType.FISH, 0.84f, 0.9f, 1.84f, 0.95f, 1.05f, 0.95f, "Burps the biggest bubbles."),
    BLORP("Blorp", 0xFF9A3C, 0xE07A1C, 0xFFE3C7, ItemType.TOY_BLOCK, 0.72f, 0.88f, 1.9f, 1f, 0.98f, 0.95f, "Three eyes, zero manners."),
    CHOMPETTE("Chompette", 0xFF8FC8, 0xE0559C, 0xFFD6EC, ItemType.CHILI, 0.78f, 0.78f, 1.78f, 0.85f, 0.95f, 0.85f, "Tiny dragon, giant appetite."),
    GRUMBO("Grumbo", 0x9FB3C8, 0x7A8FA6, 0xDCE6F0, ItemType.ICE_CUBE, 0.78f, 0.8f, 2.0f, 1.1f, 1.0f, 1.0f, "Grumpy until he giggles."),
    FIZZLE("Fizzle", 0xB6F05A, 0x86C82E, 0xE8FBC8, ItemType.SODA, 0.7f, 0.74f, 2.02f, 0.8f, 0.95f, 0.8f, "Bubbly, fizzy, full of beans."),
    SNOOZLE("Snoozle", 0xC7B8FF, 0x9F8EE8, 0xEFEAFF, ItemType.PILLOW, 0.78f, 0.84f, 1.96f, 0.95f, 1.0f, 0.95f, "Wake it up with something loud!"),
    TWIRLZ("Twirlz", 0xFFFFFF, 0xFF6B6B, 0xFFE1E1, ItemType.LOLLIPOP, 1.08f, 0.6f, 1.96f, 0.85f, 1.0f, 0.65f, "Eats to the beat."),
    BIG_MAMA("Big Mama Munch", 0xB04BE0, 0x7A2AA6, 0xF0D6FF, ItemType.CUPCAKE, 0.78f, 0.86f, 1.86f, 1f, 0.95f, 0.95f, "Queen of the Great Big Feast."),
}

/** Floating islands. Each has a resident creature and its own twist. */
enum class Island(
    val title: String,
    val creature: CreatureType,
    val twist: String,
    val skyTop: Int,
    val skyBottom: Int,
    val ground: Int,
    val music: Music,
) {
    PICNIC("Picnic Park", CreatureType.MUNCHY, "Drag a snack UP and let go to throw it!", 0x5DAEFF, 0xD6F0FF, 0x7DD14C, Music.PLAY),
    SOCKS("Sock Drawer Caves", CreatureType.GULPO, "Watch the thought bubbles... then feed the WRONG thing!", 0x3A2A6E, 0x8E6CF0, 0x6B4FA8, Music.PLAY),
    BATH("Bubble Bath Bay", CreatureType.SLURPO, "Creatures bob and drift. Lead your throws!", 0x7FC8FF, 0xE8F8FF, 0x7FD3FF, Music.PLAY),
    TOYS("Toy Box Town", CreatureType.BLORP, "Bounce off trampolines and around block walls!", 0xFFB86B, 0xFFF1D6, 0xE0A866, Music.PLAY),
    KITCHEN("Kitchen Chaos", CreatureType.CHOMPETTE, "Snacks ride the conveyor belt. Grab them fast!", 0xFFD7A8, 0xFFF6E5, 0xF3E3C8, Music.PLAY),
    SNOW("Snowy Snack Peaks", CreatureType.GRUMBO, "Whoosh! The wind pushes your snacks sideways.", 0x8FC3FF, 0xF0F8FF, 0xF2F7FF, Music.HOME),
    FACTORY("Fizzy Factory", CreatureType.FIZZLE, "Fans and bumpers! Plan a bank shot.", 0x5E8C9A, 0xC7E6E6, 0x9AA6BF, Music.PLAY),
    MOON("Pajama Moon Party", CreatureType.SNOOZLE, "Sleepyheads! Throw something noisy to wake them.", 0x141A44, 0x4A3E8C, 0x2F5A4A, Music.NIGHT),
    CARNIVAL("Carnival of Crumbs", CreatureType.TWIRLZ, "Feed to the beat when mouths pop open!", 0xFF8FB3, 0xFFE6C2, 0xD94F6B, Music.PLAY),
    FEAST("Feast Forever", CreatureType.BIG_MAMA, "Endless silly feasts. How far can you go?", 0xB86BFF, 0xFFE0F5, 0xFFD23F, Music.PLAY),
}

/** Dress-up hats. */
enum class Hat(val title: String) {
    NONE("No hat"), PARTY("Party Hat"), CROWN("Crown"), CHEF("Chef Hat"), TOP_HAT("Top Hat"),
    FLOWER("Flower"), BOW("Big Bow"), PROPELLER("Propeller Cap"), VIKING("Viking Helmet"),
    SUNHAT("Sun Hat"), BEANIE("Beanie"), WIZARD("Wizard Hat"), HEADPHONES("Headphones"),
    CHICK("Baby Chick"), HALO("Halo"),
}

/** Creature-specific surprises: these combos do something different from the item's usual reaction. */
object Surprises {
    private val map = HashMap<Long, ReactionType>()

    private fun key(c: CreatureType, i: ItemType) = (c.ordinal.toLong() shl 16) or i.ordinal.toLong()

    init {
        put(CreatureType.MUNCHY, ItemType.PIZZA, ReactionType.HEART_EYES)
        put(CreatureType.GULPO, ItemType.SOCK, ReactionType.HEART_EYES)
        put(CreatureType.GULPO, ItemType.CUPCAKE, ReactionType.SPINNY_EYES)
        put(CreatureType.SLURPO, ItemType.FISH, ReactionType.HEART_EYES)
        put(CreatureType.SLURPO, ItemType.CHILI, ReactionType.ROCKET)
        put(CreatureType.BLORP, ItemType.TOY_BLOCK, ReactionType.HEART_EYES)
        put(CreatureType.BLORP, ItemType.SOCK, ReactionType.CONFETTI_SNEEZE)
        put(CreatureType.CHOMPETTE, ItemType.CHILI, ReactionType.HEART_EYES)
        put(CreatureType.CHOMPETTE, ItemType.ICE_CUBE, ReactionType.BUBBLE_BURP)
        put(CreatureType.GRUMBO, ItemType.ICE_CUBE, ReactionType.GIGGLES)
        put(CreatureType.GRUMBO, ItemType.FEATHER, ReactionType.SUPER_SIZE)
        put(CreatureType.FIZZLE, ItemType.SODA, ReactionType.HEART_EYES)
        put(CreatureType.FIZZLE, ItemType.PEPPER, ReactionType.ROCKET)
        put(CreatureType.SNOOZLE, ItemType.PILLOW, ReactionType.HEART_EYES)
        put(CreatureType.SNOOZLE, ItemType.ALARM_CLOCK, ReactionType.BALLOON_FLOAT)
        put(CreatureType.TWIRLZ, ItemType.LOLLIPOP, ReactionType.HEART_EYES)
        put(CreatureType.TWIRLZ, ItemType.TRUMPET, ReactionType.SPARKLE_DANCE)
        put(CreatureType.BIG_MAMA, ItemType.CUPCAKE, ReactionType.SUPER_SIZE)
    }

    private fun put(c: CreatureType, i: ItemType, r: ReactionType) { map[key(c, i)] = r }

    fun reaction(c: CreatureType, i: ItemType): ReactionType = map[key(c, i)] ?: i.reaction
}
