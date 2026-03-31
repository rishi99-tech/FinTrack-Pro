package com.rishi.fintrack.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.rishi.fintrack.utils.SmsParser
import com.rishi.fintrack.utils.NotificationParser // 🌟 NAYA: Smart Parser Import Kiya
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReaderService : NotificationListenerService() {

    // 🌟 THE RADAR: Sirf in apps ke notification padhenge taaki WhatsApp/Insta ignore ho jaye
    private val targetApps = listOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "com.cred.club",                          // Cred
        "in.amazon.mShop.android.shopping"        // Amazon Pay
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // Agar notification GPay/PhonePe ka hai, ya kisi Bank App/SMS ka hai
        if (targetApps.contains(packageName) || packageName.contains("bank", ignoreCase = true) || packageName.contains("messaging", ignoreCase = true)) {

            Log.d("FINTRACK_PRO", "🔔 Radar Caught: $packageName | Title: $title | Text: $text")

            // Filter: Sirf wahi text process karo jisme paise ki baat ho
            val lowerText = text.lowercase()
            if (lowerText.contains("rs") || lowerText.contains("inr") || lowerText.contains("₹") ||
                lowerText.contains("debited") || lowerText.contains("credited") || lowerText.contains("paid")) {

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 🌟 FIX: Current User ki ID nikal kar Parser ko bhej rahe hain
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                        if (currentUserId.isNotEmpty()) {

                            // 🌟 SMART ROUTER: Faisla karo kis parser ko bhejna hai
                            if (packageName.contains("messaging", ignoreCase = true) || packageName.contains("sms", ignoreCase = true)) {
                                // Humne wahi order rakha hai jo SmsParser maang raha hai
                                Log.d("FINTRACK_PRO", "Routing to SMS Parser...")
                                SmsParser.parseSms(applicationContext, title, text, currentUserId)
                            } else {
                                // Agar GPay, PhonePe ya Bank app hai, toh naya lightning parser use hoga
                                Log.d("FINTRACK_PRO", "Routing to Notification Parser...")
                                NotificationParser.parseNotification(applicationContext, packageName, title, text, currentUserId)
                            }

                        } else {
                            Log.e("FINTRACK_PRO", "⚠️ Notification ignored: User not logged in.")
                        }
                    } catch (e: Exception) {
                        Log.e("FINTRACK_PRO", "❌ Error passing notification to Parser: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("FINTRACK_PRO", "✅ THE UNKILLABLE ENGINE STARTED: Notification Listener Active!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("FINTRACK_PRO", "⚠️ Engine Disconnected.")
    }
}