package com.rishi.fintrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PendingTransaction)

    @Query("SELECT * FROM pending_transactions ORDER BY timestamp DESC")
    fun getAllPending(): Flow<List<PendingTransaction>>

    @Delete
    suspend fun delete(transaction: PendingTransaction)
}