package com.superdevlabs.coldplunge

import android.content.Context
import com.superdevlabs.coldplunge.ads.AdsConsentManager
import com.superdevlabs.coldplunge.ads.InterstitialAdManager
import com.superdevlabs.coldplunge.data.ProtocolRepository
import com.superdevlabs.coldplunge.data.SessionRepository
import com.superdevlabs.coldplunge.data.SettingsRepository
import com.superdevlabs.coldplunge.data.db.PlungeDatabase

/** Tiny manual dependency container; one instance per process, owned by [PlungeApp]. */
class AppGraph(context: Context) {
    private val app = context.applicationContext
    private val database = PlungeDatabase.build(app)
    val sessions = SessionRepository(database.sessions())
    val protocols = ProtocolRepository(database.protocols())
    val settings = SettingsRepository(app)
    val consent = AdsConsentManager(app)
    val interstitials = InterstitialAdManager(app, consent)
}

val Context.graph: AppGraph get() = (applicationContext as PlungeApp).graph
