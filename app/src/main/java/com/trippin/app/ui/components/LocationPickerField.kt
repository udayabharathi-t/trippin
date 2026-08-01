package com.trippin.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.Locale

@Composable
fun LocationPickerField(
    label: String,
    locationName: String,
    latitude: Double?,
    longitude: Double?,
    onLocationChange: (name: String, lat: Double?, lng: Double?) -> Unit
) {
    Column {
        OutlinedTextField(
            value = locationName,
            onValueChange = { onLocationChange(it, latitude, longitude) },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = latitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty(),
            onValueChange = { v ->
                onLocationChange(locationName, v.toDoubleOrNull(), longitude)
            },
            label = { Text("Latitude") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = longitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty(),
            onValueChange = { v ->
                onLocationChange(locationName, latitude, v.toDoubleOrNull())
            },
            label = { Text("Longitude") },
            modifier = Modifier.fillMaxWidth()
        )
        if (latitude != null && longitude != null) {
            Text("Auto-captured coordinates available — edit name or coords as needed")
        }
    }
}
