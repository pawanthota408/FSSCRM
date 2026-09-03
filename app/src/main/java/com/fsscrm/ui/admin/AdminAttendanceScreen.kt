package com.fsscrm.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.AdminDashboardResponse
import com.fsscrm.network.EmployeeAttendance
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.EmptyStateCard
import com.fsscrm.ui.common.StatBox
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceScreen(
    userId: Int,
    onBack: () -> Unit,
) {
    var dashboardData by remember { mutableStateOf<AdminDashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Filters
    var selectedPeriod by remember { mutableStateOf("today") }
    var selectedDept by remember { mutableStateOf("All Departments") }
    var searchInput by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    fun fetchData() {
        if (userId <= 0) return
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                // Standardization of range keys to match backend expected format
                val rangeKey = when(selectedPeriod.lowercase()) {
                    "today" -> "today"
                    "yesterday" -> "yesterday"
                    "this week" -> "this_week"
                    "last week" -> "last_week"
                    "this month" -> "this_month"
                    else -> selectedPeriod.lowercase()
                }

                val response = RetrofitClient.apiService.getAdminDashboardData(
                    mapOf(
                        "user_id" to userId.toString(),
                        "range" to rangeKey,
                        "action" to "get_admin_dashboard",
                        "search" to activeSearch,
                    )
                )
                if (response.isSuccessful) {
                    response.toLenientJson()?.let { jsonBody ->
                        dashboardData = AdminDashboardResponse.fromJson(jsonBody)
                    }
                } else { errorMessage = "Server Error ${response.code()}" }
            } catch (e: Exception) { errorMessage = e.message }
            finally { isLoading = false }
        }
    }

    LaunchedEffect(selectedPeriod, activeSearch) { fetchData() }

    val stats = dashboardData?.stats
    val allAttendance = dashboardData?.employees_attendance ?: emptyList()
    
    val filteredAttendance = remember(allAttendance, selectedDept) {
        allAttendance.filter { emp ->
            (selectedDept == "All Departments") ||
                    (emp.department_name ?: emp.role ?: "").equals(selectedDept, ignoreCase = true)
        }
    }

    val isSearchMode = activeSearch.isNotBlank()

    val (displayTotal, displayPresent, displayLeave, displayAbsent) = if (isSearchMode) {
        val total = filteredAttendance.size
        val present = filteredAttendance.count { it.status.equals("Present", true) }
        val leave = filteredAttendance.count { it.status?.lowercase()?.contains("leave") == true }
        val calculated = (total - present - leave).coerceAtLeast(0)
        listOf(total, present, leave, calculated)
    } else {
        stats?.let { s ->
            val total = s.total_employees
            val present = s.present_today
            val leave = s.leave_today
            val absent = s.absent_today
            val calculated = if (absent == 0 && total > (present + leave)) total - present - leave else absent
            listOf(total, present, leave, calculated.coerceAtLeast(0))
        } ?: listOf(0, 0, 0, 0)
    }

    val periodLabel = when (selectedPeriod) {
        "today" -> "Today"
        "yesterday" -> "Yesterday"
        "this_week" -> "This Week"
        "last_week" -> "Last Week"
        "this_month" -> "This Month"
        else -> selectedPeriod.replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        topBar = { 
            UniversalHeader(
                title = "Team Attendance", 
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { fetchData() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                }
            ) 
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filter Bar
            Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttDropdown(
                            label = "Period: $periodLabel",
                            options = listOf(
                                "today" to "Today", "yesterday" to "Yesterday",
                                "this_week" to "This Week", "last_week" to "Last Week", "this_month" to "This Month"
                            ),
                            onSelect = { selectedPeriod = it },
                            modifier = Modifier.weight(1f)
                        )
                        AttDropdown(
                            label = if (selectedDept == "All Departments") "All Depts" else selectedDept,
                            options = (listOf("All Departments") + (dashboardData?.departments?.mapNotNull { it.name } ?: emptyList()).distinct().sorted()).map { it to it },
                            onSelect = { selectedDept = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Enter employee name...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            trailingIcon = {
                                if (searchInput.isNotEmpty()) {
                                    IconButton(onClick = { searchInput = ""; activeSearch = ""; fetchData() }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = Color(0xFFE2E8F0))
                        )
                        Button(
                            onClick = { activeSearch = searchInput.trim() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(22.dp))
                        }
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryIndigo) }
            } else if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = Color.Gray)
                        Button(onClick = { fetchData() }) { Text("Retry") }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatBox(label = if(isSearchMode) "Total Records" else "Total Staff", value = displayTotal.toString(), icon = Icons.Default.Groups, gradient = listOf(Color(0xFF6366F1), Color(0xFF818CF8)), modifier = Modifier.weight(1f))
                            StatBox(label = "Present", value = displayPresent.toString(), icon = Icons.Default.HowToReg, gradient = listOf(Color(0xFF10B981), Color(0xFF34D399)), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatBox(label = "On Leave", value = displayLeave.toString(), icon = Icons.Default.PersonOff, gradient = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)), modifier = Modifier.weight(1f))
                            StatBox(label = "Absent", value = displayAbsent.toString(), icon = Icons.Default.Badge, gradient = listOf(Color(0xFFEF4444), Color(0xFFF87171)), modifier = Modifier.weight(1f))
                        }
                    }

                    item {
                        Text(
                            text = if (isSearchMode) "History for: $activeSearch" else "Employee Status · $periodLabel",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF111827)
                        )
                        Text(text = "${filteredAttendance.size} records listed", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }

                    if (filteredAttendance.isEmpty()) {
                        item { EmptyStateCard("No attendance found for this selection") }
                    } else {
                        items(filteredAttendance) { emp ->
                            if (isSearchMode) AttendanceHistoryRow(emp) else AttendanceCardWithTime(emp)
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttDropdown(label: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(exp, { exp = it }, modifier) {
        OutlinedTextField(
            value = label, onValueChange = {}, readOnly = true, singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = PrimaryIndigo)
        )
        ExposedDropdownMenu(exp, { exp = false }) {
            options.forEach { (k, t) ->
                DropdownMenuItem(text = { Text(t, fontSize = 12.sp) }, onClick = { onSelect(k); exp = false })
            }
        }
    }
}

@Composable
fun AttendanceHistoryRow(emp: EmployeeAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(extractDate(emp.check_in ?: emp.check_out ?: "N/A"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("In: ${formatTime(emp.check_in)}", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Medium)
                    Text("Out: ${formatTime(emp.check_out)}", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                }
            }
            StatusBadge(emp.status ?: "Absent")
        }
    }
}

