package com.trippin.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trippin.app.data.dao.CarDao
import com.trippin.app.data.dao.RefuelDao
import com.trippin.app.data.dao.TripDao
import com.trippin.app.data.dao.TripSampleDao
import com.trippin.app.data.model.Car
import com.trippin.app.data.model.RefuelEvent
import com.trippin.app.data.model.Trip
import com.trippin.app.data.model.TripSample

@Database(
    entities = [Car::class, Trip::class, RefuelEvent::class, TripSample::class],
    version = 2,
    exportSchema = true
)
abstract class TrippinDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun tripDao(): TripDao
    abstract fun refuelDao(): RefuelDao
    abstract fun tripSampleDao(): TripSampleDao
}
