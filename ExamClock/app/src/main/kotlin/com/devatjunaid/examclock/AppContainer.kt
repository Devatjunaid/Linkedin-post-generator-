package com.devatjunaid.examclock

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.devatjunaid.examclock.ads.AdsManager
import com.devatjunaid.examclock.data.db.AppDatabase
import com.devatjunaid.examclock.data.prefs.PreferencesRepository
import com.devatjunaid.examclock.data.repo.StudyRepository

/** Manual dependency container, created once by [ExamClockApp]. */
class AppContainer(app: Application) {
    private val database: AppDatabase by lazy { AppDatabase.build(app) }
    val repository: StudyRepository by lazy { StudyRepository(database) }
    val preferences = PreferencesRepository(app)
    val ads = AdsManager(app)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
