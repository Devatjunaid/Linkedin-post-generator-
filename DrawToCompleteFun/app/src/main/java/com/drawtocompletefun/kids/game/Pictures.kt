package com.drawtocompletefun.kids.game

/**
 * Every picture in the game, hand drawn on a 0..100 board.
 * Tags name the lines a level may hide.
 */
object Pictures {

    // ---------------------------------------------------------------- Shapes world
    val square = picture("Square") { pg("s", 25, 25, 75, 25, 75, 75, 25, 75) }

    val triangle = picture("Triangle") { pg("t", 50, 18, 84, 78, 16, 78) }

    val circle = picture("Circle") {
        a("q1", 50, 50, 32, 32, 0, 90)
        a("q2", 50, 50, 32, 32, 90, 90)
        a("q3", 50, 50, 32, 32, 180, 90)
        a("q4", 50, 50, 32, 32, 270, 90)
    }

    val rectangle = picture("Rectangle") { pg("r", 16, 30, 84, 30, 84, 70, 16, 70) }

    val diamond = picture("Diamond") { pg("d", 50, 12, 82, 50, 50, 88, 18, 50) }

    val heart = picture("Heart") {
        a("lobeL", 37, 38, 13, 13, 180, 180)
        a("lobeR", 63, 38, 13, 13, 180, 180)
        l("sideL", 24, 38, 50, 80)
        l("sideR", 76, 38, 50, 80)
    }

    val house = picture("House") {
        l("ceil", 26, 50, 74, 50)
        l("wallL", 26, 50, 26, 86)
        l("wallR", 74, 50, 74, 86)
        l("floor", 26, 86, 74, 86)
        l("roofL", 18, 54, 50, 20)
        l("roofR", 50, 20, 82, 54)
        pl("door", 42, 86, 42, 66, 56, 66, 56, 86)
        pg("_w", 31, 57, 38, 57, 38, 64, 31, 64)
        pg("_v", 62, 57, 69, 57, 69, 64, 62, 64)
        pl("chimney", 64, 33, 64, 22, 71, 22, 71, 40)
    }

    val ladder = picture("Ladder") {
        l("railL", 34, 10, 34, 90)
        l("railR", 66, 10, 66, 90)
        l("r1", 34, 22, 66, 22)
        l("r2", 34, 38, 66, 38)
        l("r3", 34, 54, 66, 54)
        l("r4", 34, 70, 66, 70)
        l("r5", 34, 84, 66, 84)
    }

    val window = picture("Window") {
        pg("f", 24, 24, 76, 24, 76, 76, 24, 76)
        l("v", 50, 24, 50, 76)
        l("h", 24, 50, 76, 50)
        l("sill", 18, 82, 82, 82)
    }

    val star = picture("Star") { star("st", 50, 54, 38, 16) }

    val smiley = picture("Smiley") {
        c("face", 50, 50, 36)
        dot(38, 40, 4)
        dot(62, 40, 4)
        a("mouth", 50, 50, 20, 20, 25, 130)
    }

    val sun = picture("Sun") {
        c("sun", 50, 50, 16)
        rays("ray", 50, 50, 23, 38, 8)
    }

    val tree = picture("Tree") {
        c("crown", 50, 36, 24)
        l("trunkL", 44, 59, 44, 86)
        l("trunkR", 56, 59, 56, 86)
        l("ground", 26, 86, 74, 86)
    }

    val hexagon = picture("Hexagon") { regular("h", 50, 50, 34, 6, 0) }

    val arrow = picture("Arrow") {
        l("shaft", 12, 50, 86, 50)
        l("headT", 86, 50, 66, 32)
        l("headB", 86, 50, 66, 68)
        l("featherT", 12, 50, 4, 40)
        l("featherB", 12, 50, 4, 60)
    }

    val cube = picture("Box") {
        pg("f", 20, 40, 62, 40, 62, 82, 20, 82)
        l("b1", 38, 22, 80, 22)
        l("b2", 80, 22, 80, 64)
        l("c1", 20, 40, 38, 22)
        l("c2", 62, 40, 80, 22)
        l("c3", 62, 82, 80, 64)
    }

    val flag = picture("Flag") {
        l("pole", 30, 12, 30, 88)
        l("ftop", 30, 16, 78, 30)
        l("fbot", 78, 30, 30, 44)
        l("base", 18, 88, 42, 88)
    }

