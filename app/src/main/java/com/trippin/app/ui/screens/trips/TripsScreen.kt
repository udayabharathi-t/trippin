package com.trippin.app.ui.screens.trips

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trippin.app.data.repository.TripRepository
import com.trippin.app.di.AppContainer
import com.trippin.app.ui.components.TripCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripsScreen(
    container: AppContainer,
    onTripClick: (String) -> Unit
) {
    val trips by container.tripRepository.observeAll().collectAsState(initial = emptyList())
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { exitSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    when (val result = container.tripRepository.mergeTrips(selectedIds.toList())) {
                                        is TripRepository.MergeResult.Success -> {
                                            snackbarHostState.showSnackbar("Trips merged successfully")
                                            exitSelection()
                                            onTripClick(result.trip.id)
                                        }
                                        is TripRepository.MergeResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            },
                            enabled = selectedIds.size >= 2
                        ) {
                            Icon(Icons.Default.MergeType, contentDescription = null)
                            Text(" Merge")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (!selectionMode) {
                    Text("All trips", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Long press a trip to select and merge multiple segments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(trips, key = { it.id }) { trip ->
                val isSelected = selectedIds.contains(trip.id)
                TripCard(
                    trip = trip,
                    selectionMode = selectionMode,
                    selected = isSelected,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (isSelected) {
                                    selectedIds - trip.id
                                } else {
                                    selectedIds + trip.id
                                }
                                if (selectedIds.isEmpty()) {
                                    selectionMode = false
                                }
                            } else if (!trip.isActive) {
                                onTripClick(trip.id)
                            }
                        },
                        onLongClick = {
                            if (!trip.isActive) {
                                selectionMode = true
                                selectedIds = selectedIds + trip.id
                            }
                        }
                    )
                )
            }
        }
    }
}
