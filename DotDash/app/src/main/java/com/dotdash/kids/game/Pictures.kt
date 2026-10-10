package com.dotdash.kids.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// All 60 pictures. Every picture lives in a 100×100 box.
//  - `pts` is the outline: the dots follow it in order and the reveal fills it.
//  - `art` is the detail layer (eyes, wheels, stripes…) drawn on top at the reveal.
//  - `voice` names the synthesised sound the picture makes when it comes alive.

sealed interface Art
data class ArtCircle(val x: Float, val y: Float, val r: Float, val color: String) : Art
data class ArtOval(val x: Float, val y: Float, val rx: Float, val ry: Float, val color: String) : Art
data class ArtFill(val d: String, val color: String) : Art
data class ArtLine(val d: String, val color: String, val width: Float) : Art
data class ArtRect(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float, val color: String) : Art
data class ArtText(val x: Float, val y: Float, val text: String, val color: String, val size: Float) : Art
data class ArtStar(val x: Float, val y: Float, val r: Float, val color: String) : Art
data class ArtEye(val x: Float, val y: Float, val s: Float) : Art

class Picture(
    val id: String,
    val name: String,
    val fill: String,
    val voice: String,
    val pts: List<Pt>,
    val art: List<Art>,
)

data class Pt(val x: Float, val y: Float)

const val INK = "#241C5C"
private const val PINK = "#FF9AA2"
private const val WHITE = "#FFFFFF"
private const val GLASS = "#BDE8FF"

private fun Number.f() = toFloat()
private fun c(x: Number, y: Number, r: Number, f: String) = listOf(ArtCircle(x.f(), y.f(), r.f(), f))
private fun e(x: Number, y: Number, rx: Number, ry: Number, f: String) = listOf(ArtOval(x.f(), y.f(), rx.f(), ry.f(), f))
private fun p(d: String, f: String) = listOf(ArtFill(d, f))
private fun l(d: String, s: String, w: Number = 1.6) = listOf(ArtLine(d, s, w.f()))
private fun r(x: Number, y: Number, w: Number, h: Number, rx: Number, f: String) = listOf(ArtRect(x.f(), y.f(), w.f(), h.f(), rx.f(), f))
private fun t(x: Number, y: Number, text: String, f: String, size: Number) = listOf(ArtText(x.f(), y.f(), text, f, size.f()))
private fun eye(x: Number, y: Number, s: Number = 1) = listOf(ArtEye(x.f(), y.f(), s.f()))
private fun star(x: Number, y: Number, rad: Number, f: String) = listOf(ArtStar(x.f(), y.f(), rad.f(), f))
private fun wheel(x: Number, y: Number, rad: Number = 9) = c(x, y, rad, INK) + c(x, y, rad.f() * 0.45f, "#D9DEF5")
private fun cheek(x: Number, y: Number) = e(x, y, 3.5, 2.2, PINK)
private fun smile(x: Number, y: Number, w: Number = 4): List<Art> {
    val xf = x.f(); val yf = y.f(); val wf = w.f()
    return l("M${xf - wf} $yf Q$xf ${yf + wf * 0.8f} ${xf + wf} $yf", INK, 1.4)
}
private fun art(vararg parts: List<Art>): List<Art> = parts.flatMap { it }
private fun pts(vararg n: Number): List<Pt> = n.toList().chunked(2) { Pt(it[0].f(), it[1].f()) }

// Spiky ring (lion's mane): alternating outer/inner radius.
private fun burst(cx: Number, cy: Number, outer: Number, inner: Number, n: Int): List<Pt> =
    (0 until n * 2).map { i ->
        val a = PI * i / n - PI / 2
        val rad = if (i % 2 == 0) outer.f() else inner.f()
        Pt((cx.f() + rad * cos(a)).roundToInt().f(), (cy.f() + rad * sin(a)).roundToInt().f())
    }

// Egg outline: narrower on top.
private fun eggShape(cx: Number, cy: Number, w: Number, h: Number, n: Int): List<Pt> =
    (0 until n).map { i ->
        val a = 2 * PI * i / n
        val top = cos(a) > 0
        Pt((cx.f() + w.f() * sin(a) * (if (top) 0.8 else 1.0)).roundToInt().f(), (cy.f() - h.f() * cos(a)).roundToInt().f())
    }

class World(
    val id: String,
    val name: String,
    val short: String,
    val color: String,
    val dark: String,
    val ink: String,
    val bg: String,
    val bg2: String,
    val unlock: Int,
    val heroPicture: String,
    val pictures: List<Picture>,
)

