package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val repository: PraxisRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            val res = repository.getMyTrackers()
            _uiState.value = if (res.isSuccess) {
                AnalyticsUiState.Success(res.getOrNull() ?: emptyList())
            } else {
                AnalyticsUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load analytics")
            }
        }
    }
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(val trackers: List<com.praxis.android.data.model.Tracker>) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}
