package com.fsscrm.ui.admin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fsscrm.network.Employee
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AdminTasks"

data class AdminTask(
    val id: Int,
    @SerializedName("task_name") val taskName: String,
    val description: String? = null,
    val status: String? = "pending",
    @SerializedName("due_date") val dueDate: String? = null,
    val priority: String? = "low",
    @SerializedName("employee_name") val employeeName: String? = null,
    @SerializedName("department_name") val departmentName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTasksScreen(
    userId: Int,
    onMenuClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sales, 1: IT
    var tasks by remember { mutableStateOf<List<AdminTask>>(emptyList()) }
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddTask by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchData() {
        isLoading = true
        scope.launch {
            try {
                // Fetch All Tasks for Admin
                val response = RetrofitClient.apiService.getAdminQuotes(
                    mapOf("user_id" to userId.toString(), "action" to "get_all_tasks")
                )
                // Note: Reusing getAdminQuotes as a generic admin action handler if needed, 
                // but checking if getTasks works first.
                
                // Let's try getTasks first with admin ID
                val taskResp = RetrofitClient.apiService.getTasks(mapOf("user_id" to userId))
                if (taskResp.isSuccessful) {
                    val json = taskResp.toLenientJson()
                    if (json != null && json.isJsonArray) {
                        tasks = Gson().fromJson(json, object : TypeToken<List<AdminTask>>() {}.type)
                    }
                }

                // Fetch Employees for assignment
                val empResp = RetrofitClient.apiService.getEmployees(mapOf("user_id" to userId))
                if (empResp.isSuccessful) {
                    employees = empResp.body()?.employees ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchData error", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val filteredTasks = remember(tasks, selectedTab) {
        val dept = if (selectedTab == 0) "Sales" else "IT"
        tasks.filter { 
            it.departmentName?.contains(dept, ignoreCase = true) == true ||
            (selectedTab == 0 && it.departmentName == null) // Fallback for Sales
        }
    }

    Scaffold(
        topBar = {
            UniversalHeader(
                title = "Task Management",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { fetchData() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTask = true },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddTask, null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryIndigo
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sales Tasks") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("IT Tasks") }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks) { task ->
                        AdminTaskCard(task)
                    }
                    if (filteredTasks.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No tasks found for this department", color = Color.Gray)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddTask) {
        AddTaskDialog(
            userId = userId,
            department = if (selectedTab == 0) "Sales" else "IT",
            employees = employees,
            onDismiss = { showAddTask = false },
            onTaskAdded = {
                showAddTask = false
                fetchData()
                scope.launch { snackbarHostState.showSnackbar("Task assigned successfully") }
            }
        )
    }
}

@Composable
fun AdminTaskCard(task: AdminTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(task.taskName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Assigned to: ${task.employeeName ?: "Unassigned"}", fontSize = 13.sp, color = Color.Gray)
                }
                val priorityColor = when(task.priority?.lowercase()) {
                    "high" -> Color(0xFFEF4444)
                    "medium" -> Color(0xFFF59E0B)
                    else -> Color(0xFF3B82F6)
                }
                Surface(
                    color = priorityColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = (task.priority ?: "Low").uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }
            }
            
            task.description?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 14.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Due: ${task.dueDate ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                }
                
                val statusColor = if (task.status?.lowercase() == "completed") Color(0xFF10B981) else Color(0xFF6B7280)
                Text(
                    text = (task.status ?: "Pending").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    userId: Int,
    department: String,
    employees: List<Employee>,
    onDismiss: () -> Unit,
    onTaskAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var dueDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var selectedEmpId by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    val filteredEmployees = remember(employees) {
        employees.filter { it.department_name?.contains(department, ignoreCase = true) == true || 
                          it.role?.contains(department, ignoreCase = true) == true }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Assign New ${department} Task", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(12.dp))
                
                Text("Select Priority", fontSize = 12.sp, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(16.dp))
                Text("Assign To", fontSize = 12.sp, color = Color.Gray)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(filteredEmployees.find { it.id == selectedEmpId }?.name ?: "Select Staff")
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        filteredEmployees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp.name ?: "Unknown") },
                                onClick = { selectedEmpId = emp.id ?: 0; expanded = false }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            try {
                                val res = RetrofitClient.apiService.addTask(
                                    mapOf(
                                        "user_id" to userId.toString(),
                                        "employee_id" to selectedEmpId.toString(),
                                        "task_name" to name,
                                        "description" to desc,
                                        "priority" to priority,
                                        "due_date" to dueDate,
                                        "action" to "add_task"
                                    )
                                )
                                if (res.isSuccessful) onTaskAdded()
                            } catch (_: Exception) {}
                            finally { isSubmitting = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    enabled = !isSubmitting && name.isNotBlank() && selectedEmpId != 0
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text("Assign Task")
                }
            }
        }
    }
}
