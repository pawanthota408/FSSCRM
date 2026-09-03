package com.fsscrm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.fsscrm.network.AppVisibility
import com.fsscrm.network.NotificationWorker
import com.fsscrm.network.RetrofitClient
import com.fsscrm.network.SessionManager
import com.fsscrm.ui.auth.LoginScreen
import com.fsscrm.ui.common.MainContainer
import com.fsscrm.ui.common.PermissionScreen
import com.fsscrm.ui.common.SplashScreen
import com.fsscrm.ui.theme.FSSCrmTheme
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Deep-link state for Compose navigation
    private var pendingNavigateTo by mutableStateOf<String?>(null)
    private var pendingExtraId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 0. Ensure Firebase is initialized
        try {
            FirebaseApp.initializeApp(this)
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            Log.d("FCM", "Firebase initialized and Auto-Init enabled")
        } catch (e: Exception) {
            Log.e("FCM", "Firebase initialization failed", e)
        }

        // 1. Initialize Global App Visibility Tracking
        AppVisibility.init()

        // 2. Setup Notification Infrastructure
        createNotificationChannel()
        scheduleNotificationWorker() // Background fallback
        
        // 3. Handle Notification Intent if app was launched from tray
        handleNotificationIntent(intent)

        // 4. Initial Token Sync if already logged in
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            syncFcmToken(sessionManager.getUserId())
        }

        setContent {
            val context = LocalContext.current
            val sessionManagerCompose = remember { SessionManager(context) }
            var isDarkTheme by remember { mutableStateOf(false) }
            var currentScreen by remember { mutableStateOf("splash") }
            var userId by remember { mutableIntStateOf(sessionManagerCompose.getUserId()) }

            // Permission Handling
            val criticalPermissions = mutableListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                criticalPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                criticalPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }

            var permissionsGranted by remember {
                mutableStateOf(criticalPermissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                } && Settings.canDrawOverlays(context))
            }

            FSSCrmTheme(darkTheme = isDarkTheme) {
                when {
                    !permissionsGranted -> {
                        PermissionScreen(onAllGranted = { permissionsGranted = true })
                    }
                    currentScreen == "splash" -> {
                        SplashScreen(onTimeout = {
                            currentScreen = if (sessionManagerCompose.isLoggedIn()) "main_container" else "login"
                        })
                    }
                    currentScreen == "login" -> {
                        LoginScreen(onLoginSuccess = { id, name, role, dept, pos, dName ->
                            Log.d("FCM", "Login flow: Successfully logged in. userId=$id, name=$name, role=$role, dept=$dept, pos=$pos, dName=$dName")
                            sessionManagerCompose.saveSession(id, name, role, dept, position = pos, deptName = dName)
                            userId = id
                            syncFcmToken(id) // Sync token immediately on login
                            currentScreen = "main_container"
                        })
                    }
                    currentScreen == "main_container" -> {
                        MainContainer(
                            userId = userId,
                            userRole = sessionManagerCompose.getUserRole(),
                            userPosition = sessionManagerCompose.getUserPosition(),
                            userDept = sessionManagerCompose.getUserDept(),
                            userDeptName = sessionManagerCompose.getUserDeptName(),
                            isDarkTheme = isDarkTheme,
                            onThemeChange = { isDarkTheme = it },
                            onLogout = {
                                sessionManagerCompose.clearSession()
                                currentScreen = "login"
                            },
                            initialNavigateTo = pendingNavigateTo,
                            initialExtraId = pendingExtraId,
                            onDeepLinkConsumed = {
                                pendingNavigateTo = null
                                pendingExtraId = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val nav = intent.getStringExtra("navigate_to")?.trim()?.takeIf { it.isNotEmpty() }
        val id = intent.getStringExtra("extra_id")?.trim()?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra("related_id")?.trim()?.takeIf { it.isNotEmpty() }
            
        if (nav != null || id != null) {
            Log.d("MainActivity", "Deep link received: nav=$nav, id=$id")
            pendingNavigateTo = nav
            pendingExtraId = id
            
            // Clear extras to prevent re-triggering on config changes
            intent.removeExtra("navigate_to")
            intent.removeExtra("extra_id")
            intent.removeExtra("related_id")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fsscrm_notifications",
                "FSS CRM Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for FSS CRM Lead and Task alerts"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, null)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun scheduleNotificationWorker() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "background_notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun syncFcmToken(userId: Int) {
        if (userId == 0) {
            Log.e("FCM", "Sync aborted: userId is 0. Check Login API response mapping.")
            return
        }

        // Check Play Services
        val gApi = GoogleApiAvailability.getInstance()
        val resultCode = gApi.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("FCM", "Google Play Services not available: ${gApi.getErrorString(resultCode)}")
            if (gApi.isUserResolvableError(resultCode)) {
                gApi.getErrorDialog(this, resultCode, 9000)?.show()
            }
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("FCM", "Fetching FCM registration token failed. Error: ${task.exception?.message}")
                task.exception?.printStackTrace()
                return@addOnCompleteListener
            }
            
            val token = task.result
            if (token.isNullOrEmpty()) {
                Log.e("FCM", "Token is null or empty even though task was successful")
                return@addOnCompleteListener
            }
            
            Log.d("FCM", "Request: syncFcmToken for userId=$userId")
            Log.d("FCM", "Token Value: $token")
            
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.updateFcmToken(
                        mapOf(
                            "user_id" to userId.toString(),
                            "fcm_token" to token
                        )
                    )
                    if (response.isSuccessful) {
                        Log.d("FCM", "Response Success: Token saved in database. ${response.body()}")
                    } else {
                        Log.e("FCM", "Response Error: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("FCM", "Network Error: ${e.message}")
                }
            }
        }
    }
}