object Pictures {
    val animals = listOf(
        Picture("fish", "Fish", "#5CC8FF", "bubble",
            pts(90, 50, 72, 32, 48, 28, 28, 40, 10, 30, 16, 50, 10, 70, 28, 60, 48, 72, 72, 68),
            art(p("M46 29 Q56 16 66 31 Z", "#2F9BE0"), c(42, 46, 4, "#8FDBFF"), c(34, 56, 3, "#8FDBFF"), c(48, 60, 3, "#8FDBFF"), l("M60 38 Q54 50 60 62", "#2F9BE0", 2), eye(75, 45), l("M83 57 Q87 57 89 53", INK, 1.4), c(94, 36, 2.5, GLASS), c(97, 27, 1.8, GLASS))),
        Picture("turtle", "Turtle", "#8BD346", "happy",
            pts(10, 64, 20, 42, 36, 28, 58, 27, 74, 38, 80, 50, 90, 44, 97, 50, 92, 60, 82, 64, 80, 76, 70, 76, 66, 66, 34, 66, 30, 76, 20, 76, 18, 66),
            art(p("M16 64 Q18 30 46 27 Q74 28 78 62 Z", "#1FA97A"), l("M32 42 L46 36 L60 42 L58 55 L40 57 L32 50 Z", "#7ED957", 1.6), l("M46 36 L46 28 M60 42 L72 40 M58 55 L66 62 M40 57 L34 64 M32 50 L20 50", "#7ED957", 1.4), l("M16 64 L78 62", "#127A57", 2), eye(88, 50, 0.8), smile(91, 56, 2))),
        Picture("snail", "Snail", "#C3A6FF", "happy",
            pts(6, 82, 22, 74, 22, 52, 32, 34, 50, 26, 68, 32, 77, 50, 74, 68, 82, 60, 84, 42, 90, 38, 95, 46, 92, 70, 96, 82),
            art(c(49, 51, 24, "#FF9F1C"), l("M49 51 m-3 0 a3 3 0 1 1 3 3 a7 7 0 1 1 -7 -7 a11 11 0 1 1 11 11 a15 15 0 1 1 -15 -15", "#C2410C", 2), l("M88 40 L84 28 M92 40 L96 30", INK, 1.4), c(84, 27, 2, INK), c(96, 29, 2, INK), eye(89, 48, 0.7), smile(90, 56, 2))),
        Picture("cat", "Kitty", "#FF9F1C", "meow",
            pts(22, 12, 38, 30, 62, 30, 78, 12, 82, 40, 84, 58, 74, 76, 50, 84, 26, 76, 16, 58, 18, 40),
            art(p("M25 20 L34 30 L24 32 Z", "#FFB3C7"), p("M75 20 L66 30 L76 32 Z", "#FFB3C7"), l("M50 32 L50 42 M42 33 L44 41 M58 33 L56 41", "#C2410C", 2.4), eye(38, 52, 1.2), eye(62, 52, 1.2), p("M47 62 L53 62 L50 66 Z", "#FF5D73"), l("M50 66 Q46 71 42 68 M50 66 Q54 71 58 68", INK, 1.4), l("M30 64 L14 62 M30 68 L15 71 M70 64 L86 62 M70 68 L85 71", INK, 1), cheek(32, 63), cheek(68, 63))),
        Picture("owl", "Owl", "#9B6BFF", "hoot",
            pts(26, 12, 40, 24, 60, 24, 74, 12, 78, 34, 82, 56, 76, 76, 62, 88, 38, 88, 24, 76, 18, 56, 22, 34),
            art(e(50, 66, 18, 20, "#E2D3FF"), l("M43 60 l3 3 3 -3 M52 60 l3 3 3 -3 M43 70 l3 3 3 -3 M52 70 l3 3 3 -3", "#9B6BFF", 1.4), p("M22 50 Q14 70 28 82 Q30 66 22 50 Z", "#7B4FE0"), p("M78 50 Q86 70 72 82 Q70 66 78 50 Z", "#7B4FE0"), c(40, 40, 10, WHITE), c(60, 40, 10, WHITE), c(41, 41, 5, INK), c(61, 41, 5, INK), c(43, 39, 1.6, WHITE), c(63, 39, 1.6, WHITE), p("M46 48 L54 48 L50 56 Z", "#FFC93C"), c(42, 89, 3, "#FF8A3D"), c(58, 89, 3, "#FF8A3D"))),
        Picture("penguin", "Penguin", "#3B4A7A", "squeak",
            pts(50, 8, 64, 14, 70, 30, 74, 50, 86, 60, 76, 64, 74, 78, 66, 90, 56, 92, 44, 92, 34, 90, 26, 78, 24, 64, 14, 60, 26, 50, 30, 30, 36, 14),
            art(e(50, 64, 17, 25, WHITE), p("M36 30 Q50 18 64 30 Q64 44 50 44 Q36 44 36 30 Z", WHITE), eye(44, 31, 0.8), eye(56, 31, 0.8), p("M46 36 L54 36 L50 42 Z", "#FF9F1C"), cheek(40, 38), cheek(60, 38), e(42, 93, 6, 3, "#FF9F1C"), e(58, 93, 6, 3, "#FF9F1C"))),
        Picture("rabbit", "Bunny", "#EDEBFF", "squeak",
            pts(34, 6, 42, 8, 44, 34, 56, 34, 58, 8, 66, 6, 68, 36, 76, 46, 78, 62, 72, 76, 80, 90, 20, 90, 28, 76, 22, 62, 24, 46, 32, 36),
            art(p("M37 12 L40 12 L41 32 L36 32 Z", "#FFB3C7"), p("M60 12 L63 12 L64 32 L59 32 Z", "#FFB3C7"), eye(40, 52), eye(60, 52), p("M47 60 L53 60 L50 63 Z", "#FF5D73"), l("M50 63 L50 66 M50 66 Q46 69 44 67 M50 66 Q54 69 56 67", INK, 1.2), r(47.5, 66.5, 5, 4, 1, WHITE), l("M34 60 L22 58 M34 64 L23 66 M66 60 L78 58 M66 64 L77 66", INK, 1), cheek(33, 58), cheek(67, 58), e(50, 84, 14, 7, WHITE))),
        Picture("pig", "Piggy", "#FFB3C7", "oink",
            pts(20, 30, 26, 14, 38, 24, 62, 24, 74, 14, 80, 30, 86, 48, 82, 68, 68, 82, 50, 86, 32, 82, 18, 68, 14, 48),
            art(p("M26 20 L34 26 L24 30 Z", "#FF8FA3"), p("M74 20 L66 26 L76 30 Z", "#FF8FA3"), eye(38, 46), eye(62, 46), e(50, 61, 12, 8, "#FF8FA3"), e(46, 61, 2, 3, "#C2406A"), e(54, 61, 2, 3, "#C2406A"), e(28, 60, 4, 2.5, "#FF8FA3"), e(72, 60, 4, 2.5, "#FF8FA3"), smile(50, 74, 5))),
        Picture("bear", "Bear", "#B07A4A", "growl",
            pts(14, 24, 22, 12, 34, 16, 40, 24, 60, 24, 66, 16, 78, 12, 86, 24, 80, 36, 86, 52, 80, 72, 64, 84, 36, 84, 20, 72, 14, 52, 20, 36),
            art(c(25, 22, 5, "#E8B77A"), c(75, 22, 5, "#E8B77A"), e(50, 62, 14, 10, "#E8C9A0"), e(50, 57, 5, 3.5, INK), l("M50 60 L50 64 M50 64 Q46 68 42 65 M50 64 Q54 68 58 65", INK, 1.4), eye(38, 46), eye(62, 46), cheek(30, 58), cheek(70, 58))),
        Picture("duck", "Duck", "#FFE14D", "quack",
            pts(12, 46, 24, 52, 40, 48, 46, 36, 44, 22, 54, 12, 66, 14, 72, 22, 86, 26, 74, 32, 68, 40, 76, 52, 80, 64, 70, 78, 50, 84, 30, 80, 18, 66),
            art(p("M70 21 L87 26 L74 32 Z", "#FF8A3D"), eye(60, 22, 0.9), cheek(63, 30), p("M34 56 Q50 50 62 60 Q52 72 36 66 Z", "#F5C400"), p("M42 84 l-6 6 h12 Z", "#FF8A3D"), p("M58 84 l-6 6 h12 Z", "#FF8A3D"))),
        Picture("elephant", "Elephant", "#8FB8FF", "trumpet",
            pts(40, 18, 66, 26, 84, 32, 94, 46, 94, 66, 88, 72, 88, 90, 76, 90, 74, 76, 58, 76, 56, 90, 44, 90, 42, 72, 34, 62, 28, 66, 24, 82, 16, 84, 12, 78, 16, 62, 20, 44, 28, 26),
            art(p("M46 28 Q62 24 66 40 Q68 56 56 62 Q46 64 42 54 Q40 40 46 28 Z", "#6A93E0"), p("M49 33 Q60 31 62 42 Q63 54 55 58 Q48 59 46 52 Q45 42 49 33 Z", "#FFB3C7"), eye(32, 38), cheek(36, 47), p("M28 56 Q30 64 38 64 Q34 60 33 56 Z", WHITE), l("M94 54 Q99 60 97 66", INK, 1.6), c(48, 89, 1.5, WHITE), c(52, 89, 1.5, WHITE), c(80, 89, 1.5, WHITE), c(84, 89, 1.5, WHITE))),
        Picture("giraffe", "Giraffe", "#FFD166", "happy",
            pts(60, 8, 74, 8, 86, 16, 86, 22, 72, 26, 66, 30, 68, 54, 70, 70, 68, 94, 62, 94, 60, 74, 38, 74, 36, 94, 30, 94, 28, 70, 26, 58, 32, 52, 54, 50, 56, 24, 58, 14),
            art(c(40, 60, 3.5, "#C9792C"), c(50, 65, 3, "#C9792C"), c(60, 58, 2.5, "#C9792C"), c(61, 40, 2.5, "#C9792C"), c(61, 30, 2.2, "#C9792C"), c(34, 66, 2.5, "#C9792C"), c(46, 56, 2.5, "#C9792C"), l("M62 8 L61 2 M70 8 L71 2", INK, 1.8), c(61, 2, 1.8, "#C9792C"), c(71, 2, 1.8, "#C9792C"), eye(70, 14, 0.8), c(84, 19, 0.9, INK), l("M55 48 L56 26", "#C9792C", 2.4), l("M27 60 Q20 66 22 74", INK, 1.4), r(30, 91, 6, 3, 1, "#8B4A2B"), r(62, 91, 6, 3, 1, "#8B4A2B"))),
        Picture("zebra", "Zebra", "#F4F6FF", "neigh",
            pts(22, 38, 58, 36, 66, 20, 72, 10, 76, 16, 88, 26, 90, 36, 80, 38, 74, 46, 76, 58, 76, 90, 70, 90, 68, 64, 36, 64, 34, 90, 28, 90, 26, 60, 18, 52, 10, 64, 14, 48),
            art(l("M30 40 Q28 52 32 62 M40 38 Q38 50 42 62 M50 37 Q48 50 52 62 M60 40 Q64 50 62 60 M67 30 L73 36 M64 46 L72 50", INK, 2.2), l("M58 36 L66 20 L72 12", INK, 3), eye(78, 24, 0.8), c(87, 32, 1, INK), r(28, 88, 6, 3, 1, INK), r(70, 88, 6, 3, 1, INK))),
        Picture("monkey", "Monkey", "#A0663A", "ooh",
            pts(30, 18, 50, 10, 70, 18, 78, 30, 90, 28, 94, 40, 88, 52, 80, 54, 76, 72, 62, 86, 38, 86, 24, 72, 20, 54, 12, 52, 6, 40, 10, 28, 22, 30),
            art(c(12, 40, 5, "#E8C9A0"), c(88, 40, 5, "#E8C9A0"), p("M50 28 Q30 22 28 42 Q26 58 34 70 Q42 82 50 82 Q58 82 66 70 Q74 58 72 42 Q70 22 50 28 Z", "#E8C9A0"), eye(41, 44), eye(59, 44), c(47, 58, 1, INK), c(53, 58, 1, INK), l("M40 66 Q50 76 60 66", INK, 1.6), cheek(34, 58), cheek(66, 58))),
        Picture("lion", "Lion", "#F28C28", "roar:1.1",
            burst(50, 52, 44, 35, 10),
            art(c(34, 30, 6, "#FFD166"), c(66, 30, 6, "#FFD166"), c(50, 54, 26, "#FFD166"), eye(41, 48), eye(59, 48), e(50, 62, 11, 8, "#FFF1C9"), p("M46 57 L54 57 L50 61 Z", "#8B4A2B"), l("M50 61 Q47 66 44 64 M50 61 Q53 66 56 64", "#8B4A2B", 1.4), cheek(36, 60), cheek(64, 60))),
        Picture("whale", "Whale", "#5C8DFF", "bubble",
            pts(92, 58, 88, 42, 74, 32, 52, 30, 32, 38, 20, 48, 12, 32, 4, 36, 12, 54, 6, 70, 18, 66, 28, 62, 40, 70, 64, 74, 84, 70),
            art(p("M40 70 Q64 76 84 70 Q90 64 92 58 Q70 66 40 62 Z", "#BFD3FF"), eye(78, 50, 0.9), l("M82 60 Q86 62 90 58", INK, 1.4), cheek(80, 56), p("M58 64 Q52 76 62 78 Q64 70 62 64 Z", "#3F6FE0"), l("M66 30 Q64 20 58 16 M66 30 Q66 18 66 12 M66 30 Q68 20 74 16", "#5CC8FF", 2))),
        Picture("butterfly", "Butterfly", "#FF5D73", "twinkle",
            pts(50, 26, 60, 14, 80, 8, 92, 16, 88, 36, 72, 48, 86, 58, 90, 76, 76, 88, 60, 80, 53, 68, 47, 68, 40, 80, 24, 88, 10, 76, 14, 58, 28, 48, 12, 36, 8, 16, 20, 8, 40, 14),
            art(c(72, 26, 7, "#FFC93C"), c(28, 26, 7, "#FFC93C"), c(72, 68, 5, "#5CC8FF"), c(28, 68, 5, "#5CC8FF"), c(82, 18, 2.5, WHITE), c(18, 18, 2.5, WHITE), e(50, 48, 4, 22, INK), c(50, 24, 5, INK), l("M48 20 Q42 8 36 6 M52 20 Q58 8 64 6", INK, 1.4), c(36, 6, 2, INK), c(64, 6, 2, INK), c(48, 23, 1.2, WHITE), c(52, 23, 1.2, WHITE))),
        Picture("snake", "Snake", "#8BD346", "hiss",
            pts(6, 70, 16, 58, 28, 54, 40, 62, 52, 68, 62, 62, 70, 48, 78, 34, 88, 28, 96, 34, 94, 46, 84, 48, 78, 58, 68, 72, 54, 80, 40, 76, 28, 68, 18, 70),
            art(c(28, 61, 2.5, "#FFC93C"), c(41, 69, 2.5, "#FFC93C"), c(55, 73, 2.5, "#FFC93C"), c(68, 61, 2.5, "#FFC93C"), c(77, 45, 2.5, "#FFC93C"), eye(89, 35, 0.8), l("M95 42 L100 46 L102 44 M100 46 L101 49", "#FF5D73", 1.2))),
        Picture("dog", "Puppy", "#E8B77A", "woof",
            pts(30, 14, 70, 14, 82, 24, 92, 46, 86, 62, 78, 58, 78, 72, 66, 86, 34, 86, 22, 72, 22, 58, 14, 62, 8, 46, 18, 24),
            art(p("M30 16 Q14 22 10 46 Q14 60 22 56 Q24 34 30 16 Z", "#A0663A"), p("M70 16 Q86 22 90 46 Q86 60 78 56 Q76 34 70 16 Z", "#A0663A"), e(62, 40, 8, 7, "#C9925A"), eye(40, 42), eye(60, 42), e(50, 64, 14, 10, "#FFF1C9"), e(50, 58, 5, 3.5, INK), l("M50 61 L50 66 M50 66 Q46 70 42 67 M50 66 Q54 70 58 67", INK, 1.4), p("M47 68 Q50 77 53 68 Z", "#FF5D73"))),
        Picture("hippo", "Hippo", "#B48CFF", "growl:1.3",
            pts(22, 22, 30, 14, 38, 22, 62, 22, 70, 14, 78, 22, 80, 36, 86, 48, 90, 64, 84, 80, 64, 90, 36, 90, 16, 80, 10, 64, 14, 48, 20, 36),
            art(c(30, 20, 3, "#FFB3C7"), c(70, 20, 3, "#FFB3C7"), e(50, 68, 30, 18, "#C9AEFF"), e(40, 60, 2.5, 3.5, "#7B4FE0"), e(60, 60, 2.5, 3.5, "#7B4FE0"), eye(38, 38), eye(62, 38), l("M30 74 Q50 84 70 74", INK, 1.6), r(40, 75, 4, 5, 1, WHITE), r(56, 75, 4, 5, 1, WHITE), cheek(24, 64), cheek(76, 64))),
    )

