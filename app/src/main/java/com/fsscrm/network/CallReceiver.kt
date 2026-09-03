package com.fsscrm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Listens for phone state + outgoing calls and starts CallOverlayService.
 * Requires: READ_PHONE_STATE, READ_CALL_LOG, SYSTEM_ALERT_WINDOW, receiver in Manifest.
 */
class     CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        Log.d(TAG, "Action received: $action")

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        when (action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED,
            "android.intent.action.PHONE_STATE" -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                var number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                Log.d(TAG, "Phone state=$state number=$number")

                // Keep last known number (Android 10+ often sends null on later states)
                if (!number.isNullOrBlank()) {
                    prefs.edit().putString(KEY_LAST_NUMBER, number).apply()
                } else {
                    number = prefs.getString(KEY_LAST_NUMBER, null)
                }

                // Skip exact duplicate state+number within a short window
                val lastState = prefs.getString(KEY_LAST_STATE, "")
                val lastAt = prefs.getLong(KEY_LAST_STATE_AT, 0L)
                val now = System.currentTimeMillis()
                if (state == lastState && now - lastAt < 800L) {
                    Log.d(TAG, "Duplicate state ignored: $state")
                    return
                }
                prefs.edit()
                    .putString(KEY_LAST_STATE, state)
                    .putLong(KEY_LAST_STATE_AT, now)
                    .apply()

                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        Log.d(TAG, "RINGING: $number")
                        if (!number.isNullOrBlank()) {
                            startOverlay(context, number, "Incoming Call")
                        }
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        Log.d(TAG, "OFFHOOK: $number")
                        if (!number.isNullOrBlank()) {
                            // Could be answered incoming or active outgoing
                            val type = if (prefs.getBoolean(KEY_WAS_RINGING, false)) {
                                "Active Call"
                            } else {
                                "Active Call"
                            }
                            startOverlay(context, number, type)
                        }
                        prefs.edit().putBoolean(KEY_WAS_RINGING, false).apply()
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        Log.d(TAG, "IDLE (ended): $number")
                        prefs.edit().putBoolean(KEY_WAS_RINGING, false).apply()

                        // Sync call log to server - DISABLED as per user request
                        /*
                        try {
                            val work = OneTimeWorkRequestBuilder<CallSyncWorker>().build()
                            WorkManager.getInstance(context).enqueue(work)
                        } catch (e: Exception) {
                            Log.e(TAG, "WorkManager enqueue failed", e)
                        }
                        */

                        if (!number.isNullOrBlank()) {
                            startOverlay(context, number, "Post Call")
                        }

                        // Clear number after a short delay so next call is clean
                        Handler(Looper.getMainLooper()).postDelayed({
                            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            if (p.getString(KEY_LAST_STATE, "") == TelephonyManager.EXTRA_STATE_IDLE) {
                                p.edit().remove(KEY_LAST_NUMBER).apply()
                            }
                        }, 15_000L)
                    }
                }

                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    prefs.edit().putBoolean(KEY_WAS_RINGING, true).apply()
                }
            }

            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val outgoing = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                Log.d(TAG, "Outgoing: $outgoing")
                if (!outgoing.isNullOrBlank()) {
                    prefs.edit()
                        .putString(KEY_LAST_NUMBER, outgoing)
                        .putBoolean(KEY_WAS_RINGING, false)
                        .apply()
                    startOverlay(context, outgoing, "Outgoing Call")
                }
            }
        }
    }

    private fun startOverlay(context: Context, number: String, type: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, true)
        
        if (!isEnabled) {
            Log.d(TAG, "Overlay disabled by user preference")
            return
        }

        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission NOT granted")
            return
        }

        Log.d(TAG, "Starting CallOverlayService number=$number type=$type")
        val serviceIntent = Intent(context, CallOverlayService::class.java).apply {
            putExtra(EXTRA_PHONE, number)
            putExtra(EXTRA_TYPE, type)
            // Add flag to help when starting from background
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service directly, using WorkManager fallback: ${e.message}")
            // Fallback: Use WorkManager to start the service
            // Note: WorkManager can start a foreground service even when the app is in background
            // if it's within a few minutes of a broadcast.
            try {
                val data = workDataOf(
                    "phone_number" to number,
                    "call_type" to type
                )
                val request = OneTimeWorkRequestBuilder<OverlayStarterWorker>()
                    .setInputData(data)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context).enqueue(request)
            } catch (e2: Exception) {
                Log.e(TAG, "WorkManager fallback also failed: ${e2.message}")
            }
        }
    }

    companion object {
        private const val TAG = "CallReceiver"
        private const val PREFS = "call_prefs"
        private const val KEY_LAST_NUMBER = "last_number"
        private const val KEY_LAST_STATE = "last_state"
        private const val KEY_LAST_STATE_AT = "last_state_at"
        private const val KEY_WAS_RINGING = "was_ringing"
        const val KEY_OVERLAY_ENABLED = "overlay_enabled"

        const val EXTRA_PHONE = "phone_number"
        const val EXTRA_TYPE = "call_type"
    }
}
