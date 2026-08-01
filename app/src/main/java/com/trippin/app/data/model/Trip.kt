package com.trippin.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("startTime")]
)
data class Trip(
    @PrimaryKey val id: String,
    val carId: String,
    val name: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val startOdometerKm: Float? = null,
    val endOdometerKm: Float? = null,
    val startFuelPercent: Float? = null,
    val endFuelPercent: Float? = null,
    val maxSpeedKmh: Float = 0f,
    val averageSpeedKmh: Float = 0f,
    val distanceKm: Float = 0f,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val startLocationName: String? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val endLocationName: String? = null,
    val estimatedFuelCostInr: Float? = null,
    val fuelPricePerLitreInr: Float? = null,
    val isActive: Boolean = false,
    val autoStarted: Boolean = false,
    val isMerged: Boolean = false
)
