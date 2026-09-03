package com.fsscrm.ui.sales

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.fsscrm.network.PayrollRecord
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.JsonElement
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

private const val TAG = "PayrollScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollScreen(userId: Int, onMenuClick: () -> Unit) {
    var payrolls by remember { mutableStateOf<List<PayrollRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRecord by remember { mutableStateOf<PayrollRecord?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun parsePayrollJson(json: JsonElement): List<PayrollRecord> {
        val list = mutableListOf<PayrollRecord>()
        try {
            val array = when {
                json.isJsonArray -> json.asJsonArray
                json.isJsonObject -> {
                    val obj = json.asJsonObject
                    when {
                        obj.has("records") && obj.get("records").isJsonArray -> obj.getAsJsonArray("records")
                        obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                        obj.has("payrolls") && obj.get("payrolls").isJsonArray -> obj.getAsJsonArray("payrolls")
                        obj.has("payroll") && obj.get("payroll").isJsonArray -> obj.getAsJsonArray("payroll")
                        else -> null
                    }
                }
                else -> null
            }
            if (array != null) {
                for (elem in array) {
                    if (elem.isJsonObject) {
                        val rec = RetrofitClient.gson.fromJson(elem, PayrollRecord::class.java)
                        list.add(rec)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse error", e)
        }
        return list
    }

    suspend fun loadPayroll() {
        if (userId <= 0) {
            isLoading = false
            return
        }
        isLoading = true
        errorMessage = null
        try {
            // 1. Try POST get_employee_payroll.php
            val respEmp1 = RetrofitClient.apiService.getEmployeePayroll(mapOf("user_id" to userId, "employee_id" to userId))
            if (respEmp1.isSuccessful && respEmp1.body() != null) {
                val listEmp1 = parsePayrollJson(respEmp1.body()!!)
                if (listEmp1.isNotEmpty()) {
                    payrolls = listEmp1
                    return
                }
            }

            // 2. Try GET get_employee_payroll.php
            val respEmp2 = RetrofitClient.apiService.getEmployeePayrollGet(
                mapOf("user_id" to userId.toString(), "employee_id" to userId.toString())
            )
            if (respEmp2.isSuccessful && respEmp2.body() != null) {
                val listEmp2 = parsePayrollJson(respEmp2.body()!!)
                if (listEmp2.isNotEmpty()) {
                    payrolls = listEmp2
                    return
                }
            }

            // 3. Try POST get_payroll.php
            val resp1 = RetrofitClient.apiService.getPayroll(mapOf("user_id" to userId))
            if (resp1.isSuccessful && resp1.body() != null) {
                val list1 = parsePayrollJson(resp1.body()!!)
                if (list1.isNotEmpty()) {
                    payrolls = list1
                    return
                }
            }

            // 4. Try GET get_payroll.php
            val resp2 = RetrofitClient.apiService.getPayrollGet(
                mapOf("user_id" to userId.toString(), "employee_id" to userId.toString())
            )
            if (resp2.isSuccessful && resp2.body() != null) {
                val list2 = parsePayrollJson(resp2.body()!!)
                if (list2.isNotEmpty()) {
                    payrolls = list2
                    return
                }
            }

            // 5. Fallback to admin payroll API for this user
            val resp3 = RetrofitClient.apiService.getAdminPayroll(
                mapOf("user_id" to userId.toString(), "search" to "")
            )
            if (resp3.isSuccessful && resp3.body() != null) {
                val json = resp3.toLenientJson()?.asJsonObject
                val array = json?.getAsJsonArray("records")
                if (array != null) {
                    val list3 = mutableListOf<PayrollRecord>()
                    for (elem in array) {
                        if (elem.isJsonObject) {
                            val rec = RetrofitClient.gson.fromJson(elem, PayrollRecord::class.java)
                            list3.add(rec)
                        }
                    }
                    if (list3.isNotEmpty()) {
                        payrolls = list3
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadPayroll error", e)
            errorMessage = e.message ?: "Failed to load payroll"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(userId) {
        loadPayroll()
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "My Payslips & Payroll",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { scope.launch { loadPayroll() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                }
                errorMessage != null && payrolls.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage ?: "Error loading payroll", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { scope.launch { loadPayroll() } }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (payrolls.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Payments,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.LightGray
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No payroll records found", color = Color.Gray, fontWeight = FontWeight.Medium)
                                        Text("Payslips will appear here once generated by admin.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            items(payrolls) { record ->
                                PayrollCard(
                                    record = record,
                                    onClick = { selectedRecord = record },
                                    onDownloadPdf = { exportPayslipPdf(record, context) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedRecord?.let { record ->
        PayslipDetailDialog(
            record = record,
            onDismiss = { selectedRecord = null },
            onDownloadPdf = { exportPayslipPdf(record, context) }
        )
    }
}

@Composable
fun PayrollCard(
    record: PayrollRecord,
    onClick: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    val st = record.status.lowercase()
    val (statusBg, statusFg) = when (st) {
        "paid" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "processed" -> Color(0xFFDBEAFE) to Color(0xFF1E3A5F)
        "cancelled" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${record.monthName} ${record.yearStr}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                    Text("Monthly Salary", fontSize = 12.sp, color = Color.Gray)
                }
                Text(
                    "₹${formatInr(record.net_salary)}",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF059669),
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Payment Status", fontSize = 10.sp, color = Color.Gray)
                    Surface(color = statusBg, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = record.status.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!record.payment_date.isNullOrBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Paid On", fontSize = 10.sp, color = Color.Gray)
                            Text(record.payment_date, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                        }
                    }
                    
                    IconButton(
                        onClick = onDownloadPdf,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Download Payslip PDF",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayslipDetailDialog(
    record: PayrollRecord,
    onDismiss: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Payslip Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                if (!record.employee_name.isNullOrBlank()) DRow("Employee", record.employee_name)
                DRow("Period", "${record.monthName} ${record.yearStr}")
                DRow("Basic Salary", "₹${formatInr(record.basic_salary)}")
                if (record.house_rent_allowance != "0") DRow("HRA", "₹${formatInr(record.house_rent_allowance)}")
                if (record.dearness_allowance != "0") DRow("Dearness (DA)", "₹${formatInr(record.dearness_allowance)}")
                if (record.transport_allowance != "0") DRow("Transport (TA)", "₹${formatInr(record.transport_allowance)}")
                if (record.medical_allowance != "0") DRow("Medical (MA)", "₹${formatInr(record.medical_allowance)}")
                if (record.special_allowance != "0") DRow("Special (SA)", "₹${formatInr(record.special_allowance)}")
                DRow("Total Allowances", "₹${formatInr(record.total_allowances)}")
                DRow("Gross Salary", "₹${formatInr(record.gross_salary)}")
                if (record.pf_deduction != "0") DRow("PF Deduction", "₹${formatInr(record.pf_deduction)}")
                if (record.professional_tax != "0") DRow("Professional Tax", "₹${formatInr(record.professional_tax)}")
                if (record.income_tax != "0") DRow("Income Tax", "₹${formatInr(record.income_tax)}")
                if (record.other_deductions != "0") DRow("Other Deductions", "₹${formatInr(record.other_deductions)}")
                DRow("Total Deductions", "₹${formatInr(record.total_deductions)}")
                DRow("Net Salary", "₹${formatInr(record.net_salary)}")
                DRow("Status", record.status.replaceFirstChar { it.uppercase() })
                if (!record.payment_date.isNullOrBlank()) DRow("Paid On", record.payment_date)
                if (!record.payment_method.isNullOrBlank()) DRow("Method", record.payment_method)
                if (!record.transaction_id.isNullOrBlank()) DRow("Txn / UTR", record.transaction_id)
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontSize = 12.sp, color = Color(0xFF7C3AED))
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color(0xFF6B7280))
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

private fun exportPayslipPdf(record: PayrollRecord, context: Context) {
    try {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait at 72dpi
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 40f

        // Company Header
        titlePaint.color = android.graphics.Color.parseColor("#1E3A8A")
        titlePaint.textSize = 18f
        canvas.drawText("FRIENDS SOFTWARE SOLUTIONS", 40f, y, titlePaint)
        y += 18f

        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.textSize = 10f
        canvas.drawText("Tally Sales & Services | Web Development & CRM", 40f, y, paint)
        y += 24f

        // Title
        titlePaint.color = android.graphics.Color.parseColor("#0F172A")
        titlePaint.textSize = 15f
        canvas.drawText("SALARY PAYSLIP - ${record.monthName.uppercase(Locale.US)} ${record.yearStr}", 40f, y, titlePaint)
        y += 15f

        // Divider Line
        paint.color = android.graphics.Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Employee Info Box
        paint.color = android.graphics.Color.parseColor("#F8FAFC")
        canvas.drawRect(40f, y, 555f, y + 65f, paint)

        paint.color = android.graphics.Color.parseColor("#334155")
        paint.textSize = 10.5f
        titlePaint.textSize = 11f
        titlePaint.color = android.graphics.Color.parseColor("#0F172A")

        canvas.drawText("Employee: ${record.employee_name ?: "Employee"}", 50f, y + 20f, titlePaint)
        canvas.drawText("Code: ${record.employee_id ?: "N/A"}", 350f, y + 20f, paint)

        canvas.drawText("Status: ${record.status.uppercase(Locale.US)}", 50f, y + 40f, paint)
        canvas.drawText("Pay Date: ${record.payment_date ?: "--"}", 350f, y + 40f, paint)

        if (!record.payment_method.isNullOrBlank()) {
            canvas.drawText("Method: ${record.payment_method}", 50f, y + 58f, paint)
        }
        if (!record.transaction_id.isNullOrBlank()) {
            canvas.drawText("Txn/UTR: ${record.transaction_id}", 350f, y + 58f, paint)
        }
        y += 85f

        // Salary Breakdown Table Header
        paint.color = android.graphics.Color.parseColor("#1E293B")
        canvas.drawRect(40f, y, 290f, y + 22f, paint)
        canvas.drawRect(305f, y, 555f, y + 22f, paint)

        titlePaint.color = android.graphics.Color.WHITE
        titlePaint.textSize = 10.5f
        canvas.drawText("EARNINGS & ALLOWANCES", 50f, y + 15f, titlePaint)
        canvas.drawText("DEDUCTIONS", 315f, y + 15f, titlePaint)
        y += 30f

        // Items
        paint.color = android.graphics.Color.parseColor("#334155")
        paint.textSize = 10.5f

        fun drawLineItem(leftLabel: String, leftVal: String, rightLabel: String, rightVal: String, currentY: Float) {
            canvas.drawText(leftLabel, 50f, currentY, paint)
            canvas.drawText("₹$leftVal", 220f, currentY, paint)

            if (rightLabel.isNotEmpty()) {
                canvas.drawText(rightLabel, 315f, currentY, paint)
                canvas.drawText("₹$rightVal", 485f, currentY, paint)
            }
        }

        drawLineItem("Basic Salary", formatInr(record.basic_salary), "PF Deduction", formatInr(record.pf_deduction), y); y += 18f
        drawLineItem("HRA", formatInr(record.house_rent_allowance), "Professional Tax", formatInr(record.professional_tax), y); y += 18f
        drawLineItem("Dearness (DA)", formatInr(record.dearness_allowance), "Income Tax", formatInr(record.income_tax), y); y += 18f
        drawLineItem("Transport (TA)", formatInr(record.transport_allowance), "Other Deductions", formatInr(record.other_deductions), y); y += 18f
        drawLineItem("Medical (MA)", formatInr(record.medical_allowance), "", "", y); y += 18f
        drawLineItem("Special (SA)", formatInr(record.special_allowance), "", "", y); y += 22f

        // Line
        paint.color = android.graphics.Color.parseColor("#E2E8F0")
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 18f

        titlePaint.color = android.graphics.Color.parseColor("#0F172A")
        titlePaint.textSize = 11f
        canvas.drawText("Gross Salary: ₹${formatInr(record.gross_salary)}", 50f, y, titlePaint)
        canvas.drawText("Total Deductions: ₹${formatInr(record.total_deductions)}", 315f, y, titlePaint)
        y += 30f

        // Net Salary Box
        paint.color = android.graphics.Color.parseColor("#DCFCE7")
        canvas.drawRect(40f, y, 555f, y + 40f, paint)

        titlePaint.color = android.graphics.Color.parseColor("#15803D")
        titlePaint.textSize = 14f
        canvas.drawText("NET SALARY PAID: ₹${formatInr(record.net_salary)}", 50f, y + 25f, titlePaint)
        y += 60f

        // Footer note
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.textSize = 9f
        canvas.drawText("This is a computer-generated payslip and does not require a physical signature.", 40f, y, paint)

        pdf.finishPage(page)

        val dir = File(context.cacheDir, "payslips").apply { mkdirs() }
        val file = File(dir, "payslip_${record.monthName}_${record.yearStr}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open Payslip PDF")) }
            .onFailure {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallback, "Share Payslip PDF"))
            }
    } catch (e: Exception) {
        Log.e(TAG, "PDF generation failed", e)
    }
}
