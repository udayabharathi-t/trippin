package com.trippin.app.tracking

import com.trippin.app.data.model.RefuelEvent
import com.trippin.app.data.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Test

class FuelAllocationCalculatorTest {

    @Test
    fun splitsRefuelCostByDistanceAcrossTrips() {
        val refuel = RefuelEvent(
            id = "r1",
            carId = "car1",
            timestamp = 2000L,
            litresFilled = 40f,
            fuelPricePerLitreInr = 100f,
            totalCostInr = 4000f
        )
        val trips = listOf(
            trip("t1", endTime = 1500L, distanceKm = 30f),
            trip("t2", endTime = 1800L, distanceKm = 70f)
        )

        val (allocations, summaries) = FuelAllocationCalculator.allocateForCar(
            refuelsAsc = listOf(refuel),
            tripsAsc = trips
        )

        assertEquals(1, summaries.size)
        assertEquals(2, summaries[0].tripCount)
        assertEquals(100f, summaries[0].totalDistanceKm)
        assertEquals(2.5f, summaries[0].avgKmPerLitre!!, 0.01f)

        val byTrip = allocations.associateBy { it.tripId }
        assertEquals(1200f, byTrip["t1"]!!.estimatedFuelCostInr, 0.01f)
        assertEquals(2800f, byTrip["t2"]!!.estimatedFuelCostInr, 0.01f)
    }

    @Test
    fun secondRefuelOnlyAllocatesTripsSincePreviousRefuel() {
        val refuels = listOf(
            RefuelEvent("r1", "car1", 1000L, litresFilled = 45f, fuelPricePerLitreInr = 100f, totalCostInr = 4500f),
            RefuelEvent("r2", "car1", 3000L, litresFilled = 45f, fuelPricePerLitreInr = 110f, totalCostInr = 4950f)
        )
        val trips = listOf(
            trip("t1", endTime = 800L, distanceKm = 50f),
            trip("t2", endTime = 1500L, distanceKm = 50f),
            trip("t3", endTime = 2500L, distanceKm = 100f)
        )

        val (allocations, _) = FuelAllocationCalculator.allocateForCar(refuels, trips)
        val byTrip = allocations.associateBy { it.tripId }

        assertEquals(4500f, byTrip["t1"]!!.estimatedFuelCostInr, 0.01f)
        assertEquals(1650f, byTrip["t2"]!!.estimatedFuelCostInr, 0.01f)
        assertEquals(3300f, byTrip["t3"]!!.estimatedFuelCostInr, 0.01f)
    }

    private fun trip(id: String, endTime: Long, distanceKm: Float) = Trip(
        id = id,
        carId = "car1",
        startTime = endTime - 600_000L,
        endTime = endTime,
        distanceKm = distanceKm,
        isActive = false
    )
}
