package com.agon.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.agon.app.viewmodel.DaftarViewModel

@Composable
fun LoginRegisterScreen(
    navController: NavController,
    viewModel: DaftarViewModel,
    isLoginMode: Boolean = true
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    // Custom PIN Pad UI helper
    val onPinDigitClick: (String) -> Unit = { digit ->
        if (pin.length < 4) {
            pin += digit
        }
    }

    val onPinDeleteClick: () -> Unit = {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            errorMsg = ""
            if (isLoginMode) {
                val success = viewModel.loginUser(pin)
                if (success) {
                    Toast.makeText(context, "مرحباً بك مجدداً! 👋", Toast.LENGTH_SHORT).show()
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    pin = ""
                    errorMsg = "رمز PIN غير صحيح! جرب 1234"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isLoginMode) "تسجيل الدخول الآمن" else "إنشاء حساب جديد",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isLoginMode) "أدخل رمز PIN المكون من 4 أرقام للمتابعة" else "املأ البيانات لتأمين الدفتر الخاص بك",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Form / PIN Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoginMode) {
                // PIN Display Circles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < pin.length
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                        )
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // PIN Pad
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(280.dp)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("delete", "0", "check")
                    )

                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.2f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (item == "delete" || item == "check") Color.Transparent
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable {
                                            when (item) {
                                                "delete" -> onPinDeleteClick()
                                                "check" -> {
                                                    // Quick Bypass
                                                    viewModel.loginUser("1234")
                                                    navController.navigate("dashboard") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                }
                                                else -> onPinDigitClick(item)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (item) {
                                        "delete" -> Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "مسح",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        "check" -> Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "دخول سريع",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        else -> Text(
                                            text = item,
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Register Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("الاسم الكامل") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("البريد الإلكتروني") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                            label = { Text("رمز PIN للأمان (4 أرقام)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (name.isBlank() || email.isBlank() || pin.length < 4) {
                                    errorMsg = "الرجاء تعبئة كافة الحقول بشكل صحيح"
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.registerUser(name, email, pin)
                                    Toast.makeText(context, "تم إنشاء الحساب بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                                    navController.navigate("dashboard") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "إنشاء الحساب والمتابعة",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Footer / Switch mode
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            TextButton(
                onClick = {
                    if (isLoginMode) {
                        navController.navigate("register")
                    } else {
                        navController.navigate("login")
                    }
                }
            ) {
                Text(
                    text = if (isLoginMode) "ليس لديك حساب؟ سجل الآن" else "لديك حساب بالفعل؟ سجل دخولك",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quick bypass
            TextButton(
                onClick = {
                    viewModel.loginUser("1234")
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                        popUpTo("register") { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = "الدخول السريع (تخطي الأمان للتقييم) ⚡",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
