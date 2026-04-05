package com.doey.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*

// ── Tema ──────────────────────────────────────────────────────────────────────

val Surface0    = Color(0xFF10091A)
val Surface1    = Color(0xFF1D1027)
val Surface2    = Color(0xFF2B1E3A)
val Purple      = Color(0xFFD0BCFF)
val PurpleDark  = Color(0xFF4F378B)
val OnPurple    = Color(0xFF381E72)
val Label1      = Color(0xFFEAE0F8)
val Label2      = Color(0xFFCDB8E8)
val Label3      = Color(0xFF9489A4)
val ErrorRed    = Color(0xFFFFB4AB)

val DoeyColors = darkColorScheme(
    primary            = Purple,
    onPrimary          = OnPurple,
    primaryContainer   = PurpleDark,
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary          = Label2,
    secondaryContainer = Color(0xFF4A4458),
    background         = Surface0,
    surface            = Surface1,
    surfaceVariant     = Surface2,
    onBackground       = Label1,
    onSurface          = Label1,
    onSurfaceVariant   = Label2,
    outline            = Label3,
    error              = ErrorRed,
    errorContainer     = Color(0xFF93000A)
)

// ── Navegación ────────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home        : Screen("home",        "Chat",        Icons.Default.Home)
    object Schedules   : Screen("schedules",   "Schedules",   Icons.Default.Alarm)
    object Journal     : Screen("journal",     "Journal",     Icons.Default.LibraryBooks)
    object Permissions : Screen("permissions", "Permissions", Icons.Default.Lock)
    object Settings    : Screen("settings",    "Settings",    Icons.Default.Settings)
}

val NAV_ITEMS = listOf(Screen.Home, Screen.Schedules, Screen.Journal, Screen.Permissions, Screen.Settings)

// ── Raíz ──────────────────────────────────────────────────────────────────────

@Composable
fun DoeyApp() {
    val vm:  MainViewModel = viewModel()
    val nav = rememberNavController()

    MaterialTheme(colorScheme = DoeyColors) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Surface1) {
                    val backEntry by nav.currentBackStackEntryAsState()
                    val current = backEntry?.destination
                    NAV_ITEMS.forEach { screen ->
                        NavigationBarItem(
                            icon     = { Icon(screen.icon, screen.label) },
                            label    = { Text(screen.label) },
                            selected = current?.hierarchy?.any { it.route == screen.route } == true,
                            onClick  = {
                                nav.navigate(screen.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Purple,
                                selectedTextColor   = Purple,
                                unselectedIconColor = Label3,
                                unselectedTextColor = Label3,
                                indicatorColor      = PurpleDark
                            )
                        )
                    }
                }
            }
        ) { pad ->
            NavHost(nav, startDestination = Screen.Home.route, Modifier.padding(pad)) {
                composable(Screen.Home.route)        { HomeScreen(vm) }
                composable(Screen.Schedules.route)   { SchedulesScreen(vm) }
                composable(Screen.Journal.route)     { JournalScreen(vm) }
                composable(Screen.Permissions.route) { PermissionsScreen() }
                composable(Screen.Settings.route)    { SettingsScreen(vm) }
            }
        }
    }
}
