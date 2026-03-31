package com.rishi.fintrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,         // Unique ID automatically generated
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: Long,
    // 🌟 THE FIX: User identification ke liye
    val userId: String
)