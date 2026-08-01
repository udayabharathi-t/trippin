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

    var fuelBefore by remember { mutableStateOf("") }
    var fuelAfter by remember { mutableStateOf("") }
    var pricePerLitre by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Refuel events", style = MaterialTheme.typography.headlineSmall)
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
                    OutlinedTextField(fuelBefore, { fuelBefore = it }, label = { Text("Fuel % before") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(fuelAfter, { fuelAfter = it }, label = { Text("Fuel % after") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pricePerLitre, { pricePerLitre = it }, label = { Text("Price per litre (₹)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(totalCost, { totalCost = it }, label = { Text("Total cost (₹)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(tag, { tag = it }, label = { Text("Tag (optional)") }, modifier = Modifier.fillMaxWidth())

                    val car = selectedCar
                    val before = fuelBefore.toFloatOrNull()
                    val after = fuelAfter.toFloatOrNull()
                    if (car != null && before != null && after != null) {
                        val litres = ((after - before).coerceAtLeast(0f) / 100f) * car.maxFuelCapacityLitres
                        Text("Approx. litres filled: ${"%.1f".format(litres)} L")
                    }

                    TextButton(
                        onClick = {
                            val carId = selectedCar?.id ?: return@TextButton
                            val b = fuelBefore.toFloatOrNull() ?: return@TextButton
                            val a = fuelAfter.toFloatOrNull() ?: return@TextButton
                            val price = pricePerLitre.toFloatOrNull() ?: 0f
                            val cost = totalCost.toFloatOrNull() ?: 0f
                            scope.launch {
                                container.refuelRepository.recordRefuel(
                                    carId = carId,
                                    fuelPercentBefore = b,
                                    fuelPercentAfter = a,
                                    fuelPricePerLitreInr = price,
                                    totalCostInr = cost,
                                    tag = tag.ifBlank { null }
                                )
                                fuelBefore = ""
                                fuelAfter = ""
                                pricePerLitre = ""
                                totalCost = ""
                                tag = ""
                            }
                        },
                        enabled = selectedCar != null
                    ) { Text("Save refuel") }
                }
            }
        }

        items(events) { event ->
            RefuelEventCard(event)
        }
    }
}

@Composable
private fun RefuelEventCard(event: RefuelEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(Formatters.dateTime(event.timestamp), style = MaterialTheme.typography.titleSmall)
            Text("Fuel: ${Formatters.percent(event.fuelPercentBefore)} → ${Formatters.percent(event.fuelPercentAfter)}")
            Text("Litres: ${"%.1f".format(event.litresFilled)} L")
            Text("Price: ${Formatters.inr(event.fuelPricePerLitreInr)}/L · Total: ${Formatters.inr(event.totalCostInr)}")
            event.tag?.let { Text("Tag: $it") }
            if (event.isAutoDetected) {
                Text("Auto-detected", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
