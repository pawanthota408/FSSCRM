package com.fsscrm.network

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Global tracker for app visibility (foreground vs background)
 * This helps FCM decide whether to show a system notification or just log/update UI.
 */
object AppVisibility : DefaultLifecycleObserver {

    private var isAppInForeground = false

    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
    }

    fun isForeground(): Boolean = isAppInForeground
}
