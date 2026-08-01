package com.trippin.app.ui.screens.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trippin.app.di.AppContainer
import com.trippin.app.ui.components.TripCard

@Composable
fun TripsScreen(
    container: AppContainer,
    onTripClick: (String) -> Unit
) {
    val trips by container.tripRepository.observeAll().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("All trips") }
        items(trips) { trip ->
            TripCard(
                trip = trip,
                modifier = Modifier.clickable { onTripClick(trip.id) }
            )
        }
    }
}
