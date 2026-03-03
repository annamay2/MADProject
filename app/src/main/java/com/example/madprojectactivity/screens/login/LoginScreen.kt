package com.example.madprojectactivity.screens.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    vm: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    android.util.Log.e("LOGIN_SCREEN", "LoginScreen composable is running")

    val state by vm.uiState.collectAsState()

    // Local UI toggle: login vs signup
    var isSignUpMode by rememberSaveable { mutableStateOf(false) }

    // Navigate once logged in
    LaunchedEffect(state.isLoggedIn) {
        android.util.Log.d("LoginScreen", "isLoggedIn changed -> ${state.isLoggedIn}")
        if (state.isLoggedIn) {
            Log.e("LOGIN_SCREEN", "Login success -> navigating to Home")
            onLoginSuccess()

        }
    }

    val title = if (isSignUpMode) "Create account" else "Login"
    val primaryButtonText = if (isSignUpMode) "Sign up" else "Login"
    val loadingText = if (isSignUpMode) "Creating account…" else "Logging in…"
    val toggleText = if (isSignUpMode) "Already have an account? Log in"
    else "No account yet? Sign up"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { if (isSignUpMode) vm.signUp() else vm.login() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 10.dp)
                )
                Text(loadingText)
            } else {
                Text(primaryButtonText)
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                isSignUpMode = !isSignUpMode
                vm.clearError()
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(toggleText)
        }
    }
}
