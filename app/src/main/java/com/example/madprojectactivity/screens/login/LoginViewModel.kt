package com.example.madprojectactivity.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, errorMessage = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, errorMessage = null) }
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required.") }
            return
        }
        if (!email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(1200)

            val success = (email == "test@example.com" && password == "password")

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = success,
                    errorMessage = if (success) null else "Invalid credentials."
                )
            }
        }
    }

    fun logout() {
        _uiState.value = LoginUiState()
    }
}