package com.fsscrm.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.Work
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWorksScreen(
    userId: Int,
    onMenuClick: () -> Unit,
    onWorkClick: (Int) -> Unit
) {
    var works by remember { mutableStateOf<List<Work>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val scope = rememberCoroutineScope()

    fun fetchWorks() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getWorks(mapOf("user_id" to userId, "admin_view" to 1))
                if (response.isSuccessful) {
                    response.toLenientJson()?.let { json ->
                        val listType = object : TypeToken<List<Work>>() {}.type
                        val dataPart = if (json.isJsonObject && json.asJsonObject.has("data")) json.asJsonObject["data"]
                                      else if (json.isJsonObject && json.asJsonObject.has("works")) json.asJsonObject["works"]
                                      else if (json.isJsonObject && json.asJsonObject.has("work_list")) json.asJsonObject["work_list"]
                                      else if (json.isJsonObject && json.asJsonObject.has("active_works")) json.asJsonObject["active_works"]
                                      else if (json.isJsonObject && json.asJsonObject.has("projects")) json.asJsonObject["projects"]
                                      else json
                        works = Gson().fromJson<List<Work>>(dataPart, listType) ?: emptyList()
                    }
                }
            } catch (e: Exception) { } finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetchWorks() }

    val filtered = remember(works, selectedTab) {
        when (selectedTab) {
            0 -> works.filter { 
                val st = it.status.lowercase().trim()
                st != "completed" && st != "finished" && st != "done" && st != "cancelled"
            }
            1 -> works.filter { 
                val st = it.status.lowercase().trim()
                st == "completed" || st == "finished" || st == "done"
            }
            else -> works
        }
    }

    Scaffold(
        topBar = { UniversalHeader(title = "Company Projects", onMenuClick = onMenuClick) },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = PrimaryIndigo) {
                listOf("Active", "Completed", "All").forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No projects found", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered) { work ->
                        AdminWorkCard(work, onWorkClick)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWorkCard(work: Work, onClick: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick = { onClick(work.id) }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(work.work_name.ifBlank { work.description?.take(30) ?: "Project #${work.id}" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(work.customer_name ?: work.lead_name ?: work.lead_company ?: "Customer / Lead", color = Color.Gray, fontSize = 13.sp)
                }
                Surface(
                    color = if(work.status.lowercase() == "completed") Color(0xFFDCFCE7) else Color(0xFFFEF9C3),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        work.status,
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(work.status == "completed") Color(0xFF166534) else Color(0xFF854D0E)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CurrencyRupee, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(work.total_amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(work.created_at.take(10), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
