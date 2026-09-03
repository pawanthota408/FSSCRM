package com.fsscrm.ui.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "AdminPayroll"

private fun parseJsonBoolean(element: com.google.gson.JsonElement?): Boolean {
    if (element == null || element.isJsonNull) return false
    return try {
        if (element.isJsonPrimitive) {
            val prim = element.asJsonPrimitive
            when {
                prim.isBoolean -> prim.asBoolean
                prim.isNumber -> prim.asInt == 1 || prim.asDouble == 1.0
                prim.isString -> {
                    val s = prim.asString.trim().lowercase()
                    s == "1" || s == "true" || s == "yes"
                }
                else -> false
            }
        } else false
    } catch (_: Exception) {
        false
    }
}

data class PayrollItem(
    val id: Int = 0,
    val employeeId: Int = 0,
    val employeeName: String = "",
    val employeeCode: String = "",
    val payrollMonth: Int = 0,
    val payrollYear: Int = 0,
    val basicSalary: String = "0",
    val houseRentAllowance: String = "0",
    val dearnessAllowance: String = "0",
    val transportAllowance: String = "0",
    val medicalAllowance: String = "0",
    val specialAllowance: String = "0",
    val totalAllowances: String = "0",
    val grossSalary: String = "0",
    val pfDeduction: String = "0",
    val professionalTax: String = "0",
    val incomeTax: String = "0",
    val otherDeductions: String = "0",
    val totalDeductions: String = "0",
    val netSalary: String = "0",
    val status: String = "pending",
    val paymentDate: String? = null,
    val paymentMethod: String? = null,
    val transactionId: String? = null,
    val upiId: String? = null,
    val employerUpiId: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val ifscCode: String? = null
)

data class PayrollStats(
    val totalRecords: Int = 0,
    val pendingCount: Int = 0,
    val processedCount: Int = 0,
    val paidCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalSalary: Double = 0.0
)

