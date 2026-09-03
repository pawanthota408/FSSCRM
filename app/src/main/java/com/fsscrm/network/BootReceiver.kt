package com.fsscrm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || 
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Device rebooted or app updated - waking up for call detection")
            
            // Just waking up the process is often enough to keep the manifest receiver active.
            // Some devices need a foreground service to be started to really "stick".
        }
    }
}
