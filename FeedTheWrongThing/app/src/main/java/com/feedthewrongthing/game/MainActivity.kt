package com.feedthewrongthing.game

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.window.OnBackInvokedDispatcher
import com.feedthewrongthing.game.ads.AdsManager
import com.feedthewrongthing.game.engine.Meshes
import com.feedthewrongthing.game.engine.MsaaConfigChooser
import com.feedthewrongthing.game.engine.RenderFrame
import com.feedthewrongthing.game.engine.Renderer3D
import com.feedthewrongthing.game.engine.ThumbJob
import com.feedthewrongthing.game.game.Game
import com.feedthewrongthing.game.models.EnvModels
import com.feedthewrongthing.game.models.ItemModels
import com.feedthewrongthing.game.ui.Ui

class MainActivity : Activity(), Choreographer.FrameCallback, Game.Platform, AdsManager.Listener {
    private lateinit var glView: GLSurfaceView
    private lateinit var overlay: OverlayView
    private lateinit var bannerBox: FrameLayout
    private lateinit var renderer: Renderer3D
    private lateinit var game: Game
    private lateinit var ads: AdsManager
    private var frame = RenderFrame()
    private var lastNanos = 0L
    private var running = false
    private var bannerPx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        Meshes.init()
        ItemModels.build()
        EnvModels.build()
        renderer = Renderer3D(Meshes.all)

        val root = FrameLayout(this)
        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(MsaaConfigChooser())
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        overlay = OverlayView(this)
        bannerBox = FrameLayout(this)
        val match = ViewGroup.LayoutParams.MATCH_PARENT
        root.addView(glView, FrameLayout.LayoutParams(match, match))
        root.addView(overlay, FrameLayout.LayoutParams(match, match))
        root.addView(bannerBox, FrameLayout.LayoutParams(match, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        setContentView(root)

        game = Game(this, this)
        overlay.ui = Ui(game)
        overlay.onSize = { w, h -> game.resize(w, h) }
        ads = AdsManager(this, bannerBox, this)

        root.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= 28) {
                val cut = insets.displayCutout
                game.safeLeft = cut?.safeInsetLeft ?: 0
                game.safeTop = cut?.safeInsetTop ?: 0
                game.safeRight = cut?.safeInsetRight ?: 0
                game.safeBottom = cut?.safeInsetBottom ?: 0
            }
            insets
        }

        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { game.onBack() }
        }

        game.start()
        ads.init()
        hideSystemBars()
    }

    @Deprecated("Used only below Android 13; newer versions use OnBackInvokedCallback.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        game.onBack()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (Build.VERSION.SDK_INT < 35) {
                // Android 15+ is always edge-to-edge; older versions need to opt in.
                @Suppress("DEPRECATION")
                window.setDecorFitsSystemWindows(false)
            }
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
        game.onResume()
        ads.onResume()
        if (!running) {
            running = true
            lastNanos = 0L
            Choreographer.getInstance().postFrameCallback(this)
        }
        hideSystemBars()
    }

    override fun onPause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        glView.onPause()
        game.onPause()
        ads.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        ads.onDestroy()
        game.audio.release()
        super.onDestroy()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val dt = if (lastNanos == 0L) 1f / 60f else ((frameTimeNanos - lastNanos) / 1e9f)
        lastNanos = frameTimeNanos
        game.update(dt)
        game.render(frame)
        frame = renderer.submit(frame)
        glView.requestRender()
        overlay.invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    // ------------------------------------------------------------------ Game.Platform

    override fun setBannerVisible(visible: Boolean) = ads.setBannerVisible(visible)

    override fun bannerHeightPx(): Int = bannerPx

    override fun showInterstitial(then: () -> Unit) = ads.showInterstitial(then)

    override fun isRewardedReady(): Boolean = ads.isRewardedReady()

    override fun showRewarded(onDone: (earned: Boolean) -> Unit) = ads.showRewarded(onDone)

    override fun exitApp() {
        finishAndRemoveTask()
    }

    override fun openStore() {
        val id = packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")))
            } catch (_: ActivityNotFoundException) {
            }
        }
    }

    override fun vibrate() {
        overlay.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun requestThumbnails(jobs: List<ThumbJob>) {
        renderer.requestThumbnails(jobs) { key, bmp -> game.onThumb(key, bmp) }
        glView.requestRender()
    }

    // ------------------------------------------------------------------ AdsManager.Listener

    override fun onFullScreenAdShowing(showing: Boolean) {
        game.audio.duck(showing)
    }

    override fun onBannerHeightChanged(px: Int) {
        bannerPx = px
    }
}

/** Transparent view on top of the 3D surface that draws the 2D interface. */
class OverlayView(context: Context) : View(context) {
    var ui: Ui? = null
    var onSize: ((Int, Int) -> Unit)? = null

    init {
        isHapticFeedbackEnabled = true
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSize?.invoke(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        ui?.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return ui?.onTouch(event) ?: false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
