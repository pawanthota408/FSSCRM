package com.fsscrm.network

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Worker used as a trampoline to start the CallOverlayService from background.
 * This is more reliable on some devices when the app is swiped away.
 */
class OverlayStarterWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val number = inputData.getString("phone_number") ?: "Unknown"
        val type = inputData.getString("call_type") ?: "Post Call"
        
        Log.d("OverlayStarterWorker", "Starting overlay from Worker for $number")
        
        val serviceIntent = Intent(applicationContext, CallOverlayService::class.java).apply {
            putExtra(CallReceiver.EXTRA_PHONE, number)
            putExtra(CallReceiver.EXTRA_TYPE, type)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("OverlayStarterWorker", "Failed to start service from worker", e)
            return Result.failure()
        }

        return Result.success()
    }
}
