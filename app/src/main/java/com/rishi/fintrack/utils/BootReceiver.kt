package com.rishi.fintrack.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("FINTRACK_PRO", "🚀 System Rebooted: Waking up FinTrack Engine...")
            // NotificationListenerService ko Android system khud bind karta hai
            // jab app ka process memory me aa jata hai. Ye receiver process ko jagane ka kaam karta hai.
        }
    }
}