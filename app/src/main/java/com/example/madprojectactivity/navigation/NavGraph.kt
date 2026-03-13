package com.example.madprojectactivity.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.madprojectactivity.screens.home.HomeScreen
import com.example.madprojectactivity.screens.login.LoginScreen
import com.example.madprojectactivity.screens.profile.ProfileScreen
import com.example.madprojectactivity.screens.receipts.UploadReceiptScreen
import com.example.madprojectactivity.screens.viewReceipt.ViewReceiptScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Upload : Screen("uploadReceipt", "Add", Icons.Default.Add)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val startDestination = if (isLoggedIn) Screen.Home.route else "login"

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Upload,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            val route = currentDestination?.route
            if (route != null && route != "login" && !route.startsWith("viewReceipt")) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onUploadReceipt = { navController.navigate(Screen.Upload.route) },
                    onViewReceipt = { id -> navController.navigate("viewReceipt/$id") },
                    onLoggedOut = {
                        navController.navigate("login") {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Upload.route) {
                UploadReceiptScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen()
            }

            composable(
                route = "viewReceipt/{receiptId}",
                arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
            ) { backStackEntry ->
                val receiptId = backStackEntry.arguments?.getString("receiptId") ?: ""
                ViewReceiptScreen(
                    receiptId = receiptId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
