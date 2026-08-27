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
import com.praxis.android.data.model.LeaderboardEntry
import com.praxis.android.ui.viewmodel.LeaderboardViewModel
import com.praxis.android.ui.viewmodel.LeaderboardUiState

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel, onNavigateToProfile: (String) -> Unit) {
    val state = viewModel.uiState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (state) {
            is LeaderboardUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            is LeaderboardUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is LeaderboardUiState.Success -> {
                if (state.entries.isEmpty()) {
                    Text(text = "No leaderboard entries yet.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.entries) { entry: LeaderboardEntry ->
                            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "#${entry.rank} ${entry.name}", style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Streak: ${entry.streak} | Points: ${entry.points}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
