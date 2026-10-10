package com.harrycodess.drawingkids.art

import com.harrycodess.drawingkids.art.Category.BIRDS
import com.harrycodess.drawingkids.art.Category.BUGS
import com.harrycodess.drawingkids.art.Category.FARM
import com.harrycodess.drawingkids.art.Category.OCEAN
import com.harrycodess.drawingkids.art.Category.PETS
import com.harrycodess.drawingkids.art.Category.WILD

/**
 * Every coloring page in the app, hand-drawn as vector shapes in a 200x200 box.
 * Parts are listed back-to-front.
 */
object Animals {

    val all: List<AnimalArt> by lazy {
        listOf(
            cat(), dog(), rabbit(), mouse(),
            pig(), cow(), sheep(), horse(),
            lion(), elephant(), giraffe(), monkey(), tiger(), panda(), koala(), fox(), bear(), frog(), hedgehog(),
            fish(), whale(), turtle(), octopus(), crab(), starfish(), jellyfish(),
            owl(), bluebird(), penguin(), duck(), chick(), parrot(), hen(), toucan(),
            butterfly(), snail(), bee(), ladybug(), caterpillar(),
        )
    }

    fun byId(id: String) = all.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- Pets

    private fun cat() = animal("cat", "Cat", PETS) {
        fill("ears", path("M52 70 L60 20 L88 52 Z"), path("M148 70 L140 20 L112 52 Z"))
        fill("innerEars", path("M60 58 L63 34 L78 50 Z"), path("M140 58 L137 34 L122 50 Z"), outline = 3f)
        fill("head", circle(100, 108, 60))
        fill("muzzle", ellipse(100, 132, 26, 18))
        eye(78, 98, 8); eye(122, 98, 8)
        ink(path("M93 122 L107 122 L100 131 Z"))
        line(path("M100 131 Q92 142 84 136 M100 131 Q108 142 116 136"))
        line(path("M40 118 L72 124 M40 136 L72 132 M160 118 L128 124 M160 136 L128 132"), width = 3f)
    }

    private fun dog() = animal("dog", "Dog", PETS) {
        fill("ears", ellipse(46, 96, 20, 42, 18), ellipse(154, 96, 20, 42, -18))
        fill("head", ellipse(100, 100, 56, 60))
        fill("patch", ellipse(121, 88, 17, 15))
        fill("muzzle", ellipse(100, 134, 30, 22))
        eye(80, 92, 8); eye(120, 90, 8)
        ink(ellipse(100, 122, 11, 8))
        line(path("M100 130 L100 142 M88 146 Q100 154 112 146"))
        fill("tongue", path("M94 150 Q94 164 100 164 Q106 164 106 150 Z"), outline = 3f)
    }

    private fun rabbit() = animal("rabbit", "Rabbit", PETS) {
        fill("ears", ellipse(78, 56, 16, 46), ellipse(122, 56, 16, 46))
        fill("innerEars", ellipse(78, 58, 7, 32), ellipse(122, 58, 7, 32), outline = 3f)
        fill("head", circle(100, 130, 52))
        fill("cheeks", ellipse(72, 142, 9, 6), ellipse(128, 142, 9, 6), outline = 0f)
        eye(82, 122, 7); eye(118, 122, 7)
        fill("nose", ellipse(100, 140, 8, 6), outline = 3f)
        line(path("M100 146 L100 156 M90 160 Q100 166 110 160"), width = 3f)
        white(rect(94, 162, 12, 10, 2), outline = 3f)
    }

    private fun mouse() = animal("mouse", "Mouse", PETS) {
        fill("ears", circle(48, 64, 36), circle(152, 64, 36))
        fill("innerEars", circle(48, 64, 20), circle(152, 64, 20), outline = 3f)
        fill("head", ellipse(100, 120, 56, 52))
        eye(80, 110, 7); eye(120, 110, 7)
        fill("nose", circle(100, 136, 9), outline = 3f)
        line(path("M60 132 L84 136 M60 148 L84 142 M140 132 L116 136 M140 148 L116 142"), width = 3f)
        line(path("M90 152 Q100 160 110 152"), width = 3f)
    }

