package com.rishi.fintrack.data

import kotlinx.coroutines.flow.Flow

/**
 * Ye class UI ko data deti hai aur
 * Database se queries mangwati hai.
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    // Saare transactions ki list
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Naya transaction add karne ka rasta
    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    // Delete karne ka rasta
    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    // Analytics ke liye total nikalna
    fun getTotalAmount(type: String): Flow<Double?> {
        return transactionDao.getTotalByTransactionType(type)
    }
}