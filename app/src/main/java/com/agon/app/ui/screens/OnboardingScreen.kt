package com.agon.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agon.app.viewmodel.DaftarViewModel

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(navController: NavController, viewModel: DaftarViewModel) {
    var currentSlide by remember { mutableStateOf(0) }
    
    val slides = listOf(
        OnboardingPage(
            title = "إدارة أموالك بسهولة 🪙",
            description = "تتبع كل قرش يدخل أو يخرج من محفظتك وراقب نمو ثروتك بذكاء وتخلص من الأوراق والتعقيدات.",
            icon = Icons.Default.Payments,
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            title = "تتبع الديون والمصروفات ⏰",
            description = "لا تنسَ ديونك أو مستحقاتك بعد الآن، نظام تنبيهات ذكي يذكرك بمواعيد السداد ويرتب دفعاتك تلقائياً.",
            icon = Icons.Default.ReceiptLong,
            color = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            title = "تقارير ذكية واحترافية 📊",
            description = "رسوم بيانية تفاعلية وتحليلات دقيقة تساعدك على اتخاذ قرارات مالية أفضل وفهم مصادر صرفك.",
            icon = Icons.Default.Insights,
            color = MaterialTheme.colorScheme.tertiary
        )
    )

    val page = slides[currentSlide]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    viewModel.completeOnboarding()
                    navController.navigate("register") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = "تخطي",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Animated Content (Slide)
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                slideInHorizontally { width -> if (currentSlide > 0) width else -width } + fadeIn() with
                        slideOutHorizontally { width -> if (currentSlide > 0) -width else width } + fadeOut()
            },
            modifier = Modifier.weight(1f),
            label = "onboardingSlide"
        ) { targetPage ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    targetPage.color.copy(alpha = 0.2f),
                                    targetPage.color.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = targetPage.icon,
                        contentDescription = targetPage.title,
                        tint = targetPage.color,
                        modifier = Modifier.size(90.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = targetPage.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = targetPage.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Bottom section: Indicators & Navigation Buttons
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                slides.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(width = if (currentSlide == index) 24.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentSlide == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSlide > 0) {
                    OutlinedButton(
                        onClick = { currentSlide-- },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 120.dp, height = 56.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "السابق")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("السابق")
                    }
                } else {
                    Spacer(modifier = Modifier.width(120.dp))
                }

                Button(
                    onClick = {
                        if (currentSlide < slides.size - 1) {
                            currentSlide++
                        } else {
                            viewModel.completeOnboarding()
                            navController.navigate("register") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(width = 140.dp, height = 56.dp)
                ) {
                    Text(if (currentSlide == slides.size - 1) "ابدأ الآن" else "التالي")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "التالي")
                }
            }
        }
    }
}
