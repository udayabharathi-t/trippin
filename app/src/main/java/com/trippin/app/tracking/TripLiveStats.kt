package com.trippin.app.tracking

data class TripLiveStats(
    val tripId: String,
    val estimatedFuelCostInr: Float?,
    val fuelPricePerLitreInr: Float?,
    val maxSpeedKmh: Float,
    val averageSpeedKmh: Float,
    val fuelEconomyKmPerLitre: Float?,
    val distanceKm: Float,
    val currentFuelPercent: Float?,
    val lastSyncedAt: Long
)
