package com.trippin.app

import android.app.Application
import com.trippin.app.auto.CarConnectionMonitor
import com.trippin.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrippinApplication : Application() {
    lateinit var container: AppContainer
        private set

    lateinit var carConnectionMonitor: CarConnectionMonitor
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        carConnectionMonitor = CarConnectionMonitor(this)
        carConnectionMonitor.start()

        appScope.launch {
            container.tripRepository.backfillMetricsForAllCars()
        }
    }
}
