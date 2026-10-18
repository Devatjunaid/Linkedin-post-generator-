package com.unpin.photoscrub

import android.app.Application
import com.unpin.photoscrub.ads.AdsManager
import com.unpin.photoscrub.data.Storage

class UnpinApp : Application() {
    lateinit var storage: Storage
        private set
    lateinit var ads: AdsManager
        private set

    override fun onCreate() {
        super.onCreate()
        storage = Storage(this)
        ads = AdsManager(this)
    }
}
