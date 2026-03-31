package com.rishi.fintrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishi.fintrack.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    // --- Helper: Milliseconds ko readable Date mein badalne ke liye ---
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.category,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                // --- NAYA: Date aur Time dikhane ke liye ---
                Text(
                    text = dateString,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountColor = if (transaction.type == "Expense") Color.Red else Color(0xFF4CAF50)
                val prefix = if (transaction.type == "Expense") "-" else "+"

                Text(
                    text = "$prefix ₹${transaction.amount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFBDBDBD)
                    )
                }
            }
        }
    }
}