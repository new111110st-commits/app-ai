package com.agon.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.agon.app.data.*
import com.agon.app.ui.components.CommonComponents
import com.agon.app.ui.theme.*
import com.agon.app.viewmodel.DaftarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: DaftarViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Screen states
    var showAccountMenu by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountType by remember { mutableStateOf(AccountType.PERSONAL) }

    // Selected Transaction Detail BottomSheet
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Data calculations filtered by current account
    val currentAccount = state.accounts.find { it.id == state.currentAccountId } ?: state.accounts.firstOrNull()
    val accountTransactions = state.transactions.filter { it.accountId == currentAccount?.id }
    val accountDebts = state.debts.filter { it.accountId == currentAccount?.id }

    val totalIncome = accountTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = accountTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val currentBalance = totalIncome - totalExpense

    val owedToMe = accountDebts.filter { it.type == DebtType.OWED_TO_ME }.sumOf { it.remainingAmount }
    val owedByMe = accountDebts.filter { it.type == DebtType.OWED_BY_ME }.sumOf { it.remainingAmount }

    // Unread notifications count
    val unreadNotifications = state.notifications.count { !it.isRead }

    // Rotating Sync Icon Animation
    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotationAngle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "أهلاً بك، ${state.user.name.ifBlank { "عبد الرحمن" }} 👋",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showAccountMenu = true }
                        ) {
                            Text(
                                text = currentAccount?.name ?: "اختر الحساب",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "حسابات",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Account Dropdown Menu
                        DropdownMenu(
                            expanded = showAccountMenu,
                            onDismissRequest = { showAccountMenu = false }
                        ) {
                            state.accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        viewModel.setCurrentAccount(acc.id)
                                        showAccountMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when(acc.type) {
                                                AccountType.PERSONAL -> Icons.Default.Person
                                                AccountType.STORE -> Icons.Default.Storefront
                                                AccountType.COMPANY -> Icons.Default.Business
                                                AccountType.SAVINGS -> Icons.Default.AccountBalance
                                            },
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("إضافة حساب جديد +") },
                                onClick = {
                                    showAccountMenu = false
                                    showAddAccountDialog = true
                                }
                            )
                        }
                    }
                },
                actions = {
                    // Sync Button
                    IconButton(onClick = { viewModel.syncDataSimulated() }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "مزامنة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(if (isSyncing) rotationAngle else 0f)
                        )
                    }

                    // Notifications Button with Badge
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge { Text(unreadNotifications.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "الإشعارات",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_transaction") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة عملية", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Balance Overview Card
            item {
                BalanceOverviewCard(
                    balance = currentBalance,
                    income = totalIncome,
                    expense = totalExpense,
                    owedToMe = owedToMe,
                    owedByMe = owedByMe
                )
            }

            // 2. Interactive Cash Flow Line Chart
            item {
                DashboardLineChart(transactions = accountTransactions)
            }

            // 3. Section Title "Recent Transactions"
            item {
                CommonComponents.SectionHeader(
                    title = "آخر العمليات المالية 🧾",
                    actionText = "إدارة التصنيفات 📁",
                    onActionClick = { navController.navigate("categories") }
                )
            }

            // 4. Transactions List
            if (accountTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد عمليات مالية مسجلة بعد",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(accountTransactions.take(5)) { tx ->
                    val category = state.categories.find { it.id == tx.categoryId }
                    TransactionRowItem(
                        transaction = tx,
                        category = category,
                        onClick = { selectedTransaction = tx }
                    )
                }
            }
            
            // Extra spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // --- DIALOGS & BOTTOM SHEETS ---

    // Add Account Dialog
    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("إضافة حساب جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        label = { Text("اسم الحساب (مثال: متجري الخاص)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("نوع الحساب:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AccountType.values().forEach { type ->
                            val selected = newAccountType == type
                            FilterChip(
                                selected = selected,
                                onClick = { newAccountType = type },
                                label = { Text(type.arabicName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAccountName.isNotBlank()) {
                            viewModel.addAccount(newAccountName, newAccountType)
                            newAccountName = ""
                            showAddAccountDialog = false
                            Toast.makeText(context, "تمت إضافة الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Transaction Details Dialog
    if (selectedTransaction != null) {
        val tx = selectedTransaction!!
        val category = state.categories.find { it.id == tx.categoryId }

        AlertDialog(
            onDismissRequest = { selectedTransaction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    category?.let {
                        CommonComponents.CategoryIcon(iconName = it.iconName, colorHex = it.colorHex)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text("تفاصيل العملية المالية")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(label = "المبلغ:", value = CommonComponents.formatCurrency(tx.amount), valueColor = if (tx.type == TransactionType.INCOME) ColorIncome else ColorExpense)
                    DetailRow(label = "النوع:", value = if (tx.type == TransactionType.INCOME) "مدخول (+)" else "مصروف (-)")
                    DetailRow(label = "التصنيف:", value = category?.name ?: "غير محدد")
                    DetailRow(label = "طريقة الدفع:", value = tx.paymentMethod.arabicName)
                    DetailRow(label = "التاريخ والوقت:", value = "${tx.date} | ${tx.time}")
                    if (tx.note.isNotBlank()) {
                        DetailRow(label = "ملاحظة:", value = tx.note)
                    }
                    if (tx.imageUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("صورة الفاتورة/المستند:", style = MaterialTheme.typography.labelLarge)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Text("مرفق فاتورة تجريبية (صورة)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 40.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedTransaction = null }
                ) {
                    Text("إغلاق")
                }
            },
            dismissButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        selectedTransaction = null
                        Toast.makeText(context, "تم حذف العملية بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف")
                }
            }
        )
    }
}

@Composable
fun BalanceOverviewCard(
    balance: Double,
    income: Double,
    expense: Double,
    owedToMe: Double,
    owedByMe: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "إجمالي الرصيد الحالي",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CommonComponents.formatCurrency(balance),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            // Income & Expense Row
            Row(modifier = Modifier.fillMaxWidth()) {
                BalanceStatItem(
                    title = "مجموع المدخولات",
                    amount = income,
                    icon = Icons.Default.ArrowUpward,
                    color = ColorIncome,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        .align(Alignment.CenterVertically)
                )
                BalanceStatItem(
                    title = "مجموع المصروفات",
                    amount = expense,
                    icon = Icons.Default.ArrowDownward,
                    color = ColorExpense,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Debts Row
            Row(modifier = Modifier.fillMaxWidth()) {
                BalanceStatItem(
                    title = "ديون لك (تطلبها)",
                    amount = owedToMe,
                    icon = Icons.Default.TrendingUp,
                    color = ColorDebt,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        .align(Alignment.CenterVertically)
                )
                BalanceStatItem(
                    title = "ديون عليك (تؤديها)",
                    amount = owedByMe,
                    icon = Icons.Default.TrendingDown,
                    color = ColorDebt,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BalanceStatItem(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = CommonComponents.formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun DashboardLineChart(transactions: List<Transaction>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "حركة حركة الأموال (آخر 7 عمليات)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Generate values for chart
            val lastTx = transactions.takeLast(7)
            if (lastTx.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تحتاج لعمليتين على الأقل لعرض الرسم البياني",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (lastTx.size - 1)

                    // Calculate min and max amount to scale properly
                    val amounts = lastTx.map { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
                    val minVal = amounts.minOrNull() ?: 0.0
                    val maxVal = amounts.maxOrNull() ?: 1.0
                    val valRange = (maxVal - minVal).coerceAtLeast(1.0)

                    val points = amounts.mapIndexed { idx, amt ->
                        val x = idx * spacing
                        val y = height - (((amt - minVal) / valRange) * height).toFloat()
                        Offset(x, y)
                    }

                    // Draw smooth line
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val pPrev = points[i - 1]
                            val pCurr = points[i]
                            val cpX1 = pPrev.x + (pCurr.x - pPrev.x) / 2f
                            cubicTo(cpX1, pPrev.y, cpX1, pCurr.y, pCurr.x, pCurr.y)
                        }
                    }

                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw points
                    points.forEachIndexed { index, point ->
                        val color = if (lastTx[index].type == TransactionType.INCOME) ColorIncome else ColorExpense
                        drawCircle(
                            color = color,
                            radius = 5.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    category: TransactionCategory?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                category?.let {
                    CommonComponents.CategoryIcon(iconName = it.iconName, colorHex = it.colorHex)
                } ?: Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (transaction.note.isNotBlank()) transaction.note else (category?.name ?: "عملية مالية"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.paymentMethod.arabicName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${transaction.date} | ${transaction.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = (if (transaction.type == TransactionType.INCOME) "+" else "-") + CommonComponents.formatCurrency(transaction.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (transaction.type == TransactionType.INCOME) ColorIncome else ColorExpense
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}
