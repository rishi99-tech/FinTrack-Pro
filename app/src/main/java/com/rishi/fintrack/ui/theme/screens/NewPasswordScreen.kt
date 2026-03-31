package com.rishi.fintrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.rishi.fintrack.R

@Composable
fun NewPasswordScreen(
    oobCode: String, // 🌟 Ye special code email link se aayega
    onPasswordResetSuccess: () -> Unit // Password change hone ke baad wapas Login par bhejne ke liye
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()

    // 🌟 SAME PREMIUM THEME AS LOGIN SCREEN
    val AppPrimaryBlue = Color(0xFF2563EB)
    val bgColor = Color(0xFFF8FAFC)
    val textColor = Color(0xFF0F1115)
    val textMuted = Color.Gray

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ui_logo),
                contentDescription = "FinTrack Pro Logo",
                modifier = Modifier.height(90.dp).wrapContentWidth(),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Set New Password", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text("Create a strong password to secure your wealth", fontSize = 14.sp, color = textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            Spacer(modifier = Modifier.height(40.dp))

            // New Password Field
            OutlinedTextField(
                value = newPassword, onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor, unfocusedTextColor = textColor,
                    focusedLabelColor = AppPrimaryBlue, unfocusedLabelColor = textMuted,
                    focusedBorderColor = AppPrimaryBlue, unfocusedBorderColor = textMuted.copy(alpha = 0.5f),
                    cursorColor = AppPrimaryBlue
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor, unfocusedTextColor = textColor,
                    focusedLabelColor = AppPrimaryBlue, unfocusedLabelColor = textMuted,
                    focusedBorderColor = AppPrimaryBlue, unfocusedBorderColor = textMuted.copy(alpha = 0.5f),
                    cursorColor = AppPrimaryBlue
                )
            )

            // Error / Success Messages
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color(0xFFFF453A), fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
            }
            if (successMessage.isNotEmpty()) {
                Text(successMessage, color = Color(0xFF32D74B), fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        errorMessage = "Please fill all fields"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMessage = "Passwords do not match!"
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        return@Button
                    }

                    // 🌟 MAIN LOGIC: Firebase ko naya password batana
                    isLoading = true
                    errorMessage = ""
                    auth.confirmPasswordReset(oobCode, newPassword)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                successMessage = "Password Updated Successfully!"
                                // Wapas Login screen par bhej do
                                onPasswordResetSuccess()
                            } else {
                                errorMessage = "Error: ${task.exception?.message}"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}