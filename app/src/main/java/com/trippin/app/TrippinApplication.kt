package com.trippin.app

import android.app.Application
import com.trippin.app.di.AppContainer

class TrippinApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
