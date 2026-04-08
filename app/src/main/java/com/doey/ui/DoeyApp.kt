package com.doey.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch

// ── Paleta Tau (tema oscuro premium) ─────────────────────────────────────────
val TauBg          = Color(0xFF0A0A0F)
val TauSurface1    = Color(0xFF13131A)
val TauSurface2    = Color(0xFF1C1C28)
val TauSurface3    = Color(0xFF252535)
val TauAccent      = Color(0xFF7C4DFF)
val TauAccentLight = Color(0xFFB39DDB)
val TauAccentGlow  = Color(0xFF651FFF)
val TauGreen       = Color(0xFF00E676)
val TauBlue        = Color(0xFF40C4FF)
val TauOrange      = Color(0xFFFF6D00)
val TauRed         = Color(0xFFFF1744)
val TauText1       = Color(0xFFFFFFFF)
val TauText2       = Color(0xFFB0BEC5)
val TauText3       = Color(0xFF546E7A)
val TauSeparator   = Color(0xFF1E1E2E)

// Alias de compatibilidad
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

val DoeyColorsTau = darkColorScheme(
    primary            = TauAccent,
    onPrimary          = Color.White,
    primaryContainer   = TauAccentGlow.copy(alpha = 0.2f),
    secondary          = TauBlue,
    onSecondary        = Color.White,
    background         = TauBg,
    surface            = TauSurface1,
    surfaceVariant     = TauSurface2,
    onBackground       = TauText1,
    onSurface          = TauText1,
    onSurfaceVariant   = TauText2,
    outline            = TauText3,
    error              = TauRed,
    errorContainer     = TauRed.copy(alpha = 0.15f)
)

// Alias para compatibilidad con pantallas que usan DoeyColorsLight/Dark
val DoeyColorsLight = DoeyColorsTau
val DoeyColorsDark  = DoeyColorsTau

@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = TauAccent,
    unfocusedBorderColor      = TauText3,
    focusedTextColor          = TauText1,
    unfocusedTextColor        = TauText1,
    focusedLabelColor         = TauAccent,
    unfocusedLabelColor       = TauText3,
    cursorColor               = TauAccent,
    focusedPlaceholderColor   = TauText3,
    unfocusedPlaceholderColor = TauText3,
    focusedContainerColor     = TauSurface2,
    unfocusedContainerColor   = TauSurface2
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
}

val BOTTOM_NAV_ITEMS = listOf(Screen.Home, Screen.Schedules, Screen.Memories)

data class DrawerSection(val title: String, val items: List<Screen>)

val DRAWER_ITEMS_BASIC = listOf(
    DrawerSection("Principal",     listOf(Screen.Home, Screen.Schedules, Screen.Journal, Screen.Memories)),
    DrawerSection("Configuración", listOf(Screen.Profile, Screen.Settings, Screen.Permissions))
)

// FIX BUG-1: Settings y Logs SIEMPRE presentes en modo avanzado
val DRAWER_ITEMS_ADVANCED = listOf(
    DrawerSection("Principal",     listOf(Screen.Home, Screen.FlowMode, Screen.Schedules, Screen.Journal, Screen.Memories)),
    DrawerSection("Herramientas",  listOf(Screen.Skills, Screen.Macros)),
    DrawerSection("Configuración", listOf(Screen.Profile, Screen.Settings, Screen.Permissions, Screen.FriendlySettings, Screen.Logs))
)

// ── Raíz ──────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoeyApp() {
    val vm  = viewModel<MainViewModel>()
    val ctx = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }

    var onboardingDone by remember { mutableStateOf(profileStore.isOnboardingDone()) }
    var userProfile    by remember { mutableStateOf(profileStore.getUserProfile()) }
    var isLowPower     by remember { mutableStateOf(profileStore.isLowPowerMode()) }

    // FIX BUG-4: callback para que SettingsScreen notifique cambios de perfil
    val onProfileChanged: () -> Unit = {
        userProfile = profileStore.getUserProfile()
        isLowPower  = profileStore.isLowPowerMode()
    }

    // FIX BUG-6: Manejar invocación como asistente del sistema
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
        MaterialTheme(colorScheme = DoeyColorsTau) {
            OnboardingFlow { name, profile, performance, provider, apiKey ->
                profileStore.setUserName(name)
                profileStore.setUserProfile(
                    if (profile == UserProfile.BASIC) ProfileStore.PROFILE_BASIC else ProfileStore.PROFILE_ADVANCED
                )
                profileStore.setPerformanceMode(
                    if (performance == PerformanceMode.LOW_POWER) ProfileStore.PERF_LOW_POWER else ProfileStore.PERF_HIGH_PERFORMANCE
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

    MaterialTheme(colorScheme = DoeyColorsTau) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DoeyDrawerContent(nav, drawerState, isAdvanced, isLowPower, scope)
            }
        ) {
            Scaffold(
                containerColor = TauBg,
                bottomBar = {
                    DoeyBottomBar(nav = nav, onMenuClick = { scope.launch { drawerState.open() } })
                }
            ) { pad ->
                NavHost(nav, startDestination = Screen.Home.route, Modifier.padding(pad)) {
                    composable(Screen.Home.route)             { HomeScreen(vm, nav) }
                    composable(Screen.Skills.route)           { SkillsScreen(vm) }
                    composable(Screen.Memories.route)         { MemoriesScreen(vm) }
                    composable(Screen.Schedules.route)        { SchedulesScreen(vm) }
                    composable(Screen.Journal.route)          { JournalScreen(vm) }
                    composable(Screen.Permissions.route)      { PermissionsScreen() }
                    composable(Screen.Settings.route)         { SettingsScreen(vm, onProfileChanged) }
                    composable(Screen.Logs.route)             { LogScreen() }
                    composable(Screen.FlowMode.route)         { FlowModeScreen(vm) }
                    composable(Screen.Profile.route)          { ProfileScreen(vm) }
                    composable(Screen.FriendlySettings.route) { FriendlySettingsScreen(vm) }
                    composable(Screen.AutoMode.route)         { AutoModeScreen(vm) }
                    composable(Screen.Macros.route)           { MacrosScreen(vm) }
                }
            }
        }
    }
}

