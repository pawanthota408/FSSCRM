package com.fsscrm.ui.admin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "AdminLeave"

data class LeaveItem(
    val id: Int = 0,
    val employee_id: Int = 0,
    val employee_name: String = "",
    val employee_code: String = "",
    val leave_type: String = "",
    val start_date: String = "",
    val end_date: String = "",
    val days: Int = 0,
    val reason: String? = null,
    val status: String = "pending",
    val admin_remarks: String? = null
)

data class LeaveStats(
    val total: Int = 0,
    val pending: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0,
    val cancelled: Int = 0
)

data class EmpOpt(val id: Int, val name: String, val code: String)
data class LeaveTypeOpt(val name: String, val days: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeaveScreen(
    userId: Int,
    onMenuClick: () -> Unit
) {
    val cal = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedStatus by remember { mutableStateOf("pending") } // default pending like useful admin view
    var searchInput by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf("") }

    var leaves by remember { mutableStateOf<List<LeaveItem>>(emptyList()) }
    var stats by remember { mutableStateOf(LeaveStats()) }
    var employees by remember { mutableStateOf<List<EmpOpt>>(emptyList()) }
    var leaveTypes by remember { mutableStateOf<List<LeaveTypeOpt>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showCreate by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf<LeaveItem?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun parseLeave(o: JsonObject) = LeaveItem(
        id = o.get("id")?.asInt ?: 0,
        employee_id = o.get("employee_id")?.asInt ?: 0,
        employee_name = o.get("employee_name")?.asString ?: "Unknown",
        employee_code = o.get("employee_code")?.asString ?: "",
        leave_type = o.get("leave_type")?.asString ?: "",
        start_date = o.get("start_date")?.asString ?: "",
        end_date = o.get("end_date")?.asString ?: "",
        days = o.get("days")?.asInt ?: 0,
        reason = o.get("reason")?.takeIf { !it.isJsonNull }?.asString,
        status = o.get("status")?.asString ?: "pending",
        admin_remarks = o.get("admin_remarks")?.takeIf { !it.isJsonNull }?.asString
    )

    fun fetch() {
        if (userId <= 0) {
            errorMessage = "user_id required"
            isLoading = false
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminLeaves(
                    mapOf(
                        "user_id" to userId.toString(),
                        "status" to selectedStatus,
                        "month" to selectedMonth.toString(),
                        "year" to selectedYear.toString(),
                        "search" to activeSearch
                    )
                )
                if (response.isSuccessful) {
                    response.toLenientJson()?.asJsonObject?.let { root ->
                        if (root.get("success")?.asBoolean != true) {
                            errorMessage = root.get("error")?.asString ?: "Failed"
                            return@let
                        }
                        leaves = root.getAsJsonArray("leaves")?.map { parseLeave(it.asJsonObject) } ?: emptyList()
                        root.getAsJsonObject("stats")?.let { s ->
                            stats = LeaveStats(
                                total = s.get("total")?.asInt ?: 0,
                                pending = s.get("pending")?.asInt ?: 0,
                                approved = s.get("approved")?.asInt ?: 0,
                                rejected = s.get("rejected")?.asInt ?: 0,
                                cancelled = s.get("cancelled")?.asInt ?: 0
                            )
                        }
                        employees = root.getAsJsonArray("employees")?.map {
                            val e = it.asJsonObject
                            EmpOpt(e.get("id")?.asInt ?: 0, e.get("name")?.asString ?: "", e.get("employee_code")?.asString ?: "")
                        } ?: emptyList()
                        leaveTypes = root.getAsJsonArray("leave_types")?.map {
                            val t = it.asJsonObject
                            LeaveTypeOpt(t.get("name")?.asString ?: "", t.get("default_days_per_year")?.asInt ?: 0)
                        } ?: emptyList()
                    }
                } else errorMessage = "Server ${response.code()}"
            } catch (e: Exception) {
                Log.e(TAG, "fetch", e)
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun updateStatus(leaveId: Int, status: String) {
        scope.launch {
            val res = postLeave(
                mapOf(
                    "user_id" to userId.toString(),
                    "action" to "update_status",
                    "leave_id" to leaveId.toString(),
                    "status" to status.lowercase()
                )
            )
            snackbarHostState.showSnackbar(res.second ?: if (res.first) "Updated" else "Failed")
            if (res.first) fetch()
        }
    }

    LaunchedEffect(selectedMonth, selectedYear, selectedStatus, activeSearch) { fetch() }

    val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    Scaffold(
        topBar = { UniversalHeader(title = "Leave Management", onMenuClick = onMenuClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Leave", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Stats
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LeaveStat("Pending", stats.pending, Color(0xFF92400E), Modifier.weight(1f))
                LeaveStat("Approved", stats.approved, Color(0xFF065F46), Modifier.weight(1f))
                LeaveStat("Rejected", stats.rejected, Color(0xFF991B1B), Modifier.weight(1f))
                LeaveStat("Total", stats.total, Color(0xFF1E3A5F), Modifier.weight(1f))
            }

            // Filters
            Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LeaveDropdownInt(
                            label = monthNames.getOrElse(selectedMonth - 1) { "Month" },
                            options = (1..12).map { it to monthNames[it - 1] },
                            onSelect = { selectedMonth = it },
                            modifier = Modifier.weight(1f)
                        )
                        val years = ((cal.get(Calendar.YEAR) - 2)..cal.get(Calendar.YEAR)).toList()
                        LeaveDropdownInt(
                            label = selectedYear.toString(),
                            options = years.map { it to it.toString() },
                            onSelect = { selectedYear = it },
                            modifier = Modifier.weight(1f)
                        )
                        val statusOpts = listOf(
                            "all" to "All", "pending" to "Pending", "approved" to "Approved",
                            "rejected" to "Rejected", "cancelled" to "Cancelled"
                        )
                        LeaveDropdownStr(
                            label = statusOpts.find { it.first == selectedStatus }?.second ?: "Pending",
                            options = statusOpts,
                            onSelect = { selectedStatus = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Employee / type", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = { activeSearch = searchInput.trim() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                        }
                    }
                }
            }

            Text(
                "${leaves.size} requests · ${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear",
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { fetch() }) { Text("Retry") }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(leaves, key = { it.id }) { leave ->
                        AdminLeaveCard(
                            leave = leave,
                            onApprove = { updateStatus(leave.id, "approved") },
                            onReject = { updateStatus(leave.id, "rejected") },
                            onClick = { showDetail = leave }
                        )
                    }
                    if (leaves.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No leave requests found", color = Color.Gray)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreate) {
        CreateLeaveDialog(
            employees = employees,
            leaveTypes = leaveTypes,
            onDismiss = { showCreate = false },
            onCreate = { empId, type, start, end, reason, status ->
                scope.launch {
                    val res = postLeave(
                        mapOf(
                            "user_id" to userId.toString(),
                            "action" to "create_leave",
                            "employee_id" to empId.toString(),
                            "leave_type" to type,
                            "start_date" to start,
                            "end_date" to end,
                            "reason" to reason,
                            "status" to status
                        )
                    )
                    showCreate = false
                    snackbarHostState.showSnackbar(res.second ?: if (res.first) "Created" else "Failed")
                    if (res.first) fetch()
                }
            }
        )
    }

    showDetail?.let { leave ->
        LeaveDetailDialog(leave = leave, onDismiss = { showDetail = null })
    }
}

private suspend fun postLeave(params: Map<String, String>): Pair<Boolean, String?> {
    return try {
        val resp = RetrofitClient.apiService.adminLeavePost(params)
        if (resp.isSuccessful) {
            val body = resp.toLenientJson()?.asJsonObject
            val ok = body?.get("success")?.asBoolean == true
            Pair(ok, body?.get("message")?.asString ?: body?.get("error")?.asString)
        } else Pair(false, "Server ${resp.code()}")
    } catch (e: Exception) {
        Pair(false, e.message)
    }
}

@Composable
private fun LeaveStat(label: String, value: Int, fg: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = fg)
            Text(label, fontSize = 10.sp, color = Color(0xFF6B7280))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveDropdownInt(
    label: String,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }, modifier) {
        OutlinedTextField(
            value = label, onValueChange = {}, readOnly = true, singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { (k, t) ->
                DropdownMenuItem(text = { Text(t, fontSize = 13.sp) }, onClick = {
                    onSelect(k); expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveDropdownStr(
    label: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }, modifier) {
        OutlinedTextField(
            value = label, onValueChange = {}, readOnly = true, singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { (k, t) ->
                DropdownMenuItem(text = { Text(t, fontSize = 13.sp) }, onClick = {
                    onSelect(k); expanded = false
                })
            }
        }
    }
}

@Composable
fun AdminLeaveCard(
    leave: LeaveItem,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit
) {
    val st = leave.status.lowercase()
    val (bg, fg) = when (st) {
        "approved" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "rejected" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        "cancelled" -> Color(0xFFE5E7EB) to Color(0xFF374151)
        else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(leave.employee_name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(leave.leave_type, fontSize = 12.sp, color = PrimaryIndigo)
                    Text(
                        "${leave.start_date} → ${leave.end_date}" + if (leave.days > 0) " · ${leave.days}d" else "",
                        fontSize = 12.sp, color = Color(0xFF6B7280)
                    )
                }
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        leave.status.replaceFirstChar { it.uppercase() },
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg
                    )
                }
            }
            if (!leave.reason.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Reason: ${leave.reason}", fontSize = 12.sp, color = Color(0xFF4B5563), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (st == "pending") {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) { Text("Reject", fontSize = 12.sp) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) { Text("Approve", fontSize = 12.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateLeaveDialog(
    employees: List<EmpOpt>,
    leaveTypes: List<LeaveTypeOpt>,
    onDismiss: () -> Unit,
    onCreate: (empId: Int, type: String, start: String, end: String, reason: String, status: String) -> Unit
) {
    var empId by remember { mutableIntStateOf(0) }
    var leaveType by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("pending") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.94f)) {
            Column(Modifier.padding(16.dp)) {
                Text("New Leave Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                // Employee
                var eExp by remember { mutableStateOf(false) }
                val eLabel = employees.find { it.id == empId }?.let { "${it.name} (${it.code})" } ?: "Select employee"
                ExposedDropdownMenuBox(eExp, { eExp = !eExp }) {
                    OutlinedTextField(
                        value = eLabel, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(eExp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(eExp, { eExp = false }) {
                        employees.forEach { e ->
                            DropdownMenuItem(text = { Text("${e.name} (${e.code})") }, onClick = {
                                empId = e.id; eExp = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Leave type
                var tExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(tExp, { tExp = !tExp }) {
                    OutlinedTextField(
                        value = leaveType.ifBlank { "Leave type" }, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tExp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(tExp, { tExp = false }) {
                        leaveTypes.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = {
                                leaveType = t.name; tExp = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start, onValueChange = { start = it },
                        label = { Text("Start YYYY-MM-DD") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = end, onValueChange = { end = it },
                        label = { Text("End YYYY-MM-DD") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = empId > 0 && leaveType.isNotBlank() && start.isNotBlank() && end.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        onClick = { onCreate(empId, leaveType, start, end, reason, status) }
                    ) { Text("Create") }
                }
            }
        }
    }
}

@Composable
private fun LeaveDetailDialog(leave: LeaveItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(16.dp)) {
                Text("Leave Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                LRow("Employee", leave.employee_name)
                LRow("Code", leave.employee_code.ifBlank { "—" })
                LRow("Type", leave.leave_type)
                LRow("Start", leave.start_date)
                LRow("End", leave.end_date)
                LRow("Days", if (leave.days > 0) "${leave.days}" else "—")
                LRow("Status", leave.status)
                LRow("Reason", leave.reason ?: "—")
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) { Text("Close") }
            }
        }
    }
}

@Composable
private fun LRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.width(90.dp), fontSize = 12.sp, color = Color(0xFF6B7280))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}