    val envelope = picture("Letter") {
        pg("env", 16, 28, 84, 28, 84, 74, 16, 74)
        l("vL", 16, 28, 50, 54)
        l("vR", 50, 54, 84, 28)
    }

    val cup = picture("Cup") {
        l("rim", 26, 36, 66, 36)
        l("cupL", 26, 36, 30, 78)
        l("cupB", 30, 78, 62, 78)
        l("cupR", 66, 36, 62, 78)
        a("handle", 66, 56, 13, 12, -80, 160)
        pl("_s1", 40, 30, 44, 24, 40, 18)
        pl("_s2", 52, 30, 56, 24, 52, 18)
    }

    val ticTacToe = picture("Game grid") {
        l("v1", 38, 14, 38, 86)
        l("v2", 62, 14, 62, 86)
        l("h1", 14, 38, 86, 38)
        l("h2", 14, 62, 86, 62)
        l("x1", 20, 20, 32, 32)
        l("x2", 32, 20, 20, 32)
        c("o1", 74, 74, 7)
    }

    val balloon = picture("Balloon") {
        e("balloon", 50, 38, 23, 28)
        pl("_knot", 46, 70, 50, 66, 54, 70)
        pl("string", 50, 70, 45, 77, 55, 84, 50, 92)
    }

    val iceCream = picture("Ice cream") {
        a("scoop", 50, 40, 21, 21, 180, 180)
        l("scoopBase", 29, 40, 71, 40)
        l("coneL", 30, 42, 50, 90)
        l("coneR", 70, 42, 50, 90)
        dot(50, 15, 4)
    }

    val umbrella = picture("Umbrella") {
        a("canopy", 50, 46, 36, 30, 180, 180)
        l("edge", 14, 46, 86, 46)
        l("stick", 50, 46, 50, 80)
        a("hook", 44, 80, 6, 6, 0, 180)
    }

    val present = picture("Present") {
        pg("box", 22, 44, 78, 44, 78, 86, 22, 86)
        pg("lid", 18, 34, 82, 34, 82, 44, 18, 44)
        l("ribV", 50, 34, 50, 86)
        e("bowL", 42, 27, 8, 6)
        e("bowR", 58, 27, 8, 6)
    }

    val kite = picture("Kite") {
        pg("k", 50, 10, 74, 38, 50, 70, 26, 38)
        l("kv", 50, 10, 50, 70)
        l("kh", 26, 38, 74, 38)
        pl("tail", 50, 70, 44, 78, 56, 85, 48, 94)
    }

    val stairs = picture("Stairs") {
        ch("st", 12, 88, 12, 74, 27, 74, 27, 60, 42, 60, 42, 46, 57, 46, 57, 32, 72, 32, 72, 18, 88, 18)
        l("floor", 12, 88, 88, 88)
        l("wall", 88, 18, 88, 88)
    }

    val pyramid = picture("Triangles") {
        pg("o", 50, 12, 88, 82, 12, 82)
        l("i1", 31, 47, 69, 47)
        l("i2", 69, 47, 50, 82)
        l("i3", 50, 82, 31, 47)
    }

    val fence = picture("Fence") {
        repeat(6) { i ->
            val x = 14 + i * 14
            pl("p${i + 1}", x - 5, 84, x - 5, 40, x, 32, x + 5, 40, x + 5, 84)
        }
        l("railT", 6, 52, 94, 52)
        l("railB", 6, 72, 94, 72)
    }

    // ---------------------------------------------------------------- Animal world
    val fish = picture("Fish") {
        e("body", 44, 50, 27, 16)
        l("tailT", 70, 50, 88, 36)
        l("tailB", 70, 50, 88, 64)
        l("tailE", 88, 36, 88, 64)
        dot(31, 46, 3)
        a("_gill", 28, 50, 8, 10, -50, 100)
    }

