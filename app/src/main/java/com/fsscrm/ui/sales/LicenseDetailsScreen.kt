package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.*
import com.fsscrm.ui.common.handleJsonResponse
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseDetailsScreen(userId: Int, licenseKey: String, onBack: () -> Unit) {
    var details by remember { mutableStateOf<LicenseDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    var productToCreateLead by remember { mutableStateOf<Product?>(null) }
    var isCreatingLead by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadDetails() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getLicenseDetails(mapOf("license_key" to licenseKey, "user_id" to userId.toString()))
                val data = response.toLenientJson()?.let { LicenseDetailsResponse.fromJson(it) }
                if (response.isSuccessful && data != null) {
                    details = data
                    android.util.Log.d("LICENSE_UPSELL", "Success! Found ${details?.missing_products?.size} opportunities")
                } else {
                    val err = response.errorBody()?.string() ?: "Unknown error"
                    android.util.Log.e("LICENSE_UPSELL", "API Error: $err")
                    snackbarHostState.showSnackbar("Error loading details")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Exception: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun createLeadForProduct(product: Product) {
        val cust = details?.customer ?: return
        isCreatingLead = true
        scope.launch {
            try {
                val params = mapOf(
                    "user_id" to userId.toString(),
                    "name" to cust.name,
                    "phone" to (cust.phone ?: ""),
                    "email" to (cust.email ?: ""),
                    "company" to (cust.company ?: ""),
                    "customer_id" to cust.id.toString(),
                    "license_key" to licenseKey,
                    "serial_number" to licenseKey,
                    "service" to product.name,
                    "connection_type" to "Services",
                    "lead_type" to "Warm",
                    "source" to "Upsell",
                    "message" to "Upsell Lead: ${product.name} for license $licenseKey"
                )
                val response = RetrofitClient.apiService.createLead(params)
                handleJsonResponse(
                    response = response,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Lead created successfully for ${product.name}")
                            productToCreateLead = null
                            loadDetails() // Refresh to update opportunities if needed
                        }
                    },
                    onError = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to create lead")
            } finally {
                isCreatingLead = false
            }
        }
    }

    LaunchedEffect(licenseKey) {
        loadDetails()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "License Details",
                onBackClick = onBack
            )

            if (isLoading) {
                details?.customer?.let { cust ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryIndigo.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Text(cust.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(cust.company ?: "No Company", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Custom Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = PrimaryIndigo,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    val tabs = listOf("Linked Services", "Opportunities", "Quotes", "Proformas", "Payments")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Linked Items
                            if (details!!.linked_items.isEmpty()) {
                                item { LicenseEmptyState("No linked services found") }
                            } else {
                                items(details!!.linked_items) { item ->
                                    LinkedServiceCard(item, userId)
                                }
                            }
                        }
                        1 -> { // Missing Products (Opportunities)
                            if (details!!.missing_products.isEmpty()) {
                                item { LicenseEmptyState("Everything covered! No opportunities found.") }
                            } else {
                                items(details!!.missing_products) { product ->
                                    OpportunityProductCard(product) {
                                        productToCreateLead = product
                                    }
                                }
                            }
                        }
                        2 -> { // Quotes
                            if (details!!.quotes.isEmpty()) {
                                item { LicenseEmptyState("No quotes found for this license") }
                            } else {
                                items(details!!.quotes) { quote ->
                                    QuoteSummaryCard(quote)
                                }
                            }
                        }
                        3 -> { // Proformas
                            if (details!!.proformas.isEmpty()) {
                                item { LicenseEmptyState("No proformas found for this license") }
                            } else {
                                items(details!!.proformas) { pf ->
                                    ProformaSummaryCard(pf)
                                }
                            }
                        }
                        4 -> { // Payments
                            if (details!!.payments.isEmpty()) {
                                item { LicenseEmptyState("No payments recorded for this license") }
                            } else {
                                items(details!!.payments) { payment ->
                                    PaymentSummaryCard(payment)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (productToCreateLead != null) {
        AlertDialog(
            onDismissRequest = { if (!isCreatingLead) productToCreateLead = null },
            title = { Text("Create Upsell Lead") },
            text = { Text("Are you want to continue to create as a lead for the selected service or product: ${productToCreateLead?.name}?") },
            confirmButton = {
                Button(
                    onClick = { createLeadForProduct(productToCreateLead!!) },
                    enabled = !isCreatingLead
                ) {
                    if (isCreatingLead) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    else Text("YES, CONTINUE")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToCreateLead = null }, enabled = !isCreatingLead) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun LinkedServiceCard(license: CustomerLicense, userId: Int) {
    var expanded by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<ActivityTimelineItem>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(PrimaryIndigo.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Verified, null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(license.item_name ?: "Unknown Service", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Expiry: ${license.expiry_date ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = { 
                    expanded = !expanded
                    if (expanded && history.isEmpty()) {
                        isLoadingHistory = true
                        scope.launch {
                            try {
                                // Fetch timeline only when expanded - using a generic endpoint or mock for now
                                // In real app: val resp = RetrofitClient.apiService.getServiceTimeline(license.id)
                                delay(500) // Simulate network
                                history = listOf(ActivityTimelineItem(action = "Service Created", remarks = "Project completed and license issued", date = license.expiry_date))
                            } finally {
                                isLoadingHistory = false
                            }
                        }
                    }
                }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }

            // Live Countdown Logic
            ExpiryCountdown(license.expiry_date)

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Activity Timeline", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryIndigo)
                
                if (isLoadingHistory) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = PrimaryIndigo)
                } else {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        history.forEach { log ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(modifier = Modifier.size(6.dp).padding(top = 6.dp).clip(CircleShape).background(PrimaryIndigo))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(log.action ?: "Update", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(log.remarks ?: "", fontSize = 11.sp, color = Color.Gray)
                                    Text(log.date ?: "", fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiryCountdown(expiryDate: String?) {
    if (expiryDate.isNullOrBlank()) return
    
    val remainingTime = remember { mutableStateOf("") }
    val isSoon = remember { mutableStateOf(false) }

    LaunchedEffect(expiryDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        while (true) {
            try {
                val expiry = sdf.parse(expiryDate)
                val now = Date()
                if (expiry != null) {
                    val diff = expiry.time - now.time
                    if (diff > 0) {
                        val days = TimeUnit.MILLISECONDS.toDays(diff)
                        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
                        val mins = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                        val secs = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                        
                        isSoon.value = days < 30
                        remainingTime.value = String.format("%02d:%02d:%02d:%02d", days, hours, mins, secs)
                    } else {
                        remainingTime.value = "EXPIRED"
                        isSoon.value = true
                    }
                }
            } catch (e: Exception) { }
            delay(1000)
        }
    }

    if (isSoon.value) {
        Surface(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            color = Color(0xFFFEF2F2),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Will expire in ${remainingTime.value}",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OpportunityProductCard(product: Product, onPitchClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPitchClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF0EA5E9).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AddShoppingCart, null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(product.category, fontSize = 11.sp, color = Color.Gray)
            }
            Button(
                onClick = onPitchClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
            ) {
                Text("Pitch Now", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun QuoteSummaryCard(quote: QuoteDetails) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(quote.quote_no, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                Text("₹${quote.total}", fontWeight = FontWeight.Black)
            }
            Text("Date: ${quote.quote_date}", fontSize = 12.sp, color = Color.Gray)
            Text("Status: ${quote.status?.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if(quote.status == "approved") Color(0xFF10B981) else Color.Gray)
        }
    }
}

@Composable
fun ProformaSummaryCard(pf: ProformaInvoice) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F0FF))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(pf.proforma_no, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                Text("₹${pf.total}", fontWeight = FontWeight.Black)
            }
            Text("Items: ${pf.items ?: "N/A"}", fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
fun PaymentSummaryCard(payment: Payment) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("₹${payment.amount}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF10B981))
                Text(payment.payment_date, fontSize = 12.sp, color = Color.Gray)
            }
            Text(payment.payment_method?.uppercase() ?: "CASH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LicenseEmptyState(msg: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(msg, color = Color.Gray, textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}
