package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R
import com.praxis.android.ui.settings.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onOpenScreenTime: (() -> Unit)? = null, onOpenContacts: (() -> Unit)? = null) {
    val state = viewModel.uiState.value
    val settingsState = viewModel.settings.collectAsState(initial = com.praxis.android.ui.settings.SettingsState()).value
    val settings = settingsState

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(id = R.string.settings), style = MaterialTheme.typography.headlineLarge)

        when (state) {
            is com.praxis.android.ui.settings.SettingsUiState.Loading -> {
                Text(text = "Loading settings...")
            }
            is com.praxis.android.ui.settings.SettingsUiState.Success -> {
                Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)
                SettingRow(title = "Messages", checked = settings.messagesEnabled, onCheckedChange = { viewModel.setMessagesEnabled(it) })
                SettingRow(title = "Matches", checked = settings.matchesEnabled, onCheckedChange = { viewModel.setMatchesEnabled(it) })
                SettingRow(title = "All Notifications", checked = settings.notificationsEnabled, onCheckedChange = { viewModel.setNotificationsEnabled(it) })

                Text(text = "Privacy", style = MaterialTheme.typography.titleMedium)
                PraxisButton(onClick = { viewModel.setPrivacyMode(if (settings.privacyMode == "public") "friends" else "public") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Profile Visibility: ${settings.privacyMode.replaceFirstChar { it.uppercase() }}")
                }

                Text(text = "Security", style = MaterialTheme.typography.titleMedium)
                SettingRow(title = "Biometric Login", checked = settings.biometricEnabled, onCheckedChange = { viewModel.setBiometricEnabled(it) })
                SettingRow(title = "Analytics", checked = settings.analyticsEnabled, onCheckedChange = { viewModel.setAnalyticsEnabled(it) })
                SettingRow(title = "Share phone usage", checked = settings.sharePhoneUsageEnabled, onCheckedChange = { viewModel.setSharePhoneUsageEnabled(it) })

                if (onOpenScreenTime != null) {
                    Text(text = "Screen time", style = MaterialTheme.typography.titleMedium)
                    PraxisButton(onClick = onOpenScreenTime, modifier = Modifier.fillMaxWidth()) {
                        Text("Social media minutes & detox proof")
                    }
                }

                if (onOpenContacts != null) {
                    Text(text = "Sharing", style = MaterialTheme.typography.titleMedium)
                    PraxisButton(onClick = onOpenContacts, modifier = Modifier.fillMaxWidth()) {
                        Text("Invite contacts")
                    }
                }

                PraxisButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Export Data")
                }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete Account")
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(id = R.string.profile))
        }
    }
}

@Composable
fun SettingRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
