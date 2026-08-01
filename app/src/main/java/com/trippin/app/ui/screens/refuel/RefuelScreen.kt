package com.trippin.app.ui.screens.refuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trippin.app.data.model.Car
import com.trippin.app.data.model.RefuelEvent
import com.trippin.app.di.AppContainer
import com.trippin.app.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefuelScreen(container: AppContainer) {
    val cars by container.carRepository.observeAll().collectAsState(initial = emptyList())
    var selectedCar by remember { mutableStateOf<Car?>(null) }
    val events by remember(selectedCar?.id) {
        selectedCar?.let { container.refuelRepository.observeByCar(it.id) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var pricePerLitre by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Refuel events", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Fuel level before/after is captured automatically from your car when available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedCar?.name ?: "Select car",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Car") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    cars.forEach { car ->
                        DropdownMenuItem(
                            text = { Text(car.name) },
                            onClick = {
                                selectedCar = car
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add refuel", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = pricePerLitre,
                        onValueChange = { pricePerLitre = it },
                        label = { Text("Price per litre (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = totalCost,
                        onValueChange = { totalCost = it },
                        label = { Text("Total cost (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tag,
                        onValueChange = { tag = it },
                        label = { Text("Tag (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val price = pricePerLitre.toFloatOrNull()
                    val cost = totalCost.toFloatOrNull()
                    if (price != null && cost != null && price > 0f) {
                        Text("Approx. litres filled: ${"%.1f".format(cost / price)} L")
                    }

                    TextButton(
                        onClick = {
                            val carId = selectedCar?.id ?: return@TextButton
                            val priceValue = pricePerLitre.toFloatOrNull() ?: return@TextButton
                            val costValue = totalCost.toFloatOrNull() ?: return@TextButton
                            scope.launch {
                                val saved = container.refuelRepository.recordManualRefuel(
                                    carId = carId,
                                    fuelPricePerLitreInr = priceValue,
                                    totalCostInr = costValue,
                                    tag = tag.ifBlank { null }
                                )
                                if (saved != null) {
                                    statusMessage = buildSavedMessage(saved)
                                    pricePerLitre = ""
                                    totalCost = ""
                                    tag = ""
                                } else {
                                    statusMessage = "Could not save — enter a valid price and total cost."
                                }
                            }
                        },
                        enabled = selectedCar != null
                    ) { Text("Save refuel") }

                    statusMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        items(events) { event ->
            RefuelEventCard(event)
        }
    }
}

private fun buildSavedMessage(event: RefuelEvent): String {
    val fuelNote = when {
        event.fuelPercentBefore != null && event.fuelPercentAfter != null ->
            "Fuel auto-captured: ${event.fuelPercentBefore.toInt()}% → ${event.fuelPercentAfter.toInt()}%."
        event.fuelPercentAfter != null ->
            "Fuel after refill auto-captured at ${event.fuelPercentAfter.toInt()}%."
        else ->
            "Fuel level unavailable — litres estimated from cost."
    }
    return "Refuel saved. $fuelNote"
}

@Composable
private fun RefuelEventCard(event: RefuelEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(Formatters.dateTime(event.timestamp), style = MaterialTheme.typography.titleSmall)
            when {
                event.fuelPercentBefore != null && event.fuelPercentAfter != null -> {
                    Text(
                        "Fuel: ${Formatters.percent(event.fuelPercentBefore)} → " +
                            Formatters.percent(event.fuelPercentAfter)
                    )
                }
                event.fuelPercentAfter != null -> {
                    Text("Fuel after: ${Formatters.percent(event.fuelPercentAfter)}")
                }
            }
            Text("Litres: ${"%.1f".format(event.litresFilled)} L")
            Text("Price: ${Formatters.inr(event.fuelPricePerLitreInr)}/L · Total: ${Formatters.inr(event.totalCostInr)}")
            event.tag?.let { Text("Tag: $it") }
            if (event.isAutoDetected) {
                Text("Auto-detected", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
