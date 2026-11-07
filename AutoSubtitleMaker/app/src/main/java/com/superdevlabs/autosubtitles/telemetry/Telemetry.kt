package com.superdevlabs.autosubtitles.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper over Firebase Analytics + Crashlytics.
 *
 * Firebase is only active when the project was built with a google-services.json; otherwise
 * every call is a no-op, so the app never crashes because Firebase isn't configured.
 * No video, audio or caption text is ever sent — only event names and small counters.
 */
class Telemetry(context: Context) {

    private val appContext = context.applicationContext
    private val enabled: Boolean = FirebaseApp.getApps(appContext).isNotEmpty()
    private val analytics: FirebaseAnalytics? = if (enabled) FirebaseAnalytics.getInstance(appContext) else null
    private val crashlytics: FirebaseCrashlytics? = if (enabled) FirebaseCrashlytics.getInstance() else null

    /** The user's choice in Settings ("Share usage & crash reports"). */
    fun setCollectionEnabled(allowed: Boolean) {
        analytics?.setAnalyticsCollectionEnabled(allowed)
        crashlytics?.isCrashlyticsCollectionEnabled = allowed
    }

    fun event(name: String, vararg params: Pair<String, Any?>) {
        val a = analytics ?: return
        val bundle = Bundle()
        for ((key, value) in params) {
            when (value) {
                null -> Unit
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString().take(100))
            }
        }
        a.logEvent(name, bundle)
    }

    /** Non-fatal error: shows up in Crashlytics without crashing the app. */
    fun recordError(error: Throwable, where: String) {
        val c = crashlytics ?: return
        c.log(where)
        c.recordException(error)
    }

    /** Small breadcrumb that is attached to the next crash report. */
    fun breadcrumb(message: String) {
        crashlytics?.log(message)
    }

    companion object {
        /** Groups video length into buckets so analytics never stores exact values. */
        fun durationBucket(ms: Long): String = when {
            ms < 30_000 -> "<30s"
            ms < 60_000 -> "30-60s"
            ms < 180_000 -> "1-3m"
            ms < 600_000 -> "3-10m"
            else -> "10m+"
        }
    }
}
