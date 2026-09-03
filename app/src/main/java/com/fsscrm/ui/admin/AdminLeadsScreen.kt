package com.fsscrm.ui.admin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fsscrm.network.Lead
import com.fsscrm.network.LeadResponse
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch
import java.util.*

private val MainProductsList = listOf("Tally Silver", "Tally Gold", "Tally Server", "Tally Prime")
private val AddonProductsList = listOf("AMC", "TSS", "TDL", "WhatsApp", "Cloud", "BIZAPP", "BIZ Analyst", "Upgrade")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeadsScreen(
    userId: Int,
    onMenuClick: () -> Unit,
    onLeadClick: (Int) -> Unit
) {
    var allLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Filter State
    var selectedStatus by remember { mutableStateOf("All") }
    var selectedTimeRange by remember { mutableStateOf("all") }
    var searchInput by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf("") }
    var showAddLead by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetch() {
        if (userId <= 0) return
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                // Standardization of range keys to match backend expected format
                val rangeKey = when(selectedTimeRange.lowercase()) {
                    "today" -> "today"
                    "yesterday" -> "yesterday"
                    "this week" -> "this_week"
                    "last week" -> "last_week"
                    "this month" -> "this_month"
                    else -> "all"
                }
                
                val params = mapOf(
                    "user_id" to userId.toString(),
                    "status" to selectedStatus,
                    "search" to activeSearch,
                    "range" to rangeKey,
                    "action" to "get_admin_leads"
                )
                
                Log.d("AdminLeads", "Request params: $params")
                
                val resp = RetrofitClient.apiService.getAdminLeadsPost(params)
                if (resp.isSuccessful) {
                    val body = resp.toLenientJson()
                    if (body != null) { 
                        val response = LeadResponse.fromJson(body)
                        allLeads = response.leads
                        Log.d("AdminLeads", "Fetched ${allLeads.size} leads")
                    }
                } else { 
                    errorMessage = "Error ${resp.code()}" 
                }
            } catch (e: Exception) { 
                errorMessage = e.message 
            } finally { isLoading = false }
        }
    }

    LaunchedEffect(selectedStatus, activeSearch, selectedTimeRange) { fetch() }

    Scaffold(
        topBar = { 
            UniversalHeader(
                title = "Company Leads", 
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { fetch() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                }
            ) 
        },
        containerColor = Color(0xFFF1F5F9),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddLead = true }, containerColor = PrimaryIndigo, contentColor = Color.White) {
                Icon(Icons.Default.Add, null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter Bar
            Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdown(
                            label = "Status: $selectedStatus",
                            options = listOf("All", "New", "Contacted", "Qualified", "Proposal", "Won", "Lost"),
                            onSelect = { selectedStatus = it },
                            modifier = Modifier.weight(1f)
                        )

                        val timeOptions = listOf(
                            "all" to "All Time",
                            "today" to "Today",
                            "yesterday" to "Yesterday",
                            "this_week" to "This Week",
                            "last_week" to "Last Week",
                            "this_month" to "This Month"
                        )
                        FilterDropdown(
                            label = "Time: ${timeOptions.find { it.first == selectedTimeRange }?.second ?: "All Time"}",
                            options = timeOptions.map { it.second },
                            onSelect = { label -> selectedTimeRange = timeOptions.find { it.second == label }?.first ?: "all" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search customer, phone...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            trailingIcon = {
                                if (searchInput.isNotEmpty()) {
                                    IconButton(onClick = { searchInput = ""; activeSearch = ""; fetch() }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo)
                        )
                        Button(
                            onClick = { activeSearch = searchInput.trim() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(20.dp))
                        }
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryIndigo) }
            } else if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage!!, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { fetch() }) { Text("Retry") }
                    }
                }
            } else {
                if (allLeads.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text("No leads found for this period", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(allLeads) { lead ->
                            AdminLeadCard(lead) { onLeadClick(lead.id) }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showAddLead) {
        AddLeadDialog(userId, { showAddLead = false }, {
            showAddLead = false
            scope.launch { snackbarHostState.showSnackbar(it) }
            fetch()
        }, {
            scope.launch { snackbarHostState.showSnackbar(it) }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(label: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = label, onValueChange = {}, readOnly = true, singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = PrimaryIndigo)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt, fontSize = 12.sp) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddLeadDialog(userId: Int, onDismiss: () -> Unit, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("New") }
    var leadType by remember { mutableStateOf("Cold") }
    var serviceCategory by remember { mutableStateOf("Tally Product") }
    var selectedMainProduct by remember { mutableStateOf("") }
    var selectedAddons by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f), shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().background(PrimaryIndigo).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Add New Lead", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White) }
                }
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    LField("Full Name *", name) { name = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) { LField("Phone", phone, KeyboardType.Phone) { phone = it } }
                        Column(modifier = Modifier.weight(1f)) { LField("Email", email, KeyboardType.Email) { email = it } }
                    }
                    LField("Company Name", company) { company = it }
                    Text("Service Category *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    var expC by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expC, { expC = it }) {
                        OutlinedTextField(value = serviceCategory, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expC) }, shape = RoundedCornerShape(8.dp))
                        ExposedDropdownMenu(expC, { expC = false }) {
                            listOf("Tally Product", "Services/Addons").forEach { cat ->
                                DropdownMenuItem(text = { Text(text = cat) }, onClick = { serviceCategory = cat; expC = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (serviceCategory == "Tally Product") {
                        Text("Select Tally Product *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MainProductsList.forEach { p ->
                                val sel = selectedMainProduct == p
                                Surface(color = if (sel) Color(0xFFDBEAFE) else Color.White, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) PrimaryIndigo else Color.LightGray), modifier = Modifier.clickable { selectedMainProduct = p }) {
                                    Row(modifier = Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = sel, onClick = { selectedMainProduct = p }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo))
                                        Text(p, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Select Services/Addons", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AddonProductsList.forEach { a ->
                                val sel = selectedAddons.contains(a)
                                Surface(color = if (sel) Color(0xFFD1FAE5) else Color.White, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) Color(0xFF059669) else Color.LightGray), modifier = Modifier.clickable { selectedAddons = if (sel) selectedAddons - a else selectedAddons + a }) {
                                    Row(modifier = Modifier.padding(12.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = sel, onCheckedChange = { checked -> selectedAddons = if (checked) selectedAddons + a else selectedAddons - a }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669)))
                                        Text(a, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Lead Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray); var expS by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expS, { expS = it }) {
                                OutlinedTextField(value = status, onValueChange = { }, readOnly = true, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expS) }, shape = RoundedCornerShape(8.dp))
                                ExposedDropdownMenu(expS, { expS = false }) { listOf("New", "Contacted", "Qualified", "Proposal", "Won", "Lost").forEach { s -> DropdownMenuItem(text = { Text(text = s) }, onClick = { status = s; expS = false }) } }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Lead Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray); var expT by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expT, { expT = it }) {
                                OutlinedTextField(value = leadType, onValueChange = { }, readOnly = true, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expT) }, shape = RoundedCornerShape(8.dp))
                                ExposedDropdownMenu(expT, { expT = false }) { listOf("Hot", "Cold").forEach { t -> DropdownMenuItem(text = { Text(text = t) }, onClick = { leadType = t; expT = false }) } }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    val finalService = if (serviceCategory == "Tally Product") selectedMainProduct else selectedAddons.joinToString(", ")
                    Button(onClick = {
                        isSubmitting = true
                        scope.launch {
                            try {
                                val res = RetrofitClient.apiService.addAdminLead(mapOf("user_id" to userId.toString(), "action" to "add_lead", "name" to name.trim(), "phone" to phone.trim(), "email" to email.trim(), "company" to company.trim(), "status" to status, "lead_type" to leadType, "service" to finalService))
                                if (res.isSuccessful && res.body()?.asJsonObject?.get("success")?.asBoolean == true) onSuccess("Lead Added Successfully") else onError("Failed to add lead")
                            } catch (e: Exception) { onError(e.message ?: "Error") } finally { isSubmitting = false }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo), enabled = !isSubmitting && name.isNotBlank() && (selectedMainProduct.isNotBlank() || selectedAddons.isNotEmpty())) {
                        if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("Create Lead", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun LField(label: String, value: String, type: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = type), singleLine = true)
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun AdminLeadCard(lead: Lead, onClick: () -> Unit) {
    val statusColor = getStatusColor(lead.status ?: "New")
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = lead.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!lead.company.isNullOrBlank()) Text(text = lead.company, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                    Text(text = (lead.status ?: "New").uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = lead.phone ?: "N/A", fontSize = 12.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = lead.employee_name ?: "Unassigned", fontSize = 12.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!lead.date.isNullOrBlank() && lead.date != "N/A") {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = lead.date, fontSize = 11.sp, color = Color.Gray)
                }
            }
            val reqSummary = lead.formattedRequirementSummary
            if (!reqSummary.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, null, modifier = Modifier.size(14.dp), tint = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = reqSummary, fontSize = 11.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun getStatusColor(s: String): Color = when (s.lowercase()) {
    "won" -> Color(0xFF10B981)
    "lost" -> Color(0xFFEF4444)
    "new" -> Color(0xFF1E3A8A)
    else -> Color(0xFFF59E0B)
}