    val cat = picture("Kitty") {
        c("head", 50, 58, 30)
        l("earL1", 26, 40, 30, 16)
        l("earL2", 30, 16, 43, 29)
        l("earR1", 57, 29, 70, 16)
        l("earR2", 70, 16, 74, 40)
        dot(40, 54, 3.5)
        dot(60, 54, 3.5)
        dot(50, 63, 2.5)
        a("_mouth", 50, 66, 6, 4, 20, 140)
        l("whiskL1", 36, 64, 12, 59)
        l("whiskL2", 36, 69, 12, 72)
        l("whiskR1", 64, 64, 88, 59)
        l("whiskR2", 64, 69, 88, 72)
    }

    val bird = picture("Birdie") {
        e("body", 46, 56, 24, 17)
        c("head", 68, 38, 11)
        pl("beak", 78, 34, 92, 38, 78, 43)
        dot(70, 36, 2.5)
        a("wing", 42, 56, 14, 9, 180, 180)
        l("legL", 42, 73, 40, 88)
        l("legR", 52, 73, 54, 88)
        l("tail", 22, 52, 8, 44)
    }

    val snail = picture("Snail") {
        c("shell", 44, 52, 20)
        a("_sp", 44, 52, 11, 11, 0, 300)
        l("foot", 14, 74, 86, 74)
        l("neck", 72, 74, 76, 52)
        c("head", 78, 47, 6)
        l("antL", 75, 41, 70, 27)
        l("antR", 81, 41, 88, 28)
    }

    val rabbit = picture("Bunny") {
        c("head", 50, 62, 22)
        e("earL", 40, 26, 7, 17)
        e("earR", 60, 26, 7, 17)
        dot(42, 58, 3)
        dot(58, 58, 3)
        dot(50, 66, 2.5)
        l("whL", 44, 68, 28, 66)
        l("whR", 56, 68, 72, 66)
    }

    val turtle = picture("Turtle") {
        a("shell", 50, 60, 28, 24, 180, 180)
        l("belly", 22, 60, 78, 60)
        c("head", 86, 54, 8)
        l("legA", 30, 60, 30, 74)
        l("legB", 42, 60, 42, 74)
        l("legC", 58, 60, 58, 74)
        l("legD", 70, 60, 70, 74)
        l("tail", 22, 60, 12, 64)
        pl("_pat", 38, 60, 44, 46, 56, 46, 62, 60)
    }

    val butterfly = picture("Butterfly") {
        l("body", 50, 30, 50, 76)
        e("wingTL", 34, 40, 15, 12)
        e("wingTR", 66, 40, 15, 12)
        e("wingBL", 37, 63, 11, 10)
        e("wingBR", 63, 63, 11, 10)
        l("antL", 50, 30, 41, 15)
        l("antR", 50, 30, 59, 15)
    }

    val mouse = picture("Mouse") {
        e("body", 48, 58, 24, 16)
        c("ear", 40, 41, 8)
        dot(30, 54, 2.5)
        dot(24, 60, 2.5)
        pl("tail", 72, 62, 82, 56, 90, 62, 94, 74)
        l("legF", 36, 72, 34, 80)
        l("legB", 58, 72, 60, 80)
    }

    val owl = picture("Owl") {
        e("body", 50, 58, 26, 30)
        c("eyeL", 40, 46, 8)
        c("eyeR", 60, 46, 8)
        dot(40, 46, 3)
        dot(60, 46, 3)
        pl("beak", 46, 54, 50, 62, 54, 54)
        l("tuftL1", 28, 38, 28, 18)
        l("tuftL2", 28, 18, 40, 30)
        l("tuftR1", 60, 30, 72, 18)
        l("tuftR2", 72, 18, 72, 38)
        l("_f", 42, 88, 58, 88)
    }

    val pig = picture("Piggy") {
        c("head", 50, 56, 28)
        e("snout", 50, 63, 10, 7)
        dot(46, 63, 2)
        dot(54, 63, 2)
        dot(40, 50, 3)
        dot(60, 50, 3)
        l("earL1", 29, 38, 25, 20)
        l("earL2", 25, 20, 41, 30)
        l("earR1", 59, 30, 75, 20)
        l("earR2", 75, 20, 71, 38)
    }

    val duck = picture("Duck") {
        e("body", 46, 62, 28, 16)
        c("head", 68, 36, 12)
        l("neck", 62, 47, 62, 50)
        pl("beak", 79, 31, 94, 36, 79, 42)
        dot(70, 33, 2.5)
        a("wing", 42, 62, 14, 8, 180, 180)
        l("water", 8, 80, 92, 80)
    }

