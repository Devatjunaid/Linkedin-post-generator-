package com.devatjunaid.kelvin

import android.content.Context
import com.devatjunaid.kelvin.ads.AdsConsentManager
import com.devatjunaid.kelvin.ads.InterstitialAdManager
import com.devatjunaid.kelvin.data.ProtocolRepository
import com.devatjunaid.kelvin.data.SessionRepository
import com.devatjunaid.kelvin.data.SettingsRepository
import com.devatjunaid.kelvin.data.db.KelvinDatabase

/** Tiny manual dependency container; one instance per process, owned by [KelvinApp]. */
class AppGraph(context: Context) {
    private val app = context.applicationContext
    private val database = KelvinDatabase.build(app)
    val sessions = SessionRepository(database.sessions())
    val protocols = ProtocolRepository(database.protocols())
    val settings = SettingsRepository(app)
    val consent = AdsConsentManager(app)
    val interstitials = InterstitialAdManager(app, consent)
}

val Context.graph: AppGraph get() = (applicationContext as KelvinApp).graph
