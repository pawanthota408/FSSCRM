package com.fsscrm.ui.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fsscrm.MainActivity
import com.fsscrm.R
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.admin.*
import com.fsscrm.ui.ai.AiAgentScreen
import com.fsscrm.ui.it.ITAppUpdatesScreen
import com.fsscrm.ui.it.ITDashboardScreen
import com.fsscrm.ui.sales.*
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

/**
 * Universal notification helper for FSS CRM.
 */
fun showLocalNotification(context: Context, title: String, message: String, navigateTo: String? = null, extraId: String? = null) {
    val channelId = "fsscrm_notifications"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "FSS CRM Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Channel for FSS CRM Lead and Task alerts"
            enableLights(true)
            lightColor = android.graphics.Color.BLUE
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (navigateTo != null) putExtra("navigate_to", navigateTo)
        if (extraId != null) putExtra("extra_id", extraId)
    }
    
    val pendingIntent = PendingIntent.getActivity(
        context, System.currentTimeMillis().toInt(), intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.hand)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(Notification.DEFAULT_ALL)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
        .setColor(0xFF4F46E5.toInt())

    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Dashboard)
    object Attendance : Screen("attendance", "Attendance", Icons.Default.CalendarToday)
    object Leads : Screen("leads", "Leads", Icons.Default.PersonAdd)
    object LeadDetails : Screen("lead_details/{leadId}", "Lead Details", Icons.Default.Person)
    object Tasks : Screen("tasks", "Tasks", Icons.AutoMirrored.Filled.Assignment)
    object LeaveRequests : Screen("leaves", "Leave Requests", Icons.AutoMirrored.Filled.ExitToApp)
    object Payroll : Screen("payroll", "Payroll", Icons.Default.Payments)
    object Announcements : Screen("announcements", "Announcements", Icons.AutoMirrored.Filled.Announcement)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object CreateLead : Screen("create_lead?phone={phone}&notes={notes}", "Create Lead", Icons.Default.AddCircle)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object ActivityDetails : Screen("activity_details/{activityId}", "Activity Details", Icons.Default.History)
    object Activities : Screen("activities", "Activities", Icons.Default.History)
    object Notifications : Screen("notifications_history", "Notifications", Icons.Default.Notifications)
    object Customers : Screen("customers", "Customers", Icons.Default.Groups)
    object CustomerDetails : Screen("customer_details/{customerId}", "Customer Details", Icons.Default.Person)
    object LicenseDetails : Screen("license_details/{licenseKey}", "License Details", Icons.Default.VpnKey)
    object WorkDetails : Screen("work_details/{workId}", "Work Details", Icons.AutoMirrored.Filled.PlaylistAddCheck)
    object AdminAttendance : Screen("admin_attendance", "Team Attendance", Icons.Default.HowToReg)
    
    // Admin Sections
    object AdminLeads : Screen("admin_leads", "All Leads", Icons.Default.PersonAdd)
    object AdminContacts : Screen("admin_contacts", "Contacts", Icons.Default.ContactPhone)
    object AdminCustomers : Screen("admin_customers", "Customers", Icons.Default.Groups)
    object AdminFollowUps : Screen("admin_follow_ups", "Follow Ups", Icons.Default.History)
    object AdminReports : Screen("admin_crm_reports", "CRM Reports", Icons.Default.BarChart)
    object AdminTallySales : Screen("admin_tally_sales", "Tally Sales", Icons.Default.PointOfSale)
    object AdminInvoices : Screen("admin_invoices", "Invoices", Icons.Default.Description)
    object AdminQuotes : Screen("admin_quotes", "Quotes", Icons.Default.Assignment)
    object AdminPayments : Screen("admin_payments", "Payments", Icons.Default.Payments)
    object AdminTasks : Screen("admin_tasks", "Tasks", Icons.Default.AssignmentTurnedIn)
    object AdminSalesReports : Screen("admin_sales_reports", "Sales Reports", Icons.Default.PieChart)
    object AdminProjects : Screen("admin_projects", "Projects", Icons.Default.AccountTree)
    object AdminTimesheets : Screen("admin_timesheets", "Timesheet", Icons.Default.Schedule)
    object AdminEmployees : Screen("admin_employees", "Employees", Icons.Default.People)
    object AdminLeave : Screen("admin_leave", "Leave Management", Icons.Default.ExitToApp)
    object AdminPayroll : Screen("admin_payroll", "Payroll", Icons.Default.MonetizationOn)
    object AdminUsers : Screen("admin_users", "Users", Icons.Default.ManageAccounts)
    object AdminRoles : Screen("admin_roles", "Roles & Permissions", Icons.Default.Security)
    object AdminSettings : Screen("admin_settings", "Company Settings", Icons.Default.Business)
    
    // IT Department
    object ITDashboard : Screen("it_dashboard", "IT Dashboard", Icons.Default.Terminal)
    object ITServerStatus : Screen("it_server", "Server Status", Icons.Default.Storage)
    object ITAppUpdates : Screen("it_updates", "App Updates", Icons.Default.SystemUpdate)
    object ITSupport : Screen("it_support", "Support Tickets", Icons.Default.SupportAgent)
    object AiAssistant : Screen("ai_assistant", "AI Assistant", Icons.Default.AutoAwesome)
}

