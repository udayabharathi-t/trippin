package com.trippin.app

import android.app.Application
import com.trippin.app.auto.CarConnectionMonitor
import com.trippin.app.di.AppContainer

class TrippinApplication : Application() {
    lateinit var container: AppContainer
        private set

    lateinit var carConnectionMonitor: CarConnectionMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        carConnectionMonitor = CarConnectionMonitor(this)
        carConnectionMonitor.start()
    }
}
