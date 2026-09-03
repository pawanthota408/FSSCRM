package com.fsscrm.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * After a call ends, read the latest call log entry and POST to server.
 * Requires READ_CALL_LOG permission.
 */
class CallSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val userId = SessionManager(context).getUserId()
        if (userId == 0) {
            Log.w(TAG, "No userId — skip sync")
            return Result.success()
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "READ_CALL_LOG not granted — skip sync")
            return Result.success()
        }

        return try {
            // Let system write the call log first
            kotlinx.coroutines.delay(2500L)

            Log.d(TAG, "Querying call logs for userId=$userId")

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use { c ->
                if (!c.moveToFirst()) {
                    Log.d(TAG, "Call log empty")
                    return Result.success()
                }

                val number = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
                val duration = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.DURATION)) ?: "0"
                val type = c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                val date = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DATE))

                // Only last 5 minutes
                if (System.currentTimeMillis() - date > 5 * 60 * 1000L) {
                    Log.d(TAG, "Latest call too old — skip")
                    return Result.success()
                }

                val callTypeStr = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    CallLog.Calls.REJECTED_TYPE -> "Rejected"
                    else -> "Other"
                }

                Log.d(TAG, "Syncing: $number duration=$duration type=$callTypeStr")

                val response = RetrofitClient.apiService.syncCallLog(
                    mapOf(
                        "user_id" to userId.toString(),
                        "phone" to number,
                        "duration" to duration,
                        "call_type" to callTypeStr,
                        "timestamp" to date.toString()
                    )
                )
                Log.d(TAG, "syncCallLog response: ${response.code()} ${response.body()}")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing call log", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CallSyncWorker"
    }
}
