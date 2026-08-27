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
fun ProfileEditScreen(name: String, bio: String, onSave: (String, String) -> Unit, onBack: () -> Unit) {
    val nameState = remember { mutableStateOf(name) }
    val bioState = remember { mutableStateOf(bio) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Edit Profile", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = nameState.value,
            onValueChange = { nameState.value = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bioState.value,
            onValueChange = { bioState.value = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth()
        )

        PraxisButton(onClick = { onSave(nameState.value, bioState.value) }, modifier = Modifier.fillMaxWidth(), enabled = nameState.value.isNotBlank()) {
            Text("Save")
        }

        PraxisButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.settings))
        }
    }
}
