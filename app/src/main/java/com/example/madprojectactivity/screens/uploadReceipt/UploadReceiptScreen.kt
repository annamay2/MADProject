// AI-generated (Claude):  added image thumbnail preview via Coil,
// receives captured URI from camera screen.
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.madprojectactivity.screens.receipts

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.madprojectactivity.ui.theme.AccentBackground
import com.example.madprojectactivity.ui.theme.BorderAccent
import com.example.madprojectactivity.ui.theme.CardBackground
import com.example.madprojectactivity.ui.theme.IconTint
import com.example.madprojectactivity.ui.theme.PrimaryPurple
import com.example.madprojectactivity.ui.theme.SubtitleText
import com.example.madprojectactivity.ui.theme.UnderlineColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun UploadReceiptScreen(
    modifier: Modifier = Modifier,
    vm: UploadReceiptViewModel = viewModel(),
    onBack: () -> Unit,
    onDone: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    capturedImageUri: Uri? = null
) {

    val state by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    // Update ViewModel when we receive an image URI from the camera
    LaunchedEffect(capturedImageUri) {
        if (capturedImageUri != null) {
            vm.onImageUriChange(capturedImageUri)
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Take Photo pill
            Surface(
                color = AccentBackground,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Take Photo",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.dp, BorderAccent, CircleShape)
                        ) {
                            IconButton(onClick = onOpenCamera) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = "Take photo",
                                    tint = IconTint
                                )
                            }
                        }
                    }

                    // Show captured image thumbnail
                    val displayUri = state.imageUri
                    if (displayUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(displayUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Captured receipt",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .padding(bottom = 12.dp)
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Date card
            Surface(
                color = CardBackground,
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
                            color = SubtitleText
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = state.date.format(formatter),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit date", tint = IconTint)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            FilledUnderlineField(
                label = "Amount",
                value = state.amount,
                onValueChange = vm::onAmountChange,
                prefix = "€",
                keyboardType = KeyboardType.Decimal,
                cardBg = CardBackground,
                placeholder = "eg. 19.99"
            )

            Spacer(Modifier.height(10.dp))

            FilledUnderlineField(
                label = "Store Name",
                value = state.storeName,
                onValueChange = vm::onStoreNameChange,
                keyboardType = KeyboardType.Text,
                cardBg = CardBackground,
                showClear = true,
                placeholder = "eg. Aldi"
            )

            Spacer(Modifier.height(10.dp))

            FilledUnderlineField(
                label = "Gluten-Free Items",
                value = state.glutenFreeItems,
                onValueChange = vm::onGlutenFreeItemsChange,
                keyboardType = KeyboardType.Text,
                cardBg = CardBackground,
                showClear = true,
                placeholder = "eg. Bread, pasta"
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uploaded to Revenue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubtitleText,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = state.uploadedToRevenue,
                        onCheckedChange = vm::onUploadedToRevenueChange
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.saveReceipt() },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
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

            // Add extra space at the bottom to ensure the button is clear of the navbar
            Spacer(Modifier.height(20.dp))
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
    val underline = UnderlineColor

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
                color = SubtitleText
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
                            tint = IconTint
                        )
                    }
                }
            }
        }
    }
}
