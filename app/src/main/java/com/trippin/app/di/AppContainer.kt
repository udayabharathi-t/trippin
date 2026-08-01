package com.trippin.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

    val carRepository = CarRepository(database.carDao())
    val vehicleDataProvider = VehicleDataProvider(appContext)
    val tripRepository = TripRepository(
        tripDao = database.tripDao(),
        sampleDao = database.tripSampleDao(),
        stopDao = database.tripStopDao(),
        refuelDao = database.refuelDao(),
        carDao = database.carDao()
    )
    val refuelRepository = RefuelRepository(
        refuelDao = database.refuelDao(),
        carDao = database.carDao(),
        tripDao = database.tripDao(),
        tripSampleDao = database.tripSampleDao(),
        vehicleDataProvider = vehicleDataProvider
    )

    val tripTracker = TripTracker(
        context = appContext,
        carRepository = carRepository,
        tripRepository = tripRepository,
        refuelRepository = refuelRepository,
        vehicleDataProvider = vehicleDataProvider
    )

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS refuel_events_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        carId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        fuelPercentBefore REAL,
                        fuelPercentAfter REAL,
                        litresFilled REAL NOT NULL,
                        fuelPricePerLitreInr REAL NOT NULL,
                        totalCostInr REAL NOT NULL,
                        tag TEXT,
                        odometerKm REAL,
                        isAutoDetected INTEGER NOT NULL,
                        FOREIGN KEY(carId) REFERENCES cars(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO refuel_events_new
                    SELECT id, carId, timestamp, fuelPercentBefore, fuelPercentAfter,
                           litresFilled, fuelPricePerLitreInr, totalCostInr, tag, odometerKm, isAutoDetected
                    FROM refuel_events
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE refuel_events")
                db.execSQL("ALTER TABLE refuel_events_new RENAME TO refuel_events")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_refuel_events_carId ON refuel_events(carId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_refuel_events_timestamp ON refuel_events(timestamp)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN isMerged INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trip_stops (
                        id TEXT NOT NULL PRIMARY KEY,
                        tripId TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        locationName TEXT,
                        label TEXT,
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_stops_tripId ON trip_stops(tripId)")
            }
        }
    }
}
