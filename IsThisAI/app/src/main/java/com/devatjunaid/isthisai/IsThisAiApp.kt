package com.devatjunaid.isthisai

import android.app.Application
import com.devatjunaid.isthisai.data.ScanDatabase
import com.devatjunaid.isthisai.data.ScanRepository

class IsThisAiApp : Application() {
    val repository: ScanRepository by lazy { ScanRepository(this, ScanDatabase.create(this).scans()) }
}
