package com.agon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agon.app.ui.screens.*
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.viewmodel.DaftarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: DaftarViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            AgonAppTheme(darkTheme = isDarkMode) {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: DaftarViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show BottomNav only on main screens
    val showBottomNav = currentRoute in listOf("dashboard", "debts", "reports", "settings")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                BottomNav(navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("splash") {
                SplashScreen(navController, viewModel)
            }
            composable("onboarding") {
                OnboardingScreen(navController, viewModel)
            }
            composable("login") {
                LoginRegisterScreen(navController, viewModel, isLoginMode = true)
            }
            composable("register") {
                LoginRegisterScreen(navController, viewModel, isLoginMode = false)
            }
            composable("dashboard") {
                DashboardScreen(navController, viewModel)
            }
            composable("add_transaction") {
                AddTransactionScreen(navController, viewModel)
            }
            composable("debts") {
                DebtsScreen(navController, viewModel)
            }
            composable("reports") {
                ReportsScreen(navController, viewModel)
            }
            composable("categories") {
                CategoriesScreen(navController, viewModel)
            }
            composable("notifications") {
                NotificationsScreen(navController, viewModel)
            }
            composable("settings") {
                SettingsScreen(navController, viewModel)
            }
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية", modifier = Modifier.size(24.dp)) },
            label = { Text("الرئيسية") },
            selected = currentRoute == "dashboard",
            onClick = {
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = true }
                }
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "الديون", modifier = Modifier.size(24.dp)) },
            label = { Text("الديون") },
            selected = currentRoute == "debts",
            onClick = {
                navController.navigate("debts") {
                    popUpTo("dashboard")
                }
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "التقارير", modifier = Modifier.size(24.dp)) },
            label = { Text("التقارير") },
            selected = currentRoute == "reports",
            onClick = {
                navController.navigate("reports") {
                    popUpTo("dashboard")
                }
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات", modifier = Modifier.size(24.dp)) },
            label = { Text("الإعدادات") },
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    popUpTo("dashboard")
                }
            },
        )
    }
}