data class DrawerItem(val title: String, val icon: ImageVector, val route: String = "", val section: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    userId: Int, 
    userRole: String,
    userPosition: String = "",
    userDept: Int,
    userDeptName: String = "",
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    initialNavigateTo: String? = null,
    initialExtraId: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(initialNavigateTo, initialExtraId) {
        val navTo = initialNavigateTo ?: return@LaunchedEffect
        val extraId = initialExtraId ?: ""
        when (navTo) {
            "create_lead" -> {
                if (extraId.contains("=") || extraId.contains("&")) {
                    navController.navigate("create_lead?$extraId") { launchSingleTop = true }
                } else {
                    navController.navigate("create_lead?phone=$extraId") { launchSingleTop = true }
                }
            }
            "lead_details" -> {
                if (extraId.isNotEmpty()) {
                    val route = if (extraId.contains("?")) "lead_details/$extraId" else "lead_details/$extraId"
                    navController.navigate(route) { launchSingleTop = true }
                }
            }
            "customer_details" -> if (extraId.isNotEmpty()) navController.navigate("customer_details/$extraId") { launchSingleTop = true }
            "tasks" -> navController.navigate(Screen.Tasks.route) { launchSingleTop = true }
            "leads" -> navController.navigate(Screen.Leads.route) { launchSingleTop = true }
            "followups" -> navController.navigate(Screen.Home.route) { launchSingleTop = true }
            "attendance" -> navController.navigate(Screen.Attendance.route) { launchSingleTop = true }
            "leaves" -> navController.navigate(Screen.LeaveRequests.route) { launchSingleTop = true }
            "announcements" -> navController.navigate(Screen.Announcements.route) { launchSingleTop = true }
            "activities" -> navController.navigate(Screen.Activities.route) { launchSingleTop = true }
            "work_details" -> if (extraId.isNotEmpty()) navController.navigate("work_details/$extraId") { launchSingleTop = true }
            "ai_assistant" -> navController.navigate(Screen.AiAssistant.route) { launchSingleTop = true }
            "home" -> navController.navigate(Screen.Home.route) { launchSingleTop = true }
        }
        onDeepLinkConsumed()
    }
    
    val isAdmin = userRole.lowercase() == "admin"
    var currentITStatus by remember { 
        mutableStateOf(
            userRole.lowercase().contains("it") || userRole.lowercase().contains("developer") || userRole.lowercase().contains("software") ||
            userPosition.lowercase().contains("it") || userPosition.lowercase().contains("developer") || userPosition.lowercase().contains("software") ||
            userDeptName.lowercase().contains("it") || userDeptName.lowercase().contains("tech") || userDeptName.lowercase().contains("software") ||
            userDept == 5 || userDept == 4
        )
    }
    val isIT = currentITStatus

    LaunchedEffect(userId) {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        while(true) {
            try {
                val response = RetrofitClient.apiService.getDashboardData(mapOf("user_id" to userId))
                if (response.isSuccessful) {
                    response.toLenientJson()?.let { json ->
                        val data = com.fsscrm.network.DashboardResponse.fromJson(json)
                        val stats = data.getEffectiveStats()
                        prefs.edit().apply {
                            putInt("last_lead_count", stats.totalLeads)
                            putInt("last_task_count", stats.totalTasks)
                            data.profile?.let { p ->
                                p.position?.let { putString("user_position", it) }
                                p.department_name?.let { putString("user_dept_name", it) }
                                p.department_id?.let { putInt("user_dept", it) }
                                val isActuallyIT = (p.position ?: "").lowercase().contains("developer") || 
                                                  (p.position ?: "").lowercase().contains("it") || 
                                                  (p.position ?: "").lowercase().contains("software") ||
                                                  (p.department_name ?: "").lowercase().contains("it") ||
                                                  p.department_id == 5 || p.department_id == 4
                                if (isActuallyIT) currentITStatus = true
                            }
                            apply()
                        }
                    }
                }
            } catch (e: Exception) { }
            kotlinx.coroutines.delay(30000)
        }
    }

    val bottomItems = remember(isAdmin, isIT) {
        if (isAdmin) listOf(Screen.Home, Screen.AdminLeads, Screen.AdminTasks, Screen.Profile)
        else if (isIT) listOf(Screen.ITDashboard, Screen.Leads, Screen.Home, Screen.Tasks, Screen.Profile)
        else listOf(Screen.Activities, Screen.Leads, Screen.Home, Screen.Tasks, Screen.Profile)
    }

    val drawerItems = remember(isAdmin, isIT) {
        val baseItems = mutableListOf<DrawerItem>()
        if (isAdmin) {
            baseItems.add(DrawerItem("Admin Menu", Icons.Default.Dashboard, section = "HEADER"))
            baseItems.add(DrawerItem("DASHBOARD", Icons.Default.Dashboard, Screen.Home.route))
            baseItems.add(DrawerItem("CRM Operations", Icons.Default.Person, section = "SECTION"))
            baseItems.add(DrawerItem("All Leads", Screen.AdminLeads.icon, Screen.AdminLeads.route))
            baseItems.add(DrawerItem("Customers", Screen.AdminCustomers.icon, Screen.AdminCustomers.route))
            baseItems.add(DrawerItem("Follow Ups", Screen.AdminFollowUps.icon, Screen.AdminFollowUps.route))
            baseItems.add(DrawerItem("Sales & Invoices", Icons.Default.PointOfSale, section = "SECTION"))
            baseItems.add(DrawerItem("Quotation", Screen.AdminQuotes.icon, Screen.AdminQuotes.route))
            baseItems.add(DrawerItem("Invoices", Screen.AdminInvoices.icon, Screen.AdminInvoices.route))
            baseItems.add(DrawerItem("Payments", Screen.AdminPayments.icon, Screen.AdminPayments.route))
            baseItems.add(DrawerItem("Project Management", Icons.Default.AccountTree, section = "SECTION"))
            baseItems.add(DrawerItem("Active Projects", Screen.AdminProjects.icon, Screen.AdminProjects.route))
            baseItems.add(DrawerItem("All Tasks", Screen.AdminTasks.icon, Screen.AdminTasks.route))
            baseItems.add(DrawerItem("HR Management", Icons.Default.People, section = "SECTION"))
            baseItems.add(DrawerItem("Employees", Screen.AdminEmployees.icon, Screen.AdminEmployees.route))
            baseItems.add(DrawerItem("Team Attendance", Screen.AdminAttendance.icon, Screen.AdminAttendance.route))
            baseItems.add(DrawerItem("Leave Requests", Screen.AdminLeave.icon, Screen.AdminLeave.route))
            baseItems.add(DrawerItem("Payroll", Screen.AdminPayroll.icon, Screen.AdminPayroll.route))
            baseItems.add(DrawerItem("System Admin", Icons.Default.Settings, section = "SECTION"))
            baseItems.add(DrawerItem("Company Settings", Screen.AdminSettings.icon, Screen.AdminSettings.route))
        } else {
            baseItems.add(DrawerItem("Employee Panel", Icons.Default.Person, section = "HEADER"))
            baseItems.add(DrawerItem("My Dashboard", Screen.Home.icon, Screen.Home.route))
            baseItems.add(DrawerItem("My Daily Tasks", Screen.Tasks.icon, Screen.Tasks.route))
            baseItems.add(DrawerItem("My Leads", Screen.Leads.icon, Screen.Leads.route))
            baseItems.add(DrawerItem("Self Service", Icons.Default.HowToReg, section = "SECTION"))
            baseItems.add(DrawerItem("My Attendance", Screen.Attendance.icon, Screen.Attendance.route))
            baseItems.add(DrawerItem("Apply Leave", Screen.LeaveRequests.icon, Screen.LeaveRequests.route))
            baseItems.add(DrawerItem("Payslips", Screen.Payroll.icon, Screen.Payroll.route))
            baseItems.add(DrawerItem("More", Icons.Default.MoreHoriz, section = "SECTION"))
            baseItems.add(DrawerItem("Announcements", Screen.Announcements.icon, Screen.Announcements.route))
            baseItems.add(DrawerItem("Settings", Screen.Settings.icon, Screen.Settings.route))
        }
        baseItems
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PrimaryIndigo, Color(0xFF1E3A8A)))).padding(24.dp)) {
                    Column {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).padding(2.dp)) {
                            Image(painter = painterResource(id = R.drawable.hand), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("FRIENDS CRM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
                        Text("Enterprise Solutions", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
                
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                    drawerItems.forEach { item ->
                        when (item.section) {
                            "HEADER" -> { /* Already in top box */ }
                            "SECTION" -> {
                                Text(text = item.title, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }
                            else -> {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route
                                val selected = currentRoute == item.route
                                
                                NavigationDrawerItem(
                                    label = { Text(item.title, fontSize = 14.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium) },
                                    selected = selected,
                                    onClick = { 
                                        scope.launch { drawerState.close() }
                                        if (item.route.isNotEmpty()) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(item.icon, null, modifier = Modifier.size(20.dp)) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = PrimaryIndigo.copy(alpha = 0.1f),
                                        selectedIconColor = PrimaryIndigo,
                                        selectedTextColor = PrimaryIndigo,
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.DarkGray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    NavigationDrawerItem(
                        label = { Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = onLogout,
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(20.dp)) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = Color.Red, unselectedIconColor = Color.Red),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = bottomItems.any { it.route == currentRoute }

                if (showBottomBar) {
                    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                        val currentDestination = navBackStackEntry?.destination
                        bottomItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { Icon(screen.icon, null) },
                                label = { Text(screen.title, fontSize = 10.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryIndigo, selectedTextColor = PrimaryIndigo, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = PrimaryIndigo.copy(alpha = 0.1f))
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController, 
                startDestination = if(isAdmin) Screen.Home.route else if(isIT) Screen.ITDashboard.route else Screen.Activities.route, 
                Modifier.padding(bottom = innerPadding.calculateBottomPadding()) // Removed top padding to let headers fill status bar
            ) {
                composable(Screen.ITDashboard.route) { ITDashboardScreen(onMenuClick = { scope.launch { drawerState.open() } }, navController = navController) }
                composable(Screen.ITAppUpdates.route) { ITAppUpdatesScreen(onBack = { navController.popBackStack() }) }
                composable(Screen.Activities.route) { ActivityHistoryScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, navController = navController) }
                composable(Screen.Home.route) { 
                    if (isAdmin) AdminDashboard(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, onViewAllAttendance = { navController.navigate(Screen.AdminAttendance.route) })
                    else if (isIT) ITDashboardScreen(onMenuClick = { scope.launch { drawerState.open() } }, navController = navController)
                    else DashboardScreen(userId = userId, onLogout = onLogout, onMenuClick = { scope.launch { drawerState.open() } }, navController = navController) 
                }
                composable(Screen.AdminLeads.route) { AdminLeadsScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, onLeadClick = { leadId -> navController.navigate("lead_details/$leadId") }) }
                composable(Screen.AdminCustomers.route) { AdminCustomersScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, onCustomerClick = { customerId -> navController.navigate("customer_details/$customerId") }) }
                composable(Screen.AdminProjects.route) { AdminWorksScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, onWorkClick = { workId -> navController.navigate("work_details/$workId") }) }
                composable(Screen.AdminInvoices.route) { AdminInvoicesScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.AdminQuotes.route) { AdminQuotesScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.AdminTasks.route) { AdminTasksScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.AdminFollowUps.route) { AdminFollowUpsScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, onLeadClick = { id -> navController.navigate("lead_details/$id") }) }
                composable(Screen.AdminPayments.route) { AdminPaymentsScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.Attendance.route) { AttendanceHistoryScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.Leads.route) { LeadsScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, onLeadClick = { leadId -> navController.navigate("lead_details/$leadId") }, onWorkClick = { workId -> navController.navigate("work_details/$workId") }, onCreateLead = { navController.navigate(Screen.CreateLead.route) }) }
                composable(Screen.AdminEmployees.route) { AdminEmployeesScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.AdminLeave.route) { AdminLeaveScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.AdminPayroll.route) { AdminPayrollScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.LeadDetails.route) { backStackEntry -> 
                    val leadId = backStackEntry.arguments?.getString("leadId")?.toInt() ?: 0
                    if (isAdmin) AdminLeadDetailsScreen(userId = userId, leadId = leadId, onBack = { navController.popBackStack() }, navController = navController)
                    else LeadDetailsScreen(userId = userId, leadId = leadId, onBack = { navController.popBackStack() }, navController = navController)
                }
                composable(Screen.Tasks.route) { TasksScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, navController = navController) }
                composable(Screen.LeaveRequests.route) { LeaveRequestsScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.Payroll.route) { PayrollScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.Announcements.route) { AnnouncementsScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }) }
                composable(Screen.Profile.route) { ProfileScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, onSettingsClick = { navController.navigate(Screen.Settings.route) }, onLogout = onLogout) }
                composable(Screen.Customers.route) { CustomersScreen(userId = userId, onMenuClick = { scope.launch { drawerState.open() } }, navController = navController) }
                composable(Screen.CustomerDetails.route) { backStackEntry -> val customerId = backStackEntry.arguments?.getString("customerId")?.toInt() ?: 0; CustomerDetailsScreen(userId = userId, customerId = customerId, navController = navController, onBack = { navController.popBackStack() }) }
                composable(Screen.LicenseDetails.route) { backStackEntry -> val licenseKey = backStackEntry.arguments?.getString("licenseKey") ?: ""; LicenseDetailsScreen(userId = userId, licenseKey = licenseKey, onBack = { navController.popBackStack() }) }
                composable(Screen.Notifications.route) { NotificationHistoryScreen(userId = userId, onBack = { navController.popBackStack() }, navController = navController) }
                composable(
                    route = Screen.CreateLead.route,
                    arguments = listOf(
                        navArgument("phone") { defaultValue = "" },
                        navArgument("notes") { defaultValue = "" },
                        navArgument("open_quotation") { defaultValue = "" },
                        navArgument("open_followup") { defaultValue = "" }
                    )
                ) { backStackEntry -> 
                    val phone = backStackEntry.arguments?.getString("phone") ?: ""
                    val notes = backStackEntry.arguments?.getString("notes") ?: ""
                    val openQuote = backStackEntry.arguments?.getString("open_quotation") == "true"
                    val openFollowup = backStackEntry.arguments?.getString("open_followup") == "true"
                    
                    CreateLeadScreen(
                        userId = userId, 
                        initialPhone = phone, 
                        initialMessage = notes,
                        autoOpenQuotation = openQuote,
                        autoOpenFollowup = openFollowup,
                        onCancel = { navController.popBackStack() }, 
                        onSuccess = { navController.navigate(Screen.Leads.route) { popUpTo(Screen.Home.route) } }
                    ) 
                }
                composable(Screen.ActivityDetails.route) { backStackEntry -> val activityId = backStackEntry.arguments?.getString("activityId")?.toInt() ?: 0; ActivityDetailScreen(userId = userId, activityId = activityId, onBack = { navController.popBackStack() }) }
                composable(Screen.Settings.route) { SettingsScreen(isDarkTheme = isDarkTheme, onThemeChange = onThemeChange, onBack = { navController.popBackStack() }, onBusinessDetailsClick = { navController.navigate(Screen.AdminSettings.route) }) }
                composable(Screen.AdminSettings.route) { QuoteSettingsScreen(onBack = { navController.popBackStack() }) }
                composable(Screen.AdminAttendance.route) { AdminAttendanceScreen(userId = userId, onBack = { navController.popBackStack() }) }
                composable(route = Screen.WorkDetails.route, arguments = listOf(navArgument("workId") { type = NavType.IntType })) { backStackEntry -> val workId = backStackEntry.arguments?.getInt("workId") ?: 0; WorkDetailsScreen(userId = userId, workId = workId, onBack = { navController.popBackStack() }) }
                composable(Screen.AiAssistant.route) { AiAgentScreen(userId = userId, onBack = { navController.popBackStack() }) }
            }
        }
    }
}
