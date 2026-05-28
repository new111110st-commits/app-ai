package com.agon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.data.DebtStatus
import com.agon.app.data.TransactionType
import com.agon.app.ui.theme.*

object CommonComponents {

    @Composable
    fun CategoryIcon(
        iconName: String,
        colorHex: String,
        modifier: Modifier = Modifier,
        size: Int = 40
    ) {
        val color = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }

        val icon = getIconByName(iconName)

        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconName,
                tint = color,
                modifier = Modifier.size((size * 0.55).dp)
            )
        }
    }

    fun getIconByName(name: String): ImageVector {
        return when (name.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
            "receipt_long", "bills" -> Icons.Default.ReceiptLong
            "directions_car", "trans" -> Icons.Default.DirectionsCar
            "payments", "salary" -> Icons.Default.Payments
            "laptop_mac", "freelance" -> Icons.Default.LaptopMac
            "trending_up", "investment" -> Icons.Default.TrendingUp
            "business" -> Icons.Default.Business
            "storefront" -> Icons.Default.Storefront
            "account_balance" -> Icons.Default.AccountBalance
            "person" -> Icons.Default.Person
            "school" -> Icons.Default.School
            "home" -> Icons.Default.Home
            "directions_bus" -> Icons.Default.DirectionsBus
            "work" -> Icons.Default.Work
            "phone_android" -> Icons.Default.PhoneAndroid
            else -> Icons.Default.Category
        }
    }

    @Composable
    fun StatusBadge(status: DebtStatus) {
        val (bgColor, textColor, text) = when (status) {
            DebtStatus.PAID -> Triple(ColorPaid.copy(alpha = 0.15f), ColorPaid, "مدفوع")
            DebtStatus.PARTIAL -> Triple(ColorPartial.copy(alpha = 0.15f), ColorPartial, "جزئي")
            DebtStatus.UNPAID -> Triple(ColorUnpaid.copy(alpha = 0.15f), ColorUnpaid, "غير مدفوع")
            DebtStatus.OVERDUE -> Triple(ColorOverdue.copy(alpha = 0.15f), ColorOverdue, "متأخر")
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    fun TxTypeBadge(type: TransactionType) {
        val (bgColor, textColor, text) = when (type) {
            TransactionType.INCOME -> Triple(ColorIncome.copy(alpha = 0.15f), ColorIncome, "مدخول")
            TransactionType.EXPENSE -> Triple(ColorExpense.copy(alpha = 0.15f), ColorExpense, "مصروف")
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    fun formatCurrency(amount: Double): String {
        return String.format("%,.2f ر.س", amount)
    }

    @Composable
    fun SectionHeader(
        title: String,
        actionText: String? = null,
        onActionClick: (() -> Unit)? = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (actionText != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
