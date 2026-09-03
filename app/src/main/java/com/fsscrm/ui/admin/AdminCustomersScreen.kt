package com.fsscrm.ui.admin

import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.core.net.toUri
import com.fsscrm.network.AdminCustomersResponse
import com.fsscrm.network.Customer
import com.fsscrm.network.CustomerLicense
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AdminCustomers"

// ─── Palette ──────────────────────────────────────────────────────────
private val BgPage      = Color(0xFFF8FAFC)
private val CardBg      = Color.White
private val Border      = Color(0xFFE2E8F0)
private val BorderSoft  = Color(0xFFF1F5F9)
private val TextDark    = Color(0xFF111827)
private val TextMuted   = Color(0xFF6B7280)
private val TextSubtle  = Color(0xFF94A3B8)
private val Primary     = Color(0xFF2563EB)
private val Success     = Color(0xFF059669)
private val Warning     = Color(0xFFD97706)
private val Danger      = Color(0xFFDC2626)
private val Purple      = Color(0xFF7C3AED)

// ─── Navigation state ─────────────────────────────────────────────────
private sealed class CustomersScreen {
    object List : CustomersScreen()
    data class Detail(val customerId: Int, val customerName: String) : CustomersScreen()
    data class LicenseDetail(val customerId: Int, val licenseKey: String, val customerName: String) : CustomersScreen()
}

// ─── API result envelope ──────────────────────────────────────────────
private data class ApiResult(val ok: Boolean, val message: String? = null, val data: JsonObject? = null)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 1.  DATA CLASSES FOR DETAIL / LICENCE RESPONSES
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private data class CustomerDetailData(
    val customerId: Int = 0,
    val leadId: Int? = null,
    val name: String = "",
    val company: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val source: String? = null,
    val leadName: String? = null,
    val leadService: String? = null,
    val amount: String = "0",
    val status: String = "Active",
    val licenses: List<CustomerLicense> = emptyList(),
    val licenseGroups: List<LicenseGroup> = emptyList(),
    val works: List<CustomerWork> = emptyList(),
    val payments: List<CustomerPayment> = emptyList(),
    val followups: List<CustomerFollowup> = emptyList()
)

private data class LicenseGroup(
    val licenseKey: String,
    val items: List<CustomerLicense> = emptyList()
)

private data class CustomerWork(
    val id: Int,
    val workName: String? = null,
    val description: String? = null,
    val status: String? = null,
    val totalAmount: String? = null,
    val balanceAmount: String? = null,
    val leadService: String? = null,
    val createdAt: String? = null
)

private data class CustomerPayment(
    val id: Int,
    val reference: String? = null,
    val amount: String? = null,
    val paymentDate: String? = null,
    val paymentMethod: String? = null,
    val status: String? = null
)

private data class CustomerFollowup(
    val id: Int,
    val followUpDate: String? = null,
    val status: String? = null,
    val remarks: String? = null
)

private data class LicenseDetailData(
    val customerName: String = "",
    val customerCompany: String? = null,
    val customerPhone: String? = null,
    val licenseKey: String = "",
    val attached: List<AttachedItem> = emptyList(),
    val opportunities: List<Opportunity> = emptyList(),
    val proformas: List<Proforma> = emptyList(),
    val payments: List<CustomerPayment> = emptyList()
)

private data class AttachedItem(
    val id: Int = 0,
    val label: String = "",
    val expiryDate: String? = null,
    val status: String? = "active",
    val isMain: Boolean = false
)

private data class Opportunity(val id: Int, val name: String)

