package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fsscrm.R
import com.fsscrm.network.ActivityLog
import com.fsscrm.network.AppNotification
import com.fsscrm.network.AttendanceStatus
import com.fsscrm.network.CustomerLicense
import com.fsscrm.network.DashboardResponse
import com.fsscrm.network.DashboardStats
import com.fsscrm.network.FollowUp
import com.fsscrm.network.FollowUpResponse
import com.fsscrm.network.Lead
import com.fsscrm.network.RetrofitClient
import com.fsscrm.network.SessionManager
import com.fsscrm.network.Task
import com.fsscrm.ui.common.Screen
import com.fsscrm.ui.common.handleJsonResponse
import com.fsscrm.ui.common.toLenientJson
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun DashboardScreen(userId: Int, onLogout: () -> Unit, onMenuClick: () -> Unit, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var dashboardData by remember { mutableStateOf<DashboardResponse?>(null) }
    var todayFollowUps by remember { mutableStateOf<List<FollowUp>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isFetchingLocation by remember { mutableStateOf(false) }

    fun refreshData() {
        scope.launch {
            try {
                isLoading = dashboardData == null
                val response = RetrofitClient.apiService.getDashboardData(mapOf("user_id" to userId))
                if (response.isSuccessful) {
                    response.toLenientJson()?.let {
                        dashboardData = DashboardResponse.fromJson(it)
                    }
                }
                
                val followResp = RetrofitClient.apiService.getTodayFollowUps(mapOf("user_id" to userId))
                if (followResp.isSuccessful) {
                    followResp.toLenientJson()?.let { json ->
                        val allFu = FollowUpResponse.fromJson(json).followups
                        todayFollowUps = allFu.filter { it.status.lowercase() != "won" }
                    }
                }

                val notifResp = RetrofitClient.apiService.getNotifications(mapOf("user_id" to userId))
                if (notifResp.isSuccessful) {
                    notifResp.toLenientJson()?.let { json ->
                        if (json.isJsonArray) {
                            notifications = com.google.gson.Gson().fromJson(json, object : com.google.gson.reflect.TypeToken<List<AppNotification>>() {}.type)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Refresh error", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
        while(true) {
            delay(60000)
            refreshData()
        }
    }

    // --- MANDATORY FOLLOW-UP GATE ---
    val now = Calendar.getInstance().time
    val pendingFollowUp = remember(todayFollowUps, now) {
        todayFollowUps.firstOrNull { follow ->
            val status = follow.status.lowercase()
            if (status == "completed" || status == "won" || status == "success") return@firstOrNull false
            try {
                val dateStr = follow.follow_up_date
                val timeStr = follow.follow_up_time ?: "00:00"
                
                // If dateStr already contains a full timestamp, use it as is
                val combined = if (dateStr.contains(":") && dateStr.contains(" ")) dateStr else "$dateStr $timeStr"
                
                val formats = listOf(
                    "yyyy-MM-dd hh:mm a", 
                    "yyyy-MM-dd HH:mm:ss", 
                    "yyyy-MM-dd HH:mm",
                    "yyyy-MM-dd",
                    "dd-MM-yyyy hh:mm a",
                    "dd-MM-yyyy HH:mm:ss",
                    "dd-MM-yyyy HH:mm"
                )
                
                var schedDate: Date? = null
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.US) // Use US for parsing technical strings
                        sdf.isLenient = false
                        schedDate = sdf.parse(combined)
                        if (schedDate != null) break
                    } catch (e: Exception) { }
                }
                
                schedDate != null && schedDate.before(now)
            } catch (e: Exception) { 
                Log.e("DateFormat", "Error parsing follow up date: ${follow.follow_up_date}", e)
                false 
            }
        }
    }

    if (pendingFollowUp != null) {
        var newStatus by remember { mutableStateOf("") }
        var showStatusDropdown by remember { mutableStateOf(false) }
        val followupStatuses = listOf("Completed", "Call Back Later", "Not Answering", "Switched Off", "Not Interested")

        Dialog(onDismissRequest = { }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PriorityHigh, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Follow-up Overdue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your follow-up with ${pendingFollowUp.lead_name} at ${pendingFollowUp.follow_up_time} is pending. Please update its status to continue.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showStatusDropdown = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(if (newStatus.isEmpty()) "SELECT STATUS" else newStatus)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }, modifier = Modifier.fillMaxWidth(0.7f)) {
                            followupStatuses.forEach { status ->
                                DropdownMenuItem(text = { Text(status) }, onClick = { newStatus = status; showStatusDropdown = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (newStatus.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("Please select a status first") }
                                return@Button
                            }
                            
                            // MAPPING: Convert human label to technical backend status
                            val technicalStatus = when(newStatus) {
                                "Completed" -> "completed"
                                "Not Interested" -> "cancelled"
                                else -> "rescheduled" // Call Back Later, Not Answering, etc.
                            }

                            scope.launch {
                                try {
                                    val resp = RetrofitClient.apiService.updateFollowUpStatus(mapOf(
                                        "user_id" to userId.toString(),
                                        "follow_up_id" to pendingFollowUp.id.toString(),
                                        "lead_id" to pendingFollowUp.lead_id.toString(),
                                        "status" to technicalStatus,
                                        "remarks" to newStatus, // Send the specific reason in remarks
                                        "action" to "complete_followup"
                                    ))
                                    handleJsonResponse(response = resp, onSuccess = { refreshData() }, onError = { snackbarHostState.showSnackbar(it) })
                                } catch (e: Exception) { }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CorpEmerald),
                        shape = RoundedCornerShape(12.dp),
                        enabled = newStatus.isNotEmpty()
                    ) { Text("UPDATE & CONTINUE", fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { navController.navigate("lead_details/${pendingFollowUp.lead_id}") }, modifier = Modifier.fillMaxWidth()) {
                        Text("VIEW LEAD DETAILS", color = CorpBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CorpSurface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AiAssistant.route) },
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        }
    ) { padding -> 
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "FSS CRM",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        val unread = notifications.count { !it.is_read }
                        if (unread > 0) {
                            BadgedBox(badge = { Badge(containerColor = Color.Red) { Text(unread.toString()) } }) {
                                Icon(Icons.Outlined.Notifications, null, tint = Color.White)
                            }
                        } else {
                            Icon(Icons.Outlined.Notifications, null, tint = Color.White)
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    val displayName = dashboardData?.profile?.name ?: dashboardData?.rootName ?: sessionManager.getUserName()
                    val profilePic = dashboardData?.profile?.profile_image
                    DashboardWelcomeSection(name = displayName, profilePic = profilePic)
                }

                item {
                    AttendanceBanner(
                        attendance = dashboardData?.getEffectiveAttendance(),
                        isFetchingLocation = isFetchingLocation,
                        onToggle = { action ->
                            scope.launch {
                                isFetchingLocation = true
                                try {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                                            .addOnSuccessListener { loc ->
                                                scope.launch {
                                                    try {
                                                        val response = RetrofitClient.apiService.markAttendance(mapOf(
                                                            "user_id" to userId.toString(),
                                                            "action" to action,
                                                            "latitude" to (loc?.latitude?.toString() ?: "0.0"),
                                                            "longitude" to (loc?.longitude?.toString() ?: "0.0"),
                                                            "location" to "FSS Executive App"
                                                        ))
                                                        handleJsonResponse(response = response, onSuccess = { refreshData() }, onError = { snackbarHostState.showSnackbar(it) })
                                                    } finally { isFetchingLocation = false }
                                                }
                                            }
                                    } else {
                                        snackbarHostState.showSnackbar("Location permission required")
                                        isFetchingLocation = false
                                    }
                                } catch (e: Exception) { isFetchingLocation = false }
                            }
                        }
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(title = "Overview", actionText = "Today")
                        Spacer(modifier = Modifier.height(16.dp))
                        KPIGrid(dashboardData?.getEffectiveStats())
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem("Add Lead", Icons.Default.PersonAdd, CorpBlue) { navController.navigate(Screen.CreateLead.route) }
                        QuickActionItem("Customers", Icons.Default.Groups, CorpEmerald) { navController.navigate(Screen.Customers.route) }
                        QuickActionItem("Attendance", Icons.Default.CalendarMonth, CorpAmber) { navController.navigate(Screen.Attendance.route) }
                        QuickActionItem("Tasks", Icons.Default.Assignment, Color(0xFF8B5CF6)) { navController.navigate(Screen.Tasks.route) }
                        QuickActionItem("Payroll", Icons.Default.Payments, Color(0xFFEC4899)) { navController.navigate(Screen.Payroll.route) }
                    }
                }

                // Expiries
                val expiring = dashboardData?.expiringLicenses ?: emptyList()
                if (expiring.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) {
                            SectionHeader(title = "Critical Expiries", icon = Icons.Default.Timer)
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 16.dp)) {
                                items(expiring) { lic ->
                                    ExpiringCard(lic) { navController.navigate("license_details/${lic.license_key}") }
                                }
                            }
                        }
                    }
                }

                // Today's Schedule
                val filteredFollowUps = todayFollowUps.filter { it.status.lowercase() != "completed" && it.status.lowercase() != "won" }
                if (filteredFollowUps.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) {
                            SectionHeader(title = "Today's Schedule", actionText = "View all")
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 16.dp)) {
                                items(filteredFollowUps) { follow ->
                                    ScheduleCardNew(follow) { navController.navigate("lead_details/${follow.lead_id}") }
                                }
                            }
                        }
                    }
                }

                // Recent Activity
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(title = "Recent Activity", actionText = "View all")
                        Spacer(modifier = Modifier.height(12.dp))
                        val activities = dashboardData?.activities ?: emptyList()
                        if (activities.isEmpty()) {
                            Text("No recent activities", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            activities.take(3).forEach { activity ->
                                ActivityItemNew(activity) { navController.navigate("activity_details/${activity.id}") }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                // Two-column bottom section
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            RecentLeadsCard(dashboardData?.recentLeads, navController)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PriorityTasksCard(dashboardData?.tasks, navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardWelcomeSection(name: String, profilePic: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Welcome,", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text("Here's your performance overview for today.", color = Color.Gray, fontSize = 12.sp)
            }
            AsyncImage(
                model = profilePic ?: com.fsscrm.R.drawable.hand,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(com.fsscrm.R.drawable.hand)
            )
        }
    }
}

@Composable
fun AttendanceBanner(attendance: AttendanceStatus?, isFetchingLocation: Boolean, onToggle: (String) -> Unit) {
    val isActuallyIN = attendance?.isCheckedIn == true && attendance.isCheckedOut != true
    val statusText = if(isActuallyIN) "ACTIVE DUTY" else "OUT OF OFFICE"
    
    val bgBrush = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF3B82F6)))
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.background(bgBrush).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if(isActuallyIN) CorpEmerald else Color.Red, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusText, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    val dateStr = remember(locale) { SimpleDateFormat("EEE, MMM dd", locale).format(Date()) }
                    Text(dateStr, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Check-in", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(formatAttendanceTime(attendance?.checkInTime, locale), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Box(contentAlignment = Alignment.Center) {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Surface(
                                modifier = Modifier.size(60.dp).clickable { onToggle(if(isActuallyIN) "check_out" else "check_in") },
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 4.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.Login, null, tint = CorpBlue, modifier = Modifier.size(20.dp))
                                    Text(if(isActuallyIN) "END" else "START", fontSize = 9.sp, fontWeight = FontWeight.Black, color = CorpBlue)
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Check-out", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(formatAttendanceTime(attendance?.checkOutTime, locale), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun KPIGrid(stats: DashboardStats?) {
    val s = stats ?: DashboardStats()
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KPICard("Total Leads", s.totalLeads.toString(), "+ 12%", Icons.Default.Group, CorpBlue, modifier = Modifier.weight(1f))
        KPICard("Converted", s.wonDeals.toString(), "+ 8%", Icons.Default.TrendingUp, CorpEmerald, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KPICard("Revenue", "₹${String.format(locale, "%,.0f", s.revenue)}", "+ 15%", Icons.Default.AccountBalanceWallet, CorpAmber, modifier = Modifier.weight(1f))
        KPICard("Pending", s.pendingTasks.toString(), "- 3%", Icons.Default.HourglassEmpty, Color(0xFFF43F5E), modifier = Modifier.weight(1f))
    }
}

