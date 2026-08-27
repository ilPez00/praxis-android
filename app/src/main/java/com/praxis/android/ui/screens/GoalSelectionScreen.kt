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
import androidx.compose.ui.res.stringResource
import app.praxisweb.xyz.R

@Composable
fun GoalSelectionScreen(onGoalSelected: (String) -> Unit, onBack: () -> Unit) {
    val goalText = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "What are you working on?", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = goalText.value,
            onValueChange = { goalText.value = it },
            label = { Text("Your first goal") },
            modifier = Modifier.fillMaxWidth()
        )

        PraxisButton(onClick = { onGoalSelected(goalText.value) }, modifier = Modifier.fillMaxWidth(), enabled = goalText.value.isNotBlank()) {
            Text("Create Goal")
        }

        PraxisButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.settings))
        }
    }
}
