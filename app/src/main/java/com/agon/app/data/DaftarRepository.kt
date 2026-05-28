package com.agon.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "daftar_plus_prefs")

class DaftarRepository(private val context: Context) {

    private val stateKey = stringPreferencesKey("daftar_state")
    private val json = Json { ignoreUnknownKeys = true }

    // Initial rich Arabic sample data
    private val initialUser = AppUser(
        name = "عبد الرحمن المالي",
        email = "ar.finance@daftar.plus",
        pin = "1234",
        isLoggedIn = false,
        hasCompletedOnboarding = false,
        profileImage = null
    )

    private val initialAccounts = listOf(
        Account("acc_1", "الحساب الشخصي 👤", AccountType.PERSONAL),
        Account("acc_2", "متجر الأناقة 👜", AccountType.STORE),
        Account("acc_3", "شركة البرمجة 💻", AccountType.COMPANY),
        Account("acc_4", "صندوق الادخار 🏦", AccountType.SAVINGS)
    )

    private val initialCategories = listOf(
        TransactionCategory("cat_food", "طعام وشراب", "restaurant", "#FF7043", 600.0),
        TransactionCategory("cat_shopping", "تسوق ومشتريات", "shopping_bag", "#9C27B0", 1200.0),
        TransactionCategory("cat_bills", "فواتير وسكن", "receipt_long", "#2196F3", 400.0),
        TransactionCategory("cat_trans", "مواصلات", "directions_car", "#FFB300", 250.0),
        TransactionCategory("cat_salary", "راتب شهري", "payments", "#4CAF50"),
        TransactionCategory("cat_freelance", "عمل حر", "laptop_mac", "#009688"),
        TransactionCategory("cat_investment", "استثمار", "trending_up", "#E91E63")
    )

    private val initialTransactions = listOf(
        Transaction("t1", "acc_1", 7500.0, TransactionType.INCOME, "cat_salary", "2025-02-15", "08:30", "الراتب الأساسي لشهر فبراير", PaymentMethod.BANK_TRANSFER),
        Transaction("t2", "acc_1", 120.0, TransactionType.EXPENSE, "cat_food", "2025-02-16", "14:15", "غداء عمل مع الزملاء", PaymentMethod.CARD),
        Transaction("t3", "acc_1", 350.0, TransactionType.EXPENSE, "cat_shopping", "2025-02-17", "18:45", "شراء حذاء رياضي", PaymentMethod.CARD),
        Transaction("t4", "acc_1", 180.0, TransactionType.EXPENSE, "cat_bills", "2025-02-18", "10:00", "فاتورة الهاتف والإنترنت", PaymentMethod.E_WALLET),
        Transaction("t5", "acc_1", 45.0, TransactionType.EXPENSE, "cat_trans", "2025-02-19", "09:15", "تعبئة رصيد بطاقة المترو", PaymentMethod.CASH),
        Transaction("t6", "acc_1", 1500.0, TransactionType.INCOME, "cat_freelance", "2025-02-20", "16:00", "تصميم واجهات متجر إلكتروني", PaymentMethod.BANK_TRANSFER),
        Transaction("t7", "acc_1", 200.0, TransactionType.EXPENSE, "cat_food", "2025-02-21", "20:30", "عشاء عائلي نهاية الأسبوع", PaymentMethod.CASH),
        
        Transaction("t8", "acc_2", 4500.0, TransactionType.INCOME, "cat_freelance", "2025-02-15", "10:00", "مبيعات فساتين سهرة", PaymentMethod.BANK_TRANSFER),
        Transaction("t9", "acc_2", 800.0, TransactionType.EXPENSE, "cat_bills", "2025-02-16", "11:00", "إيجار ركن العرض الأسبوعي", PaymentMethod.CARD),
        Transaction("t10", "acc_2", 150.0, TransactionType.EXPENSE, "cat_shopping", "2025-02-18", "15:30", "أكياس ومواد تغليف جديدة", PaymentMethod.CASH)
    )

    private val initialDebts = listOf(
        Debt("d1", "acc_1", "عبد الله السديري", "+966501122334", 1500.0, DebtType.OWED_TO_ME, "2025-03-10", "سلفة لشراء مستلزمات منزلية", DebtStatus.PARTIAL, listOf(DebtPayment("p1", 500.0, "2025-02-18"))),
        Debt("d2", "acc_1", "محمد الشهري", "+966505566778", 2000.0, DebtType.OWED_BY_ME, "2025-02-25", "باقي قيمة شراء حاسوب محمول", DebtStatus.UNPAID),
        Debt("d3", "acc_1", "فهد الحربي", "+966509988776", 300.0, DebtType.OWED_TO_ME, "2025-02-10", "قيمة كتاب مستعار", DebtStatus.PAID, listOf(DebtPayment("p2", 300.0, "2025-02-10"))),
        Debt("d4", "acc_2", "مؤسسة التوريد الحديثة", "+966501234567", 5000.0, DebtType.OWED_BY_ME, "2025-03-01", "دفعة بضاعة العيد", DebtStatus.PARTIAL, listOf(DebtPayment("p3", 2000.0, "2025-02-12")))
    )

    private val initialNotifications = listOf(
        AppNotification("n1", "تذكير بسداد دين ⏰", "يقترب موعد استحقاق دين عبد الله السديري (المتبقي: 1000 ريال) في 2025-03-10", "2025-02-21", false, NotificationType.DEBT_REMINDER),
        AppNotification("n2", "تنبيه الميزانية ⚠️", "لقد استهلكت 85% من ميزانية 'طعام وشراب' لهذا الشهر.", "2025-02-20", false, NotificationType.BUDGET_ALERT),
        AppNotification("n3", "مرحباً بك في دفتر+ 🎉", "ابدأ الآن بتنظيم حساباتك الشخصية والتجارية بكل سهولة وأمان.", "2025-02-15", true, NotificationType.SYSTEM)
    )

    val defaultState = DaftarState(
        user = initialUser,
        accounts = initialAccounts,
        categories = initialCategories,
        transactions = initialTransactions,
        debts = initialDebts,
        notifications = initialNotifications,
        currentAccountId = "acc_1"
    )

    val stateFlow: Flow<DaftarState> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[stateKey]
        if (jsonStr == null) {
            defaultState
        } else {
            try {
                json.decodeFromString<DaftarState>(jsonStr)
            } catch (e: Exception) {
                defaultState
            }
        }
    }

    suspend fun saveState(state: DaftarState) {
        context.dataStore.edit { preferences ->
            preferences[stateKey] = json.encodeToString(state)
        }
    }
}
