package com.trippin.app.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.Observer
import com.trippin.app.TrippinApplication

class AndroidAutoConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? TrippinApplication ?: return
        val pendingResult = goAsync()
        val connection = CarConnection(context)

        val current = connection.type.value
        if (current != null) {
            app.carConnectionMonitor.onBroadcastConnectionState(current)
            pendingResult.finish()
            return
        }

        connection.type.observeForever(object : Observer<Int> {
            override fun onChanged(value: Int) {
                connection.type.removeObserver(this)
                app.carConnectionMonitor.onBroadcastConnectionState(value)
                pendingResult.finish()
            }
        })
    }
}
