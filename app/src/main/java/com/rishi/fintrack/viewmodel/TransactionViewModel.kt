package com.rishi.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rishi.fintrack.data.PendingTransaction
import com.rishi.fintrack.data.Transaction
import com.rishi.fintrack.data.TransactionDatabase
import com.rishi.fintrack.data.TransactionRepository
import com.rishi.fintrack.data.LoanAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

// 🌟 NAYA: UI ko saara calculation ek sath bhejne ke liye data class
data class BudgetState(
    val totalSalary: Double = 0.0,
    val fixedExpenses: Double = 0.0,
    val variableExpenses: Double = 0.0,
    val dailySafeLimit: Double = 0.0,
    val remainingDays: Int = 1,
    val safeBalance: Double = 0.0
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val database: TransactionDatabase
    val allTransactions: Flow<List<Transaction>>
    val pendingTransactions: StateFlow<List<PendingTransaction>>
    val allLoans: StateFlow<List<LoanAccount>>

    // 🌟 THE BRAIN: Ye UI ko directly Safe-to-Spend data dega
    val budgetState: StateFlow<BudgetState>

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: "default_user"

    init {
        database = TransactionDatabase.getDatabase(application)
        val dao = database.transactionDao()
        repository = TransactionRepository(dao)

        allTransactions = repository.allTransactions.map { list ->
            list.filter { it.userId == currentUserId }
        }

        // 🌟 THE CORE ALGORITHM: Live Budget Calculation
        budgetState = allTransactions.map { transactions ->
            calculateBudgetState(transactions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetState())

        pendingTransactions = database.pendingTransactionDao().getAllPending()
            .map { list -> list.filter { it.userId == currentUserId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allLoans = database.loanAccountDao().getAllLoans()
            .map { list -> list.filter { it.userId == currentUserId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        restoreDataFromCloud()
    }

    // 🌟 NAYA FUNCTION: Math Logic for "Safe-to-Spend"
    private fun calculateBudgetState(transactions: List<Transaction>): BudgetState {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Bacha hua din (Aaj ka din bhi include kiya hai)
        val remainingDays = (maxDays - today + 1).coerceAtLeast(1)

        var salary = 0.0
        var fixed = 0.0
        var variable = 0.0

        for (txn in transactions) {
            val txnCalendar = Calendar.getInstance().apply { timeInMillis = txn.date }
            // Sirf is mahine ka data uthao
            if (txnCalendar.get(Calendar.MONTH) == currentMonth && txnCalendar.get(Calendar.YEAR) == currentYear) {
                if (txn.type == "Income" && txn.category.equals("Salary", ignoreCase = true)) {
                    salary += txn.amount
                } else if (txn.type == "Expense") {
                    // EMI, Rent, SIP ko fixed maan rahe hain
                    if (txn.category.equals("EMI", ignoreCase = true) ||
                        txn.category.equals("Rent", ignoreCase = true) ||
                        txn.category.equals("SIP", ignoreCase = true)) {
                        fixed += txn.amount
                    } else {
                        // Baki sab (Khana, Travel, etc) variable hai
                        variable += txn.amount
                    }
                }
            }
        }

        // Formula: (Salary - EMI/Rent - Ab tak udaya hua paisa)
        val safeBalance = salary - fixed - variable
        val dailyLimit = if (safeBalance > 0) safeBalance / remainingDays else 0.0

        return BudgetState(
            totalSalary = salary,
            fixedExpenses = fixed,
            variableExpenses = variable,
            dailySafeLimit = dailyLimit,
            remainingDays = remainingDays,
            safeBalance = safeBalance
        )
    }

    fun addLoan(name: String, total: Double, emi: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val newLoan = LoanAccount(
                loanName = name,
                totalAmount = total,
                remainingAmount = total,
                emiAmount = emi,
                userId = currentUserId
            )
            database.loanAccountDao().insertLoan(newLoan)
        }
    }

    fun addTransaction(title: String, amount: Double, type: String, category: String) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val transaction = Transaction(
                title = title,
                amount = amount,
                type = type,
                category = category,
                date = timestamp,
                userId = currentUserId
            )
            repository.insert(transaction)
            android.util.Log.d("CloudSync", "Phone me save ho gaya. Ab Cloud ka try kar rahe hain...")

            if (currentUserId != "default_user") {
                val transactionMap = hashMapOf(
                    "title" to title,
                    "amount" to amount,
                    "type" to type,
                    "category" to category,
                    "date" to timestamp,
                    "userId" to currentUserId
                )
                db.collection("users").document(currentUserId)
                    .collection("transactions").document(timestamp.toString())
                    .set(transactionMap)
                    .addOnSuccessListener {
                        android.util.Log.d("CloudSync", "🔥 SUCCESS: Data Cloud me chala gaya!")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("CloudSync", "❌ ERROR: Cloud me fail ho gaya!", e)
                    }
            } else {
                android.util.Log.e("CloudSync", "⚠️ ALERT: User logged in nahi hai (default_user). Isliye data Cloud me nahi gaya!")
            }

            if (type == "Expense" && (category.equals("EMI", ignoreCase = true) || category.equals("Loan Repayment", ignoreCase = true))) {
                val currentLoans = allLoans.value
                if (currentLoans.isNotEmpty()) {
                    database.loanAccountDao().deductEMI(currentLoans[0].id, amount)
                }
            }
        }
    }

    fun approveTransaction(pending: PendingTransaction, finalCategory: String = "Others") {
        viewModelScope.launch {
            addTransaction(
                title = pending.title,
                amount = pending.amount,
                type = pending.type,
                category = finalCategory
            )
            database.pendingTransactionDao().delete(pending)
        }
    }

    fun rejectTransaction(pending: PendingTransaction) {
        viewModelScope.launch { database.pendingTransactionDao().delete(pending) }
    }

    // 🌟 FIX: Naya Delete Function Jo Cloud Se Bhi Delete Karega
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Pehle phone (Local DB) se delete karo
            repository.delete(transaction)

            // 2. Phir chupchaap Cloud (Firebase) se bhi uda do ☁️
            if (currentUserId != "default_user") {
                // Humne add karte waqt date (timestamp) ko ID banaya tha, toh wahi id delete karenge
                db.collection("users").document(currentUserId)
                    .collection("transactions").document(transaction.date.toString())
                    .delete()
                    .addOnSuccessListener {
                        android.util.Log.d("CloudSync", "✅ SUCCESS: Data Cloud se hamesha ke liye delete ho gaya!")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("CloudSync", "❌ ERROR: Cloud se delete nahi hua!", e)
                    }
            }
        }
    }

    fun deleteLoan(loan: LoanAccount) {
        viewModelScope.launch(Dispatchers.IO) { database.loanAccountDao().deleteLoan(loan) }
    }

    fun restoreDataFromCloud() {
        android.util.Log.d("CloudRestore", "1. Restore function start hua")
        if (currentUserId == "default_user") {
            android.util.Log.d("CloudRestore", "User logged in nahi hai, wapas ja rahe hain")
            return
        }

        android.util.Log.d("CloudRestore", "2. Cloud se data maang rahe hain user ID: $currentUserId")
        db.collection("users").document(currentUserId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("CloudRestore", "3. Cloud se response aaya! Total items: ${documents.size()}")
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val localData = repository.allTransactions.first().filter { it.userId == currentUserId }
                        android.util.Log.d("CloudRestore", "4. Phone me pehle se ${localData.size} items hain")

                        if (localData.isEmpty() && !documents.isEmpty) {
                            android.util.Log.d("CloudRestore", "5. Phone khali hai, Cloud se data daalna shuru!")
                            for (document in documents) {
                                val title = document.getString("title") ?: ""
                                val amount = document.getDouble("amount") ?: (document.getLong("amount")?.toDouble() ?: 0.0)
                                val type = document.getString("type") ?: ""
                                val category = document.getString("category") ?: ""
                                val date = document.getLong("date") ?: System.currentTimeMillis()

                                val transaction = Transaction(
                                    title = title, amount = amount, type = type, category = category, date = date, userId = currentUserId
                                )
                                repository.insert(transaction)
                            }
                            android.util.Log.d("CloudRestore", "6. 🔥 SUCCESS: Saara data wapas phone me aa gaya!")
                        } else {
                            android.util.Log.d("CloudRestore", "Data pehle se hai ya Cloud khali hai. Kuch nahi kiya.")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CloudRestore", "❌ ERROR Restore karte waqt: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CloudRestore", "❌ ERROR Cloud se data laane me fail: ${e.message}")
            }
    }
}