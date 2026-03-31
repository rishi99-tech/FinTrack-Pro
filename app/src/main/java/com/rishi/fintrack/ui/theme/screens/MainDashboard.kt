package com.rishi.fintrack.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.rishi.fintrack.R
import com.rishi.fintrack.data.PendingTransaction
import com.rishi.fintrack.data.LoanAccount
import com.rishi.fintrack.ui.theme.components.AddTransactionDialog
import com.rishi.fintrack.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════════
// ORIGINAL THEME COLORS & LOGO (MADE DYNAMIC)
// ═══════════════════════════════════════════════════════════════════════════════
val AppPrimaryBlue = Color(0xFF2563EB)
val CyanAccent = Color(0xFF00BCD4)
val SuccessGreen = Color(0xFF32D74B)
val DangerRed = Color(0xFFFF453A)
val WarningOrange = Color(0xFFF59E0B)
val ChartColors = listOf(AppPrimaryBlue, CyanAccent, Color(0xFF8B5CF6), SuccessGreen, Color(0xFFEC4899), WarningOrange)

val LocalThemeMode = compositionLocalOf { true }

fun formatIndianCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    return formatter.format(amount.toInt())
}

@Composable
fun AppBrandLogo(size: Dp = 40.dp) {
    val isDark = LocalThemeMode.current
    Image(
        painter = painterResource(id = R.drawable.ui_logo),
        contentDescription = "FinTrack Pro Logo",
        modifier = Modifier
            .height(size)
            .wrapContentWidth(),
        contentScale = ContentScale.Fit,
        colorFilter = if (isDark) androidx.compose.ui.graphics.ColorFilter.tint(Color.White) else null
    )
}

// ════════════ MAIN DASHBOARD ════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: TransactionViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf("Dashboard") }
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()

    val systemTheme = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemTheme) }

    CompositionLocalProvider(LocalThemeMode provides isDarkTheme) {

        val isDark = LocalThemeMode.current
        val bgColor = if (isDark) Color(0xFF0F1115) else Color(0xFFF8FAFC)
        val textColor = if (isDark) Color.White else Color(0xFF0F1115)

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = bgColor, modifier = Modifier.width(280.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        AppBrandLogo(size = 44.dp)
                    }
                    SidebarMenu(selectedScreen) { screen -> selectedScreen = screen; scope.launch { drawerState.close() } }
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationDrawerItem(
                        label = { Text("Logout", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            auth.signOut()
                        },
                        icon = { Icon(Icons.Default.Logout, null, tint = DangerRed) },
                        modifier = Modifier.padding(16.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = DangerRed)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { AppBrandLogo(size = 36.dp) },
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = textColor) } },
                        actions = {
                            IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                    contentDescription = "Toggle Theme",
                                    tint = textColor
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor)
                    )
                },
                containerColor = bgColor,
                floatingActionButton = {
                    if (selectedScreen == "Dashboard") {
                        FloatingActionButton(onClick = { showDialog = true }, containerColor = CyanAccent, contentColor = Color(0xFF0F1115), shape = CircleShape) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (selectedScreen) {
                        "Dashboard" -> DashboardContent(transactions, pendingTransactions, viewModel)
                        "Analytics" -> AnalyticsScreen(transactions)
                        "Reports" -> ReportsScreen(transactions, viewModel)
                        // 🌟 PROFILE LINK ADDED HERE
                        "Profile" -> ProfileScreen(onLogoutClick = { auth.signOut() })
                    }
                }
            }
        }
        if (showDialog) {
            AddTransactionDialog(onDismiss = { showDialog = false }, onAdd = { t, a, ty, c -> viewModel.addTransaction(t, a, ty, c); showDialog = false })
        }
    }
}