    // ---------------------------------------------------------------- Farm

    private fun pig() = animal("pig", "Pig", FARM) {
        fill("ears", path("M48 66 L46 26 L80 48 Z"), path("M152 66 L154 26 L120 48 Z"))
        fill("head", circle(100, 108, 62))
        fill("cheeks", ellipse(62, 128, 10, 7), ellipse(138, 128, 10, 7), outline = 0f)
        fill("snout", ellipse(100, 124, 28, 20))
        ink(ellipse(90, 124, 5, 7), ellipse(110, 124, 5, 7))
        eye(76, 90, 7); eye(124, 90, 7)
        line(path("M88 152 Q100 160 112 152"))
    }

    private fun cow() = animal("cow", "Cow", FARM) {
        fill("horns", path("M62 54 Q46 26 64 16 Q62 36 78 48 Z"), path("M138 54 Q154 26 136 16 Q138 36 122 48 Z"))
        fill("ears", ellipse(42, 80, 26, 12, -20), ellipse(158, 80, 26, 12, 20))
        fill("head", ellipse(100, 94, 54, 56))
        fill("spots", ellipse(76, 70, 16, 12, -15), ellipse(130, 104, 12, 16))
        eye(80, 92, 7); eye(120, 92, 7)
        fill("snout", ellipse(100, 148, 46, 30))
        ink(ellipse(84, 146, 6, 8), ellipse(116, 146, 6, 8))
        line(path("M88 164 Q100 170 112 164"))
    }

    private fun sheep() = animal("sheep", "Sheep", FARM) {
        fill(
            "wool",
            circle(60, 80, 26), circle(85, 60, 26), circle(115, 60, 26), circle(140, 80, 26),
            circle(150, 110, 26), circle(135, 138, 26), circle(100, 148, 26), circle(65, 138, 26),
            circle(50, 110, 26),
        )
        fill("wool", circle(100, 104, 56), outline = 0f)
        fill("ears", ellipse(64, 98, 20, 9, 20), ellipse(136, 98, 20, 9, -20))
        fill("face", ellipse(100, 114, 30, 38))
        fill("wool", circle(88, 80, 11), circle(100, 74, 12), circle(112, 80, 11))
        eye(88, 108, 6); eye(112, 108, 6)
        ink(circle(94, 128, 2.5f), circle(106, 128, 2.5f))
        line(path("M92 136 Q100 142 108 136"), width = 3f)
    }

    private fun horse() = animal("horse", "Horse", FARM) {
        fill("ears", path("M66 52 L70 12 L92 42 Z"), path("M134 52 L130 12 L108 42 Z"))
        fill("head", path("M62 48 Q100 20 138 48 Q150 100 138 152 L62 152 Q50 100 62 48 Z"))
        fill("mane", path("M82 30 Q100 12 118 30 Q122 50 100 58 Q78 50 82 30 Z"))
        eye(80, 88, 8); eye(120, 88, 8)
        fill("snout", ellipse(100, 158, 36, 26))
        ink(ellipse(87, 156, 5, 7), ellipse(113, 156, 5, 7))
        line(path("M90 174 Q100 180 110 174"))
        fill("cheeks", ellipse(70, 116, 8, 5), ellipse(130, 116, 8, 5), outline = 0f)
    }

    // ---------------------------------------------------------------- Wild

    private fun lion() = animal("lion", "Lion", WILD) {
        for (i in 0 until 14) {
            val a = i / 14.0 * Math.PI * 2
            fill("mane", circle(100 + 70 * Math.cos(a), 104 + 70 * Math.sin(a), 22))
        }
        fill("mane", circle(100, 104, 72), outline = 0f)
        fill("ears", circle(64, 64, 12), circle(136, 64, 12))
        fill("face", circle(100, 104, 54))
        fill("muzzle", ellipse(100, 128, 24, 18))
        eye(80, 96, 7); eye(120, 96, 7)
        ink(path("M92 118 L108 118 L100 128 Z"))
        line(path("M100 128 L100 136 M90 140 Q100 146 110 140"), width = 3f)
    }

