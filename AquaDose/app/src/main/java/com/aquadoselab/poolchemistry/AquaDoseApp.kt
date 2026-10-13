package com.aquadoselab.poolchemistry

import android.app.Application
import com.aquadoselab.poolchemistry.data.Repository

class AquaDoseApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
    }
}