// ════════════ DASHBOARD CONTENT ════════════
@Composable
fun DashboardContent(transactions: List<com.rishi.fintrack.data.Transaction>, pendingTransactions: List<PendingTransaction>, viewModel: TransactionViewModel) {
    val loans by viewModel.allLoans.collectAsState()
    val budgetState by viewModel.budgetState.collectAsState()

    val inc = transactions.filter { it.type == "Income" }.sumOf { it.amount.toDouble() }
    val exp = transactions.filter { it.type == "Expense" }.sumOf { it.amount.toDouble() }
    var showLoanDialog by remember { mutableStateOf(false) }

    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val cardColor = if (isDark) Color(0xFF1C1F26) else Color.White

    var isAlertDismissed by remember { mutableStateOf(false) }
    val showWarningAlert = inc > 0 && exp >= (inc * 0.8) && exp <= inc
    val showDangerAlert = exp > inc

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (loans.isNotEmpty()) {
            item { Text("Debt Portfolio", color = textColor, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(vertical = 16.dp)); loans.forEach { LoanStatusCard(it) { viewModel.deleteLoan(it) }; Spacer(modifier = Modifier.height(10.dp)) } }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { showLoanDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(AppPrimaryBlue.copy(alpha=0.9f), CyanAccent.copy(alpha=0.8f))))) {
                        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Setup Loan / EMI Tracker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Track your debts smartly", color = Color.White.copy(alpha=0.8f), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }

        if (pendingTransactions.isNotEmpty()) { item { PendingReviewSection(pendingTransactions, { i, c -> viewModel.approveTransaction(i, c) }, { viewModel.rejectTransaction(it) }); Spacer(modifier = Modifier.height(16.dp)) } }

        if (!isAlertDismissed) {
            if (showDangerAlert) {
                item { SmartAlertCard("Negative Cashflow!", "Expenses exceeded income.", DangerRed, Icons.Default.Warning) { isAlertDismissed = true } }
            } else if (showWarningAlert) {
                item { SmartAlertCard("High Spending", "Consumed over 80% of inflow.", WarningOrange, Icons.Default.Info) { isAlertDismissed = true } }
            }
        }

        item { SafeToSpendCard(budgetState = budgetState) }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Total Income", "₹${formatIndianCurrency(inc)}", AppPrimaryBlue, Modifier.weight(1f));
                StatCard("Total Spent", "₹${formatIndianCurrency(exp)}", DangerRed, Modifier.weight(1f))
            }
        }

        items(transactions) { DarkTransactionItem(it) { viewModel.deleteTransaction(it) }; Spacer(modifier = Modifier.height(10.dp)) }
        item { Spacer(modifier = Modifier.height(80.dp)) } // Bottom padding for FAB
    }
    if (showLoanDialog) { AddLoanDialog({ showLoanDialog = false }, { n, t, e -> viewModel.addLoan(n, t, e); showLoanDialog = false }) }
}

@Composable
fun SmartAlertCard(title: String, message: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onDismiss: () -> Unit) {
    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White.copy(alpha = 0.8f) else Color.DarkGray
    val borderStroke = if (isDark) BorderStroke(1.dp, color.copy(alpha = 0.2f)) else null

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        colors = CardDefaults.cardColors(color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = color, fontWeight = FontWeight.Bold)
                Text(message, color = textColor, fontSize = 12.sp)
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8)) }
        }
    }
}

