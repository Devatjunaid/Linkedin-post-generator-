package com.inkformlabs.pdfsigner

import android.app.Application
import androidx.room.Room
import com.inkformlabs.pdfsigner.ads.AdsManager
import com.inkformlabs.pdfsigner.ads.ConsentManager
import com.inkformlabs.pdfsigner.data.DocumentRepository
import com.inkformlabs.pdfsigner.data.ProfileStore
import com.inkformlabs.pdfsigner.data.SettingsStore
import com.inkformlabs.pdfsigner.data.SignatureRepository
import com.inkformlabs.pdfsigner.data.db.AppDatabase
import com.inkformlabs.pdfsigner.pdf.PdfEngine

/** Simple manual dependency container, created once in [InkformApp]. */
class AppContainer(val app: Application) {

    private val database: AppDatabase =
        Room.databaseBuilder(app, AppDatabase::class.java, "inkform.db").build()

    val pdfEngine = PdfEngine(app)
    val documents = DocumentRepository(app, database.documentDao(), pdfEngine)
    val signatures = SignatureRepository(app, database.signatureDao())
    val settings = SettingsStore(app)
    val profile = ProfileStore(app)
    val consent = ConsentManager(app)
    val ads = AdsManager(app, consent)
}
