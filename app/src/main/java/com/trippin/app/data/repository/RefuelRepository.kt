package com.trippin.app.data.repository

import com.trippin.app.data.dao.CarDao
import com.trippin.app.data.dao.RefuelDao
import com.trippin.app.data.dao.TripDao
import com.trippin.app.data.dao.TripSampleDao
import com.trippin.app.data.model.RefuelEvent
import com.trippin.app.tracking.VehicleDataProvider
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RefuelRepository(
    private val refuelDao: RefuelDao,
    private val carDao: CarDao,
    private val tripDao: TripDao,
    private val tripSampleDao: TripSampleDao,
    private val vehicleDataProvider: VehicleDataProvider
) {
    fun observeByCar(carId: String): Flow<List<RefuelEvent>> = refuelDao.observeByCar(carId)

    suspend fun getLatestForCar(carId: String): RefuelEvent? = refuelDao.getLatestForCar(carId)

    suspend fun recordRefuel(
        carId: String,
        fuelPercentBefore: Float?,
        fuelPercentAfter: Float?,
        fuelPricePerLitreInr: Float,
        totalCostInr: Float,
        tag: String? = null,
        odometerKm: Float? = null,
        isAutoDetected: Boolean = false
    ): RefuelEvent? {
        val car = carDao.getById(carId) ?: return null

        val litresFromFuel = if (fuelPercentBefore != null && fuelPercentAfter != null &&
            fuelPercentAfter > fuelPercentBefore
        ) {
            ((fuelPercentAfter - fuelPercentBefore) / 100f) * car.maxFuelCapacityLitres
        } else {
            null
        }

        val litresFromCost = if (fuelPricePerLitreInr > 0f && totalCostInr > 0f) {
            totalCostInr / fuelPricePerLitreInr
        } else {
            null
        }

        val litresFilled = litresFromFuel ?: litresFromCost ?: return null

        val event = RefuelEvent(
            id = UUID.randomUUID().toString(),
            carId = carId,
            timestamp = System.currentTimeMillis(),
            fuelPercentBefore = fuelPercentBefore,
            fuelPercentAfter = fuelPercentAfter,
            litresFilled = litresFilled,
            fuelPricePerLitreInr = fuelPricePerLitreInr,
            totalCostInr = totalCostInr,
            tag = tag,
            odometerKm = odometerKm,
            isAutoDetected = isAutoDetected
        )
        refuelDao.insert(event)
        return event
    }

    suspend fun recordManualRefuel(
        carId: String,
        fuelPricePerLitreInr: Float,
        totalCostInr: Float,
        tag: String? = null
    ): RefuelEvent? {
        if (fuelPricePerLitreInr <= 0f || totalCostInr <= 0f) return null

        val snapshot = vehicleDataProvider.captureSnapshot()
        val fuelAfter = snapshot.fuelPercent
        val fuelBefore = resolveLastFuelPercent(carId)

        return recordRefuel(
            carId = carId,
            fuelPercentBefore = fuelBefore,
            fuelPercentAfter = fuelAfter,
            fuelPricePerLitreInr = fuelPricePerLitreInr,
            totalCostInr = totalCostInr,
            tag = tag,
            odometerKm = snapshot.odometerKm,
            isAutoDetected = false
        )
    }

    private suspend fun resolveLastFuelPercent(carId: String): Float? {
        tripSampleDao.getLatestFuelSampleForCar(carId)?.fuelPercent?.let { return it }

        val latestTrip = tripDao.getLatestForCar(carId)
        latestTrip?.endFuelPercent?.let { return it }
        latestTrip?.startFuelPercent?.let { return it }

        refuelDao.getLatestForCar(carId)?.fuelPercentAfter?.let { return it }

        return null
    }

    suspend fun update(event: RefuelEvent) = refuelDao.update(event)

    suspend fun delete(id: String) = refuelDao.delete(id)
}
