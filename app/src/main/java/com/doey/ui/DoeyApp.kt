package com.doey.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.doey.agent.ProfileStore
import com.doey.agent.SettingsStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch

// Alias de compatibilidad (usando los de Glassmorphism.kt)
val Purple         = TauAccent
val PurpleDark     = TauAccentGlow
val OnPurple       = Color.White
val Surface0Light  = TauSurface1
val Surface1Light  = TauSurface2
val Surface2Light  = TauSurface3
val Label1Light    = TauText1
val Label2Light    = TauText2
val Label3Light    = TauText3
val ErrorRed       = TauRed

val DoeyColorsTau = lightColorScheme(
    primary            = TauAccent,
    onPrimary          = Color.White,
    primaryContainer   = TauAccentGlow.copy(alpha = 0.2f),
    secondary          = TauBlue,
    onSecondary        = Color.White,
    background         = TauBg,
    surface            = Color.White,
    surfaceVariant     = Color(0xFFF5F5F5),
    onBackground       = TauText1,
    onSurface          = TauText1,
    onSurfaceVariant   = TauText2,
    outline            = TauText3,
    error              = TauRed,
    errorContainer     = TauRed.copy(alpha = 0.15f)
)

val DoeyColorsLight = DoeyColorsTau
val DoeyColorsDark  = DoeyColorsTau

fun buildColorScheme(theme: String) = DoeyColorsTau.copy(
    primary = TauAccent,
    primaryContainer = TauAccentGlow,
    secondary = TauBlue
)

