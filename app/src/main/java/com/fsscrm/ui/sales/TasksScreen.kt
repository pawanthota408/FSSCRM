package com.fsscrm.ui.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fsscrm.network.ActivityLog
import com.fsscrm.network.AppTask
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.CorpAmber
import com.fsscrm.ui.theme.CorpBlue
import com.fsscrm.ui.theme.CorpDark
import com.fsscrm.ui.theme.CorpEmerald
import com.fsscrm.ui.theme.CorpSurface
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(userId: Int, onMenuClick: () -> Unit, navController: NavController) {
    var tasks by remember { mutableStateOf<List<AppTask>>(emptyList()) }
    var activities by remember { mutableStateOf<List<ActivityLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Assigned Tasks", "Recent Activities")

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchData() {
        isLoading = true
        scope.launch {
            try {
                // Fetch Tasks
                val taskResponse = RetrofitClient.apiService.getTasks(mapOf("user_id" to userId))
                if (taskResponse.isSuccessful) {
                    val json = taskResponse.toLenientJson()
                    if (json != null) {
                        tasks = if (json.isJsonArray) {
                            Gson().fromJson(json, object : TypeToken<List<AppTask>>() {}.type)
                        } else emptyList()
                    }
                }

                // Fetch Activities
                val activityResponse = RetrofitClient.apiService.getActivities(mapOf("user_id" to userId))
                if (activityResponse.isSuccessful) {
                    val json = activityResponse.toLenientJson()
                    if (json != null) {
                        activities = if (json.isJsonArray) {
                            Gson().fromJson(json, object : TypeToken<List<ActivityLog>>() {}.type)
                        } else emptyList()
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CorpSurface
    ) { padding: PaddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "Workspace",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { fetchData() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                }
            )
            
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = CorpBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            ) 
                        },
                        selectedContentColor = CorpBlue,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            if (isLoading && tasks.isEmpty() && activities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CorpBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTab == 0) {
                        if (tasks.isEmpty() && !isLoading) {
                            item { 
                                EmptyStateViewItem("No tasks assigned yet", Icons.Default.AssignmentLate)
                            }
                        } else {
                            items(tasks) { task: AppTask ->
                                EnhancedTaskCard(task)
                            }
                        }
                    } else {
                        if (activities.isEmpty() && !isLoading) {
                            item { 
                                EmptyStateViewItem("No activities logged yet", Icons.Default.History)
                            }
                        } else {
                            items(activities) { activityLog: ActivityLog ->
                                EnhancedActivityCard(activityLog) {
                                    navController.navigate("activity_details/${activityLog.id}")
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun LazyItemScope.EmptyStateViewItem(message: String, icon: ImageVector) {
    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { 
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.Gray, fontWeight = FontWeight.Medium) 
        }
    }
}

@Composable
fun EnhancedTaskCard(task: AppTask) {
    val priority = task.priority?.lowercase() ?: "low"
    val priorityColor = when(priority) {
        "high" -> Color(0xFFEF4444)
        "medium" -> CorpAmber
        else -> CorpBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(priorityColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = priorityColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = priority.uppercase(), color = priorityColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(text = "Due: ${formatTaskDate(task.due_date)}", fontSize = 11.sp, color = Color.Gray)
                }
                
                val status = task.status ?: "Pending"
                val statusColor = if(status.lowercase() == "completed") CorpEmerald else CorpBlue
                
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(text = status.uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = task.task_name, fontWeight = FontWeight.Black, color = CorpDark, fontSize = 18.sp)
            task.description?.let { 
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = it, fontSize = 13.sp, color = Color(0xFF475569), modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
fun EnhancedActivityCard(activity: ActivityLog, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(40.dp).background(CorpBlue.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = when (activity.activity_type.lowercase()) { "call" -> Icons.Default.Call; "meeting" -> Icons.Default.Groups; else -> Icons.Default.History }, contentDescription = null, tint = CorpBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = activity.activity_type.uppercase(), color = CorpBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = formatTaskDate(activity.created_at), fontSize = 11.sp, color = Color.Gray)
                }
                val statusColor = if (activity.status?.lowercase() == "completed") CorpEmerald else Color(0xFFF59E0B)
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(text = (activity.status ?: "PENDING").uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = activity.company_name ?: activity.client_name ?: "Unknown Company", fontWeight = FontWeight.Black, color = CorpDark, fontSize = 18.sp)
            activity.description?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = it, fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

private fun formatTaskDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "N/A"
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        output.format(input.parse(dateStr)!!)
    } catch (_: Exception) { 
        try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val output = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            output.format(input.parse(dateStr)!!)
        } catch (_: Exception) { dateStr }
    }
}
