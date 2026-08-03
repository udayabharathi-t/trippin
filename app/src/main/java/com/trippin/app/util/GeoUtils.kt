package com.trippin.app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }

    fun pathDistanceKm(points: List<Pair<Double, Double>>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 1 until points.size) {
            val (lat1, lon1) = points[i - 1]
            val (lat2, lon2) = points[i]
            total += haversineKm(lat1, lon1, lat2, lon2)
        }
        return total
    }
}
