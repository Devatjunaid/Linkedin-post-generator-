package com.sunarc.inverterlogger

import android.content.Context
import com.sunarc.inverterlogger.ads.AdsManager
import com.sunarc.inverterlogger.data.ExportManager
import com.sunarc.inverterlogger.data.LogRepository
import com.sunarc.inverterlogger.data.SettingsRepository
import com.sunarc.inverterlogger.data.db.AppDatabase
import com.sunarc.inverterlogger.ocr.OcrEngine

/** Hand-wired dependencies shared by the whole app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val settings = SettingsRepository(appContext)
    val log: LogRepository by lazy { LogRepository(database) }
    val exports: ExportManager by lazy { ExportManager(appContext, log, settings) }
    val ocr: OcrEngine by lazy { OcrEngine() }
    val ads = AdsManager(appContext)
}
