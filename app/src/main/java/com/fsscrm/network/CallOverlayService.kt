package com.fsscrm.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fsscrm.ui.common.toLenientJson
import com.fsscrm.ui.theme.PrimaryIndigo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra(CallReceiver.EXTRA_PHONE)
            ?: intent?.getStringExtra("phone_number")
            ?: "Unknown"
        val callType = intent?.getStringExtra(CallReceiver.EXTRA_TYPE)
            ?: intent?.getStringExtra("call_type")
            ?: "Post Call"

        Log.d(TAG, "onStartCommand: $phoneNumber ($callType)")

        // MUST start foreground immediately (Android 8+)
        startAsForeground(phoneNumber, callType)

        try {
            showOverlay(phoneNumber, callType)
        } catch (e: Exception) {
            Log.e(TAG, "showOverlay failed: ${e.message}", e)
            stopSafely()
        }

        return START_NOT_STICKY
    }

    private fun startAsForeground(phoneNumber: String, callType: String) {
        val text = if (callType == "Post Call") {
            "Call ended with $phoneNumber"
        } else {
            "In call with $phoneNumber"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FSS CRM Call Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground phoneCall type failed: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "startForeground fallback failed: ${e2.message}", e2)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Overlay Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Caller info during and after calls"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showOverlay(phoneNumber: String, callType: String) {
        removeOverlay()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Post Call needs touch + keyboard for note field
        val isPostCall = callType == "Post Call"
        val flags = if (isPostCall) {
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isPostCall) Gravity.BOTTOM else Gravity.CENTER
            windowAnimations = android.R.style.Animation_InputMethod // Slide up/down
        }

        val owner = OverlayLifecycleOwner().also {
            it.performRestore(null)
            it.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            it.handleLifecycleEvent(Lifecycle.Event.ON_START)
            it.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner = owner

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                MaterialTheme {
                    CallOverlayContent(
                        phoneNumber = phoneNumber,
                        callType = callType,
                        onDismiss = { stopSafely() }
                    )
                }
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay added ($callType)")
        } catch (e: Exception) {
            Log.e(TAG, "addView failed (overlay permission?): ${e.message}", e)
            stopSafely()
        }
    }

    private fun stopSafely() {
        removeOverlay()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        stopSelf()
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeViewImmediate(view)
            } catch (e: Exception) {
                Log.e(TAG, "removeView: ${e.message}")
            }
            overlayView = null
        }
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    /** Minimal lifecycle owner so ComposeView works outside Activity */
    class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun handleLifecycleEvent(event: Lifecycle.Event) =
            lifecycleRegistry.handleLifecycleEvent(event)

        fun performRestore(state: android.os.Bundle?) =
            savedStateRegistryController.performRestore(state)
    }

    companion object {
        private const val TAG = "CallOverlayService"
        private const val CHANNEL_ID = "CallOverlayChannel"
        private const val NOTIFICATION_ID = 1234
    }
}

