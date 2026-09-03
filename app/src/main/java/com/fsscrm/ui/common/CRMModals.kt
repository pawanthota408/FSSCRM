package com.fsscrm.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.fsscrm.R
import com.fsscrm.network.Employee
import com.fsscrm.network.FollowUp
import com.fsscrm.network.Lead
import com.fsscrm.network.Product
import com.fsscrm.network.ProformaInvoice
import com.fsscrm.network.QuoteDetails
import com.fsscrm.network.QuoteRequirement
import com.fsscrm.network.RetrofitClient
import com.fsscrm.network.Work
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ============================================================
// HELPER FUNCTIONS
// ============================================================

private fun parsePrice(value: String?): Double {
    if (value == null) return 0.0
    return value.replace(",", "").replace("₹", "").trim().toDoubleOrNull() ?: 0.0
}

@Composable
fun QuoteTextField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    keyboardType: KeyboardType = KeyboardType.Text,
    textAlign: TextAlign = TextAlign.Start,
    isReadOnly: Boolean = false,
    indicatorColor: Color = Color.Transparent,
    fontWeight: FontWeight = FontWeight.Normal,
    prefix: String = "",
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = Color.LightGray, fontSize = fontSize) },
        readOnly = isReadOnly,
        singleLine = true,
        prefix = if (prefix.isNotEmpty() && value.isNotEmpty()) {
            @Composable { Text(prefix, fontSize = fontSize) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = LocalTextStyle.current.copy(
            textAlign = textAlign,
            fontWeight = fontWeight,
            fontSize = fontSize
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = indicatorColor,
            unfocusedIndicatorColor = indicatorColor,
            disabledIndicatorColor = indicatorColor,
            cursorColor = Color(0xFF6366F1)
        )
    )
}

