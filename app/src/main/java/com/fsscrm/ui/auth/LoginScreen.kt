package com.fsscrm.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.R
import com.fsscrm.network.LoginRequest
import com.fsscrm.network.RetrofitClient
import com.fsscrm.network.SessionManager
import com.fsscrm.ui.common.SplashScreen
import com.fsscrm.ui.common.handleJsonResponse
import com.fsscrm.ui.theme.CorpBlue
import com.fsscrm.ui.theme.CorpDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (Int, String, String, Int, String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    
    // 0 = Employee, 1 = Admin
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionManager = remember { SessionManager(context) }

    // Splash Effect
    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Background
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)))
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hand),
                        contentDescription = "Logo",
                        modifier = Modifier.size(100.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Welcome Back", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = CorpDark)
                    Text("Sign in to your account", fontSize = 14.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(30.dp))

                    // Role Selection Toggle
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = CorpBlue,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = CorpBlue
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Employee", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Admin", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = CorpBlue) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorpBlue, unfocusedBorderColor = Color.LightGray)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = CorpBlue) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorpBlue, unfocusedBorderColor = Color.LightGray)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { /* Forgot pass */ }) {
                            Text("Forgot Password?", color = CorpBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Please fill all fields") }
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.login(LoginRequest(email, password))
                                    handleJsonResponse(
                                        response = response,
                                        onSuccess = {
                                            val body = response.body()!!.asJsonObject
                                            val userId = body.get("user_id").asInt
                                            val name = body.get("name").asString
                                            val role = body.get("role")?.asString ?: (if (selectedTab == 1) "admin" else "employee")
                                            val position = body.get("position")?.asString ?: ""
                                            val deptId = body.get("department_id")?.asInt ?: 0
                                            val deptName = body.get("department_name")?.asString ?: ""
                                            val token = body.get("token")?.asString
                                            
                                            sessionManager.saveSession(userId, name, role, deptId, token, position, deptName)
                                            onLoginSuccess(userId, name, role, deptId, position, deptName)
                                        },
                                        onError = { error ->
                                            val msg = try {
                                                val json = com.google.gson.JsonParser.parseString(error).asJsonObject
                                                json.get("error")?.asString ?: error
                                            } catch (e: Exception) { error }
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                } catch (e: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("Connection error") }
                                } finally { isLoading = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CorpBlue),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("LOGIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text("FRIENDS Software Solutions", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
