package com.agon.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agon.app.viewmodel.DaftarViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, viewModel: DaftarViewModel) {
    val state by viewModel.state.collectAsState()
    var startAnimation by remember { mutableStateOf(false) }
    val scaleAnimate by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000)
        if (!state.user.hasCompletedOnboarding) {
            navController.navigate("onboarding") {
                popUpTo("splash") { inclusive = true }
            }
        } else if (!state.user.isLoggedIn) {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("dashboard") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wallet,
                contentDescription = "Wallet Logo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scaleAnimate)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "دفتر+",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "نظام إدارة مالية شخصية وتجارية ذكي",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            )
        }
    }
}
