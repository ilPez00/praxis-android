package com.praxis.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.auth.AuthManager
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PraxisRepository, private val context: android.content.Context) : ViewModel() {
    val repo: PraxisRepository = repository
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch {
            val userId = AuthManager.getUserId(context)
            if (userId != null) {
                _uiState.value = UiState.Authenticated(userId)
            } else {
                _uiState.value = UiState.Unauthenticated
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val res = repository.login(email, password)
            if (res.isSuccess) {
                _uiState.value = UiState.Authenticated(AuthManager.getUserId(context) ?: "")
            } else {
                _uiState.value = UiState.Error(res.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        AuthManager.logout(context)
        // Credential AND widget snapshot go together: a home-screen widget
        // showing the previous account's data after sign-out is a privacy
        // failure sitting in plain sight.
        app.praxisweb.xyz.WidgetStore.get(context).clear()
        _uiState.value = UiState.Unauthenticated
    }
}

sealed class UiState {
    object Loading : UiState()
    object Unauthenticated : UiState()
    data class Authenticated(val userId: String) : UiState()
    data class Error(val message: String) : UiState()
}
