package com.feedthewrongthing.game.game

import android.content.Context
import android.graphics.Bitmap
import com.feedthewrongthing.game.audio.AudioEngine
import com.feedthewrongthing.game.audio.Music
import com.feedthewrongthing.game.audio.Sfx
import com.feedthewrongthing.game.engine.Camera
import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.engine.RenderFrame
import com.feedthewrongthing.game.engine.ThumbJob
import com.feedthewrongthing.game.models.CreatureModels
import com.feedthewrongthing.game.models.EnvModels
import com.feedthewrongthing.game.models.ItemModels
import com.feedthewrongthing.game.models.Pose
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * The whole game state machine. Runs on the main thread: it updates the simulation,
 * fills render frames for the GL thread and exposes state for the 2D overlay.
 */
class Game(context: Context, private val platform: Platform) : WorldHost {

    /** Things only the Android activity can do. */
    interface Platform {
        fun setBannerVisible(visible: Boolean)
        fun bannerHeightPx(): Int
        fun showInterstitial(then: () -> Unit)
        fun isRewardedReady(): Boolean
        fun showRewarded(onDone: (earned: Boolean) -> Unit)
        fun exitApp()
        fun openStore()
        fun vibrate()
        fun requestThumbnails(jobs: List<ThumbJob>)
    }

    enum class Screen { SPLASH, HOME, MAP, PLAY, RESULTS, COLLECTION }
    enum class Modal { NONE, PAUSE, SETTINGS, EXIT, GATE, GIFT, REWARD_OFFER, ISLAND, INTRO, RESET }

    val save = SaveData(context)
    val audio = AudioEngine(context)
    override val particles = Particles()
    override val density: Float = context.resources.displayMetrics.density
    private val rnd = Random(System.nanoTime())

    var screen = Screen.SPLASH
        private set
    var modal = Modal.NONE
        private set
    var screenT = 0f
        private set
    var modalT = 0f
        private set
    var time = 0f
        private set
    var width = 1
        private set
    var height = 1
        private set
    var safeLeft = 0
    var safeTop = 0
    var safeRight = 0
    var safeBottom = 0

    var world: PlayWorld? = null
        private set
    var levelIndex = 0
        private set
    val isEndless: Boolean get() = levelIndex >= Levels.all.size
    val levelDef: LevelDef? get() = world?.def

    val thumbs = HashMap<String, Bitmap>()
    val stage = MenuStage(this)
    val camera = Camera()
    val menuPops = ArrayList<PopText>()
    private val ctx = DrawCtx()

    // Map
    var mapPan = 0f
        private set
    private var mapVel = 0f
    private var mapDragX = 0f
    private var mapDownX = 0f
    private var mapDownY = 0f
    private var mapDragging = false
    private var mapMoved = false
    var selectedIsland = 0
        private set
    var lockedWiggle = 0f
        private set
    var lockedIsland = -1
        private set

    // Results
    var resultStars = 0
        private set
    var resultNewFinds = 0
        private set
    var resultNewIsland = -1
        private set
    var resultBest = false
        private set
    var resultWord = "YUMMY!"
        private set
    var giftEligible = false
        private set

    // Gift
    var giftHat: Hat? = null
        private set
    var giftOpened = false
        private set
    var giftFromDaily = false
        private set
    var rewardFallback = false
        private set

    // Gate
    var gateQuestion = ""
        private set
    var gateAnswers = IntArray(3)
        private set
    private var gateCorrect = 0
    private var gateAction: (() -> Unit)? = null
    private var gateReturn = Modal.NONE

    // Collection
    var colIndex = 0
        private set
    var colSpin = 0f
        private set
    var colSelected: ItemType? = null
        private set
    private var colDragX = 0f

    // Transitions
    var fade = 0f
        private set
    private var fadeDir = 0
    private var pending: (() -> Unit)? = null
    private var bannerShown = false
    private var attractT = 0f
    private var splashSockDone = false
    var tipTimer = 0f
        private set

    private val islandCount = Levels.islandCount + 1

    init {
        audio.sfxOn = save.sfxOn
        audio.musicOn = save.musicOn
        save.launches++
        save.saveSettings()
        stage.onEat = { _, _ -> }
    }

