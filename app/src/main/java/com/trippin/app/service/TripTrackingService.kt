package com.trippin.app.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.trippin.app.R
import com.trippin.app.TrippinApplication
import com.trippin.app.auto.TripStartNotifier
import com.trippin.app.ui.MainActivity

class TripTrackingService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val hardwareId = intent.getStringExtra(EXTRA_HARDWARE_ID) ?: DEFAULT_HARDWARE_ID
        val vin = intent.getStringExtra(EXTRA_VIN)
        startForeground(NOTIFICATION_ID, buildNotification("Recording trip…"))
        val tracker = (application as TrippinApplication).container.tripTracker
        tracker.startForHardware(hardwareId, vin)
      }
      ACTION_STOP -> {
        val tracker = (application as TrippinApplication).container.tripTracker
        tracker.stopTracking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    }
    return START_STICKY
  }

  private fun buildNotification(text: String): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.app_name))
      .setContentText(text)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }

  private fun createNotificationChannel() {
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Trip tracking",
      NotificationManager.IMPORTANCE_LOW
    )
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val ACTION_START = "com.trippin.app.action.START_TRACKING"
    const val ACTION_STOP = "com.trippin.app.action.STOP_TRACKING"
    const val EXTRA_HARDWARE_ID = "hardware_id"
    const val EXTRA_VIN = "vin"
    private const val CHANNEL_ID = "trip_tracking"
    private const val NOTIFICATION_ID = 1001
    private const val DEFAULT_HARDWARE_ID = "default_vehicle"

    fun start(context: Context, hardwareId: String, vin: String? = null) {
      val intent = Intent(context, TripTrackingService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_HARDWARE_ID, hardwareId)
        putExtra(EXTRA_VIN, vin)
      }
      context.startForegroundService(intent)
    }

    fun startSafely(context: Context, hardwareId: String, vin: String? = null) {
      try {
        start(context, hardwareId, vin)
      } catch (e: Exception) {
        val blocked = e is ForegroundServiceStartNotAllowedException ||
          (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            e is IllegalStateException &&
            e.message?.contains("ForegroundServiceStartNotAllowed", ignoreCase = true) == true)
        if (blocked) {
          TripStartNotifier.showTapToStart(context)
        } else {
          throw e
        }
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, TripTrackingService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }
  }
}
