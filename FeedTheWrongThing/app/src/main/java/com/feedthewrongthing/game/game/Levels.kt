package com.feedthewrongthing.game.game

import com.feedthewrongthing.game.game.ItemType.ALARM_CLOCK
import com.feedthewrongthing.game.game.ItemType.BALLOON
import com.feedthewrongthing.game.game.ItemType.BANANA
import com.feedthewrongthing.game.game.ItemType.BROCCOLI
import com.feedthewrongthing.game.game.ItemType.CARROT
import com.feedthewrongthing.game.game.ItemType.CHEESE
import com.feedthewrongthing.game.game.ItemType.CHILI
import com.feedthewrongthing.game.game.ItemType.CUPCAKE
import com.feedthewrongthing.game.game.ItemType.DONUT
import com.feedthewrongthing.game.game.ItemType.FEATHER
import com.feedthewrongthing.game.game.ItemType.FISH
import com.feedthewrongthing.game.game.ItemType.ICE_CUBE
import com.feedthewrongthing.game.game.ItemType.JELLY
import com.feedthewrongthing.game.game.ItemType.LOLLIPOP
import com.feedthewrongthing.game.game.ItemType.PEPPER
import com.feedthewrongthing.game.game.ItemType.PILLOW
import com.feedthewrongthing.game.game.ItemType.PIZZA
import com.feedthewrongthing.game.game.ItemType.RUBBER_DUCK
import com.feedthewrongthing.game.game.ItemType.SNEAKER
import com.feedthewrongthing.game.game.ItemType.SOCK
import com.feedthewrongthing.game.game.ItemType.SODA
import com.feedthewrongthing.game.game.ItemType.TOOTHBRUSH
import com.feedthewrongthing.game.game.ItemType.TOY_BLOCK
import com.feedthewrongthing.game.game.ItemType.TRUMPET
import com.feedthewrongthing.game.game.ItemType.WATERMELON
import kotlin.random.Random

/** What the player must do to finish a level. */
sealed class Goal(val count: Int) {
    class Feed(n: Int, val target: Int = -1) : Goal(n)
    class FeedItem(val item: ItemType, n: Int, val target: Int = -1) : Goal(n)
    class Requests(n: Int) : Goal(n)
    class Wrong(n: Int) : Goal(n)
    class React(val type: ReactionType, n: Int, val target: Int = -1) : Goal(n)
    class Discover(n: Int) : Goal(n)
    class Combo(n: Int) : Goal(n)
    class Everyone(n: Int) : Goal(n)
    class Order(val items: List<ItemType>) : Goal(items.size)
    class Wake(n: Int) : Goal(n)
    class Beat(n: Int) : Goal(n)
}

enum class GadgetKind { WALL, TRAMPOLINE, FAN, BUMPER, PLATFORM }

/** Stage furniture. Boxes use half sizes [w],[d]; [h] is height; fans push with ([fx],[fy],[fz]). */
class Gadget(
    val kind: GadgetKind, val x: Float, val z: Float, val w: Float, val d: Float = w, val h: Float = 1f,
    val fx: Float = 0f, val fy: Float = 0f, val fz: Float = 0f,
)

class Spawn(
    val type: CreatureType, val x: Float, val z: Float, val move: Move = Move.STATIC,
    val sleep: Boolean = false, val beat: Boolean = false, val scale: Float = 1f, val elev: Float = 0f,
)

class LevelDef(
    val island: Island,
    val name: String,
    val goal: Goal,
    val creatures: List<Spawn>,
    val tray: List<ItemType>,
    val par: Int,
    val gadgets: List<Gadget> = emptyList(),
    val wind: Float = 0f,
    val conveyor: Boolean = false,
    val tip: String? = null,
    val tutorial: Boolean = false,
    val endlessIndex: Int = -1,
)

object Levels {
    const val PER_ISLAND = 8

    private fun c(t: CreatureType, x: Float, z: Float, m: Move = Move.STATIC, sleep: Boolean = false, beat: Boolean = false,
                  scale: Float = 1f, elev: Float = 0f) = Spawn(t, x, z, m, sleep, beat, scale, elev)

