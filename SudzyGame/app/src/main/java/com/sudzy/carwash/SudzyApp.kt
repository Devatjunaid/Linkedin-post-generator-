package com.sudzy.carwash

import android.app.Application
import com.sudzy.carwash.ads.AdsManager

class SudzyApp : Application() {
    lateinit var ads: AdsManager
        private set

    override fun onCreate() {
        super.onCreate()
        ads = AdsManager(this)
    }
}