    val vehicles = listOf(
        Picture("sailboat", "Sailboat", "#FF5D73", "splash",
            pts(8, 64, 46, 64, 46, 8, 84, 60, 92, 64, 80, 84, 20, 84),
            art(p("M50 13 L81 58 L50 58 Z", WHITE), l("M50 36 L66 36", "#5CC8FF", 2), p("M46 8 L36 12 L46 16 Z", "#FFC93C"), l("M14 72 L86 72", WHITE, 2), c(32, 78, 2.5, GLASS), c(50, 78, 2.5, GLASS), c(68, 78, 2.5, GLASS), l("M2 92 Q10 88 18 92 T34 92 T50 92 T66 92 T82 92 T98 92", "#5CC8FF", 2))),
        Picture("balloon", "Hot Air Balloon", "#FFC93C", "whoosh",
            pts(50, 6, 70, 10, 84, 24, 88, 42, 80, 58, 62, 72, 60, 80, 66, 80, 66, 94, 34, 94, 34, 80, 40, 80, 38, 72, 20, 58, 12, 42, 16, 24, 30, 10),
            art(p("M50 6 Q36 30 40 72 L46 72 Q44 30 50 6 Z", "#FF5D73"), p("M50 6 Q64 30 60 72 L54 72 Q56 30 50 6 Z", "#FF5D73"), p("M30 10 Q14 30 34 70 L38 72 Q22 32 30 10 Z", "#3EE0B0"), p("M70 10 Q86 30 66 70 L62 72 Q78 32 70 10 Z", "#3EE0B0"), l("M40 72 L38 80 M60 72 L62 80", INK, 1.2), r(34, 80, 32, 14, 2, "#C9792C"), l("M34 86 L66 86 M42 80 L42 94 M50 80 L50 94 M58 80 L58 94", "#8B4A2B", 1.2), c(92, 70, 4, WHITE), c(96, 72, 3, WHITE), c(8, 84, 3, WHITE))),
        Picture("rocket", "Rocket", "#E6EAFF", "blastoff",
            pts(50, 4, 62, 16, 68, 36, 68, 62, 82, 76, 82, 88, 66, 82, 60, 90, 40, 90, 34, 82, 18, 88, 18, 76, 32, 62, 32, 36, 38, 16),
            art(p("M50 4 L62 16 Q66 24 67 30 L33 30 Q34 24 38 16 Z", "#FF5D73"), c(50, 46, 8, "#B9C3E6"), c(50, 46, 5.5, "#5CC8FF"), c(48, 44, 1.6, WHITE), p("M32 62 L18 76 L18 88 L34 82 Z", "#FF5D73"), p("M68 62 L82 76 L82 88 L66 82 Z", "#FF5D73"), r(32, 66, 36, 4, 1, "#B9C3E6"), p("M40 91 Q50 110 60 91 Z", "#FFC93C"), p("M45 91 Q50 102 55 91 Z", "#FF8A3D"))),
        Picture("car", "Car", "#2F7BFF", "vroom",
            pts(6, 72, 8, 56, 26, 52, 36, 34, 64, 34, 76, 50, 92, 54, 94, 72),
            art(p("M39 38 L49 38 L49 50 L30 50 Z", GLASS), p("M53 38 L62 38 L72 50 L53 50 Z", GLASS), l("M51 52 L51 70", INK, 1.2), r(54, 56, 5, 1.8, 0.9, INK), e(91, 60, 2, 3, "#FFE14D"), r(6, 58, 3, 4, 1, "#FF5D73"), wheel(28, 72), wheel(74, 72))),
        Picture("bus", "School Bus", "#FFC93C", "honk",
            pts(6, 24, 88, 24, 94, 34, 94, 76, 6, 76),
            art(r(10, 32, 13, 14, 2, GLASS), r(27, 32, 13, 14, 2, GLASS), r(44, 32, 13, 14, 2, GLASS), r(61, 32, 13, 14, 2, GLASS), r(79, 32, 10, 32, 2, GLASS), l("M84 32 L84 64", INK, 1), r(6, 52, 72, 4, 0, "#FF8A3D"), c(92, 68, 2, WHITE), r(6, 64, 3, 5, 1, "#FF5D73"), wheel(24, 76), wheel(74, 76))),
        Picture("dumptruck", "Dump Truck", "#FF8A3D", "honk:0.8",
            pts(6, 34, 58, 34, 58, 46, 70, 46, 78, 30, 90, 30, 94, 48, 94, 74, 6, 74),
            art(c(20, 32, 5, "#A0663A"), c(32, 30, 6, "#8B4A2B"), c(44, 32, 5, "#A0663A"), l("M14 40 L14 66 M26 40 L26 66 M38 40 L38 66 M50 40 L50 66", "#C2410C", 1.6), p("M79 34 L88 34 L91 46 L74 46 Z", GLASS), r(90, 64, 6, 6, 1, "#B9C3E6"), wheel(22, 76), wheel(46, 76), wheel(80, 76))),
        Picture("train", "Train", "#FF5D73", "choo",
            pts(4, 78, 10, 64, 10, 44, 18, 44, 16, 26, 30, 26, 28, 44, 56, 44, 56, 20, 52, 14, 94, 14, 90, 20, 90, 78),
            art(c(26, 15, 5, "#E6EAFF"), c(34, 7, 6, "#E6EAFF"), c(44, 3, 4, "#E6EAFF"), r(14, 22, 18, 5, 2, INK), r(50, 11, 46, 4, 1.5, INK), r(64, 24, 18, 14, 2, GLASS), l("M24 46 L24 74 M40 46 L40 74", "#C2304A", 2), c(13, 52, 2.5, "#FFE14D"), wheel(24, 80), wheel(48, 80), wheel(74, 80, 10))),
        Picture("airplane", "Airplane", "#F4F6FF", "whoosh",
            pts(4, 52, 16, 42, 66, 42, 76, 20, 88, 20, 84, 44, 94, 48, 90, 60, 16, 62),
            art(p("M76 20 L88 20 L84 42 L70 42 Z", "#FF5D73"), p("M8 48 L18 44 L22 50 L8 51 Z", "#5CC8FF"), c(30, 50, 2.4, "#5CC8FF"), c(40, 50, 2.4, "#5CC8FF"), c(50, 50, 2.4, "#5CC8FF"), c(60, 50, 2.4, "#5CC8FF"), c(70, 50, 2.4, "#5CC8FF"), l("M16 57 L90 56", "#2F7BFF", 2), p("M36 56 L60 56 L44 78 L36 78 Z", "#2F7BFF"), c(20, 84, 4, "#E6EAFF"), c(26, 86, 3, "#E6EAFF"))),
        Picture("helicopter", "Helicopter", "#3EE0B0", "whirr",
            pts(14, 50, 20, 36, 36, 30, 56, 32, 64, 42, 92, 40, 93, 32, 99, 36, 96, 54, 66, 54, 58, 64, 30, 66, 16, 60),
            art(p("M20 48 Q22 36 36 34 L40 34 L40 50 Z", GLASS), r(36, 22, 4, 9, 1, INK), l("M6 22 L72 22", INK, 3), l("M96 26 L96 42", INK, 2), l("M18 76 L64 76 M26 66 L24 76 M54 66 L56 76", INK, 2.4), l("M66 47 L92 46", "#1E9E78", 2))),
        Picture("submarine", "Submarine", "#FFC93C", "bubble",
            pts(8, 58, 16, 44, 36, 40, 42, 40, 44, 22, 62, 22, 64, 40, 82, 42, 92, 52, 84, 64, 30, 68, 14, 66),
            art(l("M52 22 L52 10 L60 10", INK, 2.4), r(48, 28, 10, 6, 2, "#5CC8FF"), c(30, 54, 4, "#B9C3E6"), c(30, 54, 2.6, "#5CC8FF"), c(48, 54, 4, "#B9C3E6"), c(48, 54, 2.6, "#5CC8FF"), c(66, 54, 4, "#B9C3E6"), c(66, 54, 2.6, "#5CC8FF"), p("M92 52 L98 44 L98 60 Z", "#FF8A3D"), c(4, 40, 2, GLASS), c(8, 32, 1.5, GLASS), c(4, 24, 2.2, GLASS))),
        Picture("tractor", "Tractor", "#3FAE4A", "vroom:0.7",
            pts(6, 70, 10, 50, 18, 48, 18, 32, 26, 32, 26, 48, 40, 48, 46, 24, 76, 24, 78, 50, 92, 52, 94, 70),
            art(r(16, 30, 12, 3, 1, INK), p("M50 28 L72 28 L74 46 L50 46 Z", GLASS), l("M8 56 L8 66", INK, 1.6), c(68, 72, 16, INK), c(68, 72, 7, "#FFC93C"), c(18, 74, 9, INK), c(18, 74, 4, "#FFC93C"))),
        Picture("firetruck", "Fire Truck", "#E63946", "siren",
            pts(4, 72, 4, 48, 14, 40, 30, 40, 34, 26, 52, 26, 56, 40, 96, 40, 96, 72),
            art(p("M36 30 L50 30 L52 40 L34 40 Z", GLASS), r(40, 21, 6, 5, 2, "#5CC8FF"), l("M58 34 L94 34 M58 38 L94 38", "#E6EAFF", 1.6), l("M62 34 L62 38 M68 34 L68 38 M74 34 L74 38 M80 34 L80 38 M86 34 L86 38", "#E6EAFF", 1.4), r(4, 58, 92, 3, 0, WHITE), c(76, 52, 6, "#FFC93C"), c(76, 52, 2.5, INK), c(7, 52, 2, "#FFE14D"), wheel(20, 74), wheel(80, 74))),
        Picture("ufo", "UFO", "#9B6BFF", "beam",
            pts(50, 14, 62, 18, 68, 32, 86, 38, 96, 48, 86, 58, 68, 62, 32, 62, 14, 58, 4, 48, 14, 38, 32, 32, 38, 18),
            art(p("M36 62 L64 62 L78 98 L22 98 Z", "#59FFE14D"), p("M32 33 Q34 16 50 14 Q66 16 68 33 Z", GLASS), c(50, 27, 6, "#8BD346"), c(47.5, 26, 1.4, INK), c(52.5, 26, 1.4, INK), l("M48 21 L46 17 M52 21 L54 17", "#8BD346", 1), c(20, 48, 2.5, "#FFE14D"), c(35, 51, 2.5, "#3EE0B0"), c(50, 52, 2.5, "#FF5D73"), c(65, 51, 2.5, "#3EE0B0"), c(80, 48, 2.5, "#FFE14D"))),
        Picture("ship", "Ship", "#2F7BFF", "horn",
            pts(4, 58, 20, 58, 20, 42, 46, 42, 46, 22, 58, 22, 58, 42, 76, 42, 76, 58, 96, 58, 86, 82, 14, 82),
            art(p("M20 42 L76 42 L76 58 L20 58 Z", WHITE), r(26, 46, 6, 6, 1.5, "#5CC8FF"), r(38, 46, 6, 6, 1.5, "#5CC8FF"), r(62, 46, 6, 6, 1.5, "#5CC8FF"), r(46, 27, 12, 4, 0, "#FF5D73"), r(44, 20, 16, 3, 1, INK), c(56, 14, 4, "#E6EAFF"), c(62, 8, 5, "#E6EAFF"), r(8, 66, 84, 3, 0, WHITE), c(30, 74, 2.5, GLASS), c(50, 74, 2.5, GLASS), c(70, 74, 2.5, GLASS), l("M2 90 Q10 86 18 90 T34 90 T50 90 T66 90 T82 90 T98 90", "#5CC8FF", 2))),
        Picture("icecream", "Ice Cream Van", "#FFB3C7", "jingle",
            pts(4, 74, 4, 36, 64, 36, 76, 46, 94, 52, 94, 74),
            art(p("M27 22 L41 22 L34 36 Z", "#E8B77A"), c(34, 18, 7, "#FFF1C9"), c(34, 10, 2.5, "#E63946"), r(12, 44, 32, 14, 2, GLASS), l("M10 44 L46 44", "#FF5D73", 3), p("M64 40 L74 48 L64 48 Z", GLASS), c(54, 52, 2, WHITE), c(58, 62, 2, WHITE), c(84, 60, 2, WHITE), c(26, 66, 2, WHITE), wheel(20, 76), wheel(76, 76))),
        Picture("racecar", "Race Car", "#FF5D73", "vroom:1.3",
            pts(4, 64, 4, 38, 14, 38, 14, 48, 28, 46, 36, 36, 58, 36, 68, 46, 92, 50, 96, 64),
            art(p("M38 40 L56 40 L62 46 L34 46 Z", GLASS), c(46, 42, 3.5, "#FFC93C"), l("M16 52 L92 55", WHITE, 2), c(48, 57, 5.5, WHITE), t(48, 59.6, "7", INK, 7.5), wheel(24, 66, 10), wheel(80, 66, 10))),
        Picture("police", "Police Car", "#2F5BEA", "siren:1.2",
            pts(6, 70, 8, 54, 26, 50, 34, 34, 46, 34, 46, 26, 56, 26, 56, 34, 66, 34, 76, 50, 92, 54, 94, 70),
            art(r(46, 26, 5, 8, 1, "#E63946"), r(51, 26, 5, 8, 1, "#5CC8FF"), p("M37 38 L49 38 L49 50 L28 50 Z", GLASS), p("M53 38 L64 38 L72 50 L53 50 Z", GLASS), r(30, 52, 40, 13, 2, WHITE), star(50, 58.5, 5, "#FFC93C"), e(91, 60, 2, 3, "#FFE14D"), wheel(24, 72), wheel(76, 72))),
        Picture("digger", "Digger", "#FFC93C", "vroom:0.6",
            pts(6, 84, 6, 72, 10, 64, 10, 44, 26, 36, 40, 36, 40, 50, 54, 50, 70, 26, 80, 24, 92, 40, 96, 54, 84, 58, 80, 48, 76, 40, 60, 62, 56, 72, 60, 84),
            art(r(14, 42, 14, 12, 2, GLASS), r(4, 72, 58, 13, 6.5, INK), c(12, 78.5, 3.5, "#B9C3E6"), c(24, 78.5, 3.5, "#B9C3E6"), c(36, 78.5, 3.5, "#B9C3E6"), c(48, 78.5, 3.5, "#B9C3E6"), c(70, 28, 2.5, INK), c(55, 52, 2.5, INK))),
        Picture("cablecar", "Cable Car", "#FF8A3D", "ding",
            pts(44, 6, 56, 6, 56, 18, 76, 22, 86, 34, 86, 82, 14, 82, 14, 34, 24, 22, 44, 18),
            art(l("M0 7 L100 3", INK, 1.6), r(20, 36, 26, 22, 3, GLASS), r(54, 36, 26, 22, 3, GLASS), c(30, 50, 4, "#FFD166"), c(66, 50, 4, "#E8B77A"), r(14, 64, 72, 4, 0, WHITE))),
        Picture("speedboat", "Speedboat", "#F4F6FF", "vroom:1.1",
            pts(4, 56, 56, 56, 62, 40, 74, 40, 80, 56, 96, 52, 86, 72, 14, 72),
            art(p("M62.5 41 L73.5 41 L77.5 54 L58.5 54 Z", GLASS), r(10, 61, 80, 3, 0, "#2F7BFF"), r(12, 66, 76, 2, 0, "#FF5D73"), p("M66 40 L66 30 L74 34 L66 37 Z", "#FFC93C"), l("M0 78 Q8 72 14 78 M2 68 Q6 64 10 68", GLASS, 2), l("M2 84 Q12 80 22 84 T42 84 T62 84 T82 84 T100 84", "#5CC8FF", 2))),
    )

