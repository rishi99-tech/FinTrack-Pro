package com.rishi.fintrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Saare transactions ko date ke hisab se naya wala upar dikhao (Latest First)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // Naya kharcha ya income add karne ke liye
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // Purana record delete karne ke liye
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // Data Analytics ke liye: Type ke hisab se total (Income vs Expense)
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :transactionType")
    fun getTotalByTransactionType(transactionType: String): Flow<Double?>
}