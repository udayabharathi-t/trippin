package com.trippin.app.tracking

import android.content.Context
import com.trippin.app.data.model.TripSample
import com.trippin.app.data.repository.CarRepository
import com.trippin.app.data.repository.RefuelRepository
import com.trippin.app.data.repository.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TripTracker(
    private val context: Context,
    private val carRepository: CarRepository,
    private val tripRepository: TripRepository,
    private val refuelRepository: RefuelRepository,
    private val vehicleDataProvider: VehicleDataProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var samplingJob: Job? = null
    private var activeCarId: String? = null
    private var activeTripId: String? = null
    private var lastFuelPercent: Float? = null
    private var lastHardwareId: String? = null

    private val _liveStats = MutableStateFlow<TripLiveStats?>(null)
    val liveStats: StateFlow<TripLiveStats?> = _liveStats.asStateFlow()

    companion object {
        private const val SAMPLE_INTERVAL_MS = 30_000L
        private const val REFUEL_THRESHOLD_PERCENT = 8f
    }

    fun startForHardware(hardwareId: String, vin: String? = null) {
        scope.launch {
            val car = carRepository.getOrCreateForHardware(hardwareId, vin)
            activeCarId = car.id
            lastHardwareId = hardwareId

            val trip = tripRepository.startTrip(car.id, autoStarted = true)
            activeTripId = trip.id

            syncNow()
            startSampling()
        }
    }

    fun stopTracking() {
        scope.launch {
            samplingJob?.cancel()
            samplingJob = null

            val tripId = activeTripId ?: return@launch
            val snapshot = vehicleDataProvider.captureSnapshot()
            tripRepository.endTrip(tripId, snapshot.odometerKm, snapshot.fuelPercent)

            activeTripId = null
            activeCarId = null
            lastFuelPercent = null
            _liveStats.value = null
        }
    }

    fun isTracking(): Boolean = activeTripId != null

    /**
     * Manually sync with the car — captures a fresh sensor/GPS reading and
     * recomputes live trip metrics (cost, speeds, fuel economy).
     */
    suspend fun syncNow(): TripLiveStats? {
        val tripId = activeTripId
        val carId = activeCarId

        if (tripId != null && carId != null) {
            return captureAndRefresh(tripId, carId)
        }

        val activeTrip = tripRepository.getActiveTrip() ?: return null
        activeTripId = activeTrip.id
        activeCarId = activeTrip.carId
        return captureAndRefresh(activeTrip.id, activeTrip.carId)
    }

    private suspend fun captureAndRefresh(tripId: String, carId: String): TripLiveStats? {
        val snapshot = vehicleDataProvider.captureSnapshot()

        tripRepository.updateTripFromSample(
            tripId,
            TripSample(
                tripId = tripId,
                timestamp = System.currentTimeMillis(),
                odometerKm = snapshot.odometerKm,
                fuelPercent = snapshot.fuelPercent,
                speedKmh = snapshot.speedKmh,
                latitude = snapshot.latitude,
                longitude = snapshot.longitude
            )
        )

        detectRefuel(carId, snapshot.fuelPercent, snapshot.odometerKm)
        lastFuelPercent = snapshot.fuelPercent ?: lastFuelPercent

        val updated = tripRepository.refreshActiveTripMetrics(
            tripId = tripId,
            currentFuelPercent = snapshot.fuelPercent,
            currentOdometerKm = snapshot.odometerKm
        ) ?: return null

        val stats = tripRepository.buildLiveStats(updated, snapshot.fuelPercent)
        _liveStats.value = stats
        return stats
    }

    private fun startSampling() {
        samplingJob?.cancel()
        samplingJob = scope.launch {
            while (isActive && activeTripId != null) {
                delay(SAMPLE_INTERVAL_MS)
                val tripId = activeTripId ?: continue
                val carId = activeCarId ?: continue
                captureAndRefresh(tripId, carId)
            }
        }
    }

    private suspend fun detectRefuel(carId: String, currentFuel: Float?, odometerKm: Float?) {
        val previous = lastFuelPercent ?: return
        val current = currentFuel ?: return

        if (current - previous >= REFUEL_THRESHOLD_PERCENT) {
            val latestRefuel = refuelRepository.getLatestForCar(carId)
            val price = latestRefuel?.fuelPricePerLitreInr ?: 0f

            refuelRepository.recordRefuel(
                carId = carId,
                fuelPercentBefore = previous,
                fuelPercentAfter = current,
                fuelPricePerLitreInr = price,
                totalCostInr = 0f,
                tag = "Auto-detected",
                odometerKm = odometerKm,
                isAutoDetected = true
            )
        }
    }
}
