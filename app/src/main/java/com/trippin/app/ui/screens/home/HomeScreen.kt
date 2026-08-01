package com.trippin.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trippin.app.di.AppContainer
import com.trippin.app.ui.components.TripCard
import com.trippin.app.util.Formatters

@Composable
fun HomeScreen(
    container: AppContainer,
    onTripClick: (String) -> Unit
) {
    val activeTrip by container.tripRepository.observeActiveTrip().collectAsState(initial = null)
    val recentTrips by container.tripRepository.observeAll().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Trippin", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Auto-tracks trips when Android Auto is connected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active trip", style = MaterialTheme.typography.titleMedium)
                    if (activeTrip != null) {
                        Text("Recording since ${Formatters.time(activeTrip!!.startTime)}")
                        Text("Tap Trips for details")
                    } else {
                        Text("No active trip — connect Android Auto to start")
                    }
                }
            }
        }

        item {
            Text("Recent trips", style = MaterialTheme.typography.titleMedium)
        }

        items(recentTrips.take(5)) { trip ->
            TripCard(
                trip = trip,
                modifier = Modifier.clickable { onTripClick(trip.id) }
            )
        }
    }
}