    val bee = picture("Bee") {
        e("body", 50, 52, 24, 16)
        l("st1", 40, 38, 40, 66)
        l("st2", 50, 36, 50, 68)
        l("st3", 60, 38, 60, 66)
        e("wingL", 44, 28, 8, 11)
        e("wingR", 58, 27, 8, 11)
        l("sting", 74, 52, 84, 52)
        dot(32, 48, 2.5)
        l("ant", 30, 42, 22, 30)
    }

    val spider = picture("Spider") {
        c("body", 50, 56, 12)
        c("head", 50, 38, 7)
        l("lL1", 40, 50, 20, 38)
        l("lL2", 38, 55, 16, 52)
        l("lL3", 39, 60, 18, 68)
        l("lL4", 43, 65, 28, 80)
        l("lR1", 60, 50, 80, 38)
        l("lR2", 62, 55, 84, 52)
        l("lR3", 61, 60, 82, 68)
        l("lR4", 57, 65, 72, 80)
        l("thread", 50, 31, 50, 6)
    }

    val dog = picture("Puppy") {
        c("head", 50, 52, 24)
        e("earL", 25, 50, 7, 16)
        e("earR", 75, 50, 7, 16)
        dot(42, 47, 3)
        dot(58, 47, 3)
        e("nose", 50, 58, 4, 3)
        a("mouth", 50, 60, 8, 7, 20, 140)
    }

    val bear = picture("Teddy") {
        c("head", 50, 56, 26)
        c("earL", 29, 33, 9)
        c("earR", 71, 33, 9)
        e("snout", 50, 65, 10, 8)
        dot(41, 50, 3)
        dot(59, 50, 3)
        dot(50, 62, 3)
    }

    val frog = picture("Froggy") {
        e("head", 50, 52, 30, 20)
        c("eyeL", 36, 32, 8)
        c("eyeR", 64, 32, 8)
        dot(36, 32, 3)
        dot(64, 32, 3)
        a("mouth", 50, 52, 18, 8, 20, 140)
        pl("legL", 24, 64, 14, 80, 28, 80)
        pl("legR", 76, 64, 86, 80, 72, 80)
    }

    val penguin = picture("Penguin") {
        e("body", 50, 52, 22, 34)
        e("belly", 50, 58, 14, 24)
        dot(43, 32, 2.5)
        dot(57, 32, 2.5)
        pl("beak", 46, 36, 54, 36, 50, 42, 46, 36)
        l("wingL", 29, 44, 20, 66)
        l("wingR", 71, 44, 80, 66)
        l("footL", 44, 85, 33, 90)
        l("footR", 56, 85, 67, 90)
    }

    val fish2 = picture("Goldfish") {
        e("body", 48, 50, 28, 15)
        l("tailT", 76, 50, 92, 36)
        l("tailB", 76, 50, 92, 64)
        l("tailE", 92, 36, 92, 64)
        pl("finTop", 38, 36, 50, 24, 58, 37)
        pl("finBot", 42, 64, 50, 74, 58, 64)
        dot(32, 47, 3)
    }

    val flyingBirds = picture("Birds in the sky") {
        a("b1L", 24, 34, 7, 6, 180, 180)
        a("b1R", 38, 34, 7, 6, 180, 180)
        a("b2L", 56, 46, 7, 6, 180, 180)
        a("b2R", 70, 46, 7, 6, 180, 180)
        a("b3L", 34, 64, 7, 6, 180, 180)
        a("b3R", 48, 64, 7, 6, 180, 180)
        c("_sun", 82, 18, 8)
    }

    val snake = picture("Snake") {
        ch("z", 8, 64, 23, 42, 38, 64, 53, 42, 68, 64, 82, 44)
        c("head", 87, 38, 7)
        pl("_tongue", 93, 40, 98, 42)
    }

    val caterpillar = picture("Caterpillar") {
        c("seg1", 20, 60, 9)
        c("seg2", 36, 60, 9)
        c("seg3", 52, 60, 9)
        c("seg4", 68, 60, 9)
        c("head", 84, 54, 11)
        l("leg1", 20, 69, 20, 80)
        l("leg2", 36, 69, 36, 80)
        l("leg3", 52, 69, 52, 80)
        l("leg4", 68, 69, 68, 80)
        l("ant1", 80, 44, 76, 30)
        l("ant2", 88, 44, 92, 30)
        dot(88, 52, 2.5)
    }

