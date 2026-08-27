package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.model.CreateEntryRequest
import com.praxis.android.data.model.NotebookEntriesResponse
import com.praxis.android.data.model.NotebookStatsResponse
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotebookViewModel(private val repository: PraxisRepository, private val userId: String) : ViewModel() {
    private val _uiState = MutableStateFlow<NotebookUiState>(NotebookUiState.Loading)
    val uiState: StateFlow<NotebookUiState> = _uiState

    init {
        loadEntries()
        loadStats()
    }

    fun loadEntries(entryType: String? = null, domain: String? = null, tag: String? = null, search: String? = null) {
        viewModelScope.launch {
            _uiState.value = NotebookUiState.Loading
            val res = repository.getNotebookEntries(userId, entryType = entryType, domain = domain, tag = tag, search = search)
            _uiState.value = if (res.isSuccess) {
                val data = res.getOrNull()!!
                NotebookUiState.Success(data.entries, data.total)
            } else {
                NotebookUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load entries")
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            val res = repository.getNotebookStats(userId)
            if (res.isSuccess) {
                val stats = res.getOrNull()!!
                _uiState.value = (_uiState.value as? NotebookUiState.Success)?.copy(stats = stats) ?: NotebookUiState.Success(emptyList(), 0, stats)
            }
        }
    }

    fun createEntry(content: String, entryType: String? = null, domain: String? = null, tags: List<String>? = null) {
        viewModelScope.launch {
            val res = repository.createNotebookEntry(CreateEntryRequest(content, entryType, domain, tags))
            if (res.isSuccess) {
                loadEntries()
            }
        }
    }

    /** Free-note analysis (Pro feature; 403 from the server for free users). */
    suspend fun aiScan(content: String): Result<Map<String, Any>> = repository.aiScanNote(content)
}

sealed class NotebookUiState {
    object Loading : NotebookUiState()
    data class Success(val entries: List<com.praxis.android.data.model.NotebookEntry>, val total: Int, val stats: com.praxis.android.data.model.NotebookStatsResponse? = null) : NotebookUiState()
    data class Error(val message: String) : NotebookUiState()
}
