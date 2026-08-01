package com.trippin.app.data.repository

import com.trippin.app.data.dao.CarDao
import com.trippin.app.data.model.Car
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CarRepository(private val carDao: CarDao) {
    fun observeAll(): Flow<List<Car>> = carDao.observeAll()

    suspend fun getById(id: String): Car? = carDao.getById(id)

    suspend fun getOrCreateForHardware(
        hardwareId: String,
        vin: String? = null,
        defaultName: String = "My Car"
    ): Car {
        val existing = carDao.getByHardwareId(hardwareId)
        if (existing != null) return existing

        val car = Car(
            id = UUID.randomUUID().toString(),
            name = defaultName,
            hardwareId = hardwareId,
            vin = vin
        )
        carDao.insert(car)
        return car
    }

    suspend fun update(car: Car) = carDao.update(car)

    suspend fun delete(id: String) = carDao.delete(id)
}
