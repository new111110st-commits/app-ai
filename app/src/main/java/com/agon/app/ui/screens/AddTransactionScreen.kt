package com.agon.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.agon.app.data.*
import com.agon.app.ui.components.CommonComponents
import com.agon.app.ui.theme.*
import com.agon.app.viewmodel.DaftarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, viewModel: DaftarViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Form inputs
    var amountStr by remember { mutableStateOf("") }
    var txType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var dateStr by remember { mutableStateOf("") }
    var timeStr by remember { mutableStateOf("") }
    var attachedReceipt by remember { mutableStateOf<String?>(null) } // Simulated receipt image path

    // Category dialog states
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("restaurant") }
    var newCategoryColor by remember { mutableStateOf("#FF7043") }
    var newCategoryBudget by remember { mutableStateOf("") }

    // Pre-fill current date & time
    LaunchedEffect(Unit) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        dateStr = sdfDate.format(Date())
        timeStr = sdfTime.format(Date())
        // Default to first category
        selectedCategoryId = state.categories.firstOrNull()?.id ?: ""
    }

    // Budget check logic
    val selectedCategory = state.categories.find { it.id == selectedCategoryId }
    val isBudgetExceeded = remember(amountStr, selectedCategoryId, txType) {
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        if (txType == TransactionType.EXPENSE && selectedCategory?.budgetLimit != null) {
            val currentMonth = dateStr.substring(0, 7) // "YYYY-MM"
            val totalSpent = state.transactions
                .filter { it.accountId == state.currentAccountId && it.categoryId == selectedCategoryId && it.type == TransactionType.EXPENSE && it.date.startsWith(currentMonth) }
                .sumOf { it.amount }
            (totalSpent + amount) > selectedCategory.budgetLimit
        } else {
            false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة عملية جديدة ➕", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. Amount Input Card (Big Text)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "المبلغ الإجمالي",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ر.س",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        TextField(
                            value = amountStr,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                            placeholder = { Text("0.00", style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))) },
                            textStyle = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }

            // 2. Transaction Type Segmented Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expense Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (txType == TransactionType.EXPENSE) ColorExpense else Color.Transparent)
                        .clickable { txType = TransactionType.EXPENSE },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "مصروف (-)",
                        color = if (txType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Income Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (txType == TransactionType.INCOME) ColorIncome else Color.Transparent)
                        .clickable { txType = TransactionType.INCOME },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "مدخول (+)",
                        color = if (txType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Budget Exceeded Alert (Animate Visibility)
            AnimatedVisibility(
                visible = isBudgetExceeded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "تنبيه: هذا المبلغ يتجاوز الميزانية الشهرية المحددة لتصنيف '${selectedCategory?.name ?: ""}'!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 3. Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اختر التصنيف 📁", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Text("إضافة تصنيف جديد +", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Grid of Categories
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val borderStroke = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clickable { selectedCategoryId = cat.id },
                            shape = RoundedCornerShape(16.dp),
                            border = borderStroke,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CommonComponents.CategoryIcon(iconName = cat.iconName, colorHex = cat.colorHex, size = 32)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. Payment Method Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("طريقة الدفع 💳", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PaymentMethod.values().forEach { method ->
                        val isSelected = paymentMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { paymentMethod = method },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method.arabicName,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 5. Date & Time Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("التاريخ") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = it },
                    label = { Text("الوقت") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. Note input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("ملاحظات إضافية (أين صرفت المال؟)") },
                leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 7. Receipt picker (Simulated)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("صورة الفاتورة/المستند (اختياري) 📸", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                
                if (attachedReceipt == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                attachedReceipt = "simulated_receipt_uri"
                                Toast.makeText(context, "تم إرفاق الفاتورة بنجاح! 📎", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اضغط لإرفاق صورة فاتورة أو مستند", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ColorIncome)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("تم إرفاق الفاتورة بنجاح", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            IconButton(onClick = { attachedReceipt = null }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = ColorExpense)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 8. Save Button
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ صحيح!", Toast.LENGTH_SHORT).show()
                    } else if (selectedCategoryId.isBlank()) {
                        Toast.makeText(context, "الرجاء اختيار تصنيف!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addTransaction(
                            amount = amount,
                            type = txType,
                            categoryId = selectedCategoryId,
                            date = dateStr,
                            time = timeStr,
                            note = note,
                            paymentMethod = paymentMethod,
                            imageUri = attachedReceipt
                        )
                        Toast.makeText(context, "تم حفظ العملية بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("حفظ العملية المالية", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- ADD CUSTOM CATEGORY DIALOG ---
    if (showAddCategoryDialog) {
        val iconsList = listOf("restaurant", "shopping_bag", "receipt_long", "directions_car", "payments", "laptop_mac", "trending_up", "school")
        val colorsList = listOf("#FF7043", "#9C27B0", "#2196F3", "#FFB300", "#4CAF50", "#009688", "#E91E63", "#607D8B")

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("إضافة تصنيف مخصص جديد") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("اسم التصنيف (مثال: دراسة)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCategoryBudget,
                        onValueChange = { if (it.all { c -> c.isDigit() }) newCategoryBudget = it },
                        label = { Text("الميزانية الشهرية المحددة (اختياري)") },
                        leadingIcon = { Text("ر.س", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Icon Picker
                    Text("اختر الأيقونة المناسبة:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        iconsList.take(4).forEach { iconName ->
                            val isSelected = newCategoryIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { newCategoryIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CommonComponents.getIconByName(iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        iconsList.takeLast(4).forEach { iconName ->
                            val isSelected = newCategoryIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { newCategoryIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CommonComponents.getIconByName(iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Color Picker
                    Text("اختر اللون المميز:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorsList.take(4).forEach { colorHex ->
                            val isSelected = newCategoryColor == colorHex
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { newCategoryColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorsList.takeLast(4).forEach { colorHex ->
                            val isSelected = newCategoryColor == colorHex
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { newCategoryColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(
                                name = newCategoryName,
                                iconName = newCategoryIcon,
                                colorHex = newCategoryColor,
                                budgetLimit = newCategoryBudget.toDoubleOrNull()
                            )
                            Toast.makeText(context, "تمت إضافة التصنيف بنجاح! 📁", Toast.LENGTH_SHORT).show()
                            newCategoryName = ""
                            newCategoryBudget = ""
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
