package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Chat over the REST API. The old native prototype talked to Firestore
 * directly; that is gone — chat is server-backed like the web app, with a
 * short poll keeping an open conversation live and Room serving as the
 * offline-readable cache.
 */
class ChatViewModel(
    private val repository: PraxisRepository,
    private val userId: String
) : ViewModel() {
    /** Current user id, so screens can tell own messages from the partner's. */
    val meId: String = userId
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun loadMessages(partnerId: String) {
        viewModelScope.launch {
            repository.observeCachedMessages(null, userId, partnerId).collect { cached ->
                if (cached.isNotEmpty() && _uiState.value !is ChatUiState.Success) {
                    _uiState.value = ChatUiState.Success(cached)
                }
            }
        }
        viewModelScope.launch {
            val res = repository.getMessages(userId, partnerId)
            _uiState.value = if (res.isSuccess) {
                ChatUiState.Success(res.getOrNull() ?: emptyList())
            } else if (_uiState.value !is ChatUiState.Success) {
                ChatUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load messages")
            } else {
                _uiState.value
            }
        }
    }

    /** Poll while the conversation is on screen; cancelled when it leaves. */
    fun startPolling(partnerId: String, intervalMs: Long = 5_000L) {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(intervalMs)
                val res = repository.getMessages(userId, partnerId)
                if (res.isSuccess) {
                    _uiState.value = ChatUiState.Success(res.getOrNull() ?: emptyList())
                }
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun sendMessage(partnerId: String, content: String) {
        viewModelScope.launch {
            repository.sendMessage(userId, partnerId, content)
            loadMessages(partnerId)
        }
    }
}

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<com.praxis.android.data.model.Message>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