    private fun elephant() = animal("elephant", "Elephant", WILD) {
        fill("body", ellipse(116, 118, 62, 46))
        fill("body", rect(76, 140, 22, 34, 8), rect(134, 140, 22, 34, 8))
        fill("body", circle(62, 86, 38))
        fill("body", path("M34 102 Q22 140 36 162 Q46 170 50 160 Q40 140 52 112"))
        fill("ear", ellipse(88, 84, 26, 32))
        eye(52, 78, 6)
        line(path("M178 112 Q192 118 186 132"))
        line(path("M80 168 L80 174 M88 168 L88 174 M138 168 L138 174 M146 168 L146 174"), width = 3f)
    }

    private fun giraffe() = animal("giraffe", "Giraffe", WILD) {
        line(path("M104 40 L102 18 M124 40 L126 18"))
        fill("horns", circle(102, 16, 6), circle(126, 16, 6), outline = 3f)
        fill("body", path("M84 196 L84 70 Q84 40 108 40 L132 40 Q150 40 150 56 Q150 74 132 74 L116 74 L116 196 Z"))
        fill(
            "spots",
            ellipse(96, 100, 8, 10), ellipse(106, 132, 7, 9), ellipse(94, 166, 7, 9), ellipse(108, 182, 5, 6),
            ellipse(100, 62, 6, 5),
            outline = 3f,
        )
        eye(122, 52, 5)
        ink(circle(144, 58, 2.5f))
        line(path("M132 66 Q138 70 144 66"), width = 3f)
    }

    private fun monkey() = animal("monkey", "Monkey", WILD) {
        fill("ears", circle(42, 96, 22), circle(158, 96, 22))
        fill("innerEars", circle(42, 96, 11), circle(158, 96, 11), outline = 3f)
        fill("head", circle(100, 100, 62))
        fill(
            "face",
            path("M100 78 Q118 56 136 74 Q150 92 136 112 Q140 150 100 156 Q60 150 64 112 Q50 92 64 74 Q82 56 100 78 Z"),
        )
        eye(84, 94, 8); eye(116, 94, 8)
        ink(circle(95, 120, 3), circle(105, 120, 3))
        line(path("M80 132 Q100 148 120 132"))
        line(path("M92 40 Q96 28 104 40 Q108 28 116 38"), width = 3f)
    }

    private fun tiger() = animal("tiger", "Tiger", WILD) {
        fill("ears", circle(52, 56, 20), circle(148, 56, 20))
        fill("innerEars", circle(52, 56, 9), circle(148, 56, 9), outline = 3f)
        fill("head", ellipse(100, 106, 66, 60))
        fill(
            "stripes",
            path("M100 48 L92 66 L100 74 L108 66 Z"),
            path("M36 96 L60 102 L36 110 Z"), path("M40 122 L62 122 L44 132 Z"),
            path("M164 96 L140 102 L164 110 Z"), path("M160 122 L138 122 L156 132 Z"),
            outline = 3f,
        )
        fill("muzzle", ellipse(86, 134, 18, 15), ellipse(114, 134, 18, 15))
        eye(80, 98, 8); eye(120, 98, 8)
        ink(path("M90 120 L110 120 L100 132 Z"))
        line(path("M100 132 L100 142"), width = 3f)
    }

    private fun panda() = animal("panda", "Panda", WILD) {
        fill("ears", circle(52, 56, 22), circle(148, 56, 22))
        fill("head", circle(100, 108, 64))
        fill("patches", ellipse(76, 102, 17, 23, 30), ellipse(124, 102, 17, 23, -30))
        white(circle(78, 100, 9), outline = 3f)
        white(circle(122, 100, 9), outline = 3f)
        ink(circle(79, 101, 5), circle(121, 101, 5))
        ink(ellipse(100, 130, 11, 8))
        line(path("M100 138 L100 146 M90 150 Q100 156 110 150"), width = 3f)
        fill("cheeks", ellipse(64, 134, 9, 6), ellipse(136, 134, 9, 6), outline = 0f)
    }

