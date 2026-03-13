package com.example.madprojectactivity.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madprojectactivity.ui.theme.CardBackgroundLight
import com.example.madprojectactivity.ui.theme.PrimaryPurple
import com.example.madprojectactivity.ui.theme.StatusGreen
import com.example.madprojectactivity.ui.theme.StatusOrange
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
    onUploadReceipt: () -> Unit,
    onViewReceipt: (String) -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.isLoggedIn) {
        if (!state.isLoggedIn) onLoggedOut()
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val displayName = state.userEmail
                ?.substringBefore("@")
                ?.replaceFirstChar { it.uppercase() }
                ?: ""
            Text(
                text = "Welcome, $displayName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Recent Receipts",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )

            Button(
                onClick = onUploadReceipt,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add New Receipt", style = MaterialTheme.typography.titleMedium)
            }

            if (state.receipts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No receipts yet. Add one above!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(state.receipts) { receipt ->
                        ReceiptItem(receipt = receipt, onView = { onViewReceipt(receipt.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptItem(receipt: Receipt, onView: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateStr = receipt.date?.toDate()?.let { sdf.format(it) } ?: "No date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundLight)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status marker
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (receipt.uploadedToRevenue) StatusGreen else StatusOrange,
                        shape = CircleShape
                    )
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = receipt.storeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "€${"%.2f".format(receipt.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = onView,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("View", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
