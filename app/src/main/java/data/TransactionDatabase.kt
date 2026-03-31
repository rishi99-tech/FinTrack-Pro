package com.rishi.fintrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Transaction::class, PendingTransaction::class, LoanAccount::class],
    version = 2,
    exportSchema = false
)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun loanAccountDao(): LoanAccountDao

    companion object {
        @Volatile
        private var INSTANCE: TransactionDatabase? = null

        fun getDatabase(context: Context): TransactionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "fintrack_db"
                )
                    .fallbackToDestructiveMigration() // Crash rokne ke liye sabse zaroori line
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}