    private fun koala() = animal("koala", "Koala", WILD) {
        fill("ears", circle(44, 72, 34), circle(156, 72, 34))
        fill("innerEars", circle(44, 74, 18), circle(156, 74, 18), outline = 3f)
        fill("head", ellipse(100, 110, 62, 58))
        eye(74, 104, 7); eye(126, 104, 7)
        fill("nose", ellipse(100, 120, 16, 24))
        line(path("M90 154 Q100 160 110 154"), width = 3f)
    }

    private fun fox() = animal("fox", "Fox", WILD) {
        fill("ears", path("M44 72 L52 18 L90 54 Z"), path("M156 72 L148 18 L110 54 Z"))
        fill("innerEars", path("M56 60 L58 34 L78 52 Z"), path("M144 60 L142 34 L122 52 Z"), outline = 3f)
        fill("head", path("M40 70 Q100 30 160 70 Q156 120 100 170 Q44 120 40 70 Z"))
        fill(
            "cheeks",
            path("M44 84 Q80 92 100 130 L100 170 Q60 130 44 84 Z"),
            path("M156 84 Q120 92 100 130 L100 170 Q140 130 156 84 Z"),
        )
        eye(76, 92, 7); eye(124, 92, 7)
        ink(ellipse(100, 164, 9, 7))
    }

    private fun bear() = animal("bear", "Bear", WILD) {
        fill("ears", circle(52, 58, 22), circle(148, 58, 22))
        fill("innerEars", circle(52, 58, 10), circle(148, 58, 10), outline = 3f)
        fill("head", circle(100, 108, 64))
        fill("muzzle", ellipse(100, 132, 30, 24))
        eye(78, 96, 8); eye(122, 96, 8)
        ink(ellipse(100, 122, 12, 8))
        line(path("M100 130 L100 140 M90 144 Q100 150 110 144"), width = 3f)
    }

    private fun frog() = animal("frog", "Frog", WILD) {
        fill("body", circle(66, 62, 24), circle(134, 62, 24))
        fill("body", ellipse(100, 118, 76, 56))
        white(circle(66, 62, 13), outline = 3f)
        white(circle(134, 62, 13), outline = 3f)
        ink(circle(68, 64, 6), circle(132, 64, 6))
        fill("cheeks", ellipse(50, 112, 10, 6), ellipse(150, 112, 10, 6), outline = 0f)
        line(path("M56 118 Q100 156 144 118"))
        fill("belly", ellipse(100, 152, 34, 16), outline = 3f)
    }

    private fun hedgehog() = animal("hedgehog", "Hedgehog", WILD) {
        fill(
            "spikes",
            path(
                "M30 142 L18 122 L38 118 L26 98 L48 98 L42 76 L64 84 L66 62 L86 74 L94 54 L106 72 " +
                    "L120 56 L126 78 L142 66 L144 90 L162 86 L158 108 L176 112 L166 128 Z",
            ),
        )
        fill("feet", ellipse(76, 162, 13, 8), ellipse(134, 162, 13, 8))
        fill(
            "body",
            path("M34 144 Q58 112 112 110 Q152 110 176 126 Q190 134 192 144 Q150 162 100 162 Q50 162 34 144 Z"),
        )
        ink(circle(190, 142, 6))
        eye(156, 128, 6)
        line(path("M168 150 Q174 154 180 150"), width = 3f)
    }

    // ---------------------------------------------------------------- Ocean

    private fun fish() = animal("fish", "Fish", OCEAN) {
        fill("fins", path("M150 100 L188 66 L188 134 Z"))
        fill("body", ellipse(96, 100, 70, 46))
        fill("fins", path("M80 56 Q100 30 124 60"))
        fill("fins", path("M96 100 Q110 112 96 126"))
        eye(56, 92, 9)
        line(path("M28 108 Q36 114 44 110"))
        line(path("M120 80 Q128 100 120 120 M136 84 Q142 100 136 116"), width = 3f)
        white(circle(26, 70, 5), outline = 3f)
        white(circle(16, 50, 7), outline = 3f)
    }