data class EmpOption(
    val id: Int,
    val name: String,
    val code: String,
    val hasSettings: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPayrollScreen(
    userId: Int,
    onMenuClick: () -> Unit
) {
    val cal = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedStatus by remember { mutableStateOf("all") }
    var searchInput by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf("") }

    var records by remember { mutableStateOf<List<PayrollItem>>(emptyList()) }
    var stats by remember { mutableStateOf(PayrollStats()) }
    var employees by remember { mutableStateOf<List<EmpOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showGenerate by remember { mutableStateOf(false) }
    var showAutoGenerateAllConfirm by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showMarkPaid by remember { mutableStateOf<PayrollItem?>(null) }
    var markPaidMethod by remember { mutableStateOf("Bank Transfer") }
    var showDetail by remember { mutableStateOf<PayrollItem?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun parseRecord(o: JsonObject) = PayrollItem(
        id = o.get("id")?.asInt ?: 0,
        employeeId = o.get("employee_id")?.asInt ?: 0,
        employeeName = o.get("employee_name")?.asString ?: "Unknown",
        employeeCode = o.get("employee_code")?.asString ?: "",
        payrollMonth = o.get("payroll_month")?.asInt ?: 0,
        payrollYear = o.get("payroll_year")?.asInt ?: 0,
        basicSalary = o.get("basic_salary")?.asString ?: "0",
        houseRentAllowance = o.get("house_rent_allowance")?.asString ?: "0",
        dearnessAllowance = o.get("dearness_allowance")?.asString ?: "0",
        transportAllowance = o.get("transport_allowance")?.asString ?: "0",
        medicalAllowance = o.get("medical_allowance")?.asString ?: "0",
        specialAllowance = o.get("special_allowance")?.asString ?: "0",
        totalAllowances = o.get("total_allowances")?.asString ?: "0",
        grossSalary = o.get("gross_salary")?.asString ?: "0",
        pfDeduction = o.get("pf_deduction")?.asString ?: "0",
        professionalTax = o.get("professional_tax")?.asString ?: "0",
        incomeTax = o.get("income_tax")?.asString ?: "0",
        otherDeductions = o.get("other_deductions")?.asString ?: "0",
        totalDeductions = o.get("total_deductions")?.asString ?: "0",
        netSalary = o.get("net_salary")?.asString ?: "0",
        status = o.get("status")?.asString ?: "pending",
        paymentDate = o.get("payment_date")?.takeIf { !it.isJsonNull }?.asString,
        paymentMethod = o.get("payment_method")?.takeIf { !it.isJsonNull }?.asString,
        transactionId = o.get("transaction_id")?.takeIf { !it.isJsonNull }?.asString,
        upiId = o.get("upi_id")?.takeIf { !it.isJsonNull }?.asString ?: o.get("upi")?.takeIf { !it.isJsonNull }?.asString,
        employerUpiId = o.get("employer_upi_id")?.takeIf { !it.isJsonNull }?.asString ?: o.get("employer_upi")?.takeIf { !it.isJsonNull }?.asString,
        bankName = o.get("settings_bank_name")?.takeIf { !it.isJsonNull }?.asString ?: o.get("bank_name")?.takeIf { !it.isJsonNull }?.asString,
        accountNumber = o.get("settings_account_number")?.takeIf { !it.isJsonNull }?.asString ?: o.get("account_number")?.takeIf { !it.isJsonNull }?.asString,
        ifscCode = o.get("settings_ifsc")?.takeIf { !it.isJsonNull }?.asString ?: o.get("ifsc_code")?.takeIf { !it.isJsonNull }?.asString
    )

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
                val response = RetrofitClient.apiService.getAdminPayroll(
                    mapOf(
                        "user_id" to userId.toString(),
                        "month" to selectedMonth.toString(),
                        "year" to selectedYear.toString(),
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
                        records = root.getAsJsonArray("records")?.map { parseRecord(it.asJsonObject) } ?: emptyList()
                        root.getAsJsonObject("stats")?.let { s ->
                            stats = PayrollStats(
                                totalRecords = s.get("total_records")?.asInt ?: 0,
                                pendingCount = s.get("pending_count")?.asInt ?: 0,
                                processedCount = s.get("processed_count")?.asInt ?: 0,
                                paidCount = s.get("paid_count")?.asInt ?: 0,
                                cancelledCount = s.get("cancelled_count")?.asInt ?: 0,
                                totalSalary = s.get("total_salary")?.asDouble ?: 0.0
                            )
                        }
                        employees = root.getAsJsonArray("employees")?.map {
                            val e = it.asJsonObject
                            val hasSet = parseJsonBoolean(
                                e.get("has_settings")
                                    ?: e.get("has_setting")
                                    ?: e.get("hasSettings")
                                    ?: e.get("is_configured")
                            ) || (e.has("basic_salary") && !e.get("basic_salary").isJsonNull && e.get("basic_salary").asString.isNotBlank() && e.get("basic_salary").asString != "0")
                            EmpOption(
                                id = e.get("id")?.asInt ?: 0,
                                name = e.get("name")?.asString ?: "",
                                code = e.get("employee_code")?.asString ?: "",
                                hasSettings = hasSet
                            )
                        } ?: emptyList()
                    }
                } else errorMessage = "Server ${response.code()}"
            } catch (e: Exception) {
                Log.e(TAG, "fetch", e)
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedMonth, selectedYear, selectedStatus, activeSearch) { fetch() }

    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    Scaffold(
        topBar = { UniversalHeader(title = "Company Payroll", onMenuClick = onMenuClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Auto Generate All FAB
                ExtendedFloatingActionButton(
                    onClick = { showAutoGenerateAllConfirm = true },
                    containerColor = Color(0xFFD97706),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Auto All (${monthNames.getOrElse(selectedMonth - 1) { "" }})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Settings FAB
                    FloatingActionButton(
                        onClick = { showSettings = true },
                        containerColor = Color(0xFF6B7280),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Salary Settings")
                    }
                    // Generate FAB
                    ExtendedFloatingActionButton(
                        onClick = { showGenerate = true },
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Calculate, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Stats
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStat("Records", stats.totalRecords.toString(), Modifier.weight(1f))
                MiniStat("Paid", stats.paidCount.toString(), Modifier.weight(1f))
                MiniStat("Processed", stats.processedCount.toString(), Modifier.weight(1f))
                MiniStat("Net ₹", formatInr(stats.totalSalary.toString()), Modifier.weight(1.2f))
            }

            // Filters
            Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimpleDropdown(
                            label = monthNames.getOrElse(selectedMonth - 1) { "Month" },
                            options = (1..12).map { it to monthNames[it - 1] },
                            selected = selectedMonth,
                            onSelect = { selectedMonth = it },
                            modifier = Modifier.weight(1f)
                        )
                        val years = ((cal.get(Calendar.YEAR) - 2)..cal.get(Calendar.YEAR)).toList()
                        SimpleDropdown(
                            label = selectedYear.toString(),
                            options = years.map { it to it.toString() },
                            selected = selectedYear,
                            onSelect = { selectedYear = it },
                            modifier = Modifier.weight(1f)
                        )
                        val statusOpts = listOf(
                            "all" to "All", "pending" to "Pending", "processed" to "Processed",
                            "paid" to "Paid", "cancelled" to "Cancelled"
                        )
                        SimpleDropdownStr(
                            label = statusOpts.find { it.first == selectedStatus }?.second ?: "All",
                            options = statusOpts,
                            selected = selectedStatus,
                            onSelect = { selectedStatus = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Employee name / code", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = { activeSearch = searchInput.trim() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                        }
                    }
                }
            }

            Text(
                "${records.size} records · ${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear",
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
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { fetch() }) { Text("Retry") }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { it.id }) { rec ->
                        PayrollCard(
                            record = rec,
                            monthName = monthNames.getOrElse(rec.payrollMonth - 1) { "" },
                            onView = { showDetail = rec },
                            onMarkPaid = {
                                markPaidMethod = if (!rec.upiId.isNullOrBlank()) "UPI" else "Bank Transfer"
                                showMarkPaid = rec
                            },
                            onPayUpi = {
                                if (rec.upiId.isNullOrBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Employee UPI ID is not configured") }
                                } else {
                                    markPaidMethod = "UPI"
                                    showMarkPaid = rec
                                    launchUpiApp(
                                        context = context,
                                        packageName = null,
                                        upiId = rec.upiId,
                                        employeeName = rec.employeeName,
                                        netSalary = rec.netSalary,
                                        monthName = monthNames.getOrElse(rec.payrollMonth - 1) { "" },
                                        year = rec.payrollYear
                                    )
                                }
                            }
                        )
                    }
                    if (records.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No payroll records", color = Color.Gray)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showAutoGenerateAllConfirm) {
        AlertDialog(
            onDismissRequest = { showAutoGenerateAllConfirm = false },
            title = { Text("Auto Generate All Payroll") },
            text = { Text("Auto generate/refresh payroll for ALL employees with salary settings for ${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear? Already PAID records will be skipped.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    onClick = {
                        showAutoGenerateAllConfirm = false
                        scope.launch {
                            isLoading = true
                            val res = postPayroll(
                                mapOf(
                                    "user_id" to userId.toString(),
                                    "action" to "generate_all_payroll",
                                    "month" to selectedMonth.toString(),
                                    "year" to selectedYear.toString()
                                )
                            )
                            snackbarHostState.showSnackbar(res.second ?: if (res.first) "Auto generate completed!" else "Failed")
                            fetch()
                        }
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoGenerateAllConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ==================== GENERATE DIALOG ====================
    if (showGenerate) {
        GeneratePayrollDialog(
            employees = employees,
            defaultMonth = selectedMonth,
            defaultYear = selectedYear,
            onDismiss = { showGenerate = false },
            onGenerate = { empId, m, y ->
                scope.launch {
                    val res = postPayroll(
                        mapOf(
                            "user_id" to userId.toString(),
                            "action" to "generate_payroll",
                            "employee_id" to empId.toString(),
                            "month" to m.toString(),
                            "year" to y.toString()
                        )
                    )
                    showGenerate = false
                    snackbarHostState.showSnackbar(res.second ?: if (res.first) "Done" else "Failed")
                    if (res.first) fetch()
                }
            }
        )
    }

    // ==================== SALARY SETTINGS DIALOG ====================
    if (showSettings) {
        SalarySettingsDialog(
            employees = employees,
            userId = userId,
            onDismiss = { showSettings = false },
            onSaved = { savedEmpId ->
                showSettings = false
                if (savedEmpId > 0) {
                    employees = employees.map {
                        if (it.id == savedEmpId) it.copy(hasSettings = true) else it
                    }
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Settings saved")
                    fetch() // refresh has_settings flags
                }
            }
        )
    }

    // ==================== MARK PAID ====================
    showMarkPaid?.let { rec ->
        MarkPaidDialog(
            record = rec,
            monthName = monthNames.getOrElse(rec.payrollMonth - 1) { "" },
            initialMethod = markPaidMethod,
            onDismiss = { showMarkPaid = null },
            onConfirm = { date, method, txn ->
                scope.launch {
                    val res = postPayroll(
                        mapOf(
                            "user_id" to userId.toString(),
                            "action" to "update_status",
                            "payroll_id" to rec.id.toString(),
                            "status" to "paid",
                            "payment_date" to date,
                            "payment_method" to method,
                            "transaction_id" to txn
                        )
                    )
                    showMarkPaid = null
                    snackbarHostState.showSnackbar(res.second ?: if (res.first) "Marked paid" else "Failed")
                    if (res.first) fetch()
                }
            }
        )
    }

    // ==================== DETAIL ====================
    showDetail?.let { rec ->
        PayrollDetailDialog(
            record = rec,
            monthName = monthNames.getOrElse(rec.payrollMonth - 1) { "" },
            onDismiss = { showDetail = null },
            onMarkPaid = {
                markPaidMethod = if (!rec.upiId.isNullOrBlank()) "UPI" else "Bank Transfer"
                showMarkPaid = rec
            },
            onPayUpi = {
                if (rec.upiId.isNullOrBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Employee UPI ID is not configured") }
                } else {
                    showDetail = null
                    markPaidMethod = "UPI"
                    showMarkPaid = rec
                    launchUpiApp(
                        context = context,
                        packageName = null,
                        upiId = rec.upiId,
                        employeeName = rec.employeeName,
                        netSalary = rec.netSalary,
                        monthName = monthNames.getOrElse(rec.payrollMonth - 1) { "" },
                        year = rec.payrollYear
                    )
                }
            }
        )
    }
}

// ==================== NETWORK HELPER ====================
private suspend fun postPayroll(params: Map<String, String>): Pair<Boolean, String?> {
    return try {
        val resp = RetrofitClient.apiService.adminPayrollPost(params)
        if (resp.isSuccessful) {
            val body = resp.toLenientJson()?.asJsonObject
            val ok = body?.get("success")?.asBoolean == true
            Pair(ok, body?.get("message")?.asString ?: body?.get("error")?.asString)
        } else Pair(false, "Server ${resp.code()}")
    } catch (e: Exception) {
        Pair(false, e.message)
    }
}

private fun launchUpiApp(
    context: Context,
    packageName: String?,
    upiId: String,
    employeeName: String,
    netSalary: String,
    monthName: String,
    year: Int
) {
    try {
        val cleanAmount = netSalary.replace(",", "").replace("₹", "").trim()
        val formattedAmount = String.format(Locale.US, "%.2f", cleanAmount.toDoubleOrNull() ?: 0.0)
        val note = "Salary $monthName $year"

        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId.trim())
            .appendQueryParameter("pn", employeeName.trim())
            .appendQueryParameter("am", formattedAmount)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", note)
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (!packageName.isNullOrBlank()) {
            intent.setPackage(packageName)
        }
        val finalIntent = if (!packageName.isNullOrBlank()) intent else Intent.createChooser(intent, "Pay Salary via UPI")
        context.startActivity(finalIntent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to launch UPI app ($packageName)", e)
        copyToClipboard(context, "UPI ID", upiId)
        android.widget.Toast.makeText(context, "Copied UPI ID: $upiId. Please open your payment app.", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank() || text == "—") return
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, "Copied $label: $text", android.widget.Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
}

// ==================== UI COMPONENTS ====================

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryIndigo, maxLines = 1)
            Text(label, fontSize = 9.sp, color = Color(0xFF6B7280))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }, modifier) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { (k, t) ->
                DropdownMenuItem(
                    text = {
                        Text(t, fontSize = 13.sp, fontWeight = if (k == selected) FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = { onSelect(k); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdownStr(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }, modifier) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { (k, t) ->
                DropdownMenuItem(
                    text = {
                        Text(t, fontSize = 13.sp, fontWeight = if (k == selected) FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = { onSelect(k); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PayrollCard(
    record: PayrollItem,
    monthName: String,
    onView: () -> Unit,
    onMarkPaid: () -> Unit,
    onPayUpi: (() -> Unit)? = null
) {
    val st = record.status.lowercase()
    val (bg, fg) = when (st) {
        "paid" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "cancelled" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        "processed" -> Color(0xFFDBEAFE) to Color(0xFF1E3A5F)
        else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
    }

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
                    Text(record.employeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$monthName ${record.payrollYear}" + if (record.employeeCode.isNotBlank()) " · ${record.employeeCode}" else "",
                        fontSize = 12.sp, color = Color(0xFF6B7280)
                    )
                }
                Text("₹${formatInr(record.netSalary)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = PrimaryIndigo)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        record.status.replaceFirstChar { it.uppercase() },
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg
                    )
                }
                if (st == "processed" || st == "pending") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!record.upiId.isNullOrBlank() && onPayUpi != null) {
                            OutlinedButton(
                                onClick = onPayUpi,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(12.dp), tint = Color(0xFF0284C7))
                                Spacer(Modifier.width(4.dp))
                                Text("Pay UPI", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        TextButton(onClick = onMarkPaid, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("Mark Paid", fontSize = 12.sp, color = Color(0xFF059669))
                        }
                    }
                } else if (record.paymentDate != null) {
                    Text("Paid: ${record.paymentDate}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ==================== GENERATE DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratePayrollDialog(
    employees: List<EmpOption>,
    defaultMonth: Int,
    defaultYear: Int,
    onDismiss: () -> Unit,
    onGenerate: (empId: Int, month: Int, year: Int) -> Unit
) {
    var empId by remember { mutableIntStateOf(0) }
    var month by remember { mutableIntStateOf(defaultMonth) }
    var year by remember { mutableIntStateOf(defaultYear) }
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
            Column(Modifier.padding(16.dp)) {
                Text("Generate Payroll", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                var empExp by remember { mutableStateOf(false) }
                val selectedEmp = employees.find { it.id == empId }
                val empLabel = selectedEmp?.let {
                    "${it.name} (${it.code})" + if (!it.hasSettings) " ⚠ No settings" else ""
                } ?: "Select employee"

                ExposedDropdownMenuBox(empExp, { empExp = !empExp }) {
                    OutlinedTextField(
                        value = empLabel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(empExp) },
                        shape = RoundedCornerShape(8.dp),
                        isError = selectedEmp != null && !selectedEmp.hasSettings
                    )
                    ExposedDropdownMenu(empExp, { empExp = false }) {
                        employees.forEach { e ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${e.name} (${e.code})" + if (!e.hasSettings) "  • No settings" else "",
                                        color = if (!e.hasSettings) Color(0xFFDC2626) else Color.Unspecified
                                    )
                                },
                                onClick = { empId = e.id; empExp = false }
                            )
                        }
                    }
                }

                if (selectedEmp != null && !selectedEmp.hasSettings) {
                    Text(
                        "⚠ Note: No salary settings configured for this employee. Default salary values will be used.",
                        color = Color(0xFFD97706),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleDropdown(
                        label = monthNames[month - 1],
                        options = (1..12).map { it to monthNames[it - 1] },
                        selected = month,
                        onSelect = { month = it },
                        modifier = Modifier.weight(1f)
                    )
                    val years = ((Calendar.getInstance().get(Calendar.YEAR) - 2)..Calendar.getInstance().get(Calendar.YEAR)).toList()
                    SimpleDropdown(
                        label = year.toString(),
                        options = years.map { it to it.toString() },
                        selected = year,
                        onSelect = { year = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = empId > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        onClick = { onGenerate(empId, month, year) }
                    ) { Text("Generate") }
                }
            }
        }
    }
}

// ==================== SALARY SETTINGS DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalarySettingsDialog(
    employees: List<EmpOption>,
    userId: Int,
    onDismiss: () -> Unit,
    onSaved: (empId: Int) -> Unit
) {
    var empId by remember { mutableIntStateOf(0) }
    var basic by remember { mutableStateOf("") }
    var hra by remember { mutableStateOf("") }
    var da by remember { mutableStateOf("") }
    var ta by remember { mutableStateOf("") }
    var ma by remember { mutableStateOf("") }
    var sa by remember { mutableStateOf("") }
    var pf by remember { mutableStateOf("") }
    var pt by remember { mutableStateOf("") }
    var incTax by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var employerUpiId by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var isLoadingSettings by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Load existing settings when employee changes
    LaunchedEffect(empId) {
        if (empId <= 0) return@LaunchedEffect
        isLoadingSettings = true
        try {
            // We need the full response, so call API directly
            val response = RetrofitClient.apiService.adminPayrollPost(
                mapOf(
                    "user_id" to userId.toString(),
                    "action" to "get_settings",
                    "employee_id" to empId.toString()
                )
            )
            if (response.isSuccessful) {
                val root = response.toLenientJson()?.asJsonObject
                val settings = root?.getAsJsonObject("settings")
                if (settings != null && !settings.isJsonNull) {
                    basic = settings.get("basic_salary")?.asString ?: ""
                    hra = settings.get("house_rent_allowance")?.asString ?: ""
                    da = settings.get("dearness_allowance")?.asString ?: ""
                    ta = settings.get("transport_allowance")?.asString ?: ""
                    ma = settings.get("medical_allowance")?.asString ?: ""
                    sa = settings.get("special_allowance")?.asString ?: ""
                    pf = settings.get("pf_deduction")?.asString ?: ""
                    pt = settings.get("professional_tax")?.asString ?: ""
                    incTax = settings.get("income_tax")?.asString ?: ""
                    other = settings.get("other_deductions")?.asString ?: ""
                    upiId = settings.get("upi_id")?.asString ?: settings.get("upi")?.asString ?: ""
                    employerUpiId = settings.get("employer_upi_id")?.asString ?: settings.get("employer_upi")?.asString ?: ""
                    bank = settings.get("bank_name")?.asString ?: ""
                    account = settings.get("account_number")?.asString ?: ""
                    ifsc = settings.get("ifsc_code")?.asString ?: ""
                } else {
                    // Clear fields for new
                    basic = ""; hra = ""; da = ""; ta = ""; ma = ""; sa = ""
                    pf = ""; pt = ""; incTax = ""; other = ""
                    upiId = ""; employerUpiId = ""; bank = ""; account = ""; ifsc = ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "load settings", e)
        } finally {
            isLoadingSettings = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text("Salary Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                // Employee dropdown
                var empExp by remember { mutableStateOf(false) }
                val empLabel = employees.find { it.id == empId }
                    ?.let { "${it.name} (${it.code})" } ?: "Select employee *"

                ExposedDropdownMenuBox(empExp, { empExp = !empExp }) {
                    OutlinedTextField(
                        value = empLabel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(empExp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(empExp, { empExp = false }) {
                        employees.forEach { e ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${e.name} (${e.code})" + if (e.hasSettings) " ✓" else ""
                                    )
                                },
                                onClick = { empId = e.id; empExp = false }
                            )
                        }
                    }
                }

                if (isLoadingSettings) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = PrimaryIndigo)
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = basic, onValueChange = { basic = it },
                        label = { Text("Basic Salary *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = hra, onValueChange = { hra = it },
                        label = { Text("House Rent Allowance") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = da, onValueChange = { da = it },
                        label = { Text("Dearness Allowance") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = ta, onValueChange = { ta = it },
                        label = { Text("Transport Allowance") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = ma, onValueChange = { ma = it },
                        label = { Text("Medical Allowance") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = sa, onValueChange = { sa = it },
                        label = { Text("Special Allowance") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Spacer(Modifier.height(8.dp))
                    Text("Deductions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(value = pf, onValueChange = { pf = it },
                        label = { Text("PF Deduction") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = pt, onValueChange = { pt = it },
                        label = { Text("Professional Tax") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = incTax, onValueChange = { incTax = it },
                        label = { Text("Income Tax") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = other, onValueChange = { other = it },
                        label = { Text("Other Deductions") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Spacer(Modifier.height(8.dp))
                    Text("Bank & UPI Details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(value = upiId, onValueChange = { upiId = it },
                        label = { Text("Employee UPI ID (Payee VPA)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = employerUpiId, onValueChange = { employerUpiId = it },
                        label = { Text("Employer / Company UPI ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = bank, onValueChange = { bank = it },
                        label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = account, onValueChange = { account = it },
                        label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = ifsc, onValueChange = { ifsc = it },
                        label = { Text("IFSC Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = empId > 0 && basic.toDoubleOrNull() != null && basic.toDouble() > 0 && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val res = postPayroll(
                                    mapOf(
                                        "user_id" to userId.toString(),
                                        "action" to "save_settings",
                                        "employee_id" to empId.toString(),
                                        "basic_salary" to basic,
                                        "house_rent_allowance" to (hra.ifBlank { "0" }),
                                        "dearness_allowance" to (da.ifBlank { "0" }),
                                        "transport_allowance" to (ta.ifBlank { "0" }),
                                        "medical_allowance" to (ma.ifBlank { "0" }),
                                        "special_allowance" to (sa.ifBlank { "0" }),
                                        "pf_deduction" to (pf.ifBlank { "0" }),
                                        "professional_tax" to (pt.ifBlank { "0" }),
                                        "income_tax" to (incTax.ifBlank { "0" }),
                                        "other_deductions" to (other.ifBlank { "0" }),
                                        "upi_id" to upiId,
                                        "upi" to upiId,
                                        "employer_upi_id" to employerUpiId,
                                        "employer_upi" to employerUpiId,
                                        "bank_name" to bank,
                                        "account_number" to account,
                                        "ifsc_code" to ifsc
                                    )
                                )
                                isSaving = false
                                if (res.first) {
                                    onSaved(empId)
                                } else {
                                    // You can show error snackbar here if needed
                                }
                            }
                        }
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save Settings")
                        }
                    }
                }
            }
        }
    }
}

// ==================== MARK PAID DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkPaidDialog(
    record: PayrollItem? = null,
    monthName: String = "",
    initialMethod: String = "Bank Transfer",
    onDismiss: () -> Unit,
    onConfirm: (date: String, method: String, txn: String) -> Unit
) {
    val context = LocalContext.current
    val today = remember {
        val c = Calendar.getInstance()
        String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
    var date by remember { mutableStateOf(today) }
    var method by remember { mutableStateOf(initialMethod) }
    var txn by remember { mutableStateOf("") }

    fun copyText(label: String, text: String) {
        if (text.isBlank() || text == "—") return
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Mark as Paid", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                if (record != null) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(record.employeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Net Amount: ₹${formatInr(record.netSalary)}", fontWeight = FontWeight.Black, color = Color(0xFF059669), fontSize = 15.sp)
                        }
                    }
                }

                OutlinedTextField(value = date, onValueChange = { date = it },
                    label = { Text("Payment date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                val methods = listOf("UPI", "Bank Transfer", "Cash", "Cheque", "Online")
                var mExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(mExp, { mExp = !mExp }) {
                    OutlinedTextField(
                        value = method, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mExp) }
                    )
                    ExposedDropdownMenu(mExp, { mExp = false }) {
                        methods.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { method = it; mExp = false })
                        }
                    }
                }

                if (method == "UPI" && record != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color(0xFFF0F9FF), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFBAE6FD)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp), tint = Color(0xFF0284C7))
                                Spacer(Modifier.width(6.dp))
                                Text("Send UPI Payment Request", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                            }
                            Spacer(Modifier.height(8.dp))
                            val targetUpi = record.upiId.orEmpty().ifBlank { record.employerUpiId.orEmpty() }.ifBlank { "Not set in settings" }
                            Text("Payee VPA: $targetUpi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            Text("Amount: ₹${formatInr(record.netSalary)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))

                            if (!targetUpi.contains("Not set")) {
                                Spacer(Modifier.height(10.dp))
                                Text("Select Payment App to Send Request:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            launchUpiApp(
                                                context = context,
                                                packageName = "com.google.android.apps.nbu.paisa.user",
                                                upiId = targetUpi,
                                                employeeName = record.employeeName,
                                                netSalary = record.netSalary,
                                                monthName = monthName,
                                                year = record.payrollYear
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("GPay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            launchUpiApp(
                                                context = context,
                                                packageName = "com.phonepe.app",
                                                upiId = targetUpi,
                                                employeeName = record.employeeName,
                                                netSalary = record.netSalary,
                                                monthName = monthName,
                                                year = record.payrollYear
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("PhonePe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            launchUpiApp(
                                                context = context,
                                                packageName = "net.one97.paytm",
                                                upiId = targetUpi,
                                                employeeName = record.employeeName,
                                                netSalary = record.netSalary,
                                                monthName = monthName,
                                                year = record.payrollYear
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Paytm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(onClick = { copyToClipboard(context, "UPI ID", targetUpi) }) {
                                        Text("Copy UPI ID", fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { copyToClipboard(context, "Amount", record.netSalary) }) {
                                        Text("Copy Amount", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else if (method == "Bank Transfer" && record != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Bank Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                            Spacer(Modifier.height(4.dp))
                            Text("Bank: ${record.bankName.orEmpty().ifBlank { "N/A" }}", fontSize = 12.sp)
                            Text("Account: ${record.accountNumber.orEmpty().ifBlank { "N/A" }}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("IFSC: ${record.ifscCode.orEmpty().ifBlank { "N/A" }}", fontSize = 12.sp)
                            if (!record.accountNumber.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { copyText("Account Number", record.accountNumber) }) {
                                    Text("Copy Account No.", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = txn, onValueChange = { txn = it },
                    label = { Text("Transaction ID / UTR") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        onClick = { onConfirm(date, method, txn) }
                    ) { Text("Confirm Payment") }
                }
            }
        }
    }
}

// ==================== DETAIL DIALOG ====================
@Composable
private fun PayrollDetailDialog(
    record: PayrollItem,
    monthName: String,
    onDismiss: () -> Unit,
    onMarkPaid: (() -> Unit)? = null,
    onPayUpi: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Payroll Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                DRow("Employee", record.employeeName)
                DRow("Code", record.employeeCode.ifBlank { "—" })
                DRow("Period", "$monthName ${record.payrollYear}")
                DRow("Basic Salary", "₹${formatInr(record.basicSalary)}")
                DRow("HRA", "₹${formatInr(record.houseRentAllowance)}")
                DRow("Dearness (DA)", "₹${formatInr(record.dearnessAllowance)}")
                DRow("Transport (TA)", "₹${formatInr(record.transportAllowance)}")
                DRow("Medical (MA)", "₹${formatInr(record.medicalAllowance)}")
                DRow("Special (SA)", "₹${formatInr(record.specialAllowance)}")
                DRow("Total Allowances", "₹${formatInr(record.totalAllowances)}")
                DRow("Gross Salary", "₹${formatInr(record.grossSalary)}")
                DRow("PF Deduction", "₹${formatInr(record.pfDeduction)}")
                DRow("Professional Tax", "₹${formatInr(record.professionalTax)}")
                DRow("Income Tax", "₹${formatInr(record.incomeTax)}")
                DRow("Other Deductions", "₹${formatInr(record.otherDeductions)}")
                DRow("Total Deductions", "₹${formatInr(record.totalDeductions)}")
                DRow("Net Salary", "₹${formatInr(record.netSalary)}")
                if (!record.bankName.isNullOrBlank()) DRow("Bank", record.bankName)
                if (!record.accountNumber.isNullOrBlank()) DRow("Account No.", record.accountNumber)
                if (!record.ifscCode.isNullOrBlank()) DRow("IFSC", record.ifscCode)
                if (!record.upiId.isNullOrBlank()) DRow("UPI ID", record.upiId)
                DRow("Status", record.status.replaceFirstChar { it.uppercase() })
                if (record.paymentDate != null) DRow("Paid on", record.paymentDate)
                if (record.paymentMethod != null) DRow("Method", record.paymentMethod)
                if (record.transactionId != null) DRow("Txn / UTR", record.transactionId)
                Spacer(Modifier.height(16.dp))

                val isPending = record.status.lowercase() == "pending" || record.status.lowercase() == "processed"
                if (isPending) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!record.upiId.isNullOrBlank() && onPayUpi != null) {
                            Button(
                                onClick = { onDismiss(); onPayUpi() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pay via UPI", fontSize = 12.sp)
                            }
                        }
                        if (onMarkPaid != null) {
                            Button(
                                onClick = { onDismiss(); onMarkPaid() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Mark Paid", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun DRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.width(110.dp), fontSize = 12.sp, color = Color(0xFF6B7280))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatInr(value: String?): String {
    if (value.isNullOrBlank()) return "0"
    return try {
        val d = value.replace(",", "").toDoubleOrNull() ?: 0.0
        val locale = Locale.forLanguageTag("en-IN")
        NumberFormat.getNumberInstance(locale).format(d)
    } catch (_: Exception) {
        value
    }
}