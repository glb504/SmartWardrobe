package com.smartwardrobe.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartwardrobe.ui.calendar.CalendarScreen
import com.smartwardrobe.ui.closet.ClosetScreen
import com.smartwardrobe.ui.settings.SettingsScreen
import com.smartwardrobe.ui.stylist.StylistScreen
import com.smartwardrobe.ui.tryon.TryOnScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Closet : Screen("closet", "衣橱", Icons.Default.ShoppingCart)
    data object Stylist : Screen("stylist", "AI穿搭", Icons.Default.Star)
    data object TryOn : Screen("tryon", "试衣间", Icons.Default.Person)
    data object Calendar : Screen("calendar", "日历", Icons.Default.DateRange)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

private val topLevel = listOf(Screen.Closet, Screen.Stylist, Screen.TryOn, Screen.Calendar, Screen.Settings)

@Composable
fun SmartWardrobeApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevel.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Closet.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Closet.route) { ClosetScreen() }
            composable(Screen.Stylist.route) {
                StylistScreen(onTryOn = { navController.navigate(Screen.TryOn.route) })
            }
            composable(Screen.TryOn.route) { TryOnScreen() }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
