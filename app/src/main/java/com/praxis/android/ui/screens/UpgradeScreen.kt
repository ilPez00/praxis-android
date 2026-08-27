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
fun UpgradeScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Upgrade", style = MaterialTheme.typography.headlineLarge)

        PraxisCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Pro Plan", style = MaterialTheme.typography.titleLarge)
                Text(text = "Unlock AI coaching, advanced analytics, and priority support.", style = MaterialTheme.typography.bodyMedium)
                PraxisButton(onClick = { /* TODO: Stripe checkout */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Subscribe")
                }
            }
        }

        PraxisButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.profile))
        }
    }
}
