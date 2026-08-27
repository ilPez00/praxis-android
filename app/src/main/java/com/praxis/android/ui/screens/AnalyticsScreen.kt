package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
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
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R
import com.praxis.android.ui.viewmodel.AnalyticsViewModel
import com.praxis.android.ui.viewmodel.AnalyticsUiState

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, onBack: () -> Unit) {
    val state = viewModel.uiState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (state) {
            is AnalyticsUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            is AnalyticsUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    PraxisButton(onClick = { viewModel.loadAnalytics() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Retry")
                    }
                }
            }
            is AnalyticsUiState.Success -> {
                Text(text = "Analytics", style = MaterialTheme.typography.headlineLarge)
                if (state.trackers.isEmpty()) {
                    Text(text = "No tracker data yet.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.trackers) { tracker ->
                            PraxisCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = tracker.label, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "${tracker.value} ${tracker.unit}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = tracker.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        PraxisButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.profile))
        }
    }
}
