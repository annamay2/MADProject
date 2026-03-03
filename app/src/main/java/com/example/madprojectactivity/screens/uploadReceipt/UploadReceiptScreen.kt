@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.madprojectactivity.screens.receipts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun UploadReceiptScreen(
    modifier: Modifier = Modifier,
    vm: UploadReceiptViewModel = viewModel(),
    onBack: () -> Unit,
    onDone: () -> Unit = {},
    onUploadImage: () -> Unit = {}
) {
    val state by vm.uiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    // Colors
    val pageBg = Color(0xFFFFFFFF)
    val cardBg = Color(0xFFEFEAF4)
    val uploadBg = Color(0xFFEDE4FF)
    val primaryPurple = Color(0xFF6E58B5)

    // Handle success/error messages or navigation
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            onDone()
            vm.clearMessages()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        vm.onDateChange(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("New Receipt") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Spacer(Modifier.height(28.dp))

        // Upload Image pill
        Surface(
            color = uploadBg,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Upload Image",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(34.dp)
                        .border(1.dp, Color(0xFFE0D7F2), CircleShape)
                ) {
                    IconButton(onClick = onUploadImage) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF4D4D4D))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Date card
        Surface(
            color = cardBg,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = state.date.format(formatter),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit date", tint = Color(0xFF4D4D4D))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        FilledUnderlineField(
            label = "Amount",
            value = state.amount,
            onValueChange = vm::onAmountChange,
            prefix = "€",
            keyboardType = KeyboardType.Decimal,
            cardBg = cardBg,
            placeholder = "eg. 19.99"
        )

        Spacer(Modifier.height(14.dp))

        FilledUnderlineField(
            label = "Store Name",
            value = state.storeName,
            onValueChange = vm::onStoreNameChange,
            keyboardType = KeyboardType.Text,
            cardBg = cardBg,
            showClear = true,
            placeholder = "eg. Aldi"
        )

        Spacer(Modifier.height(14.dp))

        FilledUnderlineField(
            label = "Gluten-Free Items",
            value = state.glutenFreeItems,
            onValueChange = vm::onGlutenFreeItemsChange,
            keyboardType = KeyboardType.Text,
            cardBg = cardBg,
            showClear = true,
            placeholder = "eg. Bread, pasta"
        )

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Uploaded to Revenue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Checkbox(
                checked = state.uploadedToRevenue,
                onCheckedChange = vm::onUploadedToRevenueChange
            )
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = { vm.saveReceipt() },
            enabled = !state.isSaving,
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryPurple),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FilledUnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    cardBg: Color,
    prefix: String? = null,
    showClear: Boolean = false,
    placeholder: String? = null
) {
    val underline = Color(0xFF3F3F3F)

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF555555)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prefix != null) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(4.dp))
                }

                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        if (placeholder != null) {
                            Text(
                                text = placeholder,
                                color = Color.Gray
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = underline,
                        unfocusedIndicatorColor = underline,
                        cursorColor = underline
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                )

                if (showClear && value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Clear",
                            tint = Color(0xFF4D4D4D)
                        )
                    }
                }
            }
        }
    }
}
