package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R
import com.praxis.android.ui.viewmodel.CheckInViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun CheckInScreen(viewModel: CheckInViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val state = viewModel.uiState.value
    var mood by rememberSaveable { mutableStateOf("neutral") }
    var energy by rememberSaveable { mutableStateOf(5) }
    var win by rememberSaveable { mutableStateOf("") }
    val moods = listOf("great", "good", "neutral", "tough", "rough")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            is com.praxis.android.ui.viewmodel.CheckInUiState.Loading -> {
                CircularProgressIndicator()
            }
            is com.praxis.android.ui.viewmodel.CheckInUiState.Success -> {
                if (state.checkedIn) {
                    PraxisCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = stringResource(id = R.string.checked_in_today), style = MaterialTheme.typography.titleLarge)
                            Text(text = stringResource(id = R.string.streak_value, state.streak), style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Points: ${state.totalPoints}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Text(text = stringResource(id = R.string.not_checked_in), style = MaterialTheme.typography.titleLarge)

                    PraxisCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(text = stringResource(id = R.string.streak_value, state.streak), style = MaterialTheme.typography.titleMedium)
                            }

                            Text(text = "Mood")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                moods.forEach { m ->
                                    val selected = mood == m
                                    androidx.compose.material3.FilterChip(
                                        selected = selected,
                                        onClick = { mood = m },
                                        label = { Text(m) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text(text = "Energy: $energy / 10")
                            Slider(value = energy.toFloat(), onValueChange = { energy = it.toInt() }, valueRange = 1f..10f, steps = 8)

                            OutlinedTextField(
                                value = win,
                                onValueChange = { win = it },
                                label = { Text("Win of the day") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    PraxisButton(onClick = { viewModel.checkIn(mood, energy, win) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(id = R.string.check_in_label))
                    }
                }
            }
            is com.praxis.android.ui.viewmodel.CheckInUiState.CheckedIn -> {
                PraxisCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Checked in!", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Streak: ${state.response.streak} days", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Points awarded: ${state.response.pointsAwarded}", style = MaterialTheme.typography.bodyMedium)
                        state.response.mysteryReward?.let { reward ->
                            Text(text = "${reward.emoji} Mystery reward: +${reward.amount} PP (${reward.tier})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    // Fresh check-in data → redraw the widgets and refresh
                    // their snapshot from the server.
                    com.praxis.android.widget.WidgetUpdateManager.requestWidgetUpdate()
                }
            }
            is com.praxis.android.ui.viewmodel.CheckInUiState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }

        androidx.compose.material3.TextButton(onClick = onBack) {
            Text(stringResource(id = R.string.profile))
        }
    }
}
