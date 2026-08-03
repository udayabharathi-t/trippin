package com.trippin.app.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Latest vehicle sensor readings from the car hardware API (when available).
 * Populated by [com.trippin.app.auto.CarHardwareCollector] during an active car session.
 */
object VehicleDataCache {
    data class Snapshot(
        val odometerKm: Float? = null,
        val fuelPercent: Float? = null,
        val speedKmh: Float? = null,
        val carMake: String? = null,
        val carModel: String? = null,
        val carYear: Int? = null,
        val hardwareAvailable: Boolean = false,
        val lastUpdatedAt: Long = 0L
    )

    private val latest = AtomicReference(Snapshot())
    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun update(
        odometerKm: Float? = latest.get().odometerKm,
        fuelPercent: Float? = latest.get().fuelPercent,
        speedKmh: Float? = latest.get().speedKmh,
        carMake: String? = latest.get().carMake,
        carModel: String? = latest.get().carModel,
        carYear: Int? = latest.get().carYear,
        hardwareAvailable: Boolean = latest.get().hardwareAvailable
    ) {
        val snapshot = Snapshot(
            odometerKm = odometerKm,
            fuelPercent = fuelPercent,
            speedKmh = speedKmh,
            carMake = carMake,
            carModel = carModel,
            carYear = carYear,
            hardwareAvailable = hardwareAvailable,
            lastUpdatedAt = System.currentTimeMillis()
        )
        latest.set(snapshot)
        _state.value = snapshot
    }

    fun get(): Snapshot = latest.get()

    fun clear() {
        latest.set(Snapshot())
        _state.value = Snapshot()
    }
}
