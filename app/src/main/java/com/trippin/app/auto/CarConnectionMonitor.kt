package com.trippin.app.auto

import androidx.car.app.connection.CarConnection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.trippin.app.TrippinApplication
import com.trippin.app.service.TripTrackingService
import com.trippin.app.util.PermissionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CarConnectionMonitor(
    private val application: TrippinApplication
) {
    private val carConnection = CarConnection(application)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitoring = false
    private var lastState: Int = CarConnection.CONNECTION_TYPE_NOT_CONNECTED

    private val _isAndroidAutoConnected = MutableStateFlow(false)
    val isAndroidAutoConnected: StateFlow<Boolean> = _isAndroidAutoConnected.asStateFlow()

    fun start() {
        if (monitoring) return
        monitoring = true
        carConnection.type.observeForever { state ->
            onConnectionStateChanged(state)
        }
        carConnection.type.value?.let { onConnectionStateChanged(it) }
    }

    fun onPermissionsGranted() {
        checkAndStartIfConnected(fromUserAction = true)
    }

    fun checkAndStartIfConnected(fromUserAction: Boolean = false) {
        val state = carConnection.type.value ?: CarConnection.CONNECTION_TYPE_NOT_CONNECTED
        if (state == CarConnection.CONNECTION_TYPE_PROJECTION) {
            tryStartTracking(fromUserAction = fromUserAction)
        }
    }

    fun isAndroidAutoConnected(): Boolean = _isAndroidAutoConnected.value

    fun onBroadcastConnectionState(state: Int) {
        onConnectionStateChanged(state)
    }

    private fun onConnectionStateChanged(state: Int) {
        if (state == lastState) return
        lastState = state
        _isAndroidAutoConnected.value = state == CarConnection.CONNECTION_TYPE_PROJECTION

        when (state) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> tryStartTracking(fromUserAction = false)
            CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> tryStopTracking()
        }
    }

    private fun tryStartTracking(fromUserAction: Boolean) {
        if (!PermissionsHelper.hasLocationPermission(application)) {
            TripStartNotifier.showPermissionRequired(application)
            return
        }

        if (application.container.tripTracker.isTracking()) {
            TripStartNotifier.cancelStartPrompt(application)
            return
        }

        val hardwareId = CarHardwareIds.resolve()
        val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)

        if (fromUserAction || inForeground) {
            TripTrackingService.start(application, hardwareId)
            TripStartNotifier.cancelStartPrompt(application)
        } else {
            TripTrackingService.startSafely(application, hardwareId)
        }
    }

    private fun tryStopTracking() {
        scope.launch {
            application.container.tripTracker.stopIfActive()
            TripTrackingService.stop(application)
            TripStartNotifier.cancelStartPrompt(application)
        }
    }
}
