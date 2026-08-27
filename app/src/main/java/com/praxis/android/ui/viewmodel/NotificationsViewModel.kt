package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(private val repository: PraxisRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            val res = repository.getNotifications()
            _uiState.value = if (res.isSuccess) {
                NotificationsUiState.Success(res.getOrNull() ?: emptyList())
            } else {
                NotificationsUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load notifications")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead()
            loadNotifications()
        }
    }
}

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(val notifications: List<com.praxis.android.data.model.Notification>) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}
