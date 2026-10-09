package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.BannerAdView
import com.example.ui.components.ScanProBottomBar
import com.example.ui.create.CreateQrScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.result.ScanResultDetailScreen
import com.example.ui.scanner.ScannerScreen
import com.example.ui.settings.SettingsScreen

@Composable
fun ScanProNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on camera scanner and detailed result view
    val isBottomBarVisible = currentRoute != null &&
            !currentRoute.startsWith("scan") &&
            !currentRoute.startsWith("result")

    // Robust navigation dispatcher: reliably handles popping to Home or switching destinations
    val navigateTo: (String) -> Unit = { targetRoute ->
        if (targetRoute == Screen.Home.route) {
            val popped = navController.popBackStack(Screen.Home.route, inclusive = false)
            if (!popped) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val navigateBack: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BannerAdView()
                    ScanProBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = navigateTo
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Dashboard
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel(),
                    onNavigate = navigateTo
                )
            }

            // Real-Time Camera Scanner
            composable(
                route = Screen.Scan.route,
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "single"
                    }
                )
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode")
                ScannerScreen(
                    viewModel = viewModel(),
                    initialMode = mode,
                    onNavigate = navigateTo,
                    onBack = navigateBack
                )
            }

            // QR Code Creator / Code Lab
            composable(
                route = Screen.Create.route,
                arguments = listOf(
                    navArgument("category") {
                        type = NavType.StringType
                        defaultValue = "URL"
                    }
                )
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")
                CreateQrScreen(
                    viewModel = viewModel(),
                    initialCategory = category,
                    onNavigate = navigateTo,
                    onBack = navigateBack
                )
            }

            // History Vault
            composable(
                route = Screen.History.route,
                arguments = listOf(
                    navArgument("filter") {
                        type = NavType.StringType
                        defaultValue = "all"
                    }
                )
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("filter")
                HistoryScreen(
                    viewModel = viewModel(),
                    initialFilter = filter,
                    onNavigate = navigateTo,
                    onBack = navigateBack
                )
            }

            // Settings & Preferences
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel(),
                    onNavigate = navigateTo,
                    onBack = navigateBack
                )
            }

            // Safe Scan Result Detail Screen
            composable(
                route = Screen.ResultDetail.route,
                arguments = listOf(
                    navArgument("recordId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                ScanResultDetailScreen(
                    recordId = recordId,
                    viewModel = viewModel(),
                    onNavigate = navigateTo,
                    onBack = navigateBack
                )
            }
        }
    }
}