    private fun slide(a: Float, s: Float = 0.7f, ph: Float = 0f) = Move(MoveKind.SLIDE, a, s, ph)
    private fun circle(a: Float, s: Float = 0.6f) = Move(MoveKind.CIRCLE, a, s)
    private fun hop(a: Float, s: Float = 0.8f) = Move(MoveKind.HOP, a, s)
    private fun peek(ph: Float = 0f) = Move(MoveKind.PEEK, 0f, 1f, ph)
    private fun fly(a: Float, s: Float = 0.7f) = Move(MoveKind.FLY, a, s)
    private fun bob(a: Float, s: Float = 0.8f) = Move(MoveKind.BOB, a, s)
    private fun wander(a: Float, s: Float = 0.5f) = Move(MoveKind.WANDER, a, s)

    private fun wall(x: Float, z: Float, w: Float, h: Float, d: Float = 0.3f) = Gadget(GadgetKind.WALL, x, z, w, d, h)
    private fun tramp(x: Float, z: Float, r: Float = 0.9f) = Gadget(GadgetKind.TRAMPOLINE, x, z, r, r, 0.35f)
    private fun bumper(x: Float, z: Float, r: Float = 0.55f, h: Float = 2.6f) = Gadget(GadgetKind.BUMPER, x, z, r, r, h)
    private fun fan(x: Float, z: Float, fx: Float, fy: Float, fz: Float, w: Float = 1f, d: Float = 1f, h: Float = 3.5f) =
        Gadget(GadgetKind.FAN, x, z, w, d, h, fx, fy, fz)
    private fun platform(x: Float, z: Float, h: Float, w: Float = 1f) = Gadget(GadgetKind.PLATFORM, x, z, w, w, h)

    private val M = CreatureType.MUNCHY
    private val G = CreatureType.GULPO
    private val S = CreatureType.SLURPO
    private val B = CreatureType.BLORP
    private val C = CreatureType.CHOMPETTE
    private val R = CreatureType.GRUMBO
    private val F = CreatureType.FIZZLE
    private val Z = CreatureType.SNOOZLE
    private val T = CreatureType.TWIRLZ

