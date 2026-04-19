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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddBugViewModel : ViewModel() {
    private val repository = BugRepository()

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun saveBug(
        title: String,
        description: String,
        severity: String,
        reportedBy: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val request = Bug(
                title = title,
                description = description,
                severity = severity,
                reportedBy = reportedBy,
                createdAt = getDate(),
                isResolved = false
            )

            repository.createBug(request)
                .onSuccess {
                    _uiState.value = UiState.Success(Unit)
                    onSuccess()
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Failed to save bug")
                }
        }
    }

    private fun getDate(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}