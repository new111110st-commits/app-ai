package com.agon.app.ui.screens

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
fun CategoriesScreen(navController: NavController, viewModel: DaftarViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Dialog states
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    // Add/Edit Form states
    var categoryName by remember { mutableStateOf("") }
    var categoryIcon by remember { mutableStateOf("restaurant") }
    var categoryColor by remember { mutableStateOf("#FF7043") }
    var categoryBudget by remember { mutableStateOf("") }

    // Calculate current month
    val currentMonthStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        sdf.format(Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الميزانيات والتصنيفات 📁", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    categoryName = ""
                    categoryBudget = ""
                    categoryIcon = "restaurant"
                    categoryColor = "#FF7043"
                    showAddCategoryDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة تصنيف", modifier = Modifier.size(28.dp))
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "حدد ميزانية شهرية لكل تصنيف لمراقبة حجم استهلاكك وتجنب الإسراف المالي.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(state.categories) { category ->
                // Calculate total spent in this category for the current month
                val totalSpent = state.transactions
                    .filter {
                        it.accountId == state.currentAccountId &&
                        it.categoryId == category.id &&
                        it.type == TransactionType.EXPENSE &&
                        it.date.startsWith(currentMonthStr)
                    }
                    .sumOf { it.amount }

                CategoryBudgetItem(
                    category = category,
                    totalSpent = totalSpent,
                    onClick = {
                        editingCategory = category
                        categoryName = category.name
                        categoryBudget = category.budgetLimit?.toInt()?.toString() ?: ""
                        categoryIcon = category.iconName
                        categoryColor = category.colorHex
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // --- ADD DIALOG ---
    if (showAddCategoryDialog) {
        CategoryFormDialog(
            title = "إضافة تصنيف جديد",
            name = categoryName,
            onNameChange = { categoryName = it },
            budget = categoryBudget,
            onBudgetChange = { categoryBudget = it },
            selectedIcon = categoryIcon,
            onIconSelect = { categoryIcon = it },
            selectedColor = categoryColor,
            onColorSelect = { categoryColor = it },
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = {
                if (categoryName.isNotBlank()) {
                    viewModel.addCategory(
                        name = categoryName,
                        iconName = categoryIcon,
                        colorHex = categoryColor,
                        budgetLimit = categoryBudget.toDoubleOrNull()
                    )
                    Toast.makeText(context, "تم حفظ التصنيف الجديد بنجاح!", Toast.LENGTH_SHORT).show()
                    showAddCategoryDialog = false
                } else {
                    Toast.makeText(context, "يرجى تعبئة اسم التصنيف!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // --- EDIT / BUDGET ADJUST DIALOG ---
    if (editingCategory != null) {
        val cat = editingCategory!!
        CategoryFormDialog(
            title = "تعديل ميزانية التصنيف",
            name = categoryName,
            onNameChange = { categoryName = it },
            budget = categoryBudget,
            onBudgetChange = { categoryBudget = it },
            selectedIcon = categoryIcon,
            onIconSelect = { categoryIcon = it },
            selectedColor = categoryColor,
            onColorSelect = { categoryColor = it },
            onDismiss = { editingCategory = null },
            onConfirm = {
                if (categoryName.isNotBlank()) {
                    viewModel.deleteCategory(cat.id) // Simple delete and recreate to update
                    viewModel.addCategory(
                        name = categoryName,
                        iconName = categoryIcon,
                        colorHex = categoryColor,
                        budgetLimit = categoryBudget.toDoubleOrNull()
                    )
                    Toast.makeText(context, "تم تعديل التصنيف بنجاح!", Toast.LENGTH_SHORT).show()
                    editingCategory = null
                }
            },
            isEditMode = true,
            onDeleteClick = {
                viewModel.deleteCategory(cat.id)
                Toast.makeText(context, "تم حذف التصنيف بنجاح!", Toast.LENGTH_SHORT).show()
                editingCategory = null
            }
        )
    }
}

@Composable
fun CategoryBudgetItem(
    category: TransactionCategory,
    totalSpent: Double,
    onClick: () -> Unit
) {
    val budgetLimit = category.budgetLimit
    val percentage = if (budgetLimit != null && budgetLimit > 0) (totalSpent / budgetLimit).toFloat() else 0f
    
    // Progress bar color based on consumption
    val progressColor = when {
        percentage >= 1.0f -> ColorExpense // Exceeded
        percentage >= 0.8f -> ColorPartial // Warning (80%+)
        else -> ColorIncome // Safe
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    CommonComponents.CategoryIcon(iconName = category.iconName, colorHex = category.colorHex)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(category.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        if (budgetLimit != null) {
                            Text(
                                text = "الميزانية: ${CommonComponents.formatCurrency(budgetLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("لم يتم تحديد ميزانية مخصصة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "المستهلك: ${CommonComponents.formatCurrency(totalSpent)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (budgetLimit != null && totalSpent > budgetLimit) ColorExpense else MaterialTheme.colorScheme.onSurface
                    )
                    if (budgetLimit != null) {
                        Text(
                            text = "${(percentage * 100).toInt()}% من الميزانية",
                            style = MaterialTheme.typography.labelSmall,
                            color = progressColor
                        )
                    }
                }
            }

            if (budgetLimit != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { percentage.coerceAtMost(1.0f) },
                    color = progressColor,
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

@Composable
fun CategoryFormDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    budget: String,
    onBudgetChange: (String) -> Unit,
    selectedIcon: String,
    onIconSelect: (String) -> Unit,
    selectedColor: String,
    onColorSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isEditMode: Boolean = false,
    onDeleteClick: (() -> Unit)? = null
) {
    val iconsList = listOf("restaurant", "shopping_bag", "receipt_long", "directions_car", "payments", "laptop_mac", "trending_up", "school")
    val colorsList = listOf("#FF7043", "#9C27B0", "#2196F3", "#FFB300", "#4CAF50", "#009688", "#E91E63", "#607D8B")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("اسم التصنيف المالي") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budget,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onBudgetChange(it) },
                    label = { Text("الميزانية الشهرية المحددة (ر.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Icon Picker
                Text("الأيقونة المميزة:", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    iconsList.take(4).forEach { icon ->
                        val isSelected = selectedIcon == icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onIconSelect(icon) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CommonComponents.getIconByName(icon),
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
                    iconsList.takeLast(4).forEach { icon ->
                        val isSelected = selectedIcon == icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onIconSelect(icon) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CommonComponents.getIconByName(icon),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Color Picker
                Text("اللون التعريفي:", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorsList.take(4).forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelect(colorHex) },
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
                        val isSelected = selectedColor == colorHex
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelect(colorHex) },
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
            Button(onClick = onConfirm) {
                Text("حفظ")
            }
        },
        dismissButton = {
            Row {
                if (isEditMode && onDeleteClick != null) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = ColorExpense),
                        onClick = onDeleteClick
                    ) {
                        Text("حذف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}
