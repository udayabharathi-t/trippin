package com.trippin.app.auto

import androidx.car.app.CarContext
import androidx.car.app.OnRequestPermissionsListener
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.trippin.app.tracking.VehicleDataCache

/**
 * Subscribes to odometer, fuel, and speed from the car via Car App Library hardware APIs.
 * Requires an active [CarContext] (car app session while Android Auto is connected).
 */
class CarHardwareCollector(
    private val carContext: CarContext,
    owner: LifecycleOwner
) : DefaultLifecycleObserver {

    private val carInfo = carContext.getCarService(CarHardwareManager::class.java).carInfo

    private val energyListener = OnCarDataAvailableListener<EnergyLevel> { data ->
        val fuel = data.fuelPercent.floatOrNull()
        val speed = VehicleDataCache.get().speedKmh
        val odo = VehicleDataCache.get().odometerKm
        VehicleDataCache.update(
            odometerKm = odo,
            fuelPercent = fuel,
            speedKmh = speed,
            hardwareAvailable = true
        )
    }

    private val mileageListener = OnCarDataAvailableListener<Mileage> { data ->
        val odo = data.odometerMeters.floatOrNull()
        val cached = VehicleDataCache.get()
        VehicleDataCache.update(
            odometerKm = odo,
            fuelPercent = cached.fuelPercent,
            speedKmh = cached.speedKmh,
            carMake = cached.carMake,
            carModel = cached.carModel,
            carYear = cached.carYear,
            hardwareAvailable = true
        )
    }

    private val speedListener = OnCarDataAvailableListener<Speed> { data ->
        val metersPerSec = data.rawSpeedMetersPerSecond.floatOrNull() ?: return@OnCarDataAvailableListener
        val speedKmh = metersPerSec * 3.6f
        val cached = VehicleDataCache.get()
        VehicleDataCache.update(
            odometerKm = cached.odometerKm,
            fuelPercent = cached.fuelPercent,
            speedKmh = speedKmh,
            carMake = cached.carMake,
            carModel = cached.carModel,
            carYear = cached.carYear,
            hardwareAvailable = true
        )
    }

    init {
        owner.lifecycle.addObserver(this)
    }

    fun start() {
        requestCarPermissions()
    }

    private fun requestCarPermissions() {
        carContext.requestPermissions(
            CAR_PERMISSIONS,
            carContext.mainExecutor,
            OnRequestPermissionsListener { approved, _ ->
                if (approved.any { it in CAR_PERMISSIONS }) {
                    registerListeners()
                }
            }
        )
    }

    private fun registerListeners() {
        carInfo.addEnergyLevelListener(carContext.mainExecutor, energyListener)
        carInfo.addMileageListener(carContext.mainExecutor, mileageListener)
        carInfo.addSpeedListener(carContext.mainExecutor, speedListener)

        carInfo.fetchModel(carContext.mainExecutor) { model ->
            val make = model.manufacturer.stringOrNull()
            val name = model.name.stringOrNull()
            val year = model.year.intOrNull()
            val cached = VehicleDataCache.get()
            VehicleDataCache.update(
                odometerKm = cached.odometerKm,
                fuelPercent = cached.fuelPercent,
                speedKmh = cached.speedKmh,
                carMake = make,
                carModel = name,
                carYear = year,
                hardwareAvailable = true
            )
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        runCatching { carInfo.removeEnergyLevelListener(energyListener) }
        runCatching { carInfo.removeMileageListener(mileageListener) }
        runCatching { carInfo.removeSpeedListener(speedListener) }
    }

    private fun CarValue<Float>.floatOrNull(): Float? =
        if (status == CarValue.STATUS_SUCCESS) value else null

    private fun CarValue<Int>.intOrNull(): Int? =
        if (status == CarValue.STATUS_SUCCESS) value else null

    private fun CarValue<String>.stringOrNull(): String? =
        if (status == CarValue.STATUS_SUCCESS) value else null

    companion object {
        val CAR_PERMISSIONS = listOf(
            "com.google.android.gms.permission.CAR_FUEL",
            "com.google.android.gms.permission.CAR_MILEAGE",
            "com.google.android.gms.permission.CAR_SPEED"
        )
    }
}
