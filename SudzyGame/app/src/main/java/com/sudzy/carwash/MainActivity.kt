package com.sudzy.carwash

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.window.OnBackInvokedDispatcher
import com.sudzy.carwash.engine.AndroidAudio
import com.sudzy.carwash.engine.Audio
import com.sudzy.carwash.engine.Fonts
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Platform
import com.sudzy.carwash.scenes.SplashScene

class MainActivity : Activity(), Platform {
    private lateinit var game: Game
    private lateinit var view: GameView
    private lateinit var sound: AndroidAudio
    private lateinit var bannerBox: FrameLayout
    private val ads get() = (application as SudzyApp).ads

    override val audio: Audio get() = sound

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Fonts.title = Typeface.createFromAsset(assets, "fonts/lilita_one.ttf")
        Fonts.body = Typeface.createFromAsset(assets, "fonts/fredoka.ttf")
        sound = AndroidAudio(this)
        game = Game(this, this)
        view = GameView(this, game)

        // Game on top, banner in its own strip at the bottom so it never covers gameplay.
        bannerBox = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(16, 83, 224))
            minimumHeight = ads.bannerSize(this@MainActivity).getHeightInPixels(this@MainActivity)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(bannerBox, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root)
        hideSystemBars()

        game.start(SplashScene(game))
        ads.initialize(this) { ads.loadBanner(this, bannerBox) }

        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { handleBack() }
        }
    }

    private fun handleBack() { if (!game.back()) finish() }

    @Deprecated("Pre-Android 13 back handling")
    override fun onBackPressed() { handleBack() }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        view.resume(); sound.resumeAll(); ads.resumeBanner()
    }

    override fun onPause() {
        view.pause(); sound.pauseAll(); ads.pauseBanner()
        super.onPause()
    }

    override fun onDestroy() {
        ads.destroyBanner(); sound.release()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ Platform
    override fun showInterstitial(onDone: () -> Unit) = ads.showInterstitial(this, onDone)

    override fun vibrate(ms: Long, strength: Int) {
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        if (vib?.hasVibrator() != true) return
        vib.vibrate(VibrationEffect.createOneShot(ms, strength.coerceIn(1, 255)))
    }

    /** Leaving the app on purpose: don't greet the player with an app open ad when they come back. */
    private fun leave(intent: Intent, fallback: Intent? = null) {
        ads.appOpen.skipNextForeground()
        try { startActivity(intent) } catch (e: ActivityNotFoundException) {
            fallback?.let { try { startActivity(it) } catch (_: ActivityNotFoundException) {} }
        }
    }

    override fun openStoreReview() {
        leave(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }

    override fun shareApp() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Let's wash some cars in Sudzy! https://play.google.com/store/apps/details?id=$packageName")
        }
        leave(Intent.createChooser(send, "Share Sudzy!"))
    }

    override fun contactSupport() {
        leave(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppLinks.SUPPORT_EMAIL}")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Sudzy! support")
        })
    }

    override fun openPrivacyPolicy() { leave(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PRIVACY_URL))) }

    override fun exitApp() = finish()
}

/** Replace these with your own before publishing. */
object AppLinks {
    const val SUPPORT_EMAIL = "support@example.com"
    const val PRIVACY_URL = "https://example.com/sudzy/privacy"
}
