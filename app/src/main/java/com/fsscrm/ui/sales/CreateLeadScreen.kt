package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.*
import com.fsscrm.ui.common.handleJsonResponse
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLeadScreen(
    userId: Int, 
    initialPhone: String = "", 
    initialMessage: String = "", 
    autoOpenQuotation: Boolean = false,
    autoOpenFollowup: Boolean = false,
    onCancel: () -> Unit, 
    onSuccess: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(if (initialPhone.isNotEmpty()) 1 else 0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = if (currentStep == 0) "Create Lead" else if (currentStep == 1) "New Customer" else "Existing Customer",
                onBackClick = {
                    if (currentStep == 0 || initialPhone.isNotEmpty()) {
                        onCancel()
                    } else {
                        currentStep = 0
                    }
                }
            )
            
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    0 -> CustomerTypeChoice { isNew ->
                        currentStep = if (isNew) 1 else 2
                    }
                    1 -> NewCustomerForm(
                        userId = userId, 
                        initialPhone = initialPhone, 
                        initialMessage = initialMessage, 
                        autoOpenQuotation = autoOpenQuotation,
                        autoOpenFollowup = autoOpenFollowup,
                        scope = scope, 
                        snackbarHostState = snackbarHostState, 
                        onSuccess = onSuccess
                    )
                    2 -> ExistingCustomerSearch(userId, scope, snackbarHostState, onSuccess)
                }
            }
        }
    }
}

