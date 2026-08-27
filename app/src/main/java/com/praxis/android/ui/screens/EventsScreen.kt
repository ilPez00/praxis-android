package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.praxisweb.xyz.R
import com.praxis.android.data.model.Event

@Composable
fun EventsScreen(events: List<Event>, loading: Boolean, onEventClick: (String) -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (events.isEmpty()) {
            item {
                Text(text = "No events yet.", modifier = Modifier.padding(16.dp))
            }
        }
        items(events) { event ->
            PraxisCard(modifier = Modifier.fillMaxWidth(), onClick = { onEventClick(event.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                    if (!event.description.isNullOrBlank()) {
                        Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = "${event.startDate.take(10)} · ${event.attendees} attending", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
