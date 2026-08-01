package com.trippin.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trippin.app.data.model.RefuelEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface RefuelDao {
    @Query("SELECT * FROM refuel_events WHERE carId = :carId ORDER BY timestamp DESC")
    fun observeByCar(carId: String): Flow<List<RefuelEvent>>

    @Query("SELECT * FROM refuel_events WHERE carId = :carId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForCar(carId: String): RefuelEvent?

    @Query("SELECT * FROM refuel_events WHERE id = :id")
    suspend fun getById(id: String): RefuelEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: RefuelEvent)

    @Update
    suspend fun update(event: RefuelEvent)

    @Query("SELECT * FROM refuel_events ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<RefuelEvent>

    @Query("DELETE FROM refuel_events WHERE id = :id")
    suspend fun delete(id: String)
}