    fun start() {
        audio.start()
        platform.requestThumbnails(buildThumbJobs())
        enterSplash()
    }

    fun resize(w: Int, h: Int) { width = max(1, w); height = max(1, h) }

    fun onThumb(key: String, bmp: Bitmap) { thumbs[key] = bmp }

    // ------------------------------------------------------------------ Fx / WorldHost

    override fun sfx(s: Sfx, volume: Float, rate: Float) = audio.play(s, volume, rate)

    override fun pop(text: String, x: Float, y: Float, z: Float, color: Int) {
        val w = world
        if (screen == Screen.PLAY && w != null) w.popTexts.add(PopText(text, x, y, z, color))
        else menuPops.add(PopText(text, x, y, z, color))
    }

    override fun shake(amount: Float) { world?.addShake(amount) }

    override fun hatFor(t: CreatureType): Hat = save.hatOf(t)

    override fun recordDiscovery(c: CreatureType, item: ItemType, r: ReactionType): Boolean = save.discover(c, item)

    override fun haptic() { if (save.hapticsOn) platform.vibrate() }

    // ------------------------------------------------------------------ navigation

    private fun go(action: () -> Unit) {
        if (fadeDir != 0) return
        pending = action
        fadeDir = 1
        audio.play(Sfx.WHOOSH, 0.5f, 1.2f)
    }

    private fun setScreen(s: Screen) {
        screen = s
        screenT = 0f
        modal = Modal.NONE
        particles.clear()
        menuPops.clear()
        stage.clear()
    }

    private fun enterSplash() {
        setScreen(Screen.SPLASH)
        val m = stage.add(CreatureType.MUNCHY, 0f, 0f, 0f)
        m.yOffset = 7f
        splashSockDone = false
        audio.music(Music.HOME)
    }

    fun goHome() = go { enterHome() }

    private fun enterHome() {
        world = null
        setScreen(Screen.HOME)
        stage.add(CreatureType.MUNCHY, homeIslandX(), 0f, 0f, 1.1f)
        attractT = 1.5f
        audio.music(Music.HOME)
    }

    fun homeIslandX(): Float = if (width >= height) -2.6f else 0f

    fun openMap(focusIsland: Int = -1) = go {
        world = null
        setScreen(Screen.MAP)
        val target = if (focusIsland >= 0) focusIsland else currentIslandForMap()
        selectedIsland = target.coerceIn(0, islandCount - 1)
        mapPan = selectedIsland * ISLAND_GAP
        mapVel = 0f
        audio.music(Music.HOME)
    }

    private fun currentIslandForMap(): Int {
        val next = (save.unlocked - 1).coerceAtMost(Levels.all.size)
        return if (next >= Levels.all.size) Levels.islandCount else next / Levels.PER_ISLAND
    }

    /** Arrow buttons on the map glide to the neighbouring island. */
    fun nudgeMap(d: Int) {
        selectedIsland = (selectedIsland + d).coerceIn(0, islandCount - 1)
        mapVel = d * ISLAND_GAP * 4.8f
        audio.play(Sfx.WHOOSH, 0.4f, 1.3f)
    }

    fun openCollection() = go {
        world = null
        setScreen(Screen.COLLECTION)
        colIndex = colIndex.coerceIn(0, CreatureType.entries.size - 1)
        colSelected = null
        buildCollectionStage()
        audio.music(Music.HOME)
    }

    private fun buildCollectionStage() {
        stage.clear()
        val x = if (width >= height) -3.2f else 0f
        stage.add(CreatureType.entries[colIndex], x, 0.02f, 0f, if (CreatureType.entries[colIndex] == CreatureType.BIG_MAMA) 1.15f else 1.3f)
    }

    fun startLevel(index: Int) = go { beginLevel(index) }

    private fun beginLevel(index: Int) {
        levelIndex = index
        val def = if (index >= Levels.all.size) Levels.endless(index - Levels.all.size) else Levels.all[index]
        setScreen(Screen.PLAY)
        world = PlayWorld(def, this)
        world?.setupCamera(width, height)
        audio.music(def.island.music)
        tipTimer = if (def.tip != null) 7f else 0f
        val firstOfIsland = index < Levels.all.size && index % Levels.PER_ISLAND == 0 && save.stars(index) == 0
        if (firstOfIsland) openModal(Modal.INTRO)
    }

