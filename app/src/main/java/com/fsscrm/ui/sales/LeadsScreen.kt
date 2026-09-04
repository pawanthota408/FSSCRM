package com.fsscrm.ui.sales

import androidx.core.net.toUri
import com.fsscrm.ui.common.*

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fsscrm.network.*
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    userId: Int,
    onMenuClick: () -> Unit,
    onLeadClick: (Int) -> Unit,
    onWorkClick: (Int) -> Unit,
    onCreateLead: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var leadFilter by remember { mutableStateOf("All") }
    var followUpFilter by remember { mutableStateOf("All") }
    var workFilter by remember { mutableStateOf("All") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var allLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var leadCounts by remember { mutableStateOf<LeadCounts?>(null) }
    var works by remember { mutableStateOf<List<Work>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showFollowUpModal by remember { mutableStateOf<Lead?>(null) }
    var showQuotationModal by remember { mutableStateOf<Lead?>(null) }
    var showProformaModal by remember { mutableStateOf<Lead?>(null) }
    var showWonModal by remember { mutableStateOf<Lead?>(null) }
    var showUpdateStatusModal by remember { mutableStateOf<Work?>(null) }
    var showAddPaymentModal by remember { mutableStateOf<Work?>(null) }
    var showCompleteWorkModal by remember { mutableStateOf<Work?>(null) }
    var showMessageChoiceByLead by remember { mutableStateOf<Lead?>(null) }
    var currentUser by remember { mutableStateOf<Employee?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch {
            try {
                if (currentUser == null) {
                    val profResp = RetrofitClient.apiService.getProfile(userId)
                    if (profResp.isSuccessful) {
                        currentUser = profResp.body()?.employee
                    }
                }
                val leadsResp = RetrofitClient.apiService.getLeads(mapOf("user_id" to userId))
                if (leadsResp.isSuccessful) {
                    leadsResp.toLenientJson()?.let {
                        val response = LeadResponse.fromJson(it)
                        allLeads = response.leads
                        leadCounts = response.counts
                    }
                }
                
                val worksResp = RetrofitClient.apiService.getWorks(mapOf("user_id" to userId))
                if (worksResp.isSuccessful) {
                    worksResp.toLenientJson()?.let {
                        works = WorkResponse.fromJson(it).works
                    }
                }

                val prodResp = RetrofitClient.apiService.getProducts(mapOf("user_id" to userId))
                if (prodResp.isSuccessful) {
                    prodResp.toLenientJson()?.let {
                        products = ProductResponse.fromJson(it).products
                    }
                }
            } catch (e: Exception) { snackbarHostState.showSnackbar("Error: ${e.message}") } finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    // Filtering logic based on website
    val leadsStatuses = listOf("New", "Switch off", "Not Answering", "Not Interested", "Meeting Scheduled")
    val followUpStatuses = listOf("Follow Up", "Quotation", "Demo Given", "Proposal", "Ready To Buy")
    val workStatuses = listOf("pending", "in_progress", "completed")

    val leadsList = allLeads.filter { it.status in leadsStatuses }.filter { if(leadFilter == "All") true else it.status == leadFilter }
    val followUpsList = allLeads.filter { it.status in followUpStatuses }.filter { if(followUpFilter == "All") true else it.status == followUpFilter }
    val activeWorks = works.filter { if(workFilter == "All") true else it.status == workFilter }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color(0xFFF8FAFC)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(title = "Lead Management", onMenuClick = onMenuClick, actions = { IconButton(onClick = onCreateLead) { Icon(Icons.Default.AddCircle, null, tint = Color.White, modifier = Modifier.size(28.dp)) } })
            
            // Web-style stat cards
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Active", (leadCounts?.all ?: allLeads.count { it.status in leadsStatuses }).toString(), Icons.Default.People, Color(0xFF0369A1))
                StatCard(Modifier.weight(1f), "Process", (if (leadCounts != null) (leadCounts!!.contacted + leadCounts!!.qualified + leadCounts!!.proposal) else allLeads.count { it.status in followUpStatuses }).toString(), Icons.AutoMirrored.Filled.RotateRight, Color(0xFF7E22CE))
                StatCard(Modifier.weight(1f), "Deals", (leadCounts?.won ?: allLeads.count { it.status == "Won" }).toString(), Icons.Default.EmojiEvents, Color(0xFF15803D))
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = Color(0xFF1E3A8A), divider = {}) {
                listOf("Leads", "Follow Ups", "Works").forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold) }) }
            }

            // Filter Strip
            val currentFilters = when(selectedTab) {
                0 -> listOf("All") + leadsStatuses
                1 -> listOf("All") + followUpStatuses
                else -> listOf("All") + workStatuses.map { it.uppercase() }
            }
            val currentSelectedFilter = when(selectedTab) { 0 -> leadFilter; 1 -> followUpFilter; else -> workFilter }

            ScrollableTabRow(
                selectedTabIndex = currentFilters.indexOf(currentSelectedFilter).coerceAtLeast(0),
                containerColor = Color.White,
                contentColor = Color(0xFF64748B),
                edgePadding = 16.dp,
                divider = {},
                indicator = {}
            ) {
                currentFilters.forEach { f ->
                    val isSelected = f == currentSelectedFilter
                    Tab(
                        selected = isSelected,
                        onClick = { when(selectedTab) { 0 -> leadFilter = f; 1 -> followUpFilter = f; else -> workFilter = f.lowercase() } },
                        text = {
                            Surface(
                                color = if(isSelected) Color(0xFF1E3A8A) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                                border = if(isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text(f, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = if(isSelected) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && allLeads.isEmpty()) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                else {
                    when (selectedTab) {
                        0 -> LeadsList(leadsList, onLeadClick, { showMessageChoiceByLead = it }, { l, s -> handleStatusUpdate(l, s, userId, scope, snackbarHostState, { loadData() }, { showFollowUpModal = it }, { showQuotationModal = it }, { showProformaModal = it }, { showWonModal = it }) })
                        1 -> LeadsList(followUpsList, onLeadClick, { showMessageChoiceByLead = it }, { l, s -> handleStatusUpdate(l, s, userId, scope, snackbarHostState, { loadData() }, { showFollowUpModal = it }, { showQuotationModal = it }, { showProformaModal = it }, { showWonModal = it }) })
                        2 -> WorksList(activeWorks, onWorkClick, { showUpdateStatusModal = it }, { showAddPaymentModal = it }, { showCompleteWorkModal = it })
                    }
                }
            }
        }

        // Modals from CRMModals.kt
        if (showFollowUpModal != null) { FollowUpFullScreen(showFollowUpModal!!, { showFollowUpModal = null }, { d, t, r -> scope.launch { if(RetrofitClient.apiService.addFollowUp(mapOf("user_id" to userId.toString(), "lead_id" to showFollowUpModal!!.id.toString(), "follow_up_date" to d, "follow_up_time" to t, "remarks" to r, "action" to "schedule_follow_up")).isSuccessful) { loadData(); showFollowUpModal = null } } }) }
        if (showQuotationModal != null) { QuotationFullScreen(showQuotationModal!!, products, currentUser, { showQuotationModal = null }, { data -> scope.launch { if(RetrofitClient.apiService.updateLeadStatus(data.toMutableMap().apply { put("user_id", userId.toString()); put("lead_id", showQuotationModal!!.id.toString()); put("action", "update_followup_stage"); put("stage", "Quotation") }).isSuccessful) { loadData(); showQuotationModal = null } } }) }
        if (showProformaModal != null) { ProposalFullScreen(showProformaModal!!, products, null, currentUser, { showProformaModal = null }, { data -> scope.launch { if(RetrofitClient.apiService.updateLeadStatus(data.toMutableMap().apply { put("user_id", userId.toString()); put("lead_id", showProformaModal!!.id.toString()); put("action", "update_followup_stage"); put("stage", "Proposal") }).isSuccessful) { loadData(); showProformaModal = null } } }) }
        if (showWonModal != null) { WonFullScreen(showWonModal!!, null, { showWonModal = null }, { data -> scope.launch { if(RetrofitClient.apiService.updateLeadStatus(data.toMutableMap().apply { put("user_id", userId.toString()); put("lead_id", showWonModal!!.id.toString()); put("action", "update_followup_stage"); put("stage", "Won") }).isSuccessful) { loadData(); showWonModal = null } } }) }
        if (showUpdateStatusModal != null) { UpdateWorkStatusModal(showUpdateStatusModal!!, { showUpdateStatusModal = null }, { id, st, rem, _, _ -> scope.launch { if(RetrofitClient.apiService.updateWorkStatus(mapOf("user_id" to userId.toString(), "work_id" to id.toString(), "status" to st, "remarks" to rem)).isSuccessful) { loadData(); showUpdateStatusModal = null } } }) }
        if (showAddPaymentModal != null) { AddPaymentModal(showAddPaymentModal!!, { showAddPaymentModal = null }, { id, amt, meth, typ, date, note, txn -> scope.launch { if(RetrofitClient.apiService.addWorkPayment(mapOf("user_id" to userId.toString(), "work_id" to id.toString(), "amount" to amt.toString(), "payment_method" to meth, "payment_type" to typ, "payment_date" to date, "notes" to note, "transaction_id" to txn)).isSuccessful) { loadData(); showAddPaymentModal = null } } }) }
        if (showCompleteWorkModal != null) { CompleteWorkModal(showCompleteWorkModal!!, (showCompleteWorkModal!!.customer_id != null && showCompleteWorkModal!!.customer_id != 0), { showCompleteWorkModal = null }, { id, ser, comp, date, notes, exp, em, pass -> scope.launch { if(RetrofitClient.apiService.completeWork(CompleteWorkRequest(userId, id, ser, comp, date, notes, exp, em, pass)).isSuccessful) { loadData(); showCompleteWorkModal = null } } }) }
        if (showMessageChoiceByLead != null) { AlertDialog(onDismissRequest = { showMessageChoiceByLead = null }, title = { Text("Contact") }, text = { Text("How would you like to contact?") }, confirmButton = { Button(onClick = { 
            val clean = showMessageChoiceByLead?.phone?.replace("[^0-9]".toRegex(), "")
            if (!clean.isNullOrBlank()) {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://wa.me/$clean".toUri()))
            }
            showMessageChoiceByLead = null 
        }) { Text("WhatsApp") } }, dismissButton = { OutlinedButton(onClick = { 
            val phone = showMessageChoiceByLead?.phone
            if (!phone.isNullOrBlank()) {
                context.startActivity(Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri()))
            }
            showMessageChoiceByLead = null 
        }) { Text("SMS") } }) }
    }
}

private fun handleStatusUpdate(lead: Lead, newStatus: String, userId: Int, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState, loadData: () -> Unit, showFollowUpModal: (Lead?) -> Unit, showQuotationModal: (Lead?) -> Unit, showProformaModal: (Lead?) -> Unit, showWonModal: (Lead?) -> Unit) {
    when (newStatus) {
        "Follow Up", "Call Back Later" -> showFollowUpModal(lead)
        "Quotation" -> showQuotationModal(lead)
        "Proforma Invoice", "Proposal" -> showProformaModal(lead)
        "Won" -> showWonModal(lead)
        else -> { scope.launch { if(RetrofitClient.apiService.updateLeadStatus(mapOf("user_id" to userId.toString(), "lead_id" to lead.id.toString(), "status" to newStatus, "action" to "update_status")).isSuccessful) { loadData(); snackbarHostState.showSnackbar("Status updated") } } }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, iconColor: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp)); Spacer(Modifier.height(8.dp)); Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B)); Text(label, fontSize = 11.sp, color = Color(0.6f, 0.6f, 0.6f, 1f))
        }
    }
}