    val jellyfish = picture("Jellyfish") {
        a("dome", 50, 45, 27, 25, 180, 180)
        l("domeB", 23, 45, 77, 45)
        pl("t1", 30, 45, 26, 58, 32, 70, 28, 84)
        pl("t2", 43, 45, 40, 60, 46, 74, 42, 88)
        pl("t3", 57, 45, 60, 60, 54, 74, 58, 88)
        pl("t4", 70, 45, 74, 58, 68, 70, 72, 84)
        dot(42, 36, 2.5)
        dot(58, 36, 2.5)
    }

    val crab = picture("Crab") {
        e("body", 50, 60, 24, 14)
        l("stalkL", 42, 47, 40, 34)
        l("stalkR", 58, 47, 60, 34)
        dot(40, 32, 3)
        dot(60, 32, 3)
        l("armL", 28, 54, 18, 46)
        l("armR", 72, 54, 82, 46)
        c("clawL", 14, 40, 8)
        c("clawR", 86, 40, 8)
        l("lgL1", 30, 68, 18, 78)
        l("lgL2", 38, 72, 32, 84)
        l("lgR1", 70, 68, 82, 78)
        l("lgR2", 62, 72, 68, 84)
    }

    val lion = picture("Lion") {
        c("face", 50, 52, 20)
        rays("ray", 50, 52, 25, 38, 12)
        dot(43, 48, 3)
        dot(57, 48, 3)
        pl("_nose", 46, 56, 54, 56, 50, 61, 46, 56)
    }

    // ---------------------------------------------------------------- Things world
    val car = picture("Car") {
        ch("b", 10, 62, 10, 48, 26, 48, 36, 30, 66, 30, 76, 48, 90, 48, 90, 62)
        l("base", 10, 62, 90, 62)
        l("belt", 26, 48, 76, 48)
        l("winDiv", 51, 30, 51, 48)
        c("wheelB", 30, 64, 9)
        c("wheelF", 70, 64, 9)
    }

    val bus = picture("Bus") {
        pg("bus", 10, 28, 90, 28, 90, 72, 10, 72)
        pl("w1", 16, 36, 32, 36, 32, 50, 16, 50, 16, 36)
        pl("w2", 38, 36, 54, 36, 54, 50, 38, 50, 38, 36)
        pl("w3", 60, 36, 76, 36, 76, 50, 60, 50, 60, 36)
        l("door", 82, 36, 82, 72)
        c("wh1", 28, 74, 8)
        c("wh2", 70, 74, 8)
    }

    val sailboat = picture("Sailboat") {
        pg("hull", 12, 64, 88, 64, 76, 80, 24, 80)
        l("mast", 50, 64, 50, 12)
        l("sailT", 50, 16, 82, 58)
        l("sailB", 82, 58, 50, 58)
        l("jib", 50, 22, 24, 58)
        l("jibB", 24, 58, 50, 58)
        pl("_wave", 6, 88, 18, 84, 30, 88, 42, 84, 54, 88, 66, 84, 78, 88, 94, 84)
    }

    val rocket = picture("Rocket") {
        l("noseL", 40, 34, 50, 10)
        l("noseR", 50, 10, 60, 34)
        l("sideL", 40, 34, 40, 72)
        l("sideR", 60, 34, 60, 72)
        l("bottom", 40, 72, 60, 72)
        c("win", 50, 46, 6)
        l("finL", 40, 56, 26, 78)
        l("finLb", 26, 78, 40, 72)
        l("finR", 60, 56, 74, 78)
        l("finRb", 74, 78, 60, 72)
        pl("flame", 44, 72, 50, 90, 56, 72)
    }

    val plane = picture("Airplane") {
        e("body", 50, 50, 7, 38)
        l("wingLf", 43, 44, 10, 58)
        l("wingLe", 10, 58, 10, 64)
        l("wingLb", 10, 64, 43, 58)
        l("wingRf", 57, 44, 90, 58)
        l("wingRe", 90, 58, 90, 64)
        l("wingRb", 90, 64, 57, 58)
        l("tailL", 45, 80, 32, 88)
        l("tailR", 55, 80, 68, 88)
    }

