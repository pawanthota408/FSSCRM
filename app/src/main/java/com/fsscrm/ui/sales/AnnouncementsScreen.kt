package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.Announcement
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(userId: Int, onMenuClick: () -> Unit) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getDashboardData(mapOf("user_id" to userId))
                if (response.isSuccessful) {
                    response.toLenientJson()?.let { json ->
                        val dashboard = com.fsscrm.network.DashboardResponse.fromJson(json)
                        announcements = dashboard.announcements
                    }
                }
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
        
        while(true) {
            kotlinx.coroutines.delay(5000)
            scope.launch {
                try {
                    val response = RetrofitClient.apiService.getDashboardData(mapOf("user_id" to userId))
                    if (response.isSuccessful) {
                        response.toLenientJson()?.let { json ->
                            val dashboardData = com.fsscrm.network.DashboardResponse.fromJson(json)
                            announcements = dashboardData.announcements
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "Announcements",
                onMenuClick = onMenuClick
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (announcements.isEmpty()) {
                        item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("No announcements found", color = Color.Gray) } }
                    } else {
                        items(announcements) { ann ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ann.description, fontSize = 13.sp, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(ann.date, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
