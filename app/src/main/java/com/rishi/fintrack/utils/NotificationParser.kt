package com.rishi.fintrack.utils

import android.content.Context
import android.util.Log
import com.rishi.fintrack.data.TransactionDatabase
import com.rishi.fintrack.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.regex.Pattern

object NotificationParser {

    // 🌟 LAUNCH-GRADE: Synced Merchant Lists
    private val foodMerchants = listOf("zomato", "swiggy", "eatsure", "mcdonald", "kfc", "domino", "pizzahut", "haldiram", "faasos", "behrouz", "burgerking", "foodpanda", "box8", "freshmenu", "subway", "starbucks", "barbeque nation", "uber eats", "door dash", "grubhub")
    private val groceryMerchants = listOf("instamart", "blinkit", "zepto", "jiomart", "bigbasket", "amazon fresh", "dmart", "starbazaar", "milkbasket", "nature basket", "spencer", "more supermarket", "reliance fresh", "dunzo", "walmart", "tesco", "target", "carrefour", "ecom express", "xpressbees", "shadowfax")
    private val travelMerchants = listOf("uber", "ola", "rapido", "namma yatri", "makemytrip", "redbus", "irctc", "yatra", "indigo", "cleartrip", "fastag", "uts", "goibibo", "ixigo", "blusmart", "metro", "lyft", "grab", "bolt")
    private val entertainmentMerchants = listOf("netflix", "prime", "hotstar", "disney", "sonyliv", "zee5", "spotify", "bookmyshow", "pvr", "inox", "youtube", "apple music", "hulu", "hbo", "steam", "playstation", "xbox")
    private val shoppingMerchants = listOf("flipkart", "amazon", "myntra", "meesho", "ajio", "nykaa", "tata cliq", "snapdeal", "reliance digital", "croma", "bewakoof", "decathlon", "ebay", "aliexpress", "zara", "h&m", "delhivery")
    private val healthMerchants = listOf("netmeds", "1mg", "pharmeasy", "apollo", "practo", "medplus", "healthkart", "drlalpathlabs", "srl diagnostics", "mfine", "cult.fit")

    private val emiKeywords = listOf(
        "emi", "loan", "installment", "amortization", "repayment", "debt", "mortgage",
        "ecs", "nach", "mandate", "standing instruction", "si txn", "auto-debit",
        "bajaj", "muthoot", "chola", "hdb", "tata capital", "home credit", "kreditbee", "zestmoney", "slice",
        "sbi", "hdfc", "icici", "axis", "hsbc", "citi", "barclays", "wells fargo", "jp morgan", "chase",
        "pnb", "bob", "bom", "idfc", "kotak", "canara", "yes bank", "rbl", "klarna", "affirm"
    )

    // 🌟 GHOST KILLER: Strict stop-words for production
    private val ignoreKeywords = listOf("otp", "code", "verification", "valid", "available balance", "clear bal", "bal:", "limit", "reward", "congratulations", "win", "shared", "reminder", "overdue")

    suspend fun parseNotification(context: Context, packageName: String, title: String, text: String, userId: String) {

        // 1. TEXT NORMALIZATION (The De-Fancy Engine)
        val rawCombined = "$title $text"
        val cleanedText = normalizeText(rawCombined)
        var lowerText = cleanedText.lowercase(Locale.getDefault())

        // Remove invisible Unicode spaces and clean up
        lowerText = lowerText.replace(Regex("[\\u00A0\\u2007\\u202F\\u200B]"), " ")
        lowerText = lowerText.replace(Regex("\\s+"), " ").trim()

        // 2. GHOST KILLER EXECUTION
        if (ignoreKeywords.any { lowerText.contains(it) }) {
            Log.d("FINTRACK_PRO", "🛑 Ghost Notification Ignored (Production Rule): $lowerText")
            return
        }

        val combinedText = lowerText

        var amount = 0.0
        var isDebit = false
        var isCredit = false
        var extractedName = "Unknown"

        // 3. FAST REGEX ENGINE (UPI Apps Priority)
        val paidPattern = Pattern.compile("(?i)(?:paid|sent)\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+\\.?\\d*)")
        val paidMatcher = paidPattern.matcher(combinedText)

        val receivedPattern = Pattern.compile("(?i)(?:received)\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+\\.?\\d*)")
        val receivedMatcher = receivedPattern.matcher(combinedText)

        if (paidMatcher.find()) {
            amount = paidMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            isDebit = true
            val nameMatch = Pattern.compile("(?i)(?:to)\\s+([a-zA-Z0-9\\s]+)").matcher(combinedText)
            if (nameMatch.find()) extractedName = nameMatch.group(1)?.trim()?.replaceFirstChar { it.uppercase() } ?: "Unknown"

        } else if (receivedMatcher.find()) {
            amount = receivedMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            isCredit = true
            val nameMatch = Pattern.compile("(?i)(?:from)\\s+([a-zA-Z0-9\\s]+)").matcher(combinedText)
            if (nameMatch.find()) extractedName = nameMatch.group(1)?.trim()?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        }
        // 4. FALLBACK ENGINE (Core Banking Alerts)
        else {
            val bankAmountPattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)")
            val bankMatcher = bankAmountPattern.matcher(combinedText)
            if (bankMatcher.find()) {
                amount = bankMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                isDebit = combinedText.contains("debited") || combinedText.contains("spent") || combinedText.contains("deducted")
                isCredit = combinedText.contains("credited") || combinedText.contains("deposited") || combinedText.contains("salary")
            }
        }

