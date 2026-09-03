package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.AttendanceHistoryItem
import com.fsscrm.network.AttendanceHistoryResponse
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(userId: Int, onMenuClick: () -> Unit) {
    var attendanceData by remember { mutableStateOf<AttendanceHistoryResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAttendanceHistory(mapOf("user_id" to userId))
                if (response.isSuccessful && response.body() != null) {
                    attendanceData = com.google.gson.Gson().fromJson(response.body(), AttendanceHistoryResponse::class.java)
                }
            } catch (e: Exception) { } finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "Attendance History",
                onMenuClick = onMenuClick
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else {
                attendanceData?.let { data ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    SummaryStatItem("Present", data.summary.present.toString(), Color(0xFF10B981))
                                    Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFFF1F5F9)))
                                    SummaryStatItem("Absent", data.summary.absent.toString(), Color(0xFFEF4444))
                                    Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFFF1F5F9)))
                                    SummaryStatItem("Leaves", data.summary.leaves.toString(), PrimaryIndigo)
                                }
                            }
                        }

                        item {
                            Text("Recent Attendance Logs", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                        }

                        items(data.history) { item ->
                            AttendanceHistoryRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AttendanceHistoryRow(item: AttendanceHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.date, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(item.day, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (item.status == "Present") Color(0xFFDCFCE7) else Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        item.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (item.status == "Present") Color(0xFF166534) else Color(0xFFB91C1C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("${formatTime(item.check_in)} - ${formatTime(item.check_out)}", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

private fun formatTime(time: String?): String {
    if (time.isNullOrEmpty() || time == "00:00:00") return "--:--"
    return try {
        val parts = time.split(":")
        val h = parts[0].toInt()
        val m = parts[1]
        val ampm = if (h < 12) "AM" else "PM"
        val dh = if (h % 12 == 0) 12 else h % 12
        "$dh:$m $ampm"
    } catch (_: Exception) { time }
}
