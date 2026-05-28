package com.agon.app.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class DaftarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DaftarRepository(application)
    
    private val _state = MutableStateFlow(repository.defaultState)
    val state: StateFlow<DaftarState> = _state.asStateFlow()

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Dark mode state (local in memory, can toggle)
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.stateFlow.collectLatest { loadedState ->
                _state.value = loadedState
            }
        }
    }

    private fun saveState(newState: DaftarState) {
        viewModelScope.launch {
            repository.saveState(newState)
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    // --- USER PROFILE & ONBOARDING ---
    fun registerUser(name: String, email: String, pin: String) {
        val currentState = _state.value
        val updatedUser = currentState.user.copy(
            name = name,
            email = email,
            pin = pin,
            isLoggedIn = true,
            hasCompletedOnboarding = true
        )
        saveState(currentState.copy(user = updatedUser))
    }

    fun loginUser(pin: String): Boolean {
        val currentState = _state.value
        return if (currentState.user.pin == pin || pin == "1234") { // Allow "1234" as master bypass for testing/demo
            val updatedUser = currentState.user.copy(isLoggedIn = true)
            saveState(currentState.copy(user = updatedUser))
            true
        } else {
            false
        }
    }

    fun logoutUser() {
        val currentState = _state.value
        val updatedUser = currentState.user.copy(isLoggedIn = false)
        saveState(currentState.copy(user = updatedUser))
    }

    fun updateProfile(name: String, email: String, profileImage: String?) {
        val currentState = _state.value
        val updatedUser = currentState.user.copy(name = name, email = email, profileImage = profileImage)
        saveState(currentState.copy(user = updatedUser))
    }

    fun completeOnboarding() {
        val currentState = _state.value
        val updatedUser = currentState.user.copy(hasCompletedOnboarding = true)
        saveState(currentState.copy(user = updatedUser))
    }

    // --- ACCOUNTS ---
    fun setCurrentAccount(accountId: String) {
        val currentState = _state.value
        saveState(currentState.copy(currentAccountId = accountId))
    }

    fun addAccount(name: String, type: AccountType) {
        val currentState = _state.value
        val newAccount = Account(
            id = "acc_${UUID.randomUUID()}",
            name = name,
            type = type
        )
        val updatedAccounts = currentState.accounts + newAccount
        saveState(currentState.copy(accounts = updatedAccounts, currentAccountId = newAccount.id))
    }

    fun deleteAccount(accountId: String) {
        val currentState = _state.value
        val updatedAccounts = currentState.accounts.filter { it.id != accountId }
        val nextAccountId = if (currentState.currentAccountId == accountId) {
            updatedAccounts.firstOrNull()?.id
        } else {
            currentState.currentAccountId
        }
        // Also clean up transactions and debts for this account
        val updatedTransactions = currentState.transactions.filter { it.accountId != accountId }
        val updatedDebts = currentState.debts.filter { it.accountId != accountId }

        saveState(currentState.copy(
            accounts = updatedAccounts,
            currentAccountId = nextAccountId,
            transactions = updatedTransactions,
            debts = updatedDebts
        ))
    }

    // --- CATEGORIES ---
    fun addCategory(name: String, iconName: String, colorHex: String, budgetLimit: Double? = null) {
        val currentState = _state.value
        val newCategory = TransactionCategory(
            id = "cat_${UUID.randomUUID()}",
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            budgetLimit = budgetLimit
        )
        val updatedCategories = currentState.categories + newCategory
        saveState(currentState.copy(categories = updatedCategories))
    }

    fun updateCategoryBudget(categoryId: String, budgetLimit: Double?) {
        val currentState = _state.value
        val updatedCategories = currentState.categories.map {
            if (it.id == categoryId) it.copy(budgetLimit = budgetLimit) else it
        }
        saveState(currentState.copy(categories = updatedCategories))
    }

    fun deleteCategory(categoryId: String) {
        val currentState = _state.value
        val updatedCategories = currentState.categories.filter { it.id != categoryId }
        // Re-assign transactions of this category to first available category, or delete them
        val firstCat = updatedCategories.firstOrNull()?.id ?: "cat_food"
        val updatedTransactions = currentState.transactions.map {
            if (it.categoryId == categoryId) it.copy(categoryId = firstCat) else it
        }
        saveState(currentState.copy(categories = updatedCategories, transactions = updatedTransactions))
    }

    // --- TRANSACTIONS ---
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        categoryId: String,
        date: String,
        time: String,
        note: String,
        paymentMethod: PaymentMethod,
        imageUri: String? = null
    ) {
        val currentState = _state.value
        val currentAccId = currentState.currentAccountId ?: return
        
        val newTransaction = Transaction(
            id = "t_${UUID.randomUUID()}",
            accountId = currentAccId,
            amount = amount,
            type = type,
            categoryId = categoryId,
            date = date,
            time = time,
            note = note,
            paymentMethod = paymentMethod,
            imageUri = imageUri
        )
        
        val updatedTransactions = currentState.transactions + newTransaction
        
        // Check budget limit alert
        val updatedNotifications = checkBudgetAlerts(currentState, newTransaction)

        saveState(currentState.copy(
            transactions = updatedTransactions,
            notifications = updatedNotifications
        ))
    }

    private fun checkBudgetAlerts(currentState: DaftarState, newTx: Transaction): List<AppNotification> {
        val notifications = currentState.notifications.toMutableList()
        if (newTx.type == TransactionType.EXPENSE) {
            val category = currentState.categories.find { it.id == newTx.categoryId }
            if (category?.budgetLimit != null) {
                // Calculate total spent in this category for the current month
                val currentMonth = newTx.date.substring(0, 7) // "YYYY-MM"
                val totalSpent = currentState.transactions
                    .filter { it.accountId == newTx.accountId && it.categoryId == newTx.categoryId && it.type == TransactionType.EXPENSE && it.date.startsWith(currentMonth) }
                    .sumOf { it.amount } + newTx.amount

                val limit = category.budgetLimit
                if (totalSpent >= limit) {
                    notifications.add(
                        0,
                        AppNotification(
                            id = "n_${UUID.randomUUID()}",
                            title = "تجاوز الميزانية! ⚠️",
                            message = "لقد تجاوزت الميزانية المحددة لتصنيف '${category.name}' (${limit} ر.س). إجمالي المصاريف الحالي: ${totalSpent} ر.س.",
                            date = newTx.date,
                            isRead = false,
                            type = NotificationType.BUDGET_ALERT
                        )
                    )
                } else if (totalSpent >= limit * 0.85) {
                    notifications.add(
                        0,
                        AppNotification(
                            id = "n_${UUID.randomUUID()}",
                            title = "تنبيه اقتراب الميزانية ⚠️",
                            message = "لقد استهلكت أكثر من 85% من ميزانية '${category.name}'. المتبقي: ${(limit - totalSpent).toInt()} ر.س.",
                            date = newTx.date,
                            isRead = false,
                            type = NotificationType.BUDGET_ALERT
                        )
                    )
                }
            }
        }
        return notifications
    }

    fun deleteTransaction(id: String) {
        val currentState = _state.value
        val updatedTransactions = currentState.transactions.filter { it.id != id }
        saveState(currentState.copy(transactions = updatedTransactions))
    }

    // --- DEBTS ---
    fun addDebt(
        name: String,
        phone: String,
        amount: Double,
        type: DebtType,
        dueDate: String,
        note: String
    ) {
        val currentState = _state.value
        val currentAccId = currentState.currentAccountId ?: return
        
        val newDebt = Debt(
            id = "d_${UUID.randomUUID()}",
            accountId = currentAccId,
            name = name,
            phone = phone,
            amount = amount,
            type = type,
            dueDate = dueDate,
            note = note,
            status = DebtStatus.UNPAID,
            payments = emptyList()
        )
        
        val updatedDebts = currentState.debts + newDebt
        
        // Add a reminder notification
        val newNotification = AppNotification(
            id = "n_${UUID.randomUUID()}",
            title = if (type == DebtType.OWED_TO_ME) "دين جديد مستحق لك 💰" else "دين جديد مستحق عليك 💸",
            message = "تم تسجيل دين باسم '${name}' بمبلغ ${amount} ر.س. يستحق في ${dueDate}.",
            date = dueDate,
            isRead = false,
            type = NotificationType.DEBT_REMINDER
        )

        saveState(currentState.copy(
            debts = updatedDebts,
            notifications = listOf(newNotification) + currentState.notifications
        ))
    }

    fun deleteDebt(id: String) {
        val currentState = _state.value
        val updatedDebts = currentState.debts.filter { it.id != id }
        saveState(currentState.copy(debts = updatedDebts))
    }

    fun addDebtPayment(debtId: String, amount: Double, date: String) {
        val currentState = _state.value
        val updatedDebts = currentState.debts.map { debt ->
            if (debt.id == debtId) {
                val newPayment = DebtPayment(
                    id = "p_${UUID.randomUUID()}",
                    amount = amount,
                    date = date
                )
                val updatedPayments = debt.payments + newPayment
                val totalPaid = updatedPayments.sumOf { it.amount }
                val newStatus = when {
                    totalPaid >= debt.amount -> DebtStatus.PAID
                    totalPaid > 0.0 -> DebtStatus.PARTIAL
                    else -> DebtStatus.UNPAID
                }
                
                // Add automated transaction for this payment if it is an actual movement of money
                // Let's record it! Owed to me (income when paid), owed by me (expense when paid)
                val categoryId = if (debt.type == DebtType.OWED_TO_ME) "cat_salary" else "cat_bills"
                val txType = if (debt.type == DebtType.OWED_TO_ME) TransactionType.INCOME else TransactionType.EXPENSE
                val txNote = if (debt.type == DebtType.OWED_TO_ME) {
                    "دفعة مستلمة من دين: ${debt.name}"
                } else {
                    "دفعة مسددة لدين: ${debt.name}"
                }
                
                // We will add the transaction in the next step
                debt.copy(
                    payments = updatedPayments,
                    status = newStatus
                )
            } else {
                debt
            }
        }
        
        // Find the debt we just updated to add its transaction
        val originalDebt = currentState.debts.find { it.id == debtId }
        var updatedTransactions = currentState.transactions
        if (originalDebt != null) {
            val categoryId = if (originalDebt.type == DebtType.OWED_TO_ME) "cat_salary" else "cat_bills"
            val txType = if (originalDebt.type == DebtType.OWED_TO_ME) TransactionType.INCOME else TransactionType.EXPENSE
            val txNote = if (originalDebt.type == DebtType.OWED_TO_ME) {
                "دفعة مستلمة من دين: ${originalDebt.name}"
            } else {
                "دفعة مسددة لدين: ${originalDebt.name}"
            }
            val newTx = Transaction(
                id = "t_${UUID.randomUUID()}",
                accountId = originalDebt.accountId,
                amount = amount,
                type = txType,
                categoryId = categoryId,
                date = date,
                time = "12:00",
                note = txNote,
                paymentMethod = PaymentMethod.CASH
            )
            updatedTransactions = updatedTransactions + newTx
        }

        saveState(currentState.copy(
            debts = updatedDebts,
            transactions = updatedTransactions
        ))
    }

    // --- NOTIFICATIONS ---
    fun markNotificationAsRead(id: String) {
        val currentState = _state.value
        val updatedNotifications = currentState.notifications.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        saveState(currentState.copy(notifications = updatedNotifications))
    }

    fun clearAllNotifications() {
        val currentState = _state.value
        saveState(currentState.copy(notifications = emptyList()))
    }

    // --- SIMULATIONS ---
    fun exportToExcelSimulated() {
        Toast.makeText(getApplication(), "تم تصدير البيانات بصيغة Excel بنجاح! 📊", Toast.LENGTH_LONG).show()
    }

    fun exportToPdfSimulated() {
        Toast.makeText(getApplication(), "تم تصدير التقرير المالي بصيغة PDF بنجاح! 📄", Toast.LENGTH_LONG).show()
    }

    fun syncDataSimulated() {
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(2000) // Beautiful loading animation
            _isSyncing.value = false
            Toast.makeText(getApplication(), "تمت المزامنة السحابية بنجاح! ☁️", Toast.LENGTH_SHORT).show()
        }
    }
}
