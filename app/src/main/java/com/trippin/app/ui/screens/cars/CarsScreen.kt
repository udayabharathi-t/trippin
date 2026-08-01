package com.trippin.app.ui.screens.cars

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
import com.trippin.app.di.AppContainer
import kotlinx.coroutines.launch

@Composable
fun CarsScreen(container: AppContainer) {
    val cars by container.carRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Your cars", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Cars are identified automatically when Android Auto connects. Name each car and set its fuel tank capacity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(cars) { car ->
            CarEditCard(
                car = car,
                onSave = { updated ->
                    scope.launch { container.carRepository.update(updated) }
                }
            )
        }

        if (cars.isEmpty()) {
            item {
                Text("No cars yet. Connect Android Auto to register your first car.")
            }
        }
    }
}

@Composable
private fun CarEditCard(car: Car, onSave: (Car) -> Unit) {
    var name by remember(car.id) { mutableStateOf(car.name) }
    var capacity by remember(car.id) { mutableStateOf(car.maxFuelCapacityLitres.toString()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ID: ${car.hardwareId}", style = MaterialTheme.typography.bodySmall)
            car.vin?.let { Text("VIN: $it", style = MaterialTheme.typography.bodySmall) }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Car name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = capacity,
                onValueChange = { capacity = it },
                label = { Text("Max fuel capacity (litres)") },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = {
                val litres = capacity.toFloatOrNull() ?: car.maxFuelCapacityLitres
                onSave(car.copy(name = name, maxFuelCapacityLitres = litres))
            }) {
                Text("Save")
            }
        }
    }
}
