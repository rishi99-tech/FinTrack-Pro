package com.rishi.fintrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_accounts")
data class LoanAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loanName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val emiAmount: Double,
    val interestRate: Float = 0f,
    // 🌟 THE FIX: Har loan ke sath User ID tag karne ke liye
    val userId: String
)