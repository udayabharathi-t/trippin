package com.trippin.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trippin.app.data.model.Trip
import com.trippin.app.util.Formatters

@Composable
fun TripCard(
    trip: Trip,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        trip.name ?: "Trip ${Formatters.date(trip.startTime)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (trip.isMerged) {
                        Text(
                            " · Merged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (trip.isActive) {
                        Text(
                            " · Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    "${Formatters.dateTime(trip.startTime)} → ${trip.endTime?.let { Formatters.dateTime(it) } ?: "In progress"}",
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
}
