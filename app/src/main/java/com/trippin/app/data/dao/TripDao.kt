package com.trippin.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trippin.app.data.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE carId = :carId ORDER BY startTime DESC")
    fun observeByCar(carId: String): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun observeAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: String): Trip?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: String): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrip(): Trip?

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    fun observeActiveTrip(): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE carId = :carId AND isActive = 1 LIMIT 1")
    suspend fun getActiveTripForCar(carId: String): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip)

    @Update
    suspend fun update(trip: Trip)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    suspend fun getAllOnce(): List<Trip>

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: String)
}
