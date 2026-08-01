package com.trippin.app.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlin.math.sqrt

data class VehicleSnapshot(
    val odometerKm: Float? = null,
    val fuelPercent: Float? = null,
    val speedKmh: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hardwareId: String? = null,
    val vin: String? = null
)

class VehicleDataProvider(context: Context) {
    private val fusedLocation: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastLocationTime: Long = 0
    private var gpsSpeedKmh: Float = 0f

    @SuppressLint("MissingPermission")
    suspend fun captureSnapshot(): VehicleSnapshot {
        val location = try {
            val token = CancellationTokenSource()
            fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
        } catch (_: Exception) {
            null
        }

        location?.let { updateFromLocation(it) }

        val carProps = readCarProperties()

        return VehicleSnapshot(
            odometerKm = carProps.odometerKm,
            fuelPercent = carProps.fuelPercent,
            speedKmh = carProps.speedKmh ?: gpsSpeedKmh,
            latitude = lastLatitude,
            longitude = lastLongitude,
            hardwareId = carProps.hardwareId,
            vin = carProps.vin
        )
    }

    private fun updateFromLocation(location: Location) {
        val now = System.currentTimeMillis()
        if (lastLatitude != null && lastLongitude != null && lastLocationTime > 0) {
            val dtHours = (now - lastLocationTime) / 3_600_000f
            if (dtHours > 0) {
                val distanceKm = haversineKm(
                    lastLatitude!!, lastLongitude!!,
                    location.latitude, location.longitude
                )
                gpsSpeedKmh = (distanceKm / dtHours).coerceAtLeast(0f)
            }
        }
        lastLatitude = location.latitude
        lastLongitude = location.longitude
        lastLocationTime = now
        if (location.hasSpeed()) {
            gpsSpeedKmh = location.speed * 3.6f
        }
    }

    @Suppress("DEPRECATION")
    private fun readCarProperties(): CarProperties {
        return try {
            val hardwareId = Build.MODEL + "_" + Build.DEVICE
            CarProperties(hardwareId = hardwareId)
        } catch (_: Exception) {
            CarProperties(hardwareId = Build.MODEL)
        }
    }

    private data class CarProperties(
        val odometerKm: Float? = null,
        val fuelPercent: Float? = null,
        val speedKmh: Float? = null,
        val hardwareId: String? = null,
        val vin: String? = null
    )

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }
}
