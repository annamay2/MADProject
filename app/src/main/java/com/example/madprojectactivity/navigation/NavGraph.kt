package com.example.madprojectactivity.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.madprojectactivity.screens.home.HomeScreen
import com.example.madprojectactivity.screens.login.LoginScreen
import com.example.madprojectactivity.screens.receipts.UploadReceiptScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isLoggedIn = com.google.firebase.auth.FirebaseAuth
        .getInstance()
        .currentUser != null

    val startDestination = if (isLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onUploadReceipt = { navController.navigate("uploadReceipt") },
                onLoggedOut = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("uploadReceipt") {
            UploadReceiptScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