// ── Barra inferior ─────────────────────────────────────────────────────────────
@Composable
private fun DoeyBottomBar(nav: NavController, onMenuClick: () -> Unit) {
    val backEntry by nav.currentBackStackEntryAsState()
    val current = backEntry?.destination

    Surface(color = TauSurface1, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        NavigationBar(containerColor = Color.Transparent, contentColor = TauText2, modifier = Modifier.height(64.dp)) {
            // Menú hamburguesa
            NavigationBarItem(
                icon     = { Icon(Icons.Default.Menu, "Menú", modifier = Modifier.size(22.dp)) },
                label    = { Text("Menú", fontSize = 10.sp) },
                selected = false,
                onClick  = onMenuClick,
                colors   = NavigationBarItemDefaults.colors(
                    unselectedIconColor = TauText3, unselectedTextColor = TauText3,
                    indicatorColor = TauAccent.copy(alpha = 0.15f)
                )
            )
            BOTTOM_NAV_ITEMS.forEach { screen ->
                val sel = current?.route == screen.route
                NavigationBarItem(
                    icon     = { Icon(screen.icon, screen.label, modifier = Modifier.size(22.dp)) },
                    label    = { Text(screen.label, fontSize = 10.sp) },
                    selected = sel,
                    onClick  = {
                        nav.navigate(screen.route) {
                            popUpTo(nav.graph.findStartDestination().id)
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TauAccent, selectedTextColor = TauAccent,
                        unselectedIconColor = TauText3, unselectedTextColor = TauText3,
                        indicatorColor = TauAccent.copy(alpha = 0.15f)
                    )
                )
            }
            // Ajustes rápidos
            NavigationBarItem(
                icon     = { Icon(Icons.Default.Settings, "Ajustes", modifier = Modifier.size(22.dp)) },
                label    = { Text("Ajustes", fontSize = 10.sp) },
                selected = current?.route == Screen.Settings.route,
                onClick  = {
                    nav.navigate(Screen.Settings.route) {
                        popUpTo(nav.graph.findStartDestination().id)
                        launchSingleTop = true; restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TauAccent, selectedTextColor = TauAccent,
                    unselectedIconColor = TauText3, unselectedTextColor = TauText3,
                    indicatorColor = TauAccent.copy(alpha = 0.15f)
                )
            )
        }
    }
}

// ── Drawer ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoeyDrawerContent(
    nav: NavController,
    drawerState: DrawerState,
    isAdvanced: Boolean,
    isLowPower: Boolean,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val backEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    val sections = if (isAdvanced) DRAWER_ITEMS_ADVANCED else DRAWER_ITEMS_BASIC

    ModalDrawerSheet(drawerContainerColor = TauSurface1, modifier = Modifier.width(288.dp)) {
        // Header con gradiente (estático arriba)
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0A3E), Color(0xFF0D0D1A))))
                .padding(24.dp)
        ) {
            Column {
                Box(
                    Modifier.size(60.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(TauAccent.copy(0.4f), TauAccentGlow.copy(0.1f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = TauAccentLight, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Doey", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("23.4.9 Ultra · Tau Version", fontSize = 11.sp, color = TauAccentLight.copy(0.7f))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = if (isAdvanced) TauAccent.copy(0.25f) else TauBlue.copy(0.2f)) {
                        Text(
                            if (isAdvanced) "⚡ Avanzado" else "✦ Básico",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (isAdvanced) TauAccentLight else TauBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (isLowPower) {
                        Surface(shape = RoundedCornerShape(6.dp), color = TauOrange.copy(0.2f)) {
                            Text("🔋 Eco", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = TauOrange, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }

        // Cuerpo con SCROLL (Punto 9)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            sections.forEach { section ->
                Text(section.title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    color = TauText3, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    letterSpacing = 1.5.sp)
                section.items.forEach { screen ->
                    val sel = currentRoute == screen.route
                    NavigationDrawerItem(
                        icon     = { Icon(screen.icon, screen.label, modifier = Modifier.size(20.dp)) },
                        label    = { Text(screen.label, fontSize = 14.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = sel,
                        onClick  = {
                            scope.launch { drawerState.close() }
                            nav.navigate(screen.route) {
                                popUpTo(nav.graph.findStartDestination().id)
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = TauAccent.copy(0.15f),
                            selectedIconColor = TauAccent, selectedTextColor = TauAccentLight,
                            unselectedIconColor = TauText2, unselectedTextColor = TauText2
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (section != sections.last()) {
                    HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
            }
        }

        // Footer estático abajo
        HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(TauGreen))
            Spacer(Modifier.width(8.dp))
            Text("Doey 23.4.9 Ultra · Tau", fontSize = 11.sp, color = TauText3)
        }
        Spacer(Modifier.height(8.dp))
    }
}
