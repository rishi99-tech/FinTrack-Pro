package com.rishi.fintrack.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@Composable
fun ProfileScreen(onLogoutClick: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val userEmail = currentUser?.email ?: "user@fintrack.com"
    val context = LocalContext.current

    // Smart Logic: Email se Name nikalna (e.g., rishi@gmail.com -> Rishi)
    val userName = userEmail.substringBefore("@").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
    val userInitial = userName.take(1).uppercase()

    // 🌟 DYNAMIC THEME FIX: Ab Light/Dark mode dono properly kaam karenge
    val isDark = LocalThemeMode.current
    val AppPrimaryBlue = Color(0xFF2563EB)
    val CyanAccent = Color(0xFF00BCD4)
    val DangerRed = Color(0xFFFF453A)
    val SuccessGreen = Color(0xFF32D74B)

    val bgColor = if (isDark) Color(0xFF0F1115) else Color(0xFFF8FAFC)
    val surfaceColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            // TOP BAR
            Text(
                text = "My Identity",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 🌟 HERO SECTION: Avatar & Details
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CyanAccent, AppPrimaryBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitial,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = userName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = userEmail,
                fontSize = 15.sp,
                color = textMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🌟 TRUST BADGE: Cloud Sync Status
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SuccessGreen))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cloud Sync Active & Secured",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 🌟 MENU OPTIONS
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("ACCOUNT SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.Security,
                            title = "Privacy & Security",
                            subtitle = "Device-lock, Data permissions",
                            iconColor = CyanAccent,
                            textColor = textColor,
                            textMuted = textMuted,
                            onClick = { Toast.makeText(context, "Coming soon in Pro update!", Toast.LENGTH_SHORT).show() }
                        )
                        HorizontalDivider(color = bgColor, thickness = 2.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "Smart Alerts",
                            subtitle = "Parser settings & SMS triggers",
                            iconColor = CyanAccent,
                            textColor = textColor,
                            textMuted = textMuted,
                            onClick = { Toast.makeText(context, "Coming soon in Pro update!", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("DANGER ZONE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed.copy(alpha=0.7f), modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onLogoutClick() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = DangerRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Secure Logout", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 🌟 HELPER COMPONENT: Ab isme onClick lambda pass kar diya hai toast dikhane ke liye
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    textColor: Color,
    textMuted: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = textMuted, fontSize = 13.sp)
        }

        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = textMuted, modifier = Modifier.size(16.dp))
    }
}