    val train = picture("Train") {
        pg("cab", 12, 30, 40, 30, 40, 70, 12, 70)
        pg("boiler", 40, 44, 84, 44, 84, 70, 40, 70)
        l("chimL", 62, 44, 62, 26)
        l("chimR", 78, 44, 78, 26)
        l("chimT", 58, 26, 82, 26)
        pg("win", 18, 36, 34, 36, 34, 50, 18, 50)
        c("wh1", 24, 74, 8)
        c("wh2", 50, 74, 8)
        c("wh3", 74, 74, 8)
        l("cow", 84, 70, 92, 78)
    }

    val bicycle = picture("Bike") {
        c("wheelB", 26, 64, 16)
        c("wheelF", 74, 64, 16)
        l("fr1", 26, 64, 44, 40)
        l("fr2", 44, 40, 68, 40)
        l("fr3", 44, 40, 50, 64)
        l("fr4", 26, 64, 50, 64)
        l("fr5", 50, 64, 68, 40)
        l("fork", 68, 40, 74, 64)
        l("stem", 68, 40, 66, 30)
        l("bar", 60, 30, 72, 30)
        l("seat", 36, 34, 48, 34)
        l("post", 44, 40, 42, 34)
    }

    val truck = picture("Truck") {
        pg("cargo", 8, 26, 60, 26, 60, 68, 8, 68)
        ch("cab", 60, 40, 78, 40, 90, 54, 90, 68)
        l("cabBase", 60, 68, 90, 68)
        l("cabWin", 64, 46, 80, 46)
        c("wh1", 24, 70, 8)
        c("wh2", 76, 70, 8)
    }

    val hotAirBalloon = picture("Hot air balloon") {
        c("bal", 50, 36, 26)
        a("_stripe", 50, 36, 12, 26, 270, 180)
        l("ropeL", 30, 53, 40, 74)
        l("ropeR", 70, 53, 60, 74)
        pg("bask", 38, 74, 62, 74, 60, 88, 40, 88)
    }

    val clock = picture("Clock") {
        c("face", 50, 50, 36)
        rays("t", 50, 50, 25, 34, 12)
        l("hour", 50, 50, 50, 32)
        l("min", 50, 50, 70, 50)
        dot(50, 50, 3)
    }

    val lamp = picture("Lamp") {
        l("shadeT", 38, 18, 62, 18)
        l("shadeL", 38, 18, 26, 44)
        l("shadeR", 62, 18, 74, 44)
        l("shadeB", 26, 44, 74, 44)
        l("pole", 50, 44, 50, 82)
        e("base", 50, 84, 17, 5)
    }

    val chair = picture("Chair") {
        l("bL", 30, 14, 30, 56)
        l("bR", 70, 14, 70, 56)
        l("bT", 30, 14, 70, 14)
        l("s1", 30, 28, 70, 28)
        l("s2", 30, 42, 70, 42)
        pg("seat", 24, 56, 76, 56, 76, 64, 24, 64)
        l("lL", 30, 64, 30, 90)
        l("lR", 70, 64, 70, 90)
    }

    val table = picture("Table") {
        pg("top", 10, 40, 90, 40, 90, 47, 10, 47)
        l("lL", 18, 47, 18, 86)
        l("lR", 82, 47, 82, 86)
        a("_cup", 50, 30, 7, 9, 0, 180)
        l("_cupT", 43, 30, 57, 30)
    }

    val pencil = picture("Pencil") {
        l("top", 18, 40, 72, 40)
        l("bot", 18, 60, 72, 60)
        l("erT", 8, 40, 18, 40)
        l("erB", 8, 60, 18, 60)
        l("erE", 8, 40, 8, 60)
        l("ferrule", 18, 40, 18, 60)
        l("wood", 72, 40, 72, 60)
        l("tipT", 72, 40, 92, 50)
        l("tipB", 72, 60, 92, 50)
    }

    val glasses = picture("Glasses") {
        c("lensL", 32, 52, 14)
        c("lensR", 68, 52, 14)
        a("bridge", 50, 50, 4, 4, 180, 180)
        l("armL", 18, 48, 6, 40)
        l("armR", 82, 48, 94, 40)
    }