    private fun whale() = animal("whale", "Whale", OCEAN) {
        fill(
            "body",
            path(
                "M20 112 Q24 54 96 54 Q150 54 160 100 L186 72 Q192 100 178 110 Q192 124 184 146 " +
                    "L158 120 Q140 166 80 164 Q20 160 20 112 Z",
            ),
        )
        fill("belly", path("M24 120 Q60 142 132 128 Q120 160 80 162 Q30 158 24 120 Z"))
        eye(56, 96, 7)
        line(path("M84 52 Q80 30 70 24 M84 52 Q88 28 100 22 M84 52 L84 20"), width = 3f)
        fill("cheeks", ellipse(52, 114, 8, 5), outline = 0f)
    }

    private fun turtle() = animal("turtle", "Turtle", OCEAN) {
        fill("skin", circle(166, 114, 20))
        fill("skin", ellipse(60, 146, 14, 12), ellipse(140, 146, 14, 12))
        fill("skin", path("M28 128 L12 136 L30 138 Z"))
        eye(172, 110, 4)
        line(path("M174 122 Q180 124 184 120"), width = 3f)
        fill("shell", path("M28 134 Q30 60 100 58 Q170 60 172 134 Z"))
        fill("pattern", path("M78 80 L122 80 L132 106 L100 124 L68 106 Z"))
        line(
            path("M78 80 L62 72 M122 80 L138 72 M68 106 L40 112 M132 106 L160 112 M100 124 L100 134"),
            width = 3f,
        )
        fill("rim", rect(24, 128, 152, 12, 6))
    }

    private fun octopus() = animal("octopus", "Octopus", OCEAN) {
        fill(
            "body",
            ellipse(50, 150, 12, 36, 32), ellipse(74, 162, 12, 36, 14), ellipse(100, 166, 12, 36),
            ellipse(126, 162, 12, 36, -14), ellipse(150, 150, 12, 36, -32),
        )
        fill("body", ellipse(100, 92, 62, 60))
        fill("spots", circle(76, 58, 8), circle(122, 52, 6), circle(138, 76, 7), outline = 3f)
        white(circle(82, 98, 12), outline = 3f)
        white(circle(118, 98, 12), outline = 3f)
        ink(circle(84, 100, 6), circle(116, 100, 6))
        fill("cheeks", ellipse(66, 120, 9, 6), ellipse(134, 120, 9, 6), outline = 0f)
        line(path("M88 122 Q100 132 112 122"))
    }

    private fun crab() = animal("crab", "Crab", OCEAN) {
        line(path("M50 100 L60 114 M150 100 L140 114"), width = 6f)
        line(
            path(
                "M54 132 L24 140 M54 144 L28 160 M60 154 L40 174 " +
                    "M146 132 L176 140 M146 144 L172 160 M140 154 L160 174",
            ),
            width = 6f,
        )
        fill(
            "claws",
            path("M40 100 Q16 76 30 46 L46 64 L54 40 Q74 64 58 100 Z"),
            path("M160 100 Q184 76 170 46 L154 64 L146 40 Q126 64 142 100 Z"),
        )
        line(path("M86 100 L82 72 M114 100 L118 72"))
        fill("body", ellipse(100, 130, 58, 38))
        white(circle(82, 64, 11), outline = 4f)
        white(circle(118, 64, 11), outline = 4f)
        ink(circle(82, 65, 5), circle(118, 65, 5))
        line(path("M86 136 Q100 146 114 136"))
        fill("cheeks", ellipse(70, 130, 8, 5), ellipse(130, 130, 8, 5), outline = 0f)
    }

