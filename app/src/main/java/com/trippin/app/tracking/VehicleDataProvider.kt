package com.trippin.app.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import com.trippin.app.tracking.VehicleDataCache
import com.trippin.app.util.GeoUtils

data class VehicleSnapshot(
    val odometerKm: Float? = null,
    val fuelPercent: Float? = null,
    val speedKmh: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hardwareId: String? = null,
    val vin: String? = null,
    val distanceSource: DistanceSource = DistanceSource.UNKNOWN
)

enum class DistanceSource {
    CAR_ODOMETER,
    GPS_ESTIMATE,
    UNKNOWN
}

class VehicleDataProvider(context: Context) {
    private val fusedLocation: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastLocationTime: Long = 0
    private var gpsSpeedKmh: Float = 0f
    private var gpsDistanceKm: Float = 0f

    @SuppressLint("MissingPermission")
    suspend fun captureSnapshot(): VehicleSnapshot {
        val location = try {
            val token = CancellationTokenSource()
            fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
        } catch (_: Exception) {
            null
        }

        location?.let { updateFromLocation(it) }

        val carData = VehicleDataCache.get()
        val odometerKm = carData.odometerKm
        val fuelPercent = carData.fuelPercent
        val carSpeed = carData.speedKmh

        val distanceSource = when {
            odometerKm != null -> DistanceSource.CAR_ODOMETER
            gpsDistanceKm > 0f -> DistanceSource.GPS_ESTIMATE
            else -> DistanceSource.UNKNOWN
        }

        val hardwareId = buildHardwareId(carData)

        return VehicleSnapshot(
            odometerKm = odometerKm,
            fuelPercent = fuelPercent,
            speedKmh = carSpeed ?: gpsSpeedKmh,
            latitude = lastLatitude,
            longitude = lastLongitude,
            hardwareId = hardwareId,
            distanceSource = distanceSource
        )
    }

    fun gpsEstimatedDistanceKm(): Float = gpsDistanceKm

    private fun buildHardwareId(carData: VehicleDataCache.Snapshot): String {
        val parts = listOfNotNull(carData.carMake, carData.carModel, carData.carYear?.toString())
        if (parts.isNotEmpty()) {
            return parts.joinToString("_").replace(" ", "-")
        }
        return "aa_unknown_car"
    }

    private fun updateFromLocation(location: Location) {
        val now = System.currentTimeMillis()
        if (lastLatitude != null && lastLongitude != null && lastLocationTime > 0) {
            val dtHours = (now - lastLocationTime) / 3_600_000f
            val segmentKm = GeoUtils.haversineKm(
                lastLatitude!!, lastLongitude!!,
                location.latitude, location.longitude
            )
            if (segmentKm > 0.01f) {
                gpsDistanceKm += segmentKm
            }
            if (dtHours > 0) {
                gpsSpeedKmh = (segmentKm / dtHours).coerceAtLeast(0f)
            }
        }
        lastLatitude = location.latitude
        lastLongitude = location.longitude
        lastLocationTime = now
        if (location.hasSpeed()) {
            gpsSpeedKmh = location.speed * 3.6f
        }
    }
}
