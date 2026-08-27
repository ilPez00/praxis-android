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
import com.praxis.android.data.model.NotebookEntry
import androidx.compose.ui.res.stringResource

@Composable
fun NotebookEntryEditDialog(entry: NotebookEntry? = null, onDismiss: () -> Unit, onSave: (String, String?) -> Unit) {
    val content = remember { mutableStateOf(entry?.content ?: "") }
    val domain = remember { mutableStateOf(entry?.domain ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = if (entry == null) "New Entry" else "Edit Entry", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = content.value,
            onValueChange = { content.value = it },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = domain.value,
            onValueChange = { domain.value = it },
            label = { Text("Domain") },
            modifier = Modifier.fillMaxWidth()
        )

        PraxisButton(onClick = { onSave(content.value, if (domain.value.isBlank()) null else domain.value) }, modifier = Modifier.fillMaxWidth(), enabled = content.value.isNotBlank()) {
            Text("Save")
        }

        PraxisButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
