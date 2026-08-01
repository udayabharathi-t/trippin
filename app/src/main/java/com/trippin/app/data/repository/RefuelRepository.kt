package com.trippin.app.data.repository

import com.trippin.app.data.dao.CarDao
import com.trippin.app.data.dao.RefuelDao
import com.trippin.app.data.model.RefuelEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RefuelRepository(
    private val refuelDao: RefuelDao,
    private val carDao: CarDao
) {
    fun observeByCar(carId: String): Flow<List<RefuelEvent>> = refuelDao.observeByCar(carId)

    suspend fun getLatestForCar(carId: String): RefuelEvent? = refuelDao.getLatestForCar(carId)

    suspend fun recordRefuel(
        carId: String,
        fuelPercentBefore: Float,
        fuelPercentAfter: Float,
        fuelPricePerLitreInr: Float,
        totalCostInr: Float,
        tag: String? = null,
        odometerKm: Float? = null,
        isAutoDetected: Boolean = false
    ): RefuelEvent {
        val car = carDao.getById(carId)
            ?: throw IllegalArgumentException("Car not found")

        val percentIncrease = (fuelPercentAfter - fuelPercentBefore).coerceAtLeast(0f)
        val litresFilled = (percentIncrease / 100f) * car.maxFuelCapacityLitres

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

    suspend fun update(event: RefuelEvent) = refuelDao.update(event)

    suspend fun delete(id: String) = refuelDao.delete(id)
}
