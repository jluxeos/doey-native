package com.doey.ui.core

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import com.doey.ui.core.DoeyTypography
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
    object Inicio             : Screen("inicio",             "Inicio",         Icons.Default.Home)
    object Habilidades        : Screen("habilidades",        "Habilidades",    Icons.Default.Extension)
    object Memorias         : Screen("memorias",         "Memorias",       Icons.Default.Psychology)
    object Agendas          : Screen("agendas",          "Agendas",        Icons.Default.Alarm)
    object Diario           : Screen("diario",           "Diario",         Icons.Default.LibraryBooks)
    object Permisos           : Screen("permisos",           "Permisos",       Icons.Default.Lock)
    object Configuracion      : Screen("configuracion",      "Ajustes",        Icons.Default.Settings)
    object Registros          : Screen("registros",          "Registros",      Icons.Default.BugReport)
    object ModoFlujo          : Screen("modo_flujo",         "Modo Flujo",     Icons.Default.AccountTree)
    object ModoAuto           : Screen("modo_auto",          "Modo Auto",      Icons.Default.DirectionsCar)
    object Macros           : Screen("macros",           "Macros",         Icons.Default.Star)
    object Perfil           : Screen("perfil",           "Mi Perfil",      Icons.Default.Person)
    object AjustesBasicos     : Screen("ajustes_basicos",    "Modo Básico",    Icons.Default.Spa)
    
    object Reloj            : Screen("reloj",            "Reloj",          Icons.Default.Schedule)
    object Alarmas          : Screen("alarmas",          "Alarmas",        Icons.Default.Alarm)
    object Recordatorios    : Screen("recordatorios",    "Recordatorios",  Icons.Default.NotificationsActive)
    object Temporizadores   : Screen("temporizadores",   "Temporizadores", Icons.Default.HourglassEmpty)
    object Cronometro       : Screen("cronometro",       "Cronómetro",     Icons.Default.Timer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoeyApp() {
    val vm  = viewModel<ViewModelPrincipal>()

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
        MaterialTheme(
            colorScheme = buildColorScheme(activeTheme),
            typography = DoeyTypography
        ) {
            OnboardingFlow { name, profile, performance, provider, apiKey ->
                profileStore.setUserName(name)
                profileStore.setUserProfile(
                    if (profile == UserProfile.BASIC) AlmacenPerfiles.PERFIL_BASICO else AlmacenPerfiles.PERFIL_AVANZADO
                )
                profileStore.setPerformanceMode(
                    if (performance == PerformanceMode.LOW_POWER) AlmacenPerfiles.RENDIMIENTO_BAJO else AlmacenPerfiles.RENDIMIENTO_ALTO
                )
                profileStore.setOnboardingDone(true)
                if (apiKey.isNotBlank()) {
                    val settings = vm.obtenerAjustes()
                    settings.setApiKey(provider, apiKey)
                    kotlinx.coroutines.MainScope().launch { settings.establecerProveedor(provider) }
                }
                userProfile = if (profile == UserProfile.BASIC) AlmacenPerfiles.PERFIL_BASICO else AlmacenPerfiles.PERFIL_AVANZADO
                isLowPower  = performance == PerformanceMode.LOW_POWER
                onboardingDone = true
            }
        }
        return
    }

    val nav         = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    val isAdvanced  = userProfile == AlmacenPerfiles.PERFIL_AVANZADO

    MaterialTheme(
        colorScheme = buildColorScheme(activeTheme),
        typography = DoeyTypography
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
                        composable(Screen.Habilidades.route) { SkillsScreen(vm) }
                        composable(Screen.Registros.route) { LogScreen() }
                        composable(Screen.Permisos.route) { PermissionsScreen() }
                        composable(Screen.AjustesBasicos.route) { FriendlySettingsScreen(vm) }
                        composable(Screen.ModoFlujo.route) { FlowModeScreen(vm) }
                        composable(Screen.ModoAuto.route) { PantallaModoAuto(vm) }
                        composable(Screen.Macros.route) { MacrosScreen(vm) }
                        
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoeyTopBar(vm: ViewModelPrincipal, nav: NavController, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text("Doey AI", style = MaterialTheme.typography.titleLarge, color = TauText1) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Menú", tint = TauText1)
            }
        },
        actions = {
            IconButton(onClick = { nav.navigate(Screen.Perfil.route) }) {
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
                DrawerItem(Icons.Default.Home, "Inicio", currentRoute == Screen.Inicio.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Inicio.route)
                }
                DrawerItem(Icons.Default.Person, "Mi Perfil", currentRoute == Screen.Perfil.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Perfil.route)
                }

                DrawerSectionHeader("Herramientas")
                DrawerItem(Icons.Default.Extension, "Skills", currentRoute == Screen.Habilidades.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Habilidades.route)
                }
                DrawerItem(Icons.Default.Alarm, "Agendas", currentRoute == Screen.Agendas.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Agendas.route)
                }
                DrawerItem(Icons.Default.Psychology, "Memorias", currentRoute == Screen.Memorias.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Memorias.route)
                }

                if (isAdvanced) {
                    DrawerSectionHeader("Avanzado")
                    DrawerItem(Icons.Default.AccountTree, "Modo Flujo", currentRoute == Screen.ModoFlujo.route) {
                        scope.launch { drawerState.close() }
                        nav.navigate(Screen.ModoFlujo.route)
                    }
                    DrawerItem(Icons.Default.Star, "Macros", currentRoute == Screen.Macros.route) {
                        scope.launch { drawerState.close() }
                        nav.navigate(Screen.Macros.route)
                    }
                    DrawerItem(Icons.Default.BugReport, "Logs", currentRoute == Screen.Registros.route) {
                        scope.launch { drawerState.close() }
                        nav.navigate(Screen.Registros.route)
                    }
                }

                DrawerSectionHeader("Sistema")
                DrawerItem(Icons.Default.Settings, "Ajustes", currentRoute == Screen.Configuracion.route) {
                    scope.launch { drawerState.close() }
                    nav.navigate(Screen.Configuracion.route)
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
