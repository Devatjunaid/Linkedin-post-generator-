package com.dailycash.tracker

import android.app.Application
import com.dailycash.tracker.data.AppDatabase
import com.dailycash.tracker.data.Prefs
import com.dailycash.tracker.data.RatesRepository

class DailyCashApp : Application() {
    val database by lazy { AppDatabase.build(this) }
    val prefs by lazy { Prefs(this) }
    val rates by lazy { RatesRepository(this) }
}