// ════════════ ANALYTICS SCREEN (100% ORIGINAL) ════════════
@Composable
fun AnalyticsScreen(transactions: List<com.rishi.fintrack.data.Transaction>) {
    val expenses = transactions.filter { it.type == "Expense" }
    val totalExp = expenses.sumOf { it.amount.toDouble() }.coerceAtLeast(1.0)
    val categoryData = expenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount.toDouble() } }
    val sortedCategories = categoryData.entries.sortedByDescending { it.value }
    val maxVal = sortedCategories.maxOfOrNull { it.value } ?: 1.0
    var showTrendModal by remember { mutableStateOf(false) }

    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val cardColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val borderStroke = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(cardColor),
                elevation = CardDefaults.cardElevation(0.dp),
                border = borderStroke
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Activity Overview", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            var startAngle = -90f; val stroke = Stroke(width = 40f, cap = StrokeCap.Round)
                            sortedCategories.forEachIndexed { index, entry ->
                                val sweep = (entry.value / totalExp).toFloat() * 360f
                                drawArc(color = ChartColors[index % ChartColors.size], startAngle = startAngle, sweepAngle = sweep - 4f, useCenter = false, style = stroke)
                                startAngle += sweep
                            }
                        }
                        Text("₹${formatIndianCurrency(totalExp)}", color = textColor, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    sortedCategories.take(4).forEachIndexed { index, entry ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(ChartColors[index % ChartColors.size], CircleShape))
                            Text(entry.key, color = textMuted, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                            Text("${((entry.value / totalExp) * 100).toInt()}%", color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { showTrendModal = true },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(cardColor),
                elevation = CardDefaults.cardElevation(0.dp),
                border = borderStroke
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expenditure Trend", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Icon(Icons.Default.TrendingUp, null, tint = CyanAccent)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        sortedCategories.take(7).forEach { entry ->
                            val h = (entry.value / maxVal).toFloat().coerceIn(0.1f, 1f)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.width(12.dp).fillMaxHeight(h).clip(RoundedCornerShape(4.dp)).background(Brush.verticalGradient(listOf(CyanAccent, AppPrimaryBlue))))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(entry.key.take(3).uppercase(), color = textMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        items(sortedCategories) { entry ->
            val pct = ((entry.value / totalExp) * 100).toInt()
            val advice = if (pct > 40) {
                "Critical Alert: ${pct}% of your spending is going to ${entry.key}. Consider making a strict budget here to save up to ₹${formatIndianCurrency(entry.value * 0.15)} next month."
            } else if (pct in 20..40) {
                "Moderate Burn: You spent ₹${formatIndianCurrency(entry.value)} on ${entry.key}. Small optimizations here can improve your overall cashflow."
            } else {
                "Optimal: ${entry.key} is well managed at just ${pct}% of your total outflow. Good job!"
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(cardColor),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = borderStroke
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.key, fontWeight = FontWeight.Black, fontSize = 16.sp, color = CyanAccent)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("₹${formatIndianCurrency(entry.value)}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• $advice", color = textMuted, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showTrendModal) {
        TrendLineChartModal(transactions) { showTrendModal = false }
    }
}

// ════════════ INDIVIDUAL ZIG-ZAG LINE CHART MODAL (100% ORIGINAL) ════════════
@Composable
fun TrendLineChartModal(transactions: List<com.rishi.fintrack.data.Transaction>, onClose: () -> Unit) {
    val allTxns = transactions.sortedBy { it.date }

    val isDark = LocalThemeMode.current
    val bgColor = if (isDark) Color(0xFF0F1115) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val cardColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(cardColor)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Cashflow Pulse", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Individual Transaction Trend", color = textMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.background(bgColor, CircleShape).size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(SuccessGreen, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp)); Text("Income", color = textMuted, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(DangerRed, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp)); Text("Expense", color = textMuted, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (allTxns.isNotEmpty()) {
                    val scrollState = rememberScrollState()
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp).horizontalScroll(scrollState)) {
                        val canvasWidth = maxOf(350, allTxns.size * 140).dp
                        Canvas(modifier = Modifier.width(canvasWidth).fillMaxHeight().padding(vertical = 40.dp)) {
                            val paddingX = 120f
                            val drawWidth = size.width - (2 * paddingX)
                            val maxVal = allTxns.maxOf { it.amount.toDouble() }.toFloat().coerceAtLeast(1f)
                            val xStep = if (allTxns.size > 1) drawWidth / (allTxns.size - 1) else drawWidth

                            val incPath = Path()
                            val expPath = Path()
                            var incStarted = false
                            var expStarted = false

                            allTxns.forEachIndexed { i, txn ->
                                val x = paddingX + (if (allTxns.size > 1) i * xStep else drawWidth / 2f)
                                val y = size.height - (txn.amount.toFloat() / maxVal * size.height)
                                val formattedDate = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(txn.date))

                                if (txn.type == "Expense") {
                                    if (!expStarted) { expPath.moveTo(x, y); expStarted = true } else { expPath.lineTo(x, y) }
                                    if (allTxns.filter{it.type=="Expense"}.size == 1) { expPath.moveTo(paddingX, y); expPath.lineTo(size.width - paddingX, y) }

                                    drawCircle(color = DangerRed, radius = 8f, center = androidx.compose.ui.geometry.Offset(x, y))
                                    drawContext.canvas.nativeCanvas.apply {
                                        val paintCat = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#94A3B8"); textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                                        drawText(txn.category, x, y - 45f, paintCat)
                                        val paintAmt = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FF453A"); textSize = 26f; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER }
                                        drawText("₹${txn.amount.toInt()}", x, y - 15f, paintAmt)
                                        val paintDate = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                                        drawText(formattedDate, x, size.height + 35f, paintDate)
                                    }
                                } else {
                                    if (!incStarted) { incPath.moveTo(x, y); incStarted = true } else { incPath.lineTo(x, y) }
                                    if (allTxns.filter{it.type=="Income"}.size == 1) { incPath.moveTo(paddingX, y); incPath.lineTo(size.width - paddingX, y) }

                                    drawCircle(color = SuccessGreen, radius = 8f, center = androidx.compose.ui.geometry.Offset(x, y))
                                    drawContext.canvas.nativeCanvas.apply {
                                        val paintAmt = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#32D74B"); textSize = 26f; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER }
                                        drawText("₹${txn.amount.toInt()}", x, y + 35f, paintAmt)
                                        val paintCat = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#94A3B8"); textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                                        drawText(txn.category, x, y + 60f, paintCat)
                                        val paintDate = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                                        drawText(formattedDate, x, size.height + 35f, paintDate)
                                    }
                                }
                            }
                            drawPath(path = incPath, color = SuccessGreen.copy(alpha = 0.6f), style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawPath(path = expPath, color = DangerRed.copy(alpha = 0.6f), style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Not enough data to plot trend.", color = textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(bgColor), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Total Income", color = textMuted, fontSize = 11.sp); Text("₹${formatIndianCurrency(allTxns.filter{it.type=="Income"}.sumOf{it.amount.toDouble()})}", color = SuccessGreen, fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Total Expense", color = textMuted, fontSize = 11.sp); Text("₹${formatIndianCurrency(allTxns.filter{it.type=="Expense"}.sumOf{it.amount.toDouble()})}", color = DangerRed, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

// ════════════ REPORTS SCREEN (100% ORIGINAL) ════════════
@Composable
fun ReportsScreen(transactions: List<com.rishi.fintrack.data.Transaction>, viewModel: TransactionViewModel) {
    val context = LocalContext.current
    val loans by viewModel.allLoans.collectAsState(initial = emptyList())

    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AppBrandLogo(size = 100.dp)
        Spacer(Modifier.height(32.dp))
        Text("Financial Statements", color = textColor, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text("Download your detailed cashflow & loan report for tax filing and personal records.", color = textMuted, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { generateFinancialReportPdf(context, transactions, loans) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF0F1115)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.PictureAsPdf, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Download PDF Report", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

// ════════════ PDF GENERATION LOGIC (100% ORIGINAL) ════════════
@android.annotation.SuppressLint("NewApi")
fun generateFinancialReportPdf(context: android.content.Context, transactions: List<com.rishi.fintrack.data.Transaction>, loans: List<LoanAccount>) {
    val pdfDoc = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDoc.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint()

    var y = 60f
    val margin = 50f

    val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
    if (bitmap != null) {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetHeight = 60
        val targetWidth = (targetHeight * aspectRatio).toInt()
        val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        canvas.drawBitmap(scaled, margin, y - 40f, null)
        paint.color = android.graphics.Color.parseColor("#0F1115")
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("FinTrack Pro", margin + targetWidth + 20f, y, paint)
    } else {
        paint.color = android.graphics.Color.parseColor("#0F1115")
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("FinTrack Pro", margin + 80f, y, paint)
    }

    paint.color = android.graphics.Color.parseColor("#64748B")
    paint.textSize = 14f
    paint.isFakeBoldText = false
    val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    canvas.drawText("Comprehensive Financial Report - ${dateFormat.format(java.util.Date())}", margin + 80f, y + 20f, paint)

    y += 70f
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawLine(margin, y, 595f - margin, y, paint)
    y += 40f

    val inc = transactions.filter { it.type == "Income" }.sumOf { it.amount.toDouble() }
    val exp = transactions.filter { it.type == "Expense" }.sumOf { it.amount.toDouble() }

    paint.color = android.graphics.Color.BLACK
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("1. Cashflow Summary", margin, y, paint)

    y += 30f
    paint.textSize = 14f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.parseColor("#32D74B")
    canvas.drawText("Total Income: Rs ${inc.toInt()}", margin, y, paint)
    paint.color = android.graphics.Color.parseColor("#FF453A")
    canvas.drawText("Total Expense: Rs ${exp.toInt()}", margin + 200f, y, paint)

    y += 50f

    paint.color = android.graphics.Color.BLACK
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("2. Active Debt & EMI Portfolio", margin, y, paint)

    y += 30f
    if (loans.isEmpty()) {
        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawText("No active loans found.", margin, y, paint)
        y += 30f
    } else {
        paint.textSize = 14f
        paint.color = android.graphics.Color.DKGRAY
        loans.forEach { loan ->
            paint.isFakeBoldText = true
            canvas.drawText("Loan Profile: ${loan.loanName}", margin, y, paint)
            paint.isFakeBoldText = false
            y += 20f
            canvas.drawText("Total Amount: Rs ${loan.totalAmount.toInt()}  |  Monthly EMI: Rs ${loan.emiAmount.toInt()}", margin, y, paint)
            y += 20f
            canvas.drawText("Remaining Balance: Rs ${loan.remainingAmount.toInt()}  |  Progress: ${((1 - (loan.remainingAmount / loan.totalAmount)) * 100).toInt()}% Paid", margin, y, paint)
            y += 35f
        }
    }

    y += 10f

    paint.color = android.graphics.Color.BLACK
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("3. Transaction Ledger (Recent)", margin, y, paint)
    y += 30f

    paint.textSize = 12f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawText("Date", margin, y, paint)
    canvas.drawText("Category", margin + 120f, y, paint)
    canvas.drawText("Type", margin + 280f, y, paint)
    canvas.drawText("Amount", margin + 400f, y, paint)

    y += 15f
    canvas.drawLine(margin, y, 595f - margin, y, paint)
    y += 25f

    paint.isFakeBoldText = false
    val recentTxns = transactions.sortedByDescending { it.date }.take(20)
    if(recentTxns.isEmpty()){
        canvas.drawText("No transactions found.", margin, y, paint)
    } else {
        recentTxns.forEach { t ->
            val d = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(t.date))
            canvas.drawText(d, margin, y, paint)
            canvas.drawText(t.category, margin + 120f, y, paint)
            canvas.drawText(t.type, margin + 280f, y, paint)
            if (t.type == "Income") paint.color = android.graphics.Color.parseColor("#32D74B")
            else paint.color = android.graphics.Color.parseColor("#FF453A")
            canvas.drawText("Rs ${t.amount}", margin + 400f, y, paint)
            paint.color = android.graphics.Color.DKGRAY
            y += 25f
            if (y > 800f) return@forEach
        }
    }

    pdfDoc.finishPage(page)
    val fileName = "FinTrack_Report_${System.currentTimeMillis()}.pdf"
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { pdfDoc.writeTo(it) }
                android.widget.Toast.makeText(context, "PDF Saved!", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(dir, fileName)
            java.io.FileOutputStream(file).use { pdfDoc.writeTo(it) }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("application/pdf"), null)
            android.widget.Toast.makeText(context, "PDF Saved!", android.widget.Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        pdfDoc.close()
    }
}

// 🌟 PROFILE LINK ADDED TO SIDEBAR
@Composable
fun SidebarMenu(selectedScreen: String, onSelect: (String) -> Unit) {
    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)

    val menuItems = listOf(
        "Dashboard" to Icons.Default.GridView,
        "Analytics" to Icons.Default.StackedBarChart,
        "Reports" to Icons.Default.FolderZip,
        "Profile" to Icons.Default.Person // 🌟 Yahan add hai
    )

    menuItems.forEach { (label, icon) ->
        val isSelected = selectedScreen == label
        NavigationDrawerItem(
            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            selected = isSelected,
            onClick = { onSelect(label) },
            icon = { Icon(icon, null) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = CyanAccent.copy(alpha = 0.15f),
                selectedTextColor = CyanAccent,
                selectedIconColor = CyanAccent,
                unselectedTextColor = textColor
            )
        )
    }
}

@Composable
fun LoanStatusCard(loan: LoanAccount, onDelete: () -> Unit) {
    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val cardColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val progress = (1 - (loan.remainingAmount / loan.totalAmount)).toFloat().coerceIn(0f, 1f)
    val borderStroke = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(cardColor), border = borderStroke) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(loan.loanName, color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp); Text("EMI: ₹${formatIndianCurrency(loan.emiAmount)}", color = textMuted, fontSize = 13.sp) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Close, null, tint = textMuted) } }
            Spacer(modifier = Modifier.height(16.dp)); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = CyanAccent, trackColor = Color.White.copy(alpha=0.1f))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Left: ₹${formatIndianCurrency(loan.remainingAmount)}", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("Total: ₹${formatIndianCurrency(loan.totalAmount)}", color = textMuted, fontSize = 13.sp) }
        }
    }
}