    private fun starfish() = animal("starfish", "Starfish", OCEAN) {
        fill(
            "body",
            path("M100 18 L122 74 L182 76 L134 112 L152 172 L100 138 L48 172 L66 112 L18 76 L78 74 Z"),
            outline = 5f,
        )
        fill("dots", circle(100, 48, 5), circle(150, 86, 5), circle(136, 150, 5), circle(64, 150, 5), circle(50, 86, 5), outline = 3f)
        eye(88, 98, 6); eye(112, 98, 6)
        line(path("M90 114 Q100 122 110 114"))
    }

    private fun jellyfish() = animal("jellyfish", "Jellyfish", OCEAN) {
        line(
            path(
                "M70 120 Q60 140 70 160 Q80 180 70 196 M90 124 Q100 146 90 166 Q80 186 90 196 " +
                    "M110 124 Q100 146 110 166 Q120 186 110 196 M130 120 Q140 140 130 160 Q120 180 130 196",
            ),
            width = 5f,
        )
        fill(
            "bell",
            path(
                "M36 116 Q36 40 100 40 Q164 40 164 116 Q148 130 132 116 Q116 130 100 116 " +
                    "Q84 130 68 116 Q52 130 36 116 Z",
            ),
        )
        fill("spots", circle(76, 68, 8), circle(124, 62, 6), circle(140, 86, 5), outline = 3f)
        eye(82, 92, 7); eye(118, 92, 7)
        line(path("M92 104 Q100 110 108 104"), width = 3f)
        fill("cheeks", ellipse(66, 104, 8, 5), ellipse(134, 104, 8, 5), outline = 0f)
    }

    // ---------------------------------------------------------------- Birds

    private fun owl() = animal("owl", "Owl", BIRDS) {
        fill("body", path("M52 56 L60 26 L82 46 Z"), path("M148 56 L140 26 L118 46 Z"))
        fill("body", ellipse(100, 110, 58, 70))
        fill("wings", path("M44 104 Q30 140 60 166 Q56 132 62 108 Z"), path("M156 104 Q170 140 140 166 Q144 132 138 108 Z"))
        fill("belly", ellipse(100, 134, 34, 40))
        fill("eyeRings", circle(76, 84, 22), circle(124, 84, 22))
        eye(76, 84, 8); eye(124, 84, 8)
        fill("beak", path("M92 100 L108 100 L100 114 Z"))
        line(path("M88 130 q6 6 12 0 q6 6 12 0 M84 148 q8 6 16 0 q8 6 16 0"), width = 3f)
        line(path("M84 178 L78 190 M84 178 L84 192 M84 178 L90 190 M116 178 L110 190 M116 178 L116 192 M116 178 L122 190"), width = 3f)
    }

    private fun bluebird() = animal("bluebird", "Bluebird", BIRDS) {
        fill("beak", path("M150 70 L186 78 L150 88 Z"))
        fill("tail", path("M30 120 L4 104 L10 140 Z"))
        line(path("M80 164 L80 186 M112 164 L112 186 M72 188 L88 188 M104 188 L120 188"))
        fill("body", ellipse(96, 110, 68, 56))
        fill("belly", path("M60 140 Q96 172 140 140 Q120 166 96 166 Q72 166 60 140 Z"), outline = 3f)
        fill("wing", path("M60 110 Q90 80 120 110 Q96 142 60 110 Z"))
        eye(132, 82, 7)
        fill("cheeks", ellipse(138, 102, 7, 5), outline = 0f)
    }

    private fun penguin() = animal("penguin", "Penguin", BIRDS) {
        fill("feet", ellipse(78, 186, 18, 8), ellipse(122, 186, 18, 8))
        fill("wings", ellipse(42, 120, 12, 36, 20), ellipse(158, 120, 12, 36, -20))
        fill("body", ellipse(100, 106, 62, 80))
        fill("belly", ellipse(100, 122, 40, 58))
        eye(84, 66, 7); eye(116, 66, 7)
        fill("beak", path("M90 80 L110 80 L100 94 Z"))
        fill("cheeks", ellipse(74, 86, 7, 5), ellipse(126, 86, 7, 5), outline = 0f)
    }

