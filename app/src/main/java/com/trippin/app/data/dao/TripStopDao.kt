package com.trippin.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trippin.app.data.model.TripStop
import kotlinx.coroutines.flow.Flow

@Dao
interface TripStopDao {
    @Query("SELECT * FROM trip_stops WHERE tripId = :tripId ORDER BY orderIndex ASC")
    fun observeForTrip(tripId: String): Flow<List<TripStop>>

    @Query("SELECT * FROM trip_stops WHERE tripId = :tripId ORDER BY orderIndex ASC")
    suspend fun getForTrip(tripId: String): List<TripStop>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stop: TripStop)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<TripStop>)

    @Query("DELETE FROM trip_stops WHERE tripId = :tripId")
    suspend fun deleteForTrip(tripId: String)
}
