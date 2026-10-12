package app.gigometer.mileage

import android.app.Application

class GigometerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifications.createChannels(this)
    }
}