    private fun duck() = animal("duck", "Duck", BIRDS) {
        fill("body", path("M166 120 L192 102 L184 138 Z"))
        fill("body", ellipse(104, 136, 70, 40))
        fill("wing", path("M100 128 Q130 104 162 124 Q140 152 100 128 Z"))
        fill("body", circle(70, 74, 34))
        fill("beak", path("M40 74 Q16 72 12 86 Q30 96 46 88 Z"))
        eye(64, 66, 6)
        fill("cheeks", ellipse(76, 86, 7, 5), outline = 0f)
        fill("water", path("M0 168 Q25 156 50 168 Q75 180 100 168 Q125 156 150 168 Q175 180 200 168 L200 198 L0 198 Z"))
    }

    private fun chick() = animal("chick", "Chick", BIRDS) {
        line(path("M84 178 L84 192 M76 196 L84 192 L92 196 M116 178 L116 192 M108 196 L116 192 L124 196"))
        fill("wings", ellipse(42, 124, 14, 26, 25), ellipse(158, 124, 14, 26, -25))
        fill("body", circle(100, 118, 62))
        line(path("M96 56 Q90 40 100 36 M102 56 Q110 42 116 46"))
        eye(80, 104, 8); eye(120, 104, 8)
        fill("beak", path("M90 118 L110 118 L100 132 Z"))
        fill("cheeks", ellipse(68, 124, 9, 6), ellipse(132, 124, 9, 6), outline = 0f)
    }

    private fun parrot() = animal("parrot", "Parrot", BIRDS) {
        fill("tail", path("M110 150 L116 198 L138 198 L138 146 Z"))
        fill("body", ellipse(118, 114, 42, 58, 15))
        fill("wing", path("M126 90 Q172 112 150 164 Q120 150 118 110 Z"))
        fill("body", circle(92, 62, 36))
        fill("face", ellipse(82, 64, 16, 14))
        fill("beak", path("M64 54 Q46 54 46 74 Q50 90 62 86 Q58 74 68 70 Z"))
        eye(84, 60, 6)
        fill("branch", rect(28, 166, 160, 14, 7))
        line(path("M108 164 Q104 170 108 176 M124 164 Q120 170 124 176"), width = 3f)
    }

    private fun hen() = animal("hen", "Hen", BIRDS) {
        line(path("M96 170 L96 192 M88 196 L96 192 L104 196 M114 170 L114 192 M106 196 L114 192 L122 196"))
        fill("tail", path("M150 112 Q176 64 188 96 Q196 124 170 140 Z"))
        fill("body", ellipse(108, 128, 66, 46))
        fill("comb", path("M48 48 Q48 30 60 40 Q64 24 74 38 Q82 28 84 48 Z"))
        fill("comb", ellipse(46, 92, 6, 10))
        fill("body", circle(66, 72, 28))
        fill("beak", path("M40 66 L24 72 L40 80 Z"))
        eye(62, 66, 5)
        fill("wing", path("M96 120 Q130 98 152 124 Q126 152 96 120 Z"))
    }

    private fun toucan() = animal("toucan", "Toucan", BIRDS) {
        fill("tail", path("M124 172 L118 198 L142 198 L146 168 Z"))
        fill("body", ellipse(124, 120, 40, 60, 10))
        fill("body", circle(112, 62, 28))
        fill("chest", ellipse(104, 96, 20, 24))
        fill("beak", path("M92 50 Q40 38 18 70 Q50 86 94 78 Z"))
        line(path("M30 66 Q60 56 92 62"), width = 3f)
        white(circle(118, 58, 9), outline = 3f)
        ink(circle(119, 59, 4))
        fill("branch", rect(40, 170, 150, 14, 7))
    }

    // ---------------------------------------------------------------- Bugs

