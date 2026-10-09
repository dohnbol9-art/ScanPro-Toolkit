package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen

@Composable
fun ScanProBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Home
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = {
                Text(
                    text = "Home",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute == Screen.Home.route) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_home")
        )

        // 2. Scan
        NavigationBarItem(
            selected = currentRoute?.startsWith("scan") == true,
            onClick = { onNavigate(Screen.Scan.defaultRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan"
                )
            },
            label = {
                Text(
                    text = "Scan",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute?.startsWith("scan") == true) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_scan_center")
        )

        // 3. Create QR
        NavigationBarItem(
            selected = currentRoute?.startsWith("create") == true,
            onClick = { onNavigate(Screen.Create.defaultRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AddBox,
                    contentDescription = "Create"
                )
            },
            label = {
                Text(
                    text = "Create",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute?.startsWith("create") == true) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_create")
        )

        // 4. History
        NavigationBarItem(
            selected = currentRoute?.startsWith("history") == true,
            onClick = { onNavigate(Screen.History.defaultRoute) },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History"
                )
            },
            label = {
                Text(
                    text = "History",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute?.startsWith("history") == true) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_history")
        )

        // 5. Settings
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings"
                )
            },
            label = {
                Text(
                    text = "Settings",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute == Screen.Settings.route) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
