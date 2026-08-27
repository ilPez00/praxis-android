package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.praxisweb.xyz.R
import androidx.compose.ui.res.stringResource

@Composable
fun ConnectPraxisScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Connect Praxis to an AI", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Let Claude / ChatGPT / Grok / Gemini act as you — read your charge, goals, journal & check in on your behalf.", style = MaterialTheme.typography.bodyMedium)

        PraxisCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "1 · Create a connection key", style = MaterialTheme.typography.titleMedium)
                PraxisButton(onClick = { /* TODO: mint PAT */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate key")
                }
                Text(text = "Shown once. It acts as you — store it like a password. Revoke any time in Settings → Integrations.", style = MaterialTheme.typography.bodySmall)
            }
        }

        PraxisCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "2 · Hosted server (easiest)", style = MaterialTheme.typography.titleMedium)
                Text(text = "Set PRAXIS_PAT = your key on the praxis-mcp server, then add this URL to your AI as a custom MCP connector:", style = MaterialTheme.typography.bodySmall)
                Text(text = "https://praxis-mcp-production.up.railway.app", style = MaterialTheme.typography.bodyMedium)
            }
        }

        PraxisCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Local (Claude Desktop)", style = MaterialTheme.typography.titleMedium)
                Text(text = "Build physis-mcp (npm i && npm run build), then paste into claude_desktop_config.json:", style = MaterialTheme.typography.bodySmall)
                Text(text = "PRAXIS_API_URL: https://web-production-646a4.up.railway.app/api", style = MaterialTheme.typography.bodyMedium)
            }
        }

        PraxisButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.profile))
        }
    }
}