@Composable
fun LeadsList(leads: List<Lead>, onLeadClick: (Int) -> Unit, onMessageClick: (Lead) -> Unit, onStatusUpdate: (Lead, String) -> Unit) {
    if (leads.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No leads", color = Color.Gray) }
    else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(leads) { lead -> LeadCard(lead, { onLeadClick(lead.id) }, { onMessageClick(lead) }, onStatusUpdate) }
    }
}

@Composable
fun LeadCard(lead: Lead, onClick: () -> Unit, onMessageClick: () -> Unit, onStatusUpdate: (Lead, String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp)) { Text(text = (lead.status ?: "New").uppercase(), modifier = Modifier.padding(8.dp, 2.dp), color = Color(0xFF475569), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                Text(lead.date, fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp)); Text(text = lead.safeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(text = lead.company ?: "No Company", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            val reqSummary = lead.formattedRequirementSummary
            if (!reqSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Req: $reqSummary", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = Color(0xFFF1F5F9)); Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { /* call */ }, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) { Text("Call", fontSize = 11.sp) }
                OutlinedButton(onClick = onMessageClick, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) { Text("WhatsApp", fontSize = 11.sp) }
                Box(modifier = Modifier.weight(1.2f)) {
                    Button(onClick = { showMenu = true }, modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) { Text("Action", fontSize = 11.sp); Icon(Icons.Default.ArrowDropDown, null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) { listOf("New", "Follow Up", "Quotation", "Won").forEach { st -> DropdownMenuItem(text = { Text(st) }, onClick = { showMenu = false; onStatusUpdate(lead, st) }) } }
                }
            }
        }
    }
}

@Composable
fun WorksList(works: List<Work>, onWorkClick: (Int) -> Unit, onUpdateStatus: (Work) -> Unit, onAddPayment: (Work) -> Unit, onCompleteWork: (Work) -> Unit) {
    if (works.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No active works", color = Color.Gray) }
    else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(works) { work -> WorkCard(work, { onWorkClick(work.id) }, { onUpdateStatus(work) }, { onAddPayment(work) }, { onCompleteWork(work) }) }
    }
}

@Composable
fun WorkCard(work: Work, onClick: () -> Unit, onUpdateStatus: () -> Unit, onAddPayment: () -> Unit, onCompleteWork: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(4.dp)) { Text(text = work.status.uppercase(), modifier = Modifier.padding(6.dp, 2.dp), color = Color(0xFF92400E), fontSize = 9.sp, fontWeight = FontWeight.Black) } }
            Spacer(modifier = Modifier.height(12.dp)); Text(work.work_name.ifBlank { work.description?.take(30) ?: "Project #${work.id}" }, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(work.customer_name ?: work.lead_name ?: work.lead_company ?: "Customer / Lead", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUpdateStatus, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Status", fontSize = 11.sp) }
                OutlinedButton(onClick = onAddPayment, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Payment", fontSize = 11.sp) }
                Button(onClick = onCompleteWork, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Handover", fontSize = 11.sp) }
            }
        }
    }
}
