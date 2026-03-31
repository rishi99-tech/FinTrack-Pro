package com.rishi.fintrack

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.rishi.fintrack.receiver.SmsReceiver
import com.rishi.fintrack.ui.screens.LoginScreen
import com.rishi.fintrack.ui.screens.MainDashboard
import com.rishi.fintrack.ui.screens.RegisterScreen
import com.rishi.fintrack.ui.screens.NewPasswordScreen
import com.rishi.fintrack.ui.screens.OnboardingScreen
import com.rishi.fintrack.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {

    private val smsReceiver = SmsReceiver()

    // 🌟 THE CHECKER: Pata lagata hai ki OS ne Notification power di hai ya nahi
    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat!!.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        intentFilter.priority = 2147483647

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(smsReceiver, intentFilter)
        }

        // 🌟 THE INTERCEPTOR: Deep Link Check karna app start hote hi
        var initialDeepLinkScreen = ""
        var deepLinkOobCode = ""

        val uri = intent?.data
        if (uri != null && uri.path?.contains("/__/auth/action") == true) {
            val mode = uri.getQueryParameter("mode")
            val oobCode = uri.getQueryParameter("oobCode")

            // Agar link reset password ka hai aur code mila hai
            if (mode == "resetPassword" && !oobCode.isNullOrEmpty()) {
                initialDeepLinkScreen = "new_password"
                deepLinkOobCode = oobCode
            }
        }

        setContent {
            val DeepSpace = Color(0xFF0F1115)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DeepSpace
            ) {
                // 🌟 NAYA: NOTIFICATION PERMISSION STATE
                var showNotificationDialog by remember { mutableStateOf(!isNotificationServiceEnabled()) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
                    val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
                }

                LaunchedEffect(Unit) {
                    val hasReadSms = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                    val hasReceiveSms = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

                    if (!hasReadSms || !hasReceiveSms) {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                        )
                    }
                }

                val auth = FirebaseAuth.getInstance()
                val sharedPreferences = getSharedPreferences("FinTrackPrefs", Context.MODE_PRIVATE)

                // 🌟 ViewModel ko upar laya gaya taaki AuthListener isko use kar sake bina error ke
                val viewModel: TransactionViewModel = viewModel()

                var currentScreen by remember {
                    mutableStateOf(
                        if (initialDeepLinkScreen.isNotEmpty()) {
                            initialDeepLinkScreen
                        } else if (auth.currentUser != null) {
                            val isOnboardingCompleted = sharedPreferences.getBoolean("isOnboardingCompleted", false)
                            if (isOnboardingCompleted) "dashboard" else "onboarding"
                        } else {
                            "login"
                        }
                    )
                }

                // 🌟 FIX 1: Alert STRICTLY tabhi dikhega jab user "dashboard" screen par hoga
                if (showNotificationDialog && currentScreen == "dashboard") {
                    AlertDialog(
                        onDismissRequest = { /* Bahar click karne se band nahi hoga, user ko decision lena padega */ },
                        title = { Text("Permission Required") },
                        text = { Text("FinTrack Pro requires Notification Access to securely monitor and sync your UPI transactions in the background. This ensures your expense dashboard remains accurate without requiring manual data entry.") },
                        confirmButton = {
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                startActivity(intent)
                                showNotificationDialog = false
                            }) {
                                Text("Allow Access")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationDialog = false }) {
                                Text("Later")
                            }
                        }
                    )
                }

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        if (currentScreen != "new_password") {
                            if (firebaseAuth.currentUser == null) {
                                currentScreen = "login"
                            } else {
                                // 🌟 FIX 2: Agar user pehle se logged in hai (ya re-install kiya hai), toh data turant wapas laao
                                viewModel.restoreDataFromCloud()

                                val isDone = sharedPreferences.getBoolean("isOnboardingCompleted", false)
                                if (currentScreen != "onboarding") {
                                    currentScreen = if (isDone) "dashboard" else "onboarding"
                                }
                            }
                        }
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = {
                            // 🌟 FIX 3: Naya login hone par data wapas restore karo
                            viewModel.restoreDataFromCloud()

                            val isDone = sharedPreferences.getBoolean("isOnboardingCompleted", false)
                            currentScreen = if (isDone) "dashboard" else "onboarding"
                        },
                        onNavigateToRegister = { currentScreen = "register" }
                    )
                    "register" -> RegisterScreen(
                        onRegisterSuccess = {
                            currentScreen = "onboarding"
                        },
                        onNavigateToLogin = { currentScreen = "login" }
                    )

                    "onboarding" -> OnboardingScreen(
                        onFinish = {
                            sharedPreferences.edit().putBoolean("isOnboardingCompleted", true).apply()
                            currentScreen = "dashboard"
                            showNotificationDialog = false
                        }
                    )

                    "dashboard" -> MainDashboard(viewModel = viewModel)

                    "new_password" -> NewPasswordScreen(
                        oobCode = deepLinkOobCode,
                        onPasswordResetSuccess = { currentScreen = "login" }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(smsReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}