package com.fsscrm.ui.it

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.ui.common.StatBox
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.theme.PrimaryIndigo

@Composable
fun ITDashboardScreen(
    onMenuClick: () -> Unit,
    navController: androidx.navigation.NavController
) {
    Scaffold(
        topBar = {
            UniversalHeader(
                title = "IT Dashboard",
                onMenuClick = onMenuClick
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── SYSTEM OVERVIEW ──
            item {
                Text("System Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBox(
                        label = "Server Status",
                        value = "Online",
                        icon = Icons.Default.Storage,
                        gradient = listOf(Color(0xFF10B981), Color(0xFF34D399)),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "App Version",
                        value = "v1.0.8",
                        icon = Icons.Default.SystemUpdate,
                        gradient = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBox(
                        label = "Active Users",
                        value = "12",
                        icon = Icons.Default.People,
                        gradient = listOf(Color(0xFF6366F1), Color(0xFF818CF8)),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Database",
                        value = "Healthy",
                        icon = Icons.Default.Dns,
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── CALL OVERLAY STATUS ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Call Overlay Feature", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The call overlay is enabled for all departments. This allows real-time lead creation and activity logging during calls.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "ACTIVE & IMPORTANT",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // ── QUICK ACTIONS ──
            item {
                Text("IT Quick Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ITActionItem("Push App Update", Icons.Default.CloudUpload, Color(0xFF3B82F6)) {
                        navController.navigate("it_updates")
                    }
                    ITActionItem("View Error Logs", Icons.Default.BugReport, Color(0xFFEF4444)) {
                        // Action
                    }
                    ITActionItem("User Permissions", Icons.Default.Security, Color(0xFF6366F1)) {
                        // Action
                    }
                }
            }
        }
    }
}

@Composable
fun ITActionItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}