    val dinos = listOf(
        Picture("egg", "Dino Egg", "#FFF1C9", "peep",
            eggShape(50, 52, 32, 40, 12),
            art(c(38, 38, 5, "#3EE0B0"), c(60, 28, 4, "#FF8A3D"), c(64, 68, 6, "#9B6BFF"), c(36, 72, 4, "#5CC8FF"), c(52, 80, 3, "#FF5D73"), l("M20 52 L28 46 L36 54 L44 46 L52 54 L60 46 L68 54 L80 48", "#C9A97A", 1.8))),
        Picture("volcano", "Volcano", "#8B5E3C", "rumble",
            pts(4, 86, 30, 40, 38, 30, 46, 34, 54, 28, 62, 34, 70, 40, 96, 86),
            art(p("M30 40 L38 30 L46 34 L54 28 L62 34 L70 40 L62 46 L56 42 L50 52 L44 42 L38 46 Z", "#FF8A3D"), l("M50 52 Q46 64 52 74 M38 46 Q34 56 36 62 M62 46 Q66 56 64 64", "#FF5D73", 3), c(44, 20, 5, "#B9C3E6"), c(54, 12, 7, "#B9C3E6"), c(66, 6, 5, "#B9C3E6"), c(20, 80, 3, "#6B4A30"), c(78, 78, 3.5, "#6B4A30"), c(60, 82, 2.5, "#6B4A30"))),
        Picture("nest", "Dino Nest", "#A0663A", "peep",
            pts(6, 60, 14, 48, 26, 50, 30, 36, 42, 30, 50, 40, 58, 30, 70, 36, 74, 50, 86, 48, 94, 60, 84, 78, 50, 84, 16, 78),
            art(e(36, 46, 8, 11, "#FFF1C9"), e(64, 46, 8, 11, "#BDE8FF"), e(50, 44, 8, 12, "#FFE14D"), c(34, 42, 1.8, "#3EE0B0"), c(66, 48, 1.8, "#FF8A3D"), c(50, 38, 1.8, "#FF5D73"), l("M10 62 Q50 72 90 62 M14 70 Q50 80 86 70", "#7A4A24", 2))),
        Picture("trex", "T-Rex", "#7ED957", "roar",
            pts(3, 56, 20, 53, 36, 46, 50, 43, 60, 37, 59, 23, 77, 19, 93, 22, 97, 36, 95, 49, 74, 49, 75, 61, 68, 73, 71, 89, 58, 89, 52, 77, 47, 89, 33, 89, 33, 74, 18, 70),
            art(p("M34 46 L38 38 L42 45 Z", "#FF8A3D"), p("M44 43 L48 35 L52 42 Z", "#FF8A3D"), p("M63 21 L67 13 L71 20 Z", "#FF8A3D"), p("M74 19 L78 11 L82 19 Z", "#FF8A3D"), e(60, 64, 10, 8, "#D8F5A2"), l("M70 54 Q76 54 77 58", "#5FC24A", 3), c(40, 56, 2.6, "#5FC24A"), c(30, 60, 2, "#5FC24A"), c(46, 63, 2, "#5FC24A"), eye(78, 30), cheek(87, 40), l("M74 43 Q84 47 95 42", INK, 1.4), p("M79 44 L80.5 47.5 L82 44.6 Z", WHITE), p("M86 45 L87.5 48.5 L89 45 Z", WHITE))),
        Picture("stego", "Stegosaurus", "#B48CFF", "growl:0.8",
            pts(4, 60, 16, 54, 22, 42, 30, 50, 34, 34, 44, 44, 50, 28, 58, 42, 66, 32, 70, 46, 78, 44, 88, 46, 96, 54, 92, 62, 82, 64, 78, 80, 70, 80, 68, 68, 44, 68, 42, 80, 34, 80, 32, 66, 20, 64),
            art(p("M17 54 L22 42 L30 50 Z", "#FF8A3D"), p("M30 50 L34 34 L44 44 Z", "#FF8A3D"), p("M44 44 L50 28 L58 42 Z", "#FF8A3D"), p("M58 42 L66 32 L70 46 Z", "#FF8A3D"), e(56, 62, 14, 4, "#E2D3FF"), l("M8 58 L4 52 M12 56 L10 50", INK, 1.4), eye(88, 51, 0.8), smile(92, 57, 2))),
        Picture("tricera", "Triceratops", "#3EE0B0", "roar:1.2",
            pts(6, 62, 20, 52, 36, 44, 56, 42, 62, 30, 70, 22, 80, 30, 92, 22, 86, 36, 96, 46, 92, 56, 80, 60, 72, 62, 70, 80, 62, 80, 60, 68, 36, 68, 34, 80, 26, 80, 24, 66, 14, 64),
            art(p("M56 43 L62 30 L70 22 L79 31 L76 44 Z", "#1E9E78"), c(64, 32, 1.6, "#FFC93C"), c(70, 28, 1.6, "#FFC93C"), c(74, 35, 1.6, "#FFC93C"), p("M80 30 L92 22 L86 36 Z", "#FFF1C9"), p("M90 44 L96 46 L92 50 Z", "#FFF1C9"), e(48, 62, 12, 4, "#A8F0DA"), eye(84, 44, 0.8), l("M86 54 Q90 56 93 53", INK, 1.2))),
        Picture("brachio", "Brachiosaurus", "#FFC93C", "roar:0.6",
            pts(4, 66, 22, 56, 36, 48, 54, 46, 62, 38, 66, 20, 70, 10, 82, 8, 90, 14, 86, 20, 76, 22, 74, 40, 76, 56, 74, 86, 66, 86, 64, 70, 40, 70, 38, 86, 30, 86, 28, 68, 16, 66),
            art(c(44, 56, 3, "#F59E0B"), c(56, 54, 2.5, "#F59E0B"), c(52, 62, 2, "#F59E0B"), c(70, 30, 2, "#F59E0B"), c(68, 46, 2.2, "#F59E0B"), e(52, 66, 12, 3, "#FFE58A"), eye(80, 12, 0.7), l("M84 17 Q87 18 89 15", INK, 1.1))),
        Picture("ptero", "Pterodactyl", "#FF8A3D", "screech",
            pts(4, 40, 30, 36, 44, 30, 48, 20, 56, 14, 70, 12, 58, 22, 56, 30, 70, 36, 96, 40, 70, 52, 56, 50, 54, 62, 50, 72, 46, 62, 44, 50, 30, 52),
            art(p("M50 18 L38 12 L52 15 Z", "#FF5D73"), l("M30 38 Q36 46 44 48 M70 38 Q64 46 56 48", "#C2410C", 1.4), e(50, 56, 4, 8, "#FFD9B0"), eye(53, 18, 0.6))),
        Picture("ankylo", "Ankylosaurus", "#A0663A", "growl",
            pts(4, 52, 10, 46, 16, 52, 28, 52, 36, 38, 56, 32, 76, 38, 86, 48, 96, 54, 92, 62, 84, 64, 80, 76, 72, 76, 70, 66, 42, 66, 40, 76, 32, 76, 30, 64, 16, 58, 10, 62),
            art(c(8, 54, 6, "#7A4A24"), c(44, 44, 3, "#C9925A"), c(54, 40, 3, "#C9925A"), c(64, 42, 3, "#C9925A"), c(74, 46, 2.6, "#C9925A"), c(48, 54, 3, "#C9925A"), c(60, 52, 3, "#C9925A"), c(38, 54, 2.6, "#C9925A"), eye(88, 52, 0.7), smile(92, 58, 2))),
        Picture("paraso", "Parasaurolophus", "#5CC8FF", "trumpet:1.4",
            pts(6, 62, 22, 52, 42, 44, 58, 44, 64, 30, 60, 12, 66, 8, 72, 22, 84, 24, 94, 32, 84, 38, 72, 40, 70, 54, 70, 82, 62, 82, 60, 68, 40, 68, 38, 82, 30, 82, 28, 66, 16, 64),
            art(p("M60 12 L66 8 L72 22 L66 26 Z", "#FF5D73"), l("M36 49 L38 62 M46 46 L48 62 M56 46 L56 62", "#2F9BE0", 2), p("M84 24 L94 32 L84 38 Z", "#FFC93C"), eye(77, 30, 0.8))),
        Picture("spino", "Spinosaurus", "#FF5D73", "roar:0.9",
            pts(4, 58, 18, 52, 26, 40, 32, 24, 42, 16, 52, 20, 58, 30, 62, 40, 72, 38, 80, 32, 96, 36, 94, 44, 80, 46, 74, 52, 74, 82, 66, 82, 64, 66, 42, 66, 40, 82, 32, 82, 30, 64, 16, 62),
            art(p("M24 46 L26 40 L32 24 L42 16 L52 20 L58 30 L62 42 Z", "#FFC93C"), l("M32 44 L34 26 M40 44 L42 19 M48 44 L50 21 M56 44 L56 31", "#F59E0B", 1.4), eye(84, 37, 0.7), l("M80 42 L95 41", INK, 1.2))),
        Picture("raptor", "Velociraptor", "#8BD346", "screech:0.8",
            pts(4, 40, 20, 42, 36, 40, 50, 38, 60, 26, 72, 22, 90, 28, 86, 34, 72, 36, 66, 46, 62, 60, 66, 74, 72, 86, 62, 86, 56, 74, 50, 82, 42, 84, 46, 72, 40, 60, 28, 52, 14, 48),
            art(l("M30 45 L32 50 M40 43 L42 51 M50 43 L52 52", "#4AA83A", 2), eye(75, 27, 0.7), l("M74 33 L88 31", INK, 1.1), l("M64 50 L70 54", INK, 1.6))),
        Picture("diplo", "Diplodocus", "#9B6BFF", "roar:0.55",
            pts(2, 56, 20, 52, 36, 44, 56, 42, 70, 34, 82, 22, 94, 20, 96, 28, 86, 32, 76, 44, 70, 56, 70, 82, 62, 82, 60, 66, 40, 66, 38, 82, 30, 82, 28, 62, 14, 60),
            art(e(50, 60, 14, 4, "#C9AEFF"), c(44, 50, 2.5, "#7B4FE0"), c(54, 48, 2, "#7B4FE0"), c(62, 52, 2.2, "#7B4FE0"), eye(90, 23, 0.7), smile(92, 27, 2))),
        Picture("plesio", "Plesiosaurus", "#2F7BFF", "bubble",
            pts(4, 56, 22, 50, 40, 44, 56, 44, 66, 32, 70, 18, 80, 12, 90, 16, 86, 24, 78, 26, 76, 40, 74, 52, 84, 66, 72, 66, 62, 58, 42, 60, 36, 72, 26, 72, 28, 60, 14, 60),
            art(l("M28 57 Q50 60 72 52", "#8FB8FF", 2.4), eye(83, 17, 0.7), smile(87, 21, 2), l("M0 86 Q10 80 20 86 T40 86 T60 86 T80 86 T100 86", "#5CC8FF", 2))),
        Picture("pachy", "Pachycephalosaurus", "#FFD166", "growl:1.1",
            pts(4, 56, 22, 48, 42, 42, 58, 40, 64, 26, 74, 18, 86, 20, 92, 30, 90, 40, 80, 44, 70, 48, 68, 60, 70, 82, 62, 82, 58, 66, 40, 66, 38, 82, 30, 82, 28, 62, 14, 60),
            art(p("M64 27 Q72 14 86 20 Q90 24 92 30 Q80 26 66 30 Z", "#C9792C"), c(70, 26, 1.4, "#8B4A2B"), c(78, 22, 1.4, "#8B4A2B"), c(86, 24, 1.4, "#8B4A2B"), l("M36 46 L38 58 M46 44 L48 58", "#F5B83C", 2), eye(80, 32, 0.8), l("M84 39 Q87 41 90 38", INK, 1.2))),
        Picture("dimetro", "Dimetrodon", "#FF8A3D", "growl:0.9",
            pts(4, 62, 16, 58, 24, 54, 28, 36, 40, 22, 52, 20, 64, 30, 68, 50, 80, 50, 94, 54, 90, 62, 80, 64, 76, 76, 68, 76, 66, 66, 38, 66, 36, 76, 28, 76, 26, 66, 14, 66),
            art(p("M26 54 L28 36 L40 22 L52 20 L64 30 L66 50 Z", "#9B6BFF"), l("M32 52 L34 32 M40 52 L41 24 M48 52 L48 22 M56 52 L57 26", "#C9AEFF", 1.4), eye(84, 55, 0.7), l("M86 60 L93 59", INK, 1.1))),
        Picture("hatchling", "Baby Dino", "#8BD346", "peep",
            pts(20, 62, 28, 54, 34, 62, 38, 40, 40, 24, 52, 16, 66, 20, 70, 32, 64, 38, 62, 54, 66, 62, 74, 54, 80, 62, 78, 80, 64, 92, 36, 92, 22, 80),
            art(p("M20 62 L28 54 L34 62 L42 56 L50 64 L58 56 L66 62 L74 54 L80 62 Q82 78 74 86 Q64 94 50 94 Q36 94 26 86 Q18 78 20 62 Z", "#FFF1C9"), c(34, 76, 3, "#3EE0B0"), c(60, 82, 4, "#FF8A3D"), c(48, 72, 2.5, "#9B6BFF"), eye(56, 26, 1.1), cheek(62, 32), l("M56 34 Q61 37 66 33", INK, 1.2))),
        Picture("mammoth", "Mammoth", "#A0663A", "trumpet:0.8",
            pts(40, 14, 58, 14, 76, 22, 92, 40, 94, 62, 88, 68, 88, 88, 78, 88, 76, 74, 58, 76, 56, 88, 46, 88, 44, 72, 34, 64, 28, 66, 24, 82, 16, 84, 12, 78, 16, 60, 20, 40, 28, 24),
            art(l("M50 28 l2 4 M60 30 l2 4 M70 36 l2 4 M80 46 l2 4 M66 50 l2 4 M76 60 l2 4", "#7A4A24", 1.4), p("M42 28 Q54 26 56 38 Q57 50 48 54 Q40 54 39 44 Q38 34 42 28 Z", "#7A4A24"), l("M30 58 Q34 72 48 70", "#FFF1C9", 3), eye(30, 36, 0.9))),
        Picture("iguano", "Iguanodon", "#1FA97A", "roar:1.1",
            pts(30, 8, 46, 6, 54, 14, 52, 22, 44, 24, 42, 34, 52, 42, 56, 50, 50, 52, 46, 46, 48, 60, 60, 74, 80, 86, 96, 92, 70, 90, 52, 84, 50, 94, 40, 94, 40, 80, 32, 70, 28, 50, 30, 30, 26, 20),
            art(l("M32 40 Q34 66 44 80", "#7ED957", 3), p("M56 50 L61 45 L58 52 Z", "#FFF1C9"), l("M58 76 L60 80 M68 80 L70 84 M78 84 L80 88", "#127A57", 1.6), eye(44, 12, 0.8), l("M47 19 L53 18", INK, 1.1))),
        Picture("archae", "Archaeopteryx", "#5CC8FF", "tweet",
            pts(6, 40, 24, 44, 36, 40, 48, 30, 56, 18, 66, 14, 76, 18, 90, 20, 78, 26, 70, 30, 68, 42, 80, 50, 92, 62, 70, 58, 60, 62, 56, 74, 50, 74, 48, 62, 36, 54, 18, 52),
            art(p("M68 42 L80 50 L92 62 L70 58 Z", "#FF5D73"), l("M74 50 L78 56 M80 54 L84 59", "#FFC93C", 1.4), l("M6 40 L18 46 M8 46 L20 50", "#FFC93C", 2), p("M76 18 L90 20 L78 26 Z", "#FFC93C"), eye(67, 20, 0.7))),
    )

    val worlds = listOf(
        World("animals", "Animal Park", "Animals", "#F28C28", "#A85A10", INK, "#1C4A33", "#256B45", 0, "lion", animals),
        World("vehicles", "Turbo Town", "Vehicles", "#2457E6", "#16399E", WHITE, "#0F2A66", "#173B8A", 20, "rocket", vehicles),
        World("dinos", "Dino Valley", "Dinos", "#1FA97A", "#11744F", INK, "#0F4C47", "#13605A", 40, "trex", dinos),
    )
}
