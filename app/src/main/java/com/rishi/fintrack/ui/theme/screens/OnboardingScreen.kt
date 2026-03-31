package com.rishi.fintrack.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Onboarding Data Class
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            title = "Never Type an Expense Again",
            description = "You work hard. Don't waste time logging ₹20 tea expenses. Let our AI do the heavy lifting automatically.",
            icon = Icons.Default.AutoAwesome,
            iconColor = Color(0xFF8B5CF6) // Purple
        ),
        OnboardingPage(
            title = "Spend with Zero Guilt",
            description = "We auto-detect your salary, set aside your EMIs & Rent, and give you a daily 'Safe-to-Spend' limit. No math required.",
            icon = Icons.Default.VerifiedUser,
            iconColor = Color(0xFF32D74B) // Success Green
        ),
        OnboardingPage(
            title = "Unlock Auto-Tracking ⚡",
            description = "To calculate your budget in real-time, FinTrack needs to quickly scan incoming bank alerts.",
            icon = Icons.Default.NotificationsActive,
            iconColor = Color(0xFF00BCD4) // Cyan Accent
        )
    )

    val bgColor = Color(0xFF0F1115)
    val textColor = Color.White
    val textMuted = Color(0xFF94A3B8)

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Top Logo
            AppBrandLogo(size = 48.dp) // Reusing your existing logo component

            Spacer(modifier = Modifier.weight(1f))

            // Swipeable Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { position ->
                OnboardingPageContent(page = pages[position])
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Page Indicators
            Row(
                Modifier.wrapContentHeight().fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFF00BCD4) else Color.DarkGray
                    val width = if (pagerState.currentPage == iteration) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .height(8.dp)
                            .width(width)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Actions
            if (pagerState.currentPage == 2) {
                // SCREEN 3: THE TRUST WALL
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trust Badges
                    TrustBadge(icon = Icons.Default.AccountBalance, text = "Bank Alerts Only (GPay, SBI, etc.)")
                    TrustBadge(icon = Icons.Default.ChatBubbleOutline, text = "No WhatsApp or Personal Chats read")
                    TrustBadge(icon = Icons.Default.Lock, text = "100% Private & Device-Local")

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            // 🌟 PERMISSION INTENT: Sends user directly to Notification settings
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                            onFinish() // User ko permission page par bhej kar dashboard dikha do
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Enable Smart Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reality First: The Skip Button
                    TextButton(onClick = { onFinish() }) {
                        Text("Skip & Enter Manually", color = textMuted)
                    }
                }
            } else {
                // SCREEN 1 & 2: NEXT BUTTON
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1115))
                }
                Spacer(modifier = Modifier.height(48.dp)) // To match height of Screen 3 layout
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(page.iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.iconColor,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            fontSize = 15.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun TrustBadge(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF32D74B), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color(0xFF94A3B8), fontSize = 13.sp)
    }
}