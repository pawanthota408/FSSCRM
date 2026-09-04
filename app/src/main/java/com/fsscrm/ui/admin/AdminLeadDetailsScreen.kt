package com.fsscrm.ui.admin

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.fsscrm.network.ActivityTimelineItem
import com.fsscrm.network.CustomerLicense
import com.fsscrm.network.Employee
import com.fsscrm.network.FollowUp
import com.fsscrm.network.LeadDetailsResponse
import com.fsscrm.network.Product
import com.fsscrm.network.ProformaInvoice
import com.fsscrm.network.QuoteDetails
import com.fsscrm.network.RetrofitClient
import com.fsscrm.network.Work
import com.fsscrm.ui.common.CreateNoteFullScreen
import com.fsscrm.ui.common.FollowUpFullScreen
import com.fsscrm.ui.common.LeadAssignmentDialog
import com.fsscrm.ui.common.ProformaDetailFullScreen
import com.fsscrm.ui.common.ProposalFullScreen
import com.fsscrm.ui.common.QuotationFullScreen
import com.fsscrm.ui.common.QuoteDetailFullScreen
import com.fsscrm.ui.common.RescheduleFollowUpDialog
import com.fsscrm.ui.common.StatItem
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.WonFullScreen
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.sales.LicenseDetailView
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeadDetailsScreen(userId: Int, leadId: Int, navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var details by remember { mutableStateOf<LeadDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Details", "Insights", "Files")
    val snackbarHostState = remember { SnackbarHostState() }

    var expandedSection by remember { mutableStateOf<String?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showAssignmentDialog by remember { mutableStateOf(false) }
    var selectedNewStatus by remember { mutableStateOf("") }

    // Full Screen States
    var showFollowUpFullScreen by remember { mutableStateOf(false) }
    var showQuotationFullScreen by remember { mutableStateOf(false) }
    var showProposalFullScreen by remember { mutableStateOf(false) }
    var showWonFullScreen by remember { mutableStateOf(false) }
    var showCreateNoteFullScreen by remember { mutableStateOf(false) }
    var showQuoteDetailFullScreen by remember { mutableStateOf<QuoteDetails?>(null) }
    var showProformaDetailFullScreen by remember { mutableStateOf<ProformaInvoice?>(null) }
    var showRescheduleFollowUp by remember { mutableStateOf<FollowUp?>(null) }

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var selectedLicenseForDetail by remember { mutableStateOf<CustomerLicense?>(null) }
    var currentUser by remember { mutableStateOf<Employee?>(null) }

    suspend fun refreshLead() {
        isLoading = true
        try {
            // ---------- Fetch profile if null ----------
            if (currentUser == null) {
                val profResp = RetrofitClient.apiService.getProfile(userId)
                if (profResp.isSuccessful) {
                    currentUser = profResp.body()?.employee
                }
            }
            // ---------- Fetch lead details ----------
            val response = RetrofitClient.apiService.getAdminLeadDetails(
                mapOf("user_id" to userId, "lead_id" to leadId)
            )
            if (response.isSuccessful) {
                response.toLenientJson()?.let { json ->
                    val parsed = LeadDetailsResponse.fromJson(json)
                    if (parsed.status == "success") {
                        details = parsed
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                parsed.message ?: "Failed to load lead details"
                            )
                        }
                    }
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Server error: ${response.code()}"
                scope.launch { snackbarHostState.showSnackbar(errorMsg) }
            }

            // ---------- Fetch products ----------
            val prodResp = RetrofitClient.apiService.getProducts(mapOf("user_id" to userId))
            if (prodResp.isSuccessful) {
                prodResp.toLenientJson()?.let { pJson ->
                    products = when {
                        pJson.isJsonArray -> pJson.asJsonArray.map { Product.fromJson(it) }
                        pJson.isJsonObject -> {
                            val obj = pJson.asJsonObject
                            val array = when {
                                obj.has("products_services") -> obj.get("products_services")
                                obj.has("products") -> obj.get("products")
                                obj.has("data") -> obj.get("data")
                                else -> obj.entrySet().firstOrNull { it.value.isJsonArray }?.value
                            }
                            if (array != null && array.isJsonArray) {
                                array.asJsonArray.map { Product.fromJson(it) }
                            } else emptyList()
                        }
                        else -> emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar("Error: ${e.message}") }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(leadId) {
        refreshLead()

        // Auto-open modal if requested via deep link
        val args = navController.currentBackStackEntry?.arguments
        if (args?.getString("open_quotation") == "true") {
            showQuotationFullScreen = true
        }
        if (args?.getString("open_followup") == "true") {
            showFollowUpFullScreen = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (selectedLicenseForDetail != null) {
            BackHandler {
                selectedLicenseForDetail = null
            }
            LicenseDetailView(
                userId = userId,
                leadId = leadId,
                license = selectedLicenseForDetail,
                allLicenses = details?.customerLicenses ?: emptyList(),
                products = products,
                snackbarHostState = snackbarHostState,
                padding = padding,
                onBack = { selectedLicenseForDetail = null }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                UniversalHeader(
                    title = "Lead Profile (Admin)",
                    onBackClick = onBack,
                    actions = {
                        IconButton(onClick = { scope.launch { refreshLead() } }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                    }
                )

                // ---- HERO SECTION ----
                Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = (details?.lead?.company_name
                                        ?: details?.lead?.company
                                        ?: details?.customer?.company
                                        ?: if (isLoading) "Loading..." else "No Company").uppercase(),
                                    color = Color(0xFF1E293B),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = details?.lead?.name ?: "",
                                    color = Color(0xFF64748B),
                                    fontSize = 15.sp
                                )
                            }
                            Surface(
                                onClick = { showStatusDialog = true },
                                color = when (details?.lead?.status?.lowercase()) {
                                    "won" -> Color(0xFFDCFCE7)
                                    "lost" -> Color(0xFFFEE2E2)
                                    else -> Color(0xFFFEF3C7)
                                },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
                            ) {
                                Row(Modifier.padding(8.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        (details?.lead?.status ?: "New").uppercase(),
                                        color = when (details?.lead?.status?.lowercase()) {
                                            "won" -> Color(0xFF166534)
                                            "lost" -> Color(0xFF991B1B)
                                            else -> Color(0xFF92400E)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Lead Requirement Card above Action Buttons
                        val reqSummary = details?.lead?.formattedRequirementSummary ?: details?.lead?.service
                        if (!reqSummary.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Requirement: ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = reqSummary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CRMActionButton(icon = Icons.Default.Call, label = "Call", color = Color(0xFF3B82F6)) {
                                val phone = details?.lead?.phone
                                if (!phone.isNullOrBlank()) {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                                }
                            }
                            CRMActionButton(
                                painter = androidx.compose.ui.res.painterResource(id = com.fsscrm.R.drawable.ic_whatsapp),
                                label = "WhatsApp",
                                color = Color(0xFF25D366)
                            ) {
                                val clean = details?.lead?.phone?.replace(Regex("[^0-9]"), "")
                                if (!clean.isNullOrBlank()) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://wa.me/$clean".toUri()))
                                }
                            }
                            CRMActionButton(icon = Icons.Default.PersonAdd, label = "Assign", color = Color(0xFF6366F1)) {
                                showAssignmentDialog = true
                            }
                        }
                    }
                }

                // ---- TABS ----
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E3A8A)
                )
 {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 13.sp) }
                        )
                    }
                }

                // ---- CONTENT ----
                if (isLoading && details == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            0 -> { // Details
                                item {
                                    DetailHeaderItem(
                                        "Assigned To",
                                        details?.lead?.employee_name ?: details?.lead?.assigned_name ?: "Unassigned",
                                        Icons.Default.Badge
                                    ) {}
                                    DetailHeaderItem(
                                        "Lead Source",
                                        details?.lead?.source ?: "Direct",
                                        Icons.Default.Source
                                    ) {}
                                }
                                item { StatsRow(details?.stats) }

                                // Customer Licenses (Filtered for Main Tally Products)
                                val mainTallyLicenses = details?.customerLicenses?.filter { lic ->
                                    val n = (lic.item_name ?: "").lowercase()
                                    // List of non-Tally keywords to exclude
                                    val excludeKeywords = listOf("cloud", "biz", "amc", "tss", "tdl", "whatsapp", "upgrade", "service", "addon", "support")
                                    // It must NOT contain any exclusion keywords AND must contain "tally"
                                    excludeKeywords.none { n.contains(it) } && n.contains("tally")
                                } ?: emptyList()

                                if (mainTallyLicenses.isNotEmpty()) {
                                    item {
                                        CollapsibleSection(
                                            "Existing Customer Assets",
                                            Icons.Default.VpnKey,
                                            expandedSection == "Lic",
                                            mainTallyLicenses.size,
                                            { expandedSection = if (expandedSection == "Lic") null else "Lic" }
                                        ) {
                                            Column(Modifier.padding(horizontal = 16.dp)) {
                                                mainTallyLicenses.forEach { lic ->
                                                    key(lic.id, lic.license_key) {
                                                        LicenseAssetCard(lic) {
                                                            if (!lic.license_key.isNullOrBlank()) {
                                                                expandedSection = null
                                                                selectedLicenseForDetail = lic
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Lead Requirements
                                if (!details?.lead?.service.isNullOrBlank()) {
                                    item {
                                        CollapsibleSection(
                                            "Lead Requirements",
                                            Icons.AutoMirrored.Filled.Assignment,
                                            expandedSection == "Req",
                                            null,
                                            { expandedSection = if (expandedSection == "Req") null else "Req" }
                                        ) {
                                            Column(Modifier.padding(16.dp)) {
                                                val reqs = details?.lead?.formattedRequirements() ?: emptyList()
                                                reqs.forEach { req ->
                                                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            null,
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(" $req", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Follow Ups
                                item {
                                    CollapsibleSection(
                                        "Follow Ups",
                                        Icons.Default.CalendarToday,
                                        expandedSection == "FU",
                                        details?.follow_ups?.size,
                                        { expandedSection = if (expandedSection == "FU") null else "FU" }
                                    ) {
                                        Column(Modifier.padding(horizontal = 16.dp)) {
                                            if (details?.follow_ups.isNullOrEmpty()) {
                                                Text(
                                                    "No follow ups found",
                                                    modifier = Modifier.padding(16.dp),
                                                    color = Color.Gray
                                                )
                                            } else {
                                                details?.follow_ups?.forEach { fu ->
                                                    FollowUpItemWithActions(
                                                        fu,
                                                        onComplete = {
                                                            scope.launch {
                                                                if (RetrofitClient.apiService.updateFollowUpStatus(
                                                                        mapOf(
                                                                            "user_id" to userId.toString(),
                                                                            "follow_up_id" to fu.id.toString(),
                                                                            "status" to "completed",
                                                                            "action" to "complete_followup"
                                                                        )
                                                                    ).isSuccessful
                                                                ) refreshLead()
                                                            }
                                                        },
                                                        onReschedule = { showRescheduleFollowUp = fu }
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = { showFollowUpFullScreen = true },
                                                Modifier.fillMaxWidth()
                                            ) {
                                                Text("Schedule Follow Up")
                                            }
                                        }
                                    }
                                }

                                // Quotes & Proformas
                                item {
                                    CollapsibleSection(
                                        "Quotations \u0026 Proposals",
                                        Icons.Default.Payments,
                                        expandedSection == "Finance",
                                        (details?.quotes?.size ?: 0) + (details?.proformas?.size ?: 0),
                                        { expandedSection = if (expandedSection == "Finance") null else "Finance" }
                                    ) {
                                        Column(Modifier.padding(horizontal = 16.dp)) {
                                            details?.quotes?.forEach { q ->
                                                QuoteItem(q) {
                                                    scope.launch {
                                                        try {
                                                            val resp = RetrofitClient.apiService.getQuoteDetails(
                                                                mapOf("quote_id" to q.id)
                                                            )
                                                            if (resp.isSuccessful) {
                                                                resp.toLenientJson()?.let {
                                                                    showQuoteDetailFullScreen =
                                                                        RetrofitClient.gson.fromJson(it, QuoteDetails::class.java)
                                                                } ?: run {
                                                                    showQuoteDetailFullScreen = q
                                                                }
                                                            } else {
                                                                showQuoteDetailFullScreen = q
                                                            }
                                                        } catch (_: Exception) {
                                                            showQuoteDetailFullScreen = q
                                                        }
                                                    }
                                                }
                                            }
                                            details?.proformas?.forEach { pf ->
                                                ProformaItem(pf) {
                                                    scope.launch {
                                                        try {
                                                            val resp = RetrofitClient.apiService.getProformaDetails(
                                                                mapOf("proforma_id" to pf.id)
                                                            )
                                                            if (resp.isSuccessful) {
                                                                resp.toLenientJson()?.let {
                                                                    showProformaDetailFullScreen =
                                                                        RetrofitClient.gson.fromJson(it, ProformaInvoice::class.java)
                                                                } ?: run {
                                                                    showProformaDetailFullScreen = pf
                                                                }
                                                            } else {
                                                                showProformaDetailFullScreen = pf
                                                            }
                                                        } catch (_: Exception) {
                                                            showProformaDetailFullScreen = pf
                                                        }
                                                    }
                                                }
                                            }
                                            Row {
                                                TextButton(
                                                    onClick = { showQuotationFullScreen = true },
                                                    Modifier.weight(1f)
                                                ) {
                                                    Text("New Quote")
                                                }
                                                TextButton(
                                                    onClick = {
                                                        if (details?.hasApprovedQuote() == true) {
                                                            showProposalFullScreen = true
                                                        } else {
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("Cannot create Proforma Invoice: Quotation must be approved by Admin first.")
                                                            }
                                                        }
                                                    },
                                                    Modifier.weight(1f)
                                                ) {
                                                    Text("New Proforma")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Works
                                item {
                                    CollapsibleSection(
                                        "Active Works",
                                        Icons.Default.Work,
                                        expandedSection == "Works",
                                        details?.works?.size,
                                        { expandedSection = if (expandedSection == "Works") null else "Works" }
                                    ) {
                                        Column(Modifier.padding(horizontal = 16.dp)) {
                                            details?.works?.forEach { work ->
                                                WorkItem(work) {
                                                    navController.navigate("work_details/${work.id}")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Mark as Won button
                                if (details?.lead?.status?.lowercase() != "won") {
                                    item {
                                        Button(
                                            onClick = {
                                                if (details?.hasApprovedProforma() == true) {
                                                    showWonFullScreen = true
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Cannot Mark as Won / Create Work: Proforma Invoice must be approved by Admin first.")
                                                    }
                                                }
                                            },
                                            Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Text("MARK AS WON", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            1 -> { // Insights
                                item { InsightsTimeline(details) }
                                item {
                                    val notes = details?.history?.filter {
                                        it.action?.lowercase() == "note_created"
                                    } ?: emptyList()
                                    CollapsibleSection(
                                        "Notes",
                                        Icons.AutoMirrored.Filled.Notes,
                                        expandedSection == "Notes",
                                        notes.size,
                                        { expandedSection = if (expandedSection == "Notes") null else "Notes" }
                                    ) {
                                        Column(Modifier.padding(horizontal = 16.dp)) {
                                            notes.forEach { note ->
                                                Surface(
                                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                ) {
                                                    Column(Modifier.padding(12.dp)) {
                                                        Row(
                                                            Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                note.date ?: "",
                                                                fontSize = 10.sp,
                                                                color = Color.Gray
                                                            )
                                                            Text(
                                                                "by ${note.employee_name ?: "Admin"}",
                                                                fontSize = 10.sp,
                                                                color = Color.Gray
                                                            )
                                                        }
                                                        Text(note.remarks ?: "", fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                            TextButton(
                                                onClick = { showCreateNoteFullScreen = true },
                                                Modifier.fillMaxWidth()
                                            ) {
                                                Text("Add Note")
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> { // Files
                                item { FilesTabContent() }
                            }
                        }
                    }
                }
            }
        }

        // ---- MODALS ----
        if (showStatusDialog) {
            StatusSelectionDialog(
                details?.lead?.status ?: "",
                { showStatusDialog = false }
            ) { status ->
                showStatusDialog = false
                selectedNewStatus = status
                when (status) {
                    "Follow Up", "Call Back Later" -> showFollowUpFullScreen = true
                    "Quotation" -> showQuotationFullScreen = true
                    "Proposal", "Proforma Invoice" -> {
                        if (details?.hasApprovedQuote() == true) {
                            showProposalFullScreen = true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cannot create Proforma Invoice: Quotation must be approved by Admin first.")
                            }
                        }
                    }
                    "Won" -> {
                        if (details?.hasApprovedProforma() == true) {
                            showWonFullScreen = true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cannot Mark as Won / Create Work: Proforma Invoice must be approved by Admin first.")
                            }
                        }
                    }
                    else -> {
                        scope.launch {
                            if (RetrofitClient.apiService.updateLeadStatus(
                                    mapOf(
                                        "user_id" to userId.toString(),
                                        "lead_id" to leadId.toString(),
                                        "status" to status,
                                        "action" to "update_status"
                                    )
                                ).isSuccessful
                            ) refreshLead()
                        }
                    }
                }
            }
        }

        if (showFollowUpFullScreen) {
            val lead: com.fsscrm.network.Lead? = details?.lead
            if (lead != null) {
                FollowUpFullScreen(
                    lead,
                    { showFollowUpFullScreen = false }
                ) { d: String, t: String, r: String ->
                    scope.launch {
                        if (RetrofitClient.apiService.addFollowUp(
                                mapOf(
                                    "user_id" to userId.toString(),
                                    "lead_id" to leadId.toString(),
                                    "follow_up_date" to d,
                                    "follow_up_time" to t,
                                    "remarks" to r,
                                    "action" to "schedule_follow_up"
                                )
                            ).isSuccessful
                        ) {
                            showFollowUpFullScreen = false
                            refreshLead()
                        }
                    }
                }
            }
        }

        if (showQuotationFullScreen) {
            val lead: com.fsscrm.network.Lead? = details?.lead
            if (lead != null) {
                QuotationFullScreen(
                    lead,
                    products,
                    currentUser,
                    { showQuotationFullScreen = false }
                ) { data: Map<String, String> ->
                    scope.launch {
                        if (RetrofitClient.apiService.updateLeadStatus(
                                data.toMutableMap().apply {
                                    put("user_id", userId.toString())
                                    put("lead_id", leadId.toString())
                                    put("action", "update_followup_stage")
                                    put("stage", "Quotation")
                                }
                            ).isSuccessful
                        ) {
                            showQuotationFullScreen = false
                            refreshLead()
                        }
                    }
                }
            }
        }

        if (showProposalFullScreen) {
            val lead: com.fsscrm.network.Lead? = details?.lead
            if (lead != null) {
                ProposalFullScreen(
                    lead = lead,
                    products = products,
                    latestQuote = details?.quotes?.lastOrNull(),
                    currentUser = currentUser,
                    onDismiss = { showProposalFullScreen = false }
                ) { data: Map<String, String> ->
                    scope.launch {
                        if (RetrofitClient.apiService.updateLeadStatus(
                                data.toMutableMap().apply {
                                    put("user_id", userId.toString())
                                    put("lead_id", leadId.toString())
                                    put("action", "update_followup_stage")
                                    put("stage", "Proposal")
                                }
                            ).isSuccessful
                        ) {
                            showProposalFullScreen = false
                            refreshLead()
                        }
                    }
                }
            }
        }

        if (showWonFullScreen) {
            val lead: com.fsscrm.network.Lead? = details?.lead
            if (lead != null) {
                WonFullScreen(
                    lead,
                    details?.proformas?.firstOrNull(),
                    { showWonFullScreen = false }
                ) { data: Map<String, String> ->
                    scope.launch {
                        if (RetrofitClient.apiService.updateLeadStatus(
                                data.toMutableMap().apply {
                                    put("user_id", userId.toString())
                                    put("lead_id", leadId.toString())
                                    put("action", "update_followup_stage")
                                    put("stage", "Won")
                                }
                            ).isSuccessful
                        ) {
                            showWonFullScreen = false
                            refreshLead()
                        }
                    }
                }
            }
        }

        if (showCreateNoteFullScreen) {
            CreateNoteFullScreen(
                { showCreateNoteFullScreen = false }
            ) { n: String ->
                scope.launch {
                    if (RetrofitClient.apiService.updateActivity(
                            mapOf(
                                "user_id" to userId.toString(),
                                "lead_id" to leadId.toString(),
                                "action" to "note_created",
                                "remarks" to n
                            )
                        ).isSuccessful
                    ) {
                        showCreateNoteFullScreen = false
                        refreshLead()
                    }
                }
            }
        }

        val fuResch: com.fsscrm.network.FollowUp? = showRescheduleFollowUp
        if (fuResch != null) {
            RescheduleFollowUpDialog(
                fuResch,
                { showRescheduleFollowUp = null }
            ) { d, t, r ->
                scope.launch {
                    if (RetrofitClient.apiService.rescheduleFollowUp(
                            mapOf(
                                "user_id" to userId.toString(),
                                "follow_up_id" to fuResch.id.toString(),
                                "new_date" to d,
                                "new_time" to t,
                                "remarks" to r
                            )
                        ).isSuccessful
                    ) {
                        showRescheduleFollowUp = null
                        refreshLead()
                    }
                }
            }
        }

        val qDet: com.fsscrm.network.QuoteDetails? = showQuoteDetailFullScreen
        if (qDet != null) {
            QuoteDetailFullScreen(qDet) {
                showQuoteDetailFullScreen = null
            }
        }

        val pfDet: com.fsscrm.network.ProformaInvoice? = showProformaDetailFullScreen
        if (pfDet != null) {
            ProformaDetailFullScreen(pfDet) {
                showProformaDetailFullScreen = null
            }
        }

        if (showAssignmentDialog) {
            LeadAssignmentDialog(
                userId = userId,
                leadId = leadId,
                initialEmployees = details?.employees,
                onDismiss = { showAssignmentDialog = false },
                onAssigned = {
                    showAssignmentDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Lead assigned successfully")
                        refreshLead()
                    }
                }
            )
        }
    }
}

// ---------- Helper Composables (unchanged) ----------

@Composable
fun RowScope.CRMActionButton(
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(46.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            } else if (painter != null) {
                Icon(painter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = Color(0xFF1E293B),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FollowUpItemWithActions(
    fu: FollowUp,
    onComplete: () -> Unit,
    onReschedule: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (fu.status == "completed") Icons.Default.CheckCircle else Icons.Default.Schedule,
                    null,
                    tint = if (fu.status == "completed") Color(0xFF10B981) else Color(0xFFF59E0B)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${fu.follow_up_date} ${fu.follow_up_time ?: ""}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(fu.remarks ?: "", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(
                    color = if (fu.status == "completed") Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        fu.status.uppercase(),
                        Modifier.padding(6.dp, 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (fu.status == "completed") Color(0xFF166534) else Color(0xFF92400E)
                    )
                }
            }
            if (fu.status == "pending") {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onComplete,
                        Modifier.weight(1f).height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("COMPLETE", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = onReschedule,
                        Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("RESCHEDULE", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LicenseAssetCard(lic: CustomerLicense, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Verified, null, tint = Color(0xFF4F46E5))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lic.item_name ?: "Product", fontWeight = FontWeight.Bold)
                Text(lic.license_key ?: "", fontSize = 12.sp, color = Color.Gray)
            }
            Surface(
                color = if (lic.status == "active") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    lic.status?.uppercase() ?: "",
                    Modifier.padding(6.dp, 2.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (lic.status == "active") Color(0xFF166534) else Color(0xFF991B1B)
                )
            }
        }
    }
}

@Composable
fun DetailHeaderItem(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, Modifier.fillMaxWidth(), color = Color.White) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Icon(icon, null, tint = Color.LightGray)
        }
    }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
fun StatsRow(stats: Map<String, Any>?) {
    if (stats == null) return
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        fun toInt(v: Any?): Int = when(v) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: 0
            else -> 0
        }
        fun toDouble(v: Any?): Double = when(v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        val totalProformas = toInt(stats["total_proformas"])
        val activeWorks = toInt(stats["active_works"])
        val totalPaid = toDouble(stats["total_paid"])

        StatItem("Quotes", totalProformas.toString(), Color(0xFFF59E0B))
        StatItem("Works", activeWorks.toString(), Color(0xFF3B82F6))
        StatItem("Paid", "₹${String.format(Locale.US, "%.2f", totalPaid)}", Color(0xFF10B981))
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    badgeCount: Int? = null,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onToggle,
        Modifier.fillMaxWidth().padding(top = 1.dp),
        color = Color.White
    ) {
        Column {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF1E3A8A))
                Spacer(Modifier.width(16.dp))
                Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                if (badgeCount != null && badgeCount > 0) {
                    Surface(
                        color = Color(0xFF1E3A8A).copy(0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            badgeCount.toString(),
                            Modifier.padding(6.dp, 2.dp),
                            color = Color(0xFF1E3A8A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = Color.Gray
                )
            }
            if (isExpanded) content()
        }
    }
}

@Composable
fun InsightsTimeline(details: LeadDetailsResponse?) {
    Column(Modifier.padding(16.dp)) {
        Text("Activity Timeline", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        details?.history?.forEach { TimelineItem(it) }
    }
}

@Composable
fun TimelineItem(item: ActivityTimelineItem) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(
                Modifier.size(24.dp).background(Color(0xFFEEF2FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.History,
                    null,
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(Modifier.width(1.dp).weight(1f).background(Color(0xFFE2E8F0)))
        }
        Column(Modifier.weight(1f).padding(bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    item.action?.uppercase() ?: "EVENT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF1E3A8A)
                )
                Text(item.date ?: "", fontSize = 9.sp, color = Color.Gray)
            }
            if (!item.remarks.isNullOrEmpty()) {
                Text(item.remarks, fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun FilesTabContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FolderOpen, null, Modifier.size(64.dp), tint = Color.LightGray)
            Text("No Files Found", color = Color.Gray)
        }
    }
}

@Composable
fun StatusSelectionDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onStatusSelect: (String) -> Unit
) {
    val statuses = listOf("New", "Follow Up", "Call Back Later", "Quotation", "Demo Given", "Won", "Lost")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Status") },
        text = {
            Column {
                statuses.forEach { status ->
                    TextButton(
                        onClick = { onStatusSelect(status) },
                        Modifier.fillMaxWidth()
                    ) {
                        Text(
                            status,
                            color = if (status == currentStatus) Color(0xFF1E3A8A) else Color.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuoteItem(quote: QuoteDetails, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(quote.quote_no.ifBlank { "Q-${quote.id}" }, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(4.dp)) {
                        Text("Quotation", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                    }
                }
                if (!quote.created_at.isNullOrBlank()) {
                    Text(quote.created_at.take(10), fontSize = 11.sp, color = Color.Gray)
                }
            }
            Text("₹${quote.total}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
        }
    }
}

@Composable
fun ProformaItem(pf: ProformaInvoice, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(pf.proforma_no.ifBlank { "PF-${pf.id}" }, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(4.dp)) {
                        Text("Proforma Invoice", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold)
                    }
                }
                if (!pf.created_at.isNullOrBlank()) {
                    Text(pf.created_at.take(10), fontSize = 11.sp, color = Color.Gray)
                }
            }
            Text("₹${pf.total}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
        }
    }
}

@Composable
fun WorkItem(work: Work, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(work.work_name.ifBlank { work.description?.take(30) ?: "Project #${work.id}" }, fontWeight = FontWeight.Bold)
                if (!work.start_date.isNullOrBlank()) {
                    Text(work.start_date, fontSize = 11.sp, color = Color.Gray)
                }
            }
            Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp)) {
                Text(work.status.uppercase(), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}