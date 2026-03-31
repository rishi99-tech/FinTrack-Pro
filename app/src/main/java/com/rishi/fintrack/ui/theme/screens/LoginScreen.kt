package com.rishi.fintrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter // 🌟 NEW IMPORT for Logo Fix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.rishi.fintrack.R

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    // 🌟 Success message dikhane ke liye
    var successMessage by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    // 🌟 PREMIUM THEME UPGRADE (NEO-BANKING DARK MODE 🌌)
    val AppPrimaryBlue = Color(0xFF2563EB)
    val CyanAccent = Color(0xFF00BCD4)
    val bgColor = Color(0xFF0F1115)
    val surfaceColor = Color(0xFF1C1F26)
    val textColor = Color.White
    val textMuted = Color(0xFF94A3B8)

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 🌟 LOGO FIX: Added ColorFilter.tint(Color.White) to force monochrome clean look
            Image(
                painter = painterResource(id = R.drawable.ui_logo),
                contentDescription = "FinTrack Pro Logo",
                modifier = Modifier.height(80.dp).wrapContentWidth(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White) // 🌟 YAHI HAI THE FIX!
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Typography
            Text("Welcome Back", fontSize = 32.sp, fontWeight = FontWeight.Black, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Securely track your wealth", fontSize = 15.sp, color = textMuted)

            Spacer(modifier = Modifier.height(48.dp))

            // Premium Input Field: Email
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = textMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), // More rounded like Axio
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedLabelColor = CyanAccent,
                    unfocusedLabelColor = textMuted,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = Color.Transparent, // Invisible border until focused
                    cursorColor = CyanAccent
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Premium Input Field: Password
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon", tint = textMuted) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedLabelColor = CyanAccent,
                    unfocusedLabelColor = textMuted,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = CyanAccent
                )
            )

            // 🌟 Forgot Password ka pura logic bina purana kuch chhede
            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = {
                    if (email.isNotEmpty()) {
                        val actionCodeSettings = ActionCodeSettings.newBuilder()
                            .setUrl("https://fintracker-pro-2ac50.firebaseapp.com/__/auth/action")
                            .setHandleCodeInApp(true)
                            .setAndroidPackageName(
                                "com.rishi.fintrack",
                                true,
                                null
                            )
                            .build()

                        auth.sendPasswordResetEmail(email, actionCodeSettings)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    successMessage = "Password reset link sent to your registered email."
                                    errorMessage = ""
                                } else {
                                    errorMessage = "Error: ${task.exception?.message}"
                                    successMessage = ""
                                }
                            }
                    } else {
                        errorMessage = "Please enter email to reset password"
                    }
                }) {
                    Text("Forgot Password?", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color(0xFFFF453A), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }

            if (successMessage.isNotEmpty()) {
                Text(successMessage, color = Color(0xFF32D74B), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Button with Axio Vibe
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) onLoginSuccess()
                                else errorMessage = "Error: ${task.exception?.message}"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Login to FinTrack Pro", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F1115))
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("New user? ", color = textMuted, fontSize = 15.sp)
                Text("Create an Account", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}