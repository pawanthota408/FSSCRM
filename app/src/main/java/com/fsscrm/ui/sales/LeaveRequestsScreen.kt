package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.LeaveRequest
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestsScreen(userId: Int, onMenuClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var leaves by remember { mutableStateOf<List<LeaveRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var selectedLeave by remember { mutableStateOf<LeaveRequest?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadLeaves() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getLeaves(mapOf("user_id" to userId))
                if (response.isSuccessful) {
                    val json = response.toLenientJson()
                    if (json != null && json.isJsonArray) {
                        leaves = com.google.gson.Gson().fromJson(
                            json,
                            object : com.google.gson.reflect.TypeToken<List<LeaveRequest>>() {}.type
                        )
                    }
                }
            } catch (e: Exception) { } finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { loadLeaves() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "Leave Management",
                onMenuClick = onMenuClick,
                actions = { 
                    IconButton(onClick = { showApplyDialog = true }) { 
                        Icon(Icons.Default.Add, null, tint = Color.White) 
                    } 
                }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (leaves.isEmpty()) {
                        item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("No leave requests found", color = Color.Gray) } }
                    } else {
                        items(leaves) { leave ->
                            LeaveCard(leave) { selectedLeave = leave }
                        }
                    }
                }
            }
        }
    }

    if (selectedLeave != null) {
        LeaveDetailsDialog(leave = selectedLeave!!, onDismiss = { selectedLeave = null })
    }

    if (showApplyDialog) {
        ApplyLeaveDialog(
            onDismiss = { showApplyDialog = false },
            onConfirm = { type, start, end, reason ->
                scope.launch {
                    try {
                        val res = RetrofitClient.apiService.applyLeave(mapOf(
                            "user_id" to userId.toString(),
                            "leave_type" to type,
                            "start_date" to start,
                            "end_date" to end,
                            "reason" to reason
                        ))
                        handleJsonResponse(
                            response = res,
                            onSuccess = {
                                loadLeaves()
                                showApplyDialog = false
                                snackbarHostState.showSnackbar("Leave application submitted")
                            },
                            onError = { snackbarHostState.showSnackbar(it) }
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun LeaveCard(leave: LeaveRequest, onClick: () -> Unit) {
    val statusColor = when(leave.status) {
        "Approved" -> Color(0xFF10B981)
        "Rejected" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(leave.leave_type, fontWeight = FontWeight.Bold, color = Color.Black)
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(leave.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${leave.start_date} to ${leave.end_date}", fontSize = 13.sp, color = Color.Black)
            Text("${leave.days} Day(s)", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun LeaveDetailsDialog(leave: LeaveRequest, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(leave.leave_type, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SharedDetailRow("Status", leave.status)
                SharedDetailRow("Start Date", leave.start_date)
                SharedDetailRow("End Date", leave.end_date)
                SharedDetailRow("Total Days", "${leave.days} Day(s)")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Reason:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                Text(leave.reason.orEmpty().ifEmpty { "No reason provided" }, fontSize = 14.sp, color = Color.Black)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var type by remember { mutableStateOf("Sick Leave") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    val startPickerState = rememberDatePickerState()
    val endPickerState = rememberDatePickerState()

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let {
                        val selCal = Calendar.getInstance().apply { timeInMillis = it }
                        start = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                            selCal.get(Calendar.YEAR), 
                            selCal.get(Calendar.MONTH) + 1, 
                            selCal.get(Calendar.DAY_OF_MONTH))
                    }
                    showStartPicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let {
                        val selCal = Calendar.getInstance().apply { timeInMillis = it }
                        end = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                            selCal.get(Calendar.YEAR), 
                            selCal.get(Calendar.MONTH) + 1, 
                            selCal.get(Calendar.DAY_OF_MONTH))
                    }
                    showEndPicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply for Leave", color = Color.Black, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val leaveTypes = listOf("Sick Leave", "Casual Leave", "Paid Leave", "Emergency Leave", "Other")
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Leave Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        leaveTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    type = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = start,
                    onValueChange = { },
                    label = { Text("Start Date") },
                    modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true },
                    enabled = false,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray
                    )
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { },
                    label = { Text("End Date") },
                    modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true },
                    enabled = false,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray
                    )
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = { 
            Button(
                onClick = { onConfirm(type, start, end, reason) },
                enabled = start.isNotEmpty() && end.isNotEmpty() && reason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) { 
                Text("Submit") 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = Color.Gray) 
            } 
        }
    )
}
