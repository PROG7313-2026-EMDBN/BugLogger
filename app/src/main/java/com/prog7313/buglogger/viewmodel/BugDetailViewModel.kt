package com.prog7313.buglogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog7313.buglogger.data.model.Bug
import com.prog7313.buglogger.data.repository.BugRepository
import com.prog7313.buglogger.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BugDetailViewModel : ViewModel() {
    private val repository = BugRepository()

    private val _uiState = MutableStateFlow<UiState<Bug>>(UiState.Idle)
    val uiState: StateFlow<UiState<Bug>> = _uiState.asStateFlow()

    fun loadBug(id: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getBugById(id)
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load bug") }
        }
    }
}