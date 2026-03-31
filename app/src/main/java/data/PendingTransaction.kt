package com.rishi.fintrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String,      // "Expense" or "Income"
    val timestamp: Long,
    val category: String = "Uncategorized",
    // 🌟 THE FIX: Har pending transaction ko user se link karne ke liye
    val userId: String
)