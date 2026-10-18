package com.inkformlabs.pdfsigner

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class InkformApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // PdfBox-Android reads its fonts and CMaps from the APK assets.
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
    }
}