@Composable
fun TotalRow(label: String, value: String, isBold: Boolean = false, highlight: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlight) Color(0xFFF3F4F6) else Color.Transparent)
            .padding(vertical = if (highlight) 6.dp else 3.dp, horizontal = if (highlight) 8.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = if (highlight) 14.sp else 13.sp,
            color = if (isBold || highlight) Color.Black else Color.Gray,
            fontWeight = if (isBold || highlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            fontSize = if (highlight) 14.sp else 13.sp,
            color = Color.Black,
            fontWeight = if (isBold || highlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ============================================================
// MAIN EDIT FORM (matches second screenshot exactly)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDocumentComponent(
    title: String,
    lead: Lead,
    products: List<Product> = emptyList(),
    licenses: List<com.fsscrm.network.CustomerLicense> = emptyList(),
    currentUser: Employee? = null,
    isReadOnly: Boolean = false,
    initialQuote: QuoteDetails? = null,
    initialItems: List<QuoteRequirement> = emptyList(),
    initialDiscount: String = "0.00",
    initialStatus: String = "Draft",
    initialComments: String = "",
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onConfirm: (List<QuoteRequirement>, String, String, String, String, String, String, String, String) -> Unit
) {
    val isQuotation = title.lowercase().contains("quote") && !title.lowercase().contains("proforma")
    
    var clientName by remember { mutableStateOf(initialQuote?.customer_name?.ifBlank { null } ?: lead.name ?: "") }
    var clientEmail by remember { mutableStateOf(initialQuote?.customer_email?.ifBlank { null } ?: lead.email ?: "") }
    var clientPhone by remember { mutableStateOf(initialQuote?.customer_phone?.ifBlank { null } ?: lead.phone ?: "") }
    var clientCompany by remember { mutableStateOf(initialQuote?.customer_company?.ifBlank { null } ?: lead.company ?: "") }
    var serviceProject by remember { mutableStateOf(lead.service ?: "N/A") }

    val items = remember { mutableStateListOf<QuoteRequirement>() }

    // Financial calculations
    var discountInput by remember { mutableStateOf(initialQuote?.discount ?: initialDiscount) }
    var otherTaxInput by remember { mutableStateOf("0.00") }
    var validDaysInput by remember { mutableStateOf("15") }
    var remarksInput by remember { mutableStateOf(initialQuote?.admin_notes ?: initialComments) }

    // Seed or initialQuote/initialItems sync
    LaunchedEffect(initialQuote, initialItems) {
        val reqs = initialQuote?.requirements?.takeIf { it.isNotEmpty() } ?: initialItems
        if (reqs.isNotEmpty()) {
            items.clear()
            items.addAll(reqs)
        } else if (items.isEmpty() && !isReadOnly) {
            val svc = lead.service?.trim() ?: ""
            if (svc.isNotEmpty()) {
                val isMain = (svc.contains("gold", ignoreCase = true) ||
                        svc.contains("silver", ignoreCase = true) ||
                        svc.contains("server", ignoreCase = true) ||
                        svc.contains("prime", ignoreCase = true)) &&
                        !svc.contains("cloud", ignoreCase = true) &&
                        !svc.contains("amc", ignoreCase = true) &&
                        !svc.contains("tss", ignoreCase = true) &&
                        !svc.contains("tdl", ignoreCase = true) &&
                        !svc.contains("whatsapp", ignoreCase = true) &&
                        !svc.contains("biz", ignoreCase = true)

                val sn = if (isMain) "" else (lead.effectiveLicenseNumber ?: "")
                val label = "$svc${if (sn.isNotBlank()) " [SN: $sn]" else ""}${if (isMain) " (licence after work)" else ""}"

                items.add(
                    QuoteRequirement(
                        requirement = label,
                        cost = "0.00",
                        quantity = "1",
                        serial_number = sn.ifBlank { null },
                        serial_at = if (isMain) "completion" else "quote"
                    )
                )
            }
        }

        if (initialQuote != null) {
            if (!initialQuote.customer_name.isNullOrBlank()) clientName = initialQuote.customer_name
            if (!initialQuote.customer_email.isNullOrBlank()) clientEmail = initialQuote.customer_email
            if (!initialQuote.customer_phone.isNullOrBlank()) clientPhone = initialQuote.customer_phone
            if (!initialQuote.customer_company.isNullOrBlank()) clientCompany = initialQuote.customer_company
            if (!initialQuote.discount.isNullOrBlank()) discountInput = initialQuote.discount
            if (!initialQuote.admin_notes.isNullOrBlank()) remarksInput = initialQuote.admin_notes
        }
    }

    // Catalog & Custom item selection state
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDropdown by remember { mutableStateOf(false) }
    var productCostInput by remember { mutableStateOf("") }
    var productSerialInput by remember { mutableStateOf("") }
    var productExpiryInput by remember { mutableStateOf("") }

    var customDescInput by remember { mutableStateOf("") }
    var customCostInput by remember { mutableStateOf("") }

    val subtotal = items.sumOf { parsePrice(it.cost) * it.quantityInt }
    val tax = subtotal * 0.18
    val discVal = parsePrice(discountInput)
    val otherTaxVal = parsePrice(otherTaxInput)
    val grandTotal = (subtotal + tax + otherTaxVal - discVal).coerceAtLeast(0.0)

    val cardBgColor = if (isQuotation) Color(0xFFFFFBEB) else Color(0xFFEFF6FF)
    val cardBorderColor = if (isQuotation) Color(0xFFFCD34D) else Color(0xFF93C5FD)
    val headerTextColor = if (isQuotation) Color(0xFF92400E) else Color(0xFF1D4ED8)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Scaffold(
                topBar = {
                    UniversalHeader(
                        title = if (isQuotation) "Quotation" else "Proforma Invoice",
                        onBackClick = onDismiss,
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isReadOnly) {
                        Surface(
                            shadowElevation = 8.dp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    onConfirm(
                                        items.toList(),
                                        discountInput,
                                        "₹",
                                        "Draft",
                                        remarksInput,
                                        title,
                                        clientName,
                                        clientPhone,
                                        clientCompany
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isQuotation) Color(0xFFD97706) else Color(0xFF2563EB)
                                )
                            ) {
                                Text(
                                    if (isQuotation) "Create Quotation (sent to Admin for approval)"
                                    else "Create Proforma Invoice",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                },
                containerColor = Color.White
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stage Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(1.5.dp, cardBorderColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isQuotation) "📄 Create Quotation (sent to Admin for approval)"
                                    else "📋 Proforma Invoice (Proposal)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = headerTextColor
                                )
                            }
                            Text(
                                if (isQuotation) "Add line items based on the service required by the customer"
                                else "Prefills from quotation — sent to Admin for approval",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Customer Information Form
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadOnly,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = { clientEmail = it },
                        label = { Text("Customer Email") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadOnly,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Customer Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadOnly,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = clientCompany,
                        onValueChange = { clientCompany = it },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadOnly,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = serviceProject,
                        onValueChange = { },
                        label = { Text("Service / Project") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9)
                        )
                    )

                    // Products & Services Section Header
                    Text(
                        "Products & Services *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )

                    // Auto-fill Info Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isQuotation) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isQuotation) Color(0xFFFCD34D) else Color(0xFF93C5FD))
                    ) {
                        Text(
                            if (isQuotation) "Auto-filled from service: $serviceProject — enter the cost on the line above, then submit for admin approval."
                            else if (initialQuote != null && !initialQuote.quote_no.isNullOrBlank()) "Auto-filled from quotation #${initialQuote.quote_no} — review or edit items, then submit for admin approval."
                            else "Auto-filled from service: $serviceProject — enter the cost on the line above, then submit for admin approval.",
                            fontSize = 12.sp,
                            color = if (isQuotation) Color(0xFF92400E) else Color(0xFF1E40AF),
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    // Existing Line Items
                    items.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.8f)) {
                                    Text(
                                        "${index + 1}. ${item.requirement}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                OutlinedTextField(
                                    value = item.cost,
                                    onValueChange = { newCost ->
                                        items[index] = item.copy(cost = newCost)
                                    },
                                    label = { Text("Cost ₹ *") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    readOnly = isReadOnly,
                                    shape = RoundedCornerShape(6.dp)
                                )

                                if (!isReadOnly) {
                                    IconButton(
                                        onClick = { items.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Product Box
                    if (!isReadOnly) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "📦 Select product/service — cost entered by you",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedProduct?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Product / Service") },
                                        placeholder = { Text("— Choose —") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showProductDropdown = true },
                                        trailingIcon = {
                                            IconButton(onClick = { showProductDropdown = true }) {
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    DropdownMenu(
                                        expanded = showProductDropdown,
                                        onDismissRequest = { showProductDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        products.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text("${p.name} (${p.category})", fontSize = 13.sp) },
                                                onClick = {
                                                    selectedProduct = p
                                                    showProductDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = productCostInput,
                                        onValueChange = { productCostInput = it },
                                        label = { Text("Cost ₹ *") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    val isMainSelected = selectedProduct?.let { p ->
                                        p.serial_at == "completion" ||
                                                (p.name.contains("gold", ignoreCase = true) ||
                                                        p.name.contains("silver", ignoreCase = true) ||
                                                        p.name.contains("server", ignoreCase = true) ||
                                                        p.name.contains("prime", ignoreCase = true)) &&
                                                        !p.name.contains("cloud", ignoreCase = true) &&
                                                        !p.name.contains("amc", ignoreCase = true)
                                    } ?: false

                                    if (!isMainSelected) {
                                        OutlinedTextField(
                                            value = productSerialInput,
                                            onValueChange = { productSerialInput = it },
                                            label = { Text("Licence / Serial #") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }

                                val isMainSel = selectedProduct?.let { p ->
                                    p.serial_at == "completion" ||
                                            (p.name.contains("gold", ignoreCase = true) ||
                                                    p.name.contains("silver", ignoreCase = true) ||
                                                    p.name.contains("server", ignoreCase = true) ||
                                                    p.name.contains("prime", ignoreCase = true)) &&
                                                    !p.name.contains("cloud", ignoreCase = true) &&
                                                    !p.name.contains("amc", ignoreCase = true)
                                } ?: false

                                if (!isMainSel && selectedProduct != null) {
                                    OutlinedTextField(
                                        value = productExpiryInput,
                                        onValueChange = { productExpiryInput = it },
                                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val p = selectedProduct ?: return@Button
                                        val costVal = productCostInput.trim()
                                        if (costVal.isBlank() || (costVal.toDoubleOrNull() ?: 0.0) <= 0.0) return@Button

                                        val serialVal = productSerialInput.trim()
                                        val expiryVal = productExpiryInput.trim()

                                        val label = "${p.name}${if (serialVal.isNotBlank()) " [SN: $serialVal]" else ""}${if (isMainSel) " (licence after work)" else ""}${if (expiryVal.isNotBlank()) " [Exp: $expiryVal]" else ""}"

                                        items.add(
                                            QuoteRequirement(
                                                requirement = label,
                                                cost = costVal,
                                                quantity = "1",
                                                product_id = p.id,
                                                serial_number = serialVal.ifBlank { null },
                                                licence_type = p.licence_type ?: p.name,
                                                serial_at = if (isMainSel) "completion" else "quote",
                                                expiry_date = expiryVal.ifBlank { null }
                                            )
                                        )

                                        selectedProduct = null
                                        productCostInput = ""
                                        productSerialInput = ""
                                        productExpiryInput = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("+ Add Product/Service", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                HorizontalDivider(color = Color(0xFFFDE68A))

                                // Custom line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customDescInput,
                                        onValueChange = { customDescInput = it },
                                        label = { Text("Custom requirement") },
                                        modifier = Modifier.weight(1.8f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    OutlinedTextField(
                                        value = customCostInput,
                                        onValueChange = { customCostInput = it },
                                        label = { Text("Cost ₹") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    Button(
                                        onClick = {
                                            val desc = customDescInput.trim()
                                            val costVal = customCostInput.trim()
                                            if (desc.isNotBlank() && (costVal.toDoubleOrNull() ?: 0.0) > 0.0) {
                                                items.add(
                                                    QuoteRequirement(
                                                        requirement = desc,
                                                        cost = costVal,
                                                        quantity = "1"
                                                    )
                                                )
                                                customDescInput = ""
                                                customCostInput = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B))
                                    ) {
                                        Text("+ Custom", fontSize = 12.sp)
                                    }
                                }

                                Text(
                                    "Tally Gold/Silver/Server -> licence after work complete. Others -> serial required now.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }

                    // Financial Totals Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = "%.2f".format(subtotal),
                                onValueChange = {},
                                label = { Text("Subtotal (Amount) *") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                shape = RoundedCornerShape(6.dp)
                            )

                            OutlinedTextField(
                                value = "%.2f".format(tax),
                                onValueChange = {},
                                label = { Text("Tax / GST (18%)") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                shape = RoundedCornerShape(6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = { discountInput = it },
                                    label = { Text("Discount ₹") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    readOnly = isReadOnly,
                                    shape = RoundedCornerShape(6.dp)
                                )

                                OutlinedTextField(
                                    value = otherTaxInput,
                                    onValueChange = { otherTaxInput = it },
                                    label = { Text("Other Tax ₹ (optional)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    readOnly = isReadOnly,
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }

                            OutlinedTextField(
                                value = "%.2f".format(grandTotal),
                                onValueChange = {},
                                label = { Text("Grand Total *") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(6.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF1F5F9),
                                    unfocusedContainerColor = Color(0xFFF1F5F9)
                                )
                            )

                            OutlinedTextField(
                                value = validDaysInput,
                                onValueChange = { validDaysInput = it },
                                label = { Text("Valid For (days)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                readOnly = isReadOnly,
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }

                    // Alert Note
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = cardBgColor,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Text(
                            if (isQuotation) " This quotation will be sent to Admin for approval. You will be notified once approved/rejected."
                            else " Proforma invoice will be sent to Admin for approval. You will be notified once approved/rejected.",
                            fontSize = 12.sp,
                            color = headerTextColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Remarks
                    OutlinedTextField(
                        value = remarksInput,
                        onValueChange = { remarksInput = it },
                        label = { Text("Remarks") },
                        placeholder = { Text("Add remarks...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        readOnly = isReadOnly,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ============================================================
// FORMAL PREVIEW SCREEN (matches first screenshot exactly)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePreviewScreen(
    title: String,
    settings: QuoteSettings,
    clientName: String,
    clientPhone: String,
    clientCompany: String = "",
    items: List<QuoteRequirement>,
    discount: Double,
    tax: Double,
    grandTotal: Double,
    date: String,
    validUntil: String,
    isViewOnly: Boolean = false,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onSaveAndFinish: () -> Unit,
    onSaveAndSend: () -> Unit
) {
    val subtotal = items.sumOf { (it.cost.toDoubleOrNull() ?: 0.0) * it.quantityInt }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8F9FB)
        ) {
            Scaffold(
                topBar = {
                    UniversalHeader(
                        title = title,
                        onBackClick = onDismiss,
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isViewOnly) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onSaveAndFinish) {
                                    Text(
                                        "Save & Finish",
                                        color = Color(0xFF7C4DFF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                                Button(
                                    onClick = onSaveAndSend,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF7C4DFF)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        "Save & Send",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = Color(0xFFF8F9FB)
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // White document card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(0.dp),
                        shadowElevation = 1.dp
                    ) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                            // Company header + logo
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        settings.companyName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (settings.companyWebsite.isNotEmpty()) Text(settings.companyWebsite, fontSize = 11.sp, color = Color.Gray)
                                    if (settings.companyAddress.isNotEmpty()) Text(settings.companyAddress, fontSize = 11.sp, color = Color.Gray)
                                    if (settings.companyEmail.isNotEmpty()) Text(settings.companyEmail, fontSize = 11.sp, color = Color.Gray)
                                    if (settings.companyPhone.isNotEmpty()) Text("Tel: ${settings.companyPhone}", fontSize = 11.sp, color = Color.Gray)
                                }

                                // Logo
                                Box(
                                    Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (settings.logoUri.isNotEmpty()) {
                                        AsyncImage(
                                            model = settings.logoUri,
                                            contentDescription = "Logo",
                                            modifier = Modifier.fillMaxSize(),
                                            error = androidx.compose.ui.res.painterResource(id = R.drawable.hand),
                                            fallback = androidx.compose.ui.res.painterResource(id = R.drawable.hand)
                                        )
                                    } else {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.hand),
                                            contentDescription = null,
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(28.dp))

                            // Gray title bar
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F4F6))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            // Client
                            Text(
                                "To: $clientName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            
                            if (clientCompany.isNotBlank()) {
                                Text(
                                    clientCompany,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }

                            if (clientPhone.isNotEmpty()) {
                                Text(
                                    "Phone: $clientPhone",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            // Table header
                            Row(Modifier.fillMaxWidth()) {
                                Text("#", modifier = Modifier.width(28.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("Description", modifier = Modifier.weight(1.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("Qty", modifier = Modifier.width(40.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                                Text("Unit Price", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.End)
                                Text("Amount", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.End)
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFF3F4F6))

                            // Line items
                            items.forEachIndexed { index, item ->
                                val amount = parsePrice(item.cost) * item.quantityInt
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", modifier = Modifier.width(24.dp), fontSize = 11.sp)
                                    Column(modifier = Modifier.weight(1.6f)) {
                                        val lic = item.serial_number?.takeIf { it.isNotBlank() }
                                        val req = item.requirement
                                        val text = if (!lic.isNullOrBlank() && !req.contains(lic, ignoreCase = true)) "$lic : $req" else req
                                        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (!item.serial_number.isNullOrBlank() && !req.contains(item.serial_number!!, ignoreCase = true)) {
                                            Text("License: ${item.serial_number}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                    Text(item.quantity, modifier = Modifier.width(36.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
                                    Text(String.format(Locale.US, "%,.2f", parsePrice(item.cost)), modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                    Text(String.format(Locale.US, "%,.2f", amount), modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color(0xFFF9FAFB))
                            }

                            Spacer(Modifier.height(12.dp))

                            // Totals helper
                            @Composable
                            fun TotalLine(label: String, value: Double, highlight: Boolean = false) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(label, modifier = Modifier.width(110.dp), fontSize = 11.sp, textAlign = TextAlign.End, color = if (highlight) Color.Black else Color.Gray)
                                    Spacer(Modifier.width(16.dp))
                                    Box(
                                        modifier = if (highlight)
                                            Modifier
                                                .background(Color(0xFFF3F4F6))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        else Modifier
                                    ) {
                                        Text(
                                            String.format(Locale.US, "%,.2f", value),
                                            fontSize = 12.sp,
                                            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }

                            TotalLine("Subtotal", subtotal)
                            TotalLine("Discount (${settings.currency})", discount)
                            if (settings.includeTax) {
                                TotalLine("Tax (${settings.taxRate}%)", tax)
                            }
                            TotalLine("Total", grandTotal, highlight = true)

                            Spacer(Modifier.height(28.dp))

                            Text("Date: $date", fontSize = 11.sp, color = Color.Gray)
                            Text("Valid until: $validUntil", fontSize = 11.sp, color = Color.Gray)

                            if (settings.footerText.isNotBlank()) {
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    settings.footerText,
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// ENTRY POINTS (Quotation / Proforma / Details)
// ============================================================

@Composable
fun QuotationFullScreen(
    lead: Lead,
    products: List<Product>,
    currentUser: Employee? = null,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var licenses by remember { mutableStateOf<List<com.fsscrm.network.CustomerLicense>>(emptyList()) }

    LaunchedEffect(lead.id) {
        if (lead.id > 0) {
            try {
                val uId = currentUser?.id ?: 0
                val resp = if (uId > 0) {
                    RetrofitClient.apiService.getLeadDetails(mapOf("lead_id" to lead.id, "user_id" to uId))
                } else {
                    RetrofitClient.apiService.getAdminLeadDetails(mapOf("lead_id" to lead.id))
                }
                val finalResp = if (resp.isSuccessful) resp else RetrofitClient.apiService.getAdminLeadDetails(mapOf("lead_id" to lead.id))
                if (finalResp.isSuccessful) {
                    finalResp.toLenientJson()?.let {
                        licenses = com.fsscrm.network.LeadDetailsResponse.fromJson(it).customerLicenses
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val context = LocalContext.current
    var settings by remember { mutableStateOf(QuoteSettingsManager.loadSettings(context)) }

    if (showSettings) {
        QuoteSettingsScreen { 
            settings = QuoteSettingsManager.loadSettings(context)
            showSettings = false 
        }
    } else {
        QuoteDocumentComponent(
            title = "Quotation",
            lead = lead,
            products = products,
            licenses = licenses,
            currentUser = currentUser,
            onDismiss = onDismiss,
            onSettingsClick = { showSettings = true },
            onConfirm = { items, disc, discType, status, comments, docTitle, name, phone, company ->
                val subtotal = items.sumOf { parsePrice(it.cost) * it.quantityInt }
                val discVal = parsePrice(disc)
                val calculatedDiscount = if (discType == "%") subtotal * (discVal / 100) else discVal
                val tax = if (settings.includeTax) {
                    (subtotal - calculatedDiscount) * (settings.taxRate / 100)
                } else 0.0
                val total = subtotal - calculatedDiscount + tax

                val map = buildConfirmMap(
                    items, calculatedDiscount, tax, total,
                    "Quotation", name, phone, company, status, comments,
                    isProforma = false
                )
                onConfirm(map)
            }
        )
    }
}

@Composable
fun ProposalFullScreen(
    lead: Lead,
    products: List<Product>,
    latestQuote: QuoteDetails? = null,
    currentUser: Employee? = null,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    var licenses by remember { mutableStateOf<List<com.fsscrm.network.CustomerLicense>>(emptyList()) }
    var fetchedQuote by remember { mutableStateOf<QuoteDetails?>(null) }
    
    LaunchedEffect(lead.id) {
        if (lead.id > 0) {
            try {
                val uId = currentUser?.id ?: 0
                val resp = if (uId > 0) {
                    RetrofitClient.apiService.getLeadDetails(mapOf("lead_id" to lead.id, "user_id" to uId))
                } else {
                    RetrofitClient.apiService.getAdminLeadDetails(mapOf("lead_id" to lead.id))
                }
                val finalResp = if (resp.isSuccessful) resp else RetrofitClient.apiService.getAdminLeadDetails(mapOf("lead_id" to lead.id))
                if (finalResp.isSuccessful) {
                    finalResp.toLenientJson()?.let {
                        val details = com.fsscrm.network.LeadDetailsResponse.fromJson(it)
                        licenses = details.customerLicenses
                        if (fetchedQuote == null) {
                            fetchedQuote = details.quotes.lastOrNull { q -> q.requirements?.isNotEmpty() == true || !q.customer_name.isNullOrBlank() }
                                ?: details.quotes.lastOrNull()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val context = LocalContext.current
    var settings by remember { mutableStateOf(QuoteSettingsManager.loadSettings(context)) }

    if (showSettings) {
        QuoteSettingsScreen { 
            settings = QuoteSettingsManager.loadSettings(context)
            showSettings = false 
        }
    } else {
        val activeQuote = latestQuote ?: fetchedQuote
        QuoteDocumentComponent(
            title = "Proforma Invoice",
            lead = lead,
            products = products,
            licenses = licenses,
            currentUser = currentUser,
            initialQuote = activeQuote,
            initialItems = activeQuote?.requirements ?: emptyList(),
            initialDiscount = activeQuote?.discount ?: "0.00",
            initialComments = activeQuote?.admin_notes ?: "",
            onDismiss = onDismiss,
            onSettingsClick = { showSettings = true },
            onConfirm = { items, disc, discType, status, comments, docTitle, name, phone, company ->
                val subtotal = items.sumOf { parsePrice(it.cost) * it.quantityInt }
                val discVal = parsePrice(disc)
                val calculatedDiscount = if (discType == "%") subtotal * (discVal / 100) else discVal
                val tax = if (settings.includeTax) {
                    (subtotal - calculatedDiscount) * (settings.taxRate / 100)
                } else 0.0
                val total = subtotal - calculatedDiscount + tax

                val map = buildConfirmMap(
                    items, calculatedDiscount, tax, total,
                    "Proforma Invoice", name, phone, company, status, comments,
                    isProforma = true
                )
                onConfirm(map)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailFullScreen(quote: QuoteDetails, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings = QuoteSettingsManager.loadSettings(context)
    
    QuotePreviewScreen(
        title = "Quote #${quote.quote_no}",
        settings = settings,
        clientName = quote.customer_name,
        clientPhone = quote.customer_phone ?: "",
        clientCompany = quote.customer_company ?: "",
        items = quote.requirements ?: emptyList(),
        discount = parsePrice(quote.discount),
        tax = parsePrice(quote.tax),
        grandTotal = parsePrice(quote.total),
        date = quote.quote_date ?: quote.created_at?.take(11) ?: "",
        validUntil = quote.valid_until ?: "",
        isViewOnly = true,
        onDismiss = onDismiss,
        onSettingsClick = {},
        onSaveAndFinish = {},
        onSaveAndSend = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProformaDetailFullScreen(pf: ProformaInvoice, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings = QuoteSettingsManager.loadSettings(context)
    
    QuotePreviewScreen(
        title = pf.proforma_no,
        settings = settings,
        clientName = pf.customer_name,
        clientPhone = pf.customer_phone ?: "",
        clientCompany = pf.customer_company ?: "",
        items = pf.items_parsed,
        discount = parsePrice(pf.discount),
        tax = parsePrice(pf.tax),
        grandTotal = parsePrice(pf.total),
        date = pf.created_at?.take(11) ?: "",
        validUntil = "", // No valid until in model?
        isViewOnly = true,
        onDismiss = onDismiss,
        onSettingsClick = {},
        onSaveAndFinish = {},
        onSaveAndSend = {}
    )
}

// ============================================================
// OTHER FULL SCREEN MODALS
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpFullScreen(lead: Lead, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var d by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var t by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var r by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                UniversalHeader(
                    title = "Schedule Follow-Up",
                    onBackClick = onDismiss,
                    actions = {
                        TextButton(onClick = { if (r.isNotBlank()) onConfirm(d, t, r) }) {
                            Text("SAVE", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                )
            }
        ) { p ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(p)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(lead.safeName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(lead.company ?: "Individual Lead", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(d, { d = it }, Modifier.fillMaxWidth(), label = { Text("Date") })
                OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), label = { Text("Time") })
                OutlinedTextField(r, { r = it }, Modifier.fillMaxWidth(), label = { Text("Remarks *") }, minLines = 4)
            }
        }
    }
}

@Composable
fun WonFullScreen(
    lead: Lead,
    latestProforma: ProformaInvoice?,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    var comp by remember { mutableStateOf(lead.company ?: "") }
    var name by remember { mutableStateOf(lead.name ?: "") }
    var amt by remember { mutableStateOf(latestProforma?.total ?: "") }
    var adv by remember { mutableStateOf("0") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                UniversalHeader(
                    title = "Won Deal Setup",
                    onBackClick = onDismiss,
                    actions = {
                        TextButton(onClick = {
                            onConfirm(
                                mapOf(
                                    "company" to comp,
                                    "customer_name" to name,
                                    "total_amount" to amt,
                                    "advance_received" to adv,
                                    "work_name" to (lead.service ?: "New Project")
                                )
                            )
                        }) {
                            Text("CONFIRM", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { p ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(p)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(comp, { comp = it }, Modifier.fillMaxWidth(), label = { Text("Final Company") })
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Final Customer Name") })
                OutlinedTextField(amt, { amt = it }, Modifier.fillMaxWidth(), label = { Text("Deal Total") })
                OutlinedTextField(adv, { adv = it }, Modifier.fillMaxWidth(), label = { Text("Advance Received") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteFullScreen(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var n by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                UniversalHeader(
                    title = "Internal Note",
                    onBackClick = onDismiss,
                    actions = {
                        TextButton(onClick = { if (n.isNotBlank()) onConfirm(n) }) {
                            Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { p ->
            Column(Modifier.padding(p).padding(24.dp)) {
                OutlinedTextField(
                    n,
                    { n = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Discussion Details") },
                    minLines = 10
                )
            }
        }
    }
}

@Composable
fun RescheduleFollowUpDialog(fu: FollowUp, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var d by remember { mutableStateOf(fu.follow_up_date ?: "") }
    var t by remember { mutableStateOf(fu.follow_up_time ?: "10:00") }
    var r by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule") },
        text = {
            Column {
                OutlinedTextField(d, { d = it }, Modifier.fillMaxWidth(), label = { Text("New Date") })
                OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), label = { Text("New Time") })
                OutlinedTextField(r, { r = it }, Modifier.fillMaxWidth(), label = { Text("Reason") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(d, t, r) }) { Text("CONFIRM") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun UpdateWorkStatusModal(
    work: Work,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, String, Double) -> Unit
) {
    var st by remember { mutableStateOf(work.status) }
    var rem by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Work Status") },
        text = {
            Column {
                listOf("pending", "in_progress", "on_hold").forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { st = s }
                    ) {
                        RadioButton(selected = st == s, onClick = { st = s })
                        Text(s.uppercase())
                    }
                }
                OutlinedTextField(rem, { rem = it }, Modifier.fillMaxWidth(), label = { Text("Remarks") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(work.id, st, rem, "", 0.0) }) { Text("UPDATE") }
        }
    )
}

@Composable
fun AddPaymentModal(
    work: Work,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, String, String, String, String, String) -> Unit
) {
    var amt by remember { mutableStateOf("") }
    var meth by remember { mutableStateOf("cash") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Payment") },
        text = {
            Column {
                Text("Balance: ₹${work.balance_amount ?: "0"}", fontWeight = FontWeight.Bold)
                OutlinedTextField(amt, { amt = it }, Modifier.fillMaxWidth(), label = { Text("Amount") })
                listOf("cash", "upi", "bank").forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { meth = m }
                    ) {
                        RadioButton(selected = meth == m, onClick = { meth = m })
                        Text(m.uppercase())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amt.toDoubleOrNull()
                if (a != null) onConfirm(work.id, a, meth, "partial", "", "", "")
            }) { Text("PAY") }
        }
    )
}

@Composable
fun CompleteWorkModal(
    work: Work,
    customerExists: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, String, String, String, String, String) -> Unit
) {
    var ser by remember { mutableStateOf(work.license_key ?: work.serial_number ?: "") }
    var em by remember { mutableStateOf(work.customer_email ?: work.lead_email ?: "") }
    var pas by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Completion & Handover") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(ser, { ser = it }, Modifier.fillMaxWidth(), label = { Text("Final License Key / Serial") })
                if (!customerExists) {
                    OutlinedTextField(em, { em = it }, Modifier.fillMaxWidth(), label = { Text("Customer Email") })
                    OutlinedTextField(
                        value = pas,
                        onValueChange = { pas = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Set Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(work.id, ser, work.customer_company ?: "", "", "", "{}", em, pas)
            }) { Text("COMPLETE") }
        }
    )
}

@Composable
fun LeadAssignmentDialog(
    userId: Int,
    leadId: Int,
    initialEmployees: List<Employee>? = null,
    onDismiss: () -> Unit,
    onAssigned: () -> Unit
) {
    var employees by remember { mutableStateOf(initialEmployees ?: emptyList()) }
    var isLoading by remember { mutableStateOf(initialEmployees == null) }
    var isAssigning by remember { mutableStateOf<Int?>(null) } // Store ID of employee being assigned
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (initialEmployees != null && initialEmployees.isNotEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            val resp = RetrofitClient.apiService.getEmployees(mapOf("user_id" to userId))
            if (resp.isSuccessful) {
                employees = resp.body()?.employees ?: emptyList()
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (isAssigning == null) onDismiss() },
        title = { Text("Assign Lead to Sales Staff") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sales staff...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(8.dp),
                    enabled = isAssigning == null
                )

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                } else {
                    val filtered = employees.filter {
                        // Filter by Sales department AND search query
                        val inSales = it.department_name?.contains("Sales", ignoreCase = true) == true ||
                                     it.role?.contains("Sales", ignoreCase = true) == true
                        
                        inSales && (
                            (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                            (it.role ?: "").contains(searchQuery, ignoreCase = true)
                        )
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            if (searchQuery.isBlank()) "No sales staff found" else "No staff matches search",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    } else {
                        Column(
                            Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())
                        ) {
                            filtered.forEach { emp ->
                                val empId = emp.id ?: 0
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable(enabled = isAssigning == null) {
                                            isAssigning = empId
                                            scope.launch {
                                                try {
                                                    val response = RetrofitClient.apiService.assignLead(
                                                        mapOf(
                                                            "user_id" to userId.toString(),
                                                            "lead_id" to leadId.toString(),
                                                            "employee_id" to empId.toString(),
                                                            "action" to "assign_lead"
                                                        )
                                                    )
                                                    if (response.isSuccessful) {
                                                        val body = response.body()?.asJsonObject
                                                        if (body?.get("success")?.asBoolean == true || body?.get("status")?.asString == "success") {
                                                            onAssigned()
                                                        } else {
                                                            val msg = body?.get("message")?.asString ?: body?.get("error")?.asString ?: "Assignment failed"
                                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Server error: ${response.code()}", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isAssigning = null
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isAssigning == empId) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                                    ),
                                    border = BorderStroke(1.dp, if (isAssigning == empId) PrimaryIndigo else Color(0xFFE2E8F0))
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (isAssigning == empId) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = PrimaryIndigo)
                                        } else {
                                            Icon(Icons.Default.Person, null, tint = PrimaryIndigo)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(emp.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(emp.role ?: "Staff", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { 
            TextButton(onClick = onDismiss, enabled = isAssigning == null) { 
                Text("Cancel") 
            } 
        }
    )
}

// ============================================================
// SMALL HELPER
// ============================================================

private fun buildConfirmMap(
    items: List<QuoteRequirement>,
    discount: Double,
    tax: Double,
    total: Double,
    title: String,
    name: String,
    phone: String,
    company: String,
    status: String,
    comments: String,
    isProforma: Boolean = false
): Map<String, String> {
    val subtotal = items.sumOf { parsePrice(it.cost) * it.quantityInt }
    val jsonItems = Gson().toJson(items)
    val subtotalStr = "%.2f".format(subtotal)
    val taxStr = "%.2f".format(tax)
    val discountStr = "%.2f".format(discount)
    val totalStr = "%.2f".format(total)
    return mapOf(
        "customer_name" to name,
        "customer_phone" to phone,
        "customer_company" to company,
        "proforma_customer_name" to name,
        "proforma_customer_phone" to phone,
        "proforma_customer_company" to company,
        if (isProforma) "proforma_title" to title else "quote_title" to title,
        "amount" to subtotalStr,
        "quote_amount" to subtotalStr,
        "proforma_amount" to subtotalStr,
        "tax" to taxStr,
        "quote_tax" to taxStr,
        "proforma_tax" to taxStr,
        "discount" to discountStr,
        "quote_discount" to discountStr,
        "proforma_discount" to discountStr,
        "total" to totalStr,
        "quote_total" to totalStr,
        "proforma_total" to totalStr,
        "status" to status,
        "comments" to comments,
        "remarks" to comments,
        "admin_notes" to comments,
        "requirements" to jsonItems,
        "items" to jsonItems,
        "proforma_items" to jsonItems
    )
}
