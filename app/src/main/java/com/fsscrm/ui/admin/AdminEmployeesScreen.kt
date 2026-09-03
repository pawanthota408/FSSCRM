package com.fsscrm.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fsscrm.network.Employee
import com.fsscrm.network.EmployeesResponse
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmployeesScreen(
    userId: Int,
    onMenuClick: () -> Unit
) {
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    fun fetchEmployees() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getEmployees(mapOf("user_id" to userId))
                if (response.isSuccessful && response.body() != null) {
                    employees = response.body()!!.employees
                }
            } catch (e: Exception) {
                // Error handling
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchEmployees()
    }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isEmpty()) employees
        else employees.filter { 
            (it.name ?: "").contains(searchQuery, ignoreCase = true) || 
            (it.role ?: "").contains(searchQuery, ignoreCase = true) ||
            (it.employee_code ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            UniversalHeader(
                title = "Staff Management",
                onMenuClick = onMenuClick
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search staff...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White, 
                    unfocusedContainerColor = Color.White
                )
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredEmployees) { emp ->
                        EmployeeCard(emp)
                    }
                    if (filteredEmployees.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No employees found", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeCard(emp: Employee) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (emp.profile_image != null) {
                    AsyncImage(
                        model = emp.profile_image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = PrimaryIndigo)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(emp.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(emp.role ?: "Employee", color = Color.Gray, fontSize = 13.sp)
                if (emp.employee_code != null) {
                    Text("ID: ${emp.employee_code}", fontSize = 12.sp, color = Color.LightGray)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (emp.status == "active") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        emp.status ?: "inactive",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (emp.status == "active") Color(0xFF166534) else Color(0xFF991B1B)
                    )
                }
            }
        }
    }
}
