package com.example.madprojectactivity.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
    onUploadReceipt: () -> Unit,
    onLoggedOut: () -> Unit
) {
    android.util.Log.e("LOGIN_SCREEN", "LoginScreen composable is running")

    val state by vm.uiState.collectAsState()

    // If user becomes logged out, bounce them back to login
    LaunchedEffect(state.isLoggedIn) {
        if (!state.isLoggedIn) onLoggedOut()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    TextButton(onClick = vm::logout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome${state.userEmail?.let { ", $it" } ?: ""}",
                style = MaterialTheme.typography.headlineSmall
            )

            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Upload a receipt image and enter the celiac amount manually.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onUploadReceipt,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload Receipt")
                    }
                }
            }

            Button(
                onClick = onUploadReceipt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload Receipt")
            }

            // Placeholder for later: totals summary widget (week/month/year)
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Your totals", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Weekly: (coming soon)")
                    Text("Monthly: (coming soon)")
                    Text("Yearly: (coming soon)")
                }
            }
        }
    }
}
