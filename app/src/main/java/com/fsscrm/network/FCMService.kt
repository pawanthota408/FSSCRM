package com.fsscrm.network

import android.util.Log
import com.fsscrm.ui.common.showLocalNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "onNewToken: Token refreshed -> $token")
        
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.getUserId()
        if (userId != 0) {
            syncTokenToServer(userId.toString(), token)
        } else {
            Log.d("FCM", "onNewToken: User not logged in, skipping sync.")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "onMessageReceived: From=${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = data["title"] ?: notification?.title ?: "FSS CRM"
        val message = data["message"] ?: data["body"] ?: notification?.body ?: ""
        val type = data["type"]
        val navigateTo = data["navigate_to"] ?: mapTypeToRoute(type)
        val extraId = data["extra_id"] ?: data["related_id"]

        Log.d("FCM", "onMessageReceived: Payload title=$title, navigateTo=$navigateTo, extraId=$extraId")

        if (AppVisibility.isForeground()) {
            Log.d("FCM", "onMessageReceived: App is in foreground, suppressing notification.")
            return
        }

        if (message.isNotEmpty()) {
            showLocalNotification(
                context = applicationContext,
                title = title,
                message = message,
                navigateTo = navigateTo,
                extraId = extraId
            )
        }
    }

    private fun mapTypeToRoute(type: String?): String? {
        return when (type?.lowercase()) {
            "lead" -> "lead_details"
            "task" -> "tasks"
            "leave" -> "leaves"
            "activity" -> "activities"
            "attendance" -> "attendance"
            "work" -> "work_details"
            "announcement" -> "announcements"
            "quote", "proforma", "payment", "ticket" -> "home"
            else -> null
        }
    }

    private fun syncTokenToServer(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FCM", "Request: syncTokenToServer (onNewToken) for userId=$userId")
                val response = RetrofitClient.apiService.updateFcmToken(
                    mapOf(
                        "user_id" to userId,
                        "fcm_token" to token
                    )
                )
                if (response.isSuccessful) {
                    Log.d("FCM", "Response Success (onNewToken): ${response.body()}")
                } else {
                    Log.e("FCM", "Response Error (onNewToken): HTTP ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Network Error (onNewToken): ${e.message}")
            }
        }
    }
}