@Composable
fun CallOverlayContent(
    phoneNumber: String,
    callType: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var identifiedLead by remember { mutableStateOf<Lead?>(null) }
    var identifiedCustomer by remember { mutableStateOf<Customer?>(null) }
    var leadDetails by remember { mutableStateOf<LeadDetailsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var hasActiveWork by remember { mutableStateOf(false) }
    
    // Tracking identification data for logging
    val identificationData = remember(identifiedLead, identifiedCustomer) {
        val callerName = identifiedLead?.name ?: identifiedCustomer?.name ?: "Unknown"
        val callerEmail = identifiedLead?.email ?: identifiedCustomer?.email ?: ""
        val callerLeadId = identifiedLead?.id ?: identifiedCustomer?.lead_id ?: 0
        val callerCustId = identifiedCustomer?.id ?: 0
        mapOf(
            "name" to callerName,
            "email" to callerEmail,
            "lead_id" to callerLeadId.toString(),
            "customer_id" to callerCustId.toString()
        )
    }

    LaunchedEffect(phoneNumber) {
        loading = true
        if (phoneNumber.isNotBlank() && phoneNumber != "Unknown") {
            try {
                val userId = SessionManager(context).getUserId()
                if (userId != 0) {
                    // 1. Identification logic
                    fun norm(p: String?) = p?.replace(Regex("[^0-9]"), "")?.takeLast(10).orEmpty()
                    val target = norm(phoneNumber)

                    val leadsResponse = RetrofitClient.apiService.getLeads(mapOf("user_id" to userId))
                    if (leadsResponse.isSuccessful) {
                        leadsResponse.toLenientJson()?.let { json ->
                            val leadsList = LeadResponse.fromJson(json).leads
                            identifiedLead = leadsList.find {
                                val n = norm(it.phone)
                                n.isNotEmpty() && (n == target || n.contains(target) || target.contains(n))
                            }
                        }
                    }

                    if (identifiedLead == null) {
                        val custResponse = RetrofitClient.apiService.getCustomers(
                            mapOf("user_id" to userId.toString(), "query" to phoneNumber)
                        )
                        if (custResponse.isSuccessful) {
                            custResponse.toLenientJson()?.let { json ->
                                val customersList = AdminCustomersResponse.fromJson(json).customers ?: emptyList()
                                identifiedCustomer = customersList.find {
                                    val n = norm(it.phone)
                                    n.isNotEmpty() && (n == target || n.contains(target) || target.contains(n))
                                }
                            }
                        }
                    }

                    val currentLead = identifiedLead
                    val currentCustomer = identifiedCustomer

                    // Fetch details to check for active works
                    val leadIdToFetch = currentLead?.id ?: currentCustomer?.lead_id ?: 0
                    if (leadIdToFetch != 0) {
                        val detailsResp = RetrofitClient.apiService.getLeadDetails(
                            mapOf("lead_id" to leadIdToFetch)
                        )
                        if (detailsResp.isSuccessful) {
                            detailsResp.toLenientJson()?.let {
                                val parsed = LeadDetailsResponse.fromJson(it)
                                leadDetails = parsed
                                // Check for active work (status not 'completed')
                                hasActiveWork = parsed.works.any { w -> 
                                    w.status.lowercase() != "completed" && w.status.lowercase() != "cancelled"
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CallOverlayContent", "Sync/Log error: ${e.message}", e)
            }
        }
        loading = false
    }

    if (callType == "Post Call") {
        PostCallLayout(
            phoneNumber = phoneNumber,
            lead = identifiedLead,
            customer = identifiedCustomer,
            details = leadDetails,
            loading = loading,
            hasActiveWork = hasActiveWork,
            identificationData = identificationData,
            onDismiss = onDismiss
        )
    } else {

        ActiveCallLayout(
            phoneNumber = phoneNumber,
            lead = identifiedLead,
            customer = identifiedCustomer,
            hasActiveWork = hasActiveWork,
            type = callType,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ActiveCallLayout(
    phoneNumber: String,
    lead: Lead?,
    customer: Customer?,
    hasActiveWork: Boolean = false,
    type: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val name = lead?.name ?: customer?.name ?: "Unknown"
    val company = lead?.company_name ?: customer?.company ?: "Not in CRM"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .padding(12.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PhoneCallback,
                    null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = type,
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(text = phoneNumber, fontSize = 13.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Company: $company",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    if (lead != null || customer != null) {
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = lead?.status ?: "Customer",
                                modifier = Modifier.padding(8.dp, 4.dp),
                                color = PrimaryIndigo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Button 1: Call (if exists) or Add Lead (if unknown)
                    if (lead != null || customer != null) {
                        ActionButton(Icons.Default.Call, "Call", Color(0xFF0EA5E9), size = 38.dp) {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply { 
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK 
                                }
                            )
                        }
                    } else {
                        ActionButton(Icons.Default.PersonAdd, "Add Lead", Color(0xFF10B981), size = 38.dp) {
                            val intent = Intent(context, Class.forName("com.fsscrm.MainActivity")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("navigate_to", "create_lead")
                                putExtra("extra_id", "phone=$phoneNumber")
                            }
                            context.startActivity(intent)
                            onDismiss()
                        }
                    }

                    // Message
                    ActionButton(Icons.Default.Email, "Message", Color(0xFF6366F1), size = 38.dp) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phoneNumber")).apply { 
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK 
                            }
                        )
                    }

                    // WhatsApp
                    ActionButton(Icons.AutoMirrored.Filled.Chat, "WhatsApp", Color(0xFF22C55E), size = 38.dp) {
                        val clean = phoneNumber.replace(Regex("[^0-9]"), "")
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clean")).apply { 
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK 
                            }
                        )
                    }

                    // Quotation (ALWAYS visible now)
                    ActionButton(Icons.AutoMirrored.Filled.Assignment, "Quotation", Color(0xFFF59E0B), size = 38.dp) {
                        val intent = Intent(context, Class.forName("com.fsscrm.MainActivity")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            val id = lead?.id ?: customer?.lead_id
                            if (id != null && id != 0) {
                                putExtra("navigate_to", "lead_details")
                                putExtra("extra_id", "$id?open_quotation=true")
                            } else {
                                // Unknown number: Go to Create Lead with Quotation flag
                                putExtra("navigate_to", "create_lead")
                                putExtra("extra_id", "phone=$phoneNumber&open_quotation=true")
                            }
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }

                    // Follow Up
                    ActionButton(Icons.Default.Schedule, "Follow Up", PrimaryIndigo, size = 38.dp) {
                        val intent = Intent(context, Class.forName("com.fsscrm.MainActivity")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            val leadId = lead?.id ?: customer?.lead_id
                            if (leadId != null && leadId != 0) {
                                // Redirection Logic: If active work -> Lead Profile. Else -> Customer Profile.
                                if (hasActiveWork) {
                                    putExtra("navigate_to", "lead_details")
                                    putExtra("extra_id", "$leadId?open_followup=true")
                                } else if (customer != null) {
                                    putExtra("navigate_to", "customer_details")
                                    putExtra("extra_id", customer.id.toString())
                                } else {
                                    putExtra("navigate_to", "lead_details")
                                    putExtra("extra_id", "$leadId?open_followup=true")
                                }
                            } else {
                                // For unknown, "Follow Up" button goes to Add Lead screen
                                putExtra("navigate_to", "create_lead")
                                putExtra("extra_id", "phone=$phoneNumber")
                            }
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
fun PostCallLayout(
    phoneNumber: String,
    lead: Lead?,
    customer: Customer?,
    details: LeadDetailsResponse?,
    loading: Boolean,
    hasActiveWork: Boolean = false,
    identificationData: Map<String, String>? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var callReason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var callTypeSelection by remember { mutableStateOf("Business") } // "Business" or "Personal"
    val name = lead?.name ?: customer?.name ?: "Unknown Caller"


    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
        color = Color.White,
        shadowElevation = 24.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
            // Drag Handle
            Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray, CircleShape).align(Alignment.CenterHorizontally).padding(bottom = 16.dp))
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(text = phoneNumber, fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color(0xFFF1F5F9), CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                }
            }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryIndigo)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // If lead/customer exists, Button 1 is Call. Else, Button 1 is Add Lead.
                if (lead != null || customer != null) {
                    ActionButton(Icons.Default.Call, "Call", Color(0xFF0EA5E9)) {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                } else {
                    ActionButton(Icons.Default.PersonAdd, "Add Lead", Color(0xFF10B981)) {
                        val intent = Intent(context, Class.forName("com.fsscrm.MainActivity")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("navigate_to", "create_lead")
                            val encodedNote = try { java.net.URLEncoder.encode(callReason, "UTF-8") } catch(e:Exception) { "" }
                            putExtra("extra_id", "phone=$phoneNumber&notes=$encodedNote")
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }
                }

                ActionButton(Icons.Default.Email, "Message", Color(0xFF6366F1)) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phoneNumber")).apply { 
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK 
                        }
                    )
                }

                ActionButton(Icons.AutoMirrored.Filled.Chat, "WhatsApp", Color(0xFF22C55E)) {
                    val clean = phoneNumber.replace(Regex("[^0-9]"), "")
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clean")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    )
                }

                // Quotation (ALWAYS visible now)
                ActionButton(Icons.AutoMirrored.Filled.Assignment, "Quotation", Color(0xFFF59E0B)) {
                    val intent = Intent(context, Class.forName("com.fsscrm.MainActivity")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        val id = lead?.id ?: customer?.lead_id
                        if (id != null && id != 0) {
                            putExtra("navigate_to", "lead_details")
                            putExtra("extra_id", "$id?open_quotation=true")
                        } else {
                            // Unknown number: Go to Create Lead with Quotation flag
                            putExtra("navigate_to", "create_lead")
                            val encodedNote = try { java.net.URLEncoder.encode(callReason, "UTF-8") } catch(e:Exception) { "" }
                            putExtra("extra_id", "phone=$phoneNumber&notes=$encodedNote&open_quotation=true")
                        }
                    }
                    context.startActivity(intent)
                    onDismiss()
                }

                // Follow Up
                ActionButton(Icons.Default.Schedule, "Follow Up", PrimaryIndigo) {
                    val intent = Intent(context, Class.forName("com.fsscrm.MainActivity")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        val leadId = lead?.id ?: customer?.lead_id
                        if (leadId != null && leadId != 0) {
                            // Redirection Logic: If active work -> Lead Profile. Else -> Customer Profile.
                            if (hasActiveWork) {
                                putExtra("navigate_to", "lead_details")
                                putExtra("extra_id", "$leadId?open_followup=true")
                            } else if (customer != null) {
                                putExtra("navigate_to", "customer_details")
                                putExtra("extra_id", customer.id.toString())
                            } else {
                                putExtra("navigate_to", "lead_details")
                                putExtra("extra_id", "$leadId?open_followup=true")
                            }
                        } else {
                            putExtra("navigate_to", "create_lead")
                            // Pass current note to Create Lead screen
                            val encodedNote = try { java.net.URLEncoder.encode(callReason, "UTF-8") } catch(e:Exception) { "" }
                            putExtra("extra_id", "phone=$phoneNumber&notes=$encodedNote")
                        }
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Is this a Business Call?",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).clickable { callTypeSelection = "Personal" },
                    color = if (callTypeSelection == "Personal") Color(0xFFF1F5F9) else Color.White,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, if (callTypeSelection == "Personal") PrimaryIndigo else Color(0xFFE2E8F0))
                ) {
                    Text("PERSONAL", modifier = Modifier.padding(12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold, color = if (callTypeSelection == "Personal") PrimaryIndigo else Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    modifier = Modifier.weight(1f).clickable { callTypeSelection = "Business" },
                    color = if (callTypeSelection == "Business") Color(0xFFF1F5F9) else Color.White,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, if (callTypeSelection == "Business") PrimaryIndigo else Color(0xFFE2E8F0))
                ) {
                    Text("BUSINESS", modifier = Modifier.padding(12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold, color = if (callTypeSelection == "Business") PrimaryIndigo else Color.Gray, fontSize = 12.sp)
                }
            }

            if (callTypeSelection == "Business") {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Add Quick Note",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )

                OutlinedTextField(
                    value = callReason,
                    onValueChange = { callReason = it },
                    placeholder = { Text("Summarize the call conversation...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }
            
            Button(
                onClick = {
                    if (callTypeSelection == "Personal") {
                        onDismiss()
                        return@Button
                    }

                    isSubmitting = true
                    scope.launch {
                        try {
                            val userId = SessionManager(context).getUserId()
                            if (userId != 0) {
                                // 1. Log Call History for Business Calls
                                val SDF_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val SDF_TIME = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                val now = Date()

                                val lId = (identificationData?.get("lead_id")?.takeIf { it != "0" } ?: lead?.id?.toString() ?: customer?.lead_id?.toString() ?: "0")
                                val cId = (identificationData?.get("customer_id")?.takeIf { it != "0" } ?: customer?.id?.toString() ?: "0")
                                val cName = (identificationData?.get("name")?.takeIf { it.isNotBlank() } ?: lead?.name ?: customer?.name ?: "Unknown")
                                val cEmail = (identificationData?.get("email")?.takeIf { it.isNotBlank() } ?: lead?.email ?: customer?.email ?: "")

                                val logParams = mutableMapOf(
                                    "employee_id" to userId.toString(),
                                    "mobile" to phoneNumber,
                                    "call_date" to SDF_DATE.format(now),
                                    "call_time" to SDF_TIME.format(now),
                                    "name" to cName,
                                    "email" to cEmail,
                                    "lead_id" to lId,
                                    "customer_id" to cId,
                                    "notes" to callReason
                                )

                                val logResp = RetrofitClient.apiService.logCallHistory(logParams)
                                if (logResp.isSuccessful && logResp.body() != null) {
                                    val element = logResp.body()!!
                                    if (element.isJsonObject) {
                                        val obj = element.asJsonObject
                                        val newCallId = obj.get("id")?.asString ?: obj.get("call_id")?.asString
                                        if (!newCallId.isNullOrBlank() && callReason.isNotBlank()) {
                                            try {
                                                RetrofitClient.apiService.updateCallNotes(
                                                    mapOf(
                                                        "call_id" to newCallId,
                                                        "id" to newCallId,
                                                        "notes" to callReason,
                                                        "user_id" to userId.toString()
                                                    )
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }

                                // 2. If lead exists, also add to CRM Follow-up timeline
                                if (lId != "0") {
                                    RetrofitClient.apiService.addFollowUp(
                                        mapOf(
                                            "lead_id" to lId,
                                            "user_id" to userId.toString(),
                                            "remarks" to "Call Note: $callReason",
                                            "status" to (lead?.status ?: "Follow Up"),
                                            "follow_up_date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        )
                                    )
                                }
                            }
                            onDismiss()
                        } catch (e: Exception) {
                            Log.e("PostCall", "Save note failed", e)
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                enabled = !isSubmitting && (callTypeSelection == "Personal" || callReason.isNotBlank()),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    val btnText = if (callTypeSelection == "Business") "SAVE BUSINESS LOG" else "DISMISS PERSONAL CALL"
                    Text(btnText, fontWeight = FontWeight.ExtraBold)
                }
            }

        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector, 
    label: String, 
    color: Color, 
    size: androidx.compose.ui.unit.Dp = 44.dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.45f))
            }
        }
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
    }
}
