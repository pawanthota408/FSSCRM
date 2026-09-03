package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.fsscrm.network.*
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.emptyList

sealed class HistoryItem {
    data class Call(val name: String?, val number: String, val date: Long, val duration: Long, val type: Int) : HistoryItem()
    data class CRMActivity(val activity: ActivityLog) : HistoryItem()
    
    val timestamp: Long
        get() = when (this) {
            is Call -> date
            is CRMActivity -> {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    sdf.parse(activity.created_at)?.time ?: 0L
                } catch (e: Exception) { 0L }
            }
        }
}

data class RecentCall(
    val name: String? = null,
    val number: String,
    val date: Long,
    val duration: Long,
    val type: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(userId: Int, onMenuClick: () -> Unit, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var historyItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var leadLookupMap by remember { mutableStateOf<Map<String, Lead>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val sheetState = rememberModalBottomSheetState()
    var showActionsSheet by remember { mutableStateOf(false) }
    var showMoveToSheet by remember { mutableStateOf(false) }
    var showDialerSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<HistoryItem?>(null) }
    
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions[Manifest.permission.READ_CALL_LOG] == true &&
                         permissions[Manifest.permission.READ_CONTACTS] == true
    }

    fun loadData() {
        isLoading = true
        scope.launch {
            try {
                // Fetch Leads for Identification
                try {
                    val leadsResponse = RetrofitClient.apiService.getLeads(mapOf("user_id" to userId))
                    if (leadsResponse.isSuccessful) {
                        leadsResponse.toLenientJson()?.let { json ->
                            val leadsList = if (json.isJsonArray) {
                                com.google.gson.Gson().fromJson<List<Lead>>(json, object : com.google.gson.reflect.TypeToken<List<Lead>>() {}.type)
                            } else if (json.isJsonObject && json.asJsonObject.has("leads")) {
                                LeadResponse.fromJson(json).leads
                            } else emptyList()
                            
                            leadLookupMap = leadsList.associateBy { it.phone ?: "" }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ActivityHistory", "Error fetching leads", e)
                }

                val activities = try {
                    val response = RetrofitClient.apiService.getActivities(mapOf("user_id" to userId))
                    if (response.isSuccessful) {
                        val json = response.toLenientJson()
                        if (json != null && json.isJsonArray) {
                            com.google.gson.Gson().fromJson<List<ActivityLog>>(
                                json,
                                object : com.google.gson.reflect.TypeToken<List<ActivityLog>>() {}.type
                            ).map { HistoryItem.CRMActivity(it) }
                        } else emptyList()
                    } else emptyList()
                } catch (e: Exception) { emptyList() }

                val calls = if (hasPermissions) {
                    withContext(Dispatchers.IO) {
                        try {
                            fetchLocalCalls(context).map { HistoryItem.Call(it.name, it.number, it.date, it.duration, it.type) }
                        } catch (e: Exception) { emptyList() }
                    }
                } else emptyList()

                historyItems = (activities + calls).sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                android.util.Log.e("ActivityHistory", "Data loading error", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) {
            launcher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS))
        }
        loadData()
    }

    Scaffold(
        containerColor = CorpSurface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialerSheet = true },
                containerColor = CorpBlue,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Dialpad, null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "CRM & Call History",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        Icon(Icons.Outlined.Notifications, null, tint = Color.White)
                    }
                }
            )
            
            // Search Bar (Traditional style)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Search activity logs or call history...", color = Color.Gray, fontSize = 14.sp)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CorpBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historyItems.take(100)) { item ->
                        HistoryCardRedesigned(
                            item = item, 
                            lead = if (item is HistoryItem.Call) leadLookupMap[item.number] else null, 
                            navController = navController, 
                            onShowActions = { selectedItemForActions = item; showActionsSheet = true }, 
                            onShowMoveTo = { selectedItemForActions = item; showMoveToSheet = true }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showMoveToSheet) {
        val phone = when (val item = selectedItemForActions) {
            is HistoryItem.Call -> item.number
            else -> null
        }
        MoveToBottomSheet(sheetState = sheetState, onDismiss = { showMoveToSheet = false }, onNavigateToCreate = { route -> showMoveToSheet = false; navController.navigate(route) }, phone = phone)
    }

    if (showActionsSheet) {
        ActionsBottomSheet(sheetState = sheetState, onDismiss = { showActionsSheet = false })
    }

    if (showDialerSheet) {
        DialerBottomSheet(onDismiss = { showDialerSheet = false })
    }
}