    fun restartLevel() = go { beginLevel(levelIndex) }

    private fun nextLevelIndex(): Int {
        return if (isEndless) levelIndex + 1
        else if (levelIndex + 1 < Levels.all.size) levelIndex + 1
        else Levels.all.size + save.endlessBest
    }

    fun nextLevel() {
        if (giftEligible && !save.giftClaimed(levelIndex)) { openModal(Modal.REWARD_OFFER); return }
        val next = nextLevelIndex()
        withAd { startLevel(next) }
    }

    fun resultsToMap() {
        val focus = if (isEndless) Levels.islandCount else min(Levels.islandCount, nextLevelIndex() / Levels.PER_ISLAND)
        withAd { openMap(focus) }
    }

    /** Shows an interstitial at a natural break (the platform enforces the 30 s cap). */
    private fun withAd(then: () -> Unit) {
        if (save.levelsPlayed < 2) { then(); return }
        platform.showInterstitial(then)
    }

    fun quitLevelToMap() { withAd { openMap(world?.def?.island?.ordinal?.coerceAtMost(Levels.islandCount) ?: -1) } }

    fun quitLevelToHome() { withAd { goHome() } }

    // ------------------------------------------------------------------ modals

    fun openModal(m: Modal) {
        modal = m
        modalT = 0f
        world?.cancelTouch()
        audio.play(Sfx.POP, 0.6f)
    }

    fun closeModal() {
        if (modal == Modal.INTRO) audio.play(Sfx.YAY, 0.7f)
        modal = Modal.NONE
    }

    fun openIsland(i: Int) {
        if (i >= Levels.islandCount) {
            if (save.isIslandUnlocked(i)) startLevel(Levels.all.size + save.endlessBest)
            else lockedTap(i)
            return
        }
        if (!save.isIslandUnlocked(i)) { lockedTap(i); return }
        selectedIsland = i
        openModal(Modal.ISLAND)
    }

    private fun lockedTap(i: Int) {
        lockedIsland = i
        lockedWiggle = 1f
        audio.play(Sfx.BONK, 0.6f)
    }

    fun openSettings() = openModal(Modal.SETTINGS)

    fun toggleMusic() { save.musicOn = !save.musicOn; audio.musicOn = save.musicOn; save.saveSettings() }
    fun toggleSfx() { save.sfxOn = !save.sfxOn; audio.sfxOn = save.sfxOn; save.saveSettings(); audio.play(Sfx.CLICK) }
    fun toggleHaptics() { save.hapticsOn = !save.hapticsOn; save.saveSettings(); haptic() }

    /** Grown-up check before leaving the app or wiping progress. */
    fun askGate(returnTo: Modal, action: () -> Unit) {
        val a = 6 + rnd.nextInt(4)
        val b = 6 + rnd.nextInt(4)
        gateCorrect = a * b
        gateQuestion = "$a × $b = ?"
        val wrong1 = gateCorrect + (if (rnd.nextBoolean()) a else -b)
        var wrong2 = gateCorrect + (if (rnd.nextBoolean()) 10 else -10)
        if (wrong2 == wrong1) wrong2 += 3
        val list = mutableListOf(gateCorrect, wrong1, wrong2).shuffled(rnd)
        gateAnswers = list.toIntArray()
        gateAction = action
        gateReturn = returnTo
        openModal(Modal.GATE)
    }

    fun answerGate(v: Int) {
        if (v == gateCorrect) {
            val a = gateAction
            gateAction = null
            modal = Modal.NONE
            a?.invoke()
        } else {
            audio.play(Sfx.BONK)
            modal = gateReturn
            modalT = 0f
        }
    }

    fun cancelGate() { modal = gateReturn; modalT = 0f; gateAction = null }

    fun rateUs() = askGate(Modal.EXIT) { platform.openStore() }

    fun exitApp() { save.save(); platform.exitApp() }

    fun resetProgress() = askGate(Modal.SETTINGS) {
        modal = Modal.RESET
        modalT = 0f
    }

    fun confirmReset() {
        save.resetAll()
        audio.play(Sfx.POOF)
        modal = Modal.NONE
        enterHome()
    }

    // Daily gift ----------------------------------------------------------

