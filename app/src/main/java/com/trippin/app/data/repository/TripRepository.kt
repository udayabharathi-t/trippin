package com.trippin.app.data.repository

import com.trippin.app.data.dao.CarDao
import com.trippin.app.data.dao.RefuelDao
import com.trippin.app.data.dao.TripDao
import com.trippin.app.data.dao.TripSampleDao
import com.trippin.app.data.dao.TripStopDao
import com.trippin.app.data.model.Car
import com.trippin.app.data.model.Trip
import com.trippin.app.data.model.TripSample
import com.trippin.app.data.model.TripStop
import com.trippin.app.tracking.FuelAllocationCalculator
import com.trippin.app.tracking.TripLiveStats
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import com.trippin.app.util.GeoUtils
import kotlin.math.max

class TripRepository(
    private val tripDao: TripDao,
    private val sampleDao: TripSampleDao,
    private val stopDao: TripStopDao,
    private val refuelDao: RefuelDao,
    private val carDao: CarDao
) {
    fun observeAll(): Flow<List<Trip>> = tripDao.observeAll()

    fun observeByCar(carId: String): Flow<List<Trip>> = tripDao.observeByCar(carId)

    fun observeById(id: String): Flow<Trip?> = tripDao.observeById(id)

    fun observeStopsForTrip(tripId: String): Flow<List<TripStop>> = stopDao.observeForTrip(tripId)

    suspend fun getStopsForTrip(tripId: String): List<TripStop> = stopDao.getForTrip(tripId)

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
        val lastLocation = samples.lastOrNull { it.latitude != null }

        val updated = trip.copy(
            endFuelPercent = endFuel,
            endOdometerKm = endOdo,
            distanceKm = distance,
            averageSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
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
        FuelAllocationCalculator.fuelEconomyKmPerLitre(trip)?.let { return it }

        val start = trip.startFuelPercent ?: return null
        val end = fuelPercent ?: trip.endFuelPercent ?: return null
        val capacity = car?.maxFuelCapacityLitres ?: return null
        if (start <= end || trip.distanceKm <= 0f) return null

        val litresUsed = ((start - end) / 100f) * capacity
        if (litresUsed <= 0f) return null
        return trip.distanceKm / litresUsed
    }

    suspend fun updateTrip(trip: Trip) = tripDao.update(trip)

    sealed class MergeResult {
        data class Success(val trip: Trip) : MergeResult()
        data class Error(val message: String) : MergeResult()
    }

    suspend fun mergeTrips(tripIds: List<String>): MergeResult {
        if (tripIds.size < 2) {
            return MergeResult.Error("Select at least 2 trips to merge")
        }

        val trips = tripDao.getByIds(tripIds)
        if (trips.size < 2) {
            return MergeResult.Error("Could not find selected trips")
        }
        if (trips.any { it.isActive }) {
            return MergeResult.Error("Cannot merge an active trip")
        }
        if (trips.map { it.carId }.distinct().size > 1) {
            return MergeResult.Error("Selected trips must belong to the same car")
        }
        if (trips.any { it.endTime == null }) {
            return MergeResult.Error("All selected trips must be completed")
        }

        val sorted = trips.sortedBy { it.startTime }
        val first = sorted.first()
        val last = sorted.last()
        val newId = UUID.randomUUID().toString()

        val totalDistance = computeMergedDistance(sorted)
        val startTime = first.startTime
        val endTime = last.endTime!!
        val durationHours = (endTime - startTime).coerceAtLeast(1L) / 3_600_000f
        val avgSpeed = if (durationHours > 0f) totalDistance / durationHours else 0f
        val maxSpeed = sorted.maxOf { it.maxSpeedKmh }

        val mergedName = first.name?.let { name ->
            if (sorted.size > 1) "$name (+${sorted.size - 1} segments)" else name
        }

        val merged = Trip(
            id = newId,
            carId = first.carId,
            name = mergedName,
            startTime = startTime,
            endTime = endTime,
            startOdometerKm = first.startOdometerKm,
            endOdometerKm = last.endOdometerKm,
            startFuelPercent = first.startFuelPercent,
            endFuelPercent = last.endFuelPercent,
            maxSpeedKmh = maxSpeed,
            averageSpeedKmh = avgSpeed,
            distanceKm = totalDistance,
            startLatitude = first.startLatitude,
            startLongitude = first.startLongitude,
            startLocationName = first.startLocationName,
            endLatitude = last.endLatitude,
            endLongitude = last.endLongitude,
            endLocationName = last.endLocationName,
            isActive = false,
            autoStarted = false,
            isMerged = true
        )

        val stops = sorted.dropLast(1).mapIndexed { index, trip ->
            TripStop(
                id = UUID.randomUUID().toString(),
                tripId = newId,
                orderIndex = index,
                timestamp = trip.endTime ?: trip.startTime,
                latitude = trip.endLatitude,
                longitude = trip.endLongitude,
                locationName = trip.endLocationName,
                label = trip.name ?: "Stop ${index + 1}"
            )
        }

        val oldIds = sorted.map { it.id }
        sampleDao.reassignTrips(oldIds, newId)
        oldIds.forEach { tripDao.delete(it) }

        tripDao.insert(merged)
        if (stops.isNotEmpty()) {
            stopDao.insertAll(stops)
        }

        recalculateFuelAllocationsForCar(first.carId)

        return MergeResult.Success(merged)
    }

    private fun computeMergedDistance(trips: List<Trip>): Float {
        val first = trips.first()
        val last = trips.last()
        val startOdo = first.startOdometerKm
        val endOdo = last.endOdometerKm
        if (startOdo != null && endOdo != null && endOdo >= startOdo) {
            return endOdo - startOdo
        }
        return trips.sumOf { it.distanceKm.toDouble() }.toFloat()
    }

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

    suspend fun recomputeTripDistancesFromGpsForCar(carId: String) {
        val trips = tripDao.getByCarAscending(carId)
        trips.forEach { trip ->
            if (trip.isActive) return@forEach
            val samples = sampleDao.getForTrip(trip.id)
            val distance = computeDistanceKm(trip, samples, trip.endOdometerKm)
            if (distance != trip.distanceKm) {
                tripDao.update(trip.copy(distanceKm = distance))
            }
        }
    }

    suspend fun recalculateFuelAllocationsForCar(carId: String): List<FuelAllocationCalculator.PeriodSummary> {
        recomputeTripDistancesFromGpsForCar(carId)

        val refuels = refuelDao.getByCarAscending(carId)
        val trips = tripDao.getByCarAscending(carId)

        tripDao.clearFuelCostsForCar(carId)

        val (allocations, summaries) = FuelAllocationCalculator.allocateForCar(refuels, trips)
        allocations.forEach { allocation ->
            val trip = tripDao.getById(allocation.tripId) ?: return@forEach
            tripDao.update(
                trip.copy(
                    estimatedFuelCostInr = allocation.estimatedFuelCostInr,
                    fuelPricePerLitreInr = allocation.fuelPricePerLitreInr
                )
            )
        }
        return summaries
    }

    suspend fun backfillMetricsForAllCars() {
        carDao.getAll().forEach { car ->
            recalculateFuelAllocationsForCar(car.id)
        }
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

        val gpsPoints = samples.mapNotNull { sample ->
            if (sample.latitude != null && sample.longitude != null) {
                sample.latitude to sample.longitude
            } else {
                null
            }
        }
        val gpsDistance = GeoUtils.pathDistanceKm(gpsPoints)
        if (gpsDistance > 0f) return gpsDistance

        return trip.distanceKm
    }
}