private data class Proforma(
    val id: Int,
    val proformaNo: String? = null,
    val service: String? = null,
    val total: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val serialMatched: Boolean = false
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 2.  JSON HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private fun JsonObject.optString(name: String): String? =
    this.get(name)?.takeIf { !it.isJsonNull }?.asString

private fun JsonObject.optInt(name: String): Int? =
    this.get(name)?.takeIf { !it.isJsonNull }?.asInt

private fun parseWork(o: JsonObject) = CustomerWork(
    id           = o.optInt("id") ?: 0,
    workName     = o.optString("work_name"),
    description  = o.optString("description"),
    status       = o.optString("status"),
    totalAmount  = o.optString("total_amount"),
    balanceAmount= o.optString("balance_amount"),
    leadService  = o.optString("lead_service"),
    createdAt    = o.optString("created_at")
)

private fun parsePayment(o: JsonObject) = CustomerPayment(
    id            = o.optInt("id") ?: 0,
    reference     = o.optString("payment_reference") ?: o.optString("transaction_id"),
    amount        = o.optString("amount"),
    paymentDate   = o.optString("payment_date"),
    paymentMethod = o.optString("payment_method"),
    status        = o.optString("status")
)

private fun parseFollowup(o: JsonObject) = CustomerFollowup(
    id            = o.optInt("id") ?: 0,
    followUpDate  = o.optString("follow_up_date"),
    status        = o.optString("status"),
    remarks       = o.optString("remarks")
)

private fun parseCustomerDetail(json: JsonObject): CustomerDetailData? {
    return try {
        if (json.optString("success")?.toBooleanStrictOrNull() != true) return null
        val d = json.getAsJsonObject("data") ?: return null
        CustomerDetailData(
            customerId    = d.optInt("customer_id") ?: 0,
            leadId        = d.optInt("lead_id"),
            name          = d.optString("lead_name") ?: d.optString("customer_name") ?: "",
            company       = d.optString("company"),
            phone         = d.optString("lead_phone") ?: d.optString("customer_phone"),
            email         = d.optString("lead_email") ?: d.optString("customer_email"),
            source        = d.optString("source"),
            leadName      = d.optString("lead_name"),
            leadService   = d.optString("lead_service"),
            amount        = d.optString("amount") ?: "0",
            status        = d.optString("customer_status") ?: "Active",
            licenses      = d.getAsJsonArray("licenses")?.map {
                CustomerLicense.fromJson(it.asJsonObject)
            } ?: emptyList(),
            licenseGroups = d.getAsJsonArray("license_groups")?.map { g ->
                val go = g.asJsonObject
                LicenseGroup(
                    licenseKey = go.optString("license_key") ?: "",
                    items      = go.getAsJsonArray("items")?.map {
                        CustomerLicense.fromJson(it.asJsonObject)
                    } ?: emptyList()
                )
            } ?: emptyList(),
            works         = d.getAsJsonArray("works")?.map { parseWork(it.asJsonObject) } ?: emptyList(),
            payments      = d.getAsJsonArray("payments")?.map { parsePayment(it.asJsonObject) } ?: emptyList(),
            followups     = d.getAsJsonArray("followups")?.map { parseFollowup(it.asJsonObject) } ?: emptyList()
        )
    } catch (e: Exception) {
        Log.e(TAG, "parseCustomerDetail error", e); null
    }
}

private fun parseLicenseDetail(json: JsonObject): LicenseDetailData? {
    return try {
        if (json.optString("success")?.toBooleanStrictOrNull() != true) return null
        val d = json.getAsJsonObject("data") ?: return null
        val cust = d.getAsJsonObject("customer")
        LicenseDetailData(
            customerName    = cust?.optString("name") ?: "",
            customerCompany = cust?.optString("company"),
            customerPhone   = cust?.optString("phone"),
            licenseKey      = d.optString("license_key") ?: "",
            attached        = d.getAsJsonArray("attached")?.map { a ->
                val ao = a.asJsonObject
                val label = ao.optString("label") ?: ao.optString("item_name") ?: ao.optString("license_type") ?: "Item"
                AttachedItem(
                    id         = ao.optInt("id") ?: 0,
                    label      = label,
                    expiryDate = ao.optString("expiry_date"),
                    status     = ao.optString("status") ?: "active",
                    isMain     = (ao.optInt("is_main") ?: 0) == 1 || isTallyMain(label)
                )
            } ?: emptyList(),
            opportunities   = d.getAsJsonArray("opportunities")?.map { o ->
                val oo = o.asJsonObject
                Opportunity(oo.optInt("id") ?: 0, oo.optString("name") ?: "")
            } ?: emptyList(),
            proformas       = d.getAsJsonArray("proformas")?.map { p ->
                val po = p.asJsonObject
                Proforma(
                    id            = po.optInt("id") ?: 0,
                    proformaNo    = po.optString("proforma_no"),
                    service       = po.optString("service"),
                    total         = po.optString("total"),
                    status        = po.optString("status"),
                    createdAt     = po.optString("created_at"),
                    serialMatched = (po.optInt("serial_matched") ?: 0) == 1
                )
            } ?: emptyList(),
            payments        = d.getAsJsonArray("payments")?.map { parsePayment(it.asJsonObject) } ?: emptyList()
        )
    } catch (e: Exception) {
        Log.e(TAG, "parseLicenseDetail error", e); null
    }
}

private fun isTallyMain(name: String): Boolean {
    val n = name.lowercase()
    if (n.contains("cloud")) return false
    return (n.contains("tally") && (n.contains("gold") || n.contains("silver") ||
            n.contains("server") || n.contains("prime") || n.contains("erp")))
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 3.  API HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private suspend fun callPost(params: Map<String, String>): ApiResult {
    return try {
        val resp = RetrofitClient.apiService.adminCustomersPost(params)
        if (resp.isSuccessful) {
            val body = resp.toLenientJson()?.asJsonObject
            val ok = body?.optString("success")?.toBooleanStrictOrNull() == true
            ApiResult(ok, body?.optString("error") ?: body?.optString("message"), body)
        } else {
            val errBody = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            ApiResult(false, "Server error: ${resp.code()}${errBody?.let { " — $it" } ?: ""}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "callPost error", e); ApiResult(false, e.message)
    }
}

private suspend fun callGet(params: Map<String, String>): ApiResult {
    return try {
        val resp = RetrofitClient.apiService.adminCustomersGet(params)
        if (resp.isSuccessful) {
            val body = resp.toLenientJson()?.asJsonObject
            ApiResult(true, data = body)
        } else ApiResult(false, "Server error: ${resp.code()}")
    } catch (e: Exception) {
        Log.e(TAG, "callGet error", e); ApiResult(false, e.message)
    }
}

private suspend fun callUpload(fieldParams: Map<String, String>, file: File): ApiResult {
    return try {
        val parts = fieldParams.mapValues { (_, v) -> v.toRequestBody("text/plain".toMediaTypeOrNull()) }
        val mediaType = "text/csv".toMediaTypeOrNull()
        val filePart = MultipartBody.Part.createFormData("import_file", file.name, file.asRequestBody(mediaType))
        val resp = RetrofitClient.apiService.adminCustomersUpload(parts, filePart)
        if (resp.isSuccessful) {
            val body = resp.toLenientJson()?.asJsonObject
            val ok = body?.optString("success")?.toBooleanStrictOrNull() == true
            ApiResult(ok, body?.optString("message") ?: body?.optString("error"), body)
        } else ApiResult(false, "Upload failed: ${resp.code()}")
    } catch (e: Exception) {
        Log.e(TAG, "callUpload error", e); ApiResult(false, e.message)
    }
}

private fun copyUriToCache(uri: Uri, context: Context): File? {
    return try {
        val name = uri.lastPathSegment ?: "upload.csv"
        val safe = name.substringAfterLast('/').ifBlank { "upload.csv" }
        val out = File(context.cacheDir, safe)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        out
    } catch (e: Exception) {
        Log.e(TAG, "copyUriToCache error", e); null
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 4.  MAIN COMPOSABLE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomersScreen(
    userId: Int,
    onMenuClick: () -> Unit,
    onCustomerClick: (Int) -> Unit = {}
) {
    var screen by remember { mutableStateOf<CustomersScreen>(CustomersScreen.List) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddCustomer by remember { mutableStateOf(false) }
    var showCsvImport by remember { mutableStateOf(false) }

    val triggerRefresh: () -> Unit = { refreshTrigger++ }

    val handleSuccess: (String) -> Unit = { msg ->
        triggerRefresh()
        screen = CustomersScreen.List
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    val handleError: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            UniversalHeader(
                title = "Company Customers",
                onMenuClick = onMenuClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgPage,
        floatingActionButton = {
            if (screen is CustomersScreen.List) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { showCsvImport = true },
                        containerColor = Success,
                        contentColor = Color.White
                    ) { Icon(Icons.Default.FileUpload, "Import CSV") }

                    ExtendedFloatingActionButton(
                        onClick = { showAddCustomer = true },
                        containerColor = Primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Customer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = screen) {
                CustomersScreen.List -> CustomersListContent(
                    userId = userId,
                    refreshTrigger = refreshTrigger,
                    onCustomerClick = { id, name ->
                        onCustomerClick(id)
                        screen = CustomersScreen.Detail(id, name)
                    }
                )
                is CustomersScreen.Detail -> CustomerDetailContent(
                    userId = userId,
                    customerId = s.customerId,
                    customerName = s.customerName,
                    onBack = { screen = CustomersScreen.List },
                    onOpenLicense = { cid, key, name ->
                        screen = CustomersScreen.LicenseDetail(cid, key, name)
                    },
                    onSuccess = handleSuccess,
                    onError = handleError
                )
                is CustomersScreen.LicenseDetail -> LicenseDetailContent(
                    userId = userId,
                    customerId = s.customerId,
                    licenseKey = s.licenseKey,
                    customerName = s.customerName,
                    onBack = { screen = CustomersScreen.Detail(s.customerId, s.customerName) }
                )
            }
        }
    }

    if (showAddCustomer) {
        AddCustomerDialog(
            userId = userId,
            onDismiss = { showAddCustomer = false },
            onSuccess = { msg ->
                showAddCustomer = false
                handleSuccess(msg)
            },
            onError = handleError
        )
    }

    if (showCsvImport) {
        CsvImportDialog(
            userId = userId,
            onDismiss = { showCsvImport = false },
            onSuccess = { msg ->
                showCsvImport = false
                handleSuccess(msg)
            },
            onError = handleError
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 5.  LIST CONTENT (stats + search + cards)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomersListContent(
    userId: Int,
    refreshTrigger: Int,
    onCustomerClick: (Int, String) -> Unit
) {
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // ── Derived stats (matches PHP: main_licenses only) ──
    val totalCustomers = customers.size
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    var activeLicenses = 0
    var expiringSoon = 0
    var expired = 0
    val now = System.currentTimeMillis()
    val in30 = now + 30L * 24 * 60 * 60 * 1000
    val seenKeys = HashSet<String>()
    customers.forEach { c ->
        c.main_licenses?.forEach { lic ->
            val st = (lic.status ?: "active").lowercase()
            val key = lic.license_key?.trim().orEmpty()
            val keyId = key.ifEmpty { "id:${lic.id}" }
            if (st == "active" || st.isEmpty()) {
                if (seenKeys.add(keyId)) activeLicenses++
            }
            val exp = lic.expiry_date
            if (!exp.isNullOrBlank() && exp != "0000-00-00" && !isTallyMain(lic.licence_type ?: lic.item_name ?: "")) {
                val t = runCatching { today.parse(exp)?.time }.getOrNull()
                if (t != null) {
                    if (t < now) expired++
                    else if (t <= in30) expiringSoon++
                }
            }
        }
    }

    fun fetchCustomers() {
        if (userId <= 0) {
            errorMessage = "user_id required"
            isLoading = false
            return
        }

        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminCustomers(
                    userId = userId,
                    search = searchQuery
                )
                if (response.isSuccessful) {
                    val raw = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    response.toLenientJson()?.let { json ->
                        val obj = json.asJsonObject
                        Log.d(TAG, "Parsed JSON success=${obj.get("success")?.asBoolean}, customers=${obj.getAsJsonArray("customers")?.size()}")
                        val data = AdminCustomersResponse.fromJson(json)
                        if (data.success) {
                            customers = data.customers ?: emptyList()
                            Log.d(TAG, "Loaded ${customers.size} customers")
                        } else {
                            errorMessage = data.error ?: "Failed to load"
                        }
                    } ?: run {
                        Log.e(TAG, "Empty response body. Raw error: $raw")
                        errorMessage = "Empty response"
                    }
                } else {
                    val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    Log.e(TAG, "HTTP ${response.code()}: $errBody")
                    errorMessage = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "list error", e)
                errorMessage = e.message ?: "Network error"
            } finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetchCustomers() }
    LaunchedEffect(refreshTrigger) { fetchCustomers() }
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2 || searchQuery.isEmpty()) {
            delay(400.milliseconds)
            fetchCustomers()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Total", totalCustomers, Color(0xFFDBEAFE), Primary, Icons.Default.People, Modifier.weight(1f))
            StatCard("Active", activeLicenses, Color(0xFFD1FAE5), Success, Icons.Default.Key, Modifier.weight(1f))
            StatCard("Expiring", expiringSoon, Color(0xFFFEF3C7), Warning, Icons.Default.Schedule, Modifier.weight(1f))
            StatCard("Expired", expired, Color(0xFFFEE2E2), Danger, Icons.Default.Warning, Modifier.weight(1f))
        }

        // Search bar
        Surface(color = CardBg, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, company, phone, serial…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        when {
            isLoading && customers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp))
            }
            errorMessage != null && customers.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = Danger, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(errorMessage!!, color = TextMuted, textAlign = TextAlign.Center, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { fetchCustomers() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Retry", fontSize = 13.sp) }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customers.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PeopleOutline, null, tint = TextSubtle, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No customers found", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
                items(customers, key = { it.id }) { customer ->
                    AdminCustomerCard(customer) { onCustomerClick(customer.id, customer.name) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 6.  CUSTOMER CARD (existing) + StatCard helper
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun AdminCustomerCard(customer: Customer, onClick: () -> Unit) {
    val mainLicenses = customer.main_licenses ?: emptyList()
    val licenseCount = (customer.licence_key_count ?: 0).takeIf { it > 0 }
        ?: (customer.license_count ?: 0)
            .takeIf { it > 0 }
        ?: mainLicenses.size

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary, Purple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        customer.name.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                        fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!customer.company.isNullOrBlank()) {
                        Text(customer.company, fontSize = 11.sp, color = TextMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (!customer.amount.isNullOrBlank() && customer.amount != "0" && customer.amount != "0.0") {
                        Text("₹${formatAmount(customer.amount)}", fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, color = Success)
                    }
                    val status = customer.customer_status ?: customer.status ?: "Active"
                    val (bg, fg) = if (status.lowercase() == "active") Color(0xFFD1FAE5) to Color(0xFF065F46)
                    else Color(0xFFFEE2E2) to Color(0xFF991B1B)
                    Box(
                        Modifier.padding(top = 4.dp).background(bg, RoundedCornerShape(99.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(status, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    }
                }
            }
            if (!customer.lead_service.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.background(Color(0xFFEFF6FF), RoundedCornerShape(99.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(customer.lead_service, fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                }
            }
            if (mainLicenses.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(mainLicenses.take(4)) { lic -> LicenseBadge(lic) }
                    if (mainLicenses.size > 4) item {
                        Text("+${mainLicenses.size - 4}", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!customer.phone.isNullOrBlank()) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(11.dp), tint = TextMuted)
                    Spacer(Modifier.width(4.dp))
                    Text(customer.phone, fontSize = 11.sp, color = TextMuted)
                    Spacer(Modifier.width(12.dp))
                }
                if (licenseCount > 0) {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(11.dp), tint = Purple)
                    Spacer(Modifier.width(4.dp))
                    Text("$licenseCount licence(s)", fontSize = 11.sp, color = Purple)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = TextSubtle, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, bg: Color, fg: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(value.toString(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            }
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 7.  CUSTOMER DETAIL CONTENT (hero + cards)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDetailContent(
    userId: Int,
    customerId: Int,
    @Suppress("UNUSED_PARAMETER") customerName: String,
    onBack: () -> Unit,
    onOpenLicense: (Int, String, String) -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    var detail by remember { mutableStateOf<CustomerDetailData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddLicense by remember { mutableStateOf(false) }
    var editingLicense by remember { mutableStateOf<CustomerLicense?>(null) }
    val scope = rememberCoroutineScope()
    val today = remember { java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) } }

    fun load() {
        isLoading = true; error = null
        scope.launch {
            val res = callGet(mapOf("user_id" to userId.toString(), "ajax" to "get_customer", "id" to customerId.toString()))
            val parsed = res.data?.let { parseCustomerDetail(it) }
            if (res.ok && parsed != null) detail = parsed
            else error = res.message ?: "Failed to load"
            isLoading = false
        }
    }
    LaunchedEffect(customerId) { load() }

    if (isLoading && detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp))
        }
        return
    }

    val d = detail
    if (d == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, null, tint = Danger, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(error ?: "Customer not found", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Retry", fontSize = 13.sp) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onBack) { Text("Back to list") }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // ── Hero ──
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1E3A5F), Primary, Purple)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Customer Details", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(d.name.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                            fontWeight = FontWeight.Bold, color = Color.White, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("#${d.customerId}", Color.White.copy(alpha = 0.2f), Color.White)
                            Pill(d.status, Color.White.copy(alpha = 0.25f), Color.White)
                            if (!d.leadService.isNullOrBlank()) Pill(d.leadService, Color.White.copy(alpha = 0.25f), Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentContext = LocalContext.current
                    d.phone?.let {
                        HeroActionBtn(Icons.Default.Phone, "Call", Color(0xFF10B981)) {
                            dialPhone(it, currentContext)
                        }
                        HeroActionBtn(Icons.AutoMirrored.Filled.Message, "WhatsApp", Color(0xFF25D366)) {
                            openWhatsApp(it, currentContext)
                        }
                    }
                    d.email?.let { p ->
                        HeroActionBtn(Icons.Default.Email, "Email", Color(0xFF6366F1)) {
                            sendEmail(p, currentContext)
                        }
                    }
                    HeroActionBtn(Icons.Default.Key, "Add Licence", Color.White.copy(alpha = 0.2f)) {
                        showAddLicense = true
                    }
                }
            }
        }

        Column(Modifier.padding(14.dp)) {

            // ── Contact ──
            SectionCard("Contact", Icons.Default.Person, Purple) {
                InfoGrid(listOf(
                    "Name" to d.name,
                    "Phone" to (d.phone ?: "—"),
                    "Email" to (d.email ?: "—"),
                    "Company" to (d.company ?: "—"),
                    "Amount Paid" to "₹${formatAmount(d.amount)}",
                    "Source" to (d.source ?: "—")
                ))
            }

            // ── Follow-ups ──
            SectionCard("Follow-ups (${d.followups.size})", Icons.Default.History, Primary, badge = d.followups.size.toString()) {
                if (d.followups.isEmpty()) EmptyHint("No follow-ups yet")
                else d.followups.take(6).forEach { fu ->
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(BorderSoft, RoundedCornerShape(7.dp)).padding(9.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatDate(fu.followUpDate), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Spacer(Modifier.width(6.dp))
                                Pill(fu.status ?: "Pending", Color(0xFFFEF3C7), Color(0xFF92400E))
                            }
                            if (!fu.remarks.isNullOrBlank()) {
                                Text(fu.remarks, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }

            // ── Licences ──
            val groups = d.licenseGroups
            val licenceKeyCount = groups.count { g ->
                val key = g.licenseKey
                if (key.isEmpty() || key.startsWith("__nokey_")) false
                else g.items.any { isTallyMain(it.licence_type ?: it.item_name ?: "") }
            }
            SectionCard("Tally Licences ($licenceKeyCount)", Icons.Default.Key, Purple,
                badge = licenceKeyCount.toString(),
                action = {
                    SmallAddBtn { showAddLicense = true }
                }
            ) {
                if (groups.isEmpty()) EmptyHint("No licences yet — tap Add to create one.")
                else groups.forEach { g ->
                    val key = g.licenseKey
                    val items = g.items
                    val mainItems = items.filter { isTallyMain(it.licence_type ?: it.item_name ?: "") }
                    val svcItems = items.filterNot { isTallyMain(it.licence_type ?: it.item_name ?: "") }
                    // Skip pure-service no-key groups
                    if (mainItems.isEmpty() && (key.startsWith("__nokey_") || key.isEmpty())) return@forEach

                    val keyLabel = if (key.startsWith("__nokey_")) "(no key)" else key
                    val canOpen = key.isNotEmpty() && !key.startsWith("__nokey_")
                    val mainLabel = mainItems.firstOrNull()?.let { it.licence_type ?: it.item_name } ?: "Licence"

                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFFDBEAFE)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (canOpen) {
                                Button(
                                    onClick = { onOpenLicense(d.customerId, key, d.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(7.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(keyLabel, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Box(
                                    Modifier.background(Color(0xFF64748B), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(keyLabel, color = Color.White, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                            Pill(mainLabel, Color(0xFFDbeafe), Color(0xFF1D4ED8))
                            Spacer(Modifier.weight(1f))
                            Text("${mainItems.size} licence · ${svcItems.size} svc", fontSize = 10.sp, color = TextMuted)
                        }
                        items.forEach { lic ->
                            val type = lic.licence_type ?: lic.item_name ?: "Software"
                            val isMain = isTallyMain(type)
                            val exp = lic.expiry_date
                            val expMs = exp?.takeIf { it != "0000-00-00" }?.let {
                                runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.time }.getOrNull()
                            }
                            val (stLabel, stBg, stFg) = when {
                                expMs != null && expMs < today.timeInMillis -> Triple("Expired", Color(0xFFFEE2E2), Color(0xFF991B1B))
                                expMs != null && expMs - today.timeInMillis <= 30L * 86400000 -> Triple("Expiring", Color(0xFFFEF3C7), Color(0xFF92400E))
                                else -> Triple(lic.status ?: "active", Color(0xFFDCFCE7), Color(0xFF166534))
                            }
                            Row(
                                Modifier.fillMaxWidth()
                                    .border(0.5.dp, BorderSoft, RoundedCornerShape(0.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Pill(if (isMain) "Main" else "Service", if (isMain) Color(0xFFDBEAFE) else Color(0xFFFCE7F3), if (isMain) Color(0xFF1D4ED8) else Color(0xFF9D174D))
                                Text(type, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(1f))
                                if (expMs != null && !isMain) {
                                    Text("Exp ${formatDate(exp)}", fontSize = 10.sp, color = TextMuted)
                                } else if (isMain) {
                                    Text("No expiry", fontSize = 10.sp, color = TextMuted)
                                }
                                Pill(stLabel, stBg, stFg)
                                IconButton(onClick = { editingLicense = lic }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, "Edit", tint = TextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Works ──
            SectionCard("Works (${d.works.size})", Icons.AutoMirrored.Filled.Assignment, Warning, badge = d.works.size.toString()) {
                if (d.works.isEmpty()) EmptyHint("No works linked")
                else d.works.forEach { w ->
                    val (stBg, stFg) = when (w.status) {
                        "completed" -> Color(0xFFDCFCE7) to Color(0xFF166534)
                        "in_progress" -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
                        else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(BorderSoft, RoundedCornerShape(8.dp)).padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(w.workName ?: "Work", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                            if (!w.leadService.isNullOrBlank()) {
                                Text(w.leadService, fontSize = 10.sp, color = TextMuted)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${formatAmount(w.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            val bal = w.balanceAmount?.toDoubleOrNull() ?: 0.0
                            if (bal > 0) {
                                Text("Bal ₹${formatAmount(w.balanceAmount)}", fontSize = 10.sp, color = Danger)
                            }
                            Pill((w.status ?: "pending").replace("_", " "), stBg, stFg)
                        }
                    }
                }
            }

            // ── Payments ──
            SectionCard("Payments (${d.payments.size})", Icons.Default.Payments, Success, badge = d.payments.size.toString()) {
                if (d.payments.isEmpty()) EmptyHint("No payments")
                else d.payments.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(BorderSoft, RoundedCornerShape(7.dp)).padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("₹${formatAmount(p.amount)}", fontWeight = FontWeight.Bold, color = Success, fontSize = 12.sp)
                            Text("${formatDate(p.paymentDate)} · ${p.paymentMethod ?: ""}", fontSize = 10.sp, color = TextMuted)
                        }
                        if (!p.reference.isNullOrBlank()) {
                            Text(p.reference, fontSize = 10.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showAddLicense) {
        AddEditLicenseDialog(
            mode = LicenseDialogMode.Add(customerId),
            userId = userId,
            onDismiss = { showAddLicense = false },
            onSuccess = { msg -> showAddLicense = false; load(); onSuccess(msg) },
            onError = onError
        )
    }
    editingLicense?.let { lic ->
        AddEditLicenseDialog(
            mode = LicenseDialogMode.Edit(lic, customerId),
            userId = userId,
            onDismiss = { editingLicense = null },
            onSuccess = { msg -> editingLicense = null; load(); onSuccess(msg) },
            onError = onError
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 8.  LICENCE FULL-SCREEN DETAIL
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LicenseDetailContent(
    userId: Int,
    customerId: Int,
    licenseKey: String,
    customerName: String,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf<LicenseDetailData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true; error = null
        scope.launch {
            val res = callGet(mapOf(
                "user_id" to userId.toString(),
                "ajax" to "get_license_detail",
                "customer_id" to customerId.toString(),
                "license_key" to licenseKey
            ))
            val parsed = res.data?.let { parseLicenseDetail(it) }
            if (res.ok && parsed != null) data = parsed
            else error = res.message ?: "Failed to load"
            isLoading = false
        }
    }
    LaunchedEffect(customerId, licenseKey) { load() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF0F2F5))) {

        // ── Header ──
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E3A5F))))
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("Licence detail", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text(licenseKey, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = data?.let {
                        val sb = StringBuilder(it.customerName)
                        if (!it.customerCompany.isNullOrBlank()) sb.append(" · ").append(it.customerCompany)
                        if (!it.customerPhone.isNullOrBlank()) sb.append(" · ").append(it.customerPhone)
                        sb.toString()
                    } ?: customerName
                    Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Retry", fontSize = 12.sp) }
                }
            }
            data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No data", color = TextMuted) }
            else -> {
                val d = data!!
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {

                    // Attached
                    LicSection("Attached products & services", "${d.attached.size}", Icons.Default.Link, Primary) {
                        if (d.attached.isEmpty()) EmptyHint("Nothing attached under this licence yet.")
                        else d.attached.forEach { a ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Pill(if (a.isMain) "Main" else "Service / Addon",
                                    if (a.isMain) Color(0xFFDBEAFE) else Color(0xFFFCE7F3),
                                    if (a.isMain) Color(0xFF1D4ED8) else Color(0xFF9D174D))
                                Text(a.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(1f))
                                if (!a.expiryDate.isNullOrBlank() && a.expiryDate != "0000-00-00" && !a.isMain) {
                                    Text("Exp ${formatDate(a.expiryDate)}", fontSize = 10.sp, color = TextMuted)
                                }
                                Text(a.status ?: "active", fontSize = 10.sp, color = TextMuted)
                            }
                            HorizontalDivider(color = BorderSoft)
                        }
                    }

                    // Opportunities
                    LicSection("Opportunities — not yet on this licence", "${d.opportunities.size}", Icons.Default.Lightbulb, Warning) {
                        Text(
                            "Services from the catalogue that are not yet attached to this licence number.",
                            fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (d.opportunities.isEmpty()) EmptyHint("All catalogue addons already attached — great coverage.")
                        else FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            d.opportunities.forEach { o ->
                                Row(
                                    Modifier
                                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(99.dp))
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(99.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AddCircle, null, tint = Success, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(o.name, fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Proformas
                    LicSection("Related proformas", "${d.proformas.size}", Icons.Default.Description, Purple) {
                        if (d.proformas.isEmpty()) EmptyHint("No proformas linked to this licence / leads.")
                        else {
                            Column(Modifier.fillMaxWidth().background(BorderSoft, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                d.proformas.forEachIndexed { idx, p ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(p.proformaNo ?: "#${p.id}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        if (p.serialMatched) Pill("serial match", Color(0xFFDBEAFE), Color(0xFF1D4ED8))
                                        Spacer(Modifier.width(6.dp))
                                        Text(p.service ?: "—", fontSize = 11.sp, color = TextMuted, modifier = Modifier.width(80.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("₹${formatAmount(p.total)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (idx < d.proformas.lastIndex) HorizontalDivider(color = Color.White, thickness = 1.dp)
                                }
                            }
                        }
                    }

                    // Payments
                    LicSection("Related payments", "${d.payments.size}", Icons.Default.AccountBalanceWallet, Success) {
                        if (d.payments.isEmpty()) EmptyHint("No payments on this customer yet.")
                        else {
                            Column(Modifier.fillMaxWidth().background(BorderSoft, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                d.payments.forEachIndexed { idx, p ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(p.reference ?: "—", fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("₹${formatAmount(p.amount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Success)
                                        Spacer(Modifier.width(6.dp))
                                        Text(formatDate(p.paymentDate), fontSize = 10.sp, color = TextMuted)
                                    }
                                    if (idx < d.payments.lastIndex) HorizontalDivider(color = Color.White, thickness = 1.dp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 9.  ADD CUSTOMER DIALOG (Work-Completed Flow)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private val MainProducts = listOf("Tally Silver", "Tally Gold", "Tally Server", "Tally Prime")
private val AddonProducts = listOf("AMC", "TSS", "TDL", "WhatsApp", "Cloud", "BIZAPP", "BIZ Analyst", "Upgrade")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomerDialog(
    userId: Int,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var workCompleted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Form state
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("Direct") }
    var mainProduct by remember { mutableStateOf("") }
    var licenseKey by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var selectedAddons by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(14.dp),
            color = CardBg
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().background(Primary).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("New Customer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (!isSubmitting) onDismiss() }) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {

                    if (step == 1) {
                        // Step 1
                        Text("Step 1 — Has the work already been completed?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                        Spacer(Modifier.height(6.dp))
                        Text("If work is not finished, create this as a Lead first.", fontSize = 12.sp, color = TextMuted)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { workCompleted = false; step = 1 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("No – Work not completed", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { workCompleted = true; step = 2 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Yes – Work completed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (!workCompleted) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                color = Color(0xFFFFF7ED),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Please first create this as a Lead and complete the work.", fontSize = 12.sp, color = Color(0xFF92400E))
                                    Spacer(Modifier.height(6.dp))
                                    Text("After the work is done, come back and convert to Customer.", fontSize = 12.sp, color = Color(0xFF92400E))
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Go to Leads →", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Step 2
                        Text("Step 2 — Customer & Licence", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                        Spacer(Modifier.height(10.dp))

                        FieldLabel("Customer Name *")
                        FormField(value = name, onChange = { name = it }, placeholder = "Full name")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Phone")
                                FormField(value = phone, onChange = { phone = it }, placeholder = "9876543210", keyboardType = KeyboardType.Phone)
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Email")
                                FormField(value = email, onChange = { email = it }, placeholder = "name@company.com", keyboardType = KeyboardType.Email)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Company")
                                FormField(value = company, onChange = { company = it }, placeholder = "Company name")
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Source")
                                FormField(value = source, onChange = { source = it }, placeholder = "Direct")
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = BorderSoft)
                        Spacer(Modifier.height(10.dp))

                        Text("Main Product (requires Licence Number)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Primary)

                        Spacer(Modifier.height(6.dp))
                        // Main product radio chips
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MainProducts.forEach { p ->
                                val selected = mainProduct == p
                                Surface(
                                    color = if (selected) Color(0xFFDBEAFE) else CardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (selected) 1.5.dp else 1.dp,
                                        if (selected) Primary else Border
                                    ),
                                    modifier = Modifier.clickable { mainProduct = p }
                                ) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = selected, onClick = { mainProduct = p }, colors = RadioButtonDefaults.colors(selectedColor = Primary))
                                        Text(p, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextDark)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1.4f)) {
                                FieldLabel("Licence / Serial Number *")
                                FormField(value = licenseKey, onChange = { licenseKey = it.trim() }, placeholder = "e.g. ya1234567890", monospace = true)
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Expiry Date")
                                FormField(value = expiry, onChange = { expiry = it }, placeholder = "YYYY-MM-DD")
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("Addons for this Licence (optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Success)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AddonProducts.forEach { a ->
                                val selected = selectedAddons.contains(a)
                                Surface(
                                    color = if (selected) Color(0xFFD1FAE5) else CardBg,
                                    shape = RoundedCornerShape(7.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (selected) 1.5.dp else 1.dp,
                                        if (selected) Success else Border
                                    ),
                                    modifier = Modifier.clickable {
                                        selectedAddons = if (selected) selectedAddons - a else selectedAddons + a
                                    }
                                ) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selected,
                                            onCheckedChange = { checked ->
                                                selectedAddons = if (checked) selectedAddons + a else selectedAddons - a
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Success),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(a, fontSize = 12.sp, color = TextDark)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Footer
                Row(
                    Modifier.fillMaxWidth().background(BorderSoft).padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
                    Spacer(Modifier.width(6.dp))
                    if (step == 2) {
                        Button(
                            enabled = !isSubmitting && name.isNotBlank() && mainProduct.isNotBlank() && licenseKey.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            onClick = {
                                isSubmitting = true
                                scope.launch {
                                    val res = callPost(mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "add_customer_with_license",
                                        "work_completed" to "yes",
                                        "name" to name.trim(),
                                        "phone" to phone.trim(),
                                        "email" to email.trim(),
                                        "company" to company.trim(),
                                        "source" to source.trim(),
                                        "main_product" to mainProduct,
                                        "license_key" to licenseKey.trim(),
                                        "expiry_date" to expiry.trim(),
                                        "addons" to selectedAddons.joinToString(",")
                                    ))
                                    isSubmitting = false
                                    if (res.ok) onSuccess(res.message ?: "Customer created")
                                    else onError(res.message ?: "Failed to create")
                                }
                            }
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text("Create Customer + Licence", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 10.  ADD / EDIT LICENCE DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private sealed class LicenseDialogMode {
    data class Add(val customerId: Int) : LicenseDialogMode()
    data class Edit(val license: CustomerLicense, val customerId: Int) : LicenseDialogMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditLicenseDialog(
    mode: LicenseDialogMode,
    userId: Int,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val isEdit = mode is LicenseDialogMode.Edit
    val customerId = when (mode) {
        is LicenseDialogMode.Add -> mode.customerId
        is LicenseDialogMode.Edit -> mode.customerId
    }
    val initial = mode as? LicenseDialogMode.Edit

    var type by remember { mutableStateOf(initial?.license?.licence_type ?: initial?.license?.item_name ?: "") }
    var serial by remember { mutableStateOf(initial?.license?.license_key ?: "") }
    var expiry by remember { mutableStateOf(initial?.license?.expiry_date ?: "") }
    var status by remember { mutableStateOf(initial?.license?.status?.replaceFirstChar { it.uppercase() } ?: "Active") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isTallyMainSelected = isTallyMain(type)

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(14.dp),
            color = CardBg
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(if (isEdit) Primary else Purple).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isEdit) Icons.Default.Edit else Icons.Default.Key, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEdit) "Edit Licence" else "Add Licence", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                }

                Column(Modifier.padding(16.dp)) {
                    FieldLabel("Product / Item *")
                    // Dropdown-like select
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            placeholder = { Text("Select product") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            Text("Main Products", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            MainProducts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = { type = p; expanded = false }
                                )
                            }
                            HorizontalDivider()
                            Text("Addons", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            AddonProducts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = { type = p; expanded = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    FieldLabel("Licence / Serial Number${if (isTallyMainSelected) " *" else " (optional for addons)"}")
                    FormField(
                        value = serial, onChange = { serial = it.trim() },
                        placeholder = if (isTallyMainSelected) "Required for Tally" else "Leave empty to bind to existing key",
                        monospace = true
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            FieldLabel("Expiry")
                            FormField(
                                value = expiry, onChange = { expiry = it },
                                placeholder = "YYYY-MM-DD",
                                enabled = !isTallyMainSelected
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            FieldLabel("Status")
                            var statusExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = statusExpanded,
                                onExpandedChange = { statusExpanded = !statusExpanded }
                            ) {
                                OutlinedTextField(
                                    value = status,
                                    onValueChange = { status = it },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                    listOf("Active", "Inactive").forEach { s ->
                                        DropdownMenuItem(text = { Text(s) }, onClick = { status = s; statusExpanded = false })
                                    }
                                }
                            }
                        }
                    }

                    if (isEdit) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isSubmitting = true
                                    val res = callPost(mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "delete_license",
                                        "license_id" to (initial?.license?.id?.toString() ?: "")
                                    ))
                                    isSubmitting = false
                                    if (res.ok) onSuccess(res.message ?: "Licence deleted")
                                    else onError(res.message ?: "Delete failed")
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete this licence", fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().background(BorderSoft).padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        enabled = !isSubmitting && type.isNotBlank() && (!isTallyMainSelected || serial.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        onClick = {
                            isSubmitting = true
                            scope.launch {
                                val res = if (isEdit) {
                                    callPost(mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "edit_license",
                                        "license_id" to (initial?.license?.id?.toString() ?: ""),
                                        "customer_id" to customerId.toString(),
                                        "license_type" to type,
                                        "serial_number" to serial,
                                        "expiry_date" to expiry,
                                        "status" to status.lowercase()
                                    ))
                                } else {
                                    callPost(mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "add_license",
                                        "customer_id" to customerId.toString(),
                                        "license_type" to type,
                                        "serial_number" to serial,
                                        "expiry_date" to expiry,
                                        "status" to status.lowercase()
                                    ))
                                }
                                isSubmitting = false
                                if (res.ok) onSuccess(res.message ?: "Saved")
                                else onError(res.message ?: "Failed")
                            }
                        }
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isEdit) "Update" else "Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 11.  CSV IMPORT DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvImportDialog(
    userId: Int,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedName = uri.lastPathSegment?.substringAfterLast('/') ?: "customers.csv"
        }
    }

    Dialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(14.dp),
            color = CardBg
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(Success).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FileUpload, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Customers (CSV)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                }

                Column(Modifier.padding(16.dp)) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("CSV columns required:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Primary)
                            Spacer(Modifier.height(4.dp))
                            Text("name, email, phone, company, source", fontSize = 10.sp, color = TextMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("license_key, tally_product, addons, addon_expiry", fontSize = 10.sp, color = TextMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Spacer(Modifier.height(6.dp))
                            Text("• tally_product = Tally Silver/Gold/Server/Prime", fontSize = 10.sp, color = TextMuted)
                            Text("• addons = comma-separated (AMC,TSS,TDL…)", fontSize = 10.sp, color = TextMuted)
                            Text("• Phone: keep as Text in Excel to avoid 9.88E+09", fontSize = 10.sp, color = TextMuted)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Select CSV File")
                    OutlinedButton(
                        onClick = { picker.launch("text/csv") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(selectedName ?: "Choose CSV file…", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (selectedName != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("Selected: $selectedName", fontSize = 10.5.sp, color = Success, fontWeight = FontWeight.Medium)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().background(BorderSoft).padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isUploading) { Text("Cancel") }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        enabled = !isUploading && selectedUri != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Success),
                        onClick = {
                            val uri = selectedUri ?: return@Button
                            isUploading = true
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    copyUriToCache(uri, context)
                                }
                                if (file == null) {
                                    isUploading = false
                                    onError("Could not read file")
                                    return@launch
                                }
                                val res = callUpload(
                                    fieldParams = mapOf("user_id" to userId.toString(), "action" to "import_customers"),
                                    file = file
                                )
                                isUploading = false
                                if (res.ok) onSuccess(res.message ?: "Imported successfully")
                                else onError(res.message ?: "Import failed")
                            }
                        }
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 12.  SHARED SMALL COMPOSABLES
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(
        Modifier.background(bg, RoundedCornerShape(99.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    badge: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFFFAFBFC)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.background(Primary, CircleShape).padding(horizontal = 8.dp, vertical = 1.dp)) {
                        Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                action?.invoke()
            }
            Column(Modifier.padding(12.dp), content = content)
        }
    }
}

@Composable
private fun LicSection(title: String, badge: String, icon: ImageVector, iconColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFFFAFBFC)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.background(iconColor, CircleShape).padding(horizontal = 8.dp, vertical = 1.dp)) {
                    Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(14.dp), content = content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoGrid(items: List<Pair<String, String>>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 2
    ) {
        items.forEach { (label, value) ->
            Box(
                Modifier.weight(1f, fill = false).fillMaxWidth(0.48f).background(BorderSoft, RoundedCornerShape(7.dp)).padding(9.dp)
            ) {
                Column {
                    Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(value.ifBlank { "—" }, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, color = TextSubtle)
    }
}

@Composable
private fun SmallAddBtn(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Success),
        shape = RoundedCornerShape(6.dp)
    ) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text("Add", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HeroActionBtn(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Color.White),
        shape = RoundedCornerShape(7.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(bottom = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    monospace: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        enabled = enabled,
        textStyle = if (monospace) TextStyle(
            fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        ) else TextStyle(fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedIndicatorColor = Primary,
            unfocusedIndicatorColor = Border,
            disabledContainerColor = Color(0xFFF1F5F9),
            disabledIndicatorColor = Border
        )
    )
}

@Composable
private fun LicenseBadge(lic: CustomerLicense) {
    val type = lic.licence_type ?: lic.license_type ?: lic.item_name ?: "Tally"
    val (bg, fg) = when {
        type.contains("Gold", true)   -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        type.contains("Silver", true) -> Color(0xFFE5E7EB) to Color(0xFF374151)
        type.contains("Server", true) -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)
        type.contains("Prime", true)  -> Color(0xFFEDE9FE) to Color(0xFF6D28D9)
        else -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)
    }
    val serial = lic.serial_number ?: lic.license_key ?: ""
    Box(
        Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (serial.isNotBlank()) "$type · $serial" else type,
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 13.  UTILS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private fun formatAmount(value: String?): String {
    if (value.isNullOrBlank()) return "0"
    return try {
        val clean = value.replace(",", "")
        val d = clean.toDoubleOrNull() ?: 0.0
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Locale.forLanguageTag("en-IN")
        } else {
            Locale("en", "IN")
        }
        NumberFormat.getNumberInstance(locale).format(d.toLong())
    } catch (_: Exception) { value }
}

private fun formatDate(d: String?): String {
    if (d.isNullOrBlank() || d == "0000-00-00") return "N/A"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date = parser.parse(d.substringBefore(" ")) ?: return d
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)
    } catch (_: Exception) { d }
}

private fun dialPhone(num: String, context: Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL, num.toUri())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

private fun openWhatsApp(num: String, context: Context) {
    runCatching {
        val clean = num.replace(Regex("[^0-9]"), "")
        val url = "https://wa.me/$clean"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

private fun sendEmail(addr: String, context: Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:$addr".toUri())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    }
}