        // Abort if no financial value detected
        if (amount <= 0.0) return

        // 5. AI CATEGORIZATION ENGINE
        var finalCategory = "Others"
        var finalTitle = extractedName
        val finalType = if (isCredit) "Income" else "Expense"

        when {
            isCredit && (lowerText.contains("salary") || lowerText.contains("sal ") || lowerText.contains("payroll")) -> {
                finalTitle = "Salary Credited"
                finalCategory = "Salary"
            }
            isDebit && emiKeywords.any { lowerText.contains(it) } -> {
                finalTitle = "Loan / EMI Repayment"
                finalCategory = "EMI"
            }
            isDebit -> {
                val merchantsMap = mapOf(
                    "Food" to foodMerchants, "Grocery" to groceryMerchants, "Travel" to travelMerchants,
                    "Entertainment" to entertainmentMerchants, "Shopping" to shoppingMerchants,
                    "Health" to healthMerchants
                )

                for ((cat, list) in merchantsMap) {
                    if (list.any { lowerText.contains(it) || finalTitle.lowercase(Locale.getDefault()).contains(it) }) {
                        finalCategory = cat
                        break
                    }
                }
            }
        }

        // 6. SAFE DATABASE TRANSACTION
        withContext(Dispatchers.IO) {
            try {
                val db = TransactionDatabase.getDatabase(context)
                val newTxn = Transaction(
                    title = finalTitle,
                    amount = amount,
                    type = finalType,
                    category = finalCategory,
                    date = System.currentTimeMillis(),
                    userId = userId
                )
                db.transactionDao().insertTransaction(newTxn)
                Log.d("FINTRACK_PRO", "✅ Production Notification Saved: $finalTitle | ₹$amount | Cat: $finalCategory")

                // Update Loan/EMI Math
                if (finalCategory == "EMI") {
                    val activeLoans = db.loanAccountDao().getAllLoans().first().filter { it.userId == userId }
                    if (activeLoans.isNotEmpty()) {
                        val currentLoan = activeLoans.first()
                        val updatedRemaining = (currentLoan.remainingAmount - amount).coerceAtLeast(0.0)
                        db.loanAccountDao().updateLoan(currentLoan.copy(remainingAmount = updatedRemaining))
                    }
                }
            } catch (e: Exception) {
                Log.e("FINTRACK_PRO", "❌ Notification DB Error: ${e.message}")
            }
        }
    }

    // 🌟 DE-FANCY ENGINE: Converts Unicode Math Fonts to standard text
    private fun normalizeText(text: String): String {
        // Core replacements for known bad formats
        var cleanText = text
            .replace("𝖽𝖾𝖻𝗂𝗍𝖾𝖽", "debited").replace("𝖣𝖾𝖻𝗂𝗍𝖾𝖽", "debited")
            .replace("𝖼𝗋𝖾𝖽𝗂𝗍𝖾𝖽", "credited").replace("𝖢𝗋𝖾𝖽𝗂𝗍𝖾𝖽", "credited")
            .replace("𝖱𝗌.", "Rs.").replace("𝖨𝖭𝖱", "INR")
            .replace("𝖠/𝖼", "A/c")
            .replace("𝖿𝗈𝗋", "for").replace("𝖴𝖯𝖨", "UPI")
            .replace("𝗉𝖺𝗒𝗆𝖾𝗇𝗍", "payment").replace("𝗍𝗈", "to")

        val sb = StringBuilder()
        var i = 0
        while (i < cleanText.length) {
            val cp = cleanText.codePointAt(i)
            // Advanced Unicode Mathematical Alphanumeric filter
            if (cp in 0x1D400..0x1D7FF) {
                val base = when {
                    cp >= 0x1D670 -> 0x1D670; cp >= 0x1D63C -> 0x1D63C; cp >= 0x1D608 -> 0x1D608; cp >= 0x1D5D4 -> 0x1D5D4; cp >= 0x1D5A0 -> 0x1D5A0; cp >= 0x1D56C -> 0x1D56C; cp >= 0x1D538 -> 0x1D538; cp >= 0x1D504 -> 0x1D504; cp >= 0x1D4D0 -> 0x1D4D0; cp >= 0x1D49C -> 0x1D49C; cp >= 0x1D468 -> 0x1D468; cp >= 0x1D434 -> 0x1D434; else -> 0x1D400
                }
                if (cp in 0x1D41A..0x1D433 || cp in 0x1D44E..0x1D467 || cp in 0x1D482..0x1D49B || cp in 0x1D4B6..0x1D4CF || cp in 0x1D4EA..0x1D503 || cp in 0x1D51E..0x1D537 || cp in 0x1D552..0x1D56B || cp in 0x1D586..0x1D59F || cp in 0x1D5BA..0x1D5D3 || cp in 0x1D5EE..0x1D607 || cp in 0x1D622..0x1D63B || cp in 0x1D656..0x1D66F || cp in 0x1D68A..0x1D6A3)
                    sb.append((cp - (base + 26) + 97).toChar()) else sb.append((cp - base + 65).toChar())
            } else {
                sb.append(Character.toChars(cp))
            }
            i += Character.charCount(cp)
        }
        return java.text.Normalizer.normalize(sb.toString(), java.text.Normalizer.Form.NFKD)
    }
}