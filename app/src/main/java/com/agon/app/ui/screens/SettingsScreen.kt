package com.agon.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agon.app.data.Account
import com.agon.app.data.AccountType
import com.agon.app.ui.theme.*
import com.agon.app.viewmodel.DaftarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: DaftarViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Edit Profile states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(state.user.name) }
    var editEmail by remember { mutableStateOf(state.user.email) }

    // Security states
    var isPinEnabled by remember { mutableStateOf(true) }
    var isBiometricEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات والملف الشخصي ⚙️", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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
            // 1. Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = state.user.name.ifBlank { "مستخدم تجريبي" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = state.user.email.ifBlank { "demo@daftar.plus" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Edit Profile Button
                    IconButton(
                        onClick = {
                            editName = state.user.name
                            editEmail = state.user.email
                            showEditProfileDialog = true
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 2. Multi-Account Section
            Text("إدارة الحسابات المتعددة 📂", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    state.accounts.forEach { acc ->
                        val isCurrent = state.currentAccountId == acc.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when(acc.type) {
                                        AccountType.PERSONAL -> Icons.Default.Person
                                        AccountType.STORE -> Icons.Default.Storefront
                                        AccountType.COMPANY -> Icons.Default.Business
                                        AccountType.SAVINGS -> Icons.Default.AccountBalance
                                    },
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Text("نشط ✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                if (state.accounts.size > 1) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteAccount(acc.id)
                                            Toast.makeText(context, "تم حذف الحساب وتصفية عملياته", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الحساب", tint = ColorExpense, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Application Preferences
            Text("تفضيلات التطبيق 📱", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Dark Mode Toggle
                    SettingsToggleRow(
                        title = "الوضع الليلي (Dark Mode)",
                        subtitle = "تفعيل المظهر الداكن لتوفير طاقة البطارية وراحة العين",
                        icon = Icons.Default.DarkMode,
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // PIN lock toggle
                    SettingsToggleRow(
                        title = "قفل الأمان (PIN Lock)",
                        subtitle = "طلب رمز PIN للأمان عند فتح التطبيق لحماية خصوصيتك",
                        icon = Icons.Default.Lock,
                        checked = isPinEnabled,
                        onCheckedChange = { isPinEnabled = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Biometric toggle
                    SettingsToggleRow(
                        title = "بصمة الإصبع (Biometric)",
                        subtitle = "تسجيل الدخول السريع باستخدام بصمة الوجه أو الإصبع",
                        icon = Icons.Default.Fingerprint,
                        checked = isBiometricEnabled,
                        onCheckedChange = { isBiometricEnabled = it }
                    )
                }
            }

            // 4. Data Sync & Export
            Text("مزامنة وتصدير البيانات ☁️", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsActionRow(
                        title = "مزامنة البيانات سحابياً",
                        subtitle = "حفظ البيانات واستعادتها تلقائياً على خوادم دفتر+ الآمنة",
                        icon = Icons.Default.CloudUpload,
                        onClick = { viewModel.syncDataSimulated() }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsActionRow(
                        title = "تصدير البيانات بصيغة Excel",
                        subtitle = "تصدير كافة العمليات المالية والديون في ملف جداول Excel",
                        icon = Icons.Default.Share,
                        onClick = { viewModel.exportToExcelSimulated() }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsActionRow(
                        title = "تصدير التقرير بصيغة PDF",
                        subtitle = "تنزيل تقرير مالي مفصل ومنظم جاهز للطباعة أو المشاركة",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = { viewModel.exportToPdfSimulated() }
                    )
                }
            }

            // 5. Account Logout
            Button(
                onClick = {
                    viewModel.logoutUser()
                    Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorExpense),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج من الحساب", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }

            // Version Info Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "دفتر+ (Daftar+) v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "جميع الحقوق محفوظة © 2025",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    // --- EDIT PROFILE DIALOG ---
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("تعديل الملف الشخصي 👤") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم الكامل") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("البريد الإلكتروني") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editEmail.isNotBlank()) {
                            viewModel.updateProfile(editName, editEmail, null)
                            Toast.makeText(context, "تم تحديث البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                            showEditProfileDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
