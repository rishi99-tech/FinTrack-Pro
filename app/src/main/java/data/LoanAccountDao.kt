package com.rishi.fintrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanAccountDao {
    @Query("SELECT * FROM loan_accounts")
    fun getAllLoans(): Flow<List<LoanAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanAccount)

    @Update
    suspend fun updateLoan(loan: LoanAccount)

    @Query("UPDATE loan_accounts SET remainingAmount = remainingAmount - :paidAmount WHERE id = :loanId")
    suspend fun deductEMI(loanId: Int, paidAmount: Double)

    @Delete
    suspend fun deleteLoan(loan: LoanAccount)
}