package com.rishi.fintrack.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rishi.fintrack.ui.screens.LocalThemeMode // 🌟 NAYA IMPORT: App ka smart theme check karne ke liye

// Color Variables
val AppPrimaryBlue = Color(0xFF2563EB)
val AppCyan = Color(0xFF00BCD4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Expense") }

    val expenseCategories = listOf("Food", "Grocery", "EMI", "Loan Repayment", "Rent", "Petrol", "Bills", "Recharge", "Shopping", "Health", "Travel", "Entertainment", "Others")
    val incomeCategories = listOf("Salary", "Pocket Money", "Business", "Freelance", "Gift", "Investment", "Others")

    var category by remember { mutableStateOf(expenseCategories[0]) }
    var expanded by remember { mutableStateOf(false) }

    // 🌟 THE FIX: Ab ye app ke sun/moon button ke hisaab se color badlega!
    val isDark = LocalThemeMode.current
    val bgColor = if (isDark) Color(0xFF1C1F26) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F1115)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color.Gray

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = bgColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Transaction", style = MaterialTheme.typography.headlineSmall, color = textColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Chai, Room Rent)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = AppCyan,
                        focusedLabelColor = AppCyan,
                        unfocusedBorderColor = textMuted.copy(alpha = 0.5f), // 👈 FIX: Box ki line hamesha dikhegi
                        unfocusedLabelColor = textMuted,
                        cursorColor = AppPrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = AppCyan,
                        focusedLabelColor = AppCyan,
                        unfocusedBorderColor = textMuted.copy(alpha = 0.5f), // 👈 FIX: Box ki line
                        unfocusedLabelColor = textMuted,
                        cursorColor = AppPrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = type == "Income",
                        onClick = {
                            type = "Income"
                            category = incomeCategories[0]
                        },
                        label = { Text("Income", color = if (type == "Income") AppPrimaryBlue else textColor) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppPrimaryBlue.copy(alpha = 0.2f),
                            selectedLabelColor = AppPrimaryBlue
                        )
                    )
                    FilterChip(
                        selected = type == "Expense",
                        onClick = {
                            type = "Expense"
                            category = expenseCategories[0]
                        },
                        label = { Text("Expense", color = if (type == "Expense") AppPrimaryBlue else textColor) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppPrimaryBlue.copy(alpha = 0.2f),
                            selectedLabelColor = AppPrimaryBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CATEGORY DROPDOWN
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Category") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, "contentDescription",
                                Modifier.clickable { expanded = !expanded }, tint = textColor)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = AppCyan,
                            focusedLabelColor = AppCyan,
                            unfocusedBorderColor = textMuted.copy(alpha = 0.5f),
                            unfocusedLabelColor = textMuted
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f).background(bgColor)
                    ) {
                        val currentList = if (type == "Expense") expenseCategories else incomeCategories
                        currentList.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label, color = textColor) },
                                onClick = {
                                    category = label
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && amount.isNotBlank()) {
                            onAdd(title, amount.toDoubleOrNull() ?: 0.0, type, category)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue)
                ) {
                    Text("Save Transaction", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}