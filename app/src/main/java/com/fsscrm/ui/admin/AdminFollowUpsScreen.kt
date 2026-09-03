package com.fsscrm.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.FollowUp
import com.fsscrm.network.FollowUpResponse
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.EmptyStateCard
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFollowUpsScreen(userId: Int, onMenuClick: () -> Unit, onLeadClick: (Int) -> Unit) {
    var followUps by remember { mutableStateOf<List<FollowUp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf("pending") }
    val scope = rememberCoroutineScope()

    fun fetch() {
        isLoading = true
        scope.launch {
            try {
                // Using get_followups.php - assuming admin user_id fetches broader list or filtered by status
                val resp = RetrofitClient.apiService.getFollowUps(mapOf("user_id" to userId))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        val response = FollowUpResponse.fromJson(body)
                        followUps = response.followups
                    }
                }
            } catch (_: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    Scaffold(
        topBar = { UniversalHeader("Team Follow-Ups", onMenuClick) },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Simple status filter
            TabRow(selectedTabIndex = if(selectedStatus == "pending") 0 else 1, containerColor = Color.White) {
                Tab(selected = selectedStatus == "pending", onClick = { selectedStatus = "pending" }, text = { Text("Pending") })
                Tab(selected = selectedStatus == "completed", onClick = { selectedStatus = "completed" }, text = { Text("Completed") })
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryIndigo) }
            } else {
                val filtered = followUps.filter { it.status.equals(selectedStatus, true) }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyStateCard("No $selectedStatus follow ups") }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filtered) { fu ->
                            FollowUpCard(fu) { onLeadClick(fu.lead_id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUpCard(fu: FollowUp, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color(0xFFEEF2FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarToday, null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(fu.lead_name ?: "Unknown Client", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${fu.follow_up_date} at ${fu.follow_up_time ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                if (!fu.remarks.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(fu.remarks, fontSize = 13.sp, color = Color.DarkGray, maxLines = 2)
                }
            }
        }
    }
}
