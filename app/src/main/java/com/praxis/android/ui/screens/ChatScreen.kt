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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.praxisweb.xyz.R
import com.praxis.android.data.model.Message
import com.praxis.android.ui.viewmodel.ChatViewModel
import com.praxis.android.ui.viewmodel.ChatUiState

@Composable
fun ChatScreen(viewModel: ChatViewModel, partnerId: String, partnerName: String) {
    val state = viewModel.uiState.value
    val messageText = remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state) {
        if (state is ChatUiState.Success) {
            listState.animateScrollToItem(state.messages.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is ChatUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            is ChatUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ChatUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages) { message: Message ->
                        // Own messages sit on the right with the amber-tinted
                        // container; the partner's on the left on raised gray.
                        val isMe = message.senderId == viewModel.meId
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(0.75f),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                    com.praxis.android.ui.components.design.PraxisButton(onClick = {
                        if (messageText.value.isNotBlank()) {
                            viewModel.sendMessage(partnerId, messageText.value)
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
