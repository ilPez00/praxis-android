package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.praxisweb.xyz.R
import com.praxis.android.data.model.Place

@Composable
fun PlacesScreen(places: List<Place>, loading: Boolean, onPlaceClick: (String) -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (places.isEmpty()) {
            item {
                Text(text = "No places yet.", modifier = Modifier.padding(16.dp))
            }
        }
        items(places) { place ->
            PraxisCard(modifier = Modifier.fillMaxWidth(), onClick = { onPlaceClick(place.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = place.name, style = MaterialTheme.typography.titleMedium)
                    if (!place.description.isNullOrBlank()) {
                        Text(text = place.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = place.category, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
