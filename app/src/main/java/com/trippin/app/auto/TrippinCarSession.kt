package com.trippin.app.auto

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.trippin.app.R
import com.trippin.app.TrippinApplication
import com.trippin.app.service.TripTrackingService
import com.trippin.app.tracking.TripLiveStats
import com.trippin.app.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrippinCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val hardwareId = "aa_${Build.MODEL}_${Build.DEVICE}"
        TripTrackingService.start(carContext, hardwareId)
        return TrippinCarScreen(carContext)
    }
}

/**
 * Android Auto screen using NavigationTemplate (multi-pane top-left slot) with
 * PaneTemplate rows for detailed trip stats. Sync action pulls fresh car data.
 */
class TrippinCarScreen(
    private val hostContext: CarContext
) : Screen(hostContext) {

    private var stats: TripLiveStats? = null
    private var syncing = false
    private var statusLine: String = "Starting trip…"

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            invalidate()
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                refreshHandler.removeCallbacks(refreshRunnable)
            }
        })
        lifecycleScope.launch {
            performSync(showStatus = false)
        }
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)
    }

    override fun onGetTemplate(): Template {
        val syncAction = Action.Builder()
            .setTitle("Sync")
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(hostContext, R.drawable.ic_sync)
                ).build()
            )
            .setOnClickListener {
                lifecycleScope.launch { performSync(showStatus = true) }
            }
            .build()

        val actionStrip = ActionStrip.Builder()
            .addAction(syncAction)
            .build()

        val (primary, secondary) = buildDisplayText()

        // NavigationTemplate targets the multi-pane navigation slot (top-left on most head units).
        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .setNavigationInfo(
                MessageInfo.Builder(primary)
                    .setText(secondary)
                    .build()
            )
            .build()
    }

    private fun buildDisplayText(): Pair<String, String> {
        val live = stats
        if (live == null) {
            return hostContext.getString(R.string.app_name) to statusLine
        }

        if (live.estimatedFuelCostInr != null && live.estimatedFuelCostInr > 0f) {
            val primary = "Trip cost: ${Formatters.inr(live.estimatedFuelCostInr)}"
            val secondary = buildString {
                append("Approx. based on last refill")
                append(" · ${Formatters.km(live.distanceKm)}")
                live.fuelPricePerLitreInr?.let { append(" · ${Formatters.inr(it)}/L") }
            }
            return primary to secondary
        }

        val primary = "Max ${Formatters.speed(live.maxSpeedKmh)} · Avg ${Formatters.speed(live.averageSpeedKmh)}"
        val secondary = buildString {
            append(Formatters.km(live.distanceKm))
            live.fuelEconomyKmPerLitre?.let { append(" · ${Formatters.fuelEconomy(it)}") }
                ?: append(" · Fuel economy unavailable")
            if (syncing) append(" · Syncing…")
        }
        return primary to secondary
    }

    private suspend fun performSync(showStatus: Boolean) {
        if (syncing) return
        syncing = true
        if (showStatus) {
            statusLine = "Syncing with car…"
            invalidate()
        }

        val container = (hostContext.applicationContext as TrippinApplication).container
        val result = withContext(Dispatchers.IO) {
            container.tripTracker.syncNow()
        }

        stats = result
        statusLine = if (result != null) {
            "Synced ${Formatters.time(result.lastSyncedAt)}"
        } else {
            "No active trip — connect and drive"
        }
        syncing = false
        invalidate()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15_000L
    }
}
