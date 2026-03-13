package com.example.madprojectactivity.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madprojectactivity.screens.home.HomeViewModel
import com.example.madprojectactivity.ui.theme.CardBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    homeVm: HomeViewModel = viewModel(),
    onLoggedOut: () -> Unit = {}
) {
    val state by homeVm.uiState.collectAsState()
    val totalSpent = state.receipts.sumOf { it.amount }
    val receiptCount = state.receipts.size
    val uploadedCount = state.receipts.count { it.uploadedToRevenue }
    val pendingCount = receiptCount - uploadedCount

    val displayName = state.userEmail
        ?.substringBefore("@")
        ?.replaceFirstChar { it.uppercase() }
        ?: ""

    LaunchedEffect(state.isLoggedIn) {
        if (!state.isLoggedIn) onLoggedOut()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile & Metrics") })
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome, $displayName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Your totals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Weekly: €${"%.2f".format(totalSpent)}")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "My Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total Expenditure", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "€${"%.2f".format(totalSpent)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = "Receipts",
                    value = receiptCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Uploaded",
                    value = uploadedCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            MetricCard(
                label = "Pending Upload",
                value = pendingCount.toString(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { homeVm.logout() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Logout", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
