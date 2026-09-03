package com.fsscrm.ui.common

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fsscrm.network.Employee
import com.fsscrm.network.ProfileResponse
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userId: Int, onMenuClick: () -> Unit, onSettingsClick: () -> Unit, onLogout: () -> Unit) {
    var profileData by remember { mutableStateOf<Employee?>(null) }
    var dashboardData by remember { mutableStateOf<com.fsscrm.network.DashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun loadProfile() {
        scope.launch {
            if (userId == 0) {
                snackbarHostState.showSnackbar("Error: Invalid User ID (0)")
                isLoading = false
                return@launch
            }
            try {
                isLoading = true
                // Fetch full dashboard data to get profile + stats
                val response = RetrofitClient.apiService.getDashboardData(mapOf("user_id" to userId))
                
                val data = response.toLenientJson()?.let { com.fsscrm.network.DashboardResponse.fromJson(it) }
                if (response.isSuccessful && data != null) {
                    dashboardData = data
                    profileData = data.profile
                } else {
                    // Fallback to simple profile if dashboard fails
                    val profResp = RetrofitClient.apiService.getProfile(userId)
                    if (profResp.isSuccessful && profResp.body() != null) {
                        profileData = profResp.body()?.employee
                    }
                }
            } catch (e: Exception) {
                Log.e("PROFILE", "Error loading profile", e)
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (isLoading && profileData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryIndigo)
            }
        } else if (profileData != null) {
            val employee = profileData!!
            val stats = dashboardData?.getEffectiveStats()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                UniversalHeader(
                    title = "My Profile",
                    onMenuClick = onMenuClick,
                    actions = {
                        IconButton(onClick = { loadProfile() }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Outlined.Settings, null, tint = Color.White)
                        }
                    }
                )

                // --- TRADITIONAL HERO SECTION ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E3A8A)) // Navy Blue
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Image
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                if (!employee.profile_image.isNullOrBlank()) {
                                    AsyncImage(
                                        model = employee.profile_image,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = employee.name?.take(1)?.uppercase() ?: "?",
                                            color = Color.White,
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            // Active Status Ring
                            Surface(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .border(2.dp, Color(0xFF1E3A8A), CircleShape),
                                color = if (employee.status?.lowercase() == "active") Color(0xFF22C55E) else Color.Gray,
                                shape = CircleShape
                            ) {}
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = employee.name ?: "Unnamed User",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = (employee.position ?: "Team Member").uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.5.sp
                        )
                        
                        Surface(
                            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                            color = Color.Black.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "ID: ${employee.employee_id ?: employee.employee_code ?: "N/A"}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // --- PERFORMANCE STATS CARD ---
                if (stats != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .offset(y = (-24).dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatMiniItem("Total Leads", stats.totalLeads.toString(), Color(0xFF3B82F6))
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFF1F5F9)))
                            StatMiniItem("Won Deals", stats.wonDeals.toString(), Color(0xFF10B981))
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFF1F5F9)))
                            StatMiniItem("Pending Tasks", stats.pendingTasks.toString(), Color(0xFFF59E0B))
                        }
                    }
                }

                // --- INFO SECTIONS ---
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = if (stats != null) 0.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Employment Details
                    ProfileSectionCardRedesign(
                        title = "Employment Details",
                        icon = Icons.Default.WorkOutline,
                        iconColor = Color(0xFF6366F1)
                    ) {
                        InfoRowRedesign("Official Role", employee.role ?: "N/A", Icons.Default.VerifiedUser)
                        val deptDisplay = employee.department_name ?: if (employee.department_id == 5 || employee.department_id == 4) "IT & Tech" else "Operations"
                        InfoRowRedesign("Department", deptDisplay, Icons.Default.Business)
                        InfoRowRedesign("Joining Date", employee.joining_date ?: "N/A", Icons.Default.EventAvailable)
                        InfoRowRedesign("Employment Status", employee.status?.replaceFirstChar { it.uppercase() } ?: "Active", Icons.Default.OfflineBolt)
                    }

                    // Contact Information
                    ProfileSectionCardRedesign(
                        title = "Contact Information",
                        icon = Icons.Default.AlternateEmail,
                        iconColor = Color(0xFFEC4899) // Pink 500
                    ) {
                        InfoRowRedesign("Email Address", employee.email ?: "N/A", Icons.Default.Mail)
                        InfoRowRedesign("Mobile Number", employee.mobile ?: employee.phone ?: "N/A", Icons.Default.Smartphone)
                    }

                    // Personal Information
                    ProfileSectionCardRedesign(
                        title = "Personal Details",
                        icon = Icons.Default.PersonOutline,
                        iconColor = Color(0xFF8B5CF6) // Violet 500
                    ) {
                        InfoRowRedesign("Date of Birth", employee.date_of_birth ?: "N/A", Icons.Default.Cake)
                    }

                    // Quick Actions
                    ProfileSectionCardRedesign(
                        title = "Account Actions",
                        icon = Icons.Default.Settings,
                        iconColor = Color(0xFF475569) // Slate 600
                    ) {
                        ActionRowRedesign("Edit Profile Information", "Update name and details", Icons.Default.Edit) { showEditProfile = true }
                        ActionRowRedesign("Security & Password", "Manage account access", Icons.Default.Security) { showChangePassword = true }
                    }

                    // --- LOGOUT DANGER ZONE ---
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFEF4444)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("SIGN OUT FROM ACCOUNT", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Dialogs
    if (showEditProfile) {
        EditProfileDialog(
            currentName = profileData?.name ?: "",
            userId = userId,
            onDismiss = { showEditProfile = false },
            onSuccess = {
                loadProfile()
                showEditProfile = false
                scope.launch {
                    snackbarHostState.showSnackbar("Profile updated successfully")
                }
            },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            userId = userId,
            onDismiss = { showChangePassword = false },
            onSuccess = {
                showChangePassword = false
                scope.launch {
                    snackbarHostState.showSnackbar("Password changed successfully")
                }
            },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }
}

@Composable
fun StatMiniItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileSectionCardRedesign(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.padding(8.dp).size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1E293B))
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun InfoRowRedesign(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(Color(0xFFF8FAFC), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        }
    }
}

@Composable
fun ActionRowRedesign(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    userId: Int,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Name", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter your full name as it should appear in the system.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isBlank()) {
                        onError("Name cannot be empty")
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.updateProfile(
                                mapOf(
                                    "user_id" to userId.toString(),
                                    "name" to newName
                                )
                            )
                            handleJsonResponse(
                                response = response,
                                onSuccess = { onSuccess() },
                                onError = { onError(it) }
                            )
                        } catch (e: Exception) {
                            onError("Error: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    userId: Int,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Update your account password regularly to stay secure.", fontSize = 13.sp, color = Color.Gray)

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        oldPassword.isBlank() -> {
                            onError("Please enter current password")
                            return@Button
                        }
                        newPassword.isBlank() -> {
                            onError("Please enter new password")
                            return@Button
                        }
                        newPassword.length < 6 -> {
                            onError("New password must be at least 6 characters")
                            return@Button
                        }
                        newPassword != confirmPassword -> {
                            onError("Passwords do not match")
                            return@Button
                        }
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.updateProfile(
                                mapOf(
                                    "user_id" to userId.toString(),
                                    "old_password" to oldPassword,
                                    "new_password" to newPassword,
                                    "action" to "change_password"
                                )
                            )
                            handleJsonResponse(
                                response = response,
                                onSuccess = { onSuccess() },
                                onError = { onError(it) }
                            )
                        } catch (e: Exception) {
                            onError("Error: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Update Password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
