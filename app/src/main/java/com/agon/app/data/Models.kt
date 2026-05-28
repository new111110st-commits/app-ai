package com.agon.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class AccountType(val arabicName: String) {
    PERSONAL("شخصي"),
    STORE("متجر"),
    COMPANY("شركة"),
    SAVINGS("ادخار")
}

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType
)

@Serializable
enum class TransactionType {
    INCOME, // مدخول
    EXPENSE // مصروف
}

@Serializable
enum class PaymentMethod(val arabicName: String) {
    CASH("نقداً"),
    BANK_TRANSFER("تحويل بنكي"),
    CARD("بطاقة"),
    E_WALLET("محفظة إلكترونية")
}

@Serializable
data class TransactionCategory(
    val id: String,
    val name: String,
    val iconName: String, // e.g. "shopping_cart", "restaurant", "directions_car", etc.
    val colorHex: String, // e.g. "#FF5722"
    val budgetLimit: Double? = null // ميزانية شهرية
)

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val note: String,
    val paymentMethod: PaymentMethod,
    val imageUri: String? = null // صورة فاتورة اختيارية
)

@Serializable
enum class DebtType {
    OWED_TO_ME, // لي (دائن)
    OWED_BY_ME  // علي (مدين)
}

@Serializable
enum class DebtStatus(val arabicName: String) {
    PAID("مدفوع"),
    PARTIAL("جزئي"),
    OVERDUE("متأخر"),
    UNPAID("غير مدفوع")
}

@Serializable
data class DebtPayment(
    val id: String,
    val amount: Double,
    val date: String
)

@Serializable
data class Debt(
    val id: String,
    val accountId: String,
    val name: String,
    val phone: String,
    val amount: Double,
    val type: DebtType,
    val dueDate: String, // YYYY-MM-DD
    val note: String,
    val status: DebtStatus,
    val payments: List<DebtPayment> = emptyList()
) {
    val totalPaid: Double
        get() = payments.sumOf { it.amount }

    val remainingAmount: Double
        get() = (amount - totalPaid).coerceAtLeast(0.0)
}

@Serializable
enum class NotificationType {
    DEBT_REMINDER,
    BUDGET_ALERT,
    SYSTEM
}

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val date: String,
    val isRead: Boolean = false,
    val type: NotificationType
)

@Serializable
data class AppUser(
    val name: String,
    val email: String,
    val pin: String,
    val isLoggedIn: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val profileImage: String? = null
)

@Serializable
data class DaftarState(
    val user: AppUser = AppUser("", "", "", false, false, null),
    val accounts: List<Account> = emptyList(),
    val categories: List<TransactionCategory> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val currentAccountId: String? = null
)
