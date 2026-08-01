package com.trippin.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trippin.app.data.model.Car
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY name ASC")
    fun observeAll(): Flow<List<Car>>

    @Query("SELECT * FROM cars ORDER BY name ASC")
    suspend fun getAll(): List<Car>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getById(id: String): Car?

    @Query("SELECT * FROM cars WHERE hardwareId = :hardwareId LIMIT 1")
    suspend fun getByHardwareId(hardwareId: String): Car?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(car: Car)

    @Update
    suspend fun update(car: Car)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun delete(id: String)
}
