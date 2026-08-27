package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: PraxisRepository, private val userId: String) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCachedProfile(userId).collect { cached ->
                if (cached != null && _uiState.value is ProfileUiState.Loading) {
                    _uiState.value = ProfileUiState.Success(cached)
                }
            }
        }
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val res = repository.getUserProfile(userId)
            _uiState.value = if (res.isSuccess) {
                ProfileUiState.Success(res.getOrNull()!!)
            } else {
                ProfileUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load profile")
            }
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: com.praxis.android.data.model.UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