    private fun today(): Long = System.currentTimeMillis() / 86_400_000L

    val dailyGiftReady: Boolean get() = save.lastGiftDay != today() && Hat.entries.any { it != Hat.NONE && !save.ownsHat(it) }

    fun openDailyGift() {
        giftFromDaily = true
        giftOpened = false
        giftHat = null
        rewardFallback = false
        openModal(Modal.GIFT)
    }

    fun openGiftBox() {
        if (giftOpened) return
        giftOpened = true
        giftHat = save.grantNextHat()
        if (giftFromDaily) { save.lastGiftDay = today(); save.saveSettings() }
        audio.play(Sfx.UNLOCK)
        particles.confetti(0f, 2f, 0f, 30)
        haptic()
    }

    fun tryOnGift() {
        val h = giftHat ?: return
        save.equip(CreatureType.MUNCHY, h)
        closeModal()
        colIndex = 0
        openCollection()
    }

    // Rewarded gift every few levels ---------------------------------------

    fun watchRewardedForGift() {
        val level = levelIndex
        if (platform.isRewardedReady()) {
            platform.showRewarded { earned ->
                if (earned) grantRewardGift(level, false) else { modal = Modal.NONE }
            }
        } else {
            // No video available (offline etc.): be kind, the gift opens anyway.
            grantRewardGift(level, true)
        }
    }

    private fun grantRewardGift(level: Int, fallback: Boolean) {
        save.claimGift(level)
        giftFromDaily = false
        giftOpened = false
        giftHat = null
        rewardFallback = fallback
        openModal(Modal.GIFT)
        openGiftBox()
    }

    fun skipRewardOffer() {
        save.claimGift(levelIndex)
        modal = Modal.NONE
        nextLevel()
    }

    // Collection -------------------------------------------------------------

    fun collectionNext(d: Int) {
        colIndex = (colIndex + d + CreatureType.entries.size) % CreatureType.entries.size
        colSelected = null
        buildCollectionStage()
        audio.play(Sfx.POP, 0.5f, 1.2f)
    }

    fun collectionTapItem(item: ItemType) {
        val c = CreatureType.entries[colIndex]
        if (!save.creatureMet(c)) return
        colSelected = item
        if (save.discovered(c, item)) {
            val a = stage.actors.firstOrNull() ?: return
            stage.toss(a, item, a.x + 4f, 4f, 3f, 0.7f)
            audio.play(Sfx.WHOOSH, 0.5f)
        } else audio.play(Sfx.CLICK)
    }

    fun equipHat(h: Hat) {
        val c = CreatureType.entries[colIndex]
        if (!save.creatureMet(c) || !save.ownsHat(h)) return
        save.equip(c, h)
        stage.actors.firstOrNull()?.let { it.wake(this); particles.sparkles(it.x, 2.4f, it.z, 10, 0.8f) }
        audio.play(Sfx.SPARKLE, 0.7f)
    }

    // ------------------------------------------------------------------ back button

    fun onBack() {
        if (fadeDir != 0) return
        when (modal) {
            Modal.NONE -> {}
            Modal.GATE -> { cancelGate(); return }
            Modal.RESET -> { modal = Modal.SETTINGS; return }
            Modal.REWARD_OFFER -> { skipRewardOffer(); return }
            else -> { closeModal(); return }
        }
        when (screen) {
            Screen.SPLASH -> {}
            Screen.HOME -> openModal(Modal.EXIT)
            Screen.MAP -> goHome()
            Screen.PLAY -> if (world?.done != true) openModal(Modal.PAUSE)
            Screen.RESULTS -> resultsToMap()
            Screen.COLLECTION -> goHome()
        }
    }

    // ------------------------------------------------------------------ input from overlay (non-button touches)

    fun sceneDown(x: Float, y: Float) {
        when (screen) {
            Screen.PLAY -> if (modal == Modal.NONE) world?.touchDown(x, y)
            Screen.MAP -> { mapDragging = true; mapMoved = false; mapDownX = x; mapDownY = y; mapDragX = x; mapVel = 0f }
            Screen.SPLASH -> if (screenT > 1f) goHome()
            Screen.HOME -> tapHomeCreature(x, y)
            Screen.COLLECTION -> colDragX = x
            Screen.RESULTS -> {}
        }
    }

