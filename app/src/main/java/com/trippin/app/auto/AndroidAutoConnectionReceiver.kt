package com.trippin.app.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.car.app.connection.CarConnection
import com.trippin.app.service.TripTrackingService

class AndroidAutoConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val connection = CarConnection(context)
        val type = connection.type.value ?: CarConnection.CONNECTION_TYPE_NOT_CONNECTED

        val hardwareId = "aa_${Build.MODEL}_${Build.DEVICE}"

        when (type) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> {
                TripTrackingService.start(context, hardwareId)
            }
            CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> {
                if ((context.applicationContext as? com.trippin.app.TrippinApplication)
                        ?.container?.tripTracker?.isTracking() == true
                ) {
                    TripTrackingService.stop(context)
                }
            }
        }
    }
}
