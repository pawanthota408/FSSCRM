package com.fsscrm.ui.sales

import com.fsscrm.ui.common.*
import com.fsscrm.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri
import com.fsscrm.network.Customer
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    userId: Int,
    onMenuClick: () -> Unit,
    navController: NavController
) {
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var totalRecords by remember { mutableStateOf(0) }
    var employeeInfo by remember { mutableStateOf<EmployeeInfo?>(null) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Load customers with pagination
    fun loadCustomers(
        query: String = "",
        page: Int = 1,
        isLoadMore: Boolean = false
    ) {
        if (isLoadMore) {
            isLoadingMore = true
        } else {
            isLoading = true
            errorMessage = null
        }

        scope.launch {
            try {
                val params = mutableMapOf(
                    "user_id" to userId.toString(),
                    "page" to page.toString(),
                    "limit" to "20"
                )

                if (query.isNotEmpty()) {
                    params["query"] = query
                }

                val response = RetrofitClient.apiService.getCustomers(params)

                if (response.isSuccessful) {
                    val result = response.toLenientJson()?.let { parseCustomersResponse(it) }

                    if (result != null) {
                        // Update pagination info
                        totalPages = result.pagination?.totalPages ?: 1
                        totalRecords = result.pagination?.totalRecords ?: 0
                        currentPage = page

                        // Update employee info
                        employeeInfo = result.userInfo

                        // Update customers list
                        if (isLoadMore) {
                            customers = customers + result.data
                        } else {
                            customers = result.data
                        }

                        errorMessage = null
                    } else {
                        errorMessage = "Failed to parse data"
                    }
                } else {
                    errorMessage = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    // Load initial data
    LaunchedEffect(Unit) {
        loadCustomers()
    }

    // Handle search
    fun performSearch(query: String) {
        searchQuery = query
        if (query.length >= 2 || query.isEmpty()) {
            loadCustomers(query = query, page = 1)
        }
    }

    // Load more when reaching the end
    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (lastVisibleIndex >= 0 &&
            lastVisibleIndex >= customers.size - 3 &&
            currentPage < totalPages &&
            !isLoadingMore &&
            !isLoading
        ) {
            loadCustomers(query = searchQuery, page = currentPage + 1, isLoadMore = true)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            UniversalHeader(
                title = "Customers List",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { loadCustomers() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                }
            )

            // Search Bar (Traditional Style)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { performSearch(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by customer name or company...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Stats bar
            if (customers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Showing ${customers.size} of $totalRecords customers",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        "Page $currentPage of $totalPages",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Error message
            errorMessage?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE2E2)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = Color.Red, fontSize = 13.sp)
                    }
                }
            }

            // Main content
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryIndigo)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading customers...", color = Color.Gray)
                        }
                    }
                }

                customers.isEmpty() && searchQuery.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Group,
                        title = "No Customers Found",
                        message = "You haven't completed any customers yet.\nUse the search bar above to find customers by name, company, or serial number.",
                        buttonText = "Refresh",
                        onButtonClick = { loadCustomers() }
                    )
                }

                customers.isEmpty() && searchQuery.isNotEmpty() -> {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = "No Results Found",
                        message = "No customers found matching \"$searchQuery\"",
                        buttonText = "Clear Search",
                        onButtonClick = {
                            searchQuery = ""
                            loadCustomers()
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(customers) { customer ->
                            CustomerCard(
                                customer = customer,
                                onClick = {
                                    navController.navigate("customer_details/${customer.id}")
                                }
                            )
                        }

                        // Loading more indicator
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(horizontalArrangement = Arrangement.Center) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = PrimaryIndigo,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading more...", color = Color.Gray, fontSize = 12.sp)
                                    }
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
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(buttonText, color = Color.White)
            }
        }
    }
}

@Composable
fun CustomerCard(customer: Customer, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        customer.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Company and Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        customer.company ?: "No Company",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            customer.name,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Employee badge
                        customer.assignedEmployeeName?.let { empName ->
                            Surface(
                                color = PrimaryIndigo.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "👤 $empName",
                                    fontSize = 9.sp,
                                    color = PrimaryIndigo,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Status Badge
                val isActive = customer.status?.lowercase() == "active"
                Surface(
                    color = if (isActive) Color(0xFF10B981).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        (customer.status ?: "Unknown").uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isActive) Color(0xFF10B981) else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Contact Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Phone
                customer.phone?.let { phone ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryIndigo
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            phone,
                            fontSize = 12.sp,
                            color = PrimaryIndigo
                        )
                    }
                }

                // Email
                customer.email?.let { email ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                        }
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            email,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Expand/Collapse for more details
            if (customer.source != null || customer.contactPerson != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (expanded) "Show Less" else "Show More",
                            fontSize = 11.sp,
                            color = PrimaryIndigo
                        )
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryIndigo
                        )
                    }
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        customer.source?.let {
                            DetailRow("Source", it)
                        }
                        customer.contactPerson?.let {
                            DetailRow("Contact Person", it)
                        }
                        customer.contactEmail?.let {
                            DetailRow("Contact Email", it)
                        }
                        customer.contactMobile?.let {
                            DetailRow("Contact Mobile", it)
                        }
                        customer.employeeCode?.let {
                            DetailRow("Employee Code", it)
                        }
                        customer.employeePosition?.let {
                            DetailRow("Position", it)
                        }
                        customer.licenseKey?.let {
                            DetailRow("License Key", it)
                        }
                        customer.createdAt?.let {
                            DetailRow("Created", formatDate(it))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 11.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}



// Data classes for API response
data class PaginationInfo(
    val currentPage: Int,
    val perPage: Int,
    val totalRecords: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class EmployeeInfo(
    val userId: Int,
    val employeeId: Int,
    val employeeName: String,
    val isAdmin: Boolean
)

data class CustomersResponse(
    val status: String,
    val message: String,
    val data: List<Customer>,
    val pagination: PaginationInfo?,
    val userInfo: EmployeeInfo?,
    val count: Int
)

// Parser function
fun parseCustomersResponse(json: com.google.gson.JsonElement): CustomersResponse? {
    return try {
        val gson = Gson()
        if (json.isJsonObject) {
            val obj = json.asJsonObject

            // Check for status
            val status = if (obj.has("status")) obj.get("status").asString else "success"

            // Get customers data
            val customers: List<Customer> = if (obj.has("data")) {
                val dataElement = obj.get("data")
                if (dataElement.isJsonArray) {
                    gson.fromJson(dataElement, object : TypeToken<List<Customer>>() {}.type)
                } else {
                    emptyList()
                }
            } else if (obj.has("customers")) {
                gson.fromJson(obj.get("customers"), object : TypeToken<List<Customer>>() {}.type)
            } else {
                emptyList()
            }

            // Get pagination info
            val pagination = if (obj.has("pagination")) {
                gson.fromJson(obj.get("pagination"), PaginationInfo::class.java)
            } else {
                null
            }

            // Get user info
            val userInfo = if (obj.has("user_info")) {
                gson.fromJson(obj.get("user_info"), EmployeeInfo::class.java)
            } else {
                null
            }

            CustomersResponse(
                status = status,
                message = if (obj.has("message") && !obj.get("message").isJsonNull) obj.get("message").asString else "",
                data = customers,
                pagination = pagination,
                userInfo = userInfo,
                count = if (obj.has("count") && !obj.get("count").isJsonNull) obj.get("count").asInt else customers.size
            )
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}