    fun sceneMove(x: Float, y: Float) {
        when (screen) {
            Screen.PLAY -> world?.touchMove(x, y)
            Screen.MAP -> if (mapDragging) {
                val dx = x - mapDragX
                mapDragX = x
                if (hypot(x - mapDownX, y - mapDownY) > 12f * density) mapMoved = true
                val upp = ISLAND_GAP / (width * 0.55f)
                mapPan -= dx * upp
                mapVel = -dx * upp * 60f
            }
            Screen.COLLECTION -> { colSpin += (x - colDragX) * 0.4f; colDragX = x }
            else -> {}
        }
    }

    fun sceneUp(x: Float, y: Float) {
        when (screen) {
            Screen.PLAY -> world?.touchUp(x, y)
            Screen.MAP -> {
                mapDragging = false
                if (!mapMoved) tapMap(x, y)
            }
            else -> {}
        }
    }

    fun sceneCancel() { world?.cancelTouch(); mapDragging = false }

    private val tp = FloatArray(3)

    private fun tapMap(x: Float, y: Float) {
        var best = -1
        var bestD = Float.MAX_VALUE
        for (i in 0 until islandCount) {
            if (!camera.project(i * ISLAND_GAP, 0.8f, 0f, tp)) continue
            val d = hypot(tp[0] - x, tp[1] - y)
            val reach = camera.pixelsPerUnit(tp[2]) * 3.4f
            if (d < reach && d < bestD) { best = i; bestD = d }
        }
        if (best >= 0) {
            selectedIsland = best
            openIsland(best)
        }
    }

    private fun tapHomeCreature(x: Float, y: Float) {
        val a = stage.actors.firstOrNull() ?: return
        if (!camera.project(a.x, 1.1f, a.z, tp)) return
        val reach = camera.pixelsPerUnit(tp[2]) * 1.4f
        if (hypot(tp[0] - x, tp[1] - y) < reach) {
            if (stage.busy) a.bonk(this) else tossRandomAtHome()
        }
    }

    private fun tossRandomAtHome() {
        val a = stage.actors.firstOrNull() ?: return
        val item = ItemType.entries[rnd.nextInt(ItemType.entries.size)]
        val side = if (rnd.nextBoolean()) 1f else -1f
        stage.toss(a, item, a.x + side * 6f, 5f, 3f, 0.85f)
        audio.play(Sfx.WHOOSH, 0.4f)
    }

    // ------------------------------------------------------------------ update

    fun update(dt0: Float) {
        val dt = dt0.coerceIn(0f, 0.05f)
        time += dt
        screenT += dt
        modalT += dt
        if (lockedWiggle > 0f) lockedWiggle = max(0f, lockedWiggle - dt * 2f)
        if (tipTimer > 0f && modal == Modal.NONE) tipTimer -= dt

        if (fadeDir != 0) {
            fade += dt * 4.5f * fadeDir
            if (fadeDir > 0 && fade >= 1f) {
                fade = 1f
                pending?.invoke()
                pending = null
                fadeDir = -1
            } else if (fadeDir < 0 && fade <= 0f) {
                fade = 0f
                fadeDir = 0
            }
        }

        when (screen) {
            Screen.SPLASH -> updateSplash(dt)
            Screen.HOME -> {
                attractT -= dt
                if (attractT <= 0f && modal == Modal.NONE) {
                    attractT = 4.2f + rnd.nextFloat() * 1.5f
                    if (!stage.busy) tossRandomAtHome()
                }
                stage.update(dt, camera.eye[0], camera.eye[1] * 0.4f, camera.eye[2])
                particles.update(dt)
            }
            Screen.MAP -> {
                if (!mapDragging) {
                    mapPan += mapVel * dt
                    mapVel *= (1f - min(1f, dt * 5f))
                    if (abs(mapVel) < 2f) {
                        val snap = (mapPan / ISLAND_GAP).roundToInt().coerceIn(0, islandCount - 1)
                        mapPan += (snap * ISLAND_GAP - mapPan) * min(1f, dt * 6f)
                        selectedIsland = snap
                    }
                }
                mapPan = mapPan.coerceIn(-2f, (islandCount - 1) * ISLAND_GAP + 2f)
                particles.update(dt)
            }
            Screen.PLAY -> {
                val w = world
                if (w != null) {
                    w.setupCamera(width, height)
                    if (modal == Modal.NONE) w.update(dt)
                    if (w.finished) finishLevel(w)
                }
            }
            Screen.RESULTS -> {
                stage.update(dt, camera.eye[0], camera.eye[1], camera.eye[2])
                particles.update(dt)
                if (rnd.nextFloat() < dt * 2.5f) particles.confetti(-2f + rnd.nextFloat() * 4f, 6f, -1f, 10, 3f)
                val shownBefore = starsShown(screenT - dt)
                val shownNow = starsShown(screenT)
                if (shownNow > shownBefore) {
                    audio.play(Sfx.STAR, 0.9f, 1f + 0.15f * (shownNow - 1))
                    haptic()
                }
            }
            Screen.COLLECTION -> {
                colSpin *= (1f - min(1f, dt * 1.5f))
                stage.actors.firstOrNull()?.let { it.pose.yaw = 0f }
                stage.update(dt, camera.eye[0], camera.eye[1], camera.eye[2])
                particles.update(dt)
            }
        }
        val it = menuPops.iterator()
        while (it.hasNext()) { val p = it.next(); p.age += dt; if (p.age > p.life) it.remove() }

        val bannerWanted = (screen == Screen.HOME || screen == Screen.MAP || screen == Screen.COLLECTION) &&
            modal == Modal.NONE && fadeDir == 0
        if (bannerWanted != bannerShown) {
            bannerShown = bannerWanted
            platform.setBannerVisible(bannerWanted)
        }
    }

