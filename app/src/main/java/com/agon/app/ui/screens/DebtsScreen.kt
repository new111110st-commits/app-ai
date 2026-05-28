package com.agon.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agon.app.data.*
import com.agon.app.ui.components.CommonComponents
import com.agon.app.ui.theme.*
import com.agon.app.viewmodel.DaftarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(navController: NavController, viewModel: DaftarViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Owed to me, 2: Owed by me, 3: Overdue, 4: Paid

    // Dialog states
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf<Debt?>(null) }
    var showDebtDetailsDialog by remember { mutableStateOf<Debt?>(null) }

    // Add Debt Form inputs
    var debtorName by remember { mutableStateOf("") }
    var debtorPhone by remember { mutableStateOf("") }
    var debtAmount by remember { mutableStateOf("") }
    var debtType by remember { mutableStateOf(DebtType.OWED_TO_ME) }
    var debtDueDate by remember { mutableStateOf("") }
    var debtNote by remember { mutableStateOf("") }

    // Add Payment Form inputs
    var paymentAmount by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf("") }

    // Filter & Search Debts logic
    val currentAccountDebts = state.debts.filter { it.accountId == state.currentAccountId }
    val filteredDebts = currentAccountDebts.filter { debt ->
        val matchesSearch = debt.name.contains(searchQuery, ignoreCase = true) || debt.note.contains(searchQuery, ignoreCase = true)
        val matchesTab = when (selectedTab) {
            0 -> true
            1 -> debt.type == DebtType.OWED_TO_ME
            2 -> debt.type == DebtType.OWED_BY_ME
            3 -> debt.status == DebtStatus.OVERDUE || (debt.status != DebtStatus.PAID && isOverdueDate(debt.dueDate))
            4 -> debt.status == DebtStatus.PAID
            else -> true
        }
        matchesSearch && matchesTab
    }

    // Default dates
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        debtDueDate = sdf.format(Date(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000)) // 14 days later
        paymentDate = sdf.format(Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الديون والمديونين 💰", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDebtDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة دين", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث باسم الشخص أو الملاحظة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                val tabs = listOf("الكل", "لي (دائن) 📥", "علي (مدين) 📤", "متأخرة ⚠️", "مسددة ✅")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // 3. Debts List
            if (filteredDebts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد ديون مطابقة للمواصفات",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDebts) { debt ->
                        DebtRowItem(
                            debt = debt,
                            onClick = { showDebtDetailsDialog = debt },
                            onCallClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${debt.phone}"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "اتصال بـ ${debt.name}: ${debt.phone}", Toast.LENGTH_LONG).show()
                                }
                            },
                            onPaymentClick = { showAddPaymentDialog = debt },
                            onDeleteClick = {
                                viewModel.deleteDebt(debt.id)
                                Toast.makeText(context, "تم حذف الدين بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Add Debt Dialog
    if (showAddDebtDialog) {
        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text("تسجيل دين جديد 📝") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = debtorName,
                        onValueChange = { debtorName = it },
                        label = { Text("اسم الشخص") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = debtorPhone,
                        onValueChange = { debtorPhone = it },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = debtAmount,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) debtAmount = it },
                        label = { Text("مبلغ الدين الإجمالي") },
                        leadingIcon = { Text("ر.س", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Debt Type Toggle
                    Text("نوع الدين:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (debtType == DebtType.OWED_TO_ME) ColorIncome else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { debtType = DebtType.OWED_TO_ME },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("أنا الدائن (أطلب مالي) 📥", color = if (debtType == DebtType.OWED_TO_ME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (debtType == DebtType.OWED_BY_ME) ColorExpense else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { debtType = DebtType.OWED_BY_ME },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("أنا المدين (عليّ مال) 📤", color = if (debtType == DebtType.OWED_BY_ME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    OutlinedTextField(
                        value = debtDueDate,
                        onValueChange = { debtDueDate = it },
                        label = { Text("تاريخ الاستحقاق (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = debtNote,
                        onValueChange = { debtNote = it },
                        label = { Text("ملاحظات (سبب الدين)") },
                        leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = debtAmount.toDoubleOrNull()
                        if (debtorName.isBlank() || amount == null || amount <= 0.0) {
                            Toast.makeText(context, "الرجاء ملء البيانات بشكل صحيح!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addDebt(
                                name = debtorName,
                                phone = debtorPhone,
                                amount = amount,
                                type = debtType,
                                dueDate = debtDueDate,
                                note = debtNote
                            )
                            Toast.makeText(context, "تم تسجيل الدين بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                            debtorName = ""
                            debtorPhone = ""
                            debtAmount = ""
                            debtNote = ""
                            showAddDebtDialog = false
                        }
                    }
                ) {
                    Text("تسجيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Payment Dialog
    if (showAddPaymentDialog != null) {
        val debt = showAddPaymentDialog!!
        AlertDialog(
            onDismissRequest = { showAddPaymentDialog = null },
            title = { Text("تسجيل دفعة سداد جديدة 💵") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("سداد جزء من دين: ${debt.name}", style = MaterialTheme.typography.bodyMedium)
                    Text("المبلغ المتبقي: ${CommonComponents.formatCurrency(debt.remainingAmount)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) paymentAmount = it },
                        label = { Text("مبلغ الدفعة") },
                        leadingIcon = { Text("ر.س", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("تاريخ الدفع (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0.0 || amount > debt.remainingAmount) {
                            Toast.makeText(context, "الرجاء إدخال مبلغ دفع صحيح لا يتجاوز المتبقي!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addDebtPayment(debt.id, amount, paymentDate)
                            Toast.makeText(context, "تم تسجيل دفعة السداد بنجاح! ✅", Toast.LENGTH_SHORT).show()
                            paymentAmount = ""
                            showAddPaymentDialog = null
                        }
                    }
                ) {
                    Text("سداد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPaymentDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Debt Details Dialog
    if (showDebtDetailsDialog != null) {
        val debt = showDebtDetailsDialog!!
        AlertDialog(
            onDismissRequest = { showDebtDetailsDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (debt.type == DebtType.OWED_TO_ME) ColorIncome.copy(alpha = 0.15f) else ColorExpense.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (debt.type == DebtType.OWED_TO_ME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (debt.type == DebtType.OWED_TO_ME) ColorIncome else ColorExpense
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(debt.name)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    DetailRow(label = "نوع الدين:", value = if (debt.type == DebtType.OWED_TO_ME) "مستحق لك (دائن)" else "مستحق عليك (مدين)")
                    DetailRow(label = "المبلغ الإجمالي:", value = CommonComponents.formatCurrency(debt.amount))
                    DetailRow(label = "المدفوع حتى الآن:", value = CommonComponents.formatCurrency(debt.totalPaid), valueColor = ColorIncome)
                    DetailRow(label = "المبلغ المتبقي:", value = CommonComponents.formatCurrency(debt.remainingAmount), valueColor = ColorExpense)
                    DetailRow(label = "تاريخ الاستحقاق:", value = debt.dueDate)
                    DetailRow(label = "الحالة:", value = debt.status.arabicName)
                    if (debt.note.isNotBlank()) {
                        DetailRow(label = "ملاحظات:", value = debt.note)
                    }

                    HorizontalDivider()
                    Text("سجل الدفعات المستلمة/المسددة 🧾:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    
                    if (debt.payments.isEmpty()) {
                        Text("لا توجد دفعات مسجلة بعد لهذا الدين.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        debt.payments.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(p.date, style = MaterialTheme.typography.bodySmall)
                                Text(CommonComponents.formatCurrency(p.amount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = ColorIncome)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDebtDetailsDialog = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun DebtRowItem(
    debt: Debt,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = debt.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تاريخ الاستحقاق: ${debt.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CommonComponents.StatusBadge(status = debt.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Progress & Money Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("المبلغ الإجمالي: ${CommonComponents.formatCurrency(debt.amount)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "المتبقي: ${CommonComponents.formatCurrency(debt.remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (debt.type == DebtType.OWED_TO_ME) ColorIncome else ColorExpense
                    )
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Button
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "اتصال", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    // Add Payment Button (only if not fully paid)
                    if (debt.status != DebtStatus.PAID) {
                        IconButton(
                            onClick = onPaymentClick,
                            modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ColorIncome.copy(alpha = 0.1f))
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = "سداد دفعة", tint = ColorIncome, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ColorExpense.copy(alpha = 0.1f))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = ColorExpense, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

fun isOverdueDate(dateStr: String): Boolean {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val dueDate = sdf.parse(dateStr)
        dueDate?.before(Date()) ?: false
    } catch (e: Exception) {
        false
    }
}
