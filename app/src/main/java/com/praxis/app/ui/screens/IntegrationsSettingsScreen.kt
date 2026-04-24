package com.praxis.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praxis.app.integrations.IntegrationManager
import com.praxis.app.integrations.IntegrationType
import com.praxis.app.integrations.services.*
import kotlinx.coroutines.launch

/**
 * Integrations Settings Screen
 * 
 * Allows users to:
 * - View available integrations
 * - Connect/disconnect external apps
 * - Enable/disable sync
 * - Trigger manual sync
 * - View sync status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var integrations by remember { mutableStateOf(getIntegrationItems(context)) }
    var syncing by remember { mutableStateOf<IntegrationType?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Integrations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Connect your apps to automatically log activities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(integrations) { integration ->
                IntegrationCard(
                    integration = integration,
                    onConnect = { 
                        scope.launch {
                            handleConnect(context, integration, onConnected = {
                                integrations = getIntegrationItems(context)
                            })
                        }
                    },
                    onDisconnect = {
                        scope.launch {
                            IntegrationManager.disableIntegration(integration.type)
                            integrations = getIntegrationItems(context)
                        }
                    },
                    onSync = {
                        scope.launch {
                            syncing = integration.type
                            IntegrationManager.syncIntegration(integration.type)
                            syncing = null
                            integrations = getIntegrationItems(context)
                        }
                    },
                    isSyncing = syncing == integration.type
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            IntegrationManager.syncAllEnabled()
                            integrations = getIntegrationItems(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync All")
                }
            }
        }
    }
}

/**
 * Data class for integration UI items
 */
data class IntegrationItem(
    val type: IntegrationType,
    val name: String,
    val description: String,
    val icon: Int,
    val isConnected: Boolean,
    val lastSync: String?
)

/**
 * Get list of integration items with current status
 */
fun getIntegrationItems(context: Context): List<IntegrationItem> {
    return listOf(
        IntegrationItem(
            type = IntegrationType.HEALTH_CONNECT,
            name = "Health Connect",
            description = "Steps, workouts, sleep, heart rate",
            icon = android.R.drawable.ic_menu_today,
            isConnected = IntegrationManager.isConnected(IntegrationType.HEALTH_CONNECT),
            lastSync = null
        ),
        IntegrationItem(
            type = IntegrationType.STRAVA,
            name = "Strava",
            description = "Running, cycling, hiking",
            icon = android.R.drawable.ic_menu_compass,
            isConnected = IntegrationManager.isConnected(IntegrationType.STRAVA),
            lastSync = null
        ),
        IntegrationItem(
            type = IntegrationType.GOOGLE_CALENDAR,
            name = "Google Calendar",
            description = "Events and appointments",
            icon = android.R.drawable.ic_menu_my_calendar,
            isConnected = IntegrationManager.isConnected(IntegrationType.GOOGLE_CALENDAR),
            lastSync = null
        ),
        IntegrationItem(
            type = IntegrationType.YAZIO,
            name = "Yazio",
            description = "Nutrition and meal tracking",
            icon = android.R.drawable.ic_menu_edit,
            isConnected = IntegrationManager.isConnected(IntegrationType.YAZIO),
            lastSync = null
        ),
        IntegrationItem(
            type = IntegrationType.FITBIT,
            name = "Fitbit",
            description = "Activity, sleep, heart rate",
            icon = android.R.drawable.ic_menu_mylocation,
            isConnected = IntegrationManager.isConnected(IntegrationType.FITBIT),
            lastSync = null
        )
    )
}

/**
 * Card UI for each integration
 */
@Composable
fun IntegrationCard(
    integration: IntegrationItem,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    isSyncing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (integration.isConnected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = integration.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = integration.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (integration.isConnected) {
                    Text(
                        text = if (isSyncing) "Syncing..." else "Connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Actions
            if (integration.isConnected) {
                IconButton(onClick = onSync, enabled = !isSyncing) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Default.Cached else Icons.Default.Sync,
                        contentDescription = "Sync"
                    )
                }
                IconButton(onClick = onDisconnect) {
                    Icon(Icons.Default.Delete, contentDescription = "Disconnect")
                }
            } else {
                Button(onClick = onConnect) {
                    Text("Connect")
                }
            }
        }
    }
}

/**
 * Handle connecting to an integration
 */
suspend fun handleConnect(
    context: Context,
    integration: IntegrationItem,
    onConnected: () -> Unit
) {
    when (integration.type) {
        IntegrationType.HEALTH_CONNECT -> {
            // Request Health Connect permissions
            val service = HealthConnectService(context)
            if (service.initialize()) {
                IntegrationManager.enableIntegration(IntegrationType.HEALTH_CONNECT)
                onConnected()
            }
        }
        IntegrationType.STRAVA -> {
            // Launch Strava OAuth
            val service = StravaService(context)
            service.launchOAuth(context)
            // Will be completed in callback
        }
        IntegrationType.GOOGLE_CALENDAR -> {
            // Launch Google account picker
            // Simplified - would use GoogleAccountCredential.newChooseAccountIntent()
            val service = GoogleCalendarService(context)
            service.setAccount("default") // Would be from picker
            IntegrationManager.enableIntegration(IntegrationType.GOOGLE_CALENDAR)
            onConnected()
        }
        IntegrationType.YAZIO -> {
            // Launch Yazio OAuth
            val authUrl = "https://www.yazio.com/oauth/authorize?client_id=${YazioService.CLIENT_ID}&redirect_uri=${YazioService.REDIRECT_URI}&response_type=code"
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(context, Uri.parse(authUrl))
        }
        IntegrationType.FITBIT -> {
            // Launch Fitbit OAuth
            val authUrl = "https://www.fitbit.com/oauth2/authorize?client_id=${FitbitService.CLIENT_ID}&redirect_uri=${FitbitService.REDIRECT_URI}&response_type=code&scope=activity+sleep+heart_rate"
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(context, Uri.parse(authUrl))
        }
        else -> {}
    }
}