    fun starsShown(t: Float): Int = when {
        t < 0.6f -> 0
        t < 1.0f -> min(1, resultStars)
        t < 1.4f -> min(2, resultStars)
        else -> resultStars
    }

    private fun updateSplash(dt: Float) {
        val m = stage.actors.firstOrNull() ?: return
        if (screenT > 0.15f && m.yOffset > 0f && !splashSockDone && screenT < 0.25f) audio.play(Sfx.WHOOSH, 0.5f, 0.8f)
        if (screenT > 0.9f && !splashSockDone) {
            splashSockDone = true
            stage.toss(m, ItemType.SOCK, 7f, 4f, 2f, 0.75f)
            audio.play(Sfx.WHOOSH, 0.6f)
        }
        stage.update(dt, 0f, 2f, 10f)
        particles.update(dt)
        if (screenT > 4.2f && fadeDir == 0) goHome()
    }

    private fun finishLevel(w: PlayWorld) {
        val stars = w.stars()
        resultStars = stars
        resultNewFinds = w.newFinds
        resultNewIsland = -1
        resultBest = false
        if (isEndless) {
            val k = levelIndex - Levels.all.size
            resultBest = k + 1 > save.endlessBest
            save.completeEndless(k)
            giftEligible = false
        } else {
            val before = save.stars(levelIndex)
            resultBest = stars > before
            val newIsland = save.completeLevel(levelIndex, stars)
            if (newIsland) resultNewIsland = (levelIndex + 1) / Levels.PER_ISLAND
            giftEligible = (levelIndex + 1) % 5 == 0 && !save.giftClaimed(levelIndex) &&
                Hat.entries.any { it != Hat.NONE && !save.ownsHat(it) }
        }
        resultWord = listOf("YUMMY!", "DELICIOUS!", "BURP-TASTIC!", "SILLY-LICIOUS!", "MUNCH-TACULAR!", "SCRUMPTIOUS!")[rnd.nextInt(6)]
        val main = w.def.creatures.firstOrNull()?.type ?: CreatureType.MUNCHY
        world = null
        setScreen(Screen.RESULTS)
        val x = if (width >= height) -3f else 0f
        stage.add(main, x, 0.02f, 0f, if (main == CreatureType.BIG_MAMA) 1.1f else 1.25f).celebrate()
        audio.music(Music.HOME)
        audio.play(Sfx.YAY)
    }

    // ------------------------------------------------------------------ rendering

