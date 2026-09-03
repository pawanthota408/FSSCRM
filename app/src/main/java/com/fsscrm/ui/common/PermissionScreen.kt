package com.fsscrm.ui.common

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun PermissionScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    
    val phonePermissions = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )
    
    val mediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else emptyList()

    var isPhoneGranted by remember { 
        mutableStateOf(phonePermissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        })
    }
    
    var isMediaGranted by remember {
        mutableStateOf(mediaPermissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        })
    }

    var isNotificationGranted by remember {
        mutableStateOf(notificationPermission.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager }
    var isBatteryOptimized by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Update status when returning to app
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        isOverlayGranted = Settings.canDrawOverlays(context)
        isBatteryOptimized = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        isPhoneGranted = phonePermissions.all { results[it] == true }
        isMediaGranted = mediaPermissions.all { results[it] == true }
        isNotificationGranted = notificationPermission.all { results[it] == true }
        
        if (isPhoneGranted && isMediaGranted && isNotificationGranted && isOverlayGranted) {
            onAllGranted()
        }
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = { 
                    if (isPhoneGranted && isMediaGranted && isNotificationGranted && isOverlayGranted && isBatteryOptimized) {
                        onAllGranted()
                    } else if (!isOverlayGranted) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else if (!isBatteryOptimized) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        launcher.launch((phonePermissions + mediaPermissions + notificationPermission).toTypedArray())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val btnText = when {
                        isPhoneGranted && isMediaGranted && isNotificationGranted && !isOverlayGranted -> "Enable Overlay"
                        isPhoneGranted && isMediaGranted && isNotificationGranted && isOverlayGranted && !isBatteryOptimized -> "Ignore Optimization"
                        else -> "Allow All & Continue"
                    }
                    Text(
                        text = btnText,
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "Permissions Required",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            
            Text(
                "Grant these permissions to ensure the CRM works smoothly.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PermissionCard(
                title = "Call Tracking",
                description = "Lets FSSCrm identify leads during calls and log details automatically.",
                icon = Icons.Default.Call,
                isGranted = isPhoneGranted,
                onAllow = { launcher.launch(phonePermissions.toTypedArray()) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PermissionCard(
                title = "Files, Photos & Music",
                description = "Required to access and upload documents, photos or call recordings.",
                icon = Icons.Default.Folder,
                isGranted = isMediaGranted,
                onAllow = { launcher.launch(mediaPermissions.toTypedArray()) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                title = "Notifications",
                description = "Receive alerts for new leads, tasks, and follow-ups in real-time.",
                icon = Icons.Default.Notifications,
                isGranted = isNotificationGranted,
                onAllow = { launcher.launch(notificationPermission.toTypedArray()) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                title = "Display Over Apps",
                description = "Shows the Call Assistant bubble on top of your call screen.",
                icon = Icons.Default.WebAsset,
                isGranted = isOverlayGranted,
                onAllow = { 
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                title = "Background Running",
                description = "Crucial! Ensures the overlay works even when the app is closed. Also enable 'Auto-start' in your phone settings if available.",
                icon = Icons.Default.Check,
                isGranted = isBatteryOptimized,
                onAllow = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onAllow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp, 
                if (isGranted) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF0F172A).copy(alpha = 0.05f), 
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFFF0FDF4) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = if (isGranted) Color(0xFF10B981) else Color(0xFF475569), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                }
                
                if (isGranted) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                } else {
                    Button(
                        onClick = onAllow,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Grant", fontSize = 11.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(description, fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 18.sp)
            
            if (!isGranted) {
                Text(
                    "Read Disclosure", 
                    color = Color(0xFF3B82F6), 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp).clickable { }
                )
            }
        }
    }
}
