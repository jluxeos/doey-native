package com.doey.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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

// ── Paleta Glass (base neutra + acento variable) ────────────────────────────
val TauBg          = Color(0xFF070A13)
val TauSurface1    = Color(0x141FFFFFF)
val TauSurface2    = Color(0x1FECF3FF)
val TauSurface3    = Color(0x26FFFFFF)
val TauAccent      = Color(0xFF8B7BFF)
val TauAccentLight = Color(0xFFD8D3FF)
val TauAccentGlow  = Color(0xFF5AC8FA)
val TauGreen       = Color(0xFF32D74B)
val TauBlue        = Color(0xFF5AC8FA)
val TauOrange      = Color(0xFFFF9F0A)
val TauRed         = Color(0xFFFF453A)
val TauText1       = Color(0xFFF6F8FF)
val TauText2       = Color(0xFFCCD3E2)
val TauText3       = Color(0xFF8D98AE)
val TauSeparator   = Color(0x26FFFFFF)

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

private fun glassColorScheme(primary: Color, secondary: Color = primary, onPrimary: Color = Color.White) = darkColorScheme(
    primary            = primary,
    onPrimary          = onPrimary,
    primaryContainer   = primary.copy(alpha = 0.24f),
    secondary          = secondary,
    onSecondary        = Color.White,
    secondaryContainer = secondary.copy(alpha = 0.18f),
    tertiary           = TauAccentGlow,
    background         = TauBg,
    surface            = TauSurface1,
    surfaceVariant     = TauSurface2,
    surfaceTint        = primary,
    onBackground       = TauText1,
    onSurface          = TauText1,
    onSurfaceVariant   = TauText2,
    outline            = Color.White.copy(alpha = 0.22f),
    outlineVariant     = TauSeparator,
    error              = TauRed,
    errorContainer     = TauRed.copy(alpha = 0.18f)
)

val DoeyColorsTau = glassColorScheme(primary = TauAccent, secondary = TauAccentGlow)

// Alias para compatibilidad con pantallas que usan DoeyColorsLight/Dark
val DoeyColorsLight = DoeyColorsTau
val DoeyColorsDark  = DoeyColorsTau

fun buildColorScheme(theme: String) = when (theme) {
    "blue"   -> glassColorScheme(primary = TauBlue)
    "green"  -> glassColorScheme(primary = TauGreen, onPrimary = Color.Black)
    "orange" -> glassColorScheme(primary = TauOrange)
    "red"    -> glassColorScheme(primary = TauRed)
    else      -> DoeyColorsTau
}