    fun render(f: RenderFrame) {
        f.reset()
        ctx.begin(f)
        ctx.time = time
        when (screen) {
            Screen.PLAY -> {
                val w = world ?: return
                w.draw(ctx, f)
            }
            Screen.SPLASH -> renderSplash(f)
            Screen.HOME -> renderHome(f)
            Screen.MAP -> renderMap(f)
            Screen.RESULTS -> renderPodium(f, 0x7A5BD6, 0x2B1D5C, 0xFF8FC8)
            Screen.COLLECTION -> renderPodium(f, 0xC7B8FF, 0xF1ECFF, 0xFFD23F)
        }
    }

    private fun skyDefault(f: RenderFrame) = EnvModels.lighting(f, Island.PICNIC)

    private fun renderSplash(f: RenderFrame) {
        skyDefault(f)
        camera.setup(0f, 2.8f, 11f, 0f, 1.6f, 0f, 40f, width, height)
        camera.copyInto(f)
        ctx.push(); ctx.translate(0f, 0f, 0f); EnvModels.drawIsland(ctx, Island.PICNIC, time); ctx.pop()
        clouds(f)
        stage.draw(ctx, { save.hatOf(it) })
        particles.draw(ctx)
    }

    private fun renderHome(f: RenderFrame) {
        skyDefault(f)
        val landscape = width >= height
        val ix = homeIslandX()
        if (landscape) camera.setup(0f, 3.0f, 11.5f, 0f, 1.5f, 0f, 40f, width, height)
        else camera.setup(0f, 3.6f, 14f, 0f, 2.6f, 0f, 44f, width, height)
        camera.copyInto(f)
        ctx.push()
        ctx.translate(ix, 0.15f * sin(time * 1.2f), 0f)
        EnvModels.drawIsland(ctx, Island.PICNIC, time)
        ctx.pop()
        stage.actors.firstOrNull()?.baseY = 0.15f * sin(time * 1.2f)
        clouds(f)
        stage.draw(ctx, { save.hatOf(it) })
        particles.draw(ctx)
    }

    private fun clouds(f: RenderFrame) {
        for (k in 0 until 5) {
            val x = ((k * 7.3f + time * 0.4f) % 36f) - 18f
            ctx.part(com.feedthewrongthing.game.engine.Meshes.cloud, 0xFFFFFF, x, 5.5f + (k % 3) * 1.6f, -12f - k * 2f,
                1.4f + (k % 2) * 0.5f, gloss = 0f, rim = 0.25f)
        }
        f.fog[0] = 30f; f.fog[1] = 70f
    }

    private val mapPose = Pose()

    private fun renderMap(f: RenderFrame) {
        EnvModels.lighting(f, Island.PICNIC)
        f.skyTop[0] = 0.36f; f.skyTop[1] = 0.66f; f.skyTop[2] = 1f
        f.skyBottom[0] = 0.96f; f.skyBottom[1] = 0.9f; f.skyBottom[2] = 1f
        val landscape = width >= height
        val ez = if (landscape) 14f else 19f
        camera.setup(mapPan, 4.6f, ez, mapPan, 0.8f, 0f, 40f, width, height)
        camera.copyInto(f)
        for (i in 0 until islandCount) {
            val x = i * ISLAND_GAP
            if (abs(x - mapPan) > 26f) continue
            val island = if (i >= Levels.islandCount) Island.FEAST else Island.entries[i]
            val unlocked = save.isIslandUnlocked(i)
            val wig = if (i == lockedIsland) sin(lockedWiggle * 30f) * lockedWiggle * 8f else 0f
            ctx.push()
            ctx.translate(x, 0.25f * sin(time * 1.1f + i), 0f)
            ctx.rotateZ(wig)
            ctx.scale(0.9f)
            EnvModels.drawIsland(ctx, island, time, locked = !unlocked)
            mapPose.reset()
            mapPose.time = time
            mapPose.hat = save.hatOf(island.creature)
            mapPose.sy = 1f + 0.03f * sin(time * 2.4f + i)
            mapPose.mouth = 0.15f
            mapPose.lookY = 0.2f
            ctx.push()
            ctx.translate(0.3f, 0.02f, 0.8f)
            ctx.scale(if (island == Island.FEAST) 0.95f else 0.85f)
            if (!unlocked) { ctx.tintColor = 0x2B2340; ctx.tintAmount = 1f }
            CreatureModels.draw(ctx, island.creature, mapPose)
            ctx.tintAmount = 0f
            ctx.pop()
            ctx.pop()
        }
        for (k in 0 until 8) {
            val x = mapPan - 20f + ((k * 6.1f + time * 0.5f) % 40f)
            ctx.part(com.feedthewrongthing.game.engine.Meshes.cloud, 0xFFFFFF, x, -4.5f + (k % 3) * 0.8f, -6f + (k % 4) * 2.5f,
                2.2f, gloss = 0f, rim = 0.3f)
        }
        particles.draw(ctx)
    }

