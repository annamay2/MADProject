package com.example.madprojectactivity.screens.home

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HomeUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null
)

class HomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoggedIn = auth.currentUser != null,
            userEmail = auth.currentUser?.email
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    private val listener = FirebaseAuth.AuthStateListener { a ->
        _uiState.value = HomeUiState(
            isLoggedIn = a.currentUser != null,
            userEmail = a.currentUser?.email
        )
    }

    init {
        auth.addAuthStateListener(listener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(listener)
        super.onCleared()
    }

    fun logout() = auth.signOut()
}