@Composable
fun KPICard(label: String, value: String, trend: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorpDark)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    if(trend.startsWith("+")) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, 
                    null, 
                    tint = if(trend.startsWith("+")) Color(0xFF059669) else Color(0xFFDC2626), 
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    trend, 
                    fontSize = 10.sp, 
                    color = if(trend.startsWith("+")) Color(0xFF059669) else Color(0xFFDC2626), 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(48.dp).clickable { onClick() }, 
            shape = RoundedCornerShape(12.dp), 
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector? = null, actionText: String? = null, onActionClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = CorpBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorpDark)
        Spacer(modifier = Modifier.weight(1f))
        if (actionText != null) {
            TextButton(onClick = onActionClick, contentPadding = PaddingValues(0.dp)) {
                Text(actionText, color = CorpBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExpiringCard(lic: CustomerLicense, onClick: () -> Unit) {
    Card(modifier = Modifier.width(220.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("EXPIRING SOON", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(lic.item_name ?: "Product", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(lic.license_key ?: "", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(lic.expiry_date ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScheduleCardNew(follow: FollowUp, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(4.dp)) {
                    Text("MEETING", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF4F46E5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(follow.follow_up_time ?: "", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(follow.lead_name ?: "Unknown Lead", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(follow.remarks ?: "Product Discussion", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
fun ActivityItemNew(activity: ActivityLog, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(Color(0xFFF0FDF4), CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when(activity.activity_type.lowercase()) {
                    "call" -> Icons.Default.Call
                    "meeting" -> Icons.Default.Groups
                    else -> Icons.Default.History
                },
                contentDescription = null, 
                tint = CorpEmerald, 
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // PROMINENT COMPANY NAME
            Text(activity.company_name ?: activity.client_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(activity.activity_type, fontSize = 12.sp, color = Color.Gray)
        }
        val locale = LocalConfiguration.current.locales[0]
        Text(formatAttendanceTime(activity.date, locale).take(6), fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun RecentLeadsCard(leads: List<Lead>?, navController: NavController) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonSearch, null, tint = CorpBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recent Leads", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text((leads?.size ?: 0).toString(), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("New leads this week", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { navController.navigate(Screen.Leads.route) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF))) {
                Text("See Leads", color = CorpBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PriorityTasksCard(tasks: List<Task>?, navController: NavController) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AssignmentTurnedIn, null, tint = CorpAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Priority Tasks", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text((tasks?.size ?: 0).toString(), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Tasks pending", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { navController.navigate(Screen.Tasks.route) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED))) {
                Text("See Tasks", color = CorpAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun formatAttendanceTime(time: String?, locale: Locale): String {
    if (time.isNullOrEmpty() || time == "00:00:00" || time == "null") return "--:--"
    if (time.uppercase().contains("AM") || time.uppercase().contains("PM")) return time
    return try {
        if (time.contains(" ")) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = inputFormat.parse(time)
            val outputFormat = SimpleDateFormat("hh:mm a", locale)
            outputFormat.format(date!!)
        } else if (time.contains(":")) {
            // Likely just HH:mm:ss or HH:mm
            val inputFormat = if (time.count { it == ':' } == 2) 
                SimpleDateFormat("HH:mm:ss", Locale.US) 
            else 
                SimpleDateFormat("HH:mm", Locale.US)
            
            val date = inputFormat.parse(time)
            val outputFormat = SimpleDateFormat("hh:mm a", locale)
            outputFormat.format(date!!)
        } else time
    } catch (e: Exception) { 
        Log.e("DateFormat", "Error formatting time: $time", e)
        time 
    }
}
