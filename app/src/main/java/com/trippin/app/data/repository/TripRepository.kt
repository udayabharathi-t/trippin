package com.trippin.app.data.repository

import com.trippin.app.data.dao.CarDao
import com.trippin.app.data.dao.RefuelDao
import com.trippin.app.data.dao.TripDao
import com.trippin.app.data.dao.TripSampleDao
import com.trippin.app.data.model.Car
import com.trippin.app.data.model.Trip
import com.trippin.app.data.model.TripSample
import com.trippin.app.tracking.TripLiveStats
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.max

class TripRepository(
    private val tripDao: TripDao,
    private val sampleDao: TripSampleDao,
    private val refuelDao: RefuelDao,
    private val carDao: CarDao
) {
    fun observeAll(): Flow<List<Trip>> = tripDao.observeAll()

    fun observeByCar(carId: String): Flow<List<Trip>> = tripDao.observeByCar(carId)

    fun observeById(id: String): Flow<Trip?> = tripDao.observeById(id)

    fun observeActiveTrip(): Flow<Trip?> = tripDao.observeActiveTrip()

    suspend fun getActiveTripForCar(carId: String): Trip? = tripDao.getActiveTripForCar(carId)

    suspend fun startTrip(carId: String, autoStarted: Boolean = true): Trip {
        getActiveTripForCar(carId)?.let { return it }

        val trip = Trip(
            id = UUID.randomUUID().toString(),
            carId = carId,
            startTime = System.currentTimeMillis(),
            isActive = true,
            autoStarted = autoStarted
        )
        tripDao.insert(trip)
        return trip
    }

    suspend fun endTrip(tripId: String, endOdometerKm: Float?, endFuelPercent: Float?) {
        val trip = tripDao.getById(tripId) ?: return
        val samples = sampleDao.getForTrip(tripId)
        val endTime = System.currentTimeMillis()
        val durationHours = (endTime - trip.startTime).coerceAtLeast(1L) / 3_600_000f

        val maxSpeed = samples.mapNotNull { it.speedKmh }.maxOrNull() ?: trip.maxSpeedKmh
        val distance = computeDistanceKm(trip, samples, endOdometerKm)
        val avgSpeed = if (durationHours > 0) distance / durationHours else 0f

        val lastLocation = samples.lastOrNull { it.latitude != null }
        val fuelCost = estimateFuelCost(trip.carId, trip.startFuelPercent, endFuelPercent)

        tripDao.update(
            trip.copy(
                endTime = endTime,
                endOdometerKm = endOdometerKm ?: trip.endOdometerKm,
                endFuelPercent = endFuelPercent ?: trip.endFuelPercent,
                maxSpeedKmh = max(trip.maxSpeedKmh, maxSpeed),
                averageSpeedKmh = avgSpeed,
                distanceKm = distance,
                endLatitude = lastLocation?.latitude ?: trip.endLatitude,
                endLongitude = lastLocation?.longitude ?: trip.endLongitude,
                estimatedFuelCostInr = fuelCost?.first,
                fuelPricePerLitreInr = fuelCost?.second,
                isActive = false
            )
        )
    }

    suspend fun getActiveTrip(): Trip? = tripDao.getActiveTrip()

    suspend fun refreshActiveTripMetrics(
        tripId: String,
        currentFuelPercent: Float?,
        currentOdometerKm: Float?
    ): Trip? {
        val trip = tripDao.getById(tripId) ?: return null
        val samples = sampleDao.getForTrip(tripId)
        val endFuel = currentFuelPercent ?: samples.lastOrNull()?.fuelPercent
        val endOdo = currentOdometerKm ?: samples.lastOrNull()?.odometerKm

        val distance = computeDistanceKm(trip, samples, endOdo)
        val elapsedHours = (System.currentTimeMillis() - trip.startTime) / 3_600_000f
        val avgSpeed = if (elapsedHours > 0f) distance / elapsedHours else trip.averageSpeedKmh
        val maxSpeed = max(
            trip.maxSpeedKmh,
            samples.mapNotNull { it.speedKmh }.maxOrNull() ?: 0f
        )
        val fuelCost = estimateFuelCost(trip.carId, trip.startFuelPercent, endFuel)
        val lastLocation = samples.lastOrNull { it.latitude != null }

        val updated = trip.copy(
            endFuelPercent = endFuel,
            endOdometerKm = endOdo,
            distanceKm = distance,
            averageSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            estimatedFuelCostInr = fuelCost?.first,
            fuelPricePerLitreInr = fuelCost?.second,
            endLatitude = lastLocation?.latitude ?: trip.endLatitude,
            endLongitude = lastLocation?.longitude ?: trip.endLongitude
        )
        tripDao.update(updated)
        return updated
    }

    suspend fun buildLiveStats(trip: Trip, currentFuelPercent: Float? = null): TripLiveStats {
        val car = carDao.getById(trip.carId)
        val fuelPercent = currentFuelPercent ?: trip.endFuelPercent
        return TripLiveStats(
            tripId = trip.id,
            estimatedFuelCostInr = trip.estimatedFuelCostInr,
            fuelPricePerLitreInr = trip.fuelPricePerLitreInr,
            maxSpeedKmh = trip.maxSpeedKmh,
            averageSpeedKmh = trip.averageSpeedKmh,
            fuelEconomyKmPerLitre = computeFuelEconomyKmPerLitre(trip, car, fuelPercent),
            distanceKm = trip.distanceKm,
            currentFuelPercent = fuelPercent,
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    fun computeFuelEconomyKmPerLitre(trip: Trip, car: Car?, fuelPercent: Float?): Float? {
        val start = trip.startFuelPercent ?: return null
        val end = fuelPercent ?: trip.endFuelPercent ?: return null
        val capacity = car?.maxFuelCapacityLitres ?: return null
        if (start <= end || trip.distanceKm <= 0f) return null

        val litresUsed = ((start - end) / 100f) * capacity
        if (litresUsed <= 0f) return null
        return trip.distanceKm / litresUsed
    }

    suspend fun updateTrip(trip: Trip) = tripDao.update(trip)

    suspend fun addSample(sample: TripSample) = sampleDao.insert(sample)

    suspend fun updateTripFromSample(tripId: String, sample: TripSample) {
        val trip = tripDao.getById(tripId) ?: return
        sampleDao.insert(sample)

        val updated = trip.copy(
            startOdometerKm = trip.startOdometerKm ?: sample.odometerKm,
            startFuelPercent = trip.startFuelPercent ?: sample.fuelPercent,
            maxSpeedKmh = max(trip.maxSpeedKmh, sample.speedKmh ?: 0f),
            startLatitude = trip.startLatitude ?: sample.latitude,
            startLongitude = trip.startLongitude ?: sample.longitude,
            endLatitude = sample.latitude ?: trip.endLatitude,
            endLongitude = sample.longitude ?: trip.endLongitude
        )
        tripDao.update(updated)
    }

    private suspend fun estimateFuelCost(
        carId: String,
        startFuel: Float?,
        endFuel: Float?
    ): Pair<Float, Float>? {
        val car = carDao.getById(carId) ?: return null
        val lastRefuel = refuelDao.getLatestForCar(carId) ?: return null
        if (startFuel == null || endFuel == null) return null

        val fuelUsedPercent = (startFuel - endFuel).coerceAtLeast(0f)
        val litresUsed = (fuelUsedPercent / 100f) * car.maxFuelCapacityLitres
        val cost = litresUsed * lastRefuel.fuelPricePerLitreInr
        return cost to lastRefuel.fuelPricePerLitreInr
    }

    private fun computeDistanceKm(
        trip: Trip,
        samples: List<TripSample>,
        endOdometer: Float?
    ): Float {
        val startOdo = trip.startOdometerKm
        val endOdo = endOdometer ?: samples.mapNotNull { it.odometerKm }.lastOrNull()
        if (startOdo != null && endOdo != null && endOdo >= startOdo) {
            return endOdo - startOdo
        }
        return trip.distanceKm
    }
}
