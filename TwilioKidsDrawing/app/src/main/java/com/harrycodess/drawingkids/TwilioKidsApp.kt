package com.harrycodess.drawingkids

import android.app.Application
import com.harrycodess.drawingkids.ads.Ads
import com.harrycodess.drawingkids.data.Gallery
import com.harrycodess.drawingkids.data.Prefs

class TwilioKidsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Gallery.init(this)
        Ads.start(this)
    }
}
