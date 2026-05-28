package com.agon.app.ui.screens

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun ReportsScreen(navController: NavController, viewModel: DaftarViewModel) {
    val state by viewModel.state.collectAsState()

    // Reports state
    var selectedPeriodTab by remember { mutableStateOf(2) } // 0: Daily, 1: Weekly, 2: Monthly, 3: Yearly
    var chartTypeTab by remember { mutableStateOf(0) } // 0: Pie Chart, 1: Bar Chart

    // Filter transactions based on selected period
    val currentAccountTransactions = state.transactions.filter { it.accountId == state.currentAccountId }
    val filteredTransactions = remember(currentAccountTransactions, selectedPeriodTab) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Date()
        currentAccountTransactions.filter { tx ->
            try {
                val txDate = sdf.parse(tx.date) ?: return@filter false
                val diffInMillis = now.time - txDate.time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                when (selectedPeriodTab) {
                    0 -> diffInDays <= 1 // Daily
                    1 -> diffInDays <= 7 // Weekly
                    2 -> diffInDays <= 30 // Monthly
                    3 -> diffInDays <= 365 // Yearly
                    else -> true
                }
            } catch (e: Exception) {
                true
            }
        }
    }

    // Calculations
    val totalIncome = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val netProfit = totalIncome - totalExpense

    // Group expenses by category
    val categoryExpenses = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val category = state.categories.find { it.id == catId }
                val amount = txs.sumOf { it.amount }
                category to amount
            }
            .filter { it.first != null }
            .sortedByDescending { it.second }
    }

    val totalCategoryExpense = categoryExpenses.sumOf { it.second }

    // Animations for charts
    var animateCharts by remember { mutableStateOf(false) }
    val chartProgress by animateFloatAsState(
        targetValue = if (animateCharts) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "chartProgress"
    )

    LaunchedEffect(selectedPeriodTab, chartTypeTab) {
        animateCharts = false
        kotlinx.coroutines.delay(100)
        animateCharts = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير والإحصائيات الذكية 📊", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                actions = {
                    IconButton(onClick = { viewModel.exportToExcelSimulated() }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "تصدير Excel", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedPeriodTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    val tabs = listOf("يومي", "أسبوعي", "شهري", "سنوي")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedPeriodTab == index,
                            onClick = { selectedPeriodTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // 2. Net Profit Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (netProfit >= 0) ColorIncome.copy(alpha = 0.1f) else ColorExpense.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (netProfit >= 0) "صافي الأرباح والادخار 📈" else "صافي الخسائر والعجز 📉",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (netProfit >= 0) ColorIncome else ColorExpense
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CommonComponents.formatCurrency(netProfit),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (netProfit >= 0) ColorIncome else ColorExpense
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "للفترة المحددة بناءً على حساباتك الحالية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Mini Income vs Expense Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(ColorIncome.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ColorIncome, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("المدخولات", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CommonComponents.formatCurrency(totalIncome), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ColorIncome)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(ColorExpense.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ColorExpense, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("المصروفات", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CommonComponents.formatCurrency(totalExpense), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ColorExpense)
                            }
                        }
                    }
                }
            }

            // 4. Chart Type Selector (Pie vs Bar)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (chartTypeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { chartTypeTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("التقسيم الدائري للتصنيفات 🍕", color = if (chartTypeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (chartTypeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { chartTypeTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("مقارنة الدخل والمصروف 📊", color = if (chartTypeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // 5. Interactive Charts Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (chartTypeTab == 0) {
                            // Pie Chart
                            if (categoryExpenses.isEmpty()) {
                                Text("لا توجد مصروفات مسجلة لعرضها كتقسيم دائري", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Canvas Pie Drawing
                                    Canvas(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .weight(1f)
                                    ) {
                                        var startAngle = 0f
                                        categoryExpenses.forEach { (cat, amount) ->
                                            val categoryColor = try {
                                                Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#9E9E9E"))
                                            } catch (e: Exception) {
                                                Color.Gray
                                            }
                                            val sweepAngle = ((amount / totalCategoryExpense) * 360f).toFloat() * chartProgress
                                            drawArc(
                                                color = categoryColor,
                                                startAngle = startAngle,
                                                sweepAngle = sweepAngle,
                                                useCenter = true,
                                                size = Size(size.width, size.height)
                                            )
                                            startAngle += sweepAngle
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Legends Panel
                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        categoryExpenses.take(4).forEach { (cat, amount) ->
                                            val categoryColor = try {
                                                Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#9E9E9E"))
                                            } catch (e: Exception) {
                                                Color.Gray
                                            }
                                            val percentage = (amount / totalCategoryExpense) * 100
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(categoryColor))
                                                Text(
                                                    text = "${cat?.name ?: "تصنيف"}: ${percentage.toInt()}%",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Bar Chart Comparing Income vs Expenses
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val maxVal = (totalIncome.coerceAtLeast(totalExpense)).coerceAtLeast(1.0)
                                val incomeHeightMultiplier = (totalIncome / maxVal).toFloat() * chartProgress
                                val expenseHeightMultiplier = (totalExpense / maxVal).toFloat() * chartProgress

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Income Bar
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(CommonComponents.formatCurrency(totalIncome), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(50.dp)
                                                .height((180 * incomeHeightMultiplier).dp)
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(ColorIncome)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("المدخولات", style = MaterialTheme.typography.bodySmall)
                                    }

                                    // Expense Bar
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(CommonComponents.formatCurrency(totalExpense), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(50.dp)
                                                .height((180 * expenseHeightMultiplier).dp)
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(ColorExpense)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("المصروفات", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Most Expensive Categories Header
            item {
                CommonComponents.SectionHeader(title = "تحليل المصروفات حسب التصنيف 📁")
            }

            // 7. Most Expensive Categories List
            if (categoryExpenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد مصروفات مسجلة في هذه الفترة", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(categoryExpenses) { (cat, amount) ->
                    val percentage = if (totalCategoryExpense > 0) (amount / totalCategoryExpense).toFloat() else 0f
                    val categoryColor = try {
                        Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#9E9E9E"))
                    } catch (e: Exception) {
                        Color.Gray
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CommonComponents.CategoryIcon(iconName = cat?.iconName ?: "category", colorHex = cat?.colorHex ?: "#9E9E9E")
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(cat?.name ?: "تصنيف", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Text(CommonComponents.formatCurrency(amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ColorExpense)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { percentage },
                                color = categoryColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // 8. Export Reports Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportToExcelSimulated() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorIncome),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير Excel 📊")
                    }

                    Button(
                        onClick = { viewModel.exportToPdfSimulated() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير PDF 📄")
                    }
                }
            }
        }
    }
}
