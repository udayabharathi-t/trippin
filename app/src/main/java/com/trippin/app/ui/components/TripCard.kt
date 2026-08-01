package com.trippin.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trippin.app.data.model.Trip
import com.trippin.app.util.Formatters

@Composable
fun TripCard(trip: Trip, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                trip.name ?: "Trip ${Formatters.date(trip.startTime)}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${Formatters.dateTime(trip.startTime)} → ${trip.endTime?.let { Formatters.dateTime(it) } ?: "Active"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Distance: ${Formatters.km(trip.distanceKm)} · Avg: ${Formatters.speed(trip.averageSpeedKmh)}")
            trip.estimatedFuelCostInr?.let {
                Text("Est. fuel: ${Formatters.inr(it)}")
                Text(
                    "Approx. based on last refill",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
