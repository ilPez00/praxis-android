package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R
import com.praxis.android.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onLogout: () -> Unit, onNavigateToCheckIn: () -> Unit = {}) {
    val state = viewModel.uiState.value

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            is com.praxis.android.ui.viewmodel.ProfileUiState.Loading -> {
                CircularProgressIndicator()
            }
            is com.praxis.android.ui.viewmodel.ProfileUiState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
            is com.praxis.android.ui.viewmodel.ProfileUiState.Success -> {
                val profile = state.profile
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(bottom = 8.dp))
                Text(text = profile.name, style = MaterialTheme.typography.headlineLarge)
                Text(text = profile.email, style = MaterialTheme.typography.bodyMedium)
                if (!profile.bio.isNullOrBlank()) {
                    Text(text = profile.bio, style = MaterialTheme.typography.bodyMedium)
                }
                PraxisCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(id = R.string.streak_value, profile.streak), style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Points: ${profile.praxisPoints}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Reliability: ${(profile.reliabilityScore * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                        if (profile.isPremium) {
                            Text(text = "Premium", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                PraxisButton(onClick = onNavigateToCheckIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.check_in_label))
                }
                PraxisButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.logout))
                }
            }
        }
    }
}
