package com.doey.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.doey.agent.ProfileStore
import kotlinx.coroutines.launch

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
    object FlowMode    : Screen("flow_mode",   "Modo Flujo",  Icons.Default.AccountTree)
    object AutoMode    : Screen("auto_mode",   "Modo Auto",   Icons.Default.DirectionsCar)
    object Macros      : Screen("macros",      "Macros",      Icons.Default.Star)
    object Profile     : Screen("profile",     "Mi Perfil",   Icons.Default.Person)
}

// Elementos en la barra inferior (solo los más importantes)
val BOTTOM_NAV_ITEMS = listOf(
    Screen.Home,
    Screen.Schedules,
    Screen.Memories
)

// Secciones del menú lateral
data class DrawerSection(val title: String, val items: List<Screen>)

val DRAWER_ITEMS_BASIC = listOf(
    DrawerSection("Principal", listOf(Screen.Home, Screen.Schedules, Screen.Journal, Screen.Memories)),
    DrawerSection("Configuración", listOf(Screen.Profile, Screen.Settings, Screen.Permissions))
)

val DRAWER_ITEMS_ADVANCED = listOf(
    DrawerSection("Principal", listOf(Screen.Home, Screen.FlowMode, Screen.Schedules, Screen.Journal, Screen.Memories)),
    DrawerSection("Herramientas", listOf(Screen.Skills, Screen.Macros)),
    DrawerSection("Configuración", listOf(Screen.Profile, Screen.Settings, Screen.Permissions, Screen.Logs))
)

// ── Raíz ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoeyApp() {
    val vm  = viewModel<MainViewModel>()
    val ctx = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }

    // Estado de onboarding
    var onboardingDone by remember { mutableStateOf(profileStore.isOnboardingDone()) }
    var userProfile by remember { mutableStateOf(profileStore.getUserProfile()) }
    var isLowPower by remember { mutableStateOf(profileStore.isLowPowerMode()) }

    // Mostrar onboarding si no está completo
    if (!onboardingDone) {
        MaterialTheme(colorScheme = DoeyColorsLight) {
            OnboardingFlow { profile, performance, provider, apiKey ->
                // Guardar configuración del onboarding
                profileStore.setUserProfile(
                    if (profile == UserProfile.BASIC) ProfileStore.PROFILE_BASIC else ProfileStore.PROFILE_ADVANCED
                )
                profileStore.setPerformanceMode(
                    if (performance == PerformanceMode.LOW_POWER) ProfileStore.PERF_LOW_POWER else ProfileStore.PERF_HIGH
                )
                profileStore.setOnboardingDone(true)

                // Guardar API key si se proporcionó
                if (apiKey.isNotBlank()) {
                    val settings = vm.getSettings()
                    // setApiKey es síncrono (SharedPreferences), setProvider es suspend
                    settings.setApiKey(provider, apiKey)
                    kotlinx.coroutines.MainScope().launch {
                        settings.setProvider(provider)
                    }
                }

                userProfile = if (profile == UserProfile.BASIC) ProfileStore.PROFILE_BASIC else ProfileStore.PROFILE_ADVANCED
                isLowPower = performance == PerformanceMode.LOW_POWER
                onboardingDone = true
            }
        }
        return
    }

    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isAdvanced = userProfile == ProfileStore.PROFILE_ADVANCED

    MaterialTheme(colorScheme = DoeyColorsLight) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DoeyDrawerContent(
                    nav = nav,
                    drawerState = drawerState,
                    isAdvanced = isAdvanced,
                    isLowPower = isLowPower,
                    scope = scope
                )
            }
        ) {
            Scaffold(
                bottomBar = {
                    DoeyBottomBar(
                        nav = nav,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
            ) { pad ->
                NavHost(nav, startDestination = Screen.Home.route, Modifier.padding(pad)) {
                    composable(Screen.Home.route)        { HomeScreen(vm, nav) }
                    composable(Screen.Skills.route)      { SkillsScreen(vm) }
                    composable(Screen.Memories.route)    { MemoriesScreen(vm) }
                    composable(Screen.Schedules.route)   { SchedulesScreen(vm) }
                    composable(Screen.Journal.route)     { JournalScreen(vm) }
                    composable(Screen.Permissions.route) { PermissionsScreen() }
                    composable(Screen.Settings.route)    { SettingsScreen(vm) }
                    composable(Screen.Logs.route)        { LogScreen() }
                    composable(Screen.FlowMode.route)    { FlowModeScreen(vm) }
                    composable(Screen.Profile.route)     { ProfileScreen(vm) }
                }
            }
        }
    }
}

@Composable
private fun DoeyBottomBar(
    nav: androidx.navigation.NavController,
    onMenuClick: () -> Unit
) {
    val backEntry by nav.currentBackStackEntryAsState()
    val current = backEntry?.destination

    NavigationBar(containerColor = Surface1Light) {
        // Elementos principales de navegación
        BOTTOM_NAV_ITEMS.forEach { screen ->
            NavigationBarItem(
                icon     = { Icon(screen.icon, screen.label) },
                label    = { Text(screen.label, fontSize = 10.sp) },
                selected = current?.hierarchy?.any { it.route == screen.route } == true,
                alwaysShowLabel = false,
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

        // Botón de menú hamburguesa
        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, "Menú") },
            label = { Text("Más", fontSize = 10.sp) },
            selected = false,
            alwaysShowLabel = false,
            onClick = onMenuClick,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoeyDrawerContent(
    nav: androidx.navigation.NavController,
    drawerState: DrawerState,
    isAdvanced: Boolean,
    isLowPower: Boolean,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val backEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    val sections = if (isAdvanced) DRAWER_ITEMS_ADVANCED else DRAWER_ITEMS_BASIC

    ModalDrawerSheet(
        drawerContainerColor = Surface0Light,
        modifier = Modifier.width(280.dp)
    ) {
        // Header del drawer
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF1A0A2E), Color(0xFF2D1B4E))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Purple.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Doey",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    if (isAdvanced) "Modo Avanzado" else "Modo Básico",
                    fontSize = 13.sp,
                    color = Color(0xFFBB86FC)
                )
                if (isLowPower) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF3A2A00)
                    ) {
                        Text(
                            "⚡ Bajo consumo",
                            fontSize = 10.sp,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        sections.forEach { section ->
            Text(
                section.title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Label3Light,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                letterSpacing = 1.sp
            )

            section.items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavigationDrawerItem(
                    icon = { Icon(screen.icon, screen.label) },
                    label = { Text(screen.label, fontSize = 14.sp) },
                    selected = isSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        nav.navigate(screen.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFEADDFF),
                        selectedIconColor = Purple,
                        selectedTextColor = Purple,
                        unselectedIconColor = Label2Light,
                        unselectedTextColor = Label2Light
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(4.dp))
            if (section != sections.last()) {
                HorizontalDivider(
                    color = Label3Light.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Footer del drawer
        HorizontalDivider(color = Label3Light.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            "Doey v2.0 · El mejor asistente",
            fontSize = 11.sp,
            color = Label3Light,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
    }
}
