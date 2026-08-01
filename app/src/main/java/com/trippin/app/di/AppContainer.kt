package com.trippin.app.di

import android.content.Context
import androidx.room.Room
import com.trippin.app.data.TrippinDatabase
import com.trippin.app.data.repository.CarRepository
import com.trippin.app.data.repository.RefuelRepository
import com.trippin.app.data.repository.TripRepository
import com.trippin.app.tracking.TripTracker
import com.trippin.app.tracking.VehicleDataProvider

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: TrippinDatabase = Room.databaseBuilder(
        appContext,
        TrippinDatabase::class.java,
        "trippin.db"
    ).build()

    val carRepository = CarRepository(database.carDao())
    val tripRepository = TripRepository(
        tripDao = database.tripDao(),
        sampleDao = database.tripSampleDao(),
        refuelDao = database.refuelDao(),
        carDao = database.carDao()
    )
    val refuelRepository = RefuelRepository(database.refuelDao(), database.carDao())

    val vehicleDataProvider = VehicleDataProvider(appContext)
    val tripTracker = TripTracker(
        context = appContext,
        carRepository = carRepository,
        tripRepository = tripRepository,
        refuelRepository = refuelRepository,
        vehicleDataProvider = vehicleDataProvider
    )
}
