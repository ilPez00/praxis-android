package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupsViewModel(private val repository: PraxisRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<GroupsUiState>(GroupsUiState.Loading)
    val uiState: StateFlow<GroupsUiState> = _uiState

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = GroupsUiState.Loading
            val res = repository.getGroups()
            _uiState.value = if (res.isSuccess) {
                GroupsUiState.Success(res.getOrNull() ?: emptyList())
            } else {
                GroupsUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load groups")
            }
        }
    }
}

sealed class GroupsUiState {
    object Loading : GroupsUiState()
    data class Success(val groups: List<com.praxis.android.data.model.Group>) : GroupsUiState()
    data class Error(val message: String) : GroupsUiState()
}
