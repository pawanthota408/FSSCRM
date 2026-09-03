package com.fsscrm.ui.sales

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.ActivityLog
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(userId: Int, activityId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activity by remember { mutableStateOf<ActivityLog?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchActivity() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getActivities(mapOf("user_id" to userId))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    val activities: List<ActivityLog> = when (body) {
                        is List<*> -> body.filterIsInstance<ActivityLog>()
                        is JsonElement -> {
                            if (body.isJsonArray) {
                                Gson().fromJson(
                                    body,
                                    object : TypeToken<List<ActivityLog>>() {}.type
                                )
                            } else emptyList()
                        }
                        is String -> Gson().fromJson(
                            body,
                            object : TypeToken<List<ActivityLog>>() {}.type
                        )
                        else -> {
                            // Fallback – try to parse whatever toString() produces
                            try {
                                Gson().fromJson(
                                    body.toString(),
                                    object : TypeToken<List<ActivityLog>>() {}.type
                                )
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }

                    activity = activities.find { it.id == activityId }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activityId) {
        fetchActivity()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (activity != null && activity!!.status?.lowercase() != "completed") {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Reschedule */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reschedule", color = Color.Black)
                        }
                        Button(
                            onClick = { /* Mark complete */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as complete")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            UniversalHeader(
                title = "Activity Details",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { /* Edit */ }) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { fetchActivity() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                }
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                }
                activity == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Activity not found", color = Color.Gray)
                    }
                }
                else -> {
                    val act = activity!!   // safe now

                    // Main Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = act.company_name ?: "No Company",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = PrimaryIndigo.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = act.activity_type.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = PrimaryIndigo,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formatDateOnly(act.created_at),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            if (!act.meeting_purpose.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Purpose: ${act.meeting_purpose}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            if (!act.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Instructions for Employee:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = act.description!!,
                                    fontSize = 14.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val phone = act.client_mobile ?: act.phone
                                val email = act.client_email ?: act.email

                                ActivityCircleAction(Icons.Default.Call, Color(0xFF10B981)) {
                                    phone?.let {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                                        )
                                    }
                                }
                                ActivityCircleAction(Icons.AutoMirrored.Filled.Chat, Color(0xFF25D366)) {
                                    phone?.let {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://api.whatsapp.com/send?phone=$it")
                                            )
                                        )
                                    }
                                }
                                ActivityCircleAction(Icons.AutoMirrored.Filled.Message, Color(0xFFF59E0B)) {
                                    phone?.let {
                                        context.startActivity(
                                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$it"))
                                        )
                                    }
                                }
                                ActivityCircleAction(Icons.Default.Email, Color(0xFF3B82F6)) {
                                    email?.let {
                                        context.startActivity(
                                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it"))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Related Entity Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Related Entity",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            EntityDetailRow(Icons.Outlined.Business, "Company", act.company_name ?: "N/A")
                            EntityDetailRow(Icons.Outlined.Person, "Client Name", act.client_name ?: "N/A")
                            EntityDetailRow(Icons.Outlined.Info, "Purpose", act.meeting_purpose ?: "N/A")
                            EntityDetailRow(Icons.Outlined.Place, "Address", act.location_address ?: "N/A")

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val uri = "google.navigation:q=${act.latitude},${act.longitude}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                    intent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    Icons.Default.Navigation,
                                    null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Navigate to Location",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visit Status Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Visit Status", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            VisitDetailRow("Check-in", act.visit_start_time ?: "Not checked in")
                            VisitDetailRow("Check-out", act.visit_end_time ?: "In progress")

                            if (!act.visit_start_time.isNullOrBlank() && !act.visit_end_time.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Time Spent: ${calculateTimeSpent(act.visit_start_time, act.visit_end_time)}",
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Team & Attendees
                    if (!act.attendees.isNullOrEmpty() || !act.assignedUsers.isNullOrEmpty()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            if (!act.attendees.isNullOrEmpty()) {
                                Text(
                                    "Contact Persons",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                act.attendees!!.forEach { attendee ->
                                    AttendeeItem(attendee.name ?: "Unknown", attendee.position ?: "Client")
                                }
                            }

                            if (!act.assignedUsers.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Team Members",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                act.assignedUsers!!.forEach { user ->
                                    AttendeeItem(
                                        user.name ?: "Unknown",
                                        user.position ?: "Employee",
                                        isEmployee = true
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─── Helper composables (unchanged) ──────────────────────────────────────────

@Composable
fun EntityDetailRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ActivityCircleAction(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun VisitDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(80.dp), color = Color.Gray, fontSize = 13.sp)
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AttendeeItem(name: String, role: String, isEmployee: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isEmployee) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = if (isEmployee) PrimaryIndigo else Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(role, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

private fun formatDateOnly(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "N/A"
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        output.format(input.parse(dateStr)!!)
    } catch (_: Exception) {
        dateStr
    }
}

private fun calculateTimeSpent(start: String?, end: String?): String {
    if (start.isNullOrEmpty() || end.isNullOrEmpty()) return "N/A"
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val d1 = format.parse(start)
        val d2 = format.parse(end)
        val diff = d2!!.time - d1!!.time
        val hours = diff / (60 * 60 * 1000)
        val mins = (diff / (60 * 1000)) % 60
        if (hours > 0) "$hours hr $mins min" else "$mins min"
    } catch (_: Exception) {
        "0 min"
    }
}