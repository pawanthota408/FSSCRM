package com.fsscrm.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.Payment
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.EmptyStateCard
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentsScreen(userId: Int, onMenuClick: () -> Unit) {
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun fetch() {
        isLoading = true
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.getPayments(mapOf("user_id" to userId))
                if (resp.isSuccessful) {
                    val body = resp.toLenientJson()
                    if (body != null) {
                        val array = if (body.isJsonObject && body.asJsonObject.has("payments")) {
                            body.asJsonObject.get("payments").asJsonArray
                        } else if (body.isJsonArray) {
                            body.asJsonArray
                        } else null
                        
                        if (array != null) {
                            payments = RetrofitClient.gson.fromJson(array, object : TypeToken<List<Payment>>() {}.type)
                        }
                    }
                }
            } catch (_: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    Scaffold(
        topBar = { UniversalHeader("Recent Payments", onMenuClick) },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryIndigo) }
        } else if (payments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { EmptyStateCard("No payments found") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(payments) { pay ->
                    PaymentAdminCard(pay)
                }
            }
        }
    }
}

@Composable
fun PaymentAdminCard(pay: Payment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFECFDF5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Payments, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(pay.customer_name ?: "Unknown Customer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(pay.payment_date, fontSize = 12.sp, color = Color.Gray)
                if (!pay.payment_method.isNullOrBlank()) {
                    Text("Via ${pay.payment_method.uppercase()}", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Medium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${pay.amount}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF111827))
                if (!pay.status.isNullOrBlank()) {
                    Text(pay.status.uppercase(), fontSize = 9.sp, color = if(pay.status.lowercase() == "confirmed") Color(0xFF10B981) else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
