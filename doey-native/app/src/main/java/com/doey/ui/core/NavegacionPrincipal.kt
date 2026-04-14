package com.doey.ui.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.doey.ui.core.TauAccent
import com.doey.ui.core.TauAccentGlow
import com.doey.ui.core.TauBg
import com.doey.ui.core.TauBlue
import com.doey.ui.core.TauRed
import com.doey.ui.core.TauText1
import com.doey.ui.core.TauText2
import com.doey.ui.core.TauText3
import com.doey.ui.core.updateGlassTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
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
import com.doey.MainViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.doey.agente.ProfileStore
import com.doey.agente.SettingsStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
import com.doey.ui.comun.HomeScreen
import com.doey.ui.comun.SettingsScreen
import com.doey.ui.comun.ProfileScreen
import com.doey.ui.comun.MemoriesScreen
import com.doey.ui.comun.SchedulesScreen
import com.doey.ui.comun.OnboardingFlow
import com.doey.ui.comun.UserProfile
import com.doey.ui.comun.PerformanceMode
import com.doey.ui.basico.JournalScreen
import com.doey.ui.basico.FriendlySettingsScreen
import com.doey.ui.avanzado.LogScreen
import com.doey.ui.avanzado.PermissionsScreen

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

// El sistema de colores ahora se gestiona directamente a través de SistemaGlassmorphism.kt
// Se eliminan las definiciones de Material3 ColorScheme para usar la UI propia.

// ── Navegación ────────────────────────────────────────────────────────────────
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Inicio             : Screen("inicio",             "Inicio",         CustomIcons.Home)
    object Memorias         : Screen("memorias",         "Memorias",       CustomIcons.Message)
    object Agendas          : Screen("agendas",          "Agendas",        CustomIcons.Clock)
    object Diario           : Screen("diario",           "Diario",         CustomIcons.LibraryBooks)
    object Permisos           : Screen("permisos",           "Permisos",       CustomIcons.Lock)
    object Configuracion      : Screen("configuracion",      "Ajustes",        CustomIcons.Settings)
    object Registros          : Screen("registros",          "Registros",      CustomIcons.BugReport)
    object Perfil           : Screen("perfil",           "Mi Perfil",      CustomIcons.Person)
    object AjustesBasicos     : Screen("ajustes_basicos",    "Modo Básico",    CustomIcons.Spa)
    
    object Reloj            : Screen("reloj",            "Reloj",          CustomIcons.Clock)
    object Alarmas          : Screen("alarmas",          "Alarmas",        CustomIcons.Clock)
    object Recordatorios    : Screen("recordatorios",    "Recordatorios",  CustomIcons.NotificationsActive)
    object Temporizadores   : Screen("temporizadores",   "Temporizadores", CustomIcons.Clock)
    object Cronometro       : Screen("cronometro",       "Cronómetro",     CustomIcons.Clock)
}


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
        GlassOpacity = settingsStore.getGlassOpacity()
        GlassBlur = settingsStore.getGlassBlur()
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
        // Usar UI propia sin MaterialTheme
        CompositionLocalProvider(
            // Aquí se podrían proveer tipografías o colores si fuera necesario
        ) {
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

    // Usar UI propia sin MaterialTheme
    CompositionLocalProvider(
        // Aquí se podrían proveer tipografías o colores si fuera necesario
    ) {
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
                    NavHost(navController = nav, startDestination = Screen.Inicio.route) {
                        composable(Screen.Inicio.route) { HomeScreen(vm, nav) }
                        composable(Screen.Configuracion.route) { SettingsScreen(vm, onProfileChanged) }
                        composable(Screen.Perfil.route) { ProfileScreen(vm) }
                        composable(Screen.Memorias.route) { MemoriesScreen(vm) }
                        composable(Screen.Diario.route) { JournalScreen(vm) }
                        composable(Screen.Agendas.route) { SchedulesScreen(vm) }
                        composable(Screen.Registros.route) { LogScreen() }
                        composable(Screen.Permisos.route) { PermissionsScreen() }
                        composable(Screen.AjustesBasicos.route) { FriendlySettingsScreen(vm) }
                        
                        composable(Screen.Reloj.route) { SchedulesScreen(vm) }
                        composable(Screen.Alarmas.route) { SchedulesScreen(vm) }
                        composable(Screen.Recordatorios.route) { SchedulesScreen(vm) }
                        composable(Screen.Temporizadores.route) { SchedulesScreen(vm) }
                        composable(Screen.Cronometro.route) { SchedulesScreen(vm) }
                    }
                }
            }
        }
    }
}


@Composable
private fun DoeyTopBar(vm: MainViewModel, nav: NavController, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text("Doey AI", style = DoeyTypography.titleLarge, color = TauText1) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                                Icon(CustomIcons.Menu, "Menú", tint = TauText1)
            }
        },
        actions = {
            IconButton(onClick = { nav.navigate(Screen.Perfil.route) }) {
                                Icon(CustomIcons.Person, "Perfil", tint = TauText1)
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
                        Icon(CustomIcons.SmartToy, null, tint = TauAccent, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Doey Assistant", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TauText1)
                        Text(if (isAdvanced) "Modo Experto" else "Modo Básico", fontSize = 12.sp, color = TauText3)
                    }
                }

                DrawerSectionHeader("Principal")
                DrawerItem(CustomIcons.Home, "Inicio", currentRoute == Screen.Inicio.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Inicio.route)
                }
                DrawerItem(CustomIcons.Person, "Mi Perfil", currentRoute == Screen.Perfil.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Perfil.route)
                }

                DrawerSectionHeader("Herramientas")
                DrawerItem(CustomIcons.Alarm, "Utilerías", currentRoute == Screen.Agendas.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Agendas.route)
                }
                DrawerItem(CustomIcons.Psychology, "Memorias", currentRoute == Screen.Memorias.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Memorias.route)
                }

                if (isAdvanced) {
                    DrawerSectionHeader("Avanzado")
                    DrawerItem(CustomIcons.BugReport, "Logs", currentRoute == Screen.Registros.route) {
                        scope.launch { drawerState.close() }
                        nav.navigate(Screen.Registros.route)
                    }
                }

                DrawerSectionHeader("Sistema")
                DrawerItem(CustomIcons.Settings, "Ajustes", currentRoute == Screen.Configuracion.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Configuracion.route)
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
