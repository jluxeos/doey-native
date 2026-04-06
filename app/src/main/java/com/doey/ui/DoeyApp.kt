package com.doey.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
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

// ── Colores compartidos ───────────────────────────────────────────────────────

val Purple      = Color(0xFF6750A4)
val PurpleDark  = Color(0xFF4F378B)
val OnPurple    = Color(0xFFFFFFFF)
val ErrorRed    = Color(0xFFB3261E)

// ── Tema OSCURO ───────────────────────────────────────────────────────────────

val Surface0Dark   = Color(0xFF10091A)
val Surface1Dark   = Color(0xFF1D1027)
val Surface2Dark   = Color(0xFF2B1E3A)
val Label1Dark     = Color(0xFFEAE0F8)
val Label2Dark     = Color(0xFFCDB8E8)
val Label3Dark     = Color(0xFF9489A4)

val DoeyColorsDark = darkColorScheme(
    primary            = Color(0xFFD0BCFF),
    onPrimary          = Color(0xFF381E72),
    primaryContainer   = PurpleDark,
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary          = Label2Dark,
    secondaryContainer = Color(0xFF4A4458),
    background         = Surface0Dark,
    surface            = Surface1Dark,
    surfaceVariant     = Surface2Dark,
    onBackground       = Label1Dark,
    onSurface          = Label1Dark,
    onSurfaceVariant   = Label2Dark,
    outline            = Label3Dark,
    error              = Color(0xFFFFB4AB),
    errorContainer     = Color(0xFF93000A)
)

// ── Tema CLARO (por defecto) ──────────────────────────────────────────────────

val Surface0Light  = Color(0xFFFFFBFE)
val Surface1Light  = Color(0xFFF3EDF7)
val Surface2Light  = Color(0xFFE8DEF8)
val Label1Light    = Color(0xFF1C1B1F)
val Label2Light    = Color(0xFF49454F)
val Label3Light    = Color(0xFF79747E)

// Alias dinámicos usados en pantallas — apuntan al claro (default)
var Surface0    = Surface0Light
var Surface1    = Surface1Light
var Surface2    = Surface2Light
var Label1      = Label1Light
var Label2      = Label2Light
var Label3      = Label3Light

val DoeyColorsLight = lightColorScheme(
    primary            = Purple,
    onPrimary          = OnPurple,
    primaryContainer   = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary          = Color(0xFF625B71),
    secondaryContainer = Color(0xFFE8DEF8),
    background         = Surface0Light,
    surface            = Surface1Light,
    surfaceVariant     = Surface2Light,
    onBackground       = Label1Light,
    onSurface          = Label1Light,
    onSurfaceVariant   = Label2Light,
    outline            = Label3Light,
    error              = ErrorRed,
    errorContainer     = Color(0xFFF9DEDC)
)

@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = Purple,
    unfocusedBorderColor      = Label3Light,
    focusedTextColor          = Label1Light,
    unfocusedTextColor        = Label1Light,
    focusedLabelColor         = Purple,
    unfocusedLabelColor       = Label3Light,
    cursorColor               = Purple,
    focusedPlaceholderColor   = Label3Light,
    unfocusedPlaceholderColor = Label3Light
)

// ── Navegación ────────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home        : Screen("home",        "Inicio",      Icons.Default.Home)
    object Skills      : Screen("skills",      "Skills",      Icons.Default.Extension)
    object Memories    : Screen("memories",    "Memorias",    Icons.Default.Psychology)
    object Schedules   : Screen("schedules",   "Agendas",     Icons.Default.Alarm)
    object Journal     : Screen("journal",     "Diario",      Icons.Default.LibraryBooks)
    object Permissions : Screen("permissions", "Permisos",    Icons.Default.Lock)
    object Settings    : Screen("settings",    "Ajustes",     Icons.Default.Settings)
    object Logs        : Screen("logs",        "Logs",        Icons.Default.BugReport)
}

val NAV_ITEMS = listOf(
    Screen.Home,
    Screen.Skills,
    Screen.Memories,
    Screen.Schedules,
    Screen.Journal,
    Screen.Permissions,
    Screen.Settings,
    Screen.Logs
)

// ── Raíz ──────────────────────────────────────────────────────────────────────

@Composable
fun DoeyApp() {
    val vm  = viewModel<MainViewModel>()
    val nav = rememberNavController()

    // Tema claro por defecto
    MaterialTheme(colorScheme = DoeyColorsLight) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Surface1Light) {
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
                                unselectedIconColor = Label3Light,
                                unselectedTextColor = Label3Light,
                                indicatorColor      = Color(0xFFEADDFF)
                            )
                        )
                    }
                }
            }
        ) { pad ->
            NavHost(nav, startDestination = Screen.Home.route, Modifier.padding(pad)) {
                composable(Screen.Home.route)        { HomeScreen(vm) }
                composable(Screen.Skills.route)      { SkillsScreen(vm) }
                composable(Screen.Memories.route)    { MemoriesScreen(vm) }
                composable(Screen.Schedules.route)   { SchedulesScreen(vm) }
                composable(Screen.Journal.route)     { JournalScreen(vm) }
                composable(Screen.Permissions.route) { PermissionsScreen() }
                composable(Screen.Settings.route)    { SettingsScreen(vm) }
                composable(Screen.Logs.route)         { LogScreen() }
            }
        }
    }
}