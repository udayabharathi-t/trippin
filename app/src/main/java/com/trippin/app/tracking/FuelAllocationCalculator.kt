package com.trippin.app.tracking

import com.trippin.app.data.model.RefuelEvent
import com.trippin.app.data.model.Trip

/**
 * Allocates fuel cost and economy across trips using full-tank refuel events.
 *
 * Assumes each refuel fills the tank to 100%, so [RefuelEvent.litresFilled] is the fuel
 * consumed since the previous refuel. Cost is split across intervening trips by GPS distance.
 */
object FuelAllocationCalculator {

    data class AllocationResult(
        val tripId: String,
        val estimatedFuelCostInr: Float,
        val fuelPricePerLitreInr: Float
    )

    data class PeriodSummary(
        val refuelId: String,
        val tripCount: Int,
        val totalDistanceKm: Float,
        val totalCostInr: Float,
        val avgKmPerLitre: Float?
    )

    fun allocateForCar(
        refuelsAsc: List<RefuelEvent>,
        tripsAsc: List<Trip>
    ): Pair<List<AllocationResult>, List<PeriodSummary>> {
        if (refuelsAsc.isEmpty()) return emptyList<AllocationResult>() to emptyList()

        val completedTrips = tripsAsc.filter { !it.isActive && it.endTime != null }
        val allocations = mutableListOf<AllocationResult>()
        val summaries = mutableListOf<PeriodSummary>()

        refuelsAsc.forEachIndexed { index, refuel ->
            val periodStart = if (index == 0) 0L else refuelsAsc[index - 1].timestamp
            val periodEnd = refuel.timestamp

            val periodTrips = completedTrips.filter { trip ->
                val end = trip.endTime ?: return@filter false
                end > periodStart && end <= periodEnd
            }

            if (periodTrips.isEmpty()) return@forEachIndexed

            val totalDistance = periodTrips.sumOf { it.distanceKm.toDouble() }.toFloat()
            if (totalDistance <= 0f) return@forEachIndexed

            val avgKmPerLitre = if (refuel.litresFilled > 0f) {
                totalDistance / refuel.litresFilled
            } else {
                null
            }

            summaries += PeriodSummary(
                refuelId = refuel.id,
                tripCount = periodTrips.size,
                totalDistanceKm = totalDistance,
                totalCostInr = refuel.totalCostInr,
                avgKmPerLitre = avgKmPerLitre
            )

            periodTrips.forEach { trip ->
                val share = trip.distanceKm / totalDistance
                allocations += AllocationResult(
                    tripId = trip.id,
                    estimatedFuelCostInr = refuel.totalCostInr * share,
                    fuelPricePerLitreInr = refuel.fuelPricePerLitreInr
                )
            }
        }

        return allocations to summaries
    }

    fun fuelEconomyKmPerLitre(trip: Trip): Float? {
        val cost = trip.estimatedFuelCostInr ?: return null
        val price = trip.fuelPricePerLitreInr ?: return null
        if (cost <= 0f || price <= 0f || trip.distanceKm <= 0f) return null
        val litres = cost / price
        if (litres <= 0f) return null
        return trip.distanceKm / litres
    }
}