@Composable
fun doeyFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor        = scheme.primary,
        unfocusedBorderColor      = Color.White.copy(alpha = 0.14f),
        focusedTextColor          = TauText1,
        unfocusedTextColor        = TauText1,
        focusedLabelColor         = scheme.primary,
        unfocusedLabelColor       = TauText3,
        cursorColor               = scheme.primary,
        focusedPlaceholderColor   = TauText3,
        unfocusedPlaceholderColor = TauText3,
        focusedContainerColor     = TauSurface2,
        unfocusedContainerColor   = TauSurface1
    )
}

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
    
    // Nuevos destinos para el menú reorganizado
    object Clock            : Screen("clock",            "Reloj",          Icons.Default.Schedule)
    object Alarms           : Screen("alarms",           "Alarmas",        Icons.Default.Alarm)
    object Reminders        : Screen("reminders",        "Recordatorios",  Icons.Default.NotificationsActive)
    object Timers           : Screen("timers",           "Temporizadores", Icons.Default.HourglassEmpty)
    object Stopwatch        : Screen("stopwatch",        "Cronómetro",     Icons.Default.Timer)
    object HowToUse         : Screen("how_to_use",       "¿Cómo usar?",    Icons.Default.HelpOutline)
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
    val settingsStore = remember { SettingsStore(ctx) }

    var onboardingDone by remember { mutableStateOf(profileStore.isOnboardingDone()) }
    var userProfile    by remember { mutableStateOf(profileStore.getUserProfile()) }
    var isLowPower     by remember { mutableStateOf(profileStore.isLowPowerMode()) }
    var activeTheme    by remember { mutableStateOf("tau") }

    LaunchedEffect(Unit) {
        activeTheme = settingsStore.getTheme()
    }

    // FIX BUG-4: callback para que SettingsScreen notifique cambios de perfil y tema
    val scope2 = rememberCoroutineScope()
    val onProfileChanged: () -> Unit = {
        userProfile = profileStore.getUserProfile()
        isLowPower  = profileStore.isLowPowerMode()
        scope2.launch { activeTheme = settingsStore.getTheme() }
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
                // El menú inferior ha sido eliminado según el nuevo diseño
                topBar = {
                    DoeyTopBar(vm, nav, onMenuClick = { scope.launch { drawerState.open() } })
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

// ── Barra superior (LIMPIA) ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoeyTopBar(vm: MainViewModel, nav: NavController, onMenuClick: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val isFlow = false // Simplificado para la barra superior general

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Doey", fontWeight = FontWeight.ExtraBold, color = TauText1, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                // ● Indicador (Activo/Inactivo)
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (state.isWakeWordActive) TauGreen else TauText3)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Menú", tint = TauText1)
            }
        },
        actions = {
            // 🔔 Notificaciones
            IconButton(onClick = { /* Próximamente consejos */ }) {
                Icon(Icons.Default.Notifications, "Notificaciones", tint = TauText2)
            }
            
            // Modo actual: Normal / Auto / Flujo
            var showModeMenu by remember { mutableStateOf(false) }
            TextButton(onClick = { showModeMenu = true }) {
                Text(
                    text = when {
                        state.isDrivingMode -> "Auto"
                        state.isExpertMode -> "Flujo"
                        else -> "Normal"
                    },
                    color = TauAccentLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = TauAccentLight)
                
                DropdownMenu(expanded = showModeMenu, onDismissRequest = { showModeMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Normal") },
                        onClick = { 
                            showModeMenu = false
                            // Lógica para cambiar a modo normal
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Auto") },
                        onClick = { 
                            showModeMenu = false
                            vm.toggleDrivingMode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Flujo") },
                        onClick = { 
                            showModeMenu = false
                            // Lógica para cambiar a modo flujo
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
    )
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
            if (isAdvanced) {
                // 🧠 Uso principal
                DrawerSectionHeader("🧠 Uso principal")
                DrawerItem(Screen.Memories, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.HowToUse, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.FriendlySettings, currentRoute, nav, scope, drawerState)

                HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // ⏱️ Reloj y avisos
                DrawerSectionHeader("⏱️ Reloj y avisos")
                DrawerItem(Screen.Clock, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Alarms, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Reminders, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Timers, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Stopwatch, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Journal, currentRoute, nav, scope, drawerState)

                HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // ⚙️ Configuración (COLAPSABLE)
                var configExpanded by remember { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().clickable { configExpanded = !configExpanded }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙️ CONFIGURACIÓN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = TauText3, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                    Icon(
                        if (configExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = TauText3, modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = configExpanded) {
                    Column {
                        // Subsecciones de configuración
                        DrawerSubHeader("IA")
                        DrawerItem(Screen.Settings, currentRoute, nav, scope, drawerState, "Proveedor / API Key")
                        
                        DrawerSubHeader("Voz")
                        // Aquí irían items de voz
                        
                        DrawerSubHeader("Interfaz")
                        DrawerItem(Screen.Profile, currentRoute, nav, scope, drawerState, "Perfil / Burbuja")
                        
                        DrawerSubHeader("Optimización")
                        // Tokens, Caché, Compresión
                        
                        DrawerSubHeader("Procesamiento")
                        // Iteraciones, Mensajes, Notificaciones
                        
                        DrawerSubHeader("Sistema")
                        DrawerItem(Screen.Permissions, currentRoute, nav, scope, drawerState)
                        
                        DrawerSubHeader("Modos")
                        // Selector, Normal/Auto/Flujo, Rendimiento
                    }
                }

                HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // 🧪 Logs (Solo visible si debug ON - simplificado aquí)
                DrawerSectionHeader("🧪 Logs")
                DrawerItem(Screen.Logs, currentRoute, nav, scope, drawerState)
            } else {
                // ☰ Menú hamburguesa (MODO BÁSICO)
                
                // Ajustes (versión básica)
                DrawerSectionHeader("⚙️ Ajustes")
                DrawerItem(Screen.Settings, currentRoute, nav, scope, drawerState, "Ajustes básicos")
                DrawerItem(Screen.Profile, currentRoute, nav, scope, drawerState, "Entrar a modo experto")
                DrawerItem(Screen.Permissions, currentRoute, nav, scope, drawerState, "Permisos")

                HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // Datos personales
                DrawerSectionHeader("👤 Datos personales")
                DrawerItem(Screen.Profile, currentRoute, nav, scope, drawerState, "Mi Perfil")

                HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // Modo
                DrawerSectionHeader("🔄 Modo")
                // Aquí iría el switch para Auto/Normal
                DrawerItem(Screen.AutoMode, currentRoute, nav, scope, drawerState, "Modo Auto / Normal")

                HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // Reloj y avisos
                DrawerSectionHeader("⏱️ Reloj y avisos")
                DrawerItem(Screen.Clock, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Alarms, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Timers, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.Schedules, currentRoute, nav, scope, drawerState, "Agenda")
                DrawerItem(Screen.Journal, currentRoute, nav, scope, drawerState, "Diario")

                HorizontalDivider(color = TauSeparator, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // Otros
                DrawerItem(Screen.HowToUse, currentRoute, nav, scope, drawerState)
                DrawerItem(Screen.FriendlySettings, currentRoute, nav, scope, drawerState, "Modo compacto")
                DrawerItem(Screen.Memories, currentRoute, nav, scope, drawerState)
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

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
        color = TauText3, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        letterSpacing = 1.5.sp)
}

@Composable
private fun DrawerSubHeader(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = TauAccentLight.copy(alpha = 0.6f), 
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerItem(
    screen: Screen,
    currentRoute: String?,
    nav: NavController,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: DrawerState,
    customLabel: String? = null
) {
    val sel = currentRoute == screen.route
    NavigationDrawerItem(
        icon     = { Icon(screen.icon, screen.label, modifier = Modifier.size(20.dp)) },
        label    = { Text(customLabel ?: screen.label, fontSize = 14.sp,
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
