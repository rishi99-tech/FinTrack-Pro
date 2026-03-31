package com.rishi.fintrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter // 🌟 NEW IMPORT for Logo Fix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.rishi.fintrack.R

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
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
            // 🌟 LOGO FIX: Same white filter applied here
            Image(
                painter = painterResource(id = R.drawable.ui_logo),
                contentDescription = "FinTrack Pro Logo",
                modifier = Modifier.height(80.dp).wrapContentWidth(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Typography matching Login Screen
            Text("Create Account", fontSize = 32.sp, fontWeight = FontWeight.Black, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Join the Financial Revolution", fontSize = 15.sp, color = textMuted)

            Spacer(modifier = Modifier.height(48.dp))

            // Premium Input Field: Email
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = textMuted) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // Premium Input Field: Password
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password (Min 6 chars)") },
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

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color(0xFFFF453A), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Button matching Login
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) onRegisterSuccess()
                                else errorMessage = "Error: ${task.exception?.message}"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Register Now", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F1115))
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? ", color = textMuted, fontSize = 15.sp)
                Text("Login", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}