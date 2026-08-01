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

    companion object {
        private const val SAMPLE_INTERVAL_MS = 30_000L
        private const val REFUEL_THRESHOLD_PERCENT = 8f
        private const val MIN_LITRES_FROM_AUTO = 0.5f
    }

    fun startForHardware(hardwareId: String, vin: String? = null) {
        scope.launch {
            val car = carRepository.getOrCreateForHardware(hardwareId, vin)
            activeCarId = car.id
            lastHardwareId = hardwareId

            val trip = tripRepository.startTrip(car.id, autoStarted = true)
            activeTripId = trip.id

            val snapshot = vehicleDataProvider.captureSnapshot()
            lastFuelPercent = snapshot.fuelPercent

            tripRepository.updateTripFromSample(
                trip.id,
                TripSample(
                    tripId = trip.id,
                    timestamp = System.currentTimeMillis(),
                    odometerKm = snapshot.odometerKm,
                    fuelPercent = snapshot.fuelPercent,
                    speedKmh = snapshot.speedKmh,
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude
                )
            )

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
        }
    }

    fun isTracking(): Boolean = activeTripId != null

    private fun startSampling() {
        samplingJob?.cancel()
        samplingJob = scope.launch {
            while (isActive && activeTripId != null) {
                delay(SAMPLE_INTERVAL_MS)
                captureSample()
            }
        }
    }

    private suspend fun captureSample() {
        val tripId = activeTripId ?: return
        val carId = activeCarId ?: return
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
    }

    private suspend fun detectRefuel(carId: String, currentFuel: Float?, odometerKm: Float?) {
        val previous = lastFuelPercent ?: return
        val current = currentFuel ?: return

        if (current - previous >= REFUEL_THRESHOLD_PERCENT) {
            val latestRefuel = refuelRepository.getLatestForCar(carId)
            val price = latestRefuel?.fuelPricePerLitreInr ?: 0f
            val car = carRepository.getById(carId) ?: return
            val litres = ((current - previous) / 100f) * car.maxFuelCapacityLitres
            if (litres < MIN_LITRES_FROM_AUTO) return

            val totalCost = if (price > 0f) litres * price else 0f

            refuelRepository.recordRefuel(
                carId = carId,
                fuelPercentBefore = previous,
                fuelPercentAfter = current,
                fuelPricePerLitreInr = price,
                totalCostInr = totalCost,
                tag = "Auto-detected",
                odometerKm = odometerKm,
                isAutoDetected = true
            )
        }
    }
}
