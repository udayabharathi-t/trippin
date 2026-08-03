package com.trippin.app.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trippin.app.di.AppContainer
import com.trippin.app.tracking.FuelAllocationCalculator
import com.trippin.app.ui.components.LocationPickerField
import com.trippin.app.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    container: AppContainer,
    onBack: () -> Unit
) {
    val trip by container.tripRepository.observeById(tripId).collectAsState(initial = null)
    val stops by container.tripRepository.observeStopsForTrip(tripId).collectAsState(initial = emptyList())
    var name by remember { mutableStateOf("") }
    var startLocationName by remember { mutableStateOf("") }
    var endLocationName by remember { mutableStateOf("") }
    var startLat by remember { mutableStateOf<Double?>(null) }
    var startLng by remember { mutableStateOf<Double?>(null) }
    var endLat by remember { mutableStateOf<Double?>(null) }
    var endLng by remember { mutableStateOf<Double?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trip) {
        trip?.let {
            name = it.name.orEmpty()
            startLocationName = it.startLocationName.orEmpty()
            endLocationName = it.endLocationName.orEmpty()
            startLat = it.startLatitude
            startLng = it.startLongitude
            endLat = it.endLatitude
            endLng = it.endLongitude
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "Trip details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val current = trip ?: return@TextButton
                        scope.launch {
                            container.tripRepository.updateTrip(
                                current.copy(
                                    name = name.ifBlank { null },
                                    startLocationName = startLocationName.ifBlank { null },
                                    endLocationName = endLocationName.ifBlank { null },
                                    startLatitude = startLat,
                                    startLongitude = startLng,
                                    endLatitude = endLat,
                                    endLongitude = endLng
                                )
                            )
                        }
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        val current = trip
        if (current == null) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Start: ${Formatters.dateTime(current.startTime)}")
            Text("End: ${current.endTime?.let { Formatters.dateTime(it) } ?: "In progress"}")

            Text("Odometer: ${current.startOdometerKm?.let { Formatters.km(it) } ?: "—"} → ${current.endOdometerKm?.let { Formatters.km(it) } ?: "—"}")
            Text("Fuel: ${current.startFuelPercent?.let { Formatters.percent(it) } ?: "—"} → ${current.endFuelPercent?.let { Formatters.percent(it) } ?: "—"}")
            Text("Max speed: ${Formatters.speed(current.maxSpeedKmh)}")
            Text("Avg speed: ${Formatters.speed(current.averageSpeedKmh)}")
            Text("Distance: ${Formatters.km(current.distanceKm)}")

            if (current.estimatedFuelCostInr != null) {
                val cost = current.estimatedFuelCostInr
                Text("Fuel cost: ${Formatters.inr(cost!!)}")
                FuelAllocationCalculator.fuelEconomyKmPerLitre(current)?.let { kmPerL ->
                    Text("Fuel economy: ${Formatters.fuelEconomy(kmPerL)}")
                }
                Text(
                    "Allocated from full-tank refill, split by GPS distance across trips in that tank period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!current.isActive) {
                Text(
                    "Fuel cost pending — log the next full-tank refill to allocate cost across recent trips",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (stops.isNotEmpty()) {
                Text("Midway stops", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${stops.size} stop${if (stops.size == 1) "" else "s"} from merged segments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                stops.forEach { stop ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                stop.label ?: "Stop ${stop.orderIndex + 1}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(Formatters.dateTime(stop.timestamp), style = MaterialTheme.typography.bodySmall)
                            stop.locationName?.let { Text(it) }
                            if (stop.latitude != null && stop.longitude != null) {
                                Text(
                                    "${"%.5f".format(stop.latitude)}, ${"%.5f".format(stop.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            LocationPickerField(
                label = "Start location",
                locationName = startLocationName,
                latitude = startLat,
                longitude = startLng,
                onLocationChange = { n, lat, lng ->
                    startLocationName = n
                    startLat = lat
                    startLng = lng
                }
            )

            LocationPickerField(
                label = "End location",
                locationName = endLocationName,
                latitude = endLat,
                longitude = endLng,
                onLocationChange = { n, lat, lng ->
                    endLocationName = n
                    endLat = lat
                    endLng = lng
                }
            )
        }
    }
}
