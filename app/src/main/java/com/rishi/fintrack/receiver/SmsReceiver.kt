package com.rishi.fintrack.receiver

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.rishi.fintrack.utils.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.BroadcastReceiver

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("FINTRACK_PRO", "📡 System broadcast received: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

            // 🌟 Tells Android OS to keep app awake until processing is done
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                    // 🌟 NEW: Current User ki ID nikalna taaki sahi account me data jaye
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    if (!smsMessages.isNullOrEmpty() && currentUserId.isNotEmpty()) {
                        for (message in smsMessages) {
                            val sender = message.displayOriginatingAddress ?: "Unknown"
                            val body = message.displayMessageBody ?: ""

                            if (body.isNotBlank()) {
                                Log.d("FINTRACK_PRO", "📩 SMS Captured from: $sender | Body: $body")

                                // 🚀 Sending data + UserId to our Universal Parser
                                SmsParser.parseSms(context, sender, body, currentUserId)
                            }
                        }
                    } else if (currentUserId.isEmpty()) {
                        Log.e("FINTRACK_PRO", "⚠️ No user logged in. SMS ignored.")
                    }
                } catch (e: Exception) {
                    Log.e("FINTRACK_PRO", "❌ Error processing SMS in Receiver: ${e.message}")
                } finally {
                    // 🌟 MUST DO: System ko batao ki kaam ho gaya
                    pendingResult.finish()
                    Log.d("FINTRACK_PRO", "✅ Background task completed safely.")
                }
            }
        }
    }
}