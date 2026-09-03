package com.fsscrm.ui.admin

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "AdminInvoices"

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODELS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class InvoiceItem(
    val id: Int = 0,
    val referenceNo: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val customerCompany: String? = null,
    val amount: String = "0",
    val tax: String = "0",
    val discount: String = "0",
    val total: String = "0",
    val status: String = "Unpaid",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val createdByName: String? = null,
    val type: String = "proforma",          // always proforma now
    val proformaId: Int? = null,
    val quoteId: Int? = null,
    val approvedBy: Int? = null,
    val approvedAt: String? = null,
    val sentToCustomerAt: String? = null,
    val adminNotes: String? = null,
    val itemsJson: String? = null
)

data class InvoiceLineItem(
    val name: String = "",
    val details: String = "",
    val quantity: String = "1",
    val rate: String = "0",
    val amount: String = "0"
)

data class InvoiceStats(
    val total: Int = 0,
    val paid: Int = 0,
    val unpaid: Int = 0,
    val overdue: Int = 0,
    val draft: Int = 0,
    val cancelled: Int = 0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val pendingProformas: Int = 0,
    val acceptedProformas: Int = 0
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PARSER HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun JsonObject.optStr(name: String): String? =
    this.get(name)?.takeIf { !it.isJsonNull }?.let { if (it.isJsonPrimitive) it.asString else null }

private fun parseInvoice(o: JsonObject): InvoiceItem {
    return InvoiceItem(
        id = o.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
        referenceNo = o.optStr("reference_no")
            ?: o.optStr("invoice_no")
            ?: o.optStr("proforma_no")
            ?: "",
        customerName = o.optStr("customer_name") ?: "",
        customerEmail = o.optStr("customer_email") ?: "",
        customerPhone = o.optStr("customer_phone") ?: "",
        customerCompany = o.optStr("customer_company"),
        amount = o.optStr("amount") ?: "0",
        tax = o.optStr("tax") ?: "0",
        discount = o.optStr("discount") ?: "0",
        total = o.optStr("total") ?: "0",
        status = o.optStr("status") ?: "Unpaid",
        createdAt = o.optStr("created_at"),
        updatedAt = o.optStr("updated_at"),
        createdByName = o.optStr("created_by_name"),
        type = o.optStr("type") ?: "proforma",
        proformaId = o.get("proforma_id")?.takeIf { !it.isJsonNull }?.asInt,
        quoteId = o.get("quote_id")?.takeIf { !it.isJsonNull }?.asInt,
        approvedBy = o.get("approved_by")?.takeIf { !it.isJsonNull }?.asInt,
        approvedAt = o.optStr("approved_at"),
        sentToCustomerAt = o.optStr("sent_to_customer_at"),
        adminNotes = o.optStr("admin_notes"),
        itemsJson = o.optStr("items")
    )
}

private fun parseLineItems(itemsField: Any?): List<InvoiceLineItem> {
    if (itemsField == null) return emptyList()

    val arr: JsonArray = when (itemsField) {
        is JsonArray -> itemsField
        is String -> try {
            com.google.gson.JsonParser.parseString(itemsField).asJsonArray
        } catch (_: Exception) {
            return emptyList()
        }
        else -> return emptyList()
    }

    return arr.map { el ->
        val o = el.asJsonObject
        InvoiceLineItem(
            name = o.optStr("requirement")
                ?: o.optStr("name")
                ?: o.optStr("description")
                ?: o.optStr("item")
                ?: o.optStr("service")
                ?: "",
            details = o.optStr("details") ?: o.optStr("remarks") ?: o.optStr("specifications") ?: "",
            quantity = o.optStr("quantity") ?: o.optStr("qty") ?: "1",
            rate = o.optStr("rate") ?: o.optStr("unit_price") ?: o.optStr("cost") ?: "0",
            amount = o.optStr("amount") ?: o.optStr("total") ?: o.optStr("cost") ?: "0"
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MAIN SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInvoicesScreen(
    userId: Int,
    onMenuClick: () -> Unit
) {
    var allItems by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var stats by remember { mutableStateOf(InvoiceStats()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Invoices (Approved), 1 = Pending Proformas
    var selectedStatus by remember { mutableStateOf("All") }
    var searchInput by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf("") }

    var previewItem by remember { mutableStateOf<InvoiceItem?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetch() {
        if (userId <= 0) {
            errorMessage = "user_id required"
            isLoading = false
            return
        }
        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val params = mutableMapOf(
                    "user_id" to userId.toString(),
                    "search" to activeSearch
                )
                if (selectedStatus != "All") {
                    params["status"] = selectedStatus
                }

                val response = RetrofitClient.apiService.getAdminInvoices(params)

                if (response.isSuccessful) {
                    response.toLenientJson()?.asJsonObject?.let { root ->
                        if (root.get("success")?.asBoolean != true) {
                            errorMessage = root.get("error")?.asString ?: "Failed to load"
                            return@let
                        }

                        allItems = root.getAsJsonArray("invoices")
                            ?.map { parseInvoice(it.asJsonObject) }
                            ?: emptyList()

                        root.getAsJsonObject("stats")?.let { s ->
                            stats = InvoiceStats(
                                total = s.get("total")?.asInt ?: 0,
                                paid = s.get("paid")?.asInt ?: 0,
                                unpaid = s.get("unpaid")?.asInt ?: 0,
                                overdue = s.get("overdue")?.asInt ?: 0,
                                draft = s.get("draft")?.asInt ?: 0,
                                cancelled = s.get("cancelled")?.asInt ?: 0,
                                paidAmount = s.get("paid_amount")?.asDouble ?: 0.0,
                                dueAmount = s.get("due_amount")?.asDouble ?: 0.0,
                                pendingProformas = s.get("pending_proformas")?.asInt ?: 0,
                                acceptedProformas = s.get("accepted_proformas")?.asInt ?: 0
                            )
                        }
                    }
                } else {
                    errorMessage = "Server Error ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch error", e)
                errorMessage = e.message ?: "Network error"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedStatus, activeSearch) { fetch() }

    // ★★★ UPDATED FILTERING ★★★
    // Tab 0 → Approved Proformas (shown as Invoices)
    val invoices = remember(allItems) {
        allItems.filter {
            it.status.equals("approved", ignoreCase = true)
        }
    }

    // Tab 1 → Pending Proformas
    val pendingProformas = remember(allItems) {
        allItems.filter {
            it.status.equals("pending_approval", ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            UniversalHeader(
                title = "Invoices & Billing",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { fetch() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Stats
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMini("Pending", stats.pendingProformas, Color(0xFF1E3A5F), Modifier.weight(1f))
                StatMini("Approved", stats.acceptedProformas, Color(0xFF065F46), Modifier.weight(1f))
                StatMini("Total", stats.total, Color(0xFF374151), Modifier.weight(1f))
            }

            // Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryIndigo
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Invoices (${invoices.size})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pending Proformas (${pendingProformas.size})", fontSize = 13.sp) }
                )
            }

            // Filter bar
            Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val statusOptions = listOf("All", "pending_approval", "approved", "rejected", "Cancelled")

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.weight(0.38f)
                    ) {
                        OutlinedTextField(
                            value = selectedStatus,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 13.sp) },
                                    onClick = {
                                        selectedStatus = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.weight(0.42f),
                        placeholder = { Text("Customer / Proforma #", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = { activeSearch = searchInput.trim() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    }
                }
            }

            val currentList = if (selectedTab == 0) invoices else pendingProformas

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { fetch() }) { Text("Retry") }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentList, key = { "${it.type}_${it.id}" }) { item ->
                        AdminInvoiceCard(
                            item = item,
                            onClick = { previewItem = item }
                        )
                    }
                    if (currentList.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (selectedTab == 0) "No approved invoices found" else "No pending proformas",
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Preview Dialog
    previewItem?.let { item ->
        InvoicePreviewDialog(
            item = item,
            userId = userId,
            onDismiss = { previewItem = null },
            onActionDone = {
                previewItem = null
                fetch()   // Auto refresh → moves to Invoices tab after Approve
            }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// API CALLER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private suspend fun postInvoiceAction(params: Map<String, String>): Triple<Boolean, String?, JsonObject?> {
    return try {
        val resp = RetrofitClient.apiService.adminInvoicesPost(params)
        if (resp.isSuccessful) {
            val body = resp.toLenientJson()?.asJsonObject
            val ok = body?.get("success")?.asBoolean == true
            Triple(ok, body?.get("message")?.asString ?: body?.get("error")?.asString, body)
        } else Triple(false, "Server ${resp.code()}", null)
    } catch (e: Exception) {
        Triple(false, e.message, null)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PREVIEW DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewDialog(
    item: InvoiceItem,
    userId: Int,
    onDismiss: () -> Unit,
    onActionDone: () -> Unit
) {
    var current by remember { mutableStateOf(item) }
    var lineItems by remember { mutableStateOf<List<InvoiceLineItem>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Load full details + items
    LaunchedEffect(item.id, item.type) {
        isLoadingDetails = true
        try {
            val (success, _, body) = postInvoiceAction(
                mapOf(
                    "user_id" to userId.toString(),
                    "action" to "get_detail",
                    "invoice_id" to item.id.toString(),
                    "type" to "proforma"
                )
            )
            if (success && body != null) {
                val invObj = body.getAsJsonObject("invoice")
                if (invObj != null) {
                    current = parseInvoice(invObj).copy(type = "proforma")
                }

                val itemsArr = body.getAsJsonArray("items")
                lineItems = if (itemsArr != null && itemsArr.size() > 0) {
                    parseLineItems(itemsArr)
                } else {
                    parseLineItems(invObj?.get("items") ?: current.itemsJson)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load invoice detail", e)
        } finally {
            isLoadingDetails = false
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.96f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(Modifier.fillMaxSize()) {

                // Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(PrimaryIndigo)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Proforma #${current.referenceNo}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "${current.customerName.ifBlank { "—" }} · ₹${formatInr(current.total)}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { if (!isSubmitting) onDismiss() }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                // Action icons
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(current.status, Modifier.weight(1f))

                    IconButton(
                        onClick = { shareInvoiceAsText(current, lineItems, context) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                    ) {
                        Icon(Icons.Default.Share, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    exportInvoiceAsPdf(current, lineItems, context)
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEDE9FE))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                    }
                }

                if (isLoadingDetails) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                } else {
                    // Content
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(18.dp)) {

                                // Title
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "PROFORMA INVOICE",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF111827)
                                        )
                                        Text(
                                            "#${current.referenceNo}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("PREPARED BY", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        Text(
                                            current.createdByName ?: "Employee",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF374151)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFE5E7EB))
                                Spacer(Modifier.height(16.dp))

                                // Bill To
                                Text("BILL TO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                                Spacer(Modifier.height(4.dp))
                                Text(current.customerName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                if (!current.customerCompany.isNullOrBlank()) {
                                    Text(current.customerCompany!!, fontSize = 12.sp, color = Color(0xFF4B5563))
                                }
                                if (current.customerEmail.isNotBlank()) {
                                    Text(current.customerEmail, fontSize = 12.sp, color = Color(0xFF6B7280))
                                }
                                if (current.customerPhone.isNotBlank()) {
                                    Text(current.customerPhone, fontSize = 12.sp, color = Color(0xFF6B7280))
                                }

                                Spacer(Modifier.height(18.dp))

                                // Items Header
                                Text("ITEMS / SERVICES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                                Spacer(Modifier.height(8.dp))

                                // Table Header
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.width(22.dp))
                                    Text("Description", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.weight(1f))
                                    Text("Amount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                                }

                                if (lineItems.isEmpty()) {
                                    Text(
                                        "No items found",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    lineItems.forEachIndexed { idx, line ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                "${idx + 1}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF6B7280),
                                                modifier = Modifier.width(22.dp)
                                            )
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    line.name.ifBlank { "Item ${idx + 1}" },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF111827)
                                                )
                                                if (line.details.isNotBlank()) {
                                                    Text(
                                                        line.details,
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF6B7280)
                                                    )
                                                }
                                            }
                                            Text(
                                                "₹${formatInr(line.amount)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF111827),
                                                modifier = Modifier.width(90.dp),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        HorizontalDivider(color = Color(0xFFF3F4F6))
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Totals
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                    TotalsRow("Subtotal", current.amount)
                                    if ((current.discount.toDoubleOrNull() ?: 0.0) > 0) {
                                        TotalsRow("Discount", "-${formatInr(current.discount)}", color = Color(0xFFDC2626))
                                    }
                                    TotalsRow("Tax", current.tax)
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier
                                            .background(PrimaryIndigo, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("TOTAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                                            Spacer(Modifier.width(12.dp))
                                            Text("₹${formatInr(current.total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        }
                                    }
                                }

                                if (!current.adminNotes.isNullOrBlank()) {
                                    Spacer(Modifier.height(14.dp))
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text("ADMIN NOTES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                            Text(current.adminNotes!!, fontSize = 10.sp, color = Color(0xFF78350F))
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Signature
                                Row(Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Customer Signature", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        Box(
                                            Modifier
                                                .fillMaxWidth(0.7f)
                                                .height(0.5.dp)
                                                .background(Color(0xFF9CA3AF))
                                                .padding(top = 16.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("Authorised Signatory", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        Text("Friends Software Solutions", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            // Pending Proforma → Approve / Reject
                            current.status.equals("pending_approval", ignoreCase = true) -> {

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isSubmitting = true
                                            val res = postInvoiceAction(
                                                mapOf(
                                                    "user_id" to userId.toString(),
                                                    "action" to "update_status",
                                                    "invoice_id" to current.id.toString(),
                                                    "type" to "proforma",
                                                    "status" to "rejected",
                                                    "remarks" to "Rejected by admin"
                                                )
                                            )
                                            isSubmitting = false
                                            if (res.first) onActionDone()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isSubmitting
                                ) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reject", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isSubmitting = true
                                            val res = postInvoiceAction(
                                                mapOf(
                                                    "user_id" to userId.toString(),
                                                    "action" to "update_status",
                                                    "invoice_id" to current.id.toString(),
                                                    "type" to "proforma",
                                                    "status" to "approved",
                                                    "remarks" to "Approved by admin"
                                                )
                                            )
                                            isSubmitting = false
                                            if (res.first) onActionDone()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isSubmitting
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Approve", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            else -> {
                                Text(
                                    "Status: ${current.status.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CARD + STAT + BADGE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun AdminInvoiceCard(
    item: InvoiceItem,
    onClick: () -> Unit
) {
    val st = item.status
    val (bg, fg) = when (st.lowercase()) {
        "approved" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "pending_approval" -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)
        "rejected", "cancelled" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("#${item.referenceNo}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        item.customerName,
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Proforma", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Medium)
                }
                Text(
                    "₹${formatInr(item.total)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = PrimaryIndigo
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        st.replace("_", " ").replaceFirstChar { it.uppercase() },
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.createdAt?.take(10) ?: "", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: Int, fg: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = fg)
            Text(label, fontSize = 10.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bg, fg, icon) = when (status.lowercase()) {
        "approved" -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), Icons.Default.CheckCircle)
        "rejected", "cancelled" -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), Icons.Default.Cancel)
        "pending_approval" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), Icons.Default.Schedule)
        else -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Icons.Default.Schedule)
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                status.replace("_", " ").replaceFirstChar { it.uppercase() },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
private fun TotalsRow(label: String, value: String, color: Color = Color(0xFF111827)) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF6B7280), modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
        Spacer(Modifier.width(8.dp))
        Text("₹${formatInr(value)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.width(100.dp), textAlign = TextAlign.End)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SHARE + PDF
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun shareInvoiceAsText(item: InvoiceItem, lineItems: List<InvoiceLineItem>, context: Context) {
    val sb = StringBuilder()
    sb.appendLine("PROFORMA INVOICE")
    sb.appendLine("#${item.referenceNo}")
    sb.appendLine("Friends Software Solutions")
    sb.appendLine("────────────────────────────────")
    sb.appendLine("Date: ${formatDate(item.createdAt?.take(10))}")
    sb.appendLine()
    sb.appendLine("Bill To:")
    sb.appendLine("  ${item.customerName}")
    item.customerCompany?.takeIf { it.isNotBlank() }?.let { sb.appendLine("  $it") }
    if (item.customerPhone.isNotBlank()) sb.appendLine("  ${item.customerPhone}")
    if (item.customerEmail.isNotBlank()) sb.appendLine("  ${item.customerEmail}")
    sb.appendLine()
    sb.appendLine("Items:")
    lineItems.forEachIndexed { idx, line ->
        sb.appendLine("  ${idx + 1}. ${line.name.ifBlank { "Item" }}  ₹${formatInr(line.amount)}")
        if (line.details.isNotBlank()) sb.appendLine("     ${line.details}")
    }
    sb.appendLine()
    sb.appendLine("Subtotal: ₹${formatInr(item.amount)}")
    if ((item.discount.toDoubleOrNull() ?: 0.0) > 0) sb.appendLine("Discount: -₹${formatInr(item.discount)}")
    sb.appendLine("Tax: ₹${formatInr(item.tax)}")
    sb.appendLine("TOTAL: ₹${formatInr(item.total)}")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        putExtra(Intent.EXTRA_SUBJECT, "Proforma #${item.referenceNo}")
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share Proforma")) }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun exportInvoiceAsPdf(item: InvoiceItem, lineItems: List<InvoiceLineItem>, context: Context) {
    try {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
        }
        val titlePaint = android.graphics.Paint().apply {
            color = "#111827".toColorInt()
            textSize = 20f
            isFakeBoldText = true
        }
        val mutedPaint = android.graphics.Paint().apply {
            color = "#6B7280".toColorInt()
            textSize = 9f
        }

        var y = 50f
        canvas.drawText("PROFORMA INVOICE", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("#${item.referenceNo}", 40f, y, titlePaint.apply { textSize = 14f })
        y += 16f
        canvas.drawText("Friends Software Solutions", 40f, y, mutedPaint)
        y += 20f
        canvas.drawText("Date: ${formatDate(item.createdAt?.take(10))}", 40f, y, paint)
        y += 16f
        canvas.drawText("Bill To: ${item.customerName}", 40f, y, paint)
        y += 14f
        item.customerCompany?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText(it, 40f, y, paint)
            y += 14f
        }

        y += 16f
        canvas.drawText("Items:", 40f, y, paint)
        y += 14f
        lineItems.forEachIndexed { idx, line ->
            canvas.drawText("${idx + 1}. ${line.name.ifBlank { "Item" }}", 50f, y, paint)
            y += 12f
            canvas.drawText("   ₹${formatInr(line.amount)}", 50f, y, mutedPaint)
            y += 14f
            if (y > 700f) return@forEachIndexed
        }

        y += 10f
        canvas.drawText("Subtotal: ₹${formatInr(item.amount)}", 380f, y, paint)
        y += 14f
        canvas.drawText("Tax: ₹${formatInr(item.tax)}", 380f, y, paint)
        y += 14f
        canvas.drawText("TOTAL: ₹${formatInr(item.total)}", 380f, y, titlePaint.apply { textSize = 14f })

        pdf.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "proforma_${item.id}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open PDF")) }
    } catch (e: Exception) {
        Log.e(TAG, "PDF export failed", e)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// UTILS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun formatInr(value: String?): String {
    if (value.isNullOrBlank()) return "0"
    return try {
        val d = value.replace(",", "").toDoubleOrNull() ?: 0.0
        NumberFormat.getNumberInstance(Locale("en", "IN")).format(d)
    } catch (_: Exception) {
        value
    }
}

private fun formatDate(d: String?): String {
    if (d.isNullOrBlank() || d == "0000-00-00") return "—"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(d.take(10)) ?: return d
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)
    } catch (_: Exception) {
        d
    }
}