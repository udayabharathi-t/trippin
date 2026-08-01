package com.trippin.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey val id: String,
    val name: String,
    val hardwareId: String,
    val vin: String? = null,
    val maxFuelCapacityLitres: Float = 45f,
    val createdAt: Long = System.currentTimeMillis()
)
