package com.example.madprojectactivity.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madprojectactivity.screens.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    homeVm: HomeViewModel = viewModel()
) {
    val state by homeVm.uiState.collectAsState()
    val totalSpent = state.receipts.sumOf { it.amount }
    val receiptCount = state.receipts.size
    val uploadedCount = state.receipts.count { it.uploadedToRevenue }
    val pendingCount = receiptCount - uploadedCount

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile & Metrics") })
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "My Statistics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total Expenditure", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "€${"%.2f".format(totalSpent)}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

            MetricCard(
                label = "Pending Upload",
                value = pendingCount.toString(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.weight(1f))
            
            Text(
                text = "Logged in as: ${state.userEmail ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
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
