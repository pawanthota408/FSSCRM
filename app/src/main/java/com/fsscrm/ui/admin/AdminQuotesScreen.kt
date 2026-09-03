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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.fsscrm.network.QuoteRequirement
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

private const val TAG = "AdminQuotes"

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODELS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class QuoteItem(
    val id: Int = 0,
    val quoteNo: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val company: String = "",
    val address: String = "",
    val amount: String = "0",
    val tax: String = "0",
    val discount: String = "0",
    val total: String = "0",
    val status: String = "pending",
    val quoteDate: String? = null,
    val validUntil: String? = null,
    val createdByName: String? = null,
    val createdById: Int? = null,
    val adminNotes: String? = null,
    val leadId: Int? = null,
    val leadService: String? = null,
    val terms: String? = null,
    val itemsJson: String? = null,
    val itemsCount: Int = 0,
    val createdAt: String? = null,
    val requirementNames: String = ""
)

data class QuoteStats(
    val total: Int = 0,
    val pending: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0,
    val sent: Int = 0,
    val draft: Int = 0,
    val created: Int = 0
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PARSER HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun parseQuote(o: JsonObject): QuoteItem = QuoteItem(
    id = o.get("id")?.takeIf { !it.isJsonNull }?.asInt
        ?: o.get("quote_id")?.takeIf { !it.isJsonNull }?.asInt
        ?: 0,
    quoteNo = o.optStr("quote_no") ?: o.optStr("quote_number") ?: o.optStr("quote_id_display") ?: "",
    customerName = o.optStr("customer_name") ?: o.optStr("lead_name") ?: o.optStr("name") ?: "",
    customerEmail = o.optStr("customer_email") ?: o.optStr("email") ?: "",
    customerPhone = o.optStr("customer_phone") ?: o.optStr("phone") ?: "",
    company = o.optStr("company") ?: o.optStr("customer_company") ?: o.optStr("company_name") ?: "",
    address = o.optStr("address") ?: "",
    amount = o.optStr("amount") ?: o.optStr("subtotal") ?: "0",
    tax = o.optStr("tax") ?: o.optStr("tax_amount") ?: "0",
    discount = o.optStr("discount") ?: "0",
    total = o.optStr("total") ?: o.optStr("grand_total") ?: "0",
    status = o.optStr("status") ?: "pending",
    quoteDate = o.optStr("quote_date") ?: o.optStr("date") ?: o.optStr("created_at")?.take(10),
    validUntil = o.optStr("valid_until") ?: o.optStr("valid_date"),
    createdByName = o.optStr("created_by_name") ?: o.optStr("employee_name") ?: o.optStr("creator_name"),
    createdById = o.get("created_by_id")?.takeIf { !it.isJsonNull }?.asInt
        ?: o.get("emp_id")?.takeIf { !it.isJsonNull }?.asInt,
    adminNotes = o.optStr("admin_notes") ?: o.optStr("remarks"),
    leadId = o.get("lead_id")?.takeIf { !it.isJsonNull }?.asInt,
    leadService = o.optStr("lead_service") ?: o.optStr("service"),
    terms = o.optStr("terms") ?: o.optStr("terms_conditions"),
    itemsJson = o.optStr("items") ?: o.optStr("requirements") ?: o.optStr("req_json"),
    itemsCount = o.get("req_count")?.asInt ?: o.get("items_count")?.asInt ?: 0,
    createdAt = o.optStr("created_at"),
    requirementNames = o.optStr("requirement_names") ?: ""
)

private fun JsonObject.optStr(name: String): String? =
    this.get(name)?.takeIf { !it.isJsonNull }?.let { if (it.isJsonPrimitive) it.asString else null }

// Parse items – server sends { id, requirement, cost } (cost is total)
private fun parseItems(itemsField: Any?): List<QuoteRequirement> {
    if (itemsField == null) return emptyList()
    val arr: JsonArray = when (itemsField) {
        is JsonArray -> itemsField
        is String -> try {
            com.google.gson.JsonParser.parseString(itemsField).asJsonArray
        } catch (_: Exception) { return emptyList() }
        is com.google.gson.JsonPrimitive -> if (itemsField.isString) {
            try { com.google.gson.JsonParser.parseString(itemsField.asString).asJsonArray } catch (_: Exception) { return emptyList() }
        } else return emptyList()
        else -> return emptyList()
    }

    return arr.map { el ->
        val o = el.asJsonObject
        val cost = o.optStr("cost") ?: o.optStr("rate") ?: o.optStr("unit_price") ?: "0"
        // If no explicit amount, use cost (since cost is total)
        val amount = o.optStr("amount") ?: o.optStr("total") ?: cost
        QuoteRequirement(
            requirement = o.optStr("requirement") ?: o.optStr("description") ?: o.optStr("item") ?: o.optStr("name") ?: "",
            details = o.optStr("details") ?: o.optStr("specifications") ?: o.optStr("remarks") ?: "",
            quantity = "1", // not used
            cost = cost,
            amount = amount
        )
    }
}

// Format requirement for display: "Biz Analyst[SN: Test123]" -> "Item Name: Biz Analyst, License no: Test123"
private fun formatRequirementDisplay(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val snPattern = Regex("\\[SN:\\s*(.*?)\\]")
    val match = snPattern.find(trimmed)
    val name = if (match != null) {
        trimmed.replace(match.value, "").trim()
    } else {
        trimmed
    }
    val license = match?.groupValues?.get(1)?.trim() ?: ""
    return buildString {
        append("Item Name: $name")
        if (license.isNotEmpty()) {
            append(", License no: $license")
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MAIN SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuotesScreen(
    userId: Int,
    onMenuClick: () -> Unit
) {
    var quotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    var stats by remember { mutableStateOf(QuoteStats()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedStatus by remember { mutableStateOf("pending") }
    var searchInput by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf("") }

    var previewQuote by remember { mutableStateOf<QuoteItem?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchQuotes() {
        if (userId <= 0) {
            errorMessage = "user_id required"
            isLoading = false
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminQuotes(
                    mapOf(
                        "user_id" to userId.toString(),
                        "status" to selectedStatus,
                        "search" to activeSearch
                    )
                )
                if (response.isSuccessful) {
                    response.toLenientJson()?.asJsonObject?.let { root ->
                        if (root.get("success")?.asBoolean != true) {
                            errorMessage = root.get("error")?.asString ?: "Failed"
                            return@let
                        }
                        val arr = root.getAsJsonArray("quotes")
                        quotes = arr?.map { parseQuote(it.asJsonObject) } ?: emptyList()
                        root.getAsJsonObject("stats")?.let { s ->
                            stats = QuoteStats(
                                total = s.get("total")?.asInt ?: 0,
                                pending = s.get("pending")?.asInt ?: 0,
                                approved = s.get("approved")?.asInt ?: 0,
                                rejected = s.get("rejected")?.asInt ?: 0,
                                sent = s.get("sent")?.asInt ?: 0,
                                draft = s.get("draft")?.asInt ?: 0,
                                created = s.get("created")?.asInt ?: 0
                            )
                        }
                    }
                } else {
                    errorMessage = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch error", e)
                errorMessage = e.message ?: "Network error"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedStatus, activeSearch) { fetchQuotes() }

    Scaffold(
        topBar = { UniversalHeader(title = "Company Quotations", onMenuClick = onMenuClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMini("Pending", stats.pending, Color(0xFF92400E), Modifier.weight(1f))
                StatMini("Approved", stats.approved, Color(0xFF065F46), Modifier.weight(1f))
                StatMini("Rejected", stats.rejected, Color(0xFF991B1B), Modifier.weight(1f))
                StatMini("Total", stats.total, Color(0xFF1E3A5F), Modifier.weight(1f))
            }

            Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var statusExpanded by remember { mutableStateOf(false) }
                    val statusOptions = listOf(
                        "pending" to "Pending",
                        "approved" to "Approved",
                        "rejected" to "Rejected",
                        "sent" to "Sent",
                        "draft" to "Draft",
                        "created" to "Created",
                        "all" to "All"
                    )
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = !statusExpanded },
                        modifier = Modifier.weight(0.38f)
                    ) {
                        OutlinedTextField(
                            value = statusOptions.find { it.first == selectedStatus }?.second ?: "Pending",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(statusExpanded, { statusExpanded = false }) {
                            statusOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 13.sp) },
                                    onClick = {
                                        selectedStatus = key
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.weight(0.42f),
                        placeholder = { Text("Customer / Quote #", fontSize = 12.sp) },
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

            Text(
                "${quotes.size} quotes",
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { fetchQuotes() }) { Text("Retry") }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quotes, key = { it.id }) { quote ->
                        AdminQuoteCard(
                            quote = quote,
                            onView = { previewQuote = quote }
                        )
                    }
                    if (quotes.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No quotes found", color = Color.Gray)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    previewQuote?.let { q ->
        QuotePreviewDialog(
            quote = q,
            userId = userId,
            onDismiss = { previewQuote = null },
            onActionDone = {
                previewQuote = null
                fetchQuotes()
            }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// API CALLER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private suspend fun postAction(params: Map<String, String>): Triple<Boolean, String?, JsonObject?> {
    return try {
        val resp = RetrofitClient.apiService.adminQuotesPost(params)
        if (resp.isSuccessful) {
            val body = resp.toLenientJson()?.asJsonObject
            val ok = body?.get("success")?.asBoolean == true || body?.get("status")?.asString == "success"
            Triple(ok, body?.get("message")?.asString ?: body?.get("error")?.asString, body)
        } else Triple(false, "Server ${resp.code()}", null)
    } catch (e: Exception) {
        Triple(false, e.message, null)
    }
}

private suspend fun fetchQuoteFull(quoteId: Int, userId: Int): QuoteItem? {
    val (success, _, body) = postAction(
        mapOf(
            "user_id" to userId.toString(),
            "action" to "get_quote_detail",
            "quote_id" to quoteId.toString()
        )
    )
    if (!success || body == null) return null

    val quoteObj = body.getAsJsonObject("quote") ?: return null
    val base = parseQuote(quoteObj)

    val requirementsArray = body.getAsJsonArray("requirements")
    val items = if (requirementsArray != null) {
        parseItems(requirementsArray)
    } else {
        val itemsJsonVal = quoteObj.get("items") ?: quoteObj.get("req_json") ?: quoteObj.get("data")?.takeIf { it.isJsonArray }
        parseItems(itemsJsonVal)
    }

    return if (items.isNotEmpty()) {
        base.copy(itemsJson = com.google.gson.Gson().toJson(items))
    } else base
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// QUOTE PREVIEW + EDIT DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePreviewDialog(
    quote: QuoteItem,
    userId: Int,
    onDismiss: () -> Unit,
    onActionDone: () -> Unit
) {
    var fullQuote by remember { mutableStateOf(quote) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(quote.id) {
        if (quote.id == 0) {
            isLoadingDetails = false
            return@LaunchedEffect
        }
        isLoadingDetails = true
        val fetched = fetchQuoteFull(quote.id, userId)
        if (fetched != null && fetched.id != 0) {
            fullQuote = fetched
        }
        isLoadingDetails = false
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.96f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(Modifier.fillMaxSize()) {

                Row(
                    Modifier.fillMaxWidth().background(PrimaryIndigo).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Quotation #${fullQuote.quoteNo.ifBlank { "Q-${fullQuote.id}" }}",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        Text(
                            "${fullQuote.customerName.ifBlank { "—" }} · ₹${formatInr(fullQuote.total)}",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { if (!isSubmitting) onDismiss() }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().background(Color.White).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(fullQuote.status, Modifier.weight(1f))
                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEditing) Color(0xFFFEF3C7) else Color(0xFFEFF6FF))
                            .padding(0.dp)
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            null,
                            tint = if (isEditing) Color(0xFF92400E) else PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { shareQuoteAsText(fullQuote, context) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                    ) {
                        Icon(Icons.Default.Share, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { exportQuoteAsPdf(fullQuote, context) }
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
                } else if (isEditing) {
                    EditQuoteContent(
                        quote = fullQuote,
                        onChange = { fullQuote = it },
                        isSubmitting = isSubmitting,
                        onSave = { updated ->
                            scope.launch {
                                isSubmitting = true
                                val res = postAction(
                                    mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "edit_quote",   // correct action
                                        "quote_id" to updated.id.toString(),
                                        "customer_name" to updated.customerName,
                                        "customer_email" to updated.customerEmail,
                                        "customer_phone" to updated.customerPhone,
                                        "amount" to updated.amount,
                                        "tax" to updated.tax,
                                        "total" to updated.total,
                                        "quote_date" to (updated.quoteDate ?: ""),
                                        "valid_until" to (updated.validUntil ?: ""),
                                        "admin_notes" to (updated.adminNotes ?: ""),
                                        "requirements" to (updated.itemsJson ?: "[]")   // send under 'requirements'
                                    )
                                )
                                isSubmitting = false
                                if (res.first) {
                                    fullQuote = updated
                                    isEditing = false
                                    onActionDone()
                                }
                            }
                        },
                        onCancel = { isEditing = false }
                    )
                } else {
                    PreviewQuoteContent(
                        quote = fullQuote,
                        onApprove = { notes ->
                            scope.launch {
                                isSubmitting = true
                                val res = postAction(
                                    mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "approve_quote",
                                        "quote_id" to fullQuote.id.toString(),
                                        "admin_notes" to notes
                                    )
                                )
                                isSubmitting = false
                                if (res.first) onActionDone()
                            }
                        },
                        onReject = { notes ->
                            scope.launch {
                                isSubmitting = true
                                val res = postAction(
                                    mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "reject_quote",
                                        "quote_id" to fullQuote.id.toString(),
                                        "admin_notes" to notes
                                    )
                                )
                                isSubmitting = false
                                if (res.first) onActionDone()
                            }
                        },
                        onSend = {
                            scope.launch {
                                isSubmitting = true
                                val res = postAction(
                                    mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "send_quote",
                                        "quote_id" to fullQuote.id.toString()
                                    )
                                )
                                isSubmitting = false
                                if (res.first) onActionDone()
                            }
                        },
                        isSubmitting = isSubmitting
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PREVIEW MODE – shows formatted item names
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewQuoteContent(
    quote: QuoteItem,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onSend: () -> Unit,
    isSubmitting: Boolean
) {
    val items = remember(quote.itemsJson) { parseItems(quote.itemsJson) }
    var showApprove by remember { mutableStateOf(false) }
    var showReject by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("QUOTATION", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                            Text("Friends Software Solutions", fontSize = 11.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                            Text("Tally Sales · Web & App Design", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryIndigo.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "#${quote.quoteNo.ifBlank { "Q-${quote.id}" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Date: ${formatDate(quote.quoteDate ?: quote.createdAt?.take(10))}",
                                fontSize = 10.sp, color = Color(0xFF6B7280)
                            )
                            quote.validUntil?.takeIf { it.isNotBlank() && it != "null" }?.let {
                                Text("Valid till: ${formatDate(it)}", fontSize = 10.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFE5E7EB))

                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("BILL TO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                            Spacer(Modifier.height(3.dp))
                            Text(quote.customerName.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (quote.company.isNotBlank()) Text(quote.company, fontSize = 11.sp, color = Color(0xFF374151))
                            if (quote.address.isNotBlank()) Text(quote.address, fontSize = 10.sp, color = Color(0xFF6B7280))
                            Spacer(Modifier.height(4.dp))
                            if (quote.customerEmail.isNotBlank()) Text("✉ ${quote.customerEmail}", fontSize = 10.sp, color = Color(0xFF6B7280))
                            if (quote.customerPhone.isNotBlank()) Text("☎ ${quote.customerPhone}", fontSize = 10.sp, color = Color(0xFF6B7280))
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("PREPARED BY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                            Spacer(Modifier.height(3.dp))
                            Text(quote.createdByName ?: "Admin", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            if (!quote.leadService.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("SERVICE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                                Text(quote.leadService, fontSize = 11.sp, color = Color(0xFF374151))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("ITEMS / REQUIREMENTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                    Spacer(Modifier.height(6.dp))

                    // Table header
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.width(22.dp))
                        Text("Description", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.weight(1f))
                        Text("Amount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                    }

                    if (items.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                        }
                    } else {
                        items.forEachIndexed { idx, item ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("${idx + 1}", fontSize = 10.sp, color = Color(0xFF6B7280), modifier = Modifier.width(22.dp))
                                Column(Modifier.weight(1f)) {
                                    val display = formatRequirementDisplay(item.requirement)
                                    Text(display.ifBlank { item.requirement }, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111827))
                                    if (item.details.isNotBlank()) {
                                        Text(item.details, fontSize = 9.sp, color = Color(0xFF6B7280))
                                    }
                                }
                                Text("₹${formatInr(item.cost)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827), modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Totals ──
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        TotalsRow("Subtotal", quote.amount)
                        if (quote.discount.toDoubleOrNull()?.let { it > 0 } == true) {
                            TotalsRow("Discount", "-${formatInr(quote.discount)}", color = Color(0xFFDC2626))
                        }
                        TotalsRow("Tax", quote.tax)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .background(PrimaryIndigo, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("TOTAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                                Spacer(Modifier.width(12.dp))
                                Text("₹${formatInr(quote.total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }

                    if (!quote.terms.isNullOrBlank() && quote.terms.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB))
                        Spacer(Modifier.height(8.dp))
                        Text("TERMS & CONDITIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                        Text(quote.terms, fontSize = 10.sp, color = Color(0xFF374151))
                    }

                    if (!quote.adminNotes.isNullOrBlank() && quote.adminNotes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("ADMIN NOTES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                Text(quote.adminNotes, fontSize = 10.sp, color = Color(0xFF78350F))
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

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

            Spacer(Modifier.height(14.dp))
        }

        // ── Action bar ──
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (quote.status.lowercase()) {
                "pending", "draft" -> {
                    OutlinedButton(
                        onClick = { showReject = true },
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
                        onClick = { showApprove = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        else {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approve", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                "approved", "sent" -> {
                    Button(
                        onClick = onSend,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Send to customer", fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {
                    Text(
                        "This quotation is ${quote.status}.",
                        fontSize = 12.sp, color = Color(0xFF6B7280),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showApprove) {
        NotesDialog(
            title = "Approve Quotation",
            confirmLabel = "Approve",
            confirmColor = Color(0xFF059669),
            requireNotes = false,
            onDismiss = { showApprove = false },
            onConfirm = { notes ->
                showApprove = false
                onApprove(notes)
            }
        )
    }
    if (showReject) {
        NotesDialog(
            title = "Reject Quotation",
            confirmLabel = "Reject",
            confirmColor = Color(0xFFDC2626),
            requireNotes = true,
            onDismiss = { showReject = false },
            onConfirm = { notes ->
                showReject = false
                onReject(notes)
            }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EDIT MODE – no QTY, only Description and Cost
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditQuoteContent(
    quote: QuoteItem,
    onChange: (QuoteItem) -> Unit,
    isSubmitting: Boolean,
    onSave: (QuoteItem) -> Unit,
    onCancel: () -> Unit
) {
    val items = remember { mutableStateListOf<QuoteRequirement>().apply {
        addAll(parseItems(quote.itemsJson))
    } }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp)
        ) {

            SectionLabel("Customer Details (Read-only)")
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Name: ${quote.customerName}", fontWeight = FontWeight.Bold)
                    if (quote.company.isNotBlank()) Text("Company: ${quote.company}", fontSize = 13.sp)
                    if (quote.customerEmail.isNotBlank()) Text("Email: ${quote.customerEmail}", fontSize = 13.sp)
                    if (quote.customerPhone.isNotBlank()) Text("Phone: ${quote.customerPhone}", fontSize = 13.sp)
                    if (quote.address.isNotBlank()) Text("Address: ${quote.address}", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Items / Requirements")
                TextButton(onClick = { items.add(QuoteRequirement(requirement = "", quantity = "1", cost = "0", amount = "0")) }) {
                    Text("+ Add Item", color = PrimaryIndigo)
                }
            }
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (items.isEmpty()) {
                        Text("No items. Click + Add Item to begin.", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    } else {
                        items.forEachIndexed { idx, item ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Item ${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                    IconButton(onClick = { items.removeAt(idx) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                // Description field
                                EditField("Item Name / Description", item.requirement) {
                                    items[idx] = item.copy(requirement = it)
                                }
                                Spacer(Modifier.height(8.dp))
                                // Cost field only (no QTY)
                                EditField("Cost (₹)", item.cost, KeyboardType.Decimal) {
                                    items[idx] = item.copy(cost = it, amount = it) // amount = cost
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Totals & Notes")
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val subtotal = items.sumOf { it.cost.toDoubleOrNull() ?: 0.0 }
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            Text("Subtotal: ₹${String.format(Locale.US, "%.2f", subtotal)}")
                        }
                        Box(Modifier.weight(1f)) {
                            EditField("Tax ₹", quote.tax, KeyboardType.Decimal) { onChange(quote.copy(tax = it)) }
                        }
                    }
                    EditField("Discount ₹", quote.discount, KeyboardType.Decimal) { onChange(quote.copy(discount = it)) }

                    val finalTotal = (subtotal + (quote.tax.toDoubleOrNull() ?: 0.0) - (quote.discount.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(PrimaryIndigo.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            Text("₹${String.format(Locale.US, "%.2f", finalTotal)}", fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo, fontSize = 16.sp)
                        }
                    }

                    EditField("Admin Notes", quote.adminNotes ?: "", singleLine = false, minLines = 2) { onChange(quote.copy(adminNotes = it)) }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(10.dp),
                enabled = !isSubmitting
            ) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val subtotal = items.sumOf { it.cost.toDoubleOrNull() ?: 0.0 }
                    val finalTotal = (subtotal + (quote.tax.toDoubleOrNull() ?: 0.0) - (quote.discount.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
                    // Build requirements array for server
                    val requirementsList = items.map {
                        mapOf(
                            "requirement" to it.requirement,
                            "cost" to it.cost
                        )
                    }
                    val requirementsJson = com.google.gson.Gson().toJson(requirementsList)
                    onSave(quote.copy(
                        amount = subtotal.toString(),
                        total = finalTotal.toString(),
                        itemsJson = requirementsJson   // store for later use
                    ))
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                else {
                    Icon(Icons.Default.Save, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HELPERS – UI pieces (unchanged)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bg, fg, icon) = when (status.lowercase()) {
        "approved" -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), Icons.Default.CheckCircle)
        "rejected" -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), Icons.Default.Cancel)
        "sent" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E3A5F), Icons.AutoMirrored.Filled.Send)
        "draft" -> Triple(Color(0xFFE5E7EB), Color(0xFF374151), Icons.Default.Edit)
        else -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Icons.Default.Schedule)
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(status.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(fontSize = 13.sp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun NotesDialog(
    title: String,
    confirmLabel: String,
    confirmColor: Color,
    requireNotes: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
            Column(Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (requireNotes) "Reason required…" else "Optional notes…", fontSize = 12.sp) },
                    minLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !requireNotes || notes.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                        onClick = { onConfirm(notes.trim()) }
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CARD + STAT (unchanged)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun AdminQuoteCard(
    quote: QuoteItem,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick = onView
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("#${quote.quoteNo}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        quote.customerName,
                        color = Color(0xFF6B7280), fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (quote.requirementNames.isNotBlank()) {
                        Text(
                            text = quote.requirementNames.take(60) + if (quote.requirementNames.length > 60) "…" else "",
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Text(
                    "₹${formatInr(quote.total)}",
                    fontWeight = FontWeight.Black, fontSize = 16.sp,
                    color = PrimaryIndigo
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(quote.status)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(quote.quoteDate ?: quote.createdAt?.take(10) ?: "", fontSize = 11.sp, color = Color.Gray)
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

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EXPORT – share text + PDF (updated to use formatRequirementDisplay)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun shareQuoteAsText(quote: QuoteItem, context: Context) {
    val items = parseItems(quote.itemsJson)
    val sb = StringBuilder()
    sb.appendLine("QUOTATION #${quote.quoteNo.ifBlank { "Q-${quote.id}" }}")
    sb.appendLine("Friends Software Solutions")
    sb.appendLine("────────────────────────────────")
    sb.appendLine("Date: ${formatDate(quote.quoteDate ?: quote.createdAt?.take(10))}")
    sb.appendLine()
    sb.appendLine("Bill To:")
    sb.appendLine("  ${quote.customerName}")
    if (quote.company.isNotBlank()) sb.appendLine("  ${quote.company}")
    if (quote.customerPhone.isNotBlank()) sb.appendLine("  ${quote.customerPhone}")
    if (quote.customerEmail.isNotBlank()) sb.appendLine("  ${quote.customerEmail}")
    sb.appendLine()
    sb.appendLine("Items:")
    items.forEach {
        val display = formatRequirementDisplay(it.requirement)
        sb.appendLine("  ${display.ifBlank { it.requirement }}  ₹${formatInr(it.cost)}")
    }
    sb.appendLine()
    sb.appendLine("Subtotal: ₹${formatInr(quote.amount)}")
    if (quote.discount.toDoubleOrNull()?.let { it > 0 } == true) sb.appendLine("Discount: -₹${formatInr(quote.discount)}")
    sb.appendLine("Tax: ₹${formatInr(quote.tax)}")
    sb.appendLine("TOTAL: ₹${formatInr(quote.total)}")
    if (!quote.terms.isNullOrBlank()) {
        sb.appendLine()
        sb.appendLine("Terms:")
        sb.appendLine(quote.terms)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        putExtra(Intent.EXTRA_SUBJECT, "Quotation #${quote.quoteNo}")
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share quotation")) }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun exportQuoteAsPdf(quote: QuoteItem, context: Context) {
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
            textSize = 22f
            isFakeBoldText = true
        }
        val mutedPaint = android.graphics.Paint().apply {
            color = "#6B7280".toColorInt()
            textSize = 9f
        }

        var y = 50f
        canvas.drawText("QUOTATION", 40f, y, titlePaint)
        canvas.drawText("#${quote.quoteNo.ifBlank { "Q-${quote.id}" }}", 555f - paint.measureText("#${quote.quoteNo}"), y, titlePaint)
        y += 18f
        canvas.drawText("Friends Software Solutions", 40f, y, mutedPaint.apply { textSize = 10f })
        y += 20f

        canvas.drawText("Date: ${formatDate(quote.quoteDate ?: quote.createdAt?.take(10))}", 40f, y, paint)
        y += 14f
        canvas.drawText("Bill To: ${quote.customerName}", 40f, y, paint)
        if (quote.company.isNotBlank()) {
            y += 12f
            canvas.drawText("  ${quote.company}", 40f, y, paint)
        }
        if (quote.customerPhone.isNotBlank()) {
            y += 12f
            canvas.drawText("  ${quote.customerPhone}", 40f, y, paint)
        }
        if (quote.customerEmail.isNotBlank()) {
            y += 12f
            canvas.drawText("  ${quote.customerEmail}", 40f, y, paint)
        }

        y += 24f
        val items = parseItems(quote.itemsJson)
        items.forEach {
            val display = formatRequirementDisplay(it.requirement)
            canvas.drawText("• ${display.ifBlank { it.requirement }}", 40f, y, paint)
            y += 12f
            canvas.drawText("  ₹${formatInr(it.cost)}", 50f, y, mutedPaint)
            y += 16f
            if (y > 750f) return@forEach
        }

        y += 8f
        canvas.drawText("Subtotal: ₹${formatInr(quote.amount)}", 380f, y, paint)
        y += 14f
        canvas.drawText("Tax: ₹${formatInr(quote.tax)}", 380f, y, paint)
        y += 14f
        canvas.drawText("TOTAL: ₹${formatInr(quote.total)}", 380f, y, titlePaint.apply { textSize = 14f })

        pdf.finishPage(page)
        val dir = File(context.cacheDir, "quotations").apply { mkdirs() }
        val file = File(dir, "quote_${quote.id}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open PDF")) }
            .onFailure {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallback, "Share PDF"))
            }
    } catch (e: Exception) {
        Log.e(TAG, "PDF export failed", e)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// UTILS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun recalcTotal(amount: String, tax: String, discount: String): String {
    val a = amount.toDoubleOrNull() ?: 0.0
    val t = tax.toDoubleOrNull() ?: 0.0
    val d = discount.toDoubleOrNull() ?: 0.0
    val total = (a - d + t).coerceAtLeast(0.0)
    return String.format(Locale.US, "%.2f", total)
}

private fun formatInr(value: String?): String {
    if (value.isNullOrBlank()) return "0"
    return try {
        val d = value.replace(",", "").toDoubleOrNull() ?: 0.0
        val locale = Locale("en", "IN")
        NumberFormat.getNumberInstance(locale).format(d)
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
    } catch (_: Exception) { d }
}