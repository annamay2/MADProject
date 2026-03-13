@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.madprojectactivity.screens.viewReceipt

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madprojectactivity.ui.theme.AccentBackground
import com.example.madprojectactivity.ui.theme.CardBackgroundLight
import com.example.madprojectactivity.ui.theme.PrimaryPurple
import com.example.madprojectactivity.ui.theme.StatusGreen
import com.example.madprojectactivity.ui.theme.StatusOrange
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ViewReceiptScreen(
    receiptId: String,
    modifier: Modifier = Modifier,
    vm: ViewReceiptViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by vm.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(receiptId) {
        vm.loadReceipt(receiptId)
    }

    // Handle system back button to cancel editing
    BackHandler(enabled = state.isEditing) {
        vm.cancelEditing()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Receipt") },
            text = { Text("Are you sure you want to delete this receipt? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteReceipt(onDeleted = onBack)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Receipt" else "Receipt Details") },
                navigationIcon = {
                    IconButton(onClick = if (state.isEditing) vm::cancelEditing else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                        } else {
                            TextButton(onClick = vm::saveChanges) {
                                Text("Save", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else if (state.receipt != null) {
                        IconButton(onClick = vm::startEditing) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            } else {
                state.receipt?.let { receipt ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        val context = LocalContext.current

                        if (state.isEditing) {
                            OutlinedTextField(
                                value = state.editStoreName,
                                onValueChange = vm::onEditStoreNameChange,
                                label = { Text("Store Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.editAmount,
                                onValueChange = vm::onEditAmountChange,
                                label = { Text("Amount") },
                                prefix = { Text("€") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uploaded to Revenue", style = MaterialTheme.typography.bodyLarge)
                                Checkbox(
                                    checked = state.editUploadedToRevenue,
                                    onCheckedChange = vm::onEditUploadedToRevenueChange
                                )
                            }
                        } else {
                            // Amount Circle
                            Surface(
                                shape = CircleShape,
                                color = AccentBackground,
                                modifier = Modifier.size(120.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "€${"%.2f".format(receipt.amount)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }

                            // Details Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackgroundLight)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    DetailRow("Store", receipt.storeName)
                                    
                                    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                                    val dateStr = receipt.date?.toDate()?.let { sdf.format(it) } ?: "Unknown"
                                    DetailRow("Date", dateStr)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Revenue Status", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (receipt.uploadedToRevenue) "Uploaded" else "Pending",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (receipt.uploadedToRevenue) StatusGreen else StatusOrange
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Switch(
                                                checked = receipt.uploadedToRevenue,
                                                onCheckedChange = { vm.toggleUploadedToRevenue() },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = StatusGreen,
                                                    uncheckedThumbColor = Color.White,
                                                    uncheckedTrackColor = Color.LightGray
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Revenue.ie quick-link
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.ros.ie/myaccount-web/sign_in.html")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "File on Revenue.ie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
