package com.trippin.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "refuel_events",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("timestamp")]
)
data class RefuelEvent(
    @PrimaryKey val id: String,
    val carId: String,
    val timestamp: Long,
    val fuelPercentBefore: Float? = null,
    val fuelPercentAfter: Float? = null,
    val litresFilled: Float,
    val fuelPricePerLitreInr: Float,
    val totalCostInr: Float,
    val tag: String? = null,
    val odometerKm: Float? = null,
    val isAutoDetected: Boolean = false
)