    val all: List<LevelDef> = buildList {
        val I = Island.PICNIC
        add(LevelDef(I, "First Bite", Goal.Feed(3), listOf(c(M, 0f, -4.6f)), listOf(PIZZA, CUPCAKE, BANANA), 4, tutorial = true,
            tip = "Drag a snack UP and let go to throw it to Munchy!"))
        add(LevelDef(I, "Silly Snacks", Goal.Feed(4), listOf(c(M, 0f, -4.8f)), listOf(SOCK, PIZZA, RUBBER_DUCK, FISH), 5,
            tip = "Munchy eats ANYTHING. Try the silly stuff!"))
        add(LevelDef(I, "Make It Sneeze", Goal.React(ReactionType.CONFETTI_SNEEZE, 1), listOf(c(M, 0f, -4.8f)), listOf(PIZZA, PEPPER, BANANA, SOCK), 3,
            tip = "Which thing makes Munchy sneeze? Experiment!"))
        add(LevelDef(I, "Silly Discoveries", Goal.Discover(4), listOf(c(M, 0f, -5f)), listOf(PIZZA, SOCK, RUBBER_DUCK, FISH, CHILI), 5))
        add(LevelDef(I, "Sliding Munchy", Goal.Feed(4), listOf(c(M, 0f, -4.8f, slide(2.2f, 0.8f))), listOf(CUPCAKE, PIZZA, TOOTHBRUSH, BANANA), 6,
            tip = "Munchy is on the move. Aim where it's going!"))
        add(LevelDef(I, "Far Away Feast", Goal.Feed(3), listOf(c(M, 0f, -6.6f)), listOf(DONUT, PIZZA, SOCK), 4,
            tip = "Drag further for a longer throw!"))
        add(LevelDef(I, "Spicy Surprise", Goal.React(ReactionType.FIRE_BREATH, 2), listOf(c(M, 0f, -5f, slide(1.2f, 0.6f))),
            listOf(CHILI, ICE_CUBE, PIZZA, LOLLIPOP), 3))
        add(LevelDef(I, "Picnic Party", Goal.Discover(6), listOf(c(M, 0f, -5f, circle(1.5f, 0.7f))),
            listOf(PIZZA, SOCK, CHILI, RUBBER_DUCK, TOOTHBRUSH, BALLOON), 7))
    } + buildList {
        val I = Island.SOCKS
        add(LevelDef(I, "Dream Snacks", Goal.Requests(3), listOf(c(G, 0f, -4.8f)), listOf(SOCK, FISH, PIZZA), 4,
            tip = "See the thought bubble? Feed Gulpo what it's dreaming of!"))
        add(LevelDef(I, "Feed the WRONG Thing!", Goal.Wrong(3), listOf(c(G, 0f, -4.8f)), listOf(SOCK, PIZZA, CHEESE, BANANA), 4,
            tip = "Gulpo wants something... so give it ANYTHING ELSE!"))
        add(LevelDef(I, "Two Dreamers", Goal.Requests(4), listOf(c(G, -2f, -5f), c(M, 2f, -4.6f)), listOf(SOCK, PIZZA, CUPCAKE, FISH), 6))
        add(LevelDef(I, "Sock Hop", Goal.Requests(3), listOf(c(G, 0f, -5f, hop(2f, 0.9f))), listOf(SOCK, SNEAKER, CHEESE, DONUT), 5))
        add(LevelDef(I, "Wrong Wrong Wrong", Goal.Wrong(5), listOf(c(G, 1f, -5f, slide(2f, 0.7f)), c(M, -3f, -4.2f)),
            listOf(SOCK, PIZZA, FISH, CARROT, CHEESE), 7))
        add(LevelDef(I, "Cheesy Tiny", Goal.React(ReactionType.SHRINK, 2), listOf(c(G, 0f, -5f)), listOf(CHEESE, SOCK, BROCCOLI, CARROT), 3))
        add(LevelDef(I, "Bunny Business", Goal.React(ReactionType.BUNNY_EARS, 2), listOf(c(G, -1.8f, -5f), c(M, 1.8f, -4.8f)),
            listOf(CARROT, SOCK, CUPCAKE, PIZZA), 3))
        add(LevelDef(I, "Cave Feast", Goal.Order(listOf(SOCK, PIZZA, CARROT, CHEESE)), listOf(c(G, 0f, -5.2f, slide(1.6f, 0.6f))),
            listOf(CHEESE, SOCK, PIZZA, CARROT), 5, tip = "Feed them in the order shown at the top!"))
    } + buildList {
        val I = Island.BATH
        add(LevelDef(I, "Splash Down", Goal.Feed(3), listOf(c(S, 0f, -4.8f, bob(1.5f))), listOf(FISH, RUBBER_DUCK, SOCK), 4))
        add(LevelDef(I, "Bubble Trouble", Goal.React(ReactionType.BUBBLE_BURP, 2), listOf(c(G, 2f, -5f), c(S, -1.5f, -4.8f, bob(1.2f))),
            listOf(FISH, PIZZA, ICE_CUBE, LOLLIPOP), 3, tip = "Slurpo LOVES fish... so who will burp bubbles?"))
        add(LevelDef(I, "Drifting Pals", Goal.Everyone(2), listOf(c(S, -2f, -5.2f, bob(2f, 0.7f)), c(M, 2.5f, -4f)),
            listOf(FISH, PIZZA, DONUT, JELLY), 5))
        add(LevelDef(I, "Blast Off!", Goal.React(ReactionType.ROCKET, 1), listOf(c(S, 0f, -5f, bob(1f))), listOf(SODA, FISH, BALLOON, CHILI), 2))
        add(LevelDef(I, "Rubber Ducky Rush", Goal.Combo(3), listOf(c(S, 0f, -4.6f, bob(1f, 0.6f))), listOf(RUBBER_DUCK, FISH, JELLY, SOCK), 5,
            tip = "Feed three snacks super fast for a COMBO!"))
        add(LevelDef(I, "Far Bubbles", Goal.Feed(4), listOf(c(S, 0f, -6.8f, bob(2.5f, 0.6f))), listOf(FISH, CUPCAKE, PIZZA, BALLOON), 6))
        add(LevelDef(I, "Wobbly Jelly", Goal.React(ReactionType.JELLY_WOBBLE, 2), listOf(c(S, -1.6f, -5f, bob(1f)), c(G, 2f, -5.4f)),
            listOf(JELLY, FISH, ICE_CUBE, SOCK), 3))
        add(LevelDef(I, "Bath Time Bash", Goal.Discover(6), listOf(c(S, 0f, -5f, bob(1.5f)), c(M, -3f, -4.2f, slide(0.8f)), c(G, 3f, -5.6f)),
            listOf(FISH, RUBBER_DUCK, SODA, JELLY, ICE_CUBE, PEPPER), 7))
    } + buildList {
        val I = Island.TOYS
        add(LevelDef(I, "Block Party", Goal.Feed(3), listOf(c(B, 0f, -5f)), listOf(TOY_BLOCK, PIZZA, CUPCAKE), 4,
            gadgets = listOf(wall(0f, -3f, 2.4f, 0.9f)), tip = "Throw OVER the block wall!"))
        add(LevelDef(I, "Trampoline Treat", Goal.Feed(3), listOf(c(B, 0f, -5.6f, elev = 1.8f)), listOf(DONUT, TOY_BLOCK, PIZZA), 4,
            gadgets = listOf(platform(0f, -5.6f, 1.8f), tramp(0f, -2.2f, 1.0f)), tip = "Bounce snacks off the trampoline!"))
        add(LevelDef(I, "Peekaboo", Goal.Feed(3), listOf(c(B, 0f, -5f, peek())), listOf(CUPCAKE, SOCK, TOY_BLOCK), 5,
            gadgets = listOf(wall(0f, -3.2f, 2.4f, 1.0f, 0.35f)), tip = "Wait for Blorp to pop up!"))
        add(LevelDef(I, "Blocky Buddy", Goal.React(ReactionType.BLOCKY, 2), listOf(c(B, -1.8f, -5f, slide(1.2f, 0.6f)), c(M, 2.2f, -4.8f)),
            listOf(TOY_BLOCK, BALLOON, CUPCAKE, SNEAKER), 3, tip = "Blorp LOVES blocks... so who will turn blocky?"))
        add(LevelDef(I, "Two Towers", Goal.Everyone(1), listOf(c(B, -2.5f, -5.5f, elev = 1.4f), c(M, 2.5f, -5.5f, elev = 2.0f)),
            listOf(PIZZA, TOY_BLOCK, DONUT, SOCK), 4,
            gadgets = listOf(platform(-2.5f, -5.5f, 1.4f), platform(2.5f, -5.5f, 2.0f), tramp(-1.3f, -2.2f, 0.8f), tramp(1.3f, -2.2f, 0.8f))))
        add(LevelDef(I, "Wall Maze", Goal.Feed(4), listOf(c(B, 0f, -5.4f, slide(2.5f, 0.5f))), listOf(CUPCAKE, PIZZA, TOY_BLOCK, BANANA), 6,
            gadgets = listOf(wall(-1.6f, -3.2f, 1.3f, 1.6f), wall(1.8f, -3.6f, 1.3f, 1.6f))))
        add(LevelDef(I, "Balloon Bop", Goal.React(ReactionType.BALLOON_FLOAT, 2), listOf(c(B, -1.8f, -5f), c(G, 2f, -5.2f)),
            listOf(BALLOON, TOY_BLOCK, SOCK, CHEESE), 3))
        add(LevelDef(I, "Toy Box Bonanza", Goal.Order(listOf(TOY_BLOCK, BALLOON, CUPCAKE, TRUMPET)), listOf(c(B, 0f, -5.4f, peek(1f))),
            listOf(TRUMPET, CUPCAKE, BALLOON, TOY_BLOCK), 6, gadgets = listOf(wall(0f, -3.4f, 2.4f, 1.0f, 0.35f), tramp(-2.6f, -2f, 0.8f))))
    } + buildList {
        val I = Island.KITCHEN
        add(LevelDef(I, "Hungry Dragon", Goal.Feed(3), listOf(c(C, 0f, -5f, fly(1.5f))), listOf(PIZZA, CHILI, DONUT, CUPCAKE), 4,
            conveyor = true, tip = "Snacks ride the conveyor belt. Grab one and throw!"))
        add(LevelDef(I, "Dragon in Love", Goal.React(ReactionType.HEART_EYES, 1, 0), listOf(c(C, 0f, -5f, fly(1.2f))),
            listOf(CHILI, PIZZA, ICE_CUBE, SOCK, CARROT), 3, conveyor = true, tip = "What does a dragon love most?"))
        add(LevelDef(I, "Conveyor Combo", Goal.Combo(3), listOf(c(C, 1.5f, -5f, fly(1f)), c(M, -2f, -4.4f)),
            listOf(PIZZA, BANANA, DONUT, CUPCAKE, WATERMELON), 5, conveyor = true))
        add(LevelDef(I, "Seed Spitter", Goal.React(ReactionType.SEED_SPIT, 2), listOf(c(C, 0f, -5f, fly(1.5f))),
            listOf(WATERMELON, CHILI, SODA, PIZZA), 3, conveyor = true))
        add(LevelDef(I, "Dragon Dreams", Goal.Requests(4), listOf(c(C, 0f, -5.2f, fly(2.2f, 0.6f))),
            listOf(PIZZA, CHILI, DONUT, CARROT, CHEESE), 6, conveyor = true))
        add(LevelDef(I, "Kitchen Fan", Goal.Feed(3), listOf(c(C, 2.6f, -5.6f, elev = 1.6f)), listOf(CUPCAKE, PIZZA, FEATHER), 5,
            gadgets = listOf(platform(2.6f, -5.6f, 1.6f), fan(-0.8f, -2.6f, 9f, 10f, 0f, h = 3.8f)), tip = "The fan blows snacks up and over!"))
        add(LevelDef(I, "Ice Ice Dragon", Goal.React(ReactionType.BUBBLE_BURP, 1, 0), listOf(c(C, 0f, -5f, fly(1.2f))),
            listOf(ICE_CUBE, SODA, CHILI, PIZZA), 3, conveyor = true))
        add(LevelDef(I, "Chef's Feast", Goal.Order(listOf(PIZZA, CHILI, CUPCAKE, WATERMELON, DONUT)), listOf(c(C, 0f, -5.2f, fly(1.6f))),
            listOf(DONUT, WATERMELON, CUPCAKE, CHILI, PIZZA), 7, conveyor = true))
    } + buildList {
        val I = Island.SNOW
        add(LevelDef(I, "Grumpy Grumbo", Goal.Feed(3), listOf(c(R, 0f, -5f)), listOf(ICE_CUBE, PIZZA, BROCCOLI), 4, wind = 1.2f,
            tip = "Whoosh! The wind pushes snacks. Aim a little to the side!"))
        add(LevelDef(I, "Make Him Giggle", Goal.React(ReactionType.GIGGLES, 2, 0), listOf(c(R, 0f, -5f)),
            listOf(FEATHER, ICE_CUBE, SOCK, CARROT), 3, wind = -1.5f, tip = "Grumbo is grumpy... what makes him giggle?"))
        add(LevelDef(I, "Snowball Slide", Goal.Feed(4), listOf(c(R, 0f, -5f, slide(2.2f, 0.6f))), listOf(BROCCOLI, PIZZA, ICE_CUBE, CHEESE), 6, wind = 1.8f))
        add(LevelDef(I, "Brrr Freeze", Goal.React(ReactionType.FROZEN, 2), listOf(c(M, -2f, -4.8f), c(R, 2f, -5.2f)),
            listOf(ICE_CUBE, CHILI, PIZZA, FEATHER), 3, wind = -1f))
        add(LevelDef(I, "Hot Cocoa", Goal.Requests(4), listOf(c(R, 1.8f, -5.2f), c(M, -2f, -4.6f)), listOf(CHILI, ICE_CUBE, BROCCOLI, CARROT, CHEESE), 6, wind = 2f))
        add(LevelDef(I, "Blizzard", Goal.Feed(4), listOf(c(R, 0f, -5.4f, hop(2f, 0.6f))), listOf(PIZZA, CUPCAKE, ICE_CUBE, SOCK), 7, wind = 2.6f))
        add(LevelDef(I, "Mountain Mix-up", Goal.Wrong(4), listOf(c(R, 1.5f, -5.2f, slide(1.5f, 0.5f)), c(G, -2.5f, -4.6f)),
            listOf(ICE_CUBE, SOCK, PIZZA, CARROT, BROCCOLI), 6, wind = 1.5f))
        add(LevelDef(I, "Peak Party", Goal.Discover(6), listOf(c(R, 0f, -5.2f, circle(1.8f, 0.5f)), c(M, -3.2f, -4.2f)),
            listOf(ICE_CUBE, FEATHER, BROCCOLI, CHILI, CARROT, CHEESE), 9, wind = 1.2f))
    } + buildList {
        val I = Island.FACTORY
        add(LevelDef(I, "Fizzy Friend", Goal.Feed(3), listOf(c(F, 0f, -5f)), listOf(SODA, PIZZA, PEPPER), 4,
            gadgets = listOf(bumper(-2.8f, -3.6f), bumper(2.8f, -3.6f))))
        add(LevelDef(I, "Bank Shot", Goal.Feed(3), listOf(c(F, 0f, -5.4f)), listOf(SODA, CUPCAKE, PIZZA), 5,
            gadgets = listOf(wall(0f, -3.2f, 1.2f, 1.5f), bumper(-2.6f, -3.6f, 0.6f, 3f), bumper(2.6f, -3.6f, 0.6f, 3f)),
            tip = "Bounce off the bouncy bumpers!"))
        add(LevelDef(I, "Up the Fan", Goal.Feed(3), listOf(c(F, 0f, -5.8f, elev = 2.4f)), listOf(FEATHER, BALLOON, PIZZA, SODA), 5,
            gadgets = listOf(platform(0f, -5.8f, 2.4f), fan(0f, -2.8f, 0f, 16f, -2f, w = 1.2f, d = 1f, h = 3.5f))))
        add(LevelDef(I, "Rocket Fizz", Goal.React(ReactionType.ROCKET, 2), listOf(c(F, -1.6f, -5f), c(M, 2f, -5f)),
            listOf(SODA, PEPPER, CHILI, ICE_CUBE), 3))
        add(LevelDef(I, "Assembly Line", Goal.Everyone(1), listOf(c(F, -2.8f, -5f, slide(0.8f)), c(M, 0f, -5.6f, slide(1f, 0.9f)), c(G, 2.8f, -5f, slide(0.8f, 0.6f))),
            listOf(SODA, PIZZA, SOCK, CUPCAKE, FISH), 5, conveyor = true))
        add(LevelDef(I, "Combo Machine", Goal.Combo(4), listOf(c(F, -1.4f, -4.8f), c(M, 1.4f, -4.8f)), listOf(SODA, PIZZA, DONUT, BANANA, CUPCAKE), 6))
        add(LevelDef(I, "Fan Frenzy", Goal.Feed(4), listOf(c(F, 0f, -5.4f, slide(2f, 0.6f))), listOf(PIZZA, SODA, CUPCAKE, PEPPER), 7,
            gadgets = listOf(fan(-1.8f, -3f, 7f, 2f, 0f, h = 4f), bumper(3f, -3.6f))))
        add(LevelDef(I, "Factory Feast", Goal.Order(listOf(SODA, PEPPER, PIZZA, ICE_CUBE, FISH)), listOf(c(F, 0f, -5.2f, circle(1.5f, 0.5f))),
            listOf(FISH, ICE_CUBE, PIZZA, PEPPER, SODA), 7, gadgets = listOf(bumper(-3f, -3.4f), bumper(3f, -3.4f))))
    } + buildList {
        val I = Island.MOON
        add(LevelDef(I, "Shhh... Wake Up!", Goal.Wake(2), listOf(c(Z, 0f, -5f, sleep = true)), listOf(ALARM_CLOCK, PILLOW, PIZZA, CUPCAKE), 4,
            tip = "Sleepyheads can't eat! Throw something NOISY near them."))
        add(LevelDef(I, "Pillow Fight", Goal.React(ReactionType.SLEEPY, 2), listOf(c(M, -1.8f, -4.8f), c(G, 2f, -5.2f)), listOf(PILLOW, TRUMPET, SOCK), 3))
        add(LevelDef(I, "Sleepy Pair", Goal.Wake(3), listOf(c(Z, -1.8f, -5.2f, sleep = true), c(M, 2f, -4.6f, sleep = true)),
            listOf(TRUMPET, ALARM_CLOCK, CUPCAKE, DONUT), 6))
        add(LevelDef(I, "Toot Toot", Goal.React(ReactionType.TOOT, 2), listOf(c(Z, 0f, -5f, sleep = true)), listOf(TRUMPET, PILLOW, CUPCAKE, FEATHER), 3))
        add(LevelDef(I, "Midnight Snack", Goal.Requests(3), listOf(c(Z, -1.8f, -5f, sleep = true), c(G, 2f, -5.2f, sleep = true)),
            listOf(PILLOW, SOCK, ALARM_CLOCK, CUPCAKE, PIZZA), 6))
        add(LevelDef(I, "Sleepwalker", Goal.Wake(2), listOf(c(Z, 0f, -5.2f, slide(1.8f, 0.4f), sleep = true)), listOf(ALARM_CLOCK, PIZZA, LOLLIPOP), 4))
        add(LevelDef(I, "Snooze Alarm", Goal.Everyone(1), listOf(c(Z, 0f, -5.6f, sleep = true), c(M, -2.8f, -4.6f, sleep = true), c(B, 2.8f, -4.6f, sleep = true)),
            listOf(ALARM_CLOCK, TRUMPET, RUBBER_DUCK, PIZZA, CUPCAKE), 6))
        add(LevelDef(I, "Pajama Party", Goal.Discover(6), listOf(c(Z, 0f, -5.4f, sleep = true), c(M, -2.8f, -4.4f), c(C, 2.6f, -5f, fly(0.8f))),
            listOf(PILLOW, TRUMPET, ALARM_CLOCK, LOLLIPOP, FEATHER, CUPCAKE), 8))
    } + buildList {
        val I = Island.CARNIVAL
        add(LevelDef(I, "Beat Muncher", Goal.Beat(3), listOf(c(T, 0f, -5f, beat = true)), listOf(LOLLIPOP, PIZZA, DONUT), 4,
            tip = "Twirlz opens wide on the beat. Time your throw!"))
        add(LevelDef(I, "Rhythm Pals", Goal.Beat(4), listOf(c(T, 1.8f, -5f, beat = true), c(M, -2.2f, -4.8f, beat = true)),
            listOf(LOLLIPOP, CUPCAKE, PIZZA, BANANA), 5))
        add(LevelDef(I, "Twirly Rainbow", Goal.React(ReactionType.RAINBOW, 1), listOf(c(T, 1.8f, -5f, beat = true), c(M, -2f, -4.8f)),
            listOf(LOLLIPOP, TRUMPET, DONUT, PIZZA), 3))
        add(LevelDef(I, "Dance Floor", Goal.React(ReactionType.SPARKLE_DANCE, 2), listOf(c(T, 0f, -5f, beat = true)),
            listOf(TRUMPET, TOOTHBRUSH, LOLLIPOP, BANANA), 3))
        add(LevelDef(I, "Carousel", Goal.Beat(4), listOf(c(T, 0f, -5.2f, circle(1.6f, 0.5f), beat = true)), listOf(LOLLIPOP, DONUT, CUPCAKE, PIZZA), 6))
        add(LevelDef(I, "Hula Show", Goal.React(ReactionType.HULA_HOOP, 3), listOf(c(T, 0f, -5.4f, beat = true), c(M, -2.8f, -4.6f), c(G, 2.8f, -4.8f)),
            listOf(DONUT, LOLLIPOP, SOCK, PIZZA), 4))
        add(LevelDef(I, "Grand Parade", Goal.Everyone(1), listOf(c(T, 0f, -5.6f, beat = true), c(M, -2.8f, -4.6f, slide(0.8f)),
            c(C, 2.8f, -5f, fly(0.7f)), c(S, -1.2f, -6.6f)), listOf(LOLLIPOP, PIZZA, CHILI, FISH, CUPCAKE, DONUT), 6))
        add(LevelDef(I, "The Great Big Feast", Goal.Requests(5), listOf(c(CreatureType.BIG_MAMA, 0f, -6f, slide(1.6f, 0.45f), scale = 1.6f)),
            listOf(CUPCAKE, PIZZA, DONUT, SOCK, RUBBER_DUCK, LOLLIPOP), 8, gadgets = listOf(bumper(-3.4f, -3.6f), bumper(3.4f, -3.6f)),
            tip = "Big Mama Munch is HUNGRY. Feed her every wish!"))
    }