// ── Navegación ────────────────────────────────────────────────────────────────
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home             : Screen("home",             "Inicio",         Icons.Default.Home)
    object Skills           : Screen("skills",           "Skills",         Icons.Default.Extension)
    object Memories         : Screen("memories",         "Memorias",       Icons.Default.Psychology)
    object Schedules        : Screen("schedules",        "Agendas",        Icons.Default.Alarm)
    object Journal          : Screen("journal",          "Diario",         Icons.Default.LibraryBooks)
    object Permissions      : Screen("permissions",      "Permisos",       Icons.Default.Lock)
    object Settings         : Screen("settings",         "Ajustes",        Icons.Default.Settings)
    object Logs             : Screen("logs",             "Logs",           Icons.Default.BugReport)
    object FlowMode         : Screen("flow_mode",        "Modo Flujo",     Icons.Default.AccountTree)
    object AutoMode         : Screen("auto_mode",        "Modo Auto",      Icons.Default.DirectionsCar)
    object Macros           : Screen("macros",           "Macros",         Icons.Default.Star)
    object Profile          : Screen("profile",          "Mi Perfil",      Icons.Default.Person)
    object FriendlySettings : Screen("friendly_settings","Modo Friendly",  Icons.Default.Spa)
    
    object Clock            : Screen("clock",            "Reloj",          Icons.Default.Schedule)
    object Alarms           : Screen("alarms",           "Alarmas",        Icons.Default.Alarm)
    object Reminders        : Screen("reminders",        "Recordatorios",  Icons.Default.NotificationsActive)
    object Timers           : Screen("timers",           "Temporizadores", Icons.Default.HourglassEmpty)
    object Stopwatch        : Screen("stopwatch",        "Cronómetro",     Icons.Default.Timer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoeyApp() {
    val vm  = viewModel<MainViewModel>()
    val ctx = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }
    val settingsStore = remember { SettingsStore(ctx) }

    var onboardingDone by remember { mutableStateOf(profileStore.isOnboardingDone()) }
    var userProfile    by remember { mutableStateOf(profileStore.getUserProfile()) }
    var isLowPower     by remember { mutableStateOf(profileStore.isLowPowerMode()) }
    var activeTheme    by remember { mutableStateOf("DeepSeaBlue") }

    LaunchedEffect(Unit) {
        activeTheme = settingsStore.getTheme()
        updateGlassTheme(activeTheme)
    }

    val scope2 = rememberCoroutineScope()
    val onProfileChanged: () -> Unit = {
        userProfile = profileStore.getUserProfile()
        isLowPower  = profileStore.isLowPowerMode()
        scope2.launch { 
            activeTheme = settingsStore.getTheme()
            updateGlassTheme(activeTheme)
        }
    }

    val activity = ctx as? android.app.Activity

    LaunchedEffect(Unit) {
        val intent = activity?.intent
        if (intent?.getBooleanExtra("auto_listen", false) == true) {
            intent.removeExtra("auto_listen")
            vm.startListening()
        }
        if (intent?.getBooleanExtra("stop_all", false) == true) {
            intent.removeExtra("stop_all")
            vm.stopSpeaking()
            vm.stopListening()
        }
        val pendingQuery = intent?.getStringExtra("pending_query")
        if (!pendingQuery.isNullOrBlank()) {
            intent.removeExtra("pending_query")
            vm.sendMessage(pendingQuery)
        }
    }

    if (!onboardingDone) {
        MaterialTheme(colorScheme = buildColorScheme(activeTheme)) {
            OnboardingFlow { name, profile, performance, provider, apiKey ->
                profileStore.setUserName(name)
                profileStore.setUserProfile(
                    if (profile == UserProfile.BASIC) ProfileStore.PROFILE_BASIC else ProfileStore.PROFILE_ADVANCED
                )
                profileStore.setPerformanceMode(
                    if (performance == PerformanceMode.LOW_POWER) ProfileStore.PERF_LOW_POWER else ProfileStore.PERF_HIGH
                )
                profileStore.setOnboardingDone(true)
                if (apiKey.isNotBlank()) {
                    val settings = vm.getSettings()
                    settings.setApiKey(provider, apiKey)
                    kotlinx.coroutines.MainScope().launch { settings.setProvider(provider) }
                }
                userProfile = if (profile == UserProfile.BASIC) ProfileStore.PROFILE_BASIC else ProfileStore.PROFILE_ADVANCED
                isLowPower  = performance == PerformanceMode.LOW_POWER
                onboardingDone = true
            }
        }
        return
    }

    val nav         = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    val isAdvanced  = userProfile == ProfileStore.PROFILE_ADVANCED

    MaterialTheme(colorScheme = buildColorScheme(activeTheme)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DoeyDrawerContent(nav, drawerState, isAdvanced, isLowPower, scope)
            }
        ) {
            Scaffold(
                containerColor = TauBg,
                topBar = {
                    DoeyTopBar(vm, nav, onMenuClick = { scope.launch { drawerState.open() } })
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    NavHost(navController = nav, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) { HomeScreen(vm, nav) }
                        composable(Screen.Settings.route) { SettingsScreen(vm, onProfileChanged) }
                        composable(Screen.Profile.route) { ProfileScreen(vm) }
                        composable(Screen.Memories.route) { MemoriesScreen(vm) }
                        composable(Screen.Journal.route) { JournalScreen(vm) }
                        composable(Screen.Schedules.route) { SchedulesScreen(vm) }
                        composable(Screen.Skills.route) { SkillsScreen(vm) }
                        composable(Screen.Logs.route) { LogScreen() }
                        composable(Screen.Permissions.route) { PermissionsScreen() }
                        composable(Screen.FriendlySettings.route) { FriendlySettingsScreen(vm) }
                        composable(Screen.FlowMode.route) { FlowModeScreen(vm) }
                        composable(Screen.AutoMode.route) { AutoModeScreen(vm) }
                        composable(Screen.Macros.route) { MacrosScreen(vm) }
                        
                        composable(Screen.Clock.route) { SchedulesScreen(vm) }
                        composable(Screen.Alarms.route) { SchedulesScreen(vm) }
                        composable(Screen.Reminders.route) { SchedulesScreen(vm) }
                        composable(Screen.Timers.route) { SchedulesScreen(vm) }
                        composable(Screen.Stopwatch.route) { SchedulesScreen(vm) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoeyTopBar(vm: MainViewModel, nav: NavController, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text("Doey AI", fontWeight = FontWeight.Bold, color = TauText1) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Menú", tint = TauText1)
            }
        },
        actions = {
            IconButton(onClick = { nav.navigate(Screen.Profile.route) }) {
                Icon(Icons.Default.AccountCircle, "Perfil", tint = TauText1)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DoeyDrawerContent(
    nav: NavController,
    drawerState: DrawerState,
    isAdvanced: Boolean,
    isLowPower: Boolean,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalDrawerSheet(
        drawerContainerColor = TauBg,
        drawerContentColor = TauText1,
        modifier = Modifier.width(300.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            GlassBackground(accentColor = TauAccent)
            
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Header del Drawer
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TauAccent.copy(alpha = 0.05f))
                        .border(1.dp, TauAccent.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Icon(Icons.Default.SmartToy, null, tint = TauAccent, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Doey Assistant", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TauText1)
                        Text(if (isAdvanced) "Modo Experto" else "Modo Básico", fontSize = 12.sp, color = TauText3)
                    }
                }

                DrawerSectionHeader("Principal")
                DrawerItem(Icons.Default.Home, "Inicio", currentRoute == Screen.Home.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Home.route)
                }
                DrawerItem(Icons.Default.Person, "Mi Perfil", currentRoute == Screen.Profile.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Profile.route)
                }

                DrawerSectionHeader("Herramientas")
                DrawerItem(Icons.Default.Extension, "Skills", currentRoute == Screen.Skills.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Skills.route)
                }
                DrawerItem(Icons.Default.Alarm, "Agendas", currentRoute == Screen.Schedules.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Schedules.route)
                }
                DrawerItem(Icons.Default.Psychology, "Memorias", currentRoute == Screen.Memories.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Memories.route)
                }

                if (isAdvanced) {
                    DrawerSectionHeader("Avanzado")
                    DrawerItem(Icons.Default.AccountTree, "Modo Flujo", currentRoute == Screen.FlowMode.route) {
                        scope.launch { drawerState.close() }
                        nav.navigate(Screen.FlowMode.route)
                    }
                    DrawerItem(Icons.Default.Star, "Macros", currentRoute == Screen.Macros.route) {
                        scope.launch { drawerState.close() }
                        nav.navigate(Screen.Macros.route)
                    }
                    DrawerItem(Icons.Default.BugReport, "Logs", currentRoute == Screen.Logs.route) {
                        scope.launch { drawerState.close() }
                        nav.navigate(Screen.Logs.route)
                    }
                }

                DrawerSectionHeader("Sistema")
                DrawerItem(Icons.Default.Settings, "Ajustes", currentRoute == Screen.Settings.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Settings.route)
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