    private fun renderPodium(f: RenderFrame, top: Int, bottom: Int, podium: Int) {
        EnvModels.lighting(f, Island.PICNIC)
        EnvModels.rgb(top, f.skyTop)
        EnvModels.rgb(bottom, f.skyBottom)
        EnvModels.rgb(bottom, f.fogCol)
        val landscape = width >= height
        if (landscape) camera.setup(0f, 2.8f, 10.5f, 0f, 1.5f, 0f, 40f, width, height)
        else camera.setup(0f, 3.5f, 13f, 0f, 3.2f, 0f, 44f, width, height)
        camera.copyInto(f)
        val a = stage.actors.firstOrNull()
        if (a != null) {
            ctx.push(); ctx.translate(a.x, 0f, a.z); ctx.scale(1.25f)
            EnvModels.podium(ctx, podium)
            ctx.pop()
            if (screen == Screen.COLLECTION) {
                a.pose.yaw = colSpin + sin(time * 0.6f) * 20f
            }
        }
        val c = CreatureType.entries.getOrNull(colIndex)
        stage.draw(ctx, { save.hatOf(it) }, { screen == Screen.COLLECTION && c != null && !save.creatureMet(it) })
        particles.draw(ctx)
    }

    // ------------------------------------------------------------------ thumbnails

    private fun buildThumbJobs(): List<ThumbJob> {
        val jobs = ArrayList<ThumbJob>()
        val cam = Camera()
        val c = DrawCtx()
        for (t in ItemType.entries) {
            val f = RenderFrame(200)
            f.reset(); c.begin(f)
            EnvModels.lighting(f, Island.PICNIC)
            cam.setup(0f, 0.62f, 1.6f, 0f, 0f, 0f, 30f, 1, 1)
            cam.copyInto(f)
            c.push(); c.rotateX(12f); c.rotateY(-28f)
            ItemModels.draw(c, t)
            c.pop()
            jobs.add(ThumbJob("i:${t.name}", f, 160))
        }
        val pose = Pose()
        for (t in CreatureType.entries) {
            val f = RenderFrame(400)
            f.reset(); c.begin(f)
            EnvModels.lighting(f, Island.PICNIC)
            val tall = if (t == CreatureType.GULPO) 1.3f else 1f
            cam.setup(0f, 1.3f * tall, 5.2f * tall, 0f, 1.05f * tall, 0f, 32f, 1, 1)
            cam.copyInto(f)
            pose.reset(); pose.mouth = 0.25f; pose.lookY = -0.2f
            CreatureModels.draw(c, t, pose)
            jobs.add(ThumbJob("c:${t.name}", f, 200))
        }
        for (h in Hat.entries) {
            if (h == Hat.NONE) continue
            val f = RenderFrame(400)
            f.reset(); c.begin(f)
            EnvModels.lighting(f, Island.PICNIC)
            cam.setup(0f, 1.9f, 4.6f, 0f, 1.45f, 0f, 34f, 1, 1)
            cam.copyInto(f)
            pose.reset(); pose.hat = h; pose.mouth = 0.3f
            CreatureModels.draw(c, CreatureType.MUNCHY, pose)
            jobs.add(ThumbJob("h:${h.name}", f, 160))
        }
        return jobs
    }

    fun thumb(item: ItemType): Bitmap? = thumbs["i:${item.name}"]
    fun thumb(c: CreatureType): Bitmap? = thumbs["c:${c.name}"]
    fun thumb(h: Hat): Bitmap? = thumbs["h:${h.name}"]

    fun bannerHeight(): Int = platform.bannerHeightPx()

    fun onPause() { save.save(); audio.onPause(); world?.cancelTouch() }

    fun onResume() { audio.onResume() }

    companion object {
        const val ISLAND_GAP = 9f
    }
}
