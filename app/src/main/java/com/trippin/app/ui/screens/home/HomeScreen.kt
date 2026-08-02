package com.trippin.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.trippin.app.R
import androidx.compose.ui.platform.LocalContext
import com.trippin.app.TrippinApplication
import com.trippin.app.di.AppContainer
import com.trippin.app.util.PermissionsHelper
import com.trippin.app.tracking.VehicleDataCache
import com.trippin.app.tracking.TripLiveStats
import com.trippin.app.ui.components.TripCard
import com.trippin.app.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    container: AppContainer,
    onTripClick: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TrippinApplication
    val aaConnected by app.carConnectionMonitor.isAndroidAutoConnected.collectAsState()
    val carData by VehicleDataCache.state.collectAsState()
    val hasPermissions = PermissionsHelper.hasAllRequired(context)
    val activeTrip by container.tripRepository.observeActiveTrip().collectAsState(initial = null)
    val liveStats by container.tripTracker.liveStats.collectAsState()
    val recentTrips by container.tripRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                "Auto-tracks trips when Android Auto is connected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            !hasPermissions -> "Location permission required"
                            aaConnected -> "Android Auto connected"
                            else -> "Android Auto not connected"
                        },
                        color = when {
                            !hasPermissions -> MaterialTheme.colorScheme.error
                            aaConnected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (aaConnected && hasPermissions) {
                        Text(
                            carSensorStatus(carData, activeTrip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active trip", style = MaterialTheme.typography.titleMedium)
                    if (activeTrip != null) {
                        Text("Recording since ${Formatters.time(activeTrip!!.startTime)}")
                        ActiveTripStats(liveStats)
                        Button(
                            onClick = {
                                syncing = true
                                syncMessage = null
                                scope.launch {
                                    val result = container.tripTracker.syncNow()
                                    syncing = false
                                    syncMessage = if (result != null) {
                                        "Synced at ${Formatters.time(result.lastSyncedAt)}"
                                    } else {
                                        "Could not sync — no active trip"
                                    }
                                }
                            },
                            enabled = !syncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Text(if (syncing) " Syncing…" else " Sync with car")
                        }
                        syncMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (aaConnected && hasPermissions) {
                        Text("Android Auto is connected but not recording yet")
                        Button(
                            onClick = {
                                app.carConnectionMonitor.checkAndStartIfConnected(fromUserAction = true)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start trip now")
                        }
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

private fun carSensorStatus(
    carData: VehicleDataCache.Snapshot,
    activeTrip: com.trippin.app.data.model.Trip?
): String {
    val hasOdo = carData.odometerKm != null || activeTrip?.startOdometerKm != null
    val hasFuel = carData.fuelPercent != null || activeTrip?.startFuelPercent != null

    return when {
        hasOdo && hasFuel -> "Car odometer and fuel data connected"
        hasOdo -> "Odometer connected · fuel unavailable on this car"
        hasFuel -> "Fuel connected · odometer unavailable on this car"
        carData.hardwareAvailable -> "Waiting for car sensor data…"
        else ->
            "Distance uses GPS until you open Trippin on the car screen and grant Fuel/Mileage permissions"
    }
}

@Composable
private fun ActiveTripStats(stats: TripLiveStats?) {
    if (stats == null) {
        Text(
            "Tap sync to pull latest data from your car",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    if (stats.estimatedFuelCostInr != null && stats.estimatedFuelCostInr > 0f) {
        Text("Trip cost: ${Formatters.inr(stats.estimatedFuelCostInr)}")
        Text(
            "Approx. based on last refill · ${Formatters.km(stats.distanceKm)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Max: ${Formatters.speed(stats.maxSpeedKmh)}")
            Text("Avg: ${Formatters.speed(stats.averageSpeedKmh)}")
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formatters.km(stats.distanceKm))
            stats.fuelEconomyKmPerLitre?.let {
                Text(Formatters.fuelEconomy(it))
            }
        }
    }
}
