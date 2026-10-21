package com.devatjunaid.dripdropdraw

import android.app.Application
import com.devatjunaid.dripdropdraw.ads.AdsManager
import com.devatjunaid.dripdropdraw.audio.Sfx
import com.devatjunaid.dripdropdraw.data.Progress

class DripApplication : Application() {

    val progress by lazy { Progress(this) }
    val sfx by lazy { Sfx(this) }

    override fun onCreate() {
        super.onCreate()
        AdsManager.init(this)
    }
}