@Composable
fun AttendanceCardWithTime(emp: EmployeeAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(emp.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(emp.department_name ?: emp.role ?: "Employee", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("In: ${formatTime(emp.check_in)}", fontSize = 11.sp, color = Color(0xFF059669))
                    Text("Out: ${formatTime(emp.check_out)}", fontSize = 11.sp, color = Color(0xFFDC2626))
                }
            }
            StatusBadge(emp.status ?: "Absent")
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val isPresent = status.equals("Present", true)
    val isLeave = status.lowercase().contains("leave")
    val bgColor = when {
        isPresent -> Color(0xFFDCFCE7)
        isLeave -> Color(0xFFFEF3C7)
        else -> Color(0xFFFEE2E2)
    }
    val textColor = when {
        isPresent -> Color(0xFF166534)
        isLeave -> Color(0xFF92400E)
        else -> Color(0xFF991B1B)
    }
    Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
        Text(status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

private fun formatTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "--:--"
    return try {
        val timePart = if (raw.contains(" ")) raw.split(" ").last() else raw
        if (timePart.length >= 5) timePart.substring(0, 5) else timePart
    } catch (_: Exception) { "--:--" }
}

private fun extractDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "N/A"
    return try {
        if (raw.contains(" ")) raw.split(" ").first() else raw
    } catch (_: Exception) { raw }
}