    private fun butterfly() = animal("butterfly", "Butterfly", BUGS) {
        fill("topWings", ellipse(62, 72, 40, 36), ellipse(138, 72, 40, 36))
        fill("bottomWings", ellipse(68, 136, 30, 28), ellipse(132, 136, 30, 28))
        fill("dots", circle(58, 70, 12), circle(142, 70, 12), circle(68, 138, 8), circle(132, 138, 8))
        fill("body", ellipse(100, 104, 10, 56))
        line(path("M96 50 Q84 24 72 22 M104 50 Q116 24 128 22"))
        ink(circle(72, 22, 4), circle(128, 22, 4))
    }

    private fun snail() = animal("snail", "Snail", BUGS) {
        line(path("M168 104 L160 74 M182 104 L188 74"))
        ink(circle(160, 72, 5), circle(188, 72, 5))
        fill("body", path("M18 168 Q22 150 50 150 L140 150 Q150 150 156 122 Q162 100 178 100 Q194 100 192 122 Q190 142 172 158 Q164 168 140 168 Z"))
        eye(178, 118, 4)
        line(path("M172 132 Q178 136 184 130"), width = 3f)
        fill("shell", circle(92, 106, 52))
        line(path("M86 106 A6 6 0 0 1 98 106 A12 12 0 0 1 74 106 A18 18 0 0 1 110 106 A24 24 0 0 1 62 106 A30 30 0 0 1 122 106"))
    }

    private fun bee() = animal("bee", "Bee", BUGS) {
        fill("body", path("M176 108 L196 116 L176 124 Z"))
        fill("body", ellipse(110, 118, 70, 48))
        fill(
            "stripes",
            path("M98 72 Q90 118 98 164 L118 165 Q110 118 118 71 Z"),
            path("M142 76 Q136 118 142 160 L158 152 Q152 118 158 84 Z"),
            outline = 3f,
        )
        fill("wings", ellipse(98, 58, 22, 32, -20), ellipse(130, 58, 22, 32, 20))
        line(path("M38 88 Q30 66 20 62 M54 86 Q58 64 68 58"))
        ink(circle(20, 62, 4), circle(68, 58, 4))
        fill("head", circle(46, 114, 30))
        eye(38, 108, 6); eye(56, 108, 6)
        line(path("M38 124 Q46 132 54 124"), width = 3f)
    }

    private fun ladybug() = animal("ladybug", "Ladybug", BUGS) {
        line(path("M90 24 Q84 8 74 8 M110 24 Q116 8 126 8"))
        ink(circle(74, 8, 4), circle(126, 8, 4))
        line(
            path("M48 92 L26 82 M42 120 L20 122 M50 150 L30 164 M152 92 L174 82 M158 120 L180 122 M150 150 L170 164"),
            width = 5f,
        )
        fill("head", circle(100, 44, 26))
        white(circle(90, 40, 7), outline = 3f)
        white(circle(110, 40, 7), outline = 3f)
        ink(circle(90, 41, 3), circle(110, 41, 3))
        fill("body", circle(100, 120, 66))
        line(path("M100 56 L100 186"))
        fill(
            "spots",
            circle(72, 96, 12), circle(128, 96, 12), circle(68, 142, 10), circle(132, 142, 10),
            circle(84, 172, 7), circle(116, 172, 7),
            outline = 3f,
        )
    }

    private fun caterpillar() = animal("caterpillar", "Caterpillar", BUGS) {
        fill("leaf", path("M6 170 Q100 136 194 170 Q100 200 6 170 Z"))
        line(path("M16 170 L184 170"), width = 3f)
        line(path("M50 150 L50 164 M80 144 L80 160 M110 150 L110 164 M140 144 L140 160"))
        fill("body", circle(50, 130, 24), circle(80, 124, 24), circle(110, 130, 24), circle(140, 124, 24))
        line(path("M150 72 Q144 52 136 50 M172 72 Q178 52 188 50"))
        ink(circle(136, 50, 4), circle(188, 50, 4))
        fill("head", circle(162, 100, 32))
        eye(152, 94, 6); eye(172, 94, 6)
        line(path("M152 110 Q162 118 172 110"), width = 3f)
        fill("cheeks", ellipse(144, 110, 6, 4), ellipse(180, 110, 6, 4), outline = 0f)
    }
}
