package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FailsViewModel(private val repository: PraxisRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<FailsUiState>(FailsUiState.Loading)
    val uiState: StateFlow<FailsUiState> = _uiState

    init {
        loadFails()
    }

    fun loadFails() {
        viewModelScope.launch {
            val failsRes = repository.getFails()
            val statsRes = repository.getFailsStats()
            _uiState.value = if (failsRes.isSuccess && statsRes.isSuccess) {
                FailsUiState.Success(failsRes.getOrNull() ?: emptyList(), statsRes.getOrNull() ?: emptyMap())
            } else {
                FailsUiState.Error(failsRes.exceptionOrNull()?.message ?: "Failed to load fails")
            }
        }
    }
}

sealed class FailsUiState {
    object Loading : FailsUiState()
    data class Success(val fails: List<Map<String, Any>>, val stats: Map<String, Any>) : FailsUiState()
    data class Error(val message: String) : FailsUiState()
}
