package com.fsscrm.ui.sales

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.ProformaInvoice
import com.fsscrm.network.RetrofitClient
import com.fsscrm.network.Work
import com.fsscrm.network.WorkDetailsRequest
import com.fsscrm.network.WorkDetailsResponse
import com.fsscrm.ui.common.StatItem
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.handleJsonResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailsScreen(userId: Int, workId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<WorkDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddPayment by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    suspend fun refreshDetails() {
        isLoading = true
        try {
            val response = RetrofitClient.apiService.getWorkDetails(
                WorkDetailsRequest(user_id = userId, work_id = workId)
            )

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                if (json.isJsonObject && json.asJsonObject.has("status") &&
                    json.asJsonObject.get("status").asString == "error") {
                    val msg = json.asJsonObject.get("message")?.asString ?: "Unknown API error"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    details = null
                } else {
                    details = WorkDetailsResponse.fromJson(json)
                    // Debug: Log proformas
                    android.util.Log.d("WORK_DETAILS", "Proformas count: ${details?.proformas?.size ?: 0}")
                    details?.proformas?.forEach { pf ->
                        android.util.Log.d("WORK_DETAILS", "Proforma: ${pf.proforma_no} (ID: ${pf.id}) - Status: ${pf.status}")
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("WORK_DETAILS", "Error Body: $errorBody")
                Toast.makeText(context, "Server error: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("WORK_DETAILS", "Exception: ${e.message}", e)
        } finally {
            isLoading = false
            android.util.Log.d("WORK_DETAILS", "Finished. Success: ${details != null}")
        }
    }

    LaunchedEffect(workId) {
        refreshDetails()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Details", "Insights")

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding: PaddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            UniversalHeader(
                title = "Work Details",
                onBackClick = onBack
            )
            
            // Tabs Section
            // Tabs Section
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            if (isLoading && details == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (details == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Work details not found", color = Color.Gray)
                        TextButton(onClick = { scope.launch { refreshDetails() } }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                val work = details?.work
                if (work == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Work information is missing", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
                    ) {
                        if (selectedTab == 0) { // Details
                            item { WorkHeroView(work) }

                            item {
                                SectionTitle("Client & Contact", Icons.Default.Business)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        InfoLine(Icons.Default.Domain, "Company", work.customer_company ?: work.lead_company ?: "N/A")
                                        InfoLine(Icons.Default.Person, "Contact Person", work.customer_name ?: work.lead_name ?: "N/A")
                                        InfoLine(Icons.Default.Phone, "Phone", work.customer_phone ?: work.lead_phone ?: "N/A")
                                        InfoLine(Icons.Default.Email, "Email", work.customer_email ?: work.lead_email ?: "N/A")
                                    }
                                }
                            }

                            // ============================================================
                            // UPDATED: Project Items from Proforma
                            // ============================================================
                            item {
                                SectionTitle("Project Items", Icons.Default.Inventory)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Get items from proforma instead of work_name
                                        val proformaItems = details?.proforma?.items_parsed ?: emptyList()

                                        if (proformaItems.isEmpty()) {
                                            // Fallback to work_name if no proforma items
                                            val fallbackItems = (work.work_name ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                            if (fallbackItems.isEmpty()) {
                                                Text("No items listed", color = Color.Gray, fontSize = 14.sp)
                                            } else {
                                                fallbackItems.forEachIndexed { index, item ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            item,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 15.sp,
                                                            color = Color(0xFF334155)
                                                        )
                                                    }
                                                    if (index < fallbackItems.size - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(vertical = 4.dp),
                                                            thickness = 0.5.dp,
                                                            color = Color(0xFFF1F5F9)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // Display items from proforma
                                            proformaItems.forEachIndexed { index, item ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.primary)
                                                            )
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Text(
                                                                item.requirement,
                                                                fontWeight = FontWeight.Medium,
                                                                fontSize = 15.sp,
                                                                color = Color(0xFF334155)
                                                            )
                                                        }
                                                        Text(
                                                            "₹${item.cost}",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 14.sp,
                                                            color = Color(0xFF334155)
                                                        )
                                                    }
                                                }
                                                if (index < proformaItems.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        thickness = 0.5.dp,
                                                        color = Color(0xFFF1F5F9)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            details?.proformas?.forEach { pf ->
                                item {
                                    SectionTitle("Related Proforma", Icons.Default.Description)
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F0FF)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    pf.proforma_no ?: "N/A",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 16.sp
                                                )
                                                Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                                                    Text(
                                                        (pf.status ?: "pending").uppercase(),
                                                        modifier = Modifier.padding(
                                                            horizontal = 8.dp,
                                                            vertical = 4.dp
                                                        ),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Amount Due:", fontSize = 13.sp, color = Color.Gray)
                                                Text(
                                                    "₹${pf.total ?: "0.00"}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                SectionTitle("Payment Summary", Icons.Default.AccountBalanceWallet)
                                PaymentSummaryView(work)
                            }
                        } else { // Insights
                            item {
                                SectionTitle("Activity Statistics", Icons.Default.Analytics)
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatItem(
                                            label = "Payments",
                                            value = (details?.payments?.size ?: 0).toString(),
                                            color = Color(0xFF8B5CF6)
                                        )
                                        StatItem(
                                            label = "Paid",
                                            value = "₹${work.amount_received ?: "0.00"}",
                                            color = Color(0xFF10B981)
                                        )
                                        StatItem(
                                            label = "Quotes",
                                            value = if (details?.proforma != null) "1" else "0",
                                            color = Color(0xFFF59E0B)
                                        )
                                        StatItem(
                                            label = "Works",
                                            value = "1",
                                            color = Color(0xFF3B82F6)
                                        )
                                    }
                                }
                            }

                            val timeline = details?.history ?: emptyList()
                            if (timeline.isNotEmpty()) {
                                item {
                                    SectionTitle("Activity Timeline", Icons.Default.History)
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            timeline.forEach { item ->
                                                TimelineRow(item)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = { showAddPayment = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Record Project Payment", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddPayment) {
            AddPaymentDialog(
                proformas = details?.proformas ?: emptyList(),
                onDismiss = { showAddPayment = false },
                onConfirm = { formData: Map<String, String> ->
                    // Debug: Log what's being sent
                    android.util.Log.d("PAYMENT", "Form Data: $formData")
                    android.util.Log.d("PAYMENT", "Proforma ID being sent: ${formData["proforma_id"]}")

                    scope.launch {
                        try {
                            // Get the proforma_id from the form data (could be "0" if none selected)
                            val proformaId = formData["proforma_id"]?.toIntOrNull() ?: 0

                            val resp = RetrofitClient.apiService.addWorkPayment(formData + mapOf(
                                "user_id" to userId.toString(),
                                "work_id" to workId.toString(),
                                "lead_id" to (details?.work?.lead_id?.toString() ?: ""),
                                "customer_id" to (details?.work?.customer_id?.toString() ?: ""),
                                "customer_name" to (details?.work?.customer_name ?: details?.work?.lead_name ?: ""),
                                "customer_phone" to (details?.work?.customer_phone ?: details?.work?.lead_phone ?: ""),
                                "customer_email" to (details?.work?.customer_email ?: details?.work?.lead_email ?: ""),
                                "action" to "add_work_payment",
                                "amount" to (formData["payment_amount"] ?: "0"),
                                "proforma_id" to proformaId.toString() // Ensure it's sent
                            ))

                            android.util.Log.d("PAYMENT", "Request Data: $resp")

                            handleJsonResponse(
                                response = resp,
                                onSuccess = {
                                    snackbarHostState.showSnackbar("Payment added successfully")
                                    refreshDetails()
                                },
                                onError = { message: String ->
                                    snackbarHostState.showSnackbar(message)
                                    android.util.Log.e("PAYMENT", "Error: $message")
                                }
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("PAYMENT", "Exception: ${e.message}", e)
                            snackbarHostState.showSnackbar("Failed to add payment: ${e.message}")
                        } finally {
                            showAddPayment = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1E293B))
    }
}

@Composable
fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun TimelineRow(item: com.fsscrm.network.ActivityTimelineItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.5f)))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (item.action ?: "Event").replace("_", " ").uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.date ?: "", fontSize = 10.sp, color = Color.LightGray)
            }

            if (!item.status_from.isNullOrEmpty() || !item.status_to.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(item.status_from ?: "N/A", fontSize = 11.sp, color = Color.Gray)
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(item.status_to ?: "N/A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(item.remarks ?: "", fontSize = 13.sp, color = Color.Gray)
            if (!item.employee_name.isNullOrEmpty()) {
                Text("by ${item.employee_name}", fontSize = 10.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun WorkHeroView(work: Work) {
    val statusText = work.status ?: "pending"
    val statusColor = when (statusText.lowercase()) {
        "completed" -> Color(0xFF10B981)
        "in_progress" -> Color(0xFF3B82F6)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        statusText.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("Created: ${work.created_at?.take(10) ?: "N/A"}", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(work.work_name ?: "Unnamed Project", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(work.customer_name ?: work.lead_name ?: "N/A", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Handover Date", fontSize = 10.sp, color = Color.Gray)
                    Text(work.handover_date ?: "N/A", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (!work.actual_completion.isNullOrEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Actual Completion", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            work.actual_completion,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentSummaryView(work: Work) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Payment Summary", fontWeight = FontWeight.Bold, color = Color(0xFF0369A1), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Amount", fontSize = 11.sp, color = Color.Gray)
                    Text("₹${work.total_amount ?: "0.00"}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Paid", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        "₹${work.amount_received ?: "0.00"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val total = (work.total_amount ?: "0.00").toDoubleOrNull() ?: 0.0
            val paid = (work.amount_received ?: "0.00").toDoubleOrNull() ?: 0.0
            val progress = if (total > 0) (paid / total).toFloat() else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color(0xFF0EA5E9),
                trackColor = Color(0xFFE0F2FE)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pending Balance", fontSize = 13.sp, color = Color.Gray)
                Text(
                    "₹${work.balance_amount ?: "0.00"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }
    }
}

// ============================================================
// UPDATED: AddPaymentDialog with proper proforma_id handling
// ============================================================
@Composable
fun AddPaymentDialog(
    proformas: List<ProformaInvoice> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("cash") }
    var type by remember { mutableStateOf("partial") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var transactionId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Default to first approved proforma, or first available
    var selectedPf by remember {
        mutableStateOf(
            proformas.firstOrNull { it.status == "approved" || it.status == "sent" }
                ?: proformas.firstOrNull()
        )
    }
    var pfMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💰 Record Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Proforma Selection
                if (proformas.isNotEmpty()) {
                    Text("Select Invoice/Proforma", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box {
                        OutlinedTextField(
                            value = selectedPf?.proforma_no ?: "No Invoice Selected",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { pfMenuExpanded = true },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(
                            expanded = pfMenuExpanded,
                            onDismissRequest = { pfMenuExpanded = false }
                        ) {
                            proformas.forEach { pf ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${pf.proforma_no} (₹${pf.total})", fontWeight = FontWeight.Bold)
                                            Text("Status: ${pf.status}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        selectedPf = pf
                                        pfMenuExpanded = false
                                        android.util.Log.d("PAYMENT", "Selected Proforma: ${pf.proforma_no} (ID: ${pf.id})")
                                    }
                                )
                            }
                        }
                    }

                    // Show selected proforma details
                    selectedPf?.let { pf ->
                        Surface(
                            color = Color(0xFFF0F9FF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("✅ Selected: ${pf.proforma_no}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Total Amount: ₹${pf.total}", fontSize = 12.sp, color = Color(0xFF0369A1))
                                Text("Status: ${pf.status}", fontSize = 12.sp, color = Color.Gray)
                                Text("ID: ${pf.id}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⚠️ No proforma invoices found for this work",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            color = Color(0xFF856404)
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("cash", "bank_transfer", "upi", "cheque").forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.uppercase().replace("_", " ")) }
                        )
                    }
                }

                Text("Payment Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("partial", "advance", "full").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.uppercase()) }
                        )
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    label = { Text("Ref / Transaction ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Summary section
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📋 Payment Summary", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Proforma: ${selectedPf?.proforma_no ?: "None"}", fontSize = 11.sp)
                        Text("Proforma ID: ${selectedPf?.id ?: "0"}", fontSize = 11.sp)
                        Text("Amount: ₹${amount.ifEmpty { "0" }}", fontSize = 11.sp)
                        Text("Type: ${type.uppercase()}", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val proformaId = selectedPf?.id ?: 0
                    android.util.Log.d("PAYMENT", "=== CONFIRM PAYMENT ===")
                    android.util.Log.d("PAYMENT", "Selected Proforma ID: $proformaId")
                    android.util.Log.d("PAYMENT", "Selected Proforma: ${selectedPf?.proforma_no}")
                    android.util.Log.d("PAYMENT", "Amount: $amount")
                    android.util.Log.d("PAYMENT", "Type: $type")

                    onConfirm(mapOf(
                        "payment_amount" to amount,
                        "amount" to amount,
                        "payment_method" to method,
                        "payment_type" to type,
                        "payment_date" to date,
                        "transaction_id" to transactionId,
                        "payment_notes" to notes,
                        "notes" to notes,
                        "proforma_id" to proformaId.toString() // Ensure this is sent as string
                    ))
                },
                enabled = amount.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDouble() > 0
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}