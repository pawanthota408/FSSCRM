package com.fsscrm.ui.common

import android.content.Intent
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.CallReceiver
import com.fsscrm.network.UpdateResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit, onBack: () -> Unit, onBusinessDetailsClick: () -> Unit) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isChecking by remember { mutableStateOf(false) }
    var updateData by remember { mutableStateOf<UpdateResponse?>(null) }
    val context = LocalContext.current

    if (updateData != null) {
        AlertDialog(
            onDismissRequest = { updateData = null },
            title = { Text("New Update Available", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Version: v${updateData!!.latest_version}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(updateData!!.release_notes ?: "A new version of the app is available.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val url = updateData!!.update_url
                    if (!url.isNullOrBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        context.startActivity(intent)
                    }
                    updateData = null
                }) {
                    Text("Update Now", fontSize = 14.sp)
                }
            },
            dismissButton = {
                if (updateData?.force_update != true) {
                    TextButton(onClick = { updateData = null }) {
                        Text("Later", fontSize = 14.sp)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            UniversalHeader(
                title = "Settings",
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(Color.White)
        ) {
            SettingsCategoryHeader("General")
            
            SettingsToggleRow(
                title = "Dark Theme",
                subtitle = "Enable dark mode for the app",
                checked = isDarkTheme,
                onCheckedChange = { onThemeChange(it) }
            )

            SettingsToggleRow(
                title = "Notifications",
                subtitle = "Get alerts for tasks, leads, and announcements",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            val callPrefs = context.getSharedPreferences("call_prefs", android.content.Context.MODE_PRIVATE)
            var overlayEnabled by remember { mutableStateOf(callPrefs.getBoolean(CallReceiver.KEY_OVERLAY_ENABLED, true)) }

            SettingsToggleRow(
                title = "Call Overlay",
                subtitle = "Show assistant during/after calls",
                checked = overlayEnabled,
                onCheckedChange = { 
                    overlayEnabled = it
                    callPrefs.edit { putBoolean(CallReceiver.KEY_OVERLAY_ENABLED, it) }
                }
            )

            SettingsCategoryHeader("Business")
            
            SettingsActionRow(
                title = "Logo & Business Details",
                subtitle = "Your business name, contact & logo",
                icon = Icons.Default.Business,
                onClick = onBusinessDetailsClick
            )

            SettingsCategoryHeader("App Info")

            SettingsActionRow(
                title = "About App",
                subtitle = "Version and company information",
                icon = Icons.Default.Info,
                onClick = { }
            )
            
            SettingsActionRow(
                title = "Check for Updates",
                subtitle = if (isChecking) "Checking..." else "Look for new versions",
                icon = Icons.Default.Update,
                onClick = {
                    if (!isChecking) {
                        isChecking = true
                        scope.launch {
                            try {
                                val response = com.fsscrm.network.RetrofitClient.apiService.checkUpdate()
                                if (response.isSuccessful) {
                                    response.toLenientJson()?.let {
                                        val update = Gson().fromJson(it, UpdateResponse::class.java)
                                        val currentVersionCode = 104
                                        if ((update.version_code ?: 0) > currentVersionCode) {
                                            updateData = update
                                        } else {
                                            snackbarHostState.showSnackbar("App is up to date")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Check failed")
                            } finally {
                                isChecking = false
                            }
                        }
                    }
                }
            )
            
            if (isChecking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = Color(0xFF7C4DFF))
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "Version 1.0.4 (BETA)",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 11.sp,
                color = Color.LightGray
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF7C4DFF),
        letterSpacing = 1.sp
    )
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF7C4DFF),
                    uncheckedTrackColor = Color(0xFFEEEEEE),
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.scale(0.75f)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF5F5F5))
    }
}

@Composable
fun SettingsActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(Color(0xFFF3F0FF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF5F5F5))
    }
}