@Composable
fun CustomerTypeChoice(onChoice: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onChoice(true) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("New Customer", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { onChoice(false) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, PrimaryIndigo)
        ) {
            Icon(Icons.Default.Group, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Existing Customer", fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCustomerForm(
    userId: Int, 
    initialPhone: String = "", 
    initialMessage: String = "", 
    autoOpenQuotation: Boolean = false,
    autoOpenFollowup: Boolean = false,
    scope: kotlinx.coroutines.CoroutineScope, 
    snackbarHostState: SnackbarHostState, 
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var tallySerial by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(initialMessage) }

    var existingLicenses by remember { mutableStateOf<List<CustomerLicense>>(emptyList()) }
    var isSearchingCustomer by remember { mutableStateOf(false) }

    var connectionType by remember { mutableStateOf("New Tally Software") }
    val connectionTypes = listOf("New Tally Software", "Services", "IT Services")

    var leadType by remember { mutableStateOf("Warm") }
    val leadTypes = listOf("Hot", "Warm", "Cold")

    var source by remember { mutableStateOf("Mobile App") }
    val sources = listOf("Mobile App", "Google", "Reference", "Calling", "Exhibition", "Website Enquiry")

    val selectedServices = remember { mutableStateListOf<String>() }

    var products by remember { mutableStateOf<List<com.fsscrm.network.Product>>(emptyList()) }
    
    // For auto-opening modals after creation
    var createdLeadId by remember { mutableStateOf<Int?>(null) }
    var showFollowUpModal by remember { mutableStateOf(false) }
    var showQuotationModal by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<Employee?>(null) }
    
    val currentLeadForModals = remember(createdLeadId, name, mobile, company) {
        Lead(
            id = createdLeadId ?: 0,
            name = name,
            phone = mobile,
            company = company,
            status = "New",
            created_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }

    LaunchedEffect(Unit) {
        // Fetch profile
        scope.launch {
            try {
                val profResp = RetrofitClient.apiService.getProfile(userId)
                if (profResp.isSuccessful) currentUser = profResp.body()?.employee
            } catch (e: Exception) {}
        }

        try {
            val resp = RetrofitClient.apiService.getProducts(mapOf("user_id" to userId))
            if (resp.isSuccessful) {
                resp.toLenientJson()?.let { json ->
                    products = when {
                        json.isJsonArray -> {
                            com.google.gson.Gson().fromJson(json, object : com.google.gson.reflect.TypeToken<List<com.fsscrm.network.Product>>() {}.type)
                        }
                        json.isJsonObject -> {
                            val obj = json.asJsonObject
                            val array = when {
                                obj.has("products_services") -> obj.get("products_services")
                                obj.has("products") -> obj.get("products")
                                obj.has("data") -> obj.get("data")
                                else -> obj.entrySet().firstOrNull { it.value.isJsonArray }?.value
                            }
                            if (array != null && array.isJsonArray) {
                                com.google.gson.Gson().fromJson(array, object : com.google.gson.reflect.TypeToken<List<com.fsscrm.network.Product>>() {}.type)
                            } else emptyList()
                        }
                        else -> emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CreateLead", "Error fetching products", e)
        }
    }

    // Auto-fetch licenses if existing customer found
    LaunchedEffect(mobile, email) {
        val query = if (mobile.length >= 10) mobile else if (email.contains("@") && email.length > 5) email else ""
        if (query.isNotEmpty()) {
            isSearchingCustomer = true
            try {
                val resp = RetrofitClient.apiService.getCustomers(mapOf("user_id" to userId.toString(), "query" to query))
                if (resp.isSuccessful) {
                    resp.toLenientJson()?.let { json ->
                        val customerList = if (json.isJsonArray) {
                            com.google.gson.Gson().fromJson(json, object : com.google.gson.reflect.TypeToken<List<Customer>>() {}.type)
                        } else if (json.isJsonObject && json.asJsonObject.has("data")) {
                            com.google.gson.Gson().fromJson(json.asJsonObject.get("data"), object : com.google.gson.reflect.TypeToken<List<Customer>>() {}.type)
                        } else emptyList<Customer>()
                        
                        val found = customerList.find { it.phone == mobile || it.email == email }
                        if (found != null) {
                            val licResp = RetrofitClient.apiService.getExistingLicenses(mapOf("lead_id" to (found.lead_id ?: 0), "user_id" to userId))
                            if (licResp.isSuccessful) {
                                licResp.toLenientJson()?.let { licJson ->
                                    val licResponse = CustomerLicensesResponse.fromJson(licJson)
                                    existingLicenses = licResponse.licenses ?: emptyList()
                                }
                            }
                        } else {
                            existingLicenses = emptyList()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Lookup", "Search failed", e)
            } finally {
                isSearchingCustomer = false
            }
        }
    }

    // Dynamic License Requirement Logic - More reactive and inclusive of connectionType
    val showSerialField = connectionType != "New Tally Software" || selectedServices.any { serviceName ->
        products.find { it.name == serviceName }?.serial_at == "quote"
    }
    val serialRequired = showSerialField

    // Filter products based on connection type
    val filteredProducts = remember(connectionType, products) {
        val tallyKeywords = listOf("Gold", "Silver", "Server")
        val itKeywords = listOf("Website", "App", "Development", "Hosting", "Custom Software")
        
        when (connectionType) {
            "New Tally Software" -> {
                products.filter { p -> tallyKeywords.any { k -> p.name.contains(k, ignoreCase = true) } }
            }
            "IT Services" -> {
                products.filter { p -> 
                    val n = p.name.lowercase()
                    val c = p.category.lowercase()
                    itKeywords.any { k -> n.contains(k.lowercase()) } || 
                    c.contains("it") || c.contains("software development") || c.contains("web") || c.contains("app")
                }
            }
            "Services" -> {
                products.filter { p ->
                    val n = p.name.lowercase()
                    val c = p.category.lowercase()
                    val isTally = tallyKeywords.any { k -> n.contains(k.lowercase()) }
                    val isIT = itKeywords.any { k -> n.contains(k.lowercase()) } || 
                               c.contains("it") || c.contains("software development") || c.contains("web") || c.contains("app")
                    
                    !isTally && !isIT
                }
            }
            else -> products
        }
    }

    // Reset selection when connection type changes
    LaunchedEffect(connectionType) {
        selectedServices.clear()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = mobile, 
            onValueChange = { mobile = it }, 
            label = { Text("Phone Number") }, 
            modifier = Modifier.fillMaxWidth(), 
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
            trailingIcon = { if (isSearchingCustomer) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
        )
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address *") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email))
        OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
        
        if (showSerialField) {
            var expandedLicense by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = tallySerial, 
                    onValueChange = { tallySerial = it }, 
                    label = { Text("Serial / Licence Number *") }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = tallySerial.isBlank(),
                    placeholder = { Text("e.g. ya12342588844") },
                    trailingIcon = {
                        if (existingLicenses.isNotEmpty()) {
                            IconButton(onClick = { expandedLicense = true }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    }
                )
                DropdownMenu(
                    expanded = expandedLicense, 
                    onDismissRequest = { expandedLicense = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    existingLicenses.forEach { lic ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(lic.item_name ?: "License", fontWeight = FontWeight.Bold)
                                    Text(lic.license_key ?: "", fontSize = 12.sp, color = Color.Gray)
                                }
                            },
                            onClick = { 
                                tallySerial = lic.license_key ?: ""
                                expandedLicense = false 
                            }
                        )
                    }
                }
            }
            if (serialRequired && tallySerial.isBlank()) {
                Text("Serial number is required for selected services", color = Color.Red, fontSize = 11.sp)
            }
            if (existingLicenses.isNotEmpty()) {
                Text("Found ${existingLicenses.size} existing license(s) for this customer", color = PrimaryIndigo, fontSize = 11.sp)
            }
        }

        Text("What are you connecting for? *", fontWeight = FontWeight.Bold)
        var expandedConn by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expandedConn = true }, modifier = Modifier.fillMaxWidth()) {
                Text(connectionType)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expandedConn, onDismissRequest = { expandedConn = false }) {
                connectionTypes.forEach { t ->
                    DropdownMenuItem(text = { Text(t) }, onClick = { connectionType = t; expandedConn = false })
                }
            }
        }

        Text("Select Options *", fontWeight = FontWeight.Bold)
        if (filteredProducts.isEmpty()) {
            Text("No options found for this category", color = Color.Gray, fontSize = 12.sp)
        }
        filteredProducts.forEach { service ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                if (selectedServices.contains(service.name)) selectedServices.remove(service.name) else selectedServices.add(service.name)
            }) {
                Checkbox(
                    checked = selectedServices.contains(service.name),
                    onCheckedChange = {
                        if (it) selectedServices.add(service.name) else selectedServices.remove(service.name)
                    }
                )
                Text("${service.name} (${service.category})")
            }
        }

        Text("Lead Type", fontWeight = FontWeight.Bold)
        Row {
            leadTypes.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    RadioButton(selected = leadType == type, onClick = { leadType = type })
                    Text(type)
                }
            }
        }

        Text("Lead Source", fontWeight = FontWeight.Bold)
        var expandedSource by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expandedSource = true }, modifier = Modifier.fillMaxWidth()) {
                Text(source)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expandedSource, onDismissRequest = { expandedSource = false }) {
                sources.forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = { source = s; expandedSource = false })
                }
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Your Message *") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4
        )

        Button(
            onClick = {
                val isNameValid = name.trim().isNotBlank()
                val isEmailValid = email.trim().isNotBlank()
                val isMessageValid = message.trim().isNotBlank()
                val isConnTypeValid = connectionType.isNotBlank()
                val isServicesValid = selectedServices.isNotEmpty()

                if (!isNameValid || !isEmailValid || !isMessageValid || !isConnTypeValid || !isServicesValid) {
                    scope.launch { snackbarHostState.showSnackbar("Please fill all required fields (Name, Email, Message, Services)") }
                    return@Button
                }
                
                if (serialRequired && tallySerial.trim().isBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Serial number is required for $connectionType") }
                    return@Button
                }

                val serviceStr = selectedServices.joinToString(", ")
                scope.launch {
                    try {
                        val response = RetrofitClient.apiService.createLead(mapOf(
                            "user_id" to userId.toString(),
                            "name" to name.trim(),
                            "email" to email.trim(),
                            "phone" to mobile.trim(),
                            "company" to company.trim(),
                            "connection_type" to connectionType,
                            "serial_number" to tallySerial.trim(),
                            "license_key" to tallySerial.trim(),
                            "message" to message.trim(),
                            "lead_type" to leadType,
                            "service" to serviceStr,
                            "source" to source,
                            "status" to "New",
                            "created_by" to userId.toString()
                        ))

                        handleJsonResponse(
                            response = response,
                            onSuccess = {
                                snackbarHostState.showSnackbar("Lead Created Successfully")
                                
                                val newId = if (response.isSuccessful) {
                                    val body = response.body()
                                    if (body != null && body.isJsonObject) {
                                        body.asJsonObject.get("id")?.asInt ?: 0
                                    } else 0
                                } else 0
                                
                                createdLeadId = newId
                                if (autoOpenQuotation) {
                                    showQuotationModal = true
                                } else if (autoOpenFollowup) {
                                    showFollowUpModal = true
                                } else {
                                    onSuccess()
                                }
                            },
                            onError = { errorMsg ->
                                snackbarHostState.showSnackbar("Server Error: $errorMsg")
                            }
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Connection Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Submit Lead")
        }
    }

    if (showFollowUpModal && createdLeadId != null) {
        FollowUpFullScreen(currentLeadForModals, { showFollowUpModal = false; onSuccess() }) { d, t, r ->
            scope.launch {
                if (RetrofitClient.apiService.addFollowUp(
                        mapOf(
                            "user_id" to userId.toString(),
                            "lead_id" to createdLeadId.toString(),
                            "follow_up_date" to d,
                            "follow_up_time" to t,
                            "remarks" to r,
                            "action" to "schedule_follow_up"
                        )
                    ).isSuccessful
                ) {
                    showFollowUpModal = false
                    onSuccess()
                }
            }
        }
    }

    if (showQuotationModal && createdLeadId != null) {
        QuotationFullScreen(
            currentLeadForModals,
            products,
            currentUser,
            { showQuotationModal = false; onSuccess() }
        ) { data ->
            scope.launch {
                if (RetrofitClient.apiService.updateLeadStatus(
                        data.toMutableMap().apply {
                            put("user_id", userId.toString())
                            put("lead_id", createdLeadId.toString())
                            put("action", "update_followup_stage")
                            put("stage", "Quotation")
                        }
                    ).isSuccessful
                ) {
                    showQuotationModal = false
                    onSuccess()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExistingCustomerSearch(userId: Int, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState, onSuccess: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var licenseData by remember { mutableStateOf<CustomerLicensesResponse?>(null) }
    
    var showCreateLeadConfirm by remember { mutableStateOf<String?>(null) }
    var isCreatingLead by remember { mutableStateOf(false) }

    fun performSearch(searchQuery: String = "") {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getCustomers(mapOf(
                    "user_id" to userId.toString(),
                    "query" to searchQuery
                ))
                if (response.isSuccessful) {
                    response.toLenientJson()?.let { json ->
                        if (json.isJsonArray) {
                            customers = com.google.gson.Gson().fromJson(json, object : com.google.gson.reflect.TypeToken<List<Customer>>() {}.type)
                        } else if (json.isJsonObject) {
                            val obj = json.asJsonObject
                            if (obj.has("status") && obj.get("status").asString == "error") {
                                snackbarHostState.showSnackbar(obj.get("message").asString)
                            } else if (obj.has("data") && obj.get("data").isJsonArray) {
                                customers = com.google.gson.Gson().fromJson(obj.get("data"), object : com.google.gson.reflect.TypeToken<List<Customer>>() {}.type)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                snackbarHostState.showSnackbar("Search failed: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        performSearch() // Load all customers for this employee initially
    }

    var selectedLicenseKey by remember { mutableStateOf<String?>(null) }
    var licenseDetails by remember { mutableStateOf<LicenseDetailsResponse?>(null) }
    var isLoadingLicenseDetails by remember { mutableStateOf(false) }

    fun loadLicenseDetails(key: String) {
        selectedLicenseKey = key
        isLoadingLicenseDetails = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getLicenseDetails(mapOf(
                    "license_key" to key,
                    "user_id" to userId.toString()
                ))
                if (response.isSuccessful) {
                    licenseDetails = response.toLenientJson()?.let { LicenseDetailsResponse.fromJson(it) }
                }
            } finally {
                isLoadingLicenseDetails = false
            }
        }
    }

    if (selectedCustomer == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { 
                    query = it 
                    if (it.isEmpty()) performSearch() 
                },
                label = { Text("Search (Serial / Name / Mobile / Company)") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { performSearch(query) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customers) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedCustomer = customer
                            isLoading = true
                            scope.launch {
                                try {
                                    // Fetch all licenses for this customer
                            val resp = RetrofitClient.apiService.getExistingLicenses(mapOf("lead_id" to (customer.lead_id ?: 0), "user_id" to userId))
                            if (resp.isSuccessful) {
                                resp.toLenientJson()?.let {
                                    licenseData = CustomerLicensesResponse.fromJson(it)
                                }
                            }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(customer.company ?: "Unknown Company", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(customer.name, fontSize = 14.sp)
                            Text(customer.phone ?: "No Phone", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    } else if (selectedLicenseKey == null) {
        // Customer Overview & License List
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedCustomer = null; licenseData = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Customer Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(selectedCustomer?.company ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Contact: ${selectedCustomer?.name}")
                    Text("Phone: ${selectedCustomer?.phone}")
                }
            }

            // --- NEW TALLY PURCHASE BUTTONS ---
            Text("New Tally Purchase", fontWeight = FontWeight.Bold, color = PrimaryIndigo, modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tally Silver", "Tally Gold", "Tally Server").forEach { prod ->
                    Button(
                        onClick = { showCreateLeadConfirm = prod },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(prod.replace("Tally ", ""), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Existing Licenses", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            } else if (licenseData?.licenses.isNullOrEmpty()) {
                Text("No licenses found", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                licenseData?.licenses?.forEach { license ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { loadLicenseDetails(license.license_key!!) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(license.item_name ?: "Tally License", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Key: ${license.license_key}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    } else {
        // License Details & Upsell Opportunities
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedLicenseKey = null; licenseDetails = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("License Upsell", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(selectedLicenseKey ?: "", fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (isLoadingLicenseDetails) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Linked Items", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                licenseDetails?.linked_items?.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.item_name ?: "", fontSize = 14.sp) },
                        leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Upsell Opportunities", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                licenseDetails?.missing_products?.forEach { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(prod.category, fontSize = 11.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = { showCreateLeadConfirm = prod.name },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Create Lead", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateLeadConfirm != null && selectedCustomer != null) {
        val customer = selectedCustomer!!
        AlertDialog(
            onDismissRequest = { if (!isCreatingLead) showCreateLeadConfirm = null },
            title = { Text("New Lead") },
            text = { Text("Create a new lead for '${showCreateLeadConfirm}' for ${customer.company ?: customer.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val productName = showCreateLeadConfirm!!
                        
                        // LOGIC: If it's a New Tally software purchase, don't pass the license key
                        val isNewTallyPurchase = productName.contains("Silver") || productName.contains("Gold") || productName.contains("Server")
                        val keyToUse = if (isNewTallyPurchase) "" else (selectedLicenseKey ?: "")

                        isCreatingLead = true
                        scope.launch {
                            try {
                                val params = mapOf(
                                    "user_id" to userId.toString(),
                                    "name" to customer.name,
                                    "phone" to (customer.phone ?: ""),
                                    "email" to (customer.email ?: ""),
                                    "company" to (customer.company ?: ""),
                                    "customer_id" to customer.id.toString(),
                                    "license_key" to keyToUse,
                                    "serial_number" to keyToUse,
                                    "service" to productName,
                                    "connection_type" to (if(isNewTallyPurchase) "New Tally Software" else "Services"),
                                    "lead_type" to "Warm",
                                    "source" to "Existing Customer Upsell",
                                    "message" to "Upsell Lead for existing customer.",
                                    "status" to "New",
                                    "created_by" to userId.toString()
                                )
                                val response = RetrofitClient.apiService.createLead(params)
                                handleJsonResponse(
                                    response = response,
                                    onSuccess = {
                                        snackbarHostState.showSnackbar("Lead created successfully!")
                                        showCreateLeadConfirm = null
                                        onSuccess() // Redirect to Leads list
                                    },
                                    onError = { snackbarHostState.showSnackbar("Error: $it") }
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Connection Error: ${e.message}")
                            } finally {
                                isCreatingLead = false
                            }
                        }
                    },
                    enabled = !isCreatingLead
                ) {
                    if (isCreatingLead) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    else Text("YES, CREATE")
                }
            },
            dismissButton = {
                if (!isCreatingLead) {
                    TextButton(onClick = { showCreateLeadConfirm = null }) {
                        Text("CANCEL")
                    }
                }
            }
        )
    }
}