@Composable
fun AddLoanDialog(onDismiss: () -> Unit, onAdd: (String, Double, Double) -> Unit) {
    val isDark = LocalThemeMode.current
    val bgColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray
    var name by remember { mutableStateOf("") }; var total by remember { mutableStateOf("") }; var emi by remember { mutableStateOf("") }
    val inputColors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = CyanAccent, unfocusedBorderColor = textMuted.copy(alpha=0.5f), focusedLabelColor = CyanAccent, unfocusedLabelColor = textMuted, cursorColor = CyanAccent)
    AlertDialog(onDismissRequest = onDismiss, containerColor = bgColor, title = { Text("Setup Loan", color = textColor, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, colors = inputColors, singleLine = true); OutlinedTextField(value = total, onValueChange = { total = it }, label = { Text("Total") }, colors = inputColors, singleLine = true); OutlinedTextField(value = emi, onValueChange = { emi = it }, label = { Text("EMI") }, colors = inputColors, singleLine = true) } }, confirmButton = { Button(onClick = { onAdd(name, total.toDoubleOrNull() ?: 0.0, emi.toDoubleOrNull() ?: 0.0) }, colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)) { Text("Start Tracker", color = Color(0xFF0F1115), fontWeight = FontWeight.Bold) } })
}

@Composable
fun PendingReviewSection(pendingItems: List<PendingTransaction>, onApprove: (PendingTransaction, String) -> Unit, onReject: (PendingTransaction) -> Unit) {
    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val cardColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val categoryList = listOf("EMI", "Pocket Money", "Food", "Shopping", "Salary", "Rent", "Bills", "Health", "Others")
    Column {
        Text("Needs Your Review", color = WarningOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
        LazyRow(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pendingItems) { item ->
                var expanded by remember { mutableStateOf(false) }
                Card(modifier = Modifier.width(280.dp), colors = CardDefaults.cardColors(cardColor), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, WarningOrange.copy(0.3f))) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.title, color = textColor, fontWeight=FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); Text("₹${formatIndianCurrency(item.amount)}", fontWeight=FontWeight.Black, color = if(item.type == "Income") SuccessGreen else DangerRed) }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onReject(item) }) { Text("Ignore", color = textMuted) }
                            Box { Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)) { Text("Categorize", color = Color(0xFF0F1115), fontWeight=FontWeight.Bold) }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { categoryList.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onApprove(item, it); expanded = false }) } } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DarkTransactionItem(transaction: com.rishi.fintrack.data.Transaction, onDelete: () -> Unit) {
    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val cardColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val borderStroke = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null

    val isIncome = transaction.type == "Income"
    val iconColor = if (isIncome) SuccessGreen else DangerRed
    val iconBgColor = iconColor.copy(alpha = 0.15f)

    val categoryIcon = when (transaction.category) {
        "Salary" -> Icons.Default.AccountBalance
        "Pocket Money", "PocketMoney" -> Icons.Default.AccountBalanceWallet
        "Business" -> Icons.Default.Storefront
        "Freelance" -> Icons.Default.LaptopMac
        "Gift", "Gifts" -> Icons.Default.CardGiftcard
        "Investment" -> Icons.Default.TrendingUp
        "Food" -> Icons.Default.Restaurant
        "Grocery" -> Icons.Default.LocalGroceryStore
        "EMI", "Loan Repayment" -> Icons.Default.CreditCard
        "Rent" -> Icons.Default.Home
        "Petrol", "Petrolm" -> Icons.Default.LocalGasStation
        "Bills" -> Icons.Default.Receipt
        "Recharge" -> Icons.Default.PhoneAndroid
        "Shopping" -> Icons.Default.ShoppingBag
        "Health" -> Icons.Default.LocalHospital
        "Travel" -> Icons.Default.Flight
        "Entertainment" -> Icons.Default.Movie
        "Others" -> Icons.Default.Category
        else -> if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(cardColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = borderStroke
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(iconBgColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(categoryIcon, contentDescription = transaction.category, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(transaction.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(transaction.category, color = textMuted, fontSize = 13.sp)
            }
            Text("₹${formatIndianCurrency(transaction.amount)}", color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
            IconButton(onClick = onDelete, modifier = Modifier.padding(start = 8.dp).size(24.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = textMuted.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, accent: Color, modifier: Modifier) {
    val isDark = LocalThemeMode.current
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val cardColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val borderStroke = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null

    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(cardColor),
        shape = RoundedCornerShape(20.dp),
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold);
            Spacer(modifier = Modifier.height(12.dp));
            Text(value, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun SafeToSpendCard(budgetState: com.rishi.fintrack.viewmodel.BudgetState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Safe to Spend Today", color = CyanAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (budgetState.dailySafeLimit > 0) "₹${formatIndianCurrency(budgetState.dailySafeLimit)}" else "₹0",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Safe", tint = SuccessGreen, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Remaining Balance", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text("₹${formatIndianCurrency(budgetState.safeBalance)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("For next", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text("${budgetState.remainingDays} Days", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}