    val islandCount: Int get() = all.size / PER_ISLAND

    fun islandOf(index: Int): Island = if (index >= all.size) Island.FEAST else all[index].island

    /** Generates an endless "Feast Forever" level. Deterministic per index. */
    fun endless(k: Int): LevelDef {
        val r = Random(9000 + k * 31)
        val islands = Island.entries.filter { it != Island.FEAST }
        val look = if (k % 4 == 3) Island.FEAST else islands[r.nextInt(islands.size)]
        val pool = CreatureType.entries.filter { it != CreatureType.BIG_MAMA }
        val n = 1 + r.nextInt(if (k < 5) 2 else 3)
        val xs = when (n) { 1 -> listOf(0f); 2 -> listOf(-2f, 2f); else -> listOf(-2.8f, 0f, 2.8f) }
        val creatures = xs.map { x ->
            val t = pool[r.nextInt(pool.size)]
            val mk = r.nextInt(5)
            val mv = when {
                k < 2 || mk == 0 -> Move.STATIC
                mk == 1 -> slide(0.6f + r.nextFloat() * (if (n == 1) 2f else 0.8f), 0.4f + r.nextFloat() * 0.5f)
                mk == 2 -> circle(0.6f + r.nextFloat() * 0.8f, 0.4f + r.nextFloat() * 0.4f)
                mk == 3 && t == CreatureType.CHOMPETTE -> fly(0.8f)
                else -> wander(0.6f + r.nextFloat(), 0.4f)
            }
            c(t, x, -4.6f - r.nextFloat() * 1.6f, mv, sleep = look == Island.MOON && r.nextFloat() < 0.5f,
                beat = look == Island.CARNIVAL && r.nextFloat() < 0.5f)
        }
        val items = ItemType.entries.shuffled(r).take(4 + r.nextInt(3)).toMutableList()
        if (creatures.any { it.sleep } && items.none { it.noisy }) items[0] = ALARM_CLOCK
        val goal = when (r.nextInt(7)) {
            0 -> Goal.Feed(3 + k / 4 % 4)
            1 -> Goal.Requests(3 + k / 6 % 3)
            2 -> Goal.Wrong(3 + k / 6 % 3)
            3 -> Goal.Discover(3 + k / 5 % 3)
            4 -> if (creatures.size > 1) Goal.Combo(3) else Goal.Discover(3)
            5 -> if (creatures.size > 1) Goal.Everyone(1) else Goal.Feed(4)
            else -> {
                val item = items[r.nextInt(items.size)]
                val tgt = r.nextInt(creatures.size)
                Goal.React(Surprises.reaction(creatures[tgt].type, item), 1 + r.nextInt(2), tgt)
            }
        }
        val finalGoal = if (creatures.any { it.sleep } && goal !is Goal.React) Goal.Wake(goal.count) else
            if (creatures.any { it.beat } && goal is Goal.Feed) Goal.Beat(goal.count) else goal
        val gadgets = ArrayList<Gadget>()
        if (k >= 3 && r.nextFloat() < 0.35f) { gadgets.add(bumper(-3.2f, -3.4f)); gadgets.add(bumper(3.2f, -3.4f)) }
        val wind = if (look == Island.SNOW) (r.nextFloat() - 0.5f) * 3f else 0f
        return LevelDef(look, "Feast #${k + 1}", finalGoal, creatures, items, finalGoal.count + 2 + n, gadgets, wind,
            conveyor = look == Island.KITCHEN, endlessIndex = k)
    }
}