@Composable
fun HistoryCardRedesigned(
    item: HistoryItem, 
    lead: Lead?, 
    navController: NavController, 
    onShowActions: () -> Unit, 
    onShowMoveTo: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val accentColor = when(item) {
                    is HistoryItem.Call -> if(item.type == CallLog.Calls.MISSED_TYPE) Color(0xFFEF4444) else CorpEmerald
                    is HistoryItem.CRMActivity -> CorpBlue
                }
                
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when(item) {
                        is HistoryItem.Call -> if(item.type == CallLog.Calls.OUTGOING_TYPE) Icons.Default.CallMade else Icons.Default.CallReceived
                        is HistoryItem.CRMActivity -> when(item.activity.activity_type.lowercase()) {
                            "call" -> Icons.Default.Call
                            "meeting" -> Icons.Default.Groups
                            else -> Icons.Default.History
                        }
                    }
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    val title = when (item) { 
                        is HistoryItem.Call -> lead?.name ?: item.name ?: item.number
                        // PRIMARY FOCUS ON COMPANY NAME FOR CRM ACTIVITIES
                        is HistoryItem.CRMActivity -> item.activity.company_name ?: item.activity.client_name ?: "Unknown Activity"
                    }
                    
                    Text(text = title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = CorpDark)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item is HistoryItem.Call) {
                            Text(text = item.number, fontSize = 12.sp, color = Color.Gray)
                            Text(text = " • ", color = Color.Gray)
                        } else if (item is HistoryItem.CRMActivity && item.activity.client_name != null) {
                            Text(text = item.activity.client_name!!, fontSize = 12.sp, color = Color.Gray)
                            Text(text = " • ", color = Color.Gray)
                        }
                        
                        val timeStr = when(item) {
                            is HistoryItem.Call -> formatHistoryTime(item.date)
                            is HistoryItem.CRMActivity -> formatActivityDate(item.activity.created_at).take(11)
                        }
                        Text(text = timeStr, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                
                if (lead != null) {
                    Surface(color = CorpBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = (lead.status ?: "New").uppercase(), 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Black, 
                            color = CorpBlue
                        )
                    }
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(16.dp))
                
                if (item is HistoryItem.CRMActivity && item.activity.description != null) {
                    Text(item.activity.description!!, fontSize = 13.sp, color = Color(0xFF475569), modifier = Modifier.padding(bottom = 16.dp))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HistoryActionButton(Icons.Default.Add, "Actions", CorpBlue) { onShowActions() }
                    HistoryActionButton(Icons.Default.ContactPage, "Lead Details", Color(0xFF6366F1)) {
                        if (item is HistoryItem.CRMActivity && item.activity.lead_id != null) {
                            navController.navigate("lead_details/${item.activity.lead_id}")
                        } else if (item is HistoryItem.Call) {
                            if (lead != null) navController.navigate("lead_details/${lead.id}")
                            else navController.navigate("create_lead?phone=${item.number}")
                        }
                    }
                    HistoryActionButton(Icons.Default.Shortcut, "Update Status", Color(0xFF8B5CF6)) { onShowMoveTo() }
                    
                    if (item is HistoryItem.Call) {
                        HistoryActionButton(Icons.Default.Phone, "Call", CorpEmerald) { initiateCall(context, item.number) }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = phoneNumber.ifEmpty { "Enter Number" }, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (phoneNumber.isEmpty()) Color.LightGray else Color.Black)
            if (phoneNumber.isNotEmpty()) { IconButton(onClick = { phoneNumber = phoneNumber.dropLast(1) }) { Icon(Icons.Default.Backspace, null) } }
            Spacer(modifier = Modifier.height(24.dp))
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                        for (j in 0 until 3) {
                            val key = keys[i * 3 + j]
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.2f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)).clickable { phoneNumber += key }, contentAlignment = Alignment.Center) {
                                Text(text = key, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            FloatingActionButton(onClick = { if (phoneNumber.isNotEmpty()) { initiateCall(context, phoneNumber); onDismiss() } }, containerColor = CorpEmerald, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.Call, null, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToBottomSheet(sheetState: SheetState, phone: String?, onDismiss: () -> Unit, onNavigateToCreate: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Update Status To:", modifier = Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Black, color = CorpDark)
            val options = listOf("Hot Lead", "Follow up", "Quotation", "Not interested", "Closed / Won")
            options.forEach { option -> 
                MoveToItemNew(option, false) { 
                    if (phone != null) onNavigateToCreate("create_lead?phone=$phone") else onDismiss() 
                } 
            }
        }
    }
}

@Composable
fun MoveToItemNew(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 16.sp, color = if(selected) CorpBlue else CorpDark, fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium)
        if (selected) Icon(Icons.Default.Check, null, tint = CorpBlue)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsBottomSheet(sheetState: SheetState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Quick Actions", modifier = Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Black, color = CorpDark)
            ActionItemNew(Icons.Default.EventNote, "Add note") { onDismiss() }
            ActionItemNew(Icons.Default.EditNote, "Schedule task") { onDismiss() }
            ActionItemNew(Icons.Default.Payments, "Create Quote") { onDismiss() }
            ActionItemNew(Icons.Default.Person, "Edit Lead") { onDismiss() }
        }
    }
}

@Composable
fun ActionItemNew(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CorpBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, color = CorpDark, fontWeight = FontWeight.Medium)
    }
}

fun formatHistoryTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun initiateCall(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        }
    } catch (e: Exception) {
        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) } catch (inner: Exception) { }
    }
}

private fun fetchLocalCalls(context: Context): List<RecentCall> {
    val calls = mutableListOf<RecentCall>()
    try {
        val cursor = context.contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC")
        cursor?.use {
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            var count = 0
            while (it.moveToNext() && count < 100) {
                val number = it.getString(numberIdx) ?: "Unknown"
                val name = if (nameIdx != -1) it.getString(nameIdx) else null
                calls.add(RecentCall(name ?: resolveContactName(context, number), number, it.getLong(dateIdx), it.getLong(durationIdx), it.getInt(typeIdx)))
                count++
            }
        }
    } catch (e: Exception) { }
    return calls
}

private fun resolveContactName(context: Context, phoneNumber: String): String? {
    if (phoneNumber == "Unknown") return null
    try {
        val uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
        cursor?.use { if (it.moveToFirst()) return it.getString(0) }
    } catch (e: Exception) { }
    return null
}

private fun formatActivityDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "N/A"
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        output.format(input.parse(dateStr)!!)
    } catch (_: Exception) { dateStr }
}


