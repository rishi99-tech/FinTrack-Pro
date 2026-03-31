package com.rishi.fintrack.utils

import android.content.Context
import android.util.Log
import com.rishi.fintrack.data.TransactionDatabase
import com.rishi.fintrack.data.PendingTransaction
import com.rishi.fintrack.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    private val foodMerchants = listOf("zomato", "swiggy", "eatsure", "mcdonald", "kfc", "domino", "pizzahut", "haldiram", "faasos", "behrouz", "burgerking", "foodpanda", "box8", "freshmenu", "subway", "starbucks", "barbeque nation", "uber eats", "door dash", "grubhub")

    // 🌟 UPDATED: Added Logistics & Delivery partners
    private val groceryMerchants = listOf("instamart", "blinkit", "zepto", "jiomart", "bigbasket", "amazon fresh", "dmart", "starbazaar", "milkbasket", "nature basket", "spencer", "more supermarket", "reliance fresh", "dunzo", "walmart", "tesco", "target", "carrefour", "ecom express", "xpressbees", "shadowfax")

    private val travelMerchants = listOf("uber", "ola", "rapido", "namma yatri", "makemytrip", "redbus", "irctc", "yatra", "indigo", "cleartrip", "fastag", "uts", "goibibo", "ixigo", "blusmart", "metro", "lyft", "grab", "bolt")

    private val entertainmentMerchants = listOf("netflix", "prime", "hotstar", "disney", "sonyliv", "zee5", "spotify", "bookmyshow", "pvr", "inox", "youtube", "apple music", "hulu", "hbo", "steam", "playstation", "xbox")

    // 🌟 UPDATED: Added Delhivery for Meesho/Logistics
    private val shoppingMerchants = listOf("flipkart", "amazon", "myntra", "meesho", "ajio", "nykaa", "tata cliq", "snapdeal", "reliance digital", "croma", "bewakoof", "decathlon", "ebay", "aliexpress", "zara", "h&m", "delhivery")

    private val healthMerchants = listOf("netmeds", "1mg", "pharmeasy", "apollo", "practo", "medplus", "healthkart", "drlalpathlabs", "srl diagnostics", "mfine", "cult.fit")

    private val emiKeywords = listOf(
        "emi", "loan", "installment", "amortization", "repayment", "debt", "mortgage",
        "ecs", "nach", "mandate", "standing instruction", "si txn", "auto-debit",
        "bajaj", "muthoot", "chola", "hdb", "tata capital", "home credit", "kreditbee", "zestmoney", "slice",
        "sbi", "hdfc", "icici", "axis", "hsbc", "citi", "barclays", "wells fargo", "jp morgan", "chase",
        "pnb", "bob", "bom", "idfc", "kotak", "canara", "yes bank", "rbl", "klarna", "affirm"
    )

    private val debitKeywords = listOf("debited", "deducted", "paid", "sent", "spent", "withdrawal", "w/d", "dr")
    private val creditKeywords = listOf("credited", "deposited", "received", "added", "cr", "refund")

    // 🌟 GHOST KILLER: List of words that should trigger an immediate IGNORE
    private val ignoreKeywords = listOf("otp", "code", "verification", "valid", "available balance", "clear bal", "bal:", "limit", "reward", "congratulations", "win", "shared")

    suspend fun parseSms(context: Context, sender: String, body: String, userId: String) {

        // 1. CLEAN THE TEXT & NORMALIZE (De-Fancy Engine)
        // First convert the crazy math fonts to normal text
        val cleanedBody = normalizeText(body)

        // Lowercase and trim extra spaces for stable regex matching
        var normalizedBody = cleanedBody.lowercase(Locale.getDefault())
        normalizedBody = normalizedBody.replace(Regex("[\\u00A0\\u2007\\u202F\\u200B]"), " ")
        normalizedBody = normalizedBody.replace(Regex("\\s+"), " ").trim()

        // 🌟 FIX: GHOST KILLER CHECK
        // Agar SMS me OTP ya Balance ki baat hai, toh ye financial transaction nahi, sirf alert hai.
        if (ignoreKeywords.any { normalizedBody.contains(it) }) {
            Log.d("FINTRACK_PRO", "🛑 Ghost SMS Ignored: $normalizedBody")
            return
        }

        // 2. AMOUNT EXTRACTION
        val amountPattern = Pattern.compile("(?i)(?:rs\\.?\\s*|inr\\s*|₹\\s*|usd\\s*|\\$\\s*|amount\\s*|pay\\s*)([\\d,]+(?:\\.\\d{1,2})?)")
        val matcher = amountPattern.matcher(normalizedBody)
        var amount = 0.0

        if (matcher.find()) {
            amount = matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }

        if (amount <= 0.0) {
            val simplePattern = Pattern.compile("(?i)(?:debited\\s+by\\s+|paid\\s+|sent\\s+|credited\\s+with\\s+|received\\s+)([\\d,]+(?:\\.\\d{1,2})?)")
            val simpleMatcher = simplePattern.matcher(normalizedBody)
            if (simpleMatcher.find()) {
                amount = simpleMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            }
        }

        // IF NO MONEY FOUND, ABORT.
        if (amount <= 0.0) return

        // 3. POLARITY CHECK (Is it Income or Expense?)
        val isDebit = debitKeywords.any { normalizedBody.contains(it) }
        val isCredit = creditKeywords.any { normalizedBody.contains(it) }
        val isATM = normalizedBody.contains("atm") || normalizedBody.contains("cash withdrawal")

        val finalType = if (isCredit) "Income" else "Expense"

        var extractedName = if (isCredit) "Unknown Sender" else "Unknown Receiver"
        try {
            val namePattern = Pattern.compile("(?i)(?:to|at|from|towards|vpa|favouring|favoring|paid)\\s+([a-zA-Z0-9@.\\-\\s]{3,35}?)(?:\\s+on\\b|\\s+ref\\b|\\s+rrn\\b|\\s+utr\\b|\\s+via\\b|\\s+date\\b|\\s+txn\\b|\\s+upi\\b|\\.|\\n|$)")
            val nameMatcher = namePattern.matcher(normalizedBody)

            if (nameMatcher.find()) {
                val rawName = nameMatcher.group(1)?.trim() ?: extractedName
                extractedName = rawName.split("@")[0].replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().uppercase(Locale.getDefault())
            }
        } catch (e: Exception) {
            Log.e("FINTRACK_PRO", "Name extraction failed: ${e.message}")
        }

        if (sender.contains("Pay", ignoreCase = true) || sender.contains("PhonePe", ignoreCase = true)) {
            if (extractedName.contains("UNKNOWN")) {
                extractedName = "$sender Transaction"
            }
        }

        // 5. AUTO-TAGGING ENGINE
        var isAutoApprove = false
        var finalCategory = "Others"
        var finalTitle = extractedName

        when {
            isATM -> {
                isAutoApprove = false
                finalTitle = "ATM Cash Withdrawal"
                finalCategory = "Others"
            }
            isCredit && (normalizedBody.contains("salary") || normalizedBody.contains("sal ") || normalizedBody.contains("payroll")) -> {
                isAutoApprove = true
                finalTitle = "Salary Credited"
                finalCategory = "Salary"
            }
            isCredit -> {
                isAutoApprove = false
                finalCategory = "Others"
                finalTitle = if (extractedName.contains("UNKNOWN")) "Received Money" else "Received from $extractedName"
            }
            isDebit && emiKeywords.any { normalizedBody.contains(it) } -> {
                isAutoApprove = true
                finalTitle = "Loan / EMI Repayment"
                finalCategory = "EMI"
            }
            isDebit -> {
                val merchantsMap = mapOf(
                    "Food" to foodMerchants, "Grocery" to groceryMerchants, "Travel" to travelMerchants,
                    "Entertainment" to entertainmentMerchants, "Shopping" to shoppingMerchants,
                    "Health" to healthMerchants
                )

                var matchedMerchantCategory: String? = null
                for ((cat, list) in merchantsMap) {
                    if (list.any { normalizedBody.contains(it.lowercase(Locale.getDefault())) }) {
                        matchedMerchantCategory = cat
                        break
                    }
                }

                if (matchedMerchantCategory != null) {
                    isAutoApprove = true
                    finalCategory = matchedMerchantCategory
                } else {
                    isAutoApprove = false
                    finalCategory = "Others"
                    if (extractedName.contains("UNKNOWN") && (normalizedBody.contains("atm") || normalizedBody.contains("w/d"))) {
                        finalTitle = "ATM Cash Withdrawal"
                    }
                }
            }
        }

        withContext(Dispatchers.IO) {
            try {
                val db = TransactionDatabase.getDatabase(context)
                if (isAutoApprove) {
                    val newTxn = Transaction(
                        title = finalTitle,
                        amount = amount,
                        type = finalType,
                        category = finalCategory,
                        date = System.currentTimeMillis(),
                        userId = userId
                    )
                    db.transactionDao().insertTransaction(newTxn)

                    if (finalCategory == "EMI") {
                        val activeLoans = db.loanAccountDao().getAllLoans().first().filter { it.userId == userId }
                        if (activeLoans.isNotEmpty()) {
                            val currentLoan = activeLoans.first()
                            val updatedRemaining = (currentLoan.remainingAmount - amount).coerceAtLeast(0.0)
                            db.loanAccountDao().updateLoan(currentLoan.copy(remainingAmount = updatedRemaining))
                        }
                    }
                } else {
                    val pending = PendingTransaction(
                        title = finalTitle,
                        amount = amount,
                        type = finalType,
                        timestamp = System.currentTimeMillis(),
                        userId = userId
                    )
                    db.pendingTransactionDao().insert(pending)
                }
            } catch (e: Exception) {
                Log.e("FINTRACK_PRO", "❌ DB Error: ${e.message}")
            }
        }
    }

    // 🌟 UPDATED: Permanent Fix for Mathematical/Fancy Fonts
    // Ye function Unicode character codes ko check karke unhe normal characters me map kar deta hai.
    private fun normalizeText(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            // Range check for Unicode Mathematical Alphanumeric Symbols (Fancy fonts)
            if (cp in 0x1D400..0x1D7FF) {
                val base = when {
                    cp >= 0x1D670 -> 0x1D670; cp >= 0x1D63C -> 0x1D63C; cp >= 0x1D608 -> 0x1D608; cp >= 0x1D5D4 -> 0x1D5D4; cp >= 0x1D5A0 -> 0x1D5A0; cp >= 0x1D56C -> 0x1D56C; cp >= 0x1D538 -> 0x1D538; cp >= 0x1D504 -> 0x1D504; cp >= 0x1D4D0 -> 0x1D4D0; cp >= 0x1D49C -> 0x1D49C; cp >= 0x1D468 -> 0x1D468; cp >= 0x1D434 -> 0x1D434; else -> 0x1D400
                }
                // Convert mathematical bold/italic to normal A-Z or a-z
                if (cp in 0x1D41A..0x1D433 || cp in 0x1D44E..0x1D467 || cp in 0x1D482..0x1D49B || cp in 0x1D4B6..0x1D4CF || cp in 0x1D4EA..0x1D503 || cp in 0x1D51E..0x1D537 || cp in 0x1D552..0x1D56B || cp in 0x1D586..0x1D59F || cp in 0x1D5BA..0x1D5D3 || cp in 0x1D5EE..0x1D607 || cp in 0x1D622..0x1D63B || cp in 0x1D656..0x1D66F || cp in 0x1D68A..0x1D6A3)
                    sb.append((cp - (base + 26) + 97).toChar()) else sb.append((cp - base + 65).toChar())
            } else {
                sb.append(Character.toChars(cp))
            }
            i += Character.charCount(cp)
        }
        return sb.toString()
    }
}