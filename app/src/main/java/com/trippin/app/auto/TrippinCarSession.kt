package com.trippin.app.auto

import android.content.Intent
import android.os.Build
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import com.trippin.app.service.TripTrackingService

class TrippinCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val hardwareId = "aa_${Build.MODEL}_${Build.DEVICE}"
        TripTrackingService.start(carContext, hardwareId)
        return TrippinCarScreen(carContext)
    }
}

class TrippinCarScreen(carContext: CarContext) : Screen(carContext) {
    init {
        invalidate()
    }

    override fun onGetTemplate(): androidx.car.app.model.Template {
        return androidx.car.app.model.MessageTemplate.Builder("Trippin is recording your trip")
            .setTitle("Trippin")
            .build()
    }
}
