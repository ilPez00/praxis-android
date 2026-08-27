package com.praxis.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.praxisweb.xyz.R
import com.praxis.android.data.model.Message

@Composable
fun GroupChatScreen(roomId: String, roomName: String, viewModel: com.praxis.android.ui.viewmodel.ChatViewModel) {
    val state = viewModel.uiState.value
    val messageText = remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state) {
        if (state is com.praxis.android.ui.viewmodel.ChatUiState.Success) {
            listState.animateScrollToItem(state.messages.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is com.praxis.android.ui.viewmodel.ChatUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            is com.praxis.android.ui.viewmodel.ChatUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is com.praxis.android.ui.viewmodel.ChatUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages) { message: Message ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = message.createdAt.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = messageText.value,
                        onValueChange = { messageText.value = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        if (messageText.value.isNotBlank()) {
                            viewModel.sendMessage(roomId, messageText.value)
                            messageText.value = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Send")
                    }
                }
            }
        }
    }
}
