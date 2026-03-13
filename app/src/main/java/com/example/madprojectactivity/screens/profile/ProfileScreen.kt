package com.example.madprojectactivity.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.madprojectactivity.ui.theme.PrimaryPurple
import com.example.madprojectactivity.ui.theme.StatusGreen
import com.example.madprojectactivity.ui.theme.StatusOrange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    homeVm: HomeViewModel = viewModel(),
    onLoggedOut: () -> Unit = {}
) {
    val state by homeVm.uiState.collectAsState()
    val receipts = state.receipts

    val totalSpent = receipts.sumOf { it.amount }
    val receiptCount = receipts.size
    val uploadedCount = receipts.count { it.uploadedToRevenue }
    val pendingCount = receiptCount - uploadedCount
    val avgAmount = if (receiptCount > 0) totalSpent / receiptCount else 0.0

    // Most-visited store
    val topStore = receipts
        .groupBy { it.storeName }
        .maxByOrNull { it.value.size }
        ?.let { "${it.key} (${it.value.size})" }
        ?: "—"

    // This month's spend
    val cal = Calendar.getInstance()
    val currentMonth = cal.get(Calendar.MONTH)
    val currentYear = cal.get(Calendar.YEAR)
    val monthlySpend = receipts.filter { r ->
        r.date?.toDate()?.let { d ->
            val c = Calendar.getInstance().apply { time = d }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        } ?: false
    }.sumOf { it.amount }

    // Month label
    val monthLabel = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())

    val displayName = state.userEmail
        ?.substringBefore("@")
        ?.replaceFirstChar { it.uppercase() }
        ?: ""

    LaunchedEffect(state.isLoggedIn) {
        if (!state.isLoggedIn) onLoggedOut()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome, $displayName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Total spend highlight
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Expenditure", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "€${"%.2f".format(totalSpent)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Receipt counts row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = "Receipts",
                    value = receiptCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Uploaded",
                    value = uploadedCount.toString(),
                    valueColor = StatusGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Pending",
                    value = pendingCount.toString(),
                    valueColor = StatusOrange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Additional metrics row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = "Avg. Receipt",
                    value = "€${"%.2f".format(avgAmount)}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "$monthLabel Spend",
                    value = "€${"%.2f".format(monthlySpend)}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Top store card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top Store", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(topStore, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))

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
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
