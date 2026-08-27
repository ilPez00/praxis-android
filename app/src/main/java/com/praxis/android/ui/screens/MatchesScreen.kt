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
import com.praxis.android.data.model.Match

@Composable
fun MatchesScreen(matches: List<Match>, loading: Boolean, onMatchClick: (String) -> Unit) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (matches.isEmpty()) {
            item {
                Text(text = "No matches yet. Keep journaling to find compatible partners!", modifier = Modifier.padding(16.dp))
            }
        }
        items(matches) { match ->
            PraxisCard(modifier = Modifier.fillMaxWidth(), onClick = { onMatchClick(match.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = match.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Compatibility: ${(match.score * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    if (match.sharedGoals.isNotEmpty()) {
                        Text(text = "Shared goals: ${match.sharedGoals.take(3).joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
