package com.trippin.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trippin.app.data.model.TripSample

@Dao
interface TripSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: TripSample)

    @Query("SELECT * FROM trip_samples WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getForTrip(tripId: String): List<TripSample>

    @Query("SELECT * FROM trip_samples ORDER BY tripId, timestamp ASC")
    suspend fun getAll(): List<TripSample>

    @Query("""
        SELECT ts.* FROM trip_samples ts
        INNER JOIN trips t ON t.id = ts.tripId
        WHERE t.carId = :carId AND ts.fuelPercent IS NOT NULL
        ORDER BY ts.timestamp DESC
        LIMIT 1
    """)
    suspend fun getLatestFuelSampleForCar(carId: String): TripSample?

    @Query("UPDATE trip_samples SET tripId = :newTripId WHERE tripId IN (:oldTripIds)")
    suspend fun reassignTrips(oldTripIds: List<String>, newTripId: String)

    @Query("DELETE FROM trip_samples WHERE tripId = :tripId")
    suspend fun deleteForTrip(tripId: String)
}