    val key = picture("Key") {
        c("bow", 26, 50, 14)
        c("_hole", 26, 50, 5)
        l("shaft", 40, 50, 90, 50)
        l("t1", 64, 50, 64, 63)
        l("t2", 76, 50, 76, 61)
        l("t3", 88, 50, 88, 64)
    }

    val lollipop = picture("Lollipop") {
        c("candy", 50, 34, 22)
        a("sw", 50, 34, 12, 12, 0, 300)
        l("stick", 50, 56, 50, 92)
    }

    val drum = picture("Drum") {
        e("top", 50, 38, 28, 8)
        l("sideL", 22, 38, 22, 72)
        l("sideR", 78, 38, 78, 72)
        a("bottom", 50, 72, 28, 8, 0, 180)
        ch("zz", 22, 48, 36, 64, 50, 48, 64, 64, 78, 48)
        l("stickL", 32, 32, 14, 10)
        l("stickR", 68, 32, 86, 10)
    }

    val bell = picture("Bell") {
        a("dome", 50, 48, 22, 26, 180, 180)
        l("sideL", 28, 48, 20, 76)
        l("sideR", 72, 48, 80, 76)
        l("rim", 16, 76, 84, 76)
        c("clap", 50, 82, 5)
        c("loop", 50, 18, 4)
    }

    val candle = picture("Candle") {
        pg("candle", 38, 42, 62, 42, 62, 88, 38, 88)
        l("wick", 50, 42, 50, 34)
        pl("flame", 50, 34, 42, 24, 50, 8, 58, 24, 50, 34)
    }

    val robot = picture("Robot") {
        pg("head", 30, 16, 70, 16, 70, 46, 30, 46)
        c("eyeL", 41, 29, 5)
        c("eyeR", 59, 29, 5)
        l("mouth", 40, 39, 60, 39)
        l("ant", 50, 16, 50, 6)
        dot(50, 5, 3)
        pg("body", 26, 50, 74, 50, 74, 86, 26, 86)
        l("armL", 26, 56, 12, 74)
        l("armR", 74, 56, 88, 74)
        l("neck", 50, 46, 50, 50)
    }

    // ---------------------------------------------------------------- Brain twisters
    val snowflake = picture("Snowflake") { snowflake(50, 50, 38, 6) }

    val castle = picture("Castle") {
        pg("towerL", 8, 34, 28, 34, 28, 88, 8, 88)
        pg("towerR", 72, 34, 92, 34, 92, 88, 72, 88)
        l("wallTop", 28, 50, 72, 50)
        l("wallBot", 28, 88, 72, 88)
        l("roofL1", 6, 34, 18, 12)
        l("roofL2", 18, 12, 30, 34)
        l("roofR1", 70, 34, 82, 12)
        l("roofR2", 82, 12, 94, 34)
        a("doorArc", 50, 72, 10, 10, 180, 180)
        l("doorL", 40, 72, 40, 88)
        l("doorR", 60, 72, 60, 88)
    }

    val flower = picture("Flower") {
        c("center", 50, 36, 8)
        c("p1", 50, 20, 8)
        c("p2", 64, 28, 8)
        c("p3", 64, 44, 8)
        c("p4", 50, 52, 8)
        c("p5", 36, 44, 8)
        c("p6", 36, 28, 8)
        l("stem", 50, 60, 50, 92)
        pl("leafL", 50, 78, 40, 68, 28, 70, 38, 80, 50, 78)
        pl("leafR", 50, 72, 60, 62, 72, 64, 62, 74, 50, 72)
    }

    val ufo = picture("Flying saucer") {
        a("dome", 50, 44, 16, 14, 180, 180)
        e("body", 50, 50, 36, 9)
        dot(30, 50, 2.5)
        dot(50, 52, 2.5)
        dot(70, 50, 2.5)
        l("beamL", 38, 59, 24, 90)
        l("beamR", 62, 59, 76, 90)
        l("beamB", 24, 90, 76, 90)
    }

    /** Pictures used on stickers, in unlock order. */
    val stickerPictures: List<Picture> = listOf(
        star, heart, sun, house, fish, cat, bird, butterfly, rabbit, owl,
        bee, lion, car, rocket, sailboat, hotAirBalloon, robot, flower, castle, ufo,
    )
}
