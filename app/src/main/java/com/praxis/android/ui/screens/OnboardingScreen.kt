package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.praxisweb.xyz.R

@Composable
fun OnboardingScreen(onComplete: (String, String) -> Unit) {
    val name = remember { mutableStateOf("") }
    val goal = remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to Praxis", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("What's your name?") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = goal.value,
            onValueChange = { goal.value = it },
            label = { Text("What are you working on?") },
            modifier = Modifier.fillMaxWidth()
        )

        PraxisButton(onClick = { onComplete(name.value, goal.value) }, modifier = Modifier.fillMaxWidth(), enabled = name.value.isNotBlank() && goal.value.isNotBlank()) {
            Text("Get Started")
        }
    }
}
