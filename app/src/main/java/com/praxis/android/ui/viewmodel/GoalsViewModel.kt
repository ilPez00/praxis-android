package com.praxis.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praxis.android.data.repository.PraxisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class GoalTreeItem(
    val node: com.praxis.android.data.model.GoalNode,
    val children: List<GoalTreeItem>,
    val expanded: Boolean = true
)

class GoalsViewModel(private val repository: PraxisRepository, private val userId: String) : ViewModel() {
    private val _uiState = MutableStateFlow<GoalsUiState>(GoalsUiState.Loading)
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCachedGoals(userId).collect { goals ->
                if (goals.isNotEmpty() && _uiState.value is GoalsUiState.Loading) {
                    val tree = buildTree(goals)
                    _uiState.value = GoalsUiState.Success(tree)
                }
            }
        }
        loadGoals()
    }

    fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = GoalsUiState.Loading
            val res = repository.getGoals(userId)
            _uiState.value = if (res.isSuccess) {
                val goals = res.getOrNull() ?: emptyList()
                val tree = buildTree(goals)
                GoalsUiState.Success(tree)
            } else {
                GoalsUiState.Error(res.exceptionOrNull()?.message ?: "Failed to load goals")
            }
        }
    }

    fun createGoal(name: String, description: String? = null, parentId: String? = null, domain: String? = null) {
        viewModelScope.launch {
            val res = repository.createGoal(userId, name, description, parentId, domain)
            if (res.isSuccess) loadGoals()
        }
    }

    fun updateProgress(nodeId: String, progress: Float, note: String? = null) {
        viewModelScope.launch {
            repository.updateGoalProgress(userId, nodeId, progress, note)
            loadGoals()
        }
    }

    fun toggleExpand(item: GoalTreeItem) {
        val newExpanded = !item.expanded
        val newTree = _uiState.value.let { state ->
            if (state is GoalsUiState.Success) {
                GoalsUiState.Success(state.tree.map { if (it.node.id == item.node.id) it.copy(expanded = newExpanded) else it })
            } else state
        }
        _uiState.value = newTree
    }

    private fun buildTree(nodes: List<com.praxis.android.data.model.GoalNode>): List<GoalTreeItem> {
        val nodeMap = nodes.associateBy { it.id }.toMutableMap()
        val roots = mutableListOf<GoalTreeItem>()

        for (node in nodes) {
            if (node.parentId == null || node.parentId.isEmpty()) {
                roots.add(GoalTreeItem(node, emptyList(), true))
            }
        }

        fun getChildren(parentId: String): List<GoalTreeItem> {
            return nodes.filter { it.parentId == parentId }.map { child ->
                GoalTreeItem(child, getChildren(child.id), true)
            }
        }

        return roots.map { it.copy(children = getChildren(it.node.id)) }
    }
}

sealed class GoalsUiState {
    object Loading : GoalsUiState()
    data class Success(val tree: List<GoalTreeItem>) : GoalsUiState()
    data class Error(val message: String) : GoalsUiState()
}
