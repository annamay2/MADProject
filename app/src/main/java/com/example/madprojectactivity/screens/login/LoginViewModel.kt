// AI-Assisted (Gemini): Used to verify authentication logic

package com.example.madprojectactivity.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madprojectactivity.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    private val _uiState = MutableStateFlow(
        LoginUiState(
            isLoggedIn = userRepository.isLoggedIn
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, errorMessage = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

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

        // AI Assisted (Gemini): Used to implement launch functionality
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                userRepository.signIn(email, password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = false, errorMessage = e.message ?: "Login failed.")
                }
            }
        }
    }

    fun signUp() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        // AI Assisted (Gemini): Used to implement launch functionality
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                userRepository.signUp(email, password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = false, errorMessage = e.message ?: "Sign up failed.")
                }
            }
        }
    }

    fun logout() {
        userRepository.signOut()
        _uiState.value = LoginUiState(isLoggedIn = false)